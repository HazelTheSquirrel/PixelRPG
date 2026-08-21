package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class ClassSetItemFactory {
    private ClassSetItemFactory() { }

    private static Material materialFor(PlayerClass playerClass, ClassSetSlot slot) {
        return switch (playerClass) {
            case WARRIOR -> switch (slot) {
                case WEAPON -> Material.NETHERITE_SWORD;
                case HELMET -> Material.NETHERITE_HELMET;
                case CHESTPLATE -> Material.NETHERITE_CHESTPLATE;
                case LEGGINGS -> Material.NETHERITE_LEGGINGS;
                case BOOTS -> Material.NETHERITE_BOOTS;
            };
            case RANGER -> switch (slot) {
                case WEAPON -> Material.BOW;
                case HELMET -> Material.LEATHER_HELMET;
                case CHESTPLATE -> Material.LEATHER_CHESTPLATE;
                case LEGGINGS -> Material.LEATHER_LEGGINGS;
                case BOOTS -> Material.LEATHER_BOOTS;
            };
            case ROGUE -> switch (slot) {
                case WEAPON -> Material.IRON_SWORD;
                case HELMET -> Material.CHAINMAIL_HELMET;
                case CHESTPLATE -> Material.CHAINMAIL_CHESTPLATE;
                case LEGGINGS -> Material.CHAINMAIL_LEGGINGS;
                case BOOTS -> Material.CHAINMAIL_BOOTS;
            };
            case HEALER -> switch (slot) {
                case WEAPON -> Material.STICK;
                case HELMET -> Material.GOLDEN_HELMET;
                case CHESTPLATE -> Material.GOLDEN_CHESTPLATE;
                case LEGGINGS -> Material.GOLDEN_LEGGINGS;
                case BOOTS -> Material.GOLDEN_BOOTS;
            };
            case MAGE -> switch (slot) {
                case WEAPON -> Material.BLAZE_ROD;
                case HELMET -> Material.DIAMOND_HELMET;
                case CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
                case LEGGINGS -> Material.DIAMOND_LEGGINGS;
                case BOOTS -> Material.DIAMOND_BOOTS;
            };
            default -> Material.STICK;
        };
    }

    private static String setName(PlayerClass playerClass) {
        return switch (playerClass) {
            case WARRIOR -> "Bulwark's";
            case RANGER -> "Windrunner's";
            case ROGUE -> "Shadowblade's";
            case HEALER -> "Sanctified";
            case MAGE -> "Archmage's";
            default -> "Unknown";
        };
    }

    public static ItemStack create(PlayerClass playerClass, ClassSetSlot slot, int itemLevel) {
        int safeLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, itemLevel));
        Material material = materialFor(playerClass, slot);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String slotLabel = switch (slot) {
            case WEAPON -> "Weapon";
            case HELMET -> "Helm";
            case CHESTPLATE -> "Chestguard";
            case LEGGINGS -> "Legguards";
            case BOOTS -> "Boots";
        };
        meta.displayName(Component.text(setName(playerClass) + " " + slotLabel, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        double bonus = 2.0 + safeLevel * 0.12;
        List<Component> lore = List.of(
                Component.text("Class Set: ", NamedTextColor.GRAY).append(playerClass.displayName()).decoration(TextDecoration.ITALIC, false),
                Component.text("Item Level: " + safeLevel, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("2pc: +Damage/Healing  4pc: +" + round(bonus) + "% Class Power", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);
        String instanceId = UUID.randomUUID().toString();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.classSetClass(), PersistentDataType.STRING, playerClass.name());
        pdc.set(RPGKeys.Item.classSetSlot(), PersistentDataType.STRING, slot.name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, safeLevel);
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, instanceId);
        if (slot == ClassSetSlot.WEAPON) {
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(RPGKeys.Item.attackDamageModifier(instanceId), 4.0 + safeLevel * 0.12, AttributeModifier.Operation.ADD_NUMBER));
        } else {
            meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(RPGKeys.Item.armorModifier(instanceId), 1.0 + safeLevel * 0.035, AttributeModifier.Operation.ADD_NUMBER));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
}
