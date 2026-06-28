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
    /**
     * Ejecuta el comando note, permitiendo al usuario editar el contenido de un archivo de texto.
     * @param args Los argumentos del comando, donde args[0] es el nombre del archivo a editar.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario.
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 1) {
            System.out.println("Uso: note <archivo>");
            return;
        }

        FileNode file = FileCommandSupport.findFile(session, args[0], getName());
        if (file == null) {
            return;
        }
        if (!PermissionSupport.hasAll(
                session,
                file,
                PermissionSupport.Access.READ,
                PermissionSupport.Access.WRITE
        )) {
            PermissionSupport.deny(getName(), "editar", file.getFullPath());
            return;
        }
        if (!FileCommandSupport.openFile(session, file, "ESCRITURA", getName())) {
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
    /**
     * Muestra el contenido actual del archivo y un mensaje indicando cómo finalizar la edición.
     * @param content El contenido actual del archivo a mostrar.
     */
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
    /**
     * Lee el contenido editado por el usuario hasta que se ingrese Ctrl+X o el comando de salida.
     * @param scanner El escáner para leer la entrada del usuario.
     * @return El contenido editado por el usuario, o null si la edición fue cancelada.
     */
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
    /**
     * Agrega una línea al contenido editado, asegurando que se agregue un salto de línea entre líneas.
     * @param content El StringBuilder que contiene el contenido editado.
     * @param line La línea a agregar al contenido.
     * @param firstLine Indica si es la primera línea que se está agregando.
     */
    private void appendLine(StringBuilder content, String line, boolean firstLine) {
        if (!firstLine) {
            content.append(System.lineSeparator());
        }
        content.append(line);
    }
}
