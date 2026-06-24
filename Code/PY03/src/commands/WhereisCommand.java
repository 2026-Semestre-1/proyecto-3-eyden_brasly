/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;
import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import java.util.List;
import java.util.Scanner;

/**
 * Busca archivos por nombre desde una ruta indicada o desde la raiz.
 * @author Brasly
 */
public class WhereisCommand implements Command {
     @Override
    public String getName() {
        return "whereis";
    }

    @Override
    public String getDescription() {
        return "Busca un archivo por nombre desde una ruta especifica.";
    }
    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length == 0 || args.length > 2) {
            System.out.println("Uso: whereis <nombreArchivo> [rutaInicio]");
            return;
        }

        String fileName = args[0];

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();

        String startPath = "/";
        if (args.length == 2) {
            startPath = directoryTree.normalizePath(session.getCurrentPath(), args[1]);
        }

        try {
            List<String> results = directoryTree.findFilesByName(fileName, startPath);

            if (results.isEmpty()) {
                System.out.println("whereis: no se encontro el archivo: " + fileName);
                return;
            }

            for (String path : results) {
                System.out.println(path);
            }

        } catch (IllegalArgumentException exception) {
            System.out.println("whereis: " + exception.getMessage());
        }
    }
    
}
