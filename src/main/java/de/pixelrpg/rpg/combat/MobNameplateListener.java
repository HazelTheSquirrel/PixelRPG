// src/main/java/de/pixelrpg/rpg/combat/MobNameplateListener.java
package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

public final class MobNameplateListener implements Listener {

    private final MobNameplateService nameplateService;

    public MobNameplateListener(MobNameplateService nameplateService) {
        this.nameplateService = nameplateService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!target.getPersistentDataContainer().has(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER)) {
            return;
        }

        boolean damagedByPlayer = event.getDamager() instanceof Player;

        if (!damagedByPlayer && event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            damagedByPlayer = shooter instanceof Player;
        }

        if (damagedByPlayer) {
            nameplateService.onPlayerHit(target);
        }
    }
}