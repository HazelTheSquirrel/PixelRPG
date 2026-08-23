package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

/** Handles passive companion protection and runtime lifecycle; passive companions no longer have XP progression. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService companionService;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
    }

    // Restores the player's persisted active companion after the player entity is fully joined.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        companionService.restoreActive(event.getPlayer());
    }

    // Clears a companion's active runtime state when its spawned entity dies.
    @EventHandler
    public void onCompanionDeath(EntityDeathEvent event) {
        if (!isCompanion(event.getEntity())) return;
        java.util.UUID ownerUuid = companionService.getOwnerOfEntity(event.getEntity().getUniqueId());
        if (ownerUuid != null) companionService.clearActive(ownerUuid);
    }

    // Makes all non-Unique-Mannequin companions completely invulnerable.
    @EventHandler
    public void onCompanionDamage(EntityDamageEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Prevents hostile vanilla AI from targeting passive companions or their owners.
    @EventHandler
    public void onCompanionTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !isCompanion(entity)) return;
        if (isUniqueMannequin(entity)) return;
        event.setCancelled(true);
        if (entity instanceof Mob mob) mob.setTarget(null);
    }

    // Prevents fire-based vanilla entities such as zombies from burning when retained as legacy definitions.
    @EventHandler
    public void onCompanionCombust(EntityCombustEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Despawns the runtime entity on quit while preserving the persisted active selection for the next join.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.despawn(event.getPlayer().getUniqueId());
    }

    private boolean isCompanion(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING);
    }

    private boolean isUniqueMannequin(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        return id != null && companionService.definition(id).rarity().isUnique()
                && companionService.definition(id).visual().type() == CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN;
    }
}
