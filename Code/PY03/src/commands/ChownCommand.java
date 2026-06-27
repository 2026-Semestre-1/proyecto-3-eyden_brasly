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
 * Cambia dueno de archivos o directorios.
 * 
 * @author eyden
 */
public class ChownCommand implements Command {

    @Override
    public String getName() {
        return "chown";
    }

    @Override
    public String getDescription() {
        return "Cambia el dueno de archivos o directorios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        boolean recursive = args.length > 0 && "-R".equals(args[0]);
        int argOffset = recursive ? 1 : 0;

        if (args.length - argOffset < 2) {
            System.out.println("Uso: chown [-R] <usuario> <archivo|directorio> [archivo|directorio...]");
            return;
        }

        if (!PermissionSupport.canChangeOwner(session)) {
            System.out.println("chown: solo root puede cambiar el dueno de un recurso.");
            return;
        }

        String newOwner = args[argOffset].trim().toLowerCase();
        if (!session.getUserService().exists(newOwner)) {
            System.out.println("chown: no existe el usuario: " + newOwner);
            return;
        }

        boolean changedAny = false;
        for (int index = argOffset + 1; index < args.length; index++) {
            FSNode node = FileCommandSupport.findNode(session, args[index], getName());
            if (node == null) continue;

            if (recursive && node.isDirectory()) {
                changedAny |= changeOwnerRecursive((DirectoryNode) node, newOwner);
            } else {
                changedAny |= changeOwner(node, newOwner);
            }
        }

        if (changedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("chown: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }

    private boolean changeOwnerRecursive(DirectoryNode dir, String newOwner) {
        boolean changed = false;
        for (FSNode child : dir.getChildren()) {
            if (child.isDirectory()) {
                changed |= changeOwnerRecursive((DirectoryNode) child, newOwner);
            } else {
                changed |= changeOwner(child, newOwner);
            }
        }
        changed |= changeOwner(dir, newOwner);
        return changed;
    }

    private boolean changeOwner(FSNode node, String newOwner) {
        String path = node instanceof FileNode f ? f.getFullPath()
                : node instanceof DirectoryNode d ? d.getPath() : node.getName();
        node.setOwner(newOwner);
        if (node instanceof FileNode file) file.setOwner(newOwner);
        System.out.println("Dueno actualizado: " + path + " -> " + newOwner);
        return true;
    }
}
