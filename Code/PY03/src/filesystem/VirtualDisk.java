/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import constants.SystemConstants;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Brasly
 */
public class VirtualDisk {
    private final String diskName;
    private long diskSize;
    private int totalBlocks;

    /**
     * Constructor
     * 
     */
    public VirtualDisk() throws IOException {
        this(SystemConstants.VIRTUAL_DISK_FILE_NAME, SystemConstants.VIRTUAL_DISK_DEFAULT_SIZE_MB);
    }

    public VirtualDisk(int sizeMB) throws IOException {
        this(SystemConstants.VIRTUAL_DISK_FILE_NAME, sizeMB);
    }

    public VirtualDisk(String diskName, int sizeMB) throws IOException {
        this.diskName = diskName;
        this.diskSize = (long) sizeMB * SystemConstants.BYTES_PER_MEGABYTE;
        this.totalBlocks = (int) (diskSize / SystemConstants.VIRTUAL_DISK_BLOCK_SIZE);
        RandomAccessFile disk = new RandomAccessFile(this.diskName, "rw");
        disk.setLength(0);
        disk.setLength(diskSize);
        disk.close();
    }

    private VirtualDisk(String diskName, long diskSizeBytes) {
        this.diskName = diskName;
        this.diskSize = diskSizeBytes;
        this.totalBlocks = (int) (diskSize / SystemConstants.VIRTUAL_DISK_BLOCK_SIZE);
    }

    public static VirtualDisk openExisting(String diskName) throws IOException {
        File diskFile = new File(diskName);
        if (!diskFile.exists()) {
            throw new IOException("el disco virtual no existe: " + diskName);
        }

        return new VirtualDisk(diskName, diskFile.length());
    }
    /**
     * Write a byte to an exact position on the disk
     * @param position
     * @param value
     * @throws IOException 
     */
    public void writeByte(long position, byte value)throws IOException {
        RandomAccessFile disk = new RandomAccessFile(diskName, "rw");
        disk.seek(position);
        disk.writeByte(value);

        disk.close();
    }
    /**
     * Reads a byte from an exact position on the disk.
     * @param position
     * @return the byte of the position
     * @throws IOException 
     */
    public byte readByte(long position)throws IOException {
        RandomAccessFile disk = new RandomAccessFile(diskName, "r");
        disk.seek(position);
        byte value = disk.readByte();
        disk.close();
        return value;
    }
    /**
     * convert a char to a byte and write it
     * @param position
     * @param character
     * @throws IOException 
     */
    
    public void writeChar(long position, char character) throws IOException {
        byte value = (byte) character;
        writeByte(position, value);
    }
    /**
     * read byte anb convert a byte to a char 
     * @param position
     * @return
     * @throws IOException 
     */
    public char readChar(long position) throws IOException {
        byte value = readByte(position);
        return (char) value;
    }
    /**
     * Write data in a specific block.
     * @param blockNumber
     * @param data
     * @throws IOException 
     */
    public void writeBlock(int blockNumber, byte[] data) throws IOException {
        if (data.length > SystemConstants.VIRTUAL_DISK_BLOCK_SIZE) {
            throw new IOException("los datos exceden el tamano de un bloque.");
        }

        RandomAccessFile disk = new RandomAccessFile(diskName, "rw");

        long position = (long) blockNumber * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
        byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
        System.arraycopy(data, 0, block, 0, data.length);

        disk.seek(position);
        disk.write(block);

        disk.close();
    }
    /**
     * Read a whole block.
     * @param blockNumber
     * @return
     * @throws IOException 
     */
    public byte[] readBlock(int blockNumber) throws IOException {
        RandomAccessFile disk = new RandomAccessFile(diskName, "r");

        byte[] data = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
        long position = (long) blockNumber * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;

        disk.seek(position);
        disk.read(data);

        disk.close();

        return data;
    }
     public int getBlockSize() {
        return SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public String getDiskName() {
        return diskName;
    }
    public long getDiskSize() {
        return diskSize;
    }

    
    
    
    
}
