package de.pixelrpg.rpg.equipment;

import org.bukkit.inventory.EquipmentSlot;

public enum EquipmentSlot {
    HELMET(EquipmentSlot.HEAD),
    CHEST(EquipmentSlot.CHEST),
    LEGS(EquipmentSlot.LEGS),
    FEET(EquipmentSlot.FEET),
    MAINHAND(EquipmentSlot.HAND),
    OFFHAND(EquipmentSlot.OFF_HAND);

    private final EquipmentSlot bukkitSlot;

    EquipmentSlot(EquipmentSlot bukkitSlot) {
        this.bukkitSlot = bukkitSlot;
    }

    public EquipmentSlot bukkitSlot() {
        return bukkitSlot;
    }
}
