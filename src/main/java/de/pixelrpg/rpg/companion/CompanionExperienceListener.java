package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Handles companion lifecycle and delegates all player-stat calculation to the central StatEngine. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService companionService;
    private final StatEngine statEngine;
    private final JavaPlugin plugin;
    private final BukkitTask passiveStatsTask;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
        this.plugin = JavaPlugin.getProvidingPlugin(CompanionExperienceListener.class);
        PixelRPGPlugin pixelRPG = PixelRPGPlugin.getInstance();
        this.statEngine = pixelRPG.getStatEngine();
        this.passiveStatsTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllPlayers, 1L, 10L);
    }

    // Restores the player's persisted active companion after the player entity is fully joined.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        companionService.restoreActive(event.getPlayer());
        refreshStats(event.getPlayer());
    }

    // Clears a companion's active runtime state when its spawned entity dies.
    @EventHandler
    public void onCompanionDeath(EntityDeathEvent event) {
        if (!isCompanion(event.getEntity())) return;
        java.util.UUID ownerUuid = companionService.getOwnerOfEntity(event.getEntity().getUniqueId());
        if (ownerUuid != null) {
            companionService.clearActive(ownerUuid);
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) refreshStats(owner);
        }
    }

    // Makes all non-Unique-Mannequin companions completely invulnerable.
    @EventHandler
    public void onCompanionDamage(EntityDamageEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Keeps passive companions from acquiring vanilla AI targets; the tamed wolf is intentionally exempt so it can react to owner combat.
    @EventHandler
    public void onCompanionTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !isCompanion(entity)) return;
        if (isUniqueMannequin(entity)) return;
        if (isTamedPixelRpgWolf(entity)) return;
        event.setCancelled(true);
        if (entity instanceof Mob mob) mob.setTarget(null);
    }

    // Prevents fire-based vanilla entities from burning while used as passive companions.
    @EventHandler
    public void onCompanionCombust(EntityCombustEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Despawns the runtime entity on quit while clearing all central stat modifiers for the leaving player.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.despawn(event.getPlayer().getUniqueId());
        statEngine.clear(event.getPlayer());
    }

    private void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureWolfTamed(player);
            refreshStats(player);
        }
    }

    private void refreshStats(Player player) {
        statEngine.recalculate(player);
    }

    private void ensureWolfTamed(Player owner) {
        Entity entity = companionService.getActiveEntity(owner.getUniqueId()) == null
                ? null
                : Bukkit.getEntity(companionService.getActiveEntity(owner.getUniqueId()));
        if (!(entity instanceof Wolf wolf) || !isCompanion(wolf)) return;
        String companionId = wolf.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (!"uncommon-wolf".equalsIgnoreCase(companionId)) return;
        if (!wolf.isTamed()) wolf.setTamed(true);
        if (wolf.getOwner() == null || !owner.getUniqueId().equals(wolf.getOwner().getUniqueId())) wolf.setOwner(owner);
        if (wolf.isSitting()) wolf.setSitting(false);
    }

    private boolean isTamedPixelRpgWolf(LivingEntity entity) {
        if (!(entity instanceof Wolf wolf) || !wolf.isTamed()) return false;
        String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        return "uncommon-wolf".equalsIgnoreCase(id);
    }

    private boolean isCompanion(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING);
    }

    private boolean isUniqueMannequin(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (id == null) return false;
        CompanionDefinition definition = companionService.definition(id);
        return definition.rarity().isUnique()
                && definition.visual().type() == CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN;
    }

    public void shutdown() {
        passiveStatsTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) statEngine.recalculate(player);
    }
}
