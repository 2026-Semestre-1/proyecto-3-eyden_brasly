package filesystem;

import filesystem.nodes.FCB;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordina la lectura y escritura del contenido de un archivo.
 *
 * Esta clase debe mantener sincronizados cuatro elementos:
 * - Los bytes escritos en el disco virtual.
 * - Los bloques ocupados en el Bitmap.
 * - La lista de bloques y el tamano almacenados en el FCB.
 * - La tabla de directorios persistida.
 *
 * La lista de bloques del FCB funciona como indice ordenado del contenido.
 */
public class FileContentService {
    private final FileSystem fileSystem;
    private final VirtualDisk disk;
    private final BlockManager blockManager;

    public FileContentService(FileSystem fileSystem) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem no puede ser null");
        this.disk = fileSystem.getDisk();
        this.blockManager = fileSystem.getBlockManager();
    }

    /**
     * Lee exactamente la cantidad de bytes indicada por el FCB.
     *
     * @param file archivo que se desea leer
     * @return contenido decodificado en UTF-8
     * @throws IOException si ocurre un error al leer el disco
     */
    public String readContent(FileNode file) throws IOException {
        FCB fcb = requireFCB(file);
        int requiredBlocks = requiredBlockCount(fcb.getSize());
        List<Integer> blocks = fcb.getBlocks();

        if (blocks.size() != requiredBlocks) {
            throw new IOException(
                    "el FCB de " + fcb.getFullPath()
                    + " no coincide con el tamano y los bloques registrados."
            );
        }

        byte[] content = new byte[fcb.getSize()];
        int destinationOffset = 0;

        for (int blockNumber : blocks) {
            validateAllocatedDataBlock(blockNumber);
            byte[] block = disk.readBlock(blockNumber);
            int bytesToCopy = Math.min(block.length, content.length - destinationOffset);
            System.arraycopy(block, 0, content, destinationOffset, bytesToCopy);
            destinationOffset += bytesToCopy;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Reemplaza completamente el contenido actual de un archivo.
     *
     * @param file archivo que se desea modificar
     * @param content nuevo contenido; null no es valido
     * @throws IOException si ocurre un error al escribir o persistir cambios
     */
    public void writeContent(FileNode file, String content) throws IOException {
        FCB fcb = requireFCB(file);
        Objects.requireNonNull(content, "content no puede ser null");
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        List<Integer> oldBlocks = new ArrayList<>(fcb.getBlocks());
        int oldSize = fcb.getSize();
        int requiredBlocks = requiredBlockCount(contentBytes.length);
        List<Integer> targetBlocks = new ArrayList<>();
        List<Integer> allocatedBlocks = new ArrayList<>();
        List<Integer> releasedBlocks = new ArrayList<>();
        Map<Integer, byte[]> originalData = new LinkedHashMap<>();

        try {
            for (int blockNumber : oldBlocks) {
                validateAllocatedDataBlock(blockNumber);
            }

            int reusedBlockCount = Math.min(oldBlocks.size(), requiredBlocks);
            for (int index = 0; index < reusedBlockCount; index++) {
                int blockNumber = oldBlocks.get(index);
                targetBlocks.add(blockNumber);
                originalData.put(blockNumber, disk.readBlock(blockNumber));
            }

            int additionalBlockCount = requiredBlocks - reusedBlockCount;
            if (additionalBlockCount > 0) {
                allocatedBlocks.addAll(blockManager.allocateBlocks(additionalBlockCount));
                targetBlocks.addAll(allocatedBlocks);
            }

            List<byte[]> fragments = splitIntoBlocks(contentBytes);
            for (int index = 0; index < fragments.size(); index++) {
                disk.writeBlock(targetBlocks.get(index), fragments.get(index));
            }

            if (oldBlocks.size() > requiredBlocks) {
                releasedBlocks.addAll(oldBlocks.subList(requiredBlocks, oldBlocks.size()));
                blockManager.freeBlocks(releasedBlocks);
            }

            fcb.replaceBlocks(targetBlocks);
            fcb.setSize(contentBytes.length);
            persistMetadata();
        } catch (IOException | RuntimeException exception) {
            IOException rollbackFailure = rollback(
                    fcb,
                    oldBlocks,
                    oldSize,
                    allocatedBlocks,
                    releasedBlocks,
                    originalData
            );

            if (rollbackFailure != null) {
                exception.addSuppressed(rollbackFailure);
            }

            if (exception instanceof IOException ioException) {
                throw ioException;
            }

            throw exception;
        }
    }

    /**
     * Deja el archivo con tamano cero y sin bloques de datos.
     */
    public void clearContent(FileNode file) throws IOException {
        writeContent(file, "");
    }

    /**
     * Calcula cuantos bloques se necesitan para almacenar una cantidad de
     * bytes. Un archivo vacio no necesita bloques de datos.
     */
    private int requiredBlockCount(int contentLength) {
        if (contentLength < 0) {
            throw new IllegalArgumentException("el tamano del contenido no puede ser negativo.");
        }

        if (contentLength == 0) {
            return 0;
        }

        return (int) (((long) contentLength + disk.getBlockSize() - 1) / disk.getBlockSize());
    }

    /**
     * Divide el contenido en arreglos que no excedan el tamano de un bloque.
     */
    private List<byte[]> splitIntoBlocks(byte[] contentBytes) {
        Objects.requireNonNull(contentBytes, "contentBytes no puede ser null");
        List<byte[]> blocks = new ArrayList<>();

        for (int offset = 0; offset < contentBytes.length; offset += disk.getBlockSize()) {
            int end = Math.min(offset + disk.getBlockSize(), contentBytes.length);
            blocks.add(Arrays.copyOfRange(contentBytes, offset, end));
        }

        return blocks;
    }

    /**
     * Persiste las estructuras modificadas despues de una escritura exitosa.
     */
    private void persistMetadata() throws IOException {
        fileSystem.saveBitmap();
        fileSystem.saveDirectories();
    }

    private FCB requireFCB(FileNode file) {
        Objects.requireNonNull(file, "file no puede ser null");
        return Objects.requireNonNull(file.getFCB(), "el archivo no contiene un FCB");
    }

    private IOException rollback(
            FCB fcb,
            List<Integer> oldBlocks,
            int oldSize,
            List<Integer> allocatedBlocks,
            List<Integer> releasedBlocks,
            Map<Integer, byte[]> originalData
    ) {
        IOException failure = null;

        for (Map.Entry<Integer, byte[]> entry : originalData.entrySet()) {
            try {
                disk.writeBlock(entry.getKey(), entry.getValue());
            } catch (IOException exception) {
                failure = mergeFailures(failure, exception);
            }
        }

        blockManager.freeBlocks(allocatedBlocks);
        for (int blockNumber : releasedBlocks) {
            blockManager.getBitmap().markUsed(blockNumber);
        }

        fcb.replaceBlocks(oldBlocks);
        fcb.setSize(oldSize);

        try {
            persistMetadata();
        } catch (IOException exception) {
            failure = mergeFailures(failure, exception);
        }

        return failure;
    }

    private IOException mergeFailures(IOException current, IOException next) {
        if (current == null) {
            return next;
        }

        current.addSuppressed(next);
        return current;
    }

    private void validateDataBlock(int blockNumber) throws IOException {
        if (blockNumber < constants.SystemConstants.DATA_START_BLOCK
                || blockNumber >= disk.getTotalBlocks()) {
            throw new IOException("el FCB referencia un bloque de datos invalido: " + blockNumber);
        }
    }

    private void validateAllocatedDataBlock(int blockNumber) throws IOException {
        validateDataBlock(blockNumber);
        if (blockManager.getBitmap().isFree(blockNumber)) {
            throw new IOException(
                    "el bloque " + blockNumber + " del FCB no esta ocupado en el bitmap."
            );
        }
    }
}
