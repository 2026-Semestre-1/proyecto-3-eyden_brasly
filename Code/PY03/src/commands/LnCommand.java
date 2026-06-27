/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Crea un enlace hacia un archivo existente.
 * 
 * @author brasly
 */
public class LnCommand implements Command {

    @Override
    public String getName() {
        return "ln";
    }

    @Override
    public String getDescription() {
        return "Crea un enlace hacia un archivo existente.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 2) {
            System.out.println("Uso: ln <archivoOriginal> <nombreEnlace>");
            return;
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();

        try {
            FileNode original = FileCommandSupport.findFile(session, args[0], getName());
            if (original == null) {
                return;
            }
            if (!PermissionSupport.hasAccess(session, original, PermissionSupport.Access.READ)) {
                PermissionSupport.deny(getName(), "leer", original.getFullPath());
                return;
            }

            String linkFullPath = directoryTree.normalizePath(session.getCurrentPath(), args[1]);
            String parentPath = FileCommandSupport.parentPath(linkFullPath);
            var parent = directoryTree.find(parentPath)
                    .orElseThrow(() -> new IllegalArgumentException("el directorio destino no existe: " + parentPath));
            if (!PermissionSupport.hasAll(
                    session,
                    parent,
                    PermissionSupport.Access.WRITE,
                    PermissionSupport.Access.EXECUTE
            )) {
                PermissionSupport.deny(getName(), "crear en", parentPath);
                return;
            }

            String linkPath = directoryTree.createLink(
                    session.getCurrentPath(),
                    args[0],
                    args[1]
            );

            session.getFileSystem().saveDirectories();

            System.out.println("Enlace creado: " + linkPath);

        } catch (IllegalArgumentException exception) {
            System.out.println("ln: " + exception.getMessage());
        } catch (IOException exception) {
            System.out.println("ln: no se pudo guardar la tabla de directorios: " + exception.getMessage());
        }
    }
}