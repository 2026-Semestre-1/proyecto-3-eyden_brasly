/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import filesystem.OpenFile;
import filesystem.OpenFileTable;
import java.util.Scanner;
import util.DateUtil;

/**
 * Consulta la tabla de archivos abiertos de la sesion.
 *
 * @author eyden
 */
public class ViewFilesOpenCommand implements Command {
    @Override
    public String getName() {
        return "viewFilesOpen";
    }

    @Override
    public String getDescription() {
        return "Muestra los archivos abiertos en la sesion.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length != 0) {
            System.out.println("Uso: viewFilesOpen");
            return;
        }

        var processFiles = session.getProcessOpenFiles();
        OpenFileTable globalTable = session.getFileSystem().getOpenFileTable();

        System.out.println("--- Archivos abiertos en esta terminal ---");
        System.out.println("Total: " + processFiles.size());

        for (OpenFile file : processFiles) {
            int globalCount = globalTable.getOpenCount(file.getPath());
            System.out.println(
                    file.getPath()
                    + " | usuario=" + file.getUsername()
                    + " | modo=" + file.getMode()
                    + " | apertura=" + DateUtil.formatMillis(file.getOpenedAt())
                    + " | aperturas globales=" + globalCount
            );
        }

        System.out.println();
        System.out.println("--- Tabla global del sistema ---");
        System.out.println("Archivos distintos abiertos: " + globalTable.getTotalFiles());

        for (OpenFile entry : globalTable.getOpenFiles()) {
            System.out.println(
                    entry.getPath()
                    + " | contador=" + entry.getOpenCount()
            );
        }
    }
}
