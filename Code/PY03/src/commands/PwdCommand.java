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
public class PwdCommand implements Command {
    @Override
    public String getName() {
        return "pwd";
    }

    @Override
    public String getDescription() {
        return "Muestra la ruta absoluta actual.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 0) {
            System.out.println("Uso: pwd");
            return;
        }

        System.out.println(session.getCurrentPath());
    }
}
