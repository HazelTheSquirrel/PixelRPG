package de.pixelrpg.rpg.item;

import org.bukkit.Material;
import java.util.Locale;
import java.util.Objects;

public record ItemDefinition(
        String id, String name, Material material, ItemRarity rarity, ItemCategory category,
        int itemLevel, int requiredLevel, String weaponAbility, long weaponAbilityCooldownMillis,
        boolean soulbound, boolean unique, boolean adminOnly, String resourcepackId,
        double gearscoreModifier, String equipmentSlot, String setId) {
    public ItemDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(material);
        Objects.requireNonNull(rarity);
        Objects.requireNonNull(category);
        id = normalize(id);
        weaponAbility = weaponAbility == null ? "" : weaponAbility;
        resourcepackId = resourcepackId == null || resourcepackId.isBlank() ? id : resourcepackId;
        equipmentSlot = equipmentSlot == null ? "" : equipmentSlot.trim().toUpperCase(Locale.ROOT);
        setId = setId == null ? "" : setId.trim().toLowerCase(Locale.ROOT);
        if (itemLevel < 1 || itemLevel > 99) throw new IllegalArgumentException("itemLevel must be between 1 and 99");
        if (requiredLevel < 1 || requiredLevel > itemLevel) throw new IllegalArgumentException("invalid requiredLevel");
        if (weaponAbilityCooldownMillis < 0 || gearscoreModifier <= 0 || !Double.isFinite(gearscoreModifier)) throw new IllegalArgumentException("invalid item definition values");
    }
    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
