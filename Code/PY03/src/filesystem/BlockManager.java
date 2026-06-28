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

    /**
     * Crea un administrador de bloques asociado a un bitmap.
     * @param bitmap bitmap que gestiona el estado de los bloques
     */
    public BlockManager(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * Asigna un unico bloque libre.
     * @return numero del bloque asignado
     * @throws IllegalStateException si no hay bloques libres
     */
    public int allocateBlock() {
        int blockNumber = bitmap.findFirstFree();
        if (blockNumber == -1) {
            throw new IllegalStateException("No hay bloques libres disponibles.");
        }

        bitmap.markUsed(blockNumber);
        return blockNumber;
    }

    /**
     * Asigna una cantidad de bloques libres consecutivamente.
     * @param amount cantidad de bloques a asignar
     * @return lista con los numeros de bloques asignados
     * @throws IllegalArgumentException si amount es menor o igual a cero
     * @throws IllegalStateException si no hay suficientes bloques libres
     */
    public List<Integer> allocateBlocks(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor a cero.");
        }

        if (bitmap.countFreeBlocks() < amount) {
            throw new IllegalStateException("No hay suficientes bloques libres.");
        }

        List<Integer> allocatedBlocks = new ArrayList<>();
        for (int index = 0; index < amount; index++) {
            allocatedBlocks.add(allocateBlock());
        }

        return allocatedBlocks;
    }

    /**
     * Libera un bloque marcandolo como libre en el bitmap.
     * @param blockNumber numero del bloque a liberar
     */
    public void freeBlock(int blockNumber) {
        bitmap.markFree(blockNumber);
    }

    /**
     * Libera una lista de bloques.
     * @param blocks lista de numeros de bloque a liberar (si es null no hace nada)
     */
    public void freeBlocks(List<Integer> blocks) {
        if (blocks == null) {
            return;
        }

        for (int blockNumber : blocks) {
            freeBlock(blockNumber);
        }
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
