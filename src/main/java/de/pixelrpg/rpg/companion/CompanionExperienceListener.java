package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

/** Awards companion progression and drives the combat behaviour of the Hazel Unique companion. */
public final class CompanionExperienceListener implements Listener {
    private static final String HAZEL_ID = "unique-hazel";
    private static final long MOB_KILL_EXPERIENCE = 50L;
    private static final long QUEST_COMPLETION_EXPERIENCE = 500L;
    private static final double HAZEL_ATTACK_RANGE = 3.5D;

    private final CompanionService companionService;
    private final BukkitTask combatTask;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
        this.combatTask = Bukkit.getScheduler().runTaskTimer(
                companionService.getPlugin(),
                this::tickHazelCombat,
                10L,
                20L
        );
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
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        companionService.clearActive(event.getPlayer().getUniqueId());
    }

    private void tickHazelCombat() {
        for (var world : Bukkit.getWorlds()) {
            for (Mannequin mannequin : world.getEntitiesByClass(Mannequin.class)) {
                String companionId = mannequin.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
                if (!HAZEL_ID.equals(companionId) || !mannequin.isValid()) continue;

                equipHazel(mannequin);
                LivingEntity target = findNearestEnemy(mannequin);
                if (target == null) continue;

                mannequin.attack(target);
                mannequin.swingMainHand();
            }
        }
    }

    private void equipHazel(Mannequin mannequin) {
        if (mannequin.getEquipment() == null) return;
        if (mannequin.getEquipment().getItemInMainHand().getType() != Material.NETHERITE_SWORD) {
            mannequin.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD), true);
            mannequin.getEquipment().setItemInMainHandDropChance(0.0F);
        }
    }

    private LivingEntity findNearestEnemy(Mannequin mannequin) {
        Entity nearest = null;
        double nearestDistance = HAZEL_ATTACK_RANGE * HAZEL_ATTACK_RANGE;

        for (Entity nearby : mannequin.getNearbyEntities(HAZEL_ATTACK_RANGE, HAZEL_ATTACK_RANGE, HAZEL_ATTACK_RANGE)) {
            if (!(nearby instanceof Enemy) || !(nearby instanceof LivingEntity living) || !living.isValid() || living.isDead()) continue;
            double distance = mannequin.getLocation().distanceSquared(living.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }

        return nearest instanceof LivingEntity living ? living : null;
    }

    private static long scaledExperience(long base, CompanionRarity rarity) {
        return Math.max(1L, Math.round(base * rarity.experienceMultiplier()));
    }
}
