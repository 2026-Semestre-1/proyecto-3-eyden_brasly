/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Elimina archivos, enlaces y directorios vacios.
 * Con -R elimina directorios de forma recursiva.
 * 
 * @author Brasly
 */
public class RmCommand implements Command {

    @Override
    public String getName() {
        return "rm";
    }

    @Override
    public String getDescription() {
        return "Elimina archivos o directorios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length == 0) {
            System.out.println("Uso: rm [-R] <archivo|directorio> [archivo|directorio...]");
            return;
        }

        boolean recursive = false;
        List<String> targets = new ArrayList<>();

        for (String arg : args) {
            if ("-R".equals(arg)) {
                recursive = true;
            } else {
                targets.add(arg);
            }
        }

        if (targets.isEmpty()) {
            System.out.println("Uso: rm [-R] <archivo|directorio> [archivo|directorio...]");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        boolean removedAny = false;

        for (String target : targets) {
            try {
                List<FileNode> removedFiles = directoryTree.removeNode(
                        session.getCurrentPath(),
                        target,
                        recursive
                );

                for (FileNode file : removedFiles) {
                    session.getFileSystem().getBlockManager().freeBlocks(file.getFCB().getBlocks());
                }

                removedAny = true;
                System.out.println("Eliminado: " + directoryTree.normalizePath(session.getCurrentPath(), target));

            } catch (IllegalArgumentException exception) {
                System.out.println("rm: " + exception.getMessage());
            }
        }

        if (removedAny) {
            try {
                session.getFileSystem().saveDirectories();
            } catch (IOException exception) {
                System.out.println("rm: no se pudo guardar la tabla de directorios: " + exception.getMessage());
            }
        }
    }
}