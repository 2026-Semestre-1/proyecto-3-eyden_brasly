/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import constants.SystemConstants;
import filesystem.SuperBlock;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class InfoFSCommand implements Command {
    @Override
    public String getName() {
        return "infoFS";
    }

    @Override
    public String getDescription() {
        return "Muestra informacion del File System montado.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (args.length > 0) {
            System.out.println("Uso: infoFS");
            return;
        }

        SuperBlock superBlock = session.getFileSystem().getSuperBlock();
        int freeBlocks = session.getFileSystem().getBlockManager().getFreeBlocks();
        int usedBlocks = session.getFileSystem().getBlockManager().getUsedBlocks();
        long availableBytes = (long) freeBlocks * superBlock.getBlockSize();

        System.out.println("File System: " + superBlock.getFileSystemName());
        System.out.println("Tamano total: " + superBlock.getTotalSizeBytes() + " bytes");
        System.out.println("Tamano de bloque: " + superBlock.getBlockSize() + " bytes");
        System.out.println("Bloques totales: " + superBlock.getTotalBlocks());
        System.out.println("Bloques usados: " + usedBlocks);
        System.out.println("Bloques libres: " + freeBlocks);
        System.out.println("Espacio disponible: " + availableBytes + " bytes");
        System.out.println("Primer bloque de datos: " + SystemConstants.DATA_START_BLOCK);
    }
}
