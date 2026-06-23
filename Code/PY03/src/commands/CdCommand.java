/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import java.util.Scanner;

/**
 *
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

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 1) {
            System.out.println("Uso: cd [directorio]");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String requestedPath = args.length == 0 ? "/" : args[0];
        String targetPath = directoryTree.normalizePath(session.getCurrentPath(), requestedPath);

        if (directoryTree.find(targetPath).isEmpty()) {
            System.out.println("cd: no existe el directorio: " + targetPath);
            return;
        }

        session.setCurrentPath(targetPath);
    }
}
