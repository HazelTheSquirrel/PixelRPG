package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.EquipmentSlotGroup;

public enum ItemCategory {
    MELEE_WEAPON(ItemStatProfile.WEAPON, EquipmentSlotGroup.MAINHAND, Component.text("Melee Weapon", NamedTextColor.RED)),
    RANGED_WEAPON(ItemStatProfile.WEAPON, EquipmentSlotGroup.MAINHAND, Component.text("Ranged Weapon", NamedTextColor.RED)),
    HELMET(ItemStatProfile.ARMOR, EquipmentSlotGroup.HEAD, Component.text("Helmet", NamedTextColor.BLUE)),
    CHESTPLATE(ItemStatProfile.ARMOR, EquipmentSlotGroup.CHEST, Component.text("Chestplate", NamedTextColor.BLUE)),
    LEGGINGS(ItemStatProfile.ARMOR, EquipmentSlotGroup.LEGS, Component.text("Leggings", NamedTextColor.BLUE)),
    BOOTS(ItemStatProfile.ARMOR, EquipmentSlotGroup.FEET, Component.text("Boots", NamedTextColor.BLUE)),
    SHIELD(ItemStatProfile.SHIELD, EquipmentSlotGroup.OFFHAND, Component.text("Shield", NamedTextColor.AQUA)),
    TOOL(ItemStatProfile.TOOL, EquipmentSlotGroup.MAINHAND, Component.text("Tool", NamedTextColor.YELLOW)),
    FOOD(ItemStatProfile.FOOD, EquipmentSlotGroup.ANY, Component.text("Food", NamedTextColor.GREEN));

    private final ItemStatProfile profile;
    private final EquipmentSlotGroup slotGroup;
    private final Component displayName;

    ItemCategory(ItemStatProfile profile, EquipmentSlotGroup slotGroup, Component displayName) {
        this.profile = profile;
        this.slotGroup = slotGroup;
        this.displayName = displayName;
    }

    public ItemStatProfile getProfile() { return profile; }
    public EquipmentSlotGroup getSlotGroup() { return slotGroup; }
    public Component displayName() { return displayName; }
}
