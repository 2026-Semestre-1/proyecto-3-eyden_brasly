/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.FSNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Cambia permisos de archivos o directorios usando dos digitos: dueno/grupo.
 * Ejemplo: chmod 75 archivo.txt
 * 
 * @author eyden
 */
public class ChmodCommand implements Command {

    @Override
    public String getName() {
        return "chmod";
    }

    @Override
    public String getDescription() {
        return "Cambia permisos de archivos o directorios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length < 2) {
            System.out.println("Uso: chmod <permisos> <archivo|directorio> [archivo|directorio...]");
            return;
        }

        int permissions;
        try {
            permissions = PermissionSupport.parsePermissions(args[0]);
        } catch (IllegalArgumentException exception) {
            System.out.println("chmod: " + exception.getMessage());
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
            if (!PermissionSupport.canModifyMetadata(session, node)) {
                PermissionSupport.deny(getName(), "cambiar permisos", path);
                continue;
            }

            PermissionSupport.setPermissions(node, permissions);
            changedAny = true;
            System.out.println("Permisos actualizados: " + path + " -> "
                    + PermissionSupport.formatPermissions(permissions));
        }

        if (changedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("chmod: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }
}
