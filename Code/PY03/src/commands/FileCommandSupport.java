package commands;

import app.TerminalSession;
import filesystem.FileSystem;
import filesystem.nodes.FileNode;
import java.io.IOException;

final class FileCommandSupport {
    private FileCommandSupport() {
    }

    static FileNode findFile(TerminalSession session, String requestedPath, String commandName) {
        return session.getFileSystem()
                .getDirectoryTree()
                .findFile(session.getCurrentPath(), requestedPath)
                .orElseGet(() -> {
                    System.out.println(commandName + ": no existe el archivo: " + requestedPath);
                    return null;
                });
    }

    static boolean openFile(
            TerminalSession session,
            FileNode file,
            String mode,
            String commandName
    ) {
        FileSystem fileSystem = session.getFileSystem();

        try {
            boolean opened = fileSystem.openFile(
                    file,
                    session.getActiveUser().getUsername(),
                    mode
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

    static void closeFile(TerminalSession session, FileNode file, String commandName) {
        try {
            session.getFileSystem().closeFile(file);
        } catch (IOException exception) {
            System.out.println(commandName + ": no se pudo cerrar el archivo: " + exception.getMessage());
        }
    }
}
