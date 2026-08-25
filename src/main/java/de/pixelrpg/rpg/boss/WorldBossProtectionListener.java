package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

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

    // Zuständig für die strikte Trennung: Worldboss-Events dürfen nur registrierte PixelRPG-Spieler beschädigen und umgekehrt.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldBossDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        if (isWorldBossEntity(damager) && target instanceof Player player
                && !guildAPI.isRegistered(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (target instanceof Entity bossTarget && isWorldBossEntity(bossTarget)
                && damager instanceof Player player
                && !guildAPI.isRegistered(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isWorldBossEntity(Entity entity) {
        return entity.getPersistentDataContainer().getOrDefault(
                RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN, false);
    }
}
