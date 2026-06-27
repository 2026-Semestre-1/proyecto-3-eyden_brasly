/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

import constants.SystemConstants;
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
    private int permissions;
    private final Map<String, DirectoryNode> directories;
    private final Map<String, FileNode> files;
    private final Map<String, String> links;

    public DirectoryNode(String name, String owner, String group) {
        this(name, owner, group, SystemConstants.DEFAULT_DIRECTORY_PERMISSIONS);
    }

    public DirectoryNode(String name, String owner, String group, int permissions) {
        super(name, owner, group);
        this.permissions = permissions;
        this.directories = new TreeMap<>();
        this.files = new TreeMap<>();
        this.links = new TreeMap<>();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    public DirectoryNode getParent() {
        return parent;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
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

        if (hasChild(name)) {
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

        if (hasChild(name)) {
            throw new IllegalArgumentException("Ya existe un archivo o directorio con ese nombre: " + name);
        }

        files.put(name, file);
    }

    public Collection<FileNode> getFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    public boolean hasChild(String name) {
        return hasDirectory(name) || hasFile(name) || hasLink(name);
    }
    public boolean isEmpty() {
        return directories.isEmpty() && files.isEmpty() && links.isEmpty();
    }

    public List<FSNode> getChildren() {
        List<FSNode> children = new ArrayList<>();

        children.addAll(directories.values());
        children.addAll(files.values());

        return children;
    }
    public DirectoryNode removeDirectory(String name) {
        return directories.remove(name);
    }

    public FileNode removeFile(String name) {
        return files.remove(name);
    }
    public boolean hasLink(String name) {
        return links.containsKey(name);
    }

    public String getLinkTarget(String name) {
        return links.get(name);
    }

    public void addLink(String name, String targetPath) {
        if (hasChild(name)) {
            throw new IllegalArgumentException("Ya existe un archivo, directorio o enlace con ese nombre: " + name);
        }

        links.put(name, targetPath);
    }

    public String removeLink(String name) {
        return links.remove(name);
    }

    public void updateLink(String name, String newTargetPath) {
        if (links.containsKey(name)) {
            links.put(name, newTargetPath);
        }
    }

    public Map<String, String> getLinks() {
        return Collections.unmodifiableMap(links);
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
