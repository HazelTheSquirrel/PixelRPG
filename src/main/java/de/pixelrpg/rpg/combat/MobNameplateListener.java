package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
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
    private final GuildAPI guildAPI;

    public MobNameplateListener(MobNameplateService nameplateService, GuildAPI guildAPI) {
        this.nameplateService = nameplateService;
        this.guildAPI = guildAPI;
    }

    // Zuständig für die Anzeige von Level-, HP- und Rüstungsinformationen registrierter Spieler.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageMob(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!target.getPersistentDataContainer().has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) damager = player;
        }

        if (damager == null || !guildAPI.isRegistered(damager.getUniqueId())) return;
        nameplateService.onPlayerHit(target, damager.getUniqueId());
    }
}
