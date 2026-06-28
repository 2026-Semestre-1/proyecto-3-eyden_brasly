/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.nodes.FileNode;
import java.io.IOException;
import java.util.Scanner;

/**
 * Muestra el contenido completo de un archivo.
 *
 * @author eyden
 */
public class CatCommand implements Command {
    @Override
    public String getName() {
        return "cat";
    }

    @Override
    public String getDescription() {
        return "Muestra el contenido completo de un archivo.";
    }
    /**
     * Ejecuta el comando cat, mostrando el contenido de un archivo especificado.
     * @param args Los argumentos del comando, donde args[0] debe ser el nombre del archivo.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 1) {
            System.out.println("Uso: cat <archivo>");
            return;
        }

        FileNode file = FileCommandSupport.findFile(session, args[0], getName());
        if (file == null || !FileCommandSupport.openFile(session, file, "LECTURA", getName())) {
            return;
        }

        try {
            String content = session.getFileSystem().getFileContentService().readContent(file);
            System.out.print(content);
            if (!content.isEmpty() && !content.endsWith("\n")) {
                System.out.println();
            }
        } catch (IOException exception) {
            System.out.println("cat: no se pudo leer el archivo: " + exception.getMessage());
        } finally {
            FileCommandSupport.closeFile(session, file, getName());
        }
    }
}
