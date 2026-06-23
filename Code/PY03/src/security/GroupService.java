/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

import constants.SystemConstants;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author eyden
 */
public class GroupService {
    private static final Pattern GROUP_NAME_PATTERN = Pattern.compile(SystemConstants.GROUP_NAME_REGEX);

    private final Map<String, GroupRecord> groups;

    public GroupService() {
        this.groups = new LinkedHashMap<>();
        ensureGroup(SystemConstants.ROOT_GROUP, "Grupo administrativo del sistema");
        ensureGroup(SystemConstants.DEFAULT_USER_GROUP, "Grupo general de usuarios");
    }

    public boolean addGroup(String name) {
        String normalizedName = normalizeName(name);
        validateGroupName(normalizedName);

        if (groups.containsKey(normalizedName)) {
            return false;
        }

        groups.put(normalizedName, new GroupRecord(normalizedName, "Grupo creado por el usuario"));
        return true;
    }

    public void ensureGroup(String name, String description) {
        String normalizedName = normalizeName(name);
        validateGroupName(normalizedName);
        groups.putIfAbsent(normalizedName, new GroupRecord(normalizedName, description));
    }

    public boolean exists(String name) {
        return groups.containsKey(normalizeName(name));
    }

    public Collection<GroupRecord> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private void validateGroupName(String name) {
        if (!GROUP_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("nombre de grupo invalido. Use letras minusculas, numeros, guion o guion bajo.");
        }
    }

    public static class GroupRecord {
        private final String name;
        private final String description;

        public GroupRecord(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
