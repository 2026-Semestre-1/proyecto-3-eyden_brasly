/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 *
 * @author eyden
 */
import app.TerminalSession;
import filesystem.FileContentService;
import filesystem.FileSystem;
import filesystem.nodes.FileNode;
import java.io.IOException;

public class FileService {
    private final FileSystem fileSystem;
    private final FileContentService contentService;

    public FileService(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.contentService = fileSystem.getFileContentService();
    }

    public String readContent(TerminalSession session, FileNode file) throws IOException {
        if (!commands.PermissionSupport.hasAccess(session, file, commands.PermissionSupport.Access.READ)) {
            throw new SecurityException("permiso denegado para leer: " + file.getFullPath());
        }
        return contentService.readContent(file);
    }

    public void writeContent(TerminalSession session, FileNode file, String content) throws IOException {
        if (!commands.PermissionSupport.hasAccess(session, file, commands.PermissionSupport.Access.WRITE)) {
            throw new SecurityException("permiso denegado para escribir: " + file.getFullPath());
        }
        contentService.writeContent(file, content);
    }

    public void clearContent(TerminalSession session, FileNode file) throws IOException {
        writeContent(session, file, "");
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }
}