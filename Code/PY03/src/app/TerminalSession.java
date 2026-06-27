/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

import constants.SystemConstants;
import filesystem.FileSystem;
import filesystem.OpenFile;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import security.GroupService;
import security.UserService;
import security.UserService.UserAccount;

/**
 *
 * @author eyden
 */
public class TerminalSession {
    private final String diskName;
    private GroupService groupService;
    private UserService userService;
    private FileSystem fileSystem;
    private UserAccount activeUser;
    private SystemMode mode;
    private String currentPath;
    private boolean running;
    private final Map<String, OpenFile> processOpenFiles;

    public TerminalSession() {
        this(SystemConstants.VIRTUAL_DISK_FILE_NAME);
    }

    public TerminalSession(String diskName) {
        this.diskName = diskName;
        this.mode = SystemMode.NO_FORMATTED;
        this.currentPath = "/";
        this.running = true;
        this.processOpenFiles = new LinkedHashMap<>();
    }

    public void mount(FileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
        this.groupService = fileSystem.loadGroupService();
        this.userService = fileSystem.loadUserService(groupService);
        this.activeUser = userService.findByUsername(SystemConstants.ROOT_USERNAME).orElseThrow();
        this.currentPath = SystemConstants.ROOT_HOME_PATH;
        this.mode = SystemMode.MOUNTED;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public UserService getUserService() {
        return userService;
    }

    public UserAccount getActiveUser() {
        return activeUser;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public SystemMode getMode() {
        return mode;
    }

    public String getDiskName() {
        return diskName;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public boolean isPrivileged() {
        return activeUser != null && SystemConstants.ROOT_USERNAME.equals(activeUser.getUsername());
    }

    public boolean switchUser(String username) {
        return userService.findByUsername(username)
                .map(user -> {
                    activeUser = user;
                    return true;
                })
                .orElse(false);
    }

    public boolean isFileOpenInProcess(String path) {
        return processOpenFiles.containsKey(path);
    }

    public boolean addProcessOpenFile(String path, String username, String mode) {
        if (processOpenFiles.containsKey(path)) {
            return false;
        }
        processOpenFiles.put(path, new OpenFile(path, username, mode));
        return true;
    }

    public boolean removeProcessOpenFile(String path) {
        return processOpenFiles.remove(path) != null;
    }

    public Collection<OpenFile> getProcessOpenFiles() {
        return Collections.unmodifiableCollection(processOpenFiles.values());
    }

    public String getPrompt() {
        if (mode == SystemMode.NO_FORMATTED) {
            return SystemConstants.FILE_SYSTEM_NAME + "(init)> ";
        }

        return activeUser.getUsername() + "@" + SystemConstants.FILE_SYSTEM_NAME + ":" + currentPath + "$ ";
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
