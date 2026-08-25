package de.pixelrpg.rpg.companion;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/** Handles right-click mounting for the owner's configured passive companion mount. */
public final class CompanionMountListener implements Listener {
    private final CompanionService companionService;
    private final CompanionRegistry registry;
    private final CompanionMountController mountController;

    public CompanionMountListener(CompanionService companionService, CompanionRegistry registry, CompanionMountController mountController) {
        this.companionService = companionService;
        this.registry = registry;
        this.mountController = mountController;
    }

    // Allows only the owning player to mount their configured companion.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompanionInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof LivingEntity living)) return;

        UUID owner = companionService.getOwnerOfEntity(living.getUniqueId());
        if (!player.getUniqueId().equals(owner)) return;

        String id = companionId(living);
        if (id == null) return;
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || !definition.mount().enabled()) return;

        event.setCancelled(true);
        mountController.tryMount(player, living, definition);
    }

    private static String companionId(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(
                de.pixelrpg.rpg.core.RPGKeys.Companion.id(),
                org.bukkit.persistence.PersistentDataType.STRING);
    }
}
