/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

import commands.CommandRegistry;
import constants.SystemConstants;
import filesystem.FileSystem;
import filesystem.FileSystemMounter;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String diskName = args.length > 0 ? args[0] : SystemConstants.VIRTUAL_DISK_FILE_NAME;
        TerminalSession session = new TerminalSession(diskName);
        CommandRegistry registry = new CommandRegistry();
        FileSystemMounter mounter = new FileSystemMounter();

        initializeSession(session, mounter, diskName);

        try (Scanner scanner = new Scanner(System.in)) {
            new Shell(session, registry, scanner).start();
        }
    }

    private static void initializeSession(TerminalSession session, FileSystemMounter mounter, String diskName) {
        if (!mounter.existsDisk(diskName)) {
            System.out.println("No se encontro un disco virtual formateado.");
            System.out.println("Debe ejecutar el comando format para crear el File System.");
            System.out.println();
            return;
        }

        System.out.println("Disco encontrado.");
        System.out.println("Verificando MBR...");

        if (!mounter.isValidFileSystem(diskName)) {
            System.out.println("El disco existe, pero no contiene un File System valido.");
            System.out.println("Ejecute format para crear un File System nuevo.");
            System.out.println();
            return;
        }

        try {
            System.out.println("File System valido.");
            System.out.println("Montando " + SystemConstants.FILE_SYSTEM_NAME + "...");
            FileSystem fileSystem = mounter.mount(diskName);
            session.mount(fileSystem);
            System.out.println("Sistema montado correctamente.");
            System.out.println();
        } catch (IOException exception) {
            System.out.println("No se pudo montar el File System: " + exception.getMessage());
            System.out.println();
        }
    }
}
