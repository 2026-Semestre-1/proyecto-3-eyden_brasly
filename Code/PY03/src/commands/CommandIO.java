package commands;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

final class CommandIO {
    private CommandIO() {
    }

    static String prompt(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }
    /**
     * Lee una contraseña de manera segura desde la entrada estándar, ocultando la entrada del usuario.
     * @param scanner El escáner para leer la entrada del usuario.
     * @param message El mensaje a mostrar al usuario antes de leer la contraseña.
     * @return La contraseña ingresada por el usuario como una cadena de texto.
     */
    static String readPassword(Scanner scanner, String message) {
        Console console = System.console();

        if (console != null) {
            char[] password = console.readPassword(message);
            if (password == null) {
                return "";
            }

            String value = new String(password);
            Arrays.fill(password, '\0');
            return value;
        }

        System.out.print(message);
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }
}
