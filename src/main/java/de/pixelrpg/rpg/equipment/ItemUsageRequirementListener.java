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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerStopUsingItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Blocks active use of PixelRPG weapons when the player is below their required level. */
public final class ItemUsageRequirementListener implements Listener {
    private final PlayerProfileManager profileManager;

    public ItemUsageRequirementListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    // Zuständig dafür, dass unterlevelte Waffenfähigkeiten und Item-Interaktionen nicht aktiviert werden.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (!isBelowRequiredLevel(player, item)) return;
        event.setCancelled(true);
        notifyRequiredLevel(player, item);
    }

    // Zuständig dafür, dass unterlevelte Fernkampfwaffen beim Loslassen der Benutzungstaste keine Fähigkeit auslösen.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStopUsingItem(PlayerStopUsingItemEvent event) {
        if (isBelowRequiredLevel(event.getPlayer(), event.getItem())) notifyRequiredLevel(event.getPlayer(), event.getItem());
    }

    // Zuständig dafür, dass unterlevelte Waffen keinen normalen oder RPG-Schaden verursachen können.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = event.getDamager() instanceof Player player ? player : null;
        if (attacker == null) return;
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!isBelowRequiredLevel(attacker, weapon)) return;
        event.setCancelled(true);
        notifyRequiredLevel(attacker, weapon);
    }

    private boolean isBelowRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null) required = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (required == null || required <= 0) return false;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        return profile != null && profile.isRegistered() && profile.getLevel() < required;
    }

    private void notifyRequiredLevel(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer required = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (required == null) required = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (required != null) player.sendActionBar(Component.text("Benötigt Level " + required, NamedTextColor.RED));
    }
}
