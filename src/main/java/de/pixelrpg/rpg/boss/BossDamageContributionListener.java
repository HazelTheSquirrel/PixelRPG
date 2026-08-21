package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/** Records only successfully applied damage for real boss participation rewards. */
public final class BossDamageContributionListener implements Listener {
    private final BossManager bossManager;
    private final GuildAPI guildAPI;

    public BossDamageContributionListener(BossManager bossManager, GuildAPI guildAPI) {
        this.bossManager = bossManager;
        this.guildAPI = guildAPI;
    }

    // Zuständig für das Erfassen tatsächlich angewendeten Schadens als Boss-Teilnahme.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) attacker = player;
        }
        if (attacker == null || !guildAPI.isRegistered(attacker.getUniqueId())) return;
        bossManager.recordDamage(event.getEntity().getUniqueId(), attacker.getUniqueId(), event.getFinalDamage());
    }
}
