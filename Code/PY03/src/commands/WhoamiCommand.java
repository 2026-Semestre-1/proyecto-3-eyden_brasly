/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.util.Scanner;
import security.UserService.UserAccount;

/**
 *
 * @author eyden
 */
public class WhoamiCommand implements Command {
    @Override
    public String getName() {
        return "whoami";
    }

    @Override
    public String getDescription() {
        return "Muestra el usuario activo.";
    }
    /**
     * Ejecuta el comando whoami, mostrando el nombre de usuario y el nombre completo del usuario activo en la sesión de terminal.
     * @param args Los argumentos del comando (no se utilizan en este comando).
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 0) {
            System.out.println("Uso: whoami");
            return;
        }

        UserAccount activeUser = session.getActiveUser();
        System.out.println(activeUser.getUsername() + " - " + activeUser.getFullName());
    }
}
