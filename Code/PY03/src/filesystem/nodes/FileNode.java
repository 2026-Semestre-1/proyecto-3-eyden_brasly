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

    /**
     * Crea un FileNode con un nuevo FCB a partir de los datos basicos del archivo.
     * @param name        nombre del archivo
     * @param owner       propietario del archivo
     * @param group       grupo propietario del archivo
     * @param permissions permisos del archivo en octal
     * @param fullPath    ruta absoluta del archivo
     */
    public FileNode(String name, String owner, String group, int permissions, String fullPath) {
        super(name, owner, group);
        this.fcb = new FCB(name, owner, group, permissions, fullPath);
    }

    /**
     * Crea un FileNode usando un FCB existente.
     * @param fcb objeto FCB con los datos tecnicos del archivo
     */
    public FileNode(FCB fcb) {
        super(fcb.getName(), fcb.getOwner(), fcb.getGroup());
        this.fcb = fcb;
    }

    public FCB getFCB() {
        return fcb;
    }

    public void setOwner(String owner) {
        super.setOwner(owner);
        fcb.setOwner(owner);
    }

    public void setGroup(String group) {
        super.setGroup(group);
        fcb.setGroup(group);
    }

    public void setPermissions(int permissions) {
        fcb.setPermissions(permissions);
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

    /**
     * Indica que este nodo no es un directorio.
     * @return false siempre
     */
    @Override
    public boolean isDirectory() {
        return false;
    }
}
