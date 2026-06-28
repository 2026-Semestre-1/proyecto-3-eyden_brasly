
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

/**
 * Nodo que representa un enlace o acceso directo a otro nodo del sistema de archivos.
 * 
 * @author eyden
 */
public class LinkNode extends FSNode {
    private String target;

    /**
     * Crea un enlace apuntando a un destino.
     * @param name   nombre del enlace
     * @param owner  propietario del enlace
     * @param group  grupo propietario del enlace
     * @param target ruta del nodo al que apunta el enlace
     */
    public LinkNode(String name, String owner, String group, String target) {
        super(name, owner, group);
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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
