package commands;

import app.TerminalSession;
import app.SystemMode;
import java.util.Scanner;

public class HelpCommand implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Muestra los comandos disponibles.";
    }

    @Override
    public void execute(String[] args, TerminalSession session, Scanner scanner) {
        if (session.getMode() == SystemMode.NO_FORMATTED) {
            System.out.println("Comandos disponibles: format, clear, help, exit");
            return;
        }

        System.out.println(
                "Comandos disponibles: useradd, groupadd, passwd, su, whoami, "
                + "pwd, mkdir, cd, ls, touch, whereis, infoFS, clear, help, exit"
        );
    }
}
