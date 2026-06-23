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
    private final String owner;
    private final String group;

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

    public String getGroup() {
        return group;
    }

    public abstract boolean isDirectory();
}
