package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SoulboundService {
    public enum Result { SUCCESS, ALREADY_SOULBOUND, NOT_IDENTIFIED }
    private SoulboundService() {}

    public static boolean isSoulbound(ItemStack item) {
        return item != null && item.hasItemMeta()
                && Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN));
    }

    public static Result apply(ItemStack item) {
        if (item == null || item.isEmpty()) return Result.NOT_IDENTIFIED;
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        if (!Boolean.TRUE.equals(pdc.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN))) return Result.NOT_IDENTIFIED;
        if (Boolean.TRUE.equals(pdc.get(RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN))) return Result.ALREADY_SOULBOUND;
        pdc.set(RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN, true);
        List<Component> lore = meta.lore();
        List<Component> newLore = new ArrayList<>();
        newLore.add(Component.text("⚡ Soulbound", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        if (lore != null) newLore.addAll(lore);
        meta.lore(newLore);
        item.setItemMeta(meta);
        return Result.SUCCESS;
    }
}
