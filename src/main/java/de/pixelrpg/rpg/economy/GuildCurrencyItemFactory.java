package de.pixelrpg.rpg.economy;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class GuildCurrencyItemFactory {
    private static int maxStackSize = 64;

    private GuildCurrencyItemFactory() {}

    public static void configureMaxStackSize(int value) { maxStackSize = Math.max(1, value); }

    public static ItemStack createSingleStack(long amount) {
        int clamped = (int) Math.max(1, Math.min(maxStackSize, amount));
        ItemStack item = new ItemStack(Material.SUNFLOWER, clamped);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Goldtaler", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("1 Goldtaler = 1 Gold", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("In der Bank einzahlen oder bei Tod verlieren.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(RPGKeys.Economy.guildGold(), PersistentDataType.DOUBLE, 1.0);
        item.setItemMeta(meta);
        return item;
    }

    public static List<ItemStack> createStacks(long totalAmount) {
        List<ItemStack> stacks = new ArrayList<>();
        long remaining = totalAmount;
        while (remaining > 0) {
            long take = Math.min(maxStackSize, remaining);
            stacks.add(createSingleStack(take));
            remaining -= take;
        }
        return stacks;
    }

    public static boolean isCurrency(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(RPGKeys.Economy.guildGold(), PersistentDataType.DOUBLE);
    }

    public static double readAmount(ItemStack item) {
        if (!isCurrency(item)) return 0.0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Economy.guildGold(), PersistentDataType.DOUBLE, 1.0);
    }
}