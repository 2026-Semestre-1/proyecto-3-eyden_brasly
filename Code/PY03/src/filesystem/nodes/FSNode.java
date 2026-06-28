/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

/**
 * Nodo base para los recursos del File System simulado.
 * Mantiene atributos comunes de propietario, grupo y nombre.
 * 
 * @author eyden
 */
public abstract class FSNode {
    private final String name;
    private String owner;
    private String group;

    /**
     * Constructor base para un nodo del sistema de archivos.
     * @param name  nombre del nodo
     * @param owner propietario del nodo
     * @param group grupo propietario del nodo
     */
    protected FSNode(String name, String owner, String group) {
        this.name = name;
        this.owner = owner;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Indica si este nodo representa un directorio.
     * @return true si es directorio, false si es archivo
     */
    public abstract boolean isDirectory();
}
