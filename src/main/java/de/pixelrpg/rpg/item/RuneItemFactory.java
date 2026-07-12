// src/main/java/de/pixelrpg/rpg/item/RuneItemFactory.java
package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class RuneItemFactory {

    private RuneItemFactory() {
    }

    public static ItemStack create(RuneType runeType, double value) {
        Material material = switch (runeType) {
            case DAMAGE -> Material.BLAZE_POWDER;
            case CRIT -> Material.AMETHYST_SHARD;
            case LIFESTEAL -> Material.GHAST_TEAR;
            case ARMOR -> Material.PRISMARINE_SHARD;
            case VITALITY -> Material.GLISTERING_MELON_SLICE;
            case REGENERATION -> Material.GLOW_BERRIES;
            case HASTE -> Material.SUGAR;
            default -> throw new IllegalArgumentException("Unexpected value: " + runeType);
};

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(runeType.displayName().decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Value: +" + value, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" "),
                Component.text("Drag onto a weapon or armor", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("piece with an open socket.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.runeType(), PersistentDataType.STRING, runeType.name());
        pdc.set(RPGKeys.Item.runeValue(), PersistentDataType.DOUBLE, value);
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRandom() {
        RuneType type = RuneType.values()[(int) (Math.random() * RuneType.values().length)];
        return create(type, type.rollValue());
    }
}