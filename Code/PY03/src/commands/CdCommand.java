/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import java.util.Scanner;

/**
 * Comando que permite cambiar el directorio actual en la sesión de terminal.
 * @author eyden
 */
public class CdCommand implements Command {
    @Override
    public String getName() {
        return "cd";
    }

    @Override
    public String getDescription() {
        return "Cambia el directorio actual.";
    }
    /**
     * Ejecuta el comando cd, cambiando el directorio actual de la sesión de terminal.
     * @param args Los argumentos del comando, donde args[0] es el directorio al que se desea cambiar.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 1) {
            System.out.println("Uso: cd [directorio]");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String requestedPath = args.length == 0 ? "/" : args[0];
        String targetPath = directoryTree.normalizePath(session.getCurrentPath(), requestedPath);

        var directory = directoryTree.find(targetPath);
        if (directory.isEmpty()) {
            System.out.println("cd: no existe el directorio: " + targetPath);
            return;
        }
        if (!PermissionSupport.hasAccess(session, directory.get(), PermissionSupport.Access.EXECUTE)) {
            PermissionSupport.deny(getName(), "entrar", targetPath);
            return;
        }

        session.setCurrentPath(targetPath);
    }
}
