/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

/**
 * Entrada de la tabla de archivos abiertos.
 *
 * @author braslyvm
 */
public class OpenFile {
    private final String path;
    private final String username;
    private final String mode;
    private final long openedAt;

    public OpenFile(String path, String username, String mode) {
        this.path = path;
        this.username = username;
        this.mode = mode;
        this.openedAt = System.currentTimeMillis();
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return username;
    }

    public String getMode() {
        return mode;
    }

    public long getOpenedAt() {
        return openedAt;
    }
}
