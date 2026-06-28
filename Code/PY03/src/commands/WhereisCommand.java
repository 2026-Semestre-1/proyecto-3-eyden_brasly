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
    /**
     * Ejecuta el comando whereis, buscando archivos por nombre desde una ruta indicada o desde la raíz.
     * @param args Los argumentos del comando, donde args[0] es el nombre del archivo a buscar y args[1] es opcionalmente la ruta de inicio para la búsqueda.
     * @param session La sesión de terminal actual.
     * @param scanner El escáner para leer la entrada del usuario (no se utiliza en este comando).
     */
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
        final String resolvedStartPath = startPath;

        try {
            directoryTree.find(resolvedStartPath)
                    .orElseThrow(() -> new IllegalArgumentException("el directorio de inicio no existe: " + resolvedStartPath));

            List<String> results = directoryTree.findFilesByName(fileName, resolvedStartPath);

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
