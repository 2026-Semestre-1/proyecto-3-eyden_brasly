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
            String normalizedUsername = username.trim().toLowerCase();
            boolean created = session.getUserService().addUser(username, fullName, password);
            if (created) {
                createHomeDirectory(session, normalizedUsername);
                session.getFileSystem().saveUsers(session.getUserService());
                System.out.println("Usuario '" + normalizedUsername + "' creado correctamente.");
            } else {
                System.out.println("useradd: el usuario '" + normalizedUsername + "' ya existe.");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("useradd: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("useradd: usuario creado en sesion, pero no se pudo guardar: " + exception.getMessage());
        }
    }

    private void createHomeDirectory(TerminalSession session, String username) {
        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String homePath = "/user/" + username;

        if (directoryTree.find(homePath).isPresent()) {
            return;
        }

        try {
            directoryTree.createDirectory(
                    "/user",
                    username,
                    username,
                    session.getUserService().findByUsername(username).orElseThrow().getPrimaryGroup()
            );
            session.getFileSystem().saveDirectories();
            System.out.println("Directorio home creado: " + homePath);
        } catch (IOException exception) {
            System.out.println("useradd: usuario creado, pero no se pudo guardar su home: " + exception.getMessage());
        }
    }
}
