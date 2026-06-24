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

            if (line.contains("type=FILE")) {
                FileRecord fileRecord = FileRecord.fromLine(line);

                if (fileRecord == null || fileRecord.path == null || fileRecord.path.isBlank()) {
                    continue;
                }

                String parentPath = parentPath(fileRecord.path);
                String name = fileName(fileRecord.path);

                tree.createFileFromRecord(parentPath, name, fileRecord);
            } else {
                DirectoryRecord record = DirectoryRecord.fromLine(line);

                if (record == null || "/".equals(record.path)) {
                    continue;
                }

                String parentPath = parentPath(record.path);
                String name = fileName(record.path);

                tree.createDirectory(parentPath, name, record.owner, record.group);
            }
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
        builder.append("type=DIR")
                .append("|path=")
                .append(directory.getPath())
                .append("|owner=")
                .append(directory.getOwner())
                .append("|group=")
                .append(directory.getGroup())
                .append("\n");

        for (FileNode file : directory.getFiles()) {
            appendFile(builder, file);
        }

        for (DirectoryNode child : directory.getDirectories()) {
            appendDirectory(builder, child);
        }
    }
    private void appendFile(StringBuilder builder, FileNode file) {
        FCB fcb = file.getFCB();

        builder.append("type=FILE")
                .append("|path=")
                .append(fcb.getFullPath())
                .append("|owner=")
                .append(fcb.getOwner())
                .append("|group=")
                .append(fcb.getGroup())
                .append("|permissions=")
                .append(fcb.getPermissions())
                .append("|creationDate=")
                .append(fcb.getCreationDate())
                .append("|size=")
                .append(fcb.getSize())
                .append("|open=")
                .append(fcb.isOpen())
                .append("|blocks=")
                .append(blocksToText(fcb.getBlocks()))
                .append("\n");
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
    public FileNode createFile(String currentPath, String requestedPath, String owner, String group, int permissions) {
        String fullPath = normalizePath(currentPath, requestedPath);

        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        validateName(name);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo o directorio con ese nombre: " + name);
        }

        FileNode file = new FileNode(name, owner, group, permissions, fullPath);
        parent.addFile(file);

        return file;
    }
    public List<String> findFilesByName(String fileName, String startPath) {
        validateName(fileName);

        DirectoryNode start = find(startPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio de inicio no existe: " + startPath));
        List<String> results = new ArrayList<>();
        searchFiles(start, fileName, results);

        return results;
    }

    private void searchFiles(DirectoryNode directory, String fileName, List<String> results) {
        FileNode file = directory.getFile(fileName);

        if (file != null) {
            results.add(file.getFullPath());
        }

        for (DirectoryNode child : directory.getDirectories()) {
            searchFiles(child, fileName, results);
        }
    }
    public Optional<FileNode> findFile(String currentPath, String requestedPath) {
        String fullPath = normalizePath(currentPath, requestedPath);
        String parentPath = parentPath(fullPath);
        String fileName = fileName(fullPath);
        Optional<DirectoryNode> parent = find(parentPath);
        if (parent.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(parent.get().getFile(fileName));
    }
    private void createFileFromRecord(String parentPath, String name, FileRecord record) {
        validateName(name);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo o directorio con ese nombre: " + name);
        }

        FCB fcb = new FCB(
                name,
                record.owner,
                record.group,
                record.permissions,
                record.path,
                record.creationDate,
                record.size,
                record.open,
                record.blocks
        );

        FileNode file = new FileNode(fcb);
        parent.addFile(file);
    }

    private String blocksToText(List<Integer> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        List<String> values = new ArrayList<>();

        for (Integer block : blocks) {
            values.add(String.valueOf(block));
        }

        return String.join(",", values);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static ArrayList<Integer> parseBlocks(String value) {
        ArrayList<Integer> blocks = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return blocks;
        }

        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                blocks.add(parseInt(part, -1));
            }
        }

        blocks.removeIf(block -> block < 0);

        return blocks;
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
    private static class FileRecord {

        private final String path;
        private final String owner;
        private final String group;
        private final int permissions;
        private final long creationDate;
        private final int size;
        private final boolean open;
        private final ArrayList<Integer> blocks;

        private FileRecord(String path, String owner, String group, int permissions,
                long creationDate, int size, boolean open, ArrayList<Integer> blocks) {
            this.path = path;
            this.owner = owner;
            this.group = group;
            this.permissions = permissions;
            this.creationDate = creationDate;
            this.size = size;
            this.open = open;
            this.blocks = blocks;
        }

        private static FileRecord fromLine(String line) {
            Map<String, String> values = new java.util.LinkedHashMap<>();

            for (String part : line.split("\\|")) {
                String[] pair = part.split("=", 2);

                if (pair.length == 2) {
                    values.put(pair[0], pair[1]);
                }
            }

            String path = values.get("path");

            if (path == null || path.isBlank()) {
                return null;
            }

            return new FileRecord(
                    path,
                    values.getOrDefault("owner", SystemConstants.ROOT_USERNAME),
                    values.getOrDefault("group", SystemConstants.ROOT_GROUP),
                    parseInt(values.get("permissions"), 77),
                    parseLong(values.get("creationDate"), System.currentTimeMillis()),
                    parseInt(values.get("size"), 0),
                    parseBoolean(values.get("open"), false),
                    parseBlocks(values.get("blocks"))
            );
        }
    }
}
