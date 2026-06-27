package filesystem;

import constants.SystemConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import security.GroupService;
import security.UserService;
import security.UserService.UserAccount;

/**
 * Persistencia de la tabla de usuarios en los bloques reservados del disco.
 *
 * @author eyden
 */
public class UserTableStore {
    public UserService load(VirtualDisk disk, GroupService groupService, String fallbackRootPasswordHash) throws IOException {
        String text = readText(disk);
        List<UserAccount> records = new ArrayList<>();

        if (!text.isBlank()) {
            if (isLegacyRootRecord(text)) {
                records.add(parseLegacyRootRecord(disk));
            } else {
                for (String line : text.split("\\R")) {
                    UserAccount account = parseLine(line);
                    if (account != null) {
                        records.add(account);
                    }
                }
            }
        }

        try {
            return UserService.fromStoredUsers(groupService, records, fallbackRootPasswordHash);
        } catch (IllegalArgumentException exception) {
            throw new IOException("tabla de usuarios invalida: " + exception.getMessage(), exception);
        }
    }

    public String readRootPasswordHash(VirtualDisk disk) throws IOException {
        String text = readText(disk);

        if (isLegacyRootRecord(text)) {
            Map<String, String> values = MBR.parseBlock(text.getBytes(StandardCharsets.UTF_8));
            String hash = values.get("passwordHash");
            if (hash != null && !hash.isBlank()) {
                return hash;
            }
        }

        for (String line : text.split("\\R")) {
            UserAccount account = parseLine(line);
            if (account != null && SystemConstants.ROOT_USERNAME.equals(account.getUsername())) {
                return account.getPasswordHash();
            }
        }

        throw new IOException("no se encontro la contrasena de root en la tabla de usuarios.");
    }

    public void save(VirtualDisk disk, UserService userService) throws IOException {
        StringBuilder builder = new StringBuilder();

        for (UserAccount user : userService.getUsers()) {
            builder.append("user=")
                    .append(clean(user.getUsername()))
                    .append("|fullName=")
                    .append(clean(user.getFullName()))
                    .append("|passwordHash=")
                    .append(clean(user.getPasswordHash()))
                    .append("|primaryGroup=")
                    .append(clean(user.getPrimaryGroup()))
                    .append("\n");
        }

        writeText(disk, builder.toString());
    }

    private boolean isLegacyRootRecord(String text) {
        return text.contains("username=") && text.contains("passwordHash=");
    }

    private UserAccount parseLegacyRootRecord(VirtualDisk disk) throws IOException {
        Map<String, String> values = MBR.parseBlock(disk.readBlock(SystemConstants.USER_TABLE_START_BLOCK));
        String username = values.getOrDefault("username", SystemConstants.ROOT_USERNAME);
        String fullName = values.getOrDefault("fullName", SystemConstants.ROOT_FULL_NAME);
        String passwordHash = values.get("passwordHash");
        String primaryGroup = values.getOrDefault("primaryGroup", SystemConstants.ROOT_GROUP);

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IOException("no se encontro la contrasena de root en la tabla de usuarios.");
        }

        return new UserAccount(username, fullName, passwordHash, primaryGroup);
    }

    private UserAccount parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String username = null;
        String fullName = null;
        String passwordHash = null;
        String primaryGroup = SystemConstants.DEFAULT_USER_GROUP;

        for (String part : line.trim().split("\\|")) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();

            switch (key) {
                case "user" -> username = value;
                case "fullName" -> fullName = value;
                case "passwordHash" -> passwordHash = value;
                case "primaryGroup" -> primaryGroup = value;
                default -> {
                    // Campos desconocidos se ignoran para mantener compatibilidad.
                }
            }
        }

        if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            return null;
        }

        if (fullName == null || fullName.isBlank()) {
            fullName = username;
        }

        return new UserAccount(username, fullName, passwordHash, primaryGroup);
    }

    private String readText(VirtualDisk disk) throws IOException {
        int totalBytes = SystemConstants.USER_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
        byte[] data = new byte[totalBytes];

        for (int index = 0; index < SystemConstants.USER_TABLE_BLOCK_COUNT; index++) {
            byte[] block = disk.readBlock(SystemConstants.USER_TABLE_START_BLOCK + index);
            System.arraycopy(block, 0, data, index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE, block.length);
        }

        return new String(data, StandardCharsets.UTF_8).replace("\0", "").trim();
    }

    private void writeText(VirtualDisk disk, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        int maxSize = SystemConstants.USER_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;

        if (data.length > maxSize) {
            throw new IOException("la tabla de usuarios excede el espacio reservado.");
        }

        byte[] padded = new byte[maxSize];
        System.arraycopy(data, 0, padded, 0, data.length);

        for (int index = 0; index < SystemConstants.USER_TABLE_BLOCK_COUNT; index++) {
            byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
            System.arraycopy(
                    padded,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block,
                    0,
                    block.length
            );
            disk.writeBlock(SystemConstants.USER_TABLE_START_BLOCK + index, block);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }
}
