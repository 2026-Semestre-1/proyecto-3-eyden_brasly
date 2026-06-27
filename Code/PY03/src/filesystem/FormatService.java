package filesystem;

import constants.SystemConstants;
import filesystem.nodes.DirectoryTree;
import java.io.IOException;
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
        GroupService groupService = new GroupService();
        UserService userService = UserService.fromRootPasswordHash(groupService, rootPasswordHash);
        new GroupTableStore().save(disk, groupService);
        new UserTableStore().save(disk, userService);
        DirectoryTree directoryTree = DirectoryTree.createInitialTree();
        new DirectoryTableStore().save(disk, directoryTree);

        BlockManager blockManager = new BlockManager(bitmap);
        FileSystem fileSystem = new FileSystem(disk, mbr, superBlock, bitmap, blockManager, rootPasswordHash, directoryTree);
        FileSystemMounter.cacheFileSystem(diskName, fileSystem);
        return fileSystem;
    }

    private void reserveInternalBlocks(Bitmap bitmap) {
        for (int block = SystemConstants.MBR_BLOCK; block < SystemConstants.DATA_START_BLOCK; block++) {
            bitmap.markUsed(block);
        }
    }

    private void writeBitmap(VirtualDisk disk, Bitmap bitmap) throws IOException {
        new BitmapStore().save(disk, bitmap);
    }

}
