/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

/**
 * Estado basico del File System montado para futuras extensiones.
 * Por ahora conserva la ruta de trabajo y el disco activo.
 * 
 * @author eyden
 */
public class FileSystemState {
    private FileSystem fileSystem;
    private String currentPath;

    public FileSystemState() {
        this.currentPath = "/";
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }
}
