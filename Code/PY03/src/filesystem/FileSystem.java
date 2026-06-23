/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import filesystem.nodes.DirectoryTree;
import java.io.IOException;

/**
 * Representa un File System ya montado sobre el disco virtual.
 * No ejecuta comandos por si mismo; conserva las estructuras cargadas desde
 * disco para que la shell y los servicios puedan consultarlas.
 * 
 * @author eyden
 */
public class FileSystem {
    private final VirtualDisk disk;
    private final MBR mbr;
    private final SuperBlock superBlock;
    private final Bitmap bitmap;
    private final BlockManager blockManager;
    private final String rootPasswordHash;
    private final DirectoryTree directoryTree;
    private final DirectoryTableStore directoryTableStore;

    public FileSystem(
            VirtualDisk disk,
            MBR mbr,
            SuperBlock superBlock,
            Bitmap bitmap,
            BlockManager blockManager,
            String rootPasswordHash,
            DirectoryTree directoryTree
    ) {
        this.disk = disk;
        this.mbr = mbr;
        this.superBlock = superBlock;
        this.bitmap = bitmap;
        this.blockManager = blockManager;
        this.rootPasswordHash = rootPasswordHash;
        this.directoryTree = directoryTree;
        this.directoryTableStore = new DirectoryTableStore();
    }

    public VirtualDisk getDisk() {
        return disk;
    }

    public MBR getMbr() {
        return mbr;
    }

    public SuperBlock getSuperBlock() {
        return superBlock;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public String getRootPasswordHash() {
        return rootPasswordHash;
    }

    public DirectoryTree getDirectoryTree() {
        return directoryTree;
    }

    public void saveDirectories() throws IOException {
        directoryTableStore.save(disk, directoryTree);
    }
}
