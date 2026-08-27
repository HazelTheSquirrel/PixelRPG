package de.pixelrpg.rpg.companion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** Handles owner-only mounting and the native tamed-wolf companion combat behavior. */
public final class CompanionMountListener implements Listener {
    private static final String WOLF_ID = "uncommon-wolf";
    private final CompanionService companionService;
    private final CompanionRegistry registry;
    private final CompanionMountController mountController;
    private final BukkitTask wolfMaintenanceTask;

    public CompanionMountListener(CompanionService companionService, CompanionRegistry registry, CompanionMountController mountController) {
        this.companionService = companionService;
        this.registry = registry;
        this.mountController = mountController;
        Plugin plugin = de.pixelrpg.rpg.PixelRPGPlugin.getInstance();
        this.wolfMaintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::maintainWolves, 1L, 10L);
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

    // Switches the owner's tamed wolf into attack mode when the owner attacks or is attacked.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerCombat(EntityDamageByEntityEvent event) {
        Player owner = null;
        LivingEntity target = null;

        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity living) {
            owner = player;
            target = living;
        } else if (event.getEntity() instanceof Player player) {
            owner = player;
            target = resolveDamager(event.getDamager());
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player
                && event.getEntity() instanceof LivingEntity living) {
            owner = player;
            target = living;
        }

        if (owner == null || target == null || target instanceof Player || target.isDead()) return;
        UUID entityId = companionService.getActiveEntity(owner.getUniqueId());
        if (entityId == null) return;
        Entity entity = owner.getServer().getEntity(entityId);
        if (!(entity instanceof Wolf wolf) || !WOLF_ID.equals(companionId(wolf))) return;
        ensureTamed(wolf, owner);
        wolf.setTarget(target);
    }

    // Maintains ownership, taming and safe target cleanup for every active wolf companion.
    private void maintainWolves() {
        for (Player owner : Bukkit.getOnlinePlayers()) {
            UUID entityId = companionService.getActiveEntity(owner.getUniqueId());
            if (entityId == null) continue;
            Entity entity = owner.getServer().getEntity(entityId);
            if (!(entity instanceof Wolf wolf) || !WOLF_ID.equals(companionId(wolf))) continue;
            ensureTamed(wolf, owner);
            LivingEntity target = wolf.getTarget();
            if (target == null || target.isDead() || !target.isValid()
                    || target instanceof Player
                    || target.getWorld() != owner.getWorld()
                    || target.getLocation().distanceSquared(owner.getLocation()) > 576.0D) {
                wolf.setTarget(null);
            }
        }
    }

    private static void ensureTamed(Wolf wolf, Player owner) {
        if (!wolf.isTamed()) wolf.setTamed(true);
        if (wolf.getOwner() != owner) wolf.setOwner(owner);
        wolf.setSitting(false);
        wolf.setAware(true);
    }

    private static LivingEntity resolveDamager(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private static String companionId(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(
                de.pixelrpg.rpg.core.RPGKeys.Companion.id(),
                org.bukkit.persistence.PersistentDataType.STRING);
    }
}
