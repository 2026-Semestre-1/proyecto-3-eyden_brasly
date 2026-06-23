/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Administra la asignacion y liberacion de bloques usando el Bitmap.
 * 
 * @author eyden
 */
public class BlockManager {
    private final Bitmap bitmap;

    public BlockManager(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int allocateBlock() {
        int blockNumber = bitmap.findFirstFree();
        if (blockNumber == -1) {
            throw new IllegalStateException("no hay bloques libres disponibles.");
        }

        bitmap.markUsed(blockNumber);
        return blockNumber;
    }

    public void freeBlock(int blockNumber) {
        bitmap.markFree(blockNumber);
    }

    public List<Integer> allocateBlocks(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("la cantidad de bloques debe ser mayor a cero.");
        }

        List<Integer> allocatedBlocks = new ArrayList<>();
        for (int index = 0; index < amount; index++) {
            allocatedBlocks.add(allocateBlock());
        }

        return allocatedBlocks;
    }

    public int getFreeBlocks() {
        return bitmap.countFreeBlocks();
    }

    public int getUsedBlocks() {
        return bitmap.countUsedBlocks();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
