/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class PasswdCommand implements Command {
    @Override
    public String getName() {
        return "passwd";
    }

    @Override
    public String getDescription() {
        return "Cambia la contrasena de un usuario.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 1) {
            System.out.println("Uso: passwd [usuario]");
            return;
        }

        String targetUsername = args.length == 1 ? args[0] : session.getActiveUser().getUsername();
        boolean changingOwnPassword = targetUsername.equalsIgnoreCase(session.getActiveUser().getUsername());

        if (!changingOwnPassword && !session.isPrivileged()) {
            System.out.println("passwd: permiso denegado. Solo root puede cambiar la contrasena de otro usuario.");
            return;
        }

        if (!session.getUserService().exists(targetUsername)) {
            System.out.println("passwd: el usuario '" + targetUsername + "' no existe.");
            return;
        }

        String password = CommandIO.readPassword(scanner, "Nueva contrasena: ");
        String confirmation = CommandIO.readPassword(scanner, "Confirmar nueva contrasena: ");

        if (!password.equals(confirmation)) {
            System.out.println("passwd: las contrasenas no coinciden.");
            return;
        }

        try {
            boolean updated = session.getUserService().changePassword(targetUsername, password);
            if (updated) {
                System.out.println("Contrasena actualizada correctamente.");
            } else {
                System.out.println("passwd: no se pudo actualizar la contrasena.");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("passwd: " + exception.getMessage());
        }
    }
}
