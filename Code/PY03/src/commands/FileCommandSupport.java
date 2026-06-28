package commands;

import app.TerminalSession;
import filesystem.FileSystem;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FSNode;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Optional;

/**
 * Clase de soporte para comandos relacionados con archivos y directorios.
 * Proporciona métodos auxiliares para encontrar archivos, directorios y nodos en el sistema de archivos,
 * así como para abrir y cerrar archivos de manera segura.
 *
 * @author brasly
 */
final class FileCommandSupport {
    private FileCommandSupport() {
    }
    /**
     * Busca un archivo en el sistema de archivos de la sesión de terminal.
     * @param session La sesión de terminal actual.
     * @param requestedPath La ruta del archivo solicitado.
     * @param commandName El nombre del comando que realiza la búsqueda (para mensajes de error).
     * @return El nodo de archivo encontrado, o null si no se encuentra.
     */
    static FileNode findFile(TerminalSession session, String requestedPath, String commandName) {
        return session.getFileSystem()
                .getDirectoryTree()
                .findFileResolvingLink(session.getCurrentPath(), requestedPath)
                .orElseGet(() -> {
                    System.out.println(commandName + ": no existe el archivo: " + requestedPath);
                    return null;
                });
    }

    /**
     * Busca un nodo en el sistema de archivos de la sesión de terminal.
     * @param session La sesión de terminal actual.
     * @param requestedPath La ruta del nodo solicitado.
     * @param commandName El nombre del comando que realiza la búsqueda (para mensajes de error).
     * @return El nodo encontrado, o null si no se encuentra.
     */
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
    /**
     * Busca un directorio en el sistema de archivos de la sesión de terminal.
     * @param session La sesión de terminal actual.
     * @param requestedPath La ruta del directorio solicitado.
     * @param commandName El nombre del comando que realiza la búsqueda (para mensajes de error).
     * @return El nodo de directorio encontrado, o null si no se encuentra.
     */
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
    /**
     * Abre un archivo en el sistema de archivos de la sesión de terminal, verificando los permisos de acceso.
     * @param session La sesión de terminal actual.
     * @param file El nodo de archivo a abrir.
     * @param mode El modo de apertura del archivo ("LECTURA" o "ESCRITURA").
     * @param commandName El nombre del comando que realiza la apertura (para mensajes de error).
     * @return true si el archivo se abrió correctamente, false en caso contrario.
     */
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
    /**
     * Obtiene el directorio padre de una ruta dada.
     * @param path La ruta de la cual se desea obtener el directorio padre.
     * @return La ruta del directorio padre, o "/" si no hay un directorio padre.
     */
    static String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }
    /**
     * Obtiene el nombre del archivo o directorio de una ruta dada.
     * @param path La ruta de la cual se desea obtener el nombre del archivo o directorio.
     * @return El nombre del archivo o directorio.
     */
    static String fileName(String path) {
        int separator = path.lastIndexOf('/');
        return separator == -1 ? path : path.substring(separator + 1);
    }
    /**
     * Cierra un archivo en el sistema de archivos de la sesión de terminal.
     * @param session La sesión de terminal actual.
     * @param file El nodo de archivo a cerrar.
     * @param commandName El nombre del comando que realiza el cierre (para mensajes de error).
     */
    static void closeFile(TerminalSession session, FileNode file, String commandName) {
        try {
            session.getFileSystem().closeFile(file, session);
        } catch (IOException exception) {
            System.out.println(commandName + ": no se pudo cerrar el archivo: " + exception.getMessage());
        }
    }
}
