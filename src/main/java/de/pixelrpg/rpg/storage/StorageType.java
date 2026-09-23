package de.pixelrpg.rpg.storage;

import java.util.Locale;

public enum StorageType {
    YAML,
    MYSQL;

    public static StorageType fromString(String value) {
        if (value == null || value.isBlank()) return YAML;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return YAML;
        }
    }
}
