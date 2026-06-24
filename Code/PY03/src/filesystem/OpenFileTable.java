/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Brasly
 */
public class OpenFileTable {
    private Map<String, OpenFile> openFiles;
    
    public OpenFileTable() {
        this.openFiles = new LinkedHashMap<>();
    }
    /**
     * Agrega a la tabla una archivo que se abrio
     * @param path
     * @param username
     * @param mode
     * @return true si se encontro el archivo y false si no 
     */
     public boolean openFile(String path, String username, String mode) {
        if (openFiles.containsKey(path)) {
            return false;
        }

        OpenFile file = new OpenFile(path, username, mode);
        openFiles.put(path, file);

        return true;
    }
    /**
     * cierra un archivo de la tabla 
     * @param path
     * @return 
     */
    public boolean closeFile(String path){
        if (!openFiles.containsKey(path)) {
            return false;
        }
        openFiles.remove(path);
        return true;
    }
    /**
     * valida si el archivo esta abierto 
     * @param path
     * @return true si esta e archivo false si no
     */
    public boolean isOpen(String path) {
        return openFiles.containsKey(path);
    }
    /**
     * cuneta el total de archivos abiertos 
     * @return el total de archivos 
     */
    public int getTotalFiles(){
        return openFiles.size();
    }
    /**
     * Muestra los archivos abiertos.
     */
    public void viewFilesOpen() {
        System.out.println("Total de archivos abiertos: " + openFiles.size());

        for (OpenFile file : openFiles.values()) {
            System.out.println(file);
        }
    }
    
}
