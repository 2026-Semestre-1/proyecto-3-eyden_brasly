/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

/**
 * Clase que ayuda a representar archivos abiertos
 * @author braslyvm
 */
public class OpenFile {
    private String path;
    private String username;
    private String mode;
    private long openedAt;

    public OpenFile(String path, String username, String mode) {
        this.path = path;
        this.username = username;
        this.mode = mode;
        this.openedAt = System.currentTimeMillis();
    }
    
}
