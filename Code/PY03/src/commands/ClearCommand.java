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
public class ClearCommand implements Command {
    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Limpia la pantalla.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
