/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import app.TerminalSession;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public interface Command {
    String getName();

    String getDescription();

    void execute(String[] args, TerminalSession session, Scanner scanner);
}
