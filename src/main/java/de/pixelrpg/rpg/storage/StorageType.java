// src/main/java/de/pixelrpg/rpg/storage/StorageType.java
package de.pixelrpg.rpg.storage;

public enum StorageType {
    YAML,
    MYSQL;

    public static StorageType fromString(String value) {
        try {
            return StorageType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return YAML;
        }
    }
}