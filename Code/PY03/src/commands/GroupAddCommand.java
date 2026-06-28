/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.io.IOException;
import java.util.Scanner;

/**
 * Comando que crea un grupo nuevo en el sistema de archivos.
 * @author eyden
 */
public class GroupAddCommand implements Command {
    @Override
    public String getName() {
        return "groupadd";
    }

    @Override
    public String getDescription() {
        return "Crea un grupo nuevo.";
    }
    /**
     * Ejecuta el comando groupadd, creando un grupo nuevo en el sistema de archivos.
     * @param args Los argumentos del comando, donde args[0] puede ser el nombre del grupo.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario.
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (!session.isPrivileged()) {
            System.out.println("groupadd: permiso denegado. Solo root puede crear grupos.");
            return;
        }

        if (args.length > 1) {
            System.out.println("Uso: groupadd [grupo]");
            return;
        }

        String groupName = args.length == 1 ? args[0] : CommandIO.prompt(scanner, "Nombre del grupo: ");

        try {
            boolean created = session.getGroupService().addGroup(groupName);
            if (created) {
                session.getFileSystem().saveGroups(session.getGroupService());
                System.out.println("Grupo '" + groupName.trim().toLowerCase() + "' creado correctamente.");
            } else {
                System.out.println("groupadd: el grupo '" + groupName.trim().toLowerCase() + "' ya existe.");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("groupadd: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("groupadd: grupo creado en sesion, pero no se pudo guardar: " + exception.getMessage());
        }
    }
}
