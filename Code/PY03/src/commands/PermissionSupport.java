package commands;

import app.TerminalSession;
import constants.SystemConstants;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.FSNode;
import filesystem.nodes.FileNode;

/**
 * Clase de soporte para la verificación de permisos de archivos y directorios.
 * @author eyden
 */
public final class PermissionSupport {
    public enum Access {
        READ(4),
        WRITE(2),
        EXECUTE(1);

        private final int bit;

        Access(int bit) {
            this.bit = bit;
        }
    }

    private PermissionSupport() {
    }
    /**
     * Verifica si el usuario de la sesión tiene acceso específico a un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @param node El nodo del sistema de archivos a verificar.
     * @param access El tipo de acceso a verificar (lectura, escritura o ejecución).
     * @return true si el usuario tiene el acceso especificado, false en caso contrario.
     */
    public static boolean hasAccess(TerminalSession session, FSNode node, Access access) {
        if (session.isPrivileged()) {
            return true;
        }

        int digit = permissionDigitFor(session, node);
        return (digit & access.bit) != 0;
    }
    /**
     * Verifica si el usuario de la sesión tiene todos los accesos especificados a un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @param node El nodo del sistema de archivos a verificar.
     * @param accesses Los tipos de acceso a verificar (lectura, escritura o ejecución).
     * @return true si el usuario tiene todos los accesos especificados, false en caso contrario.
     */
    static boolean hasAll(TerminalSession session, FSNode node, Access... accesses) {
        for (Access access : accesses) {
            if (!hasAccess(session, node, access)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Verifica si el usuario de la sesión tiene al menos uno de los accesos especificados a un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @param node El nodo del sistema de archivos a verificar.
     * @param accesses Los tipos de acceso a verificar (lectura, escritura o ejecución).
     * @return true si el usuario tiene al menos uno de los accesos especificados, false en caso contrario.
     */
    static boolean canModifyMetadata(TerminalSession session, FSNode node) {
        return session.isPrivileged()
                || session.getActiveUser().getUsername().equals(node.getOwner());
    }

    /**
     * Verifica si el usuario de la sesión puede cambiar el propietario de un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @return true si el usuario puede cambiar el propietario, false en caso contrario.
     */
    static boolean canChangeOwner(TerminalSession session) {
        return session.isPrivileged();
    }

    /**
     * Verifica si el usuario de la sesión puede cambiar el grupo de un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @param node El nodo del sistema de archivos a verificar.
     * @param targetGroup El grupo al que se desea cambiar.
     * @return true si el usuario puede cambiar el grupo, false en caso contrario.
     */
    static boolean canChangeGroup(TerminalSession session, FSNode node, String targetGroup) {
        return session.isPrivileged()
                || session.getActiveUser().getUsername().equals(node.getOwner())
                && session.getActiveUser().getPrimaryGroup().equals(targetGroup);
    }
    /**
     * Muestra un mensaje de permiso denegado para una acción específica en un nodo del sistema de archivos.
     * @param commandName El nombre del comando que intentó realizar la acción.
     * @param action La acción que se intentó realizar (por ejemplo, "leer", "escribir", "ejecutar").
     * @param path La ruta del nodo del sistema de archivos donde se intentó realizar la acción.
     * @return false siempre, para indicar que la acción no fue permitida.
     */
    static boolean deny(String commandName, String action, String path) {
        System.out.println(commandName + ": permiso denegado para " + action + ": " + path);
        return false;
    }
    /**
     * Verifica si un valor de permiso es válido, es decir, si es un número octal de dos dígitos entre 00 y 77.
     * @param value El valor de permiso a verificar.
     * @return true si el valor es válido, false en caso contrario.
     */
    static boolean isValidPermission(String value) {
        return value != null && value.matches("[0-7]{2}");
    }
    /**
     * Convierte un valor de permiso en formato de cadena a un entero.
     * @param value El valor de permiso en formato de cadena (por ejemplo, "75").
     * @return El valor de permiso como un entero.
     * @throws IllegalArgumentException si el valor no es un permiso válido.
     */
    static int parsePermissions(String value) {
        if (!isValidPermission(value)) {
            throw new IllegalArgumentException("los permisos deben ser dos digitos octales entre 0 y 7, por ejemplo 77 o 64.");
        }
        return Integer.parseInt(value);
    }
    /**
     * Formatea un valor de permiso como una cadena de dos dígitos.
     * @param permissions El valor de permiso a formatear.
     * @return El valor de permiso formateado como una cadena de dos dígitos.
     */
    static String formatPermissions(int permissions) {
        return String.format("%02d", permissions);
    }
    /**
     * Obtiene los permisos de un nodo del sistema de archivos.
     * @param node El nodo del sistema de archivos del cual se desean obtener los permisos.
     * @return Los permisos del nodo como un entero.
     */
    static int permissionsOf(FSNode node) {
        if (node instanceof FileNode file) {
            return file.getFCB().getPermissions();
        }
        if (node instanceof DirectoryNode directory) {
            return directory.getPermissions();
        }
        return SystemConstants.DEFAULT_FILE_PERMISSIONS;
    }
    /**
     * Establece los permisos de un nodo del sistema de archivos.
     * @param node El nodo del sistema de archivos al cual se desean establecer los permisos.
     * @param permissions Los permisos a establecer como un entero.
     */
    static void setPermissions(FSNode node, int permissions) {
        if (node instanceof FileNode file) {
            file.setPermissions(permissions);
        } else if (node instanceof DirectoryNode directory) {
            directory.setPermissions(permissions);
        }
    }
    /**
     * Obtiene el dígito de permiso correspondiente al usuario de la sesión para un nodo del sistema de archivos.
     * @param session La sesión de terminal actual.
     * @param node El nodo del sistema de archivos del cual se desea obtener el dígito de permiso.
     * @return El dígito de permiso correspondiente al usuario de la sesión.
     */
    private static int permissionDigitFor(TerminalSession session, FSNode node) {
        int permissions = Math.abs(permissionsOf(node));
        int ownerDigit = (permissions / 10) % 10;
        int groupDigit = permissions % 10;

        if (session.getActiveUser().getUsername().equals(node.getOwner())) {
            return ownerDigit;
        }

        if (session.getActiveUser().getPrimaryGroup().equals(node.getGroup())) {
            return groupDigit;
        }

        return 0;
    }
}
