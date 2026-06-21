/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Brasly
 */
public class VirtualDisk {
    private static final String Disk_name = "miDiscoDuro.fs";
    private static final int BLOCK_SIZE = 512;
    
    private long diskSize;
    private int totalBlocks;
    /**
     * Constructor
     * 
     */
    public VirtualDisk (int sizeMB)throws IOException {
        this.diskSize = (long) sizeMB * 1024 * 1024;
        this.totalBlocks = (int) (diskSize / BLOCK_SIZE); 
        RandomAccessFile disk = new RandomAccessFile(Disk_name, "rw");
        disk.setLength(diskSize);
        disk.close();
    }
    /**
     * Write a byte to an exact position on the disk
     * @param position
     * @param value
     * @throws IOException 
     */
    public void writeByte(long position, byte value)throws IOException {
        RandomAccessFile disk = new RandomAccessFile(Disk_name, "rw");
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
        RandomAccessFile disk = new RandomAccessFile(Disk_name, "r");
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
        RandomAccessFile disk = new RandomAccessFile(Disk_name, "rw");

        long position = blockNumber * BLOCK_SIZE;

        disk.seek(position);
        disk.write(data);

        disk.close();
    }
    /**
     * Read a whole block.
     * @param blockNumber
     * @return
     * @throws IOException 
     */
    public byte[] readBlock(int blockNumber) throws IOException {
        RandomAccessFile disk = new RandomAccessFile(Disk_name, "r");

        byte[] data = new byte[BLOCK_SIZE];
        long position = blockNumber * BLOCK_SIZE;

        disk.seek(position);
        disk.read(data);

        disk.close();

        return data;
    }
     public int getBlockSize() {
        return BLOCK_SIZE;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public String getDiskName() {
        return Disk_name;
    }
    public long getDiskSize() {
        return diskSize;
    }

    
    
    
    
}
