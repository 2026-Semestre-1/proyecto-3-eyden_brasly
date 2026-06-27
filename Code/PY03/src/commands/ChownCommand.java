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
        if (args.length < 2) {
            System.out.println("Uso: chown <usuario> <archivo|directorio> [archivo|directorio...]");
            return;
        }

        if (!PermissionSupport.canChangeOwner(session)) {
            System.out.println("chown: solo root puede cambiar el dueno de un recurso.");
            return;
        }

        String newOwner = args[0].trim().toLowerCase();
        if (!session.getUserService().exists(newOwner)) {
            System.out.println("chown: no existe el usuario: " + newOwner);
            return;
        }

        boolean changedAny = false;
        for (int index = 1; index < args.length; index++) {
            FSNode node = FileCommandSupport.findNode(session, args[index], getName());
            if (node == null) {
                continue;
            }

            node.setOwner(newOwner);
            if (node instanceof FileNode file) {
                file.setOwner(newOwner);
            }

            changedAny = true;
            String path = session.getFileSystem().getDirectoryTree()
                    .normalizePath(session.getCurrentPath(), args[index]);
            System.out.println("Dueno actualizado: " + path + " -> " + newOwner);
        }

        if (changedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("chown: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }
}
