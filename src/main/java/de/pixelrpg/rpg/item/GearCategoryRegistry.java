package de.pixelrpg.rpg.item;

import org.bukkit.Material;
import java.util.Optional;

public final class GearCategoryRegistry {
    private GearCategoryRegistry() {}

    public static Optional<ItemCategory> resolve(Material material) {
        String name = material.name();
        if (name.equals("SHIELD")) return Optional.of(ItemCategory.SHIELD);
        if (name.equals("FIRE_CHARGE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT")) return Optional.of(ItemCategory.RANGED_WEAPON);
        if (name.contains("SWORD") || name.contains("MACE") || (name.contains("AXE") && !name.contains("PICKAXE"))) return Optional.of(ItemCategory.MELEE_WEAPON);
        if (name.contains("HELMET") || name.contains("CAP") || name.equals("TURTLE_SHELL")) return Optional.of(ItemCategory.HELMET);
        if (name.contains("CHESTPLATE") || name.contains("TUNIC") || name.equals("ELYTRA")) return Optional.of(ItemCategory.CHESTPLATE);
        if (name.contains("LEGGINGS") || name.contains("PANTS")) return Optional.of(ItemCategory.LEGGINGS);
        if (name.contains("BOOTS")) return Optional.of(ItemCategory.BOOTS);
        if (name.contains("PICKAXE") || name.contains("SHOVEL") || name.contains("HOE") || name.contains("SHEARS") || name.contains("FISHING_ROD") || name.contains("BRUSH")) return Optional.of(ItemCategory.TOOL);
        return Optional.empty();
    }
}
