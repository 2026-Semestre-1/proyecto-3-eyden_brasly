/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 *
 * @author eyden
 */
import filesystem.nodes.DirectoryTree;

public class PathService {
    private final DirectoryTree directoryTree;

    public PathService(DirectoryTree directoryTree) {
        this.directoryTree = directoryTree;
    }

    public String normalize(String currentPath, String requestedPath) {
        return directoryTree.normalizePath(currentPath, requestedPath);
    }

    public String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    public String fileName(String path) {
        int separator = path.lastIndexOf('/');
        return separator == -1 ? path : path.substring(separator + 1);
    }

    public static String joinPath(String parentPath, String name) {
        if ("/".equals(parentPath)) {
            return "/" + name;
        }
        return parentPath + "/" + name;
    }
}