package filesystem;

import constants.SystemConstants;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Lee las estructuras persistidas en el disco virtual y construye el objeto
 * FileSystem montado cuando la firma del MBR es valida.
 */
public class FileSystemMounter {
    public FileSystem mount(String diskName) throws IOException {
        VirtualDisk disk = VirtualDisk.openExisting(diskName);
        MBR mbr = readMBR(disk);

        if (mbr == null || !mbr.hasValidSignature()) {
            throw new IOException("el disco no contiene una firma MIFS valida.");
        }

        SuperBlock superBlock = readSuperBlock(disk);
        Bitmap bitmap = loadBitmap(disk);
        BlockManager blockManager = new BlockManager(bitmap);
        String rootPasswordHash = readRootPasswordHash(disk);
        filesystem.nodes.DirectoryTree directoryTree = new DirectoryTableStore().load(disk);

        return new FileSystem(disk, mbr, superBlock, bitmap, blockManager, rootPasswordHash, directoryTree);
    }

    public boolean existsDisk() {
        return existsDisk(SystemConstants.VIRTUAL_DISK_FILE_NAME);
    }

    public boolean existsDisk(String diskName) {
        return new File(diskName).exists();
    }

    public boolean isValidFileSystem() {
        return isValidFileSystem(SystemConstants.VIRTUAL_DISK_FILE_NAME);
    }

    public boolean isValidFileSystem(String diskName) {
        if (!existsDisk(diskName)) {
            return false;
        }

        try {
            MBR mbr = readMBR(VirtualDisk.openExisting(diskName));
            return mbr != null && mbr.hasValidSignature();
        } catch (IOException exception) {
            return false;
        }
    }

    public MBR readMBR() throws IOException {
        return readMBR(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    public MBR readMBR(VirtualDisk disk) throws IOException {
        return MBR.fromBytes(disk.readBlock(SystemConstants.MBR_BLOCK));
    }

    public SuperBlock readSuperBlock() throws IOException {
        return readSuperBlock(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    public SuperBlock readSuperBlock(VirtualDisk disk) throws IOException {
        return SuperBlock.fromBytes(disk.readBlock(SystemConstants.SUPER_BLOCK));
    }

    public Bitmap loadBitmap() throws IOException {
        return loadBitmap(VirtualDisk.openExisting(SystemConstants.VIRTUAL_DISK_FILE_NAME));
    }

    public Bitmap loadBitmap(VirtualDisk disk) throws IOException {
        return new BitmapStore().load(disk);
    }

    private String readRootPasswordHash(VirtualDisk disk) throws IOException {
        Map<String, String> values = MBR.parseBlock(disk.readBlock(SystemConstants.USER_TABLE_START_BLOCK));
        String hash = values.get("passwordHash");

        if (hash == null || hash.isBlank()) {
            throw new IOException("no se encontro la contrasena de root en la tabla de usuarios.");
        }

        return hash;
    }
}
