/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.FSNode;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Cambia grupo de archivos o directorios.
 * 
 * @author eyden
 */
public class ChgrpCommand implements Command {

    @Override
    public String getName() {
        return "chgrp";
    }

    @Override
    public String getDescription() {
        return "Cambia el grupo de archivos o directorios.";
    }
    /**
     * Ejecuta el comando chgrp, cambiando el grupo de archivos o directorios especificados.
     * @param args Los argumentos del comando, donde args[0] es el nuevo grupo y los siguientes son los archivos o directorios a modificar.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        boolean recursive = args.length > 0 && "-R".equals(args[0]);
        int argOffset = recursive ? 1 : 0;

        if (args.length - argOffset < 2) {
            System.out.println("Uso: chgrp [-R] <grupo> <archivo|directorio> [archivo|directorio...]");
            return;
        }

        String newGroup = args[argOffset].trim().toLowerCase();
        if (!session.getGroupService().exists(newGroup)) {
            System.out.println("chgrp: no existe el grupo: " + newGroup);
            return;
        }

        boolean changedAny = false;
        for (int index = argOffset + 1; index < args.length; index++) {
            FSNode node = FileCommandSupport.findNode(session, args[index], getName());
            if (node == null) continue;

            if (recursive && node.isDirectory()) {
                changedAny |= changeGroupRecursive((DirectoryNode) node, newGroup, session);
            } else {
                changedAny |= changeGroup(node, newGroup, session);
            }
        }

        if (changedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("chgrp: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }
    /**
     * Cambia el grupo de un directorio y todos sus hijos de manera recursiva.
     * @param dir El directorio cuyo grupo se desea cambiar.
     * @param newGroup El nuevo grupo a asignar.
     * @param session La sesión de terminal actual.
     * @return true si se cambió al menos un grupo, false en caso contrario.
     */
    private boolean changeGroupRecursive(DirectoryNode dir, String newGroup, TerminalSession session) {
        boolean changed = false;
        for (FSNode child : dir.getChildren()) {
            if (child.isDirectory()) {
                changed |= changeGroupRecursive((DirectoryNode) child, newGroup, session);
            } else {
                changed |= changeGroup(child, newGroup, session);
            }
        }
        changed |= changeGroup(dir, newGroup, session);
        return changed;
    }
    /**
     * Cambia el grupo de un nodo de archivo o directorio.
     * @param node El nodo cuyo grupo se desea cambiar.
     * @param newGroup El nuevo grupo a asignar.
     * @param session La sesión de terminal actual.
     * @return true si se cambió el grupo, false en caso contrario.
     */
    private boolean changeGroup(FSNode node, String newGroup, TerminalSession session) {
        String path = node instanceof FileNode f ? f.getFullPath()
                : node instanceof DirectoryNode d ? d.getPath() : node.getName();
        if (!PermissionSupport.canChangeGroup(session, node, newGroup)) {
            PermissionSupport.deny(getName(), "cambiar grupo", path);
            return false;
        }
        node.setGroup(newGroup);
        if (node instanceof FileNode file) file.setGroup(newGroup);
        System.out.println("Grupo actualizado: " + path + " -> " + newGroup);
        return true;
    }
}
