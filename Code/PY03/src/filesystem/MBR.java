/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import constants.SystemConstants;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Master Boot Record del disco virtual.
 * Se guarda en el bloque 0 y contiene la informacion minima para validar y
 * ubicar las estructuras iniciales del File System.
 * 
 * @author eyden
 */
public class MBR {
    private final String signature;
    private final String fileSystemName;
    private final int diskSizeMB;
    private final int blockSize;
    private final int totalBlocks;
    private final AllocationStrategy allocationStrategy;
    private final int superBlockStartBlock;
    private final int bitmapStartBlock;
    private final int userTableStartBlock;
    private final int rootDirectoryStartBlock;

    /**
     * Crea un MBR con los valores por defecto tomados de SystemConstants.
     * @param diskSizeMB         tamano del disco en megabytes
     * @param totalBlocks        cantidad total de bloques
     * @param allocationStrategy estrategia de asignacion
     */
    public MBR(int diskSizeMB, int totalBlocks, AllocationStrategy allocationStrategy) {
        this(
                SystemConstants.FILE_SYSTEM_SIGNATURE,
                SystemConstants.FILE_SYSTEM_NAME,
                diskSizeMB,
                SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                totalBlocks,
                allocationStrategy,
                SystemConstants.SUPER_BLOCK,
                SystemConstants.BITMAP_START_BLOCK,
                SystemConstants.USER_TABLE_START_BLOCK,
                SystemConstants.ROOT_DIRECTORY_START_BLOCK
        );
    }

    private MBR(
            String signature,
            String fileSystemName,
            int diskSizeMB,
            int blockSize,
            int totalBlocks,
            AllocationStrategy allocationStrategy,
            int superBlockStartBlock,
            int bitmapStartBlock,
            int userTableStartBlock,
            int rootDirectoryStartBlock
    ) {
        this.signature = signature;
        this.fileSystemName = fileSystemName;
        this.diskSizeMB = diskSizeMB;
        this.blockSize = blockSize;
        this.totalBlocks = totalBlocks;
        this.allocationStrategy = allocationStrategy;
        this.superBlockStartBlock = superBlockStartBlock;
        this.bitmapStartBlock = bitmapStartBlock;
        this.userTableStartBlock = userTableStartBlock;
        this.rootDirectoryStartBlock = rootDirectoryStartBlock;
    }

    /**
     * Verifica si la firma del MBR coincide con la firma esperada del sistema de archivos.
     * @return true si la firma es valida
     */
    public boolean hasValidSignature() {
        return SystemConstants.FILE_SYSTEM_SIGNATURE.equals(signature);
    }

    /**
     * Serializa el MBR a un arreglo de bytes con formato clave=valor.
     * @return arreglo de bytes del tamano de un bloque
     */
    public byte[] toBytes() {
        String data = ""
                + "signature=" + signature + "\n"
                + "fileSystemName=" + fileSystemName + "\n"
                + "diskSizeMB=" + diskSizeMB + "\n"
                + "blockSize=" + blockSize + "\n"
                + "totalBlocks=" + totalBlocks + "\n"
                + "allocationStrategy=" + allocationStrategy.name() + "\n"
                + "superBlockStartBlock=" + superBlockStartBlock + "\n"
                + "bitmapStartBlock=" + bitmapStartBlock + "\n"
                + "userTableStartBlock=" + userTableStartBlock + "\n"
                + "rootDirectoryStartBlock=" + rootDirectoryStartBlock + "\n";

        return toFixedBlock(data);
    }

    /**
     * Deserializa un MBR desde un arreglo de bytes con formato clave=valor.
     * @param bytes datos del bloque leido del disco
     * @return objeto MBR restaurado, o null si los datos estan vacios
     */
    public static MBR fromBytes(byte[] bytes) {
        Map<String, String> values = parseBlock(bytes);
        if (values.isEmpty()) {
            return null;
        }

        return new MBR(
                values.getOrDefault("signature", ""),
                values.getOrDefault("fileSystemName", ""),
                parseInt(values.get("diskSizeMB")),
                parseInt(values.get("blockSize")),
                parseInt(values.get("totalBlocks")),
                AllocationStrategy.fromText(values.get("allocationStrategy")),
                parseInt(values.get("superBlockStartBlock")),
                parseInt(values.get("bitmapStartBlock")),
                parseInt(values.get("userTableStartBlock")),
                parseInt(values.get("rootDirectoryStartBlock"))
        );
    }

    private static byte[] toFixedBlock(String data) {
        byte[] source = data.getBytes(StandardCharsets.UTF_8);
        byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
        System.arraycopy(source, 0, block, 0, Math.min(source.length, block.length));
        return block;
    }

    /**
     * Parsea un bloque de bytes con lineas clave=valor a un mapa.
     * @param bytes datos del bloque
     * @return mapa con las claves y valores extraidos
     */
    static Map<String, String> parseBlock(byte[] bytes) {
        String data = new String(bytes, StandardCharsets.UTF_8).replace("\0", "").trim();
        Map<String, String> values = new LinkedHashMap<>();

        if (data.isEmpty()) {
            return values;
        }

        for (String line : data.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }

        return values;
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Integer.parseInt(value.trim());
    }

    public String getSignature() {
        return signature;
    }

    public String getFileSystemName() {
        return fileSystemName;
    }

    public int getDiskSizeMB() {
        return diskSizeMB;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }

    public int getSuperBlockStartBlock() {
        return superBlockStartBlock;
    }

    public int getBitmapStartBlock() {
        return bitmapStartBlock;
    }

    public int getUserTableStartBlock() {
        return userTableStartBlock;
    }

    public int getRootDirectoryStartBlock() {
        return rootDirectoryStartBlock;
    }
}
