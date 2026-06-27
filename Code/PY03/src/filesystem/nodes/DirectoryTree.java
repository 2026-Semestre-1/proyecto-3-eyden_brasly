package filesystem.nodes;

import constants.SystemConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Arbol jerarquico de directorios del File System.
 * Se guarda como texto para persistirlo en el disco virtual.
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

            } else if (line.contains("type=LINK")) {
                LinkRecord linkRecord = LinkRecord.fromLine(line);

                if (linkRecord == null || linkRecord.path == null || linkRecord.path.isBlank()) {
                    continue;
                }

                String parentPath = parentPath(linkRecord.path);
                String name = fileName(linkRecord.path);

                tree.createLinkFromRecord(parentPath, name, linkRecord);

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

    public Optional<FileNode> findFileResolvingLink(String currentPath, String requestedPath) {
        String fullPath = normalizePath(currentPath, requestedPath);
        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        Optional<DirectoryNode> parent = find(parentPath);

        if (parent.isEmpty()) {
            return Optional.empty();
        }

        FileNode file = parent.get().getFile(name);

        if (file != null) {
            return Optional.of(file);
        }

        String targetPath = parent.get().getLinkTarget(name);

        if (targetPath == null || targetPath.isBlank()) {
            return Optional.empty();
        }

        return findFile("/", targetPath);
    }

    public Optional<FSNode> findNode(String currentPath, String requestedPath) {
        String fullPath = normalizePath(currentPath, requestedPath);

        if ("/".equals(fullPath)) {
            return Optional.of(root);
        }

        Optional<DirectoryNode> directory = find(fullPath);

        if (directory.isPresent()) {
            return Optional.of(directory.get());
        }

        Optional<FileNode> file = findFile("/", fullPath);

        if (file.isPresent()) {
            return Optional.of(file.get());
        }

        return Optional.empty();
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

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + name);
        }

        DirectoryNode directory = new DirectoryNode(name, owner, group);
        parent.addDirectory(directory);

        return directory;
    }

    public FileNode createFile(String currentPath, String requestedPath, String owner, String group, int permissions) {
        String fullPath = normalizePath(currentPath, requestedPath);
        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        validateName(name);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + name);
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

    public FileNode removeFile(String currentPath, String requestedPath) {
        String fullPath = normalizePath(currentPath, requestedPath);
        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        FileNode removed = parent.removeFile(name);

        if (removed == null) {
            throw new IllegalArgumentException("el archivo no existe: " + fullPath);
        }

        removeLinksPointingTo(fullPath);

        return removed;
    }

    public List<FileNode> removeDirectory(String currentPath, String requestedPath, boolean recursive) {
        String fullPath = normalizePath(currentPath, requestedPath);

        if ("/".equals(fullPath)) {
            throw new IllegalArgumentException("no se puede eliminar el directorio raiz.");
        }

        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        DirectoryNode directory = parent.getDirectory(name);

        if (directory == null) {
            throw new IllegalArgumentException("el directorio no existe: " + fullPath);
        }

        if (!recursive && !directory.isEmpty()) {
            throw new IllegalArgumentException("el directorio no esta vacio. Use rm -R.");
        }

        List<FileNode> removedFiles = new ArrayList<>();
        collectFiles(directory, removedFiles);

        parent.removeDirectory(name);
        removeLinksPointingInside(fullPath);

        return removedFiles;
    }

    public List<FileNode> removeNode(String currentPath, String requestedPath, boolean recursive) {
        String fullPath = normalizePath(currentPath, requestedPath);

        if ("/".equals(fullPath)) {
            throw new IllegalArgumentException("no se puede eliminar el directorio raiz.");
        }

        String parentPath = parentPath(fullPath);
        String name = fileName(fullPath);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("la ruta no existe: " + parentPath));

        if (parent.hasFile(name)) {
            List<FileNode> removed = new ArrayList<>();
            removed.add(removeFile("/", fullPath));
            return removed;
        }

        if (parent.hasLink(name)) {
            parent.removeLink(name);
            return new ArrayList<>();
        }

        if (parent.hasDirectory(name)) {
            return removeDirectory("/", fullPath, recursive);
        }

        throw new IllegalArgumentException("no existe el archivo o directorio: " + fullPath);
    }

    public void moveNode(String currentPath, String sourcePath, String destinationPath) {
        String sourceFullPath = normalizePath(currentPath, sourcePath);

        if ("/".equals(sourceFullPath)) {
            throw new IllegalArgumentException("no se puede mover el directorio raiz.");
        }

        String sourceParentPath = parentPath(sourceFullPath);
        String sourceName = fileName(sourceFullPath);

        DirectoryNode sourceParent = find(sourceParentPath)
                .orElseThrow(() -> new IllegalArgumentException("la ruta origen no existe: " + sourceParentPath));

        String targetFullPath = normalizePath(currentPath, destinationPath);

        Optional<DirectoryNode> targetAsDirectory = find(targetFullPath);

        if (targetAsDirectory.isPresent()) {
            targetFullPath = joinPath(targetFullPath, sourceName);
        }

        String targetParentPath = parentPath(targetFullPath);
        String targetName = fileName(targetFullPath);

        validateName(targetName);

        DirectoryNode targetParent = find(targetParentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio destino no existe: " + targetParentPath));

        if (targetParent.hasChild(targetName)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + targetName);
        }

        if (sourceParent.hasFile(sourceName)) {
            moveFile(sourceParent, sourceName, sourceFullPath, targetParent, targetName, targetFullPath);
            return;
        }

        if (sourceParent.hasLink(sourceName)) {
            moveLink(sourceParent, sourceName, targetParent, targetName);
            return;
        }

        if (sourceParent.hasDirectory(sourceName)) {
            moveDirectory(sourceParent, sourceName, sourceFullPath, targetParent, targetName, targetFullPath);
            return;
        }

        throw new IllegalArgumentException("no existe el origen: " + sourceFullPath);
    }

    public String createLink(String currentPath, String originalPath, String linkPath) {
        String originalFullPath = normalizePath(currentPath, originalPath);

        FileNode originalFile = findFile("/", originalFullPath)
                .orElseThrow(() -> new IllegalArgumentException("el archivo original no existe: " + originalFullPath));

        String linkFullPath = normalizePath(currentPath, linkPath);
        String parentPath = parentPath(linkFullPath);
        String linkName = fileName(linkFullPath);

        validateName(linkName);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio destino no existe: " + parentPath));

        if (parent.hasChild(linkName)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + linkName);
        }

        parent.addLink(linkName, originalFile.getFullPath());

        return linkFullPath;
    }

    private void moveFile(DirectoryNode sourceParent, String sourceName, String sourcePath,
            DirectoryNode targetParent, String targetName, String targetPath) {
        FileNode sourceFile = sourceParent.getFile(sourceName);
        FileNode movedFile = copyFileWithNewPath(sourceFile, targetName, targetPath);

        targetParent.addFile(movedFile);
        sourceParent.removeFile(sourceName);

        updateLinksTarget(sourcePath, targetPath);
    }

    private void moveLink(DirectoryNode sourceParent, String sourceName,
            DirectoryNode targetParent, String targetName) {
        String target = sourceParent.getLinkTarget(sourceName);

        targetParent.addLink(targetName, target);
        sourceParent.removeLink(sourceName);
    }

    private void moveDirectory(DirectoryNode sourceParent, String sourceName, String sourcePath,
            DirectoryNode targetParent, String targetName, String targetPath) {
        if (targetPath.equals(sourcePath) || targetPath.startsWith(sourcePath + "/")) {
            throw new IllegalArgumentException("no se puede mover un directorio dentro de si mismo.");
        }

        DirectoryNode sourceDirectory = sourceParent.getDirectory(sourceName);
        DirectoryNode movedDirectory = copyDirectoryWithNewPath(sourceDirectory, targetName, targetPath);

        targetParent.addDirectory(movedDirectory);
        sourceParent.removeDirectory(sourceName);

        updateLinksInsideMovedDirectory(sourcePath, targetPath);
    }

    private DirectoryNode copyDirectoryWithNewPath(DirectoryNode source, String newName, String newPath) {
        DirectoryNode copy = new DirectoryNode(newName, source.getOwner(), source.getGroup());

        for (FileNode file : source.getFiles()) {
            String filePath = joinPath(newPath, file.getName());
            copy.addFile(copyFileWithNewPath(file, file.getName(), filePath));
        }

        for (Map.Entry<String, String> link : source.getLinks().entrySet()) {
            copy.addLink(link.getKey(), link.getValue());
        }

        for (DirectoryNode child : source.getDirectories()) {
            String childPath = joinPath(newPath, child.getName());
            copy.addDirectory(copyDirectoryWithNewPath(child, child.getName(), childPath));
        }

        return copy;
    }

    private FileNode copyFileWithNewPath(FileNode file, String newName, String newPath) {
        FCB oldFCB = file.getFCB();

        FCB newFCB = new FCB(
                newName,
                oldFCB.getOwner(),
                oldFCB.getGroup(),
                oldFCB.getPermissions(),
                newPath,
                oldFCB.getCreationDate(),
                oldFCB.getSize(),
                oldFCB.isOpen(),
                new ArrayList<>(oldFCB.getBlocks())
        );

        return new FileNode(newFCB);
    }

    private void collectFiles(DirectoryNode directory, List<FileNode> removedFiles) {
        removedFiles.addAll(directory.getFiles());

        for (DirectoryNode child : directory.getDirectories()) {
            collectFiles(child, removedFiles);
        }
    }

    private void removeLinksPointingTo(String targetPath) {
        removeLinksPointingTo(root, targetPath);
    }

    private void removeLinksPointingTo(DirectoryNode directory, String targetPath) {
        List<String> linksToRemove = new ArrayList<>();

        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            if (link.getValue().equals(targetPath)) {
                linksToRemove.add(link.getKey());
            }
        }

        for (String linkName : linksToRemove) {
            directory.removeLink(linkName);
        }

        for (DirectoryNode child : directory.getDirectories()) {
            removeLinksPointingTo(child, targetPath);
        }
    }

    private void removeLinksPointingInside(String directoryPath) {
        removeLinksPointingInside(root, directoryPath);
    }

    private void removeLinksPointingInside(DirectoryNode directory, String directoryPath) {
        List<String> linksToRemove = new ArrayList<>();

        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            String target = link.getValue();

            if (target.equals(directoryPath) || target.startsWith(directoryPath + "/")) {
                linksToRemove.add(link.getKey());
            }
        }

        for (String linkName : linksToRemove) {
            directory.removeLink(linkName);
        }

        for (DirectoryNode child : directory.getDirectories()) {
            removeLinksPointingInside(child, directoryPath);
        }
    }

    private void updateLinksTarget(String oldTarget, String newTarget) {
        updateLinksTarget(root, oldTarget, newTarget);
    }

    private void updateLinksTarget(DirectoryNode directory, String oldTarget, String newTarget) {
        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            if (link.getValue().equals(oldTarget)) {
                directory.updateLink(link.getKey(), newTarget);
            }
        }

        for (DirectoryNode child : directory.getDirectories()) {
            updateLinksTarget(child, oldTarget, newTarget);
        }
    }

    private void updateLinksInsideMovedDirectory(String oldPath, String newPath) {
        updateLinksInsideMovedDirectory(root, oldPath, newPath);
    }

    private void updateLinksInsideMovedDirectory(DirectoryNode directory, String oldPath, String newPath) {
        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            String target = link.getValue();

            if (target.equals(oldPath) || target.startsWith(oldPath + "/")) {
                String updatedTarget = newPath + target.substring(oldPath.length());
                directory.updateLink(link.getKey(), updatedTarget);
            }
        }

        for (DirectoryNode child : directory.getDirectories()) {
            updateLinksInsideMovedDirectory(child, oldPath, newPath);
        }
    }

    private void createFileFromRecord(String parentPath, String name, FileRecord record) {
        validateName(name);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + name);
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

    private void createLinkFromRecord(String parentPath, String name, LinkRecord record) {
        validateName(name);

        DirectoryNode parent = find(parentPath)
                .orElseThrow(() -> new IllegalArgumentException("el directorio padre no existe: " + parentPath));

        if (parent.hasChild(name)) {
            throw new IllegalArgumentException("ya existe un archivo, directorio o enlace con ese nombre: " + name);
        }

        parent.addLink(name, record.target);
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

        for (Map.Entry<String, String> link : directory.getLinks().entrySet()) {
            appendLink(builder, directory, link.getKey(), link.getValue());
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

    private void appendLink(StringBuilder builder, DirectoryNode directory, String linkName, String targetPath) {
        builder.append("type=LINK")
                .append("|path=")
                .append(joinPath(directory.getPath(), linkName))
                .append("|target=")
                .append(targetPath)
                .append("\n");
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

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.contains("/") || ".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("nombre invalido: " + name);
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

    private static String joinPath(String parentPath, String name) {
        if ("/".equals(parentPath)) {
            return "/" + name;
        }

        return parentPath + "/" + name;
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
                    case "path" ->
                        path = pair[1];
                    case "owner" ->
                        owner = pair[1];
                    case "group" ->
                        group = pair[1];
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

    private static class LinkRecord {

        private final String path;
        private final String target;

        private LinkRecord(String path, String target) {
            this.path = path;
            this.target = target;
        }

        private static LinkRecord fromLine(String line) {
            Map<String, String> values = new java.util.LinkedHashMap<>();

            for (String part : line.split("\\|")) {
                String[] pair = part.split("=", 2);

                if (pair.length == 2) {
                    values.put(pair[0], pair[1]);
                }
            }

            String path = values.get("path");
            String target = values.get("target");

            if (path == null || path.isBlank() || target == null || target.isBlank()) {
                return null;
            }

            return new LinkRecord(path, target);
        }
    }
}