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
public class ExitCommand implements Command {
    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "Finaliza la terminal.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        session.stop();
        System.out.println("Sesion finalizada.");
    }
}
