package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Awards companion progression and drives the combat behaviour of the Hazel Unique companion. */
public final class CompanionExperienceListener implements Listener {
    private static final String HAZEL_ID = "unique-hazel";
    private static final long MOB_KILL_EXPERIENCE = 50L;
    private static final long QUEST_COMPLETION_EXPERIENCE = 500L;
    private static final double HAZEL_ATTACK_RANGE = 3.5D;
    private static final double HAZEL_COMBAT_SPEED = 0.16D;
    private static final long HAZEL_ATTACK_INTERVAL_TICKS = 20L;

    private final CompanionService companionService;
    private final BukkitTask combatTask;
    private final Map<UUID, Long> nextAttackTick = new HashMap<>();

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
        if (plugin == null) throw new IllegalStateException("PixelRPG plugin is not loaded.");
        this.combatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickHazelCombat, 10L, 2L);
    }

    // Awards rarity-scaled companion XP when the player defeats a mob while a companion is active.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING)) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        Companion active = companionService.getActive(player.getUniqueId());
        if (active == null) return;
        long gained = scaledExperience(MOB_KILL_EXPERIENCE, active.rarity());
        companionService.awardExperience(player.getUniqueId(), gained);
    }

    // Awards rarity-scaled companion XP for completing a quest while the companion is active.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Companion active = companionService.getActive(event.getPlayer().getUniqueId());
        if (active == null) return;
        long gained = scaledExperience(QUEST_COMPLETION_EXPERIENCE, active.rarity());
        companionService.awardExperience(event.getPlayer().getUniqueId(), gained);
    }

    // Removes the active companion entity when its owner leaves the server.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        nextAttackTick.removeIf((entityId, ignored) -> Bukkit.getEntity(entityId) == null);
        companionService.clearActive(event.getPlayer().getUniqueId());
    }

    private void tickHazelCombat() {
        int currentTick = Bukkit.getCurrentTick();

        for (var world : Bukkit.getWorlds()) {
            for (Mannequin mannequin : world.getEntitiesByClass(Mannequin.class)) {
                String companionId = mannequin.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
                if (!HAZEL_ID.equals(companionId) || !mannequin.isValid()) continue;

                applyHazelVisuals(mannequin);
                equipHazel(mannequin);

                LivingEntity target = findNearestEnemy(mannequin);
                if (target == null) {
                    nextAttackTick.remove(mannequin.getUniqueId());
                    returnToOwner(mannequin);
                    continue;
                }

                double distanceSquared = mannequin.getLocation().distanceSquared(target.getLocation());
                if (distanceSquared > HAZEL_ATTACK_RANGE * HAZEL_ATTACK_RANGE) {
                    moveTowards(mannequin, target, HAZEL_COMBAT_SPEED);
                    continue;
                }

                mannequin.setVelocity(mannequin.getVelocity().multiply(0.2D));
                mannequin.setRotation(yawTowards(mannequin, target), mannequin.getPitch());

                long nextAttack = nextAttackTick.getOrDefault(mannequin.getUniqueId(), 0L);
                if (currentTick < nextAttack) continue;

                mannequin.attack(target);
                mannequin.swingMainHand();
                nextAttackTick.put(mannequin.getUniqueId(), (long) currentTick + HAZEL_ATTACK_INTERVAL_TICKS);
            }
        }

        nextAttackTick.entrySet().removeIf(entry -> Bukkit.getEntity(entry.getKey()) == null);
    }

    private void applyHazelVisuals(Mannequin mannequin) {
        AttributeInstance scale = mannequin.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(0.50D);
    }

    private void equipHazel(Mannequin mannequin) {
        if (mannequin.getEquipment().getItemInMainHand().getType() != Material.NETHERITE_SWORD) {
            mannequin.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD), true);
        }
    }

    private void returnToOwner(Mannequin mannequin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Companion active = companionService.getActive(player.getUniqueId());
            if (active == null || !HAZEL_ID.equals(active.id())) continue;
            UUID entityId = findActiveEntity(player.getUniqueId());
            if (!mannequin.getUniqueId().equals(entityId)) continue;
            if (mannequin.getWorld() != player.getWorld()) {
                mannequin.teleport(player.getLocation());
                return;
            }
            double distanceSquared = mannequin.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared > 576.0D) {
                mannequin.teleport(player.getLocation().clone().add(1.0D, 0.0D, 1.0D));
                mannequin.setVelocity(new Vector());
            } else if (distanceSquared > 9.0D) {
                LocationTarget target = new LocationTarget(player.getLocation().clone().add(0.0D, 0.0D, 1.5D));
                moveTowards(mannequin, target.location(), HAZEL_COMBAT_SPEED);
            } else {
                mannequin.setVelocity(mannequin.getVelocity().multiply(0.25D));
            }
            return;
        }
    }

    private UUID findActiveEntity(UUID playerId) {
        Companion active = companionService.getActive(playerId);
        if (active == null) return null;
        // The service owns the active entity mapping; use the entity currently associated with the player.
        for (Entity entity : Bukkit.getWorlds().stream().flatMap(world -> world.getEntitiesByClass(Mannequin.class).stream()).toList()) {
            String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
            if (HAZEL_ID.equals(id)) {
                // The active companion is uniquely identified by its runtime entity; ownership is verified by proximity below.
                if (entity.getLocation().distanceSquared(Bukkit.getPlayer(playerId).getLocation()) < 576.0D) return entity.getUniqueId();
            }
        }
        return null;
    }

    private LivingEntity findNearestEnemy(Mannequin mannequin) {
        LivingEntity nearest = null;
        double nearestDistance = HAZEL_ATTACK_RANGE * HAZEL_ATTACK_RANGE;

        for (Entity nearby : mannequin.getNearbyEntities(HAZEL_ATTACK_RANGE, HAZEL_ATTACK_RANGE, HAZEL_ATTACK_RANGE)) {
            if (!(nearby instanceof Enemy) || !(nearby instanceof LivingEntity living) || !living.isValid() || living.isDead()) continue;
            double distance = mannequin.getLocation().distanceSquared(living.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }

        return nearest;
    }

    private static void moveTowards(Entity entity, LocationTarget target, double speed) {
        Vector delta = target.location().toVector().subtract(entity.getLocation().toVector());
        delta.setY(0.0D);
        if (delta.lengthSquared() < 0.04D) return;
        delta.normalize().multiply(speed);
        entity.setVelocity(delta);
        entity.setRotation(yawTowards(entity, target.location()), entity.getPitch());
    }

    private static void moveTowards(Entity entity, LivingEntity target, double speed) {
        moveTowards(entity, new LocationTarget(target.getLocation()), speed);
    }

    private static float yawTowards(Entity entity, LivingEntity target) {
        return yawTowards(entity, target.getLocation());
    }

    private static float yawTowards(Entity entity, org.bukkit.Location target) {
        Vector delta = target.toVector().subtract(entity.getLocation().toVector());
        return (float) Math.toDegrees(Math.atan2(-delta.getX(), delta.getZ()));
    }

    private static long scaledExperience(long base, CompanionRarity rarity) {
        return Math.max(1L, Math.round(base * rarity.experienceMultiplier()));
    }

    private record LocationTarget(org.bukkit.Location location) {
    }
}
