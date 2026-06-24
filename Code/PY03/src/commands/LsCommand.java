package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.util.Scanner;

/**
 * Comando que permite mostrar el contenido de un directorio 
 * @author eyden
 */
public class LsCommand implements Command {

    @Override
    public String getName() {
        return "ls";
    }

    @Override
    public String getDescription() {
        return "Lista el contenido del directorio actual o indicado.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        boolean recursive = false;
        String requestedPath = session.getCurrentPath();

        for (String arg : args) {
            if ("-R".equals(arg)) {
                recursive = true;
            } else if (requestedPath.equals(session.getCurrentPath())) {
                requestedPath = arg;
            } else {
                System.out.println("Uso: ls [-R] [directorio]");
                return;
            }
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String targetPath = directoryTree.normalizePath(session.getCurrentPath(), requestedPath);
        DirectoryNode directory = directoryTree.find(targetPath).orElse(null);

        if (directory == null) {
            System.out.println("ls: no existe el directorio: " + targetPath);
            return;
        }

        if (recursive) {
            listRecursive(directory);
        } else {
            listDirectory(directory);
        }
    }

    private void listDirectory(DirectoryNode directory) {
        boolean emptyDirectories = directory.getDirectories().isEmpty();
        boolean emptyFiles = directory.getFiles().isEmpty();

        if (emptyDirectories && emptyFiles) {
            System.out.println("(vacio)");
            return;
        }

        for (DirectoryNode child : directory.getDirectories()) {
            System.out.println("[DIR] " + child.getName());
        }

        for (FileNode file : directory.getFiles()) {
            System.out.println("[FILE] " + file.getName());
        }
    }

    private void listRecursive(DirectoryNode directory) {
        System.out.println(directory.getPath() + ":");
        listDirectory(directory);

        for (DirectoryNode child : directory.getDirectories()) {
            System.out.println();
            listRecursive(child);
        }
    }
}