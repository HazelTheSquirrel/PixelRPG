package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/** Protects the world-boss event from damaging the world and from interacting with unregistered players. */
public final class WorldBossProtectionListener implements Listener {
    private final GuildAPI guildAPI;

    public WorldBossProtectionListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
    }

    // Zuständig dafür, dass Worldbosse und ihre Adds niemals registrierte Weltblöcke durch Explosionen zerstören.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldBossEntityExplode(EntityExplodeEvent event) {
        if (isWorldBossEntity(event.getEntity())) event.blockList().clear();
    }

    // Zuständig dafür, dass eine durch ein Worldboss-Event ausgelöste Blockexplosion keine Weltblöcke zerstört.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldBossBlockExplode(BlockExplodeEvent event) {
        if (event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 8.0, 8.0, 8.0)
                .stream().anyMatch(this::isWorldBossEntity)) {
            event.blockList().clear();
        }
    }

    // Zuständig dafür, dass Enderman-ähnliche Worldboss-Mechaniken keine Blöcke aufnehmen oder platzieren können.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldBossBlockChange(EntityChangeBlockEvent event) {
        if (isWorldBossEntity(event.getEntity())) event.setCancelled(true);
    }

    // Zuständig für die strikte Trennung: Worldboss-Events dürfen nur registrierte PixelRPG-Spieler beschädigen und umgekehrt.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldBossDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        Player damagerPlayer = resolvePlayerSource(damager);
        boolean worldBossSource = isWorldBossEntity(damager) || isWorldBossProjectile(damager);

        if (worldBossSource && target instanceof Player player
                && !guildAPI.isRegistered(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (isWorldBossEntity(target) && damagerPlayer != null
                && !guildAPI.isRegistered(damagerPlayer.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player resolvePlayerSource(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    private boolean isWorldBossProjectile(Entity entity) {
        if (!(entity instanceof Projectile projectile)) return false;
        ProjectileSource source = projectile.getShooter();
        return source instanceof Entity sourceEntity && isWorldBossEntity(sourceEntity);
    }

    private boolean isWorldBossEntity(Entity entity) {
        return entity.getPersistentDataContainer().getOrDefault(
                RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, false);
    }
}
