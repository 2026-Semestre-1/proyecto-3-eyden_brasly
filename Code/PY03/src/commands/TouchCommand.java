/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Crea uno o varios archivos vacios dentro del File System.
 * 
 * @author brasly
 */
public class TouchCommand implements Command {

    private static final int permissions = 77;

    @Override
    public String getName() {
        return "touch";
    }

    @Override
    public String getDescription() {
        return "Crea archivos vacios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length == 0) {
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        boolean createdAny = false;

        for (String argument : args) {
            try {
                FileNode file = directoryTree.createFile(
                        session.getCurrentPath(),
                        argument,
                        session.getActiveUser().getUsername(),
                        session.getActiveUser().getPrimaryGroup(),
                        permissions
                );

                createdAny = true;

            } catch (IllegalArgumentException exception) {
            }
        }

        if (createdAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
            }
        }
    }
}
