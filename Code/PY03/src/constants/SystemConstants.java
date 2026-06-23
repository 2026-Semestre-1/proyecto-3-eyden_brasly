package constants;

public final class SystemConstants {
    private SystemConstants() {
    }

    public static final String ROOT_USERNAME = "root";
    public static final String ROOT_FULL_NAME = "Super Usuario";
    public static final String ROOT_INITIAL_PASSWORD = "root";
    public static final String ROOT_GROUP = "root";
    public static final String DEFAULT_USER_GROUP = "users";

    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final String USERNAME_REGEX = "[a-z_][a-z0-9_-]{0,31}";
    public static final String GROUP_NAME_REGEX = "[a-z_][a-z0-9_-]{0,31}";

    public static final String VIRTUAL_DISK_FILE_NAME = "miDiscoDuro.fs";
    public static final int VIRTUAL_DISK_DEFAULT_SIZE_MB = 10;
    public static final int VIRTUAL_DISK_BLOCK_SIZE = 512;
    public static final int BYTES_PER_MEGABYTE = 1024 * 1024;

    public static final String FILE_SYSTEM_SIGNATURE = "MIFS";
    public static final String FILE_SYSTEM_NAME = "miFS";

    public static final int MBR_BLOCK = 0;
    public static final int SUPER_BLOCK = 1;
    public static final int BITMAP_START_BLOCK = 2;
    public static final int BITMAP_BLOCK_COUNT = 4;
    public static final int USER_TABLE_START_BLOCK = 6;
    public static final int GROUP_TABLE_START_BLOCK = 9;
    public static final int ROOT_DIRECTORY_START_BLOCK = 11;
    public static final int DIRECTORY_TABLE_BLOCK_COUNT = 10;
    public static final int DATA_START_BLOCK = 21;

    public static final String ROOT_HOME_PATH = "/user/root";
}
