/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;
/**
 * Nodo que representa un archivo dentro del arbol de directorios.
 * Cada archivo tiene un FCB con su informacion tecnica.
 * 
 * @author Brasly
 */
public class FileNode extends FSNode {

    private FCB fcb;

    public FileNode(String name, String owner, String group, int permissions, String fullPath) {
        super(name, owner, group);
        this.fcb = new FCB(name, owner, group, permissions, fullPath);
    }

    public FileNode(FCB fcb) {
        super(fcb.getName(), fcb.getOwner(), fcb.getGroup());
        this.fcb = fcb;
    }

    public FCB getFCB() {
        return fcb;
    }

    public String getFullPath() {
        return fcb.getFullPath();
    }

    public int getSize() {
        return fcb.getSize();
    }

    public boolean isOpen() {
        return fcb.isOpen();
    }

    public void setOpen(boolean open) {
        fcb.setOpen(open);
    }

    public void setSize(int size) {
        fcb.setSize(size);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}