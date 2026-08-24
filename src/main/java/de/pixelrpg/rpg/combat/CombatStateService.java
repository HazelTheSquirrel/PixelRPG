package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.PlayerCombatEnterEvent;
import de.pixelrpg.rpg.api.events.PlayerCombatExitEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatStateService implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;

    private final GuildAPI guildAPI;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public CombatStateService(Plugin plugin, GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 20L, 20L);
    }

    // Zuständig dafür, dass jeder RPG-Schadensaustausch den beteiligten Spieler in den Combat State setzt.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayer(event.getDamager());
        Player target = event.getEntity() instanceof Player player ? player : null;
        if (attacker != null) enter(attacker);
        if (target != null) enter(target);
    }

    // Zuständig für die sofortige Bereinigung des Combat State beim Logout.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        exit(event.getPlayer());
    }

    public boolean isInCombat(UUID uuid) {
        return combatUntil.containsKey(uuid) && combatUntil.get(uuid) > System.currentTimeMillis();
    }

    public void enter(Player player) {
        if (!guildAPI.isRegistered(player.getUniqueId())) return;
        boolean wasInCombat = isInCombat(player.getUniqueId());
        combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_TIMEOUT_MILLIS);
        if (!wasInCombat) Bukkit.getPluginManager().callEvent(new PlayerCombatEnterEvent(player));
    }

    public void exit(Player player) {
        if (combatUntil.remove(player.getUniqueId()) != null) {
            Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));
        }
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        combatUntil.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        combatUntil.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));
            return true;
        });
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }
}
