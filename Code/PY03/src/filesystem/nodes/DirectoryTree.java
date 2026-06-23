package filesystem.nodes;

import constants.SystemConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Arbol jerarquico de directorios del File System.
 * Se serializa como una linea por directorio para persistirlo en el disco
 * virtual y reconstruirlo al montar.
 */
public class DirectoryTree {
    private final DirectoryNode root;

    public DirectoryTree() {
        this.root = new DirectoryNode("", SystemConstants.ROOT_USERNAME, SystemConstants.ROOT_GROUP);
    }

    public static DirectoryTree createInitialTree() {
        DirectoryTree tree = new DirectoryTree();
        tree.createDirectory("/", "user", SystemConstants.ROOT_USERNAME, SystemConstants.ROOT_GROUP);
        tree.createDirectory("/user", SystemConstants.ROOT_USERNAME, SystemConstants.ROOT_USERNAME, SystemConstants.ROOT_GROUP);
        return tree;
    }

    public static DirectoryTree fromText(String data) {
        DirectoryTree tree = new DirectoryTree();

        if (data == null || data.isBlank()) {
            return createInitialTree();
        }

        for (String line : data.replace("\0", "").split("\\R")) {
            if (line.isBlank()) {
                continue;
            }

            DirectoryRecord record = DirectoryRecord.fromLine(line);
            if (record == null || "/".equals(record.path)) {
                continue;
            }

            String parentPath = parentPath(record.path);
            String name = fileName(record.path);
            tree.createDirectory(parentPath, name, record.owner, record.group);
        }

        return tree;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        appendDirectory(builder, root);
        return builder.toString();
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public Optional<DirectoryNode> find(String path) {
        String normalizedPath = normalizePath("/", path);
        if ("/".equals(normalizedPath)) {
            return Optional.of(root);
        }

        DirectoryNode current = root;
        for (String part : parts(normalizedPath)) {
            current = current.getDirectory(part);
            if (current == null) {
                return Optional.empty();
            }
        }

        return Optional.of(current);
    }

    public String normalizePath(String currentPath, String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            return currentPath == null || currentPath.isBlank() ? "/" : currentPath;
        }

        List<String> normalizedParts = new ArrayList<>();
        if (!requestedPath.startsWith("/") && currentPath != null) {
            normalizedParts.addAll(parts(currentPath));
        }

        for (String part : requestedPath.split("/+")) {
            if (part.isBlank() || ".".equals(part)) {
                continue;
            }

            if ("..".equals(part)) {
                if (!normalizedParts.isEmpty()) {
                    normalizedParts.remove(normalizedParts.size() - 1);
                }
                continue;
            }

            normalizedParts.add(part);
        }

        if (normalizedParts.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", normalizedParts);
    }

    public DirectoryNode createDirectory(String parentPath, String name, String owner, String group) {
        validateName(name);
        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasDirectory(name)) {
            throw new IllegalArgumentException("el directorio ya existe: " + name);
        }

        DirectoryNode directory = new DirectoryNode(name, owner, group);
        parent.addDirectory(directory);
        return directory;
    }

    private void appendDirectory(StringBuilder builder, DirectoryNode directory) {
        builder.append("path=")
                .append(directory.getPath())
                .append("|owner=")
                .append(directory.getOwner())
                .append("|group=")
                .append(directory.getGroup())
                .append("\n");

        for (DirectoryNode child : directory.getDirectories()) {
            appendDirectory(builder, child);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.contains("/") || ".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("nombre de directorio invalido: " + name);
        }
    }

    private List<String> parts(String path) {
        List<String> result = new ArrayList<>();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return result;
        }

        for (String part : path.split("/+")) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }

        return result;
    }

    private static String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    private static String fileName(String path) {
        int separator = path.lastIndexOf('/');
        return separator == -1 ? path : path.substring(separator + 1);
    }

    private static class DirectoryRecord {
        private final String path;
        private final String owner;
        private final String group;

        private DirectoryRecord(String path, String owner, String group) {
            this.path = path;
            this.owner = owner;
            this.group = group;
        }

        private static DirectoryRecord fromLine(String line) {
            if (line.contains("|")) {
                return fromPipeLine(line);
            }

            return fromLegacyLine(line);
        }

        private static DirectoryRecord fromPipeLine(String line) {
            String path = "/";
            String owner = SystemConstants.ROOT_USERNAME;
            String group = SystemConstants.ROOT_GROUP;

            for (String part : line.split("\\|")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) {
                    continue;
                }

                switch (pair[0]) {
                    case "path" -> path = pair[1];
                    case "owner" -> owner = pair[1];
                    case "group" -> group = pair[1];
                    default -> {
                    }
                }
            }

            return new DirectoryRecord(path, owner, group);
        }

        private static DirectoryRecord fromLegacyLine(String line) {
            Map<String, String> values = new java.util.LinkedHashMap<>();
            for (String part : line.split(",")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2) {
                    values.put(pair[0], pair[1]);
                }
            }

            String path = values.get("path");
            if (path == null || path.isBlank()) {
                return null;
            }

            return new DirectoryRecord(
                    path,
                    values.getOrDefault("owner", SystemConstants.ROOT_USERNAME),
                    values.getOrDefault("group", SystemConstants.ROOT_GROUP)
            );
        }
    }
}
