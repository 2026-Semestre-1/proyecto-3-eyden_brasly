/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem;

import app.TerminalSession;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.io.IOException;
import security.GroupService;
import security.UserService;

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
    private final BitmapStore bitmapStore;
    private final GroupTableStore groupTableStore;
    private final UserTableStore userTableStore;
    private final OpenFileTable openFileTable;

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
        this.bitmapStore = new BitmapStore();
        this.groupTableStore = new GroupTableStore();
        this.userTableStore = new UserTableStore();
        this.openFileTable = new OpenFileTable();
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

    public OpenFileTable getOpenFileTable() {
        return openFileTable;
    }

    public FileContentService getFileContentService() {
        return new FileContentService(this);
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

    public void saveBitmap() throws IOException {
        bitmapStore.save(disk, bitmap);
    }

    public GroupService loadGroupService() throws IOException {
        return groupTableStore.load(disk);
    }

    public UserService loadUserService(GroupService groupService) throws IOException {
        return userTableStore.load(disk, groupService, rootPasswordHash);
    }

    public void saveGroups(GroupService groupService) throws IOException {
        groupTableStore.save(disk, groupService);
    }

    public void saveUsers(UserService userService) throws IOException {
        userTableStore.save(disk, userService);
    }

    public boolean openFile(FileNode file, String username, String mode, TerminalSession session) throws IOException {
        String path = file.getFullPath();

        if (session.isFileOpenInProcess(path)) {
            return false;
        }

        if (openFileTable.getOpenCount(path) > 0) {
            return false;
        }

        int previousCount = openFileTable.getOpenCount(path);
        openFileTable.openFile(path);
        session.addProcessOpenFile(path, username, mode);

        if (previousCount == 0) {
            file.setOpen(true);
        }

        try {
            saveDirectories();
            return true;
        } catch (IOException exception) {
            openFileTable.closeFile(path);
            session.removeProcessOpenFile(path);
            if (openFileTable.getOpenCount(path) == 0) {
                file.setOpen(false);
            }
            throw exception;
        }
    }

    public void closeFile(FileNode file, TerminalSession session) throws IOException {
        String path = file.getFullPath();
        session.removeProcessOpenFile(path);
        int remaining = openFileTable.closeFile(path);
        if (remaining == 0) {
            file.setOpen(false);
        }
        saveDirectories();
    }
}
