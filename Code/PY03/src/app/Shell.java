/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

import commands.Command;
import commands.CommandRegistry;
import java.util.Scanner;

/**
 *
 * @author eyden
 */
public class Shell {
    private final TerminalSession session;
    private final CommandRegistry registry;
    private final CommandParser parser;
    private final Scanner scanner;

    public Shell(TerminalSession session, CommandRegistry registry, Scanner scanner) {
        this.session = session;
        this.registry = registry;
        this.parser = new CommandParser();
        this.scanner = scanner;
    }

    public void start() {
        while (session.isRunning()) {
            System.out.print(session.getPrompt());

            if (!scanner.hasNextLine()) {
                session.stop();
                break;
            }

            CommandParser.ParsedCommand parsedCommand = parser.parse(scanner.nextLine());
            if (parsedCommand.isEmpty()) {
                continue;
            }

            if (session.getMode() == SystemMode.NO_FORMATTED && !isAllowedInInitMode(parsedCommand.getName())) {
                System.out.println("Error: no existe un File System formateado. Ejecute primero: format");
                continue;
            }

            Command command = registry.find(parsedCommand.getName()).orElse(null);
            if (command == null) {
                System.out.println(parsedCommand.getName() + ": comando no encontrado.");
                continue;
            }

            command.execute(parsedCommand.getArgs(), session, scanner);
        }
    }

    private boolean isAllowedInInitMode(String commandName) {
        return "format".equals(commandName)
                || "exit".equals(commandName)
                || "clear".equals(commandName)
                || "help".equals(commandName);
    }
}
