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

    public boolean openFile(String path, String username, String mode) {
        if (openFiles.containsKey(path)) {
            return false;
        }

        OpenFile file = new OpenFile(path, username, mode);
        openFiles.put(path, file);

        return true;
    }

    public boolean closeFile(String path) {
        return openFiles.remove(path) != null;
    }

    public boolean isOpen(String path) {
        return openFiles.containsKey(path);
    }

    public int getTotalFiles() {
        return openFiles.size();
    }

    public Collection<OpenFile> getOpenFiles() {
        return Collections.unmodifiableCollection(openFiles.values());
    }
}
