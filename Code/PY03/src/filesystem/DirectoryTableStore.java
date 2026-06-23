package filesystem;

import constants.SystemConstants;
import filesystem.nodes.DirectoryTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Persistencia de la tabla de directorios en los bloques reservados del disco.
 */
public class DirectoryTableStore {
    public DirectoryTree load(VirtualDisk disk) throws IOException {
        byte[] data = new byte[SystemConstants.DIRECTORY_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];

        for (int index = 0; index < SystemConstants.DIRECTORY_TABLE_BLOCK_COUNT; index++) {
            byte[] block = disk.readBlock(SystemConstants.ROOT_DIRECTORY_START_BLOCK + index);
            System.arraycopy(
                    block,
                    0,
                    data,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block.length
            );
        }

        String text = new String(data, StandardCharsets.UTF_8).replace("\0", "").trim();
        return DirectoryTree.fromText(text);
    }

    public void save(VirtualDisk disk, DirectoryTree directoryTree) throws IOException {
        byte[] data = directoryTree.toText().getBytes(StandardCharsets.UTF_8);
        int maxSize = SystemConstants.DIRECTORY_TABLE_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;

        if (data.length > maxSize) {
            throw new IOException("la tabla de directorios excede el espacio reservado.");
        }

        byte[] padded = new byte[maxSize];
        System.arraycopy(data, 0, padded, 0, data.length);

        for (int index = 0; index < SystemConstants.DIRECTORY_TABLE_BLOCK_COUNT; index++) {
            byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
            System.arraycopy(
                    padded,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block,
                    0,
                    block.length
            );
            disk.writeBlock(SystemConstants.ROOT_DIRECTORY_START_BLOCK + index, block);
        }
    }
}
