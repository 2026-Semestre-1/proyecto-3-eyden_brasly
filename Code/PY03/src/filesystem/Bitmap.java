/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * Bitmap de bloques del disco virtual. Un bit encendido significa que el bloque
 * esta ocupado; un bit apagado significa que esta libre.
 * 
 * @author eyden
 */
public class Bitmap {
    private final int totalBlocks;
    private final BitSet usedBlocks;

    public Bitmap(int totalBlocks) {
        this.totalBlocks = totalBlocks;
        this.usedBlocks = new BitSet(totalBlocks);
    }

    private Bitmap(int totalBlocks, BitSet usedBlocks) {
        this.totalBlocks = totalBlocks;
        this.usedBlocks = usedBlocks;
    }

    public void markUsed(int blockNumber) {
        validateBlock(blockNumber);
        usedBlocks.set(blockNumber);
    }

    public void markFree(int blockNumber) {
        validateBlock(blockNumber);
        usedBlocks.clear(blockNumber);
    }

    public boolean isFree(int blockNumber) {
        validateBlock(blockNumber);
        return !usedBlocks.get(blockNumber);
    }

    public int findFirstFree() {
        int blockNumber = usedBlocks.nextClearBit(0);
        return blockNumber < totalBlocks ? blockNumber : -1;
    }

    public int countFreeBlocks() {
        return totalBlocks - countUsedBlocks();
    }

    public int countUsedBlocks() {
        return usedBlocks.get(0, totalBlocks).cardinality();
    }

    public byte[] toBytes(int outputSize) {
        byte[] bits = usedBlocks.toByteArray();
        byte[] data = new byte[outputSize];
        ByteBuffer.wrap(data).putInt(totalBlocks);
        System.arraycopy(bits, 0, data, Integer.BYTES, Math.min(bits.length, outputSize - Integer.BYTES));
        return data;
    }

    public static Bitmap fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int totalBlocks = buffer.getInt();
        byte[] bits = new byte[data.length - Integer.BYTES];
        buffer.get(bits);
        return new Bitmap(totalBlocks, BitSet.valueOf(bits));
    }

    private void validateBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= totalBlocks) {
            throw new IllegalArgumentException("bloque fuera de rango: " + blockNumber);
        }
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }
}
