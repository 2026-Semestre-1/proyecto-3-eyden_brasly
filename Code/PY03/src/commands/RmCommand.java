/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import util.WildcardMatcher;

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
        boolean freedBlocks = false;

        for (String target : targets) {
            List<String> expandedTargets = expandTargets(session, directoryTree, target);
            if (expandedTargets.isEmpty()) {
                System.out.println("rm: no coincide ningun recurso: " + target);
                continue;
            }

            for (String expandedTarget : expandedTargets) {
                try {
                    if (!canRemove(session, directoryTree, expandedTarget, recursive)) {
                        continue;
                    }

                    List<FileNode> removedFiles = directoryTree.removeNode(
                            "/",
                            expandedTarget,
                            recursive
                    );

                    for (FileNode file : removedFiles) {
                        session.getFileSystem().getBlockManager().freeBlocks(file.getFCB().getBlocks());
                        freedBlocks = true;
                    }

                    removedAny = true;
                    System.out.println("Eliminado: " + expandedTarget);

                } catch (IllegalArgumentException exception) {
                    System.out.println("rm: " + exception.getMessage());
                }
            }
        }

        if (removedAny) {
            try {
                session.getFileSystem().saveDirectories();
                if (freedBlocks) {
                    session.getFileSystem().saveBitmap();
                }
            } catch (IOException exception) {
                System.out.println("rm: no se pudieron guardar los cambios: " + exception.getMessage());
            }
        }
    }

    private List<String> expandTargets(TerminalSession session, DirectoryTree directoryTree, String target) {
        if (!WildcardMatcher.hasWildcard(target)) {
            return List.of(directoryTree.normalizePath(session.getCurrentPath(), target));
        }

        String fullPatternPath = directoryTree.normalizePath(session.getCurrentPath(), target);
        String parentPath = FileCommandSupport.parentPath(fullPatternPath);
        String namePattern = FileCommandSupport.fileName(fullPatternPath);

        if (WildcardMatcher.hasWildcard(parentPath)) {
            System.out.println("rm: patrones en directorios intermedios no estan soportados: " + target);
            return List.of();
        }

        DirectoryNode parent = directoryTree.find(parentPath).orElse(null);
        if (parent == null) {
            return List.of();
        }
        if (!PermissionSupport.hasAll(
                session,
                parent,
                PermissionSupport.Access.READ,
                PermissionSupport.Access.EXECUTE
        )) {
            PermissionSupport.deny(getName(), "expandir patron en", parentPath);
            return List.of();
        }

        Set<String> matches = new LinkedHashSet<>();

        for (DirectoryNode directory : parent.getDirectories()) {
            addIfMatches(matches, parentPath, directory.getName(), namePattern);
        }
        for (FileNode file : parent.getFiles()) {
            addIfMatches(matches, parentPath, file.getName(), namePattern);
        }
        for (Map.Entry<String, String> link : parent.getLinks().entrySet()) {
            addIfMatches(matches, parentPath, link.getKey(), namePattern);
        }

        return new ArrayList<>(matches);
    }

    private void addIfMatches(Set<String> matches, String parentPath, String name, String pattern) {
        if (WildcardMatcher.matches(pattern, name)) {
            matches.add(joinPath(parentPath, name));
        }
    }

    private boolean canRemove(
            TerminalSession session,
            DirectoryTree directoryTree,
            String fullPath,
            boolean recursive
    ) {
        if ("/".equals(fullPath)) {
            System.out.println("rm: no se puede eliminar el directorio raiz.");
            return false;
        }

        String parentPath = FileCommandSupport.parentPath(fullPath);
        String name = FileCommandSupport.fileName(fullPath);
        DirectoryNode parent = directoryTree.find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("la ruta no existe: " + parentPath));

        if (!PermissionSupport.hasAll(
                session,
                parent,
                PermissionSupport.Access.WRITE,
                PermissionSupport.Access.EXECUTE
        )) {
            return PermissionSupport.deny(getName(), "eliminar en", parentPath);
        }

        if (parent.hasLink(name)) {
            return true;
        }

        if (parent.hasFile(name)) {
            FileNode file = parent.getFile(name);
            if (!PermissionSupport.hasAccess(session, file, PermissionSupport.Access.WRITE)) {
                return PermissionSupport.deny(getName(), "eliminar", file.getFullPath());
            }
            return true;
        }

        if (parent.hasDirectory(name)) {
            DirectoryNode directory = parent.getDirectory(name);
            if (!PermissionSupport.hasAccess(session, directory, PermissionSupport.Access.WRITE)) {
                return PermissionSupport.deny(getName(), "eliminar", directory.getPath());
            }
            if (recursive && !canRemoveRecursive(session, directory)) {
                return false;
            }
            return true;
        }

        throw new IllegalArgumentException("no existe el archivo o directorio: " + fullPath);
    }

    private boolean canRemoveRecursive(TerminalSession session, DirectoryNode directory) {
        if (!PermissionSupport.hasAll(
                session,
                directory,
                PermissionSupport.Access.WRITE,
                PermissionSupport.Access.EXECUTE
        )) {
            return PermissionSupport.deny(getName(), "eliminar recursivamente", directory.getPath());
        }

        for (FileNode file : directory.getFiles()) {
            if (!PermissionSupport.hasAccess(session, file, PermissionSupport.Access.WRITE)) {
                return PermissionSupport.deny(getName(), "eliminar", file.getFullPath());
            }
        }

        for (DirectoryNode child : directory.getDirectories()) {
            if (!canRemoveRecursive(session, child)) {
                return false;
            }
        }

        return true;
    }

    private String joinPath(String parentPath, String name) {
        return "/".equals(parentPath) ? "/" + name : parentPath + "/" + name;
    }
}
