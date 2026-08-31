package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Provides the shared level check used to disable PixelRPG effects while retaining vanilla item behavior. */
public final class ItemLevelRequirementListener {
    private final PlayerProfileManager profileManager;

    public ItemLevelRequirementListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    /** Returns true when PixelRPG stats and abilities may be applied to the item for the player. */
    public boolean canUsePixelRpgEffects(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return true;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null) required = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (required == null || required <= 0) return true;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile == null || !profile.isRegistered() || profile.getLevel() >= required;
    }
}
