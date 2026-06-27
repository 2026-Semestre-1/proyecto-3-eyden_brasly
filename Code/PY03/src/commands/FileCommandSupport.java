package commands;

import app.TerminalSession;
import filesystem.FileSystem;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FSNode;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Optional;

final class FileCommandSupport {
    private FileCommandSupport() {
    }

    static FileNode findFile(TerminalSession session, String requestedPath, String commandName) {
        return session.getFileSystem()
                .getDirectoryTree()
                .findFileResolvingLink(session.getCurrentPath(), requestedPath)
                .orElseGet(() -> {
                    System.out.println(commandName + ": no existe el archivo: " + requestedPath);
                    return null;
                });
    }

    static FSNode findNode(TerminalSession session, String requestedPath, String commandName) {
        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String fullPath = directoryTree.normalizePath(session.getCurrentPath(), requestedPath);
        Optional<FSNode> node = directoryTree.findNode(session.getCurrentPath(), requestedPath);

        if (node.isEmpty()) {
            System.out.println(commandName + ": no existe el recurso: " + fullPath);
            return null;
        }

        return node.get();
    }

    static DirectoryNode findDirectory(TerminalSession session, String requestedPath, String commandName) {
        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        String fullPath = directoryTree.normalizePath(session.getCurrentPath(), requestedPath);
        Optional<DirectoryNode> directory = directoryTree.find(fullPath);

        if (directory.isEmpty()) {
            System.out.println(commandName + ": no existe el directorio: " + fullPath);
            return null;
        }

        return directory.get();
    }

    static boolean openFile(
            TerminalSession session,
            FileNode file,
            String mode,
            String commandName
    ) {
        FileSystem fileSystem = session.getFileSystem();
        PermissionSupport.Access access = "LECTURA".equalsIgnoreCase(mode)
                ? PermissionSupport.Access.READ
                : PermissionSupport.Access.WRITE;

        if (!PermissionSupport.hasAccess(session, file, access)) {
            return PermissionSupport.deny(commandName, mode.toLowerCase(), file.getFullPath());
        }

        try {
            boolean opened = fileSystem.openFile(
                    file,
                    session.getActiveUser().getUsername(),
                    mode,
                    session
            );

            if (!opened) {
                System.out.println(commandName + ": el archivo ya esta abierto: " + file.getFullPath());
            }

            return opened;
        } catch (IOException exception) {
            System.out.println(commandName + ": no se pudo abrir el archivo: " + exception.getMessage());
            return false;
        }
    }

    static String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    static String fileName(String path) {
        int separator = path.lastIndexOf('/');
        return separator == -1 ? path : path.substring(separator + 1);
    }

    static void closeFile(TerminalSession session, FileNode file, String commandName) {
        try {
            session.getFileSystem().closeFile(file, session);
        } catch (IOException exception) {
            System.out.println(commandName + ": no se pudo cerrar el archivo: " + exception.getMessage());
        }
    }
}
