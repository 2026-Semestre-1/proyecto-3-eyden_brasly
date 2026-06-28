package filesystem.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Son los datos tecnicos del archivo.
 * 
 * @author Brasly
 */
public class FCB {

    private String name;
    private String owner;
    private String group;
    private int permissions;
    private long creationDate;
    private int size;
    private boolean open;
    private ArrayList<Integer> blocks;
    private String fullPath;

    /**
     * Crea un FCB con valores iniciales por defecto (fecha actual, tamaño 0, cerrado, sin bloques).
     * @param name        nombre del archivo
     * @param owner       propietario del archivo
     * @param group       grupo propietario del archivo
     * @param permissions permisos del archivo en octal
     * @param fullPath    ruta absoluta del archivo
     */
    public FCB(String name, String owner, String group, int permissions, String fullPath) {
        this.name = name;
        this.owner = owner;
        this.group = group;
        this.permissions = permissions;
        this.fullPath = fullPath;
        this.creationDate = System.currentTimeMillis();
        this.size = 0;
        this.open = false;
        this.blocks = new ArrayList<>();
    }

    /**
     * Crea un FCB con todos los valores especificados, incluyendo datos históricos.
     * @param name         nombre del archivo
     * @param owner        propietario del archivo
     * @param group        grupo propietario del archivo
     * @param permissions  permisos del archivo en octal
     * @param fullPath     ruta absoluta del archivo
     * @param creationDate fecha de creacion en milisegundos desde 1970
     * @param size         tamaño del archivo en bytes
     * @param open         indica si el archivo esta abierto actualmente
     * @param blocks       lista de bloques asignados al archivo (si es null se crea vacia)
     */
    public FCB(String name, String owner, String group, int permissions, String fullPath,
        long creationDate, int size, boolean open, ArrayList<Integer> blocks) {
    this.name = name;
    this.owner = owner;
    this.group = group;
    this.permissions = permissions;
    this.fullPath = fullPath;
    this.creationDate = creationDate;
    this.size = size;
    this.open = open;
    this.blocks = blocks == null ? new ArrayList<>() : blocks;
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

    public int getPermissions() {
        return permissions;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public int getSize() {
        return size;
    }

    public boolean isOpen() {
        return open;
    }

    public ArrayList<Integer> getBlocks() {
        return blocks;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    /**
     * Agrega un bloque a la lista de bloques del archivo.
     * @param block numero de bloque a agregar
     */
    public void addBlock(int block) {
        blocks.add(block);
    }

    /**
     * Elimina todos los bloques asignados al archivo.
     */
    public void clearBlocks() {
        blocks.clear();
    }

    /**
     * Reemplaza la lista de bloques por una nueva lista.
     * @param newBlocks nueva lista de bloques (si es null se limpia la lista)
     */
    public void replaceBlocks(List<Integer> newBlocks) {
        blocks.clear();
        if (newBlocks != null) {
            blocks.addAll(newBlocks);
        }
    }
}
