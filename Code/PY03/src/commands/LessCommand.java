/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import constants.SystemConstants;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Muestra el contenido de un archivo por paginas.
 *
 * @author eyden
 */
public class LessCommand implements Command {
    @Override
    public String getName() {
        return "less";
    }

    @Override
    public String getDescription() {
        return "Muestra el contenido de un archivo por paginas.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 1) {
            System.out.println("Uso: less <archivo>");
            return;
        }

        FileNode file = FileCommandSupport.findFile(session, args[0], getName());
        if (file == null || !FileCommandSupport.openFile(session, file, "LECTURA", getName())) {
            return;
        }

        try {
            String content = session.getFileSystem().getFileContentService().readContent(file);
            showPages(content, scanner);
        } catch (IOException exception) {
            System.out.println("less: no se pudo leer el archivo: " + exception.getMessage());
        } finally {
            FileCommandSupport.closeFile(session, file, getName());
        }
    }

    private void showPages(String content, Scanner scanner) {
        if (content.isEmpty()) {
            System.out.println("(archivo vacio)");
            return;
        }

        String[] lines = content.split("\\R", -1);

        for (int index = 0; index < lines.length; index++) {
            System.out.println(lines[index]);

            boolean pageCompleted = (index + 1) % SystemConstants.LESS_PAGE_SIZE == 0;
            boolean hasMoreLines = index + 1 < lines.length;

            if (pageCompleted && hasMoreLines) {
                System.out.print("--Mas-- (Enter para continuar, q para salir): ");
                if (!scanner.hasNextLine() || "q".equalsIgnoreCase(scanner.nextLine().trim())) {
                    return;
                }
            }
        }
    }
}
