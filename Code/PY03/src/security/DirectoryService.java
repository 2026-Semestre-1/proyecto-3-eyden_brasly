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
import filesystem.FileSystem;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.LinkNode;
import java.io.IOException;
import java.util.Optional;

public class DirectoryService {
    private final FileSystem fileSystem;

    public DirectoryService(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public DirectoryTree getDirectoryTree() {
        return fileSystem.getDirectoryTree();
    }

    public Optional<DirectoryNode> find(String path) {
        return fileSystem.getDirectoryTree().find(path);
    }

    public DirectoryNode createDirectory(TerminalSession session, String parentPath, String name,
            String owner, String group) {
        return fileSystem.getDirectoryTree().createDirectory(parentPath, name, owner, group);
    }

    public DirectoryNode createDirectory(TerminalSession session, String parentPath, String name,
            String owner, String group, int permissions) {
        return fileSystem.getDirectoryTree().createDirectory(parentPath, name, owner, group, permissions);
    }

    public void removeDirectory(String currentPath, String path, boolean recursive) {
        fileSystem.getDirectoryTree().removeDirectory(currentPath, path, recursive);
    }

    public void save() throws IOException {
        fileSystem.saveDirectories();
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }
}