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
 * Editor de texto simple para reemplazar el contenido de un archivo.
 *
 * @author eyden
 */
public class NoteCommand implements Command {
    private static final char CTRL_X = '\u0018';

    @Override
    public String getName() {
        return "note";
    }

    @Override
    public String getDescription() {
        return "Edita el contenido de un archivo de texto.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 1) {
            System.out.println("Uso: note <archivo>");
            return;
        }

        FileNode file = FileCommandSupport.findFile(session, args[0], getName());
        if (file == null || !FileCommandSupport.openFile(session, file, "ESCRITURA", getName())) {
            return;
        }

        try {
            String currentContent = session.getFileSystem().getFileContentService().readContent(file);
            showCurrentContent(currentContent);
            String editedContent = readEditedContent(scanner);

            if (editedContent == null) {
                System.out.println("note: edicion cancelada.");
                return;
            }

            String answer = CommandIO.prompt(scanner, "Guardar cambios? [s/N]: ");
            if (!"s".equalsIgnoreCase(answer) && !"si".equalsIgnoreCase(answer)) {
                System.out.println("note: cambios descartados.");
                return;
            }

            session.getFileSystem().getFileContentService().writeContent(file, editedContent);
            System.out.println("note: archivo guardado correctamente.");
        } catch (IOException | IllegalStateException exception) {
            System.out.println("note: no se pudo editar el archivo: " + exception.getMessage());
        } finally {
            FileCommandSupport.closeFile(session, file, getName());
        }
    }

    private void showCurrentContent(String content) {
        System.out.println("--- Contenido actual ---");
        if (content.isEmpty()) {
            System.out.println("(archivo vacio)");
        } else {
            System.out.print(content);
            if (!content.endsWith("\n")) {
                System.out.println();
            }
        }
        System.out.println("--- Nuevo contenido ---");
        System.out.println("Finalice con Ctrl+X.");
    }

    private String readEditedContent(Scanner scanner) {
        StringBuilder content = new StringBuilder();
        boolean firstLine = true;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int controlIndex = line.indexOf(CTRL_X);

            if (controlIndex >= 0) {
                String beforeControl = line.substring(0, controlIndex);
                if (!beforeControl.isEmpty()) {
                    appendLine(content, beforeControl, firstLine);
                }
                return content.toString();
            }

            if (SystemConstants.NOTE_EXIT_COMMAND.equalsIgnoreCase(line.trim())) {
                return content.toString();
            }

            appendLine(content, line, firstLine);
            firstLine = false;
        }

        return null;
    }

    private void appendLine(StringBuilder content, String line, boolean firstLine) {
        if (!firstLine) {
            content.append(System.lineSeparator());
        }
        content.append(line);
    }
}
