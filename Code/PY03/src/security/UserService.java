/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

import constants.SystemConstants;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 *
 * @author eyden
 */
public class UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile(SystemConstants.USERNAME_REGEX);

    private final GroupService groupService;
    private final Map<String, UserAccount> users;

    public UserService(GroupService groupService) {
        this(groupService, hashPassword(SystemConstants.ROOT_USERNAME, SystemConstants.ROOT_INITIAL_PASSWORD), true);
    }

    public UserService(GroupService groupService, String rootPassword) {
        this(groupService, hashPassword(SystemConstants.ROOT_USERNAME, rootPassword), true);
    }

    private UserService(GroupService groupService, String rootPasswordHash, boolean passwordIsHashed) {
        if (groupService == null) {
            throw new IllegalArgumentException("el servicio de grupos es requerido.");
        }

        this.groupService = groupService;
        this.users = new LinkedHashMap<>();
        this.groupService.ensureGroup(SystemConstants.ROOT_GROUP, "Grupo administrativo del sistema");
        this.groupService.ensureGroup(SystemConstants.DEFAULT_USER_GROUP, "Grupo general de usuarios");
        addSystemUser(
                SystemConstants.ROOT_USERNAME,
                SystemConstants.ROOT_FULL_NAME,
                rootPasswordHash,
                SystemConstants.ROOT_GROUP
        );
    }

    public static UserService fromRootPasswordHash(GroupService groupService, String rootPasswordHash) {
        return new UserService(groupService, rootPasswordHash, true);
    }

    public static UserService fromStoredUsers(
            GroupService groupService,
            Collection<UserAccount> records,
            String fallbackRootPasswordHash
    ) {
        UserService service = new UserService(groupService, fallbackRootPasswordHash, true);

        if (!records.isEmpty()) {
            service.users.clear();

            for (UserAccount record : records) {
                service.addStoredUser(
                        record.getUsername(),
                        record.getFullName(),
                        record.getPasswordHash(),
                        record.getPrimaryGroup()
                );
            }

            if (!service.exists(SystemConstants.ROOT_USERNAME)) {
                service.addSystemUser(
                        SystemConstants.ROOT_USERNAME,
                        SystemConstants.ROOT_FULL_NAME,
                        fallbackRootPasswordHash,
                        SystemConstants.ROOT_GROUP
                );
            }
        }

        return service;
    }

    public boolean addUser(String username, String fullName, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validateFullName(fullName);
        validatePassword(password);

        if (users.containsKey(normalizedUsername)) {
            return false;
        }

        users.put(normalizedUsername, new UserAccount(
                normalizedUsername,
                fullName.trim(),
                hashPassword(normalizedUsername, password),
                SystemConstants.DEFAULT_USER_GROUP
        ));
        return true;
    }

    public boolean changePassword(String username, String newPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(newPassword);

        UserAccount account = users.get(normalizedUsername);
        if (account == null) {
            return false;
        }

        account.setPasswordHash(hashPassword(normalizedUsername, newPassword));
        return true;
    }

    public boolean authenticate(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserAccount account = users.get(normalizedUsername);

        if (account == null || password == null) {
            return false;
        }

        return account.getPasswordHash().equals(hashPassword(normalizedUsername, password));
    }

    public boolean exists(String username) {
        return users.containsKey(normalizeUsername(username));
    }

    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(normalizeUsername(username)));
    }

    public Collection<UserAccount> getUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    private void addSystemUser(String username, String fullName, String passwordHash, String primaryGroup) {
        String normalizedUsername = normalizeUsername(username);
        users.put(normalizedUsername, new UserAccount(
                normalizedUsername,
                fullName,
                passwordHash,
                primaryGroup
        ));
    }

    private void addStoredUser(String username, String fullName, String passwordHash, String primaryGroup) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validateFullName(fullName);

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("hash de contrasena invalido para " + normalizedUsername + ".");
        }

        String normalizedGroup = primaryGroup == null || primaryGroup.isBlank()
                ? SystemConstants.DEFAULT_USER_GROUP
                : primaryGroup.trim().toLowerCase();
        groupService.ensureGroup(normalizedGroup, "Grupo cargado desde disco");

        users.put(normalizedUsername, new UserAccount(
                normalizedUsername,
                fullName.trim(),
                passwordHash.trim(),
                normalizedGroup
        ));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("nombre de usuario invalido. Use letras minusculas, numeros, guion o guion bajo.");
        }
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("el nombre completo no puede estar vacio.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < SystemConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("la contrasena debe tener al menos " + SystemConstants.MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }

    public static String hashPassword(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("no se pudo preparar el hash de contrasena.", exception);
        }
    }

    public static class UserAccount {
        private final String username;
        private final String fullName;
        private String passwordHash;
        private final String primaryGroup;

        public UserAccount(String username, String fullName, String passwordHash, String primaryGroup) {
            this.username = username;
            this.fullName = fullName;
            this.passwordHash = passwordHash;
            this.primaryGroup = primaryGroup;
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public String getPrimaryGroup() {
            return primaryGroup;
        }

        private void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
}
