package de.pixelrpg.rpg.player;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Persistent player-owned companion state; definition data remains in the companion registry. */
public record CompanionState(
        String id,
        String name,
        int level,
        long experience,
        boolean unlocked,
        boolean active,
        Map<String, ItemStack> equipment,
        String skinValue,
        String skinSignature
) {
    public CompanionState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        equipment = copyEquipment(equipment);
        skinValue = skinValue == null ? "" : skinValue;
        skinSignature = skinSignature == null ? "" : skinSignature;
        if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
        if (experience < 0L) throw new IllegalArgumentException("experience must not be negative");
    }

    public CompanionState withName(String value) { return new CompanionState(id, value, level, experience, unlocked, active, equipment, skinValue, skinSignature); }
    public CompanionState withActive(boolean value) { return new CompanionState(id, name, level, experience, unlocked, value, equipment, skinValue, skinSignature); }
    public CompanionState withProgress(int value, long xp) { return new CompanionState(id, name, value, xp, unlocked, active, equipment, skinValue, skinSignature); }
    public CompanionState withEquipment(Map<String, ItemStack> value) { return new CompanionState(id, name, level, experience, unlocked, active, value, skinValue, skinSignature); }
    public CompanionState withSkin(String value, String signature) { return new CompanionState(id, name, level, experience, unlocked, active, equipment, value, signature); }

    public Map<String, ItemStack> equipmentCopy() { return copyEquipment(equipment); }

    private static Map<String, ItemStack> copyEquipment(Map<String, ItemStack> source) {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        if (source != null) source.forEach((slot, item) -> {
            if (slot != null && item != null && !item.isEmpty()) result.put(slot, item.clone());
        });
        return Map.copyOf(result);
    }
}
