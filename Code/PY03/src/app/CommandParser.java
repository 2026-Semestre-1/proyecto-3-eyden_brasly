/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase encargada de parsear una linea de comando en un nombre de comando y sus argumentos.
 * @author eyden
 */
public class CommandParser {
    public ParsedCommand parse(String line) {
        List<String> tokens = tokenize(line == null ? "" : line.trim());

        if (tokens.isEmpty()) {
            return new ParsedCommand("", new String[0]);
        }

        String commandName = tokens.get(0);
        String[] args = tokens.subList(1, tokens.size()).toArray(String[]::new);
        return new ParsedCommand(commandName, args);
    }
    /**
     * Tokeniza una linea de comando en tokens, respetando comillas simples y dobles.
     * @param line La linea de comando a tokenizar.
     * @return Una lista de tokens.
     */
    private List<String> tokenize(String line) {
        if (line.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        char quoteCharacter = '\0';

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);

            if ((character == '"' || character == '\'') && (!insideQuotes || character == quoteCharacter)) {
                insideQuotes = !insideQuotes;
                quoteCharacter = insideQuotes ? character : '\0';
                continue;
            }

            if (Character.isWhitespace(character) && !insideQuotes) {
                addToken(tokens, current);
                continue;
            }

            current.append(character);
        }

        addToken(tokens, current);
        return tokens;
    }
    /**
     * Agrega un token a la lista de tokens si el StringBuilder actual no está vacío.
     * @param tokens La lista de tokens.
     * @param current El StringBuilder que contiene el token actual.
     */
    private void addToken(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }
    /**
     * Clase que representa un comando parseado, con su nombre y argumentos.
     */
    public static class ParsedCommand {
        private final String name;
        private final String[] args;

        public ParsedCommand(String name, String[] args) {
            this.name = name;
            this.args = args;
        }

        public String getName() {
            return name;
        }

        public String[] getArgs() {
            return args;
        }

        public boolean isEmpty() {
            return name == null || name.isBlank();
        }
    }
}
