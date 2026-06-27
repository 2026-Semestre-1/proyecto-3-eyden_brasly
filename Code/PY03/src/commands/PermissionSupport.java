package commands;

import app.TerminalSession;
import constants.SystemConstants;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.FSNode;
import filesystem.nodes.FileNode;

/**
 *
 * @author eyden
 */
final class PermissionSupport {
    enum Access {
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

    static boolean hasAccess(TerminalSession session, FSNode node, Access access) {
        if (session.isPrivileged()) {
            return true;
        }

        int digit = permissionDigitFor(session, node);
        return (digit & access.bit) != 0;
    }

    static boolean hasAll(TerminalSession session, FSNode node, Access... accesses) {
        for (Access access : accesses) {
            if (!hasAccess(session, node, access)) {
                return false;
            }
        }
        return true;
    }

    static boolean canModifyMetadata(TerminalSession session, FSNode node) {
        return session.isPrivileged()
                || session.getActiveUser().getUsername().equals(node.getOwner());
    }

    static boolean canChangeOwner(TerminalSession session) {
        return session.isPrivileged();
    }

    static boolean canChangeGroup(TerminalSession session, FSNode node, String targetGroup) {
        return session.isPrivileged()
                || session.getActiveUser().getUsername().equals(node.getOwner())
                && session.getActiveUser().getPrimaryGroup().equals(targetGroup);
    }

    static boolean deny(String commandName, String action, String path) {
        System.out.println(commandName + ": permiso denegado para " + action + ": " + path);
        return false;
    }

    static boolean isValidPermission(String value) {
        return value != null && value.matches("[0-7]{2}");
    }

    static int parsePermissions(String value) {
        if (!isValidPermission(value)) {
            throw new IllegalArgumentException("los permisos deben ser dos digitos octales entre 0 y 7, por ejemplo 77 o 64.");
        }
        return Integer.parseInt(value);
    }

    static String formatPermissions(int permissions) {
        return String.format("%02d", permissions);
    }

    static int permissionsOf(FSNode node) {
        if (node instanceof FileNode file) {
            return file.getFCB().getPermissions();
        }
        if (node instanceof DirectoryNode directory) {
            return directory.getPermissions();
        }
        return SystemConstants.DEFAULT_FILE_PERMISSIONS;
    }

    static void setPermissions(FSNode node, int permissions) {
        if (node instanceof FileNode file) {
            file.setPermissions(permissions);
        } else if (node instanceof DirectoryNode directory) {
            directory.setPermissions(permissions);
        }
    }

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
