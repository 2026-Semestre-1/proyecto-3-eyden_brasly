/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.FCB;
import filesystem.nodes.FileNode;
import java.util.Scanner;
import util.DateUtil;

/**
 * Muestra los atributos estructurales del FCB de un archivo.
 *
 * @author eyden
 */
public class ViewFCBCommand implements Command {
    @Override
    public String getName() {
        return "viewFCB";
    }

    @Override
    public String getDescription() {
        return "Muestra la informacion del FCB de un archivo.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 1) {
            System.out.println("Uso: viewFCB <archivo>");
            return;
        }

        FileNode file = FileCommandSupport.findFile(session, args[0], getName());
        if (file == null) {
            return;
        }
        if (!PermissionSupport.hasAccess(session, file, PermissionSupport.Access.READ)) {
            PermissionSupport.deny(getName(), "ver FCB de", file.getFullPath());
            return;
        }

        FCB fcb = file.getFCB();
        System.out.println("Nombre: " + fcb.getName());
        System.out.println("Ruta: " + fcb.getFullPath());
        System.out.println("Dueno: " + fcb.getOwner());
        System.out.println("Grupo: " + fcb.getGroup());
        System.out.println("Permisos: " + String.format("%02d", fcb.getPermissions()));
        System.out.println("Fecha de creacion: " + DateUtil.formatMillis(fcb.getCreationDate()));
        System.out.println("Estado: " + (fcb.isOpen() ? "abierto" : "cerrado"));
        System.out.println("Tamano: " + fcb.getSize() + " bytes");
        System.out.println("Bloques: " + fcb.getBlocks());
    }
}
