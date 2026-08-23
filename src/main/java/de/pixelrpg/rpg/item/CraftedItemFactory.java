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

    /** Creates a crafted item with the same complete RPG identity and stat pipeline as other PixelRPG items. */
    public static ItemStack create(String itemId, String displayName, Material material, ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (!Level.isValidNormalLevel(itemLevel)) throw new IllegalArgumentException("itemLevel must be between 1 and 99");
        if (material == null) throw new IllegalArgumentException("material must not be null");
        if (rarity == null) throw new IllegalArgumentException("rarity must not be null");

        ItemStack item = RPGItemBuilder.createItem(material, rarity, itemLevel)
                .orElseThrow(() -> new IllegalArgumentException("Material is not a supported PixelRPG item: " + material));

        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, normalizeItemId(itemId));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) {
            lore.add(Component.text(" "));
        }
        lore.add(Component.text("Crafted Item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
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
}
