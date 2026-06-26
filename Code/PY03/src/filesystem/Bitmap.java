/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Bitmap de bloques del disco virtual. Un bit encendido significa que el bloque
 * esta ocupado; un bit apagado significa que esta libre.
 * 
 * @author eyden
 */
public class Bitmap {
    private static final int SERIALIZATION_MAGIC = 0x4D49424D;
    private static final int SERIALIZATION_HEADER_BYTES = Integer.BYTES * 3;
    private static final int SERIALIZED_RUN_BYTES = Integer.BYTES * 2;

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
        if (canUseRawSerialization(outputSize)) {
            return toRawBytes(outputSize);
        }

        if (outputSize < SERIALIZATION_HEADER_BYTES) {
            throw new IllegalArgumentException("el espacio para serializar el bitmap es insuficiente.");
        }

        List<BlockRun> runs = getUsedRuns();
        int requiredSize = SERIALIZATION_HEADER_BYTES + runs.size() * SERIALIZED_RUN_BYTES;
        if (requiredSize > outputSize) {
            throw new IllegalStateException(
                    "el bitmap esta demasiado fragmentado para el espacio reservado."
            );
        }

        byte[] data = new byte[outputSize];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putInt(SERIALIZATION_MAGIC);
        buffer.putInt(totalBlocks);
        buffer.putInt(runs.size());

        for (BlockRun run : runs) {
            buffer.putInt(run.start());
            buffer.putInt(run.length());
        }

        return data;
    }

    private boolean canUseRawSerialization(int outputSize) {
        long bitmapBytes = ((long) totalBlocks + Byte.SIZE - 1) / Byte.SIZE;
        return Integer.BYTES + bitmapBytes <= outputSize;
    }

    private byte[] toRawBytes(int outputSize) {
        byte[] bits = usedBlocks.toByteArray();
        byte[] data = new byte[outputSize];
        ByteBuffer.wrap(data).putInt(totalBlocks);
        System.arraycopy(bits, 0, data, Integer.BYTES, bits.length);
        return data;
    }

    public static Bitmap fromBytes(byte[] data) {
        if (data == null || data.length < Integer.BYTES) {
            throw new IllegalArgumentException("los datos del bitmap son invalidos.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int marker = buffer.getInt();

        if (marker == SERIALIZATION_MAGIC) {
            return fromRunLengthBytes(buffer, data.length);
        }

        int totalBlocks = marker;
        if (totalBlocks <= 0) {
            throw new IllegalArgumentException("el bitmap contiene una cantidad de bloques invalida.");
        }

        byte[] bits = new byte[data.length - Integer.BYTES];
        buffer.get(bits);
        return new Bitmap(totalBlocks, BitSet.valueOf(bits));
    }

    private static Bitmap fromRunLengthBytes(ByteBuffer buffer, int dataLength) {
        if (dataLength < SERIALIZATION_HEADER_BYTES) {
            throw new IllegalArgumentException("los datos del bitmap estan incompletos.");
        }

        int totalBlocks = buffer.getInt();
        int runCount = buffer.getInt();
        if (totalBlocks <= 0 || runCount < 0) {
            throw new IllegalArgumentException("los metadatos del bitmap son invalidos.");
        }

        long requiredSize = (long) SERIALIZATION_HEADER_BYTES
                + (long) runCount * SERIALIZED_RUN_BYTES;
        if (requiredSize > dataLength) {
            throw new IllegalArgumentException("los rangos del bitmap estan incompletos.");
        }

        BitSet usedBlocks = new BitSet(totalBlocks);
        int previousEnd = 0;

        for (int index = 0; index < runCount; index++) {
            int start = buffer.getInt();
            int length = buffer.getInt();
            long end = (long) start + length;

            if (start < previousEnd || length <= 0 || end > totalBlocks) {
                throw new IllegalArgumentException("el bitmap contiene un rango invalido.");
            }

            usedBlocks.set(start, (int) end);
            previousEnd = (int) end;
        }

        return new Bitmap(totalBlocks, usedBlocks);
    }

    private List<BlockRun> getUsedRuns() {
        List<BlockRun> runs = new ArrayList<>();
        int start = usedBlocks.nextSetBit(0);

        while (start >= 0 && start < totalBlocks) {
            int end = usedBlocks.nextClearBit(start);
            end = Math.min(end, totalBlocks);
            runs.add(new BlockRun(start, end - start));
            start = usedBlocks.nextSetBit(end);
        }

        return runs;
    }

    private void validateBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= totalBlocks) {
            throw new IllegalArgumentException("bloque fuera de rango: " + blockNumber);
        }
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    private record BlockRun(int start, int length) {
    }
}
