package de.pixelrpg.rpg.storage;
public enum StorageType { YAML, MYSQL; public static StorageType parse(String value){try{return value==null?YAML:valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException e){return YAML;}} }
