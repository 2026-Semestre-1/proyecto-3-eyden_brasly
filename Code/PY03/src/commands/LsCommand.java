package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.util.Map;
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
        if (!PermissionSupport.hasAll(
                session,
                directory,
                PermissionSupport.Access.READ,
                PermissionSupport.Access.EXECUTE
        )) {
            PermissionSupport.deny(getName(), "listar", targetPath);
            return;
        }

        if (recursive) {
            listRecursive(session, directory);
        } else {
            listDirectory(directory);
        }
    }

    private void listDirectory(DirectoryNode directory) {
        boolean emptyDirectories = directory.getDirectories().isEmpty();
        boolean emptyFiles = directory.getFiles().isEmpty();
        boolean emptyLinks = directory.getLinks().isEmpty();

        if (emptyDirectories && emptyFiles && emptyLinks) {
            System.out.println("(vacio)");
            return;
        }

        for (DirectoryNode child : directory.getDirectories()) {
            System.out.println("[DIR] " + child.getName());
        }

        for (FileNode file : directory.getFiles()) {
            System.out.println("[FILE] " + file.getName());
        }

        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            System.out.println("[LINK] " + link.getKey() + " -> " + link.getValue());
        }
    }

    private void listRecursive(TerminalSession session, DirectoryNode directory) {
        System.out.println(directory.getPath() + ":");
        listDirectory(directory);

        for (DirectoryNode child : directory.getDirectories()) {
            if (!PermissionSupport.hasAll(
                    session,
                    child,
                    PermissionSupport.Access.READ,
                    PermissionSupport.Access.EXECUTE
            )) {
                System.out.println();
                PermissionSupport.deny(getName(), "listar", child.getPath());
                continue;
            }
            System.out.println();
            listRecursive(session, child);
        }
    }
}