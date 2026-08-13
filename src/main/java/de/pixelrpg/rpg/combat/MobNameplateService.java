package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobNameplateService {

    private static final long UPDATE_INTERVAL_TICKS = 10L;

    private final PixelRPGPlugin plugin;
    private final MobScalingConfig scalingConfig;

    private record TrackedMob(Set<UUID> viewers, BukkitTask task) {
    }

    private final Map<UUID, TrackedMob> activeTasks = new ConcurrentHashMap<>();

    public MobNameplateService(PixelRPGPlugin plugin, MobScalingConfig scalingConfig) {
        this.plugin = plugin;
        this.scalingConfig = scalingConfig;
    }

    /**
     * Zeigt HP/Rang/Rüstungs-Infos ausschließlich per Actionbar an registrierte
     * Gildenmitglieder, die den Mob getroffen haben – niemals als sichtbaren
     * Entity-Namen, da dieser für ALLE Spieler in Sichtweite sichtbar wäre
     * (auch für nicht registrierte Spieler). So bleibt das Plugin für
     * Nicht-Mitglieder vollständig unsichtbar.
     */
    public void onPlayerHit(LivingEntity mob, UUID viewerUuid) {
        UUID mobUuid = mob.getUniqueId();

        TrackedMob existing = activeTasks.get(mobUuid);
        if (existing != null) {
            existing.viewers().add(viewerUuid);
            return;
        }

        Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        viewers.add(viewerUuid);

        long totalTicks = scalingConfig.getNameplateDurationTicks();
        long repeats = Math.max(1L, totalTicks / UPDATE_INTERVAL_TICKS);
        BukkitTask[] taskHolder = new BukkitTask[1];

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long executions = 0;

            @Override
            public void run() {
                if (!mob.isValid() || mob.isDead()) {
                    stop(mobUuid, taskHolder[0]);
                    return;
                }

                executions++;
                if (executions >= repeats) {
                    stop(mobUuid, taskHolder[0]);
                    return;
                }

                broadcastToViewers(mob, viewers);
            }
        }, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);

        taskHolder[0] = task;
        activeTasks.put(mobUuid, new TrackedMob(viewers, task));
        broadcastToViewers(mob, viewers);
    }

    private void stop(UUID mobUuid, BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
        activeTasks.remove(mobUuid);
    }

    private void broadcastToViewers(LivingEntity mob, Set<UUID> viewers) {
        Component info = buildInfo(mob);
        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null && viewer.isOnline()) {
                viewer.sendActionBar(info);
            }
        }
    }

    private Component buildInfo(LivingEntity mob) {
        Integer rankOrdinal = mob.getPersistentDataContainer()
                .get(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER);
        Rank rank = rankOrdinal != null ? Rank.fromOrdinalClamped(rankOrdinal) : Rank.F;

        double currentHealth = mob.getHealth();
        var maxHealthAttribute = mob.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : currentHealth;

        var armorAttribute = mob.getAttribute(Attribute.ARMOR);
        double armor = armorAttribute != null ? armorAttribute.getValue() : 0.0;

        String rawName = mob.getType().name().replace('_', ' ').toLowerCase();
        String prettyName = Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);

        return Component.text("[" + rank.name() + "] ", rank.getColor())
                .append(Component.text(prettyName, NamedTextColor.WHITE))
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%.0f", currentHealth), NamedTextColor.RED))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f", maxHealth), NamedTextColor.RED))
                .append(Component.text(" ❤ ", NamedTextColor.RED))
                .append(Component.text(String.format("%.0f", armor), NamedTextColor.AQUA))
                .append(Component.text(" 🛡", NamedTextColor.AQUA));
    }

    public void cancelAll() {
        activeTasks.values().forEach(t -> t.task().cancel());
        activeTasks.clear();
    }
}