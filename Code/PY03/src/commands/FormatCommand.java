/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import app.SystemMode;
import filesystem.AllocationStrategy;
import filesystem.FileSystem;
import filesystem.FormatService;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class FormatCommand implements Command {
    @Override
    public String getName() {
        return "format";
    }

    @Override
    public String getDescription() {
        return "Crea y monta el File System en el disco virtual.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 0) {
            System.out.println("Uso: format");
            return;
        }

        if (!confirmOverwriteIfNeeded(session, scanner)) {
            System.out.println("format: operacion cancelada.");
            return;
        }

        int sizeMB = readDiskSize(scanner);
        AllocationStrategy strategy = readAllocationStrategy(scanner);
        String password = CommandIO.readPassword(scanner, "Contrasena para root: ");
        String confirmation = CommandIO.readPassword(scanner, "Confirmar contrasena: ");

        if (!password.equals(confirmation)) {
            System.out.println("format: las contrasenas no coinciden.");
            return;
        }

        try {
            System.out.println();
            System.out.println("Creando disco virtual...");
            FormatService formatService = new FormatService();

            System.out.println("Escribiendo MBR...");
            System.out.println("Inicializando SuperBlock...");
            System.out.println("Inicializando Bitmap...");
            System.out.println("Creando usuario root...");
            System.out.println("Creando /user/root...");

            FileSystem fileSystem = formatService.format(session.getDiskName(), sizeMB, strategy, password);
            session.mount(fileSystem);

            System.out.println("File System creado correctamente.");
            System.out.println();
        } catch (IllegalArgumentException | IOException exception) {
            System.out.println("format: " + exception.getMessage());
        }
    }

    private boolean confirmOverwriteIfNeeded(TerminalSession session, Scanner scanner) {
        File diskFile = new File(session.getDiskName());
        if (!diskFile.exists() && session.getMode() == SystemMode.NO_FORMATTED) {
            return true;
        }

        String answer = CommandIO.prompt(scanner, "Ya existe un disco virtual. Se borrara su contenido. Escriba SI para continuar: ");
        return "SI".equalsIgnoreCase(answer.trim());
    }

    private int readDiskSize(Scanner scanner) {
        while (true) {
            String value = CommandIO.prompt(scanner, "Tamano del disco en MB: ");

            try {
                int size = Integer.parseInt(value);
                if (size > 0) {
                    return size;
                }
            } catch (NumberFormatException exception) {
                // Se vuelve a pedir el dato.
            }

            System.out.println("Ingrese un numero entero mayor a cero.");
        }
    }

    private AllocationStrategy readAllocationStrategy(Scanner scanner) {
        String value = CommandIO.prompt(scanner, "Estrategia de asignacion [INDEXED]: ");
        return AllocationStrategy.fromText(value);
    }
}
