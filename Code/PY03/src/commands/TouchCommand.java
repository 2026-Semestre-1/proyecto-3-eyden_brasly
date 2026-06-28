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
    /**
     * Ejecuta el comando touch, creando uno o varios archivos vacíos en el sistema de archivos.
     * @param args Los argumentos del comando, donde args[0] es la ruta del archivo a crear y args[1] es opcionalmente la ruta de otro archivo a crear.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
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
                String fullPath = directoryTree.normalizePath(session.getCurrentPath(), argument);
                String parentPath = FileCommandSupport.parentPath(fullPath);
                var parent = directoryTree.find(parentPath)
                        .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));
                if (!PermissionSupport.hasAll(
                        session,
                        parent,
                        PermissionSupport.Access.WRITE,
                        PermissionSupport.Access.EXECUTE
                )) {
                    PermissionSupport.deny(getName(), "crear en", parentPath);
                    continue;
                }

                directoryTree.createFile(
                        session.getCurrentPath(),
                        argument,
                        session.getActiveUser().getUsername(),
                        session.getActiveUser().getPrimaryGroup(),
                        SystemConstants.DEFAULT_FILE_PERMISSIONS
                );

                createdAny = true;
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
