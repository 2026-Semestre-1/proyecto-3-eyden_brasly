/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import constants.SystemConstants;
import java.util.Scanner;

/**
 *
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
        System.out.println("Sesion cambiada a '" + targetUsername.trim().toLowerCase() + "'.");
    }
}
