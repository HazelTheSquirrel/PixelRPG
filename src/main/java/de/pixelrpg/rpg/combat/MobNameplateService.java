package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
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

/** Handles lightweight per-viewer RPG mob nameplates without a repeating teleport/update loop. */
public final class MobNameplateService {
    private final PixelRPGPlugin plugin;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, TrackedMob> activeMobs = new ConcurrentHashMap<>();

    private static final class TrackedMob {
        private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();
        private long expiresAtNanos;
        private BukkitTask expiryTask;
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
        updateDisplay(mob, display);

        if (tracked.expiryTask != null) tracked.expiryTask.cancel();
        long delay = Math.max(1L, scalingConfig.getNameplateDurationTicks());
        tracked.expiryTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireIfDue(mob, mobUuid, tracked), delay);
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

    private void updateDisplay(LivingEntity mob, TextDisplay display) {
        display.text(buildInfo(mob));
    }

    private void expireIfDue(LivingEntity mob, UUID mobUuid, TrackedMob tracked) {
        if (!mob.isValid() || mob.isDead() || System.nanoTime() >= tracked.expiresAtNanos) {
            stop(mobUuid, tracked);
            return;
        }

        long remainingNanos = tracked.expiresAtNanos - System.nanoTime();
        long remainingTicks = Math.max(1L, (remainingNanos + 49_999_999L) / 50_000_000L);
        tracked.expiryTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireIfDue(mob, mobUuid, tracked), remainingTicks);
    }

    private Component buildInfo(LivingEntity mob) {
        Integer levelValue = mob.getPersistentDataContainer().get(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER);
        int level = levelValue != null
                ? Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, levelValue))
                : Level.MIN_LEVEL;

        double currentHealth = Math.max(0.0D, mob.getHealth());
        var maxHealthAttribute = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : currentHealth;

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
        if (tracked.expiryTask != null) tracked.expiryTask.cancel();
        tracked.expiryTask = null;
        tracked.displays.values().forEach(TextDisplay::remove);
        tracked.displays.clear();
        activeMobs.remove(mobUuid, tracked);
    }

    public void cancelAll() {
        activeMobs.forEach(this::stop);
        activeMobs.clear();
    }
}
