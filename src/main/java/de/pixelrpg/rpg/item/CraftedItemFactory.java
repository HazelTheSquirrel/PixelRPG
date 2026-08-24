package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.Level;
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
import java.util.UUID;

public final class CraftedItemFactory {
    private CraftedItemFactory() {
    }

    /** Creates a crafted item with the standard PixelRPG identity and stat pipeline. UNIQUE items are admin-only. */
    public static ItemStack create(String itemId, String displayName, Material material, ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (!Level.isValidNormalLevel(itemLevel)) throw new IllegalArgumentException("itemLevel must be between 1 and 99");
        if (material == null) throw new IllegalArgumentException("material must not be null");
        if (rarity == null) throw new IllegalArgumentException("rarity must not be null");
        if (rarity == ItemRarity.UNIQUE) throw new IllegalArgumentException("UNIQUE items can only be granted by an administrator");

        ItemStack item = RPGItemBuilder.createItem(material, rarity, itemLevel)
                .orElseThrow(() -> new IllegalArgumentException("Material is not a supported PixelRPG item: " + material));

        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, normalizeItemId(itemId));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.unique(), PersistentDataType.BOOLEAN, false);
        double gearscore = Math.round(itemLevel * rarity.getStatMultiplier() * 10.0D) / 10.0D;
        pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE, gearscore);

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) lore.add(Component.text(" "));
        lore.add(Component.text("Crafted Item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Gearscore " + format(gearscore), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.displayName(Component.text(displayName, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static String normalizeItemId(String itemId) {
        String normalized = itemId.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
