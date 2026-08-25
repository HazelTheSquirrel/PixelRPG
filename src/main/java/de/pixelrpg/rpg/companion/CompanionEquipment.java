package de.pixelrpg.rpg.companion;

import org.bukkit.inventory.ItemStack;

/** Persistent equipment state for a companion. Empty slots are represented by null. */
public record CompanionEquipment(
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack mainHand,
        ItemStack offHand
) {
    public static CompanionEquipment empty() {
        return new CompanionEquipment(null, null, null, null, null, null);
    }

    public CompanionEquipment copy() {
        return new CompanionEquipment(copy(helmet), copy(chestplate), copy(leggings), copy(boots), copy(mainHand), copy(offHand));
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
