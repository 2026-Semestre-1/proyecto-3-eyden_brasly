/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla en memoria de los archivos abiertos en la sesion montada.
 *
 * @author Brasly
 */
public class OpenFileTable {
    private final Map<String, OpenFile> openFiles;

    /**
     * Crea una tabla de archivos abiertos vacia.
     */
    public OpenFileTable() {
        this.openFiles = new LinkedHashMap<>();
    }

    /**
     * Registra la apertura de un archivo incrementando su contador.
     * @param path ruta del archivo a abrir
     * @return contador actual de aperturas para esa ruta
     */
    public int openFile(String path) {
        OpenFile entry = openFiles.get(path);
        if (entry == null) {
            entry = new OpenFile(path);
            openFiles.put(path, entry);
        }
        entry.setOpenCount(entry.getOpenCount() + 1);
        return entry.getOpenCount();
    }

    /**
     * Registra el cierre de un archivo decrementando su contador.
     * Si el contador llega a cero, elimina la entrada de la tabla.
     * @param path ruta del archivo a cerrar
     * @return contador restante de aperturas, o 0 si ya no esta abierto
     */
    public int closeFile(String path) {
        OpenFile entry = openFiles.get(path);
        if (entry == null) {
            return 0;
        }
        entry.setOpenCount(entry.getOpenCount() - 1);
        if (entry.getOpenCount() <= 0) {
            openFiles.remove(path);
            return 0;
        }
        return entry.getOpenCount();
    }

    /**
     * Verifica si un archivo esta abierto.
     * @param path ruta del archivo
     * @return true si el archivo esta registrado como abierto
     */
    public boolean isOpen(String path) {
        return openFiles.containsKey(path);
    }

    /**
     * Obtiene el contador de aperturas de un archivo.
     * @param path ruta del archivo
     * @return cantidad de aperturas activas, o 0 si no esta abierto
     */
    public int getOpenCount(String path) {
        OpenFile entry = openFiles.get(path);
        return entry == null ? 0 : entry.getOpenCount();
    }

    /**
     * Obtiene la cantidad de archivos abiertos distintos.
     * @return cantidad de archivos abiertos
     */
    public int getTotalFiles() {
        return openFiles.size();
    }

    /**
     * Obtiene una vista no modificable de todos los archivos abiertos.
     * @return coleccion de registros de archivos abiertos
     */
    public Collection<OpenFile> getOpenFiles() {
        return Collections.unmodifiableCollection(openFiles.values());
    }
}
