package de.pixelrpg.rpg.item;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;

/** Immutable, data-driven definition of one concrete PixelRPG item. */
public record ItemDefinition(
        String id,
        String name,
        Material material,
        ItemRarity rarity,
        ItemCategory category,
        int itemLevel,
        int requiredLevel,
        String weaponAbility,
        long weaponAbilityCooldownMillis,
        boolean soulbound,
        boolean unique,
        boolean adminOnly,
        String resourcepackId,
        double gearscoreModifier
) {
    public ItemDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(category, "category");
        id = normalize(id);
        if (itemLevel < 1 || itemLevel > 99) throw new IllegalArgumentException("itemLevel must be between 1 and 99");
        if (requiredLevel < 1 || requiredLevel > 99) throw new IllegalArgumentException("requiredLevel must be between 1 and 99");
        if (requiredLevel > itemLevel) throw new IllegalArgumentException("requiredLevel must not exceed itemLevel");
        if (unique && (!adminOnly || rarity != ItemRarity.UNIQUE)) throw new IllegalArgumentException("Unique items must be UNIQUE and adminOnly");
        if (rarity == ItemRarity.UNIQUE && !unique) throw new IllegalArgumentException("UNIQUE rarity requires unique=true");
        if (weaponAbilityCooldownMillis < 0) throw new IllegalArgumentException("weaponAbilityCooldownMillis must not be negative");
        if (gearscoreModifier <= 0.0D) throw new IllegalArgumentException("gearscoreModifier must be positive");
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
