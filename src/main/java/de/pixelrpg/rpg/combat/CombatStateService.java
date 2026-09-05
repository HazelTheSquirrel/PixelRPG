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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/** Tracks combat state with deadline-indexed cleanup instead of scanning every active combatant. */
public final class CombatStateService implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;

    private final GuildAPI guildAPI;
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final PriorityQueue<CombatExpiry> expiries = new PriorityQueue<>();
    private final BukkitTask cleanupTask;

    public CombatStateService(Plugin plugin, GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        Long expiry = combatUntil.get(uuid);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public void enter(Player player) {
        if (!guildAPI.isRegistered(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        boolean wasInCombat = isInCombat(uuid);
        long expiry = System.currentTimeMillis() + COMBAT_TIMEOUT_MILLIS;
        combatUntil.put(uuid, expiry);
        expiries.offer(new CombatExpiry(uuid, expiry));
        if (!wasInCombat) Bukkit.getPluginManager().callEvent(new PlayerCombatEnterEvent(player));
    }

    public void exit(Player player) {
        if (combatUntil.remove(player.getUniqueId()) != null) {
            Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));
        }
    }

    public void shutdown() {
        cleanupTask.cancel();
        combatUntil.clear();
        expiries.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        while (!expiries.isEmpty()) {
            CombatExpiry expiry = expiries.peek();
            if (expiry.expiresAt() > now) return;
            expiries.poll();
            Long currentExpiry = combatUntil.get(expiry.playerId());
            if (currentExpiry == null || currentExpiry.longValue() != expiry.expiresAt()) continue;
            combatUntil.remove(expiry.playerId());
            Player player = Bukkit.getPlayer(expiry.playerId());
            if (player != null && player.isOnline()) Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));
        }
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    private record CombatExpiry(UUID playerId, long expiresAt) implements Comparable<CombatExpiry> {
        @Override
        public int compareTo(CombatExpiry other) {
            return Long.compare(expiresAt, other.expiresAt);
        }
    }
}
