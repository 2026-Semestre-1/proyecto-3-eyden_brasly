package filesystem;

import constants.SystemConstants;
import java.io.IOException;

/**
 * Lee y persiste el bitmap en los bloques reservados del disco virtual.
 */
public class BitmapStore {
    public Bitmap load(VirtualDisk disk) throws IOException {
        byte[] data = new byte[getStorageSize()];

        for (int index = 0; index < SystemConstants.BITMAP_BLOCK_COUNT; index++) {
            byte[] block = disk.readBlock(SystemConstants.BITMAP_START_BLOCK + index);
            System.arraycopy(
                    block,
                    0,
                    data,
                    index * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    block.length
            );
        }

        try {
            return Bitmap.fromBytes(data);
        } catch (IllegalArgumentException exception) {
            throw new IOException("no se pudo leer el bitmap: " + exception.getMessage(), exception);
        }
    }

    public void save(VirtualDisk disk, Bitmap bitmap) throws IOException {
        byte[] data;

        try {
            data = bitmap.toBytes(getStorageSize());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("no se pudo guardar el bitmap: " + exception.getMessage(), exception);
        }

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

    private int getStorageSize() {
        return SystemConstants.BITMAP_BLOCK_COUNT * SystemConstants.VIRTUAL_DISK_BLOCK_SIZE;
    }
}
