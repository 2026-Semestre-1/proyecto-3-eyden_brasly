/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class MkdirCommand implements Command {
    @Override
    public String getName() {
        return "mkdir";
    }

    @Override
    public String getDescription() {
        return "Crea uno o varios directorios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length == 0) {
            System.out.println("Uso: mkdir <directorio> [directorio...]");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        boolean createdAny = false;

        for (String argument : args) {
            try {
                String targetPath = directoryTree.normalizePath(session.getCurrentPath(), argument);
                String parentPath = getParentPath(targetPath);
                String directoryName = getName(targetPath);

                directoryTree.createDirectory(
                        parentPath,
                        directoryName,
                        session.getActiveUser().getUsername(),
                        session.getActiveUser().getPrimaryGroup()
                );
                createdAny = true;
                System.out.println("Directorio creado: " + targetPath);
            } catch (IllegalArgumentException exception) {
                System.out.println("mkdir: " + exception.getMessage());
            }
        }

        if (createdAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("mkdir: no se pudo guardar la tabla de directorios: " + exception.getMessage());
            }
        }
    }

    private String getParentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    private String getName(String path) {
        int separator = path.lastIndexOf('/');
        return separator == -1 ? path : path.substring(separator + 1);
    }
}
