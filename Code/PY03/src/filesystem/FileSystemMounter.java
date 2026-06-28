package filesystem;

import constants.SystemConstants;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lee las estructuras persistidas en el disco virtual y construye el objeto
 * FileSystem montado cuando la firma del MBR es valida.
 */
public class FileSystemMounter {
    private static final Map<String, FileSystem> cache = new HashMap<>();

    /**
     * Invalida la entrada en cache para un disco.
     * @param diskName nombre del disco a invalidar
     */
    public static void invalidateCache(String diskName) {
        cache.remove(diskName);
    }

    /**
     * Almacena un FileSystem en la cache.
     * @param diskName   nombre del disco
     * @param fileSystem sistema de archivos a cachear
     */
    public static void cacheFileSystem(String diskName, FileSystem fileSystem) {
        cache.put(diskName, fileSystem);
    }

    /**
     * Monta el sistema de archivos desde el disco virtual.
     * Lee MBR, superbloque, bitmap, hash de root y arbol de directorios.
     * @param diskName nombre del archivo de disco virtual
     * @return sistema de archivos montado
     * @throws IOException si el disco no existe o la firma MBR es invalida
     */
    public FileSystem mount(String diskName) throws IOException {
        FileSystem cached = cache.get(diskName);
        if (cached != null) {
            return cached;
        }

        VirtualDisk disk = VirtualDisk.openExisting(diskName);
        MBR mbr = readMBR(disk);

        if (mbr == null || !mbr.hasValidSignature()) {
            throw new IOException("el disco no contiene una firma MIFS valida.");
        }

        SuperBlock superBlock = readSuperBlock(disk);
        Bitmap bitmap = loadBitmap(disk);
        BlockManager blockManager = new BlockManager(bitmap);
        String rootPasswordHash = readRootPasswordHash(disk);
        filesystem.nodes.DirectoryTree directoryTree = new DirectoryTableStore().load(disk);

        FileSystem fileSystem = new FileSystem(disk, mbr, superBlock, bitmap, blockManager, rootPasswordHash, directoryTree);
        cache.put(diskName, fileSystem);
        return fileSystem;
    }

    /**
     * Verifica si el archivo de disco por defecto existe.
     * @return true si el archivo existe
     */
    public boolean existsDisk() {
        return existsDisk(SystemConstants.VIRTUAL_DISK_FILE_NAME);
    }

    /**
     * Verifica si un archivo de disco existe.
     * @param diskName nombre del archivo de disco
     * @return true si el archivo existe
     */
    public boolean existsDisk(String diskName) {
        return new File(diskName).exists();
    }

    /**
     * Verifica si el disco por defecto contiene un sistema de archivos valido.
     * @return true si la firma MBR es valida
     */
    public boolean isValidFileSystem() {
        return isValidFileSystem(SystemConstants.VIRTUAL_DISK_FILE_NAME);
    }

    /**
     * Verifica si un disco contiene un sistema de archivos valido.
     * @param diskName nombre del archivo de disco
     * @return true si el disco existe y tiene firma MBR valida
     */
    public boolean isValidFileSystem(String diskName) {
        if (!existsDisk(diskName)) {
            return false;
        }

        try {
            MBR mbr = readMBR(VirtualDisk.openExisting(diskName));
            return mbr != null && mbr.hasValidSignature();
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Lee el MBR del disco por defecto.
     * @return objeto MBR con los datos del bloque
     * @throws IOException si ocurre un error de lectura
     */
    public MBR readMBR() throws IOException {
        return readMBR(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    /**
     * Lee el MBR de un disco virtual.
     * @param disk disco virtual de donde leer
     * @return objeto MBR con los datos del bloque
     * @throws IOException si ocurre un error de lectura
     */
    public MBR readMBR(VirtualDisk disk) throws IOException {
        return MBR.fromBytes(disk.readBlock(SystemConstants.MBR_BLOCK));
    }

    /**
     * Lee el superbloque del disco por defecto.
     * @return objeto SuperBlock con los datos
     * @throws IOException si ocurre un error de lectura
     */
    public SuperBlock readSuperBlock() throws IOException {
        return readSuperBlock(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    /**
     * Lee el superbloque de un disco virtual.
     * @param disk disco virtual de donde leer
     * @return objeto SuperBlock con los datos
     * @throws IOException si ocurre un error de lectura
     */
    public SuperBlock readSuperBlock(VirtualDisk disk) throws IOException {
        return SuperBlock.fromBytes(disk.readBlock(SystemConstants.SUPER_BLOCK));
    }

    /**
     * Carga el bitmap del disco por defecto.
     * @return objeto Bitmap restaurado
     * @throws IOException si ocurre un error de lectura
     */
    public Bitmap loadBitmap() throws IOException {
        return loadBitmap(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    /**
     * Carga el bitmap de un disco virtual.
     * @param disk disco virtual de donde leer
     * @return objeto Bitmap restaurado
     * @throws IOException si ocurre un error de lectura
     */
    public Bitmap loadBitmap(VirtualDisk disk) throws IOException {
        return new BitmapStore().load(disk);
    }

    private String readRootPasswordHash(VirtualDisk disk) throws IOException {
        return new UserTableStore().readRootPasswordHash(disk);
    }
}
