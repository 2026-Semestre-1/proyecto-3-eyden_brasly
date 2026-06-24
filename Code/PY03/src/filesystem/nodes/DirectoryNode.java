/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private final Map<String, FileNode> files;

    public DirectoryNode(String name, String owner, String group) {
        super(name, owner, group);
        this.directories = new TreeMap<>();
        this.files = new TreeMap<>();
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
        String name = directory.getName();

        if (hasDirectory(name) || hasFile(name)) {
            throw new IllegalArgumentException("Ya existe un archivo o directorio con ese nombre: " + name);
        }

        directory.setParent(this);
        directories.put(name, directory);
    }


    public Collection<DirectoryNode> getDirectories() {
        return Collections.unmodifiableCollection(directories.values());
    }
    public boolean hasFile(String name) {
        return files.containsKey(name);
    }

    public FileNode getFile(String name) {
        return files.get(name);
    }

    public void addFile(FileNode file) {
        String name = file.getName();

        if (hasDirectory(name) || hasFile(name)) {
            throw new IllegalArgumentException("Ya existe un archivo o directorio con ese nombre: " + name);
        }

        files.put(name, file);
    }

    public Collection<FileNode> getFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    public boolean hasChild(String name) {
        return hasDirectory(name) || hasFile(name);
    }

    public List<FSNode> getChildren() {
        List<FSNode> children = new ArrayList<>();

        children.addAll(directories.values());
        children.addAll(files.values());

        return children;
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
