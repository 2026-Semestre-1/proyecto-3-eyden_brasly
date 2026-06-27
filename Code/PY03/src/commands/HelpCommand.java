package commands;

import app.SystemMode;
import app.TerminalSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HelpCommand implements Command {
    private static final int COMMAND_WIDTH = 40;
    private static final List<HelpEntry> ENTRIES = new ArrayList<>();

    static {
        add("Arranque y sesion", "format", "format", "Crea y monta un disco virtual nuevo.");
        add("Arranque y sesion", "exit", "exit", "Cierra la terminal actual.");
        add("Arranque y sesion", "clear", "clear", "Limpia la pantalla de la terminal.");
        add("Arranque y sesion", "help", "help [comando]", "Muestra esta ayuda o el manual breve de un comando.");

        add("Usuarios y grupos", "useradd", "useradd [usuario]", "Crea un usuario y su directorio /user/<usuario>.");
        add("Usuarios y grupos", "groupadd", "groupadd [grupo]", "Crea un grupo. Requiere privilegios de root.");
        add("Usuarios y grupos", "passwd", "passwd [usuario]", "Cambia la contrasena propia o la de otro usuario si eres root.");
        add("Usuarios y grupos", "su", "su [usuario]", "Cambia la sesion activa. Sin argumento intenta entrar como root.");
        add("Usuarios y grupos", "whoami", "whoami", "Muestra el usuario activo y su nombre completo.");

        add("Directorios", "pwd", "pwd", "Imprime la ruta actual.");
        add("Directorios", "cd", "cd [directorio]", "Cambia el directorio actual.");
        add("Directorios", "ls", "ls [-R] [directorio]", "Lista archivos, directorios y enlaces.");
        add("Directorios", "mkdir", "mkdir <directorio>", "Crea un directorio.");
        add("Directorios", "whereis", "whereis <nombre>", "Busca archivos por nombre desde el directorio actual.");

        add("Archivos", "touch", "touch <archivo>", "Crea un archivo vacio si no existe.");
        add("Archivos", "cat", "cat <archivo>", "Muestra el contenido completo de un archivo.");
        add("Archivos", "less", "less <archivo>", "Muestra un archivo por paginas.");
        add("Archivos", "note", "note <archivo>", "Edita un archivo dentro de la terminal.");
        add("Archivos", "ln", "ln <archivoOriginal> <nombreEnlace>", "Crea un enlace hacia un archivo existente.");
        add("Archivos", "mv", "mv <origen> <destino>", "Mueve o renombra archivos, directorios y enlaces.");
        add("Archivos", "rm", "rm [-R] <ruta> [ruta...]", "Elimina archivos, enlaces o directorios. Soporta patrones.");

        add("Permisos", "chmod", "chmod <permisos> <ruta> [ruta...]", "Cambia permisos usando dos digitos: dueno/grupo.");
        add("Permisos", "chown", "chown <usuario> <ruta> [ruta...]", "Cambia el dueno de recursos. Requiere root.");
        add("Permisos", "chgrp", "chgrp <grupo> <ruta> [ruta...]", "Cambia el grupo de recursos.");

        add("Diagnostico", "infoFS", "infoFS", "Muestra informacion del sistema de archivos.");
        add("Diagnostico", "viewFCB", "viewFCB <archivo>", "Muestra metadatos internos del FCB de un archivo.");
        add("Diagnostico", "viewFilesOpen", "viewFilesOpen", "Lista los archivos abiertos en la sesion.");
    }

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
        if (args.length > 1) {
            System.out.println("Uso: help [comando]");
            return;
        }

        if (args.length == 1) {
            printCommandHelp(args[0], session);
            return;
        }

        printGeneralHelp(session);
    }

    private void printGeneralHelp(TerminalSession session) {
        boolean initMode = session.getMode() == SystemMode.NO_FORMATTED;
        String currentSection = "";

        System.out.println("miFS shell, comandos internos");
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  help [comando]");
        System.out.println();

        if (initMode) {
            System.out.println("Modo inicial:");
            System.out.println("  No hay un File System montado. Ejecute 'format' para crear uno.");
            System.out.println();
        }

        System.out.println("Comandos disponibles:");

        for (HelpEntry entry : ENTRIES) {
            if (initMode && !isAllowedInInitMode(entry.name)) {
                continue;
            }

            if (!entry.section.equals(currentSection)) {
                currentSection = entry.section;
                System.out.println();
                System.out.println(currentSection + ":");
            }

            System.out.println("  " + padRight(entry.usage, COMMAND_WIDTH) + entry.description);
        }

        System.out.println();
        System.out.println("Use 'help <comando>' para ver detalles. Ejemplo: help rm");
    }

    private void printCommandHelp(String commandName, TerminalSession session) {
        HelpEntry entry = findEntry(commandName);

        if (entry == null) {
            System.out.println("help: no hay entrada para '" + commandName + "'.");
            System.out.println("Use 'help' para listar los comandos disponibles.");
            return;
        }

        if (session.getMode() == SystemMode.NO_FORMATTED && !isAllowedInInitMode(entry.name)) {
            System.out.println(entry.name + ": comando disponible despues de formatear y montar el File System.");
            System.out.println("Ejecute primero: format");
            return;
        }

        System.out.println("NOMBRE");
        System.out.println("    " + entry.name + " - " + entry.description);
        System.out.println();
        System.out.println("SINOPSIS");
        System.out.println("    " + entry.usage);
        System.out.println();
        System.out.println("DESCRIPCION");
        System.out.println("    " + detailFor(entry.name));
        System.out.println();
        printExamples(entry.name);
    }

    private static void add(String section, String name, String usage, String description) {
        ENTRIES.add(new HelpEntry(section, name, usage, description));
    }

    private HelpEntry findEntry(String commandName) {
        for (HelpEntry entry : ENTRIES) {
            if (entry.name.equals(commandName)) {
                return entry;
            }
        }

        for (HelpEntry entry : ENTRIES) {
            if (entry.name.equalsIgnoreCase(commandName)) {
                return entry;
            }
        }

        return null;
    }

    private boolean isAllowedInInitMode(String commandName) {
        return "format".equals(commandName)
                || "exit".equals(commandName)
                || "clear".equals(commandName)
                || "help".equals(commandName);
    }

    private String detailFor(String commandName) {
        return switch (commandName) {
            case "format" -> "Inicializa MBR, SuperBlock, bitmap, tablas de usuarios/grupos y /user/root.";
            case "useradd" -> "Solicita nombre completo, contrasena y confirmacion. Solo root puede ejecutarlo.";
            case "groupadd" -> "Agrega el grupo a la tabla persistente de grupos. Solo root puede ejecutarlo.";
            case "passwd" -> "Valida confirmacion de clave y guarda el nuevo hash en la tabla de usuarios.";
            case "su" -> "Pide contrasena antes de cambiar de usuario y mueve la sesion al home si existe.";
            case "ls" -> "Usa colores por tipo de recurso y puede recorrer subdirectorios con -R.";
            case "rm" -> "Con -R elimina directorios no vacios. Acepta patrones como *, *.txt o docs/*.txt.";
            case "chmod" -> "Los permisos son dos digitos octales: primero dueno, segundo grupo. Ejemplo: 75.";
            case "note" -> "Abre el editor en la terminal. Para salir use la combinacion Ctrl+X.";
            case "less" -> "Muestra bloques de lineas y espera Enter para avanzar entre paginas.";
            case "infoFS" -> "Resume tamano, bloques, espacio usado y datos generales del disco montado.";
            case "viewFCB" -> "Muestra propietario, grupo, permisos, bloques, tamano y estado del archivo.";
            case "viewFilesOpen" -> "Consulta la tabla de archivos abiertos de la sesion actual.";
            default -> "Ejecuta la operacion indicada respetando rutas relativas, absolutas y permisos.";
        };
    }

    private void printExamples(String commandName) {
        String[] examples = switch (commandName) {
            case "format" -> new String[]{"format"};
            case "useradd" -> new String[]{"useradd ana"};
            case "groupadd" -> new String[]{"groupadd desarrollo"};
            case "passwd" -> new String[]{"passwd", "passwd ana"};
            case "su" -> new String[]{"su", "su ana"};
            case "ls" -> new String[]{"ls", "ls -R /user", "ls documentos"};
            case "cd" -> new String[]{"cd ..", "cd documentos", "cd /user/root"};
            case "mkdir" -> new String[]{"mkdir documentos"};
            case "touch" -> new String[]{"touch notas.txt"};
            case "rm" -> new String[]{"rm a.txt", "rm -R docs", "rm *.txt"};
            case "chmod" -> new String[]{"chmod 75 script.txt", "chmod 77 docs"};
            case "chown" -> new String[]{"chown ana notas.txt"};
            case "chgrp" -> new String[]{"chgrp desarrollo notas.txt"};
            case "ln" -> new String[]{"ln notas.txt acceso_notas"};
            case "mv" -> new String[]{"mv viejo.txt nuevo.txt", "mv notas.txt docs"};
            case "cat" -> new String[]{"cat notas.txt"};
            case "less" -> new String[]{"less notas.txt"};
            case "note" -> new String[]{"note notas.txt"};
            case "whereis" -> new String[]{"whereis notas.txt"};
            case "viewFCB" -> new String[]{"viewFCB notas.txt"};
            default -> new String[]{commandName};
        };

        System.out.println("EJEMPLOS");
        for (String example : examples) {
            System.out.println("    " + example);
        }
    }

    private String padRight(String text, int width) {
        if (text.length() >= width) {
            return text + "  ";
        }

        return text + " ".repeat(width - text.length());
    }

    private static class HelpEntry {
        private final String section;
        private final String name;
        private final String usage;
        private final String description;

        private HelpEntry(String section, String name, String usage, String description) {
            this.section = section;
            this.name = name;
            this.usage = usage;
            this.description = description;
        }
    }
}
