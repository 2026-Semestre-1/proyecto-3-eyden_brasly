/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import constants.SystemConstants;
import java.util.Scanner;

/**
 * Comando que permite cambiar el usuario activo de la sesión de terminal.
 * @author eyden
 */
public class SuCommand implements Command {
    @Override
    public String getName() {
        return "su";
    }

    @Override
    public String getDescription() {
        return "Cambia el usuario activo de la sesion.";
    }
    /**
     * Ejecuta el comando su, permitiendo al usuario cambiar el usuario activo de la sesión de terminal.
     * @param args Los argumentos del comando, donde args[0] es opcionalmente el nombre del usuario al que se desea cambiar.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario.
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 1) {
            System.out.println("Uso: su [usuario]");
            return;
        }

        String targetUsername = args.length == 0 ? SystemConstants.ROOT_USERNAME : args[0];

        if (!session.getUserService().exists(targetUsername)) {
            System.out.println("su: el usuario '" + targetUsername + "' no existe.");
            return;
        }

        String password = CommandIO.readPassword(scanner, "Contrasena: ");
        if (!session.getUserService().authenticate(targetUsername, password)) {
            System.out.println("su: autenticacion fallida.");
            return;
        }

        session.switchUser(targetUsername);
        String normalizedUsername = targetUsername.trim().toLowerCase();
        moveToHomeDirectory(session, normalizedUsername);
        System.out.println("Sesion cambiada a '" + normalizedUsername + "'.");
    }
    /**
     * Mueve la sesión al directorio home del usuario especificado.
     * @param session La sesión de terminal actual.
     * @param username El nombre del usuario cuyo directorio home se desea establecer como directorio actual.
     */
    private void moveToHomeDirectory(TerminalSession session, String username) {
        String homePath = SystemConstants.ROOT_USERNAME.equals(username)
                ? SystemConstants.ROOT_HOME_PATH
                : "/user/" + username;

        if (session.getFileSystem().getDirectoryTree().find(homePath).isPresent()) {
            session.setCurrentPath(homePath);
        }
    }
}
