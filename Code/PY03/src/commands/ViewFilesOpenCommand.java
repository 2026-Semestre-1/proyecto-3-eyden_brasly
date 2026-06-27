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

        OpenFileTable table = session.getFileSystem().getOpenFileTable();
        System.out.println("Total de archivos abiertos: " + table.getTotalFiles());

        for (OpenFile file : table.getOpenFiles()) {
            System.out.println(
                    file.getPath()
                    + " | usuario=" + file.getUsername()
                    + " | modo=" + file.getMode()
                    + " | apertura=" + DateUtil.formatMillis(file.getOpenedAt())
            );
        }
    }
}
