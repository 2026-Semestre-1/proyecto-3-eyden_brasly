/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import constants.SystemConstants;
import filesystem.nodes.DirectoryTree;
import java.io.IOException;
import java.util.Scanner;

/**
 * Crea uno o varios archivos vacios dentro del File System.
 * 
 * @author brasly
 */
public class TouchCommand implements Command {
    @Override
    public String getName() {
        return "touch";
    }

    @Override
    public String getDescription() {
        return "Crea archivos vacios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length == 0) {
            System.out.println("Uso: touch <archivo> [archivo...]");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        boolean createdAny = false;

        for (String argument : args) {
            try {
                directoryTree.createFile(
                        session.getCurrentPath(),
                        argument,
                        session.getActiveUser().getUsername(),
                        session.getActiveUser().getPrimaryGroup(),
                        SystemConstants.DEFAULT_FILE_PERMISSIONS
                );

                createdAny = true;
                String fullPath = directoryTree.normalizePath(session.getCurrentPath(), argument);
                System.out.println("Archivo creado: " + fullPath);
            } catch (IllegalArgumentException exception) {
                System.out.println("touch: " + exception.getMessage());
            }
        }

        if (createdAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("touch: no se pudo guardar la tabla de directorios: "
                        + exception.getMessage());
            }
        }
    }
}
