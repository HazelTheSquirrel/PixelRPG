// src/main/java/de/pixelrpg/rpg/combat/MobNameplateService.java
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobNameplateService {

    private static final long UPDATE_INTERVAL_TICKS = 10L;

    private final PixelRPGPlugin plugin;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public MobNameplateService(PixelRPGPlugin plugin, MobScalingConfig scalingConfig) {
        this.plugin = plugin;
        this.scalingConfig = scalingConfig;
    }

    public void onPlayerHit(LivingEntity mob) {
        UUID uuid = mob.getUniqueId();

        BukkitTask existing = activeTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        updateName(mob);
        mob.setCustomNameVisible(true);

        long totalTicks = scalingConfig.getNameplateDurationTicks();
        long repeats = Math.max(1L, totalTicks / UPDATE_INTERVAL_TICKS);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long executions = 0;

            @Override
            public void run() {
                if (!mob.isValid() || mob.isDead()) {
                    BukkitTask self = activeTasks.remove(uuid);
                    if (self != null) {
                        self.cancel();
                    }
                    return;
                }

                executions++;
                if (executions >= repeats) {
                    mob.customName(null);
                    mob.setCustomNameVisible(false);
                    BukkitTask self = activeTasks.remove(uuid);
                    if (self != null) {
                        self.cancel();
                    }
                    return;
                }

                updateName(mob);
            }
        }, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);

        activeTasks.put(uuid, task);
    }

    private void updateName(LivingEntity mob) {
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

        Component name = Component.text("[" + rank.name() + "] ", rank.getColor())
                .append(Component.text(prettyName, NamedTextColor.WHITE))
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%.0f", currentHealth), NamedTextColor.RED))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f", maxHealth), NamedTextColor.RED))
                .append(Component.text(" ❤ ", NamedTextColor.RED))
                .append(Component.text(String.format("%.0f", armor), NamedTextColor.AQUA))
                .append(Component.text(" 🛡", NamedTextColor.AQUA));

        mob.customName(name);
    }

    public void cancelAll() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }
}