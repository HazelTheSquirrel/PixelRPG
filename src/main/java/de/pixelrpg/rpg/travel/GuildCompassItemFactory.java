package de.pixelrpg.rpg.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuildCompassItemFactory {
    private static final String MARKER = "pixelrpg.guild_compass";
    private GuildCompassItemFactory() {}

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Gildenkompass", NamedTextColor.LIGHT_PURPLE));
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("pixelrpg", "guild_compass"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey("pixelrpg", "guild_compass"),
                org.bukkit.persistence.PersistentDataType.BYTE);
        return value != null && value == 1;
    }
}
