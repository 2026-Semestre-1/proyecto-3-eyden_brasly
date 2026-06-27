package filesystem;

import constants.SystemConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import security.GroupService;
import security.GroupService.GroupRecord;

/**
 * Persistencia de la tabla de grupos en los bloques reservados del disco.
 *
 * @author eyden
 */
public class GroupTableStore {
    public GroupService load(VirtualDisk disk) throws IOException {
        String text = readText(disk);
        List<GroupRecord> records = new ArrayList<>();

        if (!text.isBlank()) {
            for (String line : text.split("\\R")) {
                GroupRecord record = parseLine(line);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        try {
            return GroupService.fromStoredGroups(records);
        } catch (IllegalArgumentException exception) {
            throw new IOException("tabla de grupos invalida: " + exception.getMessage(), exception);
        }
    }

    public void save(VirtualDisk disk, GroupService groupService) throws IOException {
        StringBuilder builder = new StringBuilder();

        for (GroupRecord group : groupService.getGroups()) {
            builder.append("group=")
                    .append(clean(group.getName()))
                    .append("|description=")
                    .append(clean(group.getDescription()))
                    .append("\n");
        }

        writeText(disk, builder.toString());
    }

    private GroupRecord parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String normalized = line.trim().replace(",description=", "|description=");
        String groupName = null;
        String description = "Grupo cargado desde disco";

        for (String part : normalized.split("\\|")) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();

            if ("group".equals(key)) {
                groupName = value;
            } else if ("description".equals(key)) {
                description = value;
            }
        }

        if (groupName == null || groupName.isBlank()) {
            return null;
        }

        return new GroupRecord(groupName, description);
    }

    private String readText(VirtualDisk disk) throws IOException {
        int totalBytes = SystemConstants.GROUP_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
        byte[] data = new byte[totalBytes];

        for (int index = 0; index < SystemConstants.GROUP_TABLE_BLOCK_COUNT; index++) {
            byte[] block = disk.readBlock(SystemConstants.GROUP_TABLE_START_BLOCK + index);
            System.arraycopy(block, 0, data, index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE, block.length);
        }

        return new String(data, StandardCharsets.UTF_8).replace("\0", "").trim();
    }

    private void writeText(VirtualDisk disk, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        int maxSize = SystemConstants.GROUP_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;

        if (data.length > maxSize) {
            throw new IOException("la tabla de grupos excede el espacio reservado.");
        }

        byte[] padded = new byte[maxSize];
        System.arraycopy(data, 0, padded, 0, data.length);

        for (int index = 0; index < SystemConstants.GROUP_TABLE_BLOCK_COUNT; index++) {
            byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
            System.arraycopy(
                    padded,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block,
                    0,
                    block.length
            );
            disk.writeBlock(SystemConstants.GROUP_TABLE_START_BLOCK + index, block);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }
}
