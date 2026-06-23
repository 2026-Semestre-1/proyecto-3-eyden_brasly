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
public class UserAddCommand implements Command {
    @Override
    public String getName() {
        return "useradd";
    }

    @Override
    public String getDescription() {
        return "Crea un usuario nuevo.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (!session.isPrivileged()) {
            System.out.println("useradd: permiso denegado. Solo root puede crear usuarios.");
            return;
        }

        if (args.length > 1) {
            System.out.println("Uso: useradd [usuario]");
            return;
        }

        String username = args.length == 1 ? args[0] : CommandIO.prompt(scanner, "Nombre de usuario: ");
        String fullName = CommandIO.prompt(scanner, "Nombre completo: ");
        String password = CommandIO.readPassword(scanner, "Contrasena: ");
        String confirmation = CommandIO.readPassword(scanner, "Confirmar contrasena: ");

        if (!password.equals(confirmation)) {
            System.out.println("useradd: las contrasenas no coinciden.");
            return;
        }

        try {
            boolean created = session.getUserService().addUser(username, fullName, password);
            if (created) {
                System.out.println("Usuario '" + username.trim().toLowerCase() + "' creado correctamente.");
            } else {
                System.out.println("useradd: el usuario '" + username.trim().toLowerCase() + "' ya existe.");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("useradd: " + exception.getMessage());
        }
    }
}
