/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 *
 * @author eyden
 */
import constants.SystemConstants;
import filesystem.AllocationStrategy;
import filesystem.FileSystem;
import java.io.IOException;

public class FormatService {
    public FileSystem format(int sizeMB, String rootPassword) throws IOException {
        return format(SystemConstants.VIRTUAL_DISK_FILE_NAME, sizeMB, AllocationStrategy.INDEXED, rootPassword);
    }

    public FileSystem format(String diskName, int sizeMB, AllocationStrategy strategy, String rootPassword) throws IOException {
        filesystem.FormatService service = new filesystem.FormatService();
        return service.format(diskName, sizeMB, strategy, rootPassword);
    }
}