package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.PixelRPGPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Compatibility facade for existing boss reward call sites.
 * Item creation itself is owned exclusively by the central ItemService.
 */
public final class BossRewardItemFactory {
    private BossRewardItemFactory() {
    }

    public static Optional<ItemStack> create(String id, int level) {
        PixelRPGPlugin plugin = PixelRPGPlugin.getInstance();
        if (plugin == null || plugin.getItemService() == null) return Optional.empty();
        return plugin.getItemService().createItem(id, level);
    }
}
