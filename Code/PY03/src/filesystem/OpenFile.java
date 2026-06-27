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
    private int openCount;

    public OpenFile(String path, String username, String mode) {
        this.path = path;
        this.username = username;
        this.mode = mode;
        this.openedAt = System.currentTimeMillis();
        this.openCount = 0;
    }

    public OpenFile(String path) {
        this.path = path;
        this.username = null;
        this.mode = null;
        this.openedAt = 0;
        this.openCount = 0;
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

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }
}
