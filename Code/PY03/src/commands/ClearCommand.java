/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.util.Scanner;

/**
 * Comando que limpia la pantalla de la terminal.
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
    /**
     * Ejecuta el comando clear, limpiando la pantalla de la terminal.
     * @param args Los argumentos del comando (no se utilizan en este comando).
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
