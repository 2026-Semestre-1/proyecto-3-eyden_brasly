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
