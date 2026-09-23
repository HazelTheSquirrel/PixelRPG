package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight per-viewer RPG mob nameplates attached to the mob. */
public final class MobNameplateService {
    private static final long UPDATE_INTERVAL_TICKS = 5L;

    private final PixelRPGPlugin plugin;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, TrackedMob> activeMobs = new ConcurrentHashMap<>();

    private static final class TrackedMob {
        private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();
        private BukkitTask task;
        private long expiresAtNanos;
        private double lastHealth = Double.NaN;
        private double lastMaxHealth = Double.NaN;
        private int lastLevel = Integer.MIN_VALUE;
        private Component cachedInfo;
    }

    public MobNameplateService(PixelRPGPlugin plugin, MobScalingConfig scalingConfig) {
        this.plugin = plugin;
        this.scalingConfig = scalingConfig;
    }

    public void onPlayerHit(LivingEntity mob, UUID viewerUuid) {
        if (!mob.isValid() || mob.isDead()) return;

        UUID mobUuid = mob.getUniqueId();
        TrackedMob tracked = activeMobs.computeIfAbsent(mobUuid, ignored -> new TrackedMob());
        tracked.expiresAtNanos = System.nanoTime() + scalingConfig.getNameplateDurationTicks() * 50_000_000L;

        Player viewer = Bukkit.getPlayer(viewerUuid);
        if (viewer == null || !viewer.isOnline()) return;

        TextDisplay display = tracked.displays.computeIfAbsent(viewerUuid, ignored -> createDisplay(mob));
        viewer.showEntity(plugin, display);
        updateDisplay(mob, tracked, display);

        if (tracked.task == null) {
            tracked.task = Bukkit.getScheduler().runTaskTimer(plugin,
                    () -> updateTrackedMob(mob, mobUuid, tracked),
                    UPDATE_INTERVAL_TICKS,
                    UPDATE_INTERVAL_TICKS);
        }
    }

    private TextDisplay createDisplay(LivingEntity mob) {
        TextDisplay display = mob.getWorld().spawn(mob.getLocation(), TextDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setDefaultBackground(false);
            entity.setShadowed(true);
            entity.setSeeThrough(true);
            entity.setLineWidth(512);
            entity.setViewRange(32.0F);
        });
        mob.addPassenger(display);
        return display;
    }

    private void updateTrackedMob(LivingEntity mob, UUID mobUuid, TrackedMob tracked) {
        if (!mob.isValid() || mob.isDead() || System.nanoTime() >= tracked.expiresAtNanos) {
            stop(mobUuid, tracked);
            return;
        }

        for (Map.Entry<UUID, TextDisplay> entry : tracked.displays.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            TextDisplay display = entry.getValue();
            if (viewer == null || !viewer.isOnline() || !display.isValid()) {
                display.remove();
                tracked.displays.remove(entry.getKey(), display);
                continue;
            }
            viewer.showEntity(plugin, display);
            updateDisplay(mob, tracked, display);
        }
    }

    private void updateDisplay(LivingEntity mob, TrackedMob tracked, TextDisplay display) {
        Integer levelValue = mob.getPersistentDataContainer().get(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER);
        int level = levelValue != null
                ? Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, levelValue))
                : Level.MIN_LEVEL;
        double currentHealth = Math.max(0.0D, mob.getHealth());
        var maxHealthAttribute = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : currentHealth;
        if (tracked.cachedInfo == null || tracked.lastLevel != level || Double.compare(tracked.lastHealth, currentHealth) != 0 || Double.compare(tracked.lastMaxHealth, maxHealth) != 0) {
            tracked.lastLevel = level;
            tracked.lastHealth = currentHealth;
            tracked.lastMaxHealth = maxHealth;
            tracked.cachedInfo = buildInfo(mob, level, currentHealth, maxHealth);
        }
        display.text(tracked.cachedInfo);
    }

    private Component buildInfo(LivingEntity mob, int level, double currentHealth, double maxHealth) {
        String rawName = mob.getType().name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        String prettyName = Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);

        return Component.text("[" + level + "] ", NamedTextColor.GOLD)
                .append(Component.text(prettyName, NamedTextColor.WHITE))
                .append(Component.text("  ", NamedTextColor.GRAY))
                .append(Component.text(formatHealth(currentHealth) + "/" + formatHealth(maxHealth), NamedTextColor.RED));
    }

    private String formatHealth(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void stop(UUID mobUuid, TrackedMob tracked) {
        if (tracked.task != null) tracked.task.cancel();
        tracked.task = null;
        tracked.displays.values().forEach(TextDisplay::remove);
        tracked.displays.clear();
        activeMobs.remove(mobUuid, tracked);
    }

    public void cancelAll() {
        activeMobs.forEach(this::stop);
        activeMobs.clear();
    }
}
