package de.pixelrpg.rpg.storage;

import java.util.Locale;

public enum StorageType {
    YAML,
    MYSQL;

    public static StorageType fromString(String value) {
        if (value == null || value.isBlank()) {
            return YAML;
        }

        try {
            return value.trim().toUpperCase(Locale.ROOT) instanceof String normalized
                    ? StorageType.valueOf(normalized)
                    : YAML;
        } catch (IllegalArgumentException exception) {
            return YAML;
        }
    }
}
