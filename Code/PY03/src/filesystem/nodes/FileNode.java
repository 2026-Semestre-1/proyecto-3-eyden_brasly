/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

/**
 * Nodo de archivo reservado para las siguientes etapas del proyecto.
 * Por ahora se define para que el arbol pueda crecer sin cambiar la jerarquia.
 * 
 * @author eyden
 */
public class FileNode extends FSNode {
    public FileNode(String name, String owner, String group) {
        super(name, owner, group);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
