/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.util.Scanner;

/**
 * Comando que muestra la ruta absoluta del directorio actual en la sesión de terminal.
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
    /**
     * Ejecuta el comando pwd, mostrando la ruta absoluta del directorio actual en la sesión de terminal.
     * @param args Los argumentos del comando (no se utilizan en este comando).
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 0) {
            System.out.println("Uso: pwd");
            return;
        }

        System.out.println(session.getCurrentPath());
    }
}
