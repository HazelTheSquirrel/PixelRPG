package de.pixelrpg.rpg.combat;

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
import java.util.TreeSet;
import java.util.UUID;

/** Tracks combat state with indexed deadlines and bounded expiry entries per player. */
public final class CombatStateService implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;

    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, CombatExpiry> expiriesByPlayer = new HashMap<>();
    private final TreeSet<CombatExpiry> expiries = new TreeSet<>();
    private final BukkitTask cleanupTask;

    public CombatStateService(Plugin plugin) {
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
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        boolean wasInCombat = isInCombat(uuid);
        long expiry = System.currentTimeMillis() + COMBAT_TIMEOUT_MILLIS;
        combatUntil.put(uuid, expiry);

        CombatExpiry previous = expiriesByPlayer.put(uuid, new CombatExpiry(uuid, expiry));
        if (previous != null) expiries.remove(previous);
        expiries.add(expiriesByPlayer.get(uuid));

        if (!wasInCombat) Bukkit.getPluginManager().callEvent(new PlayerCombatEnterEvent(player));
    }

    public void exit(Player player) {
        UUID uuid = player.getUniqueId();
        if (combatUntil.remove(uuid) != null) {
            CombatExpiry expiry = expiriesByPlayer.remove(uuid);
            if (expiry != null) expiries.remove(expiry);
            Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));
        }
    }

    public void shutdown() {
        cleanupTask.cancel();
        combatUntil.clear();
        expiriesByPlayer.clear();
        expiries.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        while (!expiries.isEmpty()) {
            CombatExpiry expiry = expiries.first();
            if (expiry.expiresAt() > now) return;
            expiries.pollFirst();

            CombatExpiry currentExpiry = expiriesByPlayer.get(expiry.playerId());
            if (currentExpiry != expiry) continue;
            expiriesByPlayer.remove(expiry.playerId());
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
            int byExpiry = Long.compare(expiresAt, other.expiresAt);
            return byExpiry != 0 ? byExpiry : playerId.compareTo(other.playerId);
        }
    }
}
