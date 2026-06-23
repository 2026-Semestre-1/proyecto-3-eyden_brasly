package filesystem;

import constants.SystemConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import security.GroupService;
import security.UserService;

/**
 * Servicio encargado de crear un File System nuevo sobre el disco virtual.
 * Inicializa las estructuras principales y deja persistida la informacion
 * minima para montar el sistema en ejecuciones futuras.
 */
public class FormatService {
    public FileSystem format(int sizeMB, String rootPassword) throws IOException {
        return format(SystemConstants.VIRTUAL_DISK_FILE_NAME, sizeMB, AllocationStrategy.INDEXED, rootPassword);
    }

    public FileSystem format(String diskName, int sizeMB, AllocationStrategy strategy, String rootPassword) throws IOException {
        if (sizeMB <= 0) {
            throw new IllegalArgumentException("el tamano del disco debe ser mayor a cero.");
        }

        if (rootPassword == null || rootPassword.length() < SystemConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("la contrasena de root debe tener al menos "
                    + SystemConstants.MIN_PASSWORD_LENGTH + " caracteres.");
        }

        AllocationStrategy selectedStrategy = strategy == null ? AllocationStrategy.INDEXED : strategy;
        VirtualDisk disk = new VirtualDisk(diskName, sizeMB);

        Bitmap bitmap = new Bitmap(disk.getTotalBlocks());
        reserveInternalBlocks(bitmap);

        MBR mbr = new MBR(sizeMB, disk.getTotalBlocks(), selectedStrategy);
        SuperBlock superBlock = new SuperBlock(
                disk.getDiskSize(),
                disk.getTotalBlocks(),
                bitmap.countUsedBlocks(),
                bitmap.countFreeBlocks()
        );

        String rootPasswordHash = UserService.hashPassword(SystemConstants.ROOT_USERNAME, rootPassword);

        disk.writeBlock(SystemConstants.MBR_BLOCK, mbr.toBytes());
        disk.writeBlock(SystemConstants.SUPER_BLOCK, superBlock.toBytes());
        writeBitmap(disk, bitmap);
        writeInitialGroups(disk);
        writeInitialUsers(disk, rootPasswordHash);
        writeInitialDirectories(disk);

        BlockManager blockManager = new BlockManager(bitmap);
        return new FileSystem(disk, mbr, superBlock, bitmap, blockManager, rootPasswordHash);
    }

    private void reserveInternalBlocks(Bitmap bitmap) {
        for (int block = SystemConstants.MBR_BLOCK; block < SystemConstants.DATA_START_BLOCK; block++) {
            bitmap.markUsed(block);
        }
    }

    private void writeBitmap(VirtualDisk disk, Bitmap bitmap) throws IOException {
        int bitmapBytes = SystemConstants.BITMAP_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
        byte[] data = bitmap.toBytes(bitmapBytes);

        for (int index = 0; index < SystemConstants.BITMAP_BLOCK_COUNT; index++) {
            byte[] block = new byte[SystemConstants.VIRTUAL_DISK_BLOCK_SIZE];
            System.arraycopy(
                    data,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block,
                    0,
                    block.length
            );
            disk.writeBlock(SystemConstants.BITMAP_START_BLOCK + index, block);
        }
    }

    private void writeInitialGroups(VirtualDisk disk) throws IOException {
        GroupService groupService = new GroupService();
        StringBuilder data = new StringBuilder();

        groupService.getGroups().forEach(group -> data
                .append("group=")
                .append(group.getName())
                .append(",description=")
                .append(group.getDescription())
                .append("\n"));

        writeTextBlock(disk, SystemConstants.GROUP_TABLE_START_BLOCK, data.toString());
    }

    private void writeInitialUsers(VirtualDisk disk, String rootPasswordHash) throws IOException {
        String data = ""
                + "username=" + SystemConstants.ROOT_USERNAME + "\n"
                + "fullName=" + SystemConstants.ROOT_FULL_NAME + "\n"
                + "passwordHash=" + rootPasswordHash + "\n"
                + "primaryGroup=" + SystemConstants.ROOT_GROUP + "\n";

        writeTextBlock(disk, SystemConstants.USER_TABLE_START_BLOCK, data);
    }

    private void writeInitialDirectories(VirtualDisk disk) throws IOException {
        String data = ""
                + "path=/,owner=" + SystemConstants.ROOT_USERNAME + ",group=" + SystemConstants.ROOT_GROUP + "\n"
                + "path=/user,owner=" + SystemConstants.ROOT_USERNAME + ",group=" + SystemConstants.ROOT_GROUP + "\n"
                + "path=" + SystemConstants.ROOT_HOME_PATH + ",owner=" + SystemConstants.ROOT_USERNAME
                + ",group=" + SystemConstants.ROOT_GROUP + "\n";

        writeTextBlock(disk, SystemConstants.ROOT_DIRECTORY_START_BLOCK, data);
    }

    private void writeTextBlock(VirtualDisk disk, int blockNumber, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        if (data.length > disk.getBlockSize()) {
            throw new IOException("la estructura inicial excede el tamano del bloque " + blockNumber + ".");
        }

        disk.writeBlock(blockNumber, data);
    }
}
