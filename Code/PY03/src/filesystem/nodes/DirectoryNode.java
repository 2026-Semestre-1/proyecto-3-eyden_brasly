/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nodo de directorio con relacion padre-hijo.
 * Los hijos se guardan ordenados por nombre para que ls sea estable.
 * 
 * @author eyden
 */
public class DirectoryNode extends FSNode {
    private DirectoryNode parent;
    private final Map<String, DirectoryNode> directories;

    public DirectoryNode(String name, String owner, String group) {
        super(name, owner, group);
        this.directories = new TreeMap<>();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    public DirectoryNode getParent() {
        return parent;
    }

    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    public boolean hasDirectory(String name) {
        return directories.containsKey(name);
    }

    public DirectoryNode getDirectory(String name) {
        return directories.get(name);
    }

    public void addDirectory(DirectoryNode directory) {
        directory.setParent(this);
        directories.put(directory.getName(), directory);
    }

    public Collection<DirectoryNode> getDirectories() {
        return Collections.unmodifiableCollection(directories.values());
    }

    public String getPath() {
        if (parent == null) {
            return "/";
        }

        String parentPath = parent.getPath();
        if ("/".equals(parentPath)) {
            return parentPath + getName();
        }

        return parentPath + "/" + getName();
    }
}
