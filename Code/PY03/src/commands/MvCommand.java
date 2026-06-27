/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FSNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Permite mover o renombrar archivos, directorios y enlaces.
 * 
 * @author Brasly
 */
public class MvCommand implements Command {

    @Override
    public String getName() {
        return "mv";
    }

    @Override
    public String getDescription() {
        return "Mueve o renombra archivos y directorios.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 2) {
            System.out.println("Uso: mv <origen> <destino>");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();

        try {
            String sourcePath = directoryTree.normalizePath(session.getCurrentPath(), args[0]);
            String sourceParentPath = FileCommandSupport.parentPath(sourcePath);
            FSNode source = FileCommandSupport.findNode(session, args[0], getName());
            if (source == null) {
                return;
            }
            var sourceParent = directoryTree.find(sourceParentPath)
                    .orElseThrow(() -> new IllegalArgumentException("la ruta origen no existe: " + sourceParentPath));
            if (!PermissionSupport.hasAll(
                    session,
                    sourceParent,
                    PermissionSupport.Access.WRITE,
                    PermissionSupport.Access.EXECUTE
            ) || !PermissionSupport.hasAccess(session, source, PermissionSupport.Access.WRITE)) {
                PermissionSupport.deny(getName(), "mover", sourcePath);
                return;
            }

            String destinationPath = directoryTree.normalizePath(session.getCurrentPath(), args[1]);
            var destinationAsDirectory = directoryTree.find(destinationPath);
            String destinationParentPath = destinationAsDirectory.isPresent()
                    ? destinationPath
                    : FileCommandSupport.parentPath(destinationPath);
            var destinationParent = directoryTree.find(destinationParentPath)
                    .orElseThrow(() -> new IllegalArgumentException("el directorio destino no existe: " + destinationParentPath));
            if (!PermissionSupport.hasAll(
                    session,
                    destinationParent,
                    PermissionSupport.Access.WRITE,
                    PermissionSupport.Access.EXECUTE
            )) {
                PermissionSupport.deny(getName(), "mover hacia", destinationParentPath);
                return;
            }

            directoryTree.moveNode(
                    session.getCurrentPath(),
                    args[0],
                    args[1]
            );

            session.getFileSystem().saveDirectories();

            System.out.println("Movido: "
                    + directoryTree.normalizePath(session.getCurrentPath(), args[0])
                    + " -> "
                    + directoryTree.normalizePath(session.getCurrentPath(), args[1]));

        } catch (IllegalArgumentException exception) {
            System.out.println("mv: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("mv: no se pudo guardar la tabla de directorios: " + exception.getMessage());
        }
    }
}