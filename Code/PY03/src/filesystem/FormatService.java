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

    /**
     * Formatea el disco virtual por defecto con el tamano especificado.
     * @param sizeMB       tamano del disco en megabytes
     * @param rootPassword contrasena del usuario root
     * @return sistema de archivos formateado y montado
     * @throws IOException si ocurre un error de escritura
     */
    public FileSystem format(int sizeMB, String rootPassword) throws IOException {
        return format(SystemConstants.VIRTUAL_DISK_FILE_NAME, sizeMB, AllocationStrategy.INDEXED, rootPassword);
    }

    /**
     * Formatea un disco virtual creando todas las estructuras del sistema de archivos:
     * MBR, superbloque, bitmap, tablas de usuarios/grupos y arbol de directorios.
     * @param diskName       nombre del archivo de disco
     * @param sizeMB         tamano del disco en megabytes
     * @param strategy       estrategia de asignacion de bloques
     * @param rootPassword   contrasena del usuario root
     * @return sistema de archivos formateado y montado
     * @throws IOException si ocurre un error de escritura
     */
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
