package de.pixelrpg.rpg.storage;

import java.util.Locale;

public enum StorageType {
    YAML,
    MYSQL;

    public static StorageType fromString(String value) {
        try {
            return StorageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return YAML;
        }
    }
}
