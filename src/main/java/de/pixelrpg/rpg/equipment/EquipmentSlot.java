package de.pixelrpg.rpg.equipment;

public enum EquipmentSlot {
    HELMET(org.bukkit.inventory.EquipmentSlot.HEAD),
    CHEST(org.bukkit.inventory.EquipmentSlot.CHEST),
    LEGS(org.bukkit.inventory.EquipmentSlot.LEGS),
    FEET(org.bukkit.inventory.EquipmentSlot.FEET),
    MAINHAND(org.bukkit.inventory.EquipmentSlot.HAND),
    OFFHAND(org.bukkit.inventory.EquipmentSlot.OFF_HAND);

    private final org.bukkit.inventory.EquipmentSlot bukkitSlot;
    EquipmentSlot(org.bukkit.inventory.EquipmentSlot bukkitSlot) { this.bukkitSlot = bukkitSlot; }
    public org.bukkit.inventory.EquipmentSlot bukkitSlot() { return bukkitSlot; }
}
