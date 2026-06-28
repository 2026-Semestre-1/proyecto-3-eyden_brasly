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
 * Comando que permite crear un nuevo usuario en el sistema.
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
    /**
     * Ejecuta el comando useradd, permitiendo al usuario crear un nuevo usuario en el sistema.
     * @param args Los argumentos del comando, donde args[0] es opcionalmente el nombre del usuario a crear.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario.
     */
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
    /**
     * Crea el directorio home del usuario especificado si no existe.
     * @param session La sesión de terminal actual.
     * @param username El nombre del usuario cuyo directorio home se desea crear.
     */
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
