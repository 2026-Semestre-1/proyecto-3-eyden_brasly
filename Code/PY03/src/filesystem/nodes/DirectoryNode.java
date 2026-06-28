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
    private final Map<String, LinkNode> links;

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
    /**
     * Obtiene todos los hijos del directorio, incluyendo subdirectorios, archivos y enlaces.
     * @return Una lista de todos los nodos hijos del directorio.
     */
    public List<FSNode> getChildren() {
        List<FSNode> children = new ArrayList<>();

        children.addAll(directories.values());
        children.addAll(files.values());
        children.addAll(links.values());

        return children;
    }
    /**
     * Elimina un directorio hijo del directorio actual.
     * @param name El nombre del directorio a eliminar.
     * @return El nodo de directorio eliminado, o null si no se encontró.
     */
    public DirectoryNode removeDirectory(String name) {
        return directories.remove(name);
    }
    /**
     * Elimina un archivo hijo del directorio actual.
     * @param name El nombre del archivo a eliminar.
     * @return El nodo de archivo eliminado, o null si no se encontró.
     */
    public FileNode removeFile(String name) {
        return files.remove(name);
    }
    /**
     * Actualiza el nombre de un directorio hijo del directorio actual.
     * @param oldName El nombre actual del directorio.
     * @param newName El nuevo nombre a asignar al directorio.
     */
    public boolean hasLink(String name) {
        return links.containsKey(name);
    }

    /**
     * Obtiene el nodo de enlace hijo del directorio actual por su nombre.
     * @param name El nombre del enlace a obtener.
     * @return El nodo de enlace encontrado, o null si no se encontró.
     */
    public String getLinkTarget(String name) {
        LinkNode link = links.get(name);
        return link == null ? null : link.getTarget();
    }
    /**
     * Actualiza el nombre de un archivo hijo del directorio actual.
     * @param oldName El nombre actual del archivo.
     * @param newName El nuevo nombre a asignar al archivo.
     */
    public LinkNode getLink(String name) {
        return links.get(name);
    }
    /**
     * Agrega un enlace hijo al directorio actual.
     * @param link El nodo de enlace a agregar.
     */
    public void addLink(LinkNode link) {
        String name = link.getName();

        if (hasChild(name)) {
            throw new IllegalArgumentException("Ya existe un archivo, directorio o enlace con ese nombre: " + name);
        }

        links.put(name, link);
    }
    /**
     * Actualiza el nombre de un enlace hijo del directorio actual.
     * @param oldName El nombre actual del enlace.
     * @param newName El nuevo nombre a asignar al enlace.
     */
    public LinkNode removeLink(String name) {
        return links.remove(name);
    }
    /**
     * Actualiza el nombre de un enlace hijo del directorio actual.
     * @param oldName El nombre actual del enlace.
     * @param newName El nuevo nombre a asignar al enlace.
     */
    public void updateLink(String name, String newTargetPath) {
        LinkNode link = links.get(name);
        if (link != null) {
            link.setTarget(newTargetPath);
        }
    }
    /**
     * Obtiene todos los enlaces hijos del directorio actual.
     * @return Una colección de todos los nodos de enlace hijos del directorio.
     */
    public Map<String, LinkNode> getLinks() {
        return Collections.unmodifiableMap(links);
    }
    /**
     * Verifica si el directorio actual tiene enlaces hijos.
     * @return true si hay enlaces hijos, false en caso contrario.
     */
    public boolean hasLinks() {
        return !links.isEmpty();
    }

    /**
     * Obtiene la ruta completa del directorio actual desde la raíz del sistema de archivos.
     * @return La ruta completa del directorio.
     */
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
