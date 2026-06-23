/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import constants.SystemConstants;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * SuperBlock persistido en el bloque 1.
 * Guarda el resumen de capacidad y las posiciones principales del File System.
 * 
 * @author eyden
 */
public class SuperBlock {
    private final String fileSystemName;
    private final long totalSizeBytes;
    private final int blockSize;
    private final int totalBlocks;
    private final int usedBlocks;
    private final int freeBlocks;
    private final int mbrBlock;
    private final int bitmapStartBlock;
    private final int userTableStartBlock;
    private final int groupTableStartBlock;
    private final int rootDirectoryBlock;
    private final int dataStartBlock;

    public SuperBlock(long totalSizeBytes, int totalBlocks, int usedBlocks, int freeBlocks) {
        this(
                SystemConstants.FILE_SYSTEM_NAME,
                totalSizeBytes,
                SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                totalBlocks,
                usedBlocks,
                freeBlocks,
                SystemConstants.MBR_BLOCK,
                SystemConstants.BITMAP_START_BLOCK,
                SystemConstants.USER_TABLE_START_BLOCK,
                SystemConstants.GROUP_TABLE_START_BLOCK,
                SystemConstants.ROOT_DIRECTORY_START_BLOCK,
                SystemConstants.DATA_START_BLOCK
        );
    }

    private SuperBlock(
            String fileSystemName,
            long totalSizeBytes,
            int blockSize,
            int totalBlocks,
            int usedBlocks,
            int freeBlocks,
            int mbrBlock,
            int bitmapStartBlock,
            int userTableStartBlock,
            int groupTableStartBlock,
            int rootDirectoryBlock,
            int dataStartBlock
    ) {
        this.fileSystemName = fileSystemName;
        this.totalSizeBytes = totalSizeBytes;
        this.blockSize = blockSize;
        this.totalBlocks = totalBlocks;
        this.usedBlocks = usedBlocks;
        this.freeBlocks = freeBlocks;
        this.mbrBlock = mbrBlock;
        this.bitmapStartBlock = bitmapStartBlock;
        this.userTableStartBlock = userTableStartBlock;
        this.groupTableStartBlock = groupTableStartBlock;
        this.rootDirectoryBlock = rootDirectoryBlock;
        this.dataStartBlock = dataStartBlock;
    }

    public byte[] toBytes() {
        String data = ""
                + "fileSystemName=" + fileSystemName + "\n"
                + "totalSizeBytes=" + totalSizeBytes + "\n"
                + "blockSize=" + blockSize + "\n"
                + "totalBlocks=" + totalBlocks + "\n"
                + "usedBlocks=" + usedBlocks + "\n"
                + "freeBlocks=" + freeBlocks + "\n"
                + "mbrBlock=" + mbrBlock + "\n"
                + "bitmapStartBlock=" + bitmapStartBlock + "\n"
                + "userTableStartBlock=" + userTableStartBlock + "\n"
                + "groupTableStartBlock=" + groupTableStartBlock + "\n"
                + "rootDirectoryBlock=" + rootDirectoryBlock + "\n"
                + "dataStartBlock=" + dataStartBlock + "\n";

        byte[] source = data.getBytes(StandardCharsets.UTF_8);
        byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
        System.arraycopy(source, 0, block, 0, Math.min(source.length, block.length));
        return block;
    }

    public static SuperBlock fromBytes(byte[] bytes) {
        Map<String, String> values = MBR.parseBlock(bytes);
        if (values.isEmpty()) {
            return null;
        }

        return new SuperBlock(
                values.getOrDefault("fileSystemName", ""),
                parseLong(values.get("totalSizeBytes")),
                parseInt(values.get("blockSize")),
                parseInt(values.get("totalBlocks")),
                parseInt(values.get("usedBlocks")),
                parseInt(values.get("freeBlocks")),
                parseInt(values.get("mbrBlock")),
                parseInt(values.get("bitmapStartBlock")),
                parseInt(values.get("userTableStartBlock")),
                parseInt(values.get("groupTableStartBlock")),
                parseInt(values.get("rootDirectoryBlock")),
                parseInt(values.get("dataStartBlock"))
        );
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Integer.parseInt(value.trim());
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }

        return Long.parseLong(value.trim());
    }

    public String getFileSystemName() {
        return fileSystemName;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getUsedBlocks() {
        return usedBlocks;
    }

    public int getFreeBlocks() {
        return freeBlocks;
    }

    public int getMbrBlock() {
        return mbrBlock;
    }

    public int getBitmapStartBlock() {
        return bitmapStartBlock;
    }

    public int getUserTableStartBlock() {
        return userTableStartBlock;
    }

    public int getGroupTableStartBlock() {
        return groupTableStartBlock;
    }

    public int getRootDirectoryBlock() {
        return rootDirectoryBlock;
    }

    public int getDataStartBlock() {
        return dataStartBlock;
    }
}
