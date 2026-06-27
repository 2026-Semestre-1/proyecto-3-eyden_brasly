package util;

import java.util.regex.Pattern;

/**
 * Utilidad para comparar nombres con patrones simples tipo shell.
 * Soporta '*' como comodin de cero o mas caracteres.
 * 
 * @author eyden
 */
public final class WildcardMatcher {
    private WildcardMatcher() {
    }

    public static boolean hasWildcard(String value) {
        return value != null && value.contains("*");
    }

    public static boolean matches(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }

        return Pattern.compile(toRegex(pattern)).matcher(value).matches();
    }

    public static String toRegex(String pattern) {
        StringBuilder regex = new StringBuilder("^");

        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current == '*') {
                regex.append(".*");
            } else if ("\\.[]{}()+-^$?|".indexOf(current) >= 0) {
                regex.append('\\').append(current);
            } else {
                regex.append(current);
            }
        }

        return regex.append('$').toString();
    }
}
