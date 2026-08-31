package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Disables PixelRPG equipment stats and abilities below the required level without blocking vanilla item behavior. */
public final class ItemLevelRequirementListener implements Listener {
    private final PlayerProfileManager profileManager;

    public ItemLevelRequirementListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    /** Returns whether PixelRPG effects may be applied to the item for the player. */
    public boolean canUsePixelRpgEffects(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return true;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null) required = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (required == null || required <= 0) return true;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile == null || !profile.isRegistered() || profile.getLevel() >= required;
    }

    // Zuständig dafür, dass ein Spieler beim Verwenden eines unterlevelten PixelRPG-Items informiert wird, ohne die Vanilla-Interaktion zu blockieren.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (!canUsePixelRpgEffects(event.getPlayer(), item)) notifyRequiredLevel(event.getPlayer(), item);
    }

    private void notifyRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null) required = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (required != null) player.sendActionBar(Component.text("Benötigt Level " + required, NamedTextColor.RED));
    }
}
