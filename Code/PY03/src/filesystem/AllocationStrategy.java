/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

/**
 * Estrategias de asignacion soportadas por el sistema de archivos simulado.
 * En esta etapa se inicializa INDEXED por defecto, pero el enum deja listo el
 * punto de extension para las demas estrategias del proyecto.
 * 
 * @author eyden
 */
public enum AllocationStrategy {
    INDEXED,
    CONTIGUOUS,
    LINKED;

    public static AllocationStrategy fromText(String value) {
        if (value == null || value.isBlank()) {
            return INDEXED;
        }

        try {
            return AllocationStrategy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return INDEXED;
        }
    }
}
