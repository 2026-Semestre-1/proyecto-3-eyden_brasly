/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
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

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length < 2) {
            System.out.println("Uso: chgrp <grupo> <archivo|directorio> [archivo|directorio...]");
            return;
        }

        String newGroup = args[0].trim().toLowerCase();
        if (!session.getGroupService().exists(newGroup)) {
            System.out.println("chgrp: no existe el grupo: " + newGroup);
            return;
        }

        boolean changedAny = false;
        for (int index = 1; index < args.length; index++) {
            FSNode node = FileCommandSupport.findNode(session, args[index], getName());
            if (node == null) {
                continue;
            }

            String path = session.getFileSystem().getDirectoryTree()
                    .normalizePath(session.getCurrentPath(), args[index]);
            if (!PermissionSupport.canChangeGroup(session, node, newGroup)) {
                PermissionSupport.deny(getName(), "cambiar grupo", path);
                continue;
            }

            node.setGroup(newGroup);
            if (node instanceof FileNode file) {
                file.setGroup(newGroup);
            }

            changedAny = true;
            System.out.println("Grupo actualizado: " + path + " -> " + newGroup);
        }

        if (changedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("chgrp: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }
}
