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

    public OpenFileTable() {
        this.openFiles = new LinkedHashMap<>();
    }

    public int openFile(String path) {
        OpenFile entry = openFiles.get(path);
        if (entry == null) {
            entry = new OpenFile(path);
            openFiles.put(path, entry);
        }
        entry.setOpenCount(entry.getOpenCount() + 1);
        return entry.getOpenCount();
    }

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

    public boolean isOpen(String path) {
        return openFiles.containsKey(path);
    }

    public int getOpenCount(String path) {
        OpenFile entry = openFiles.get(path);
        return entry == null ? 0 : entry.getOpenCount();
    }

    public int getTotalFiles() {
        return openFiles.size();
    }

    public Collection<OpenFile> getOpenFiles() {
        return Collections.unmodifiableCollection(openFiles.values());
    }
}
