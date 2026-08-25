package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.combat.CombatDamageContext;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WeaponAbilityEngine {
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final Map<UUID, Long> cooldownExpiry = new ConcurrentHashMap<>();

    public WeaponAbilityEngine(PlayerProfileManager profileManager, StatEngine statEngine) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
    }

    public boolean cast(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta()) return false;
        var pdc = weapon.getItemMeta().getPersistentDataContainer();
        Integer itemLevel = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        if (itemLevel != null && profile.getLevel() < itemLevel) {
            player.sendActionBar(Component.text("Benötigt Level " + itemLevel, NamedTextColor.RED));
            return true;
        }

        Material material = weapon.getType();
        SkillDefinition skill = SkillDefinition.forMaterial(material);
        if (skill == null) return false;
        if (skill.ranged() && !player.isSneaking()) return false;

        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);
        if (now < expiry) {
            long seconds = (expiry - now + 999L) / 1000L;
            player.sendActionBar(Component.text("Ability cooldown: " + seconds + "s", NamedTextColor.GRAY));
            return true;
        }

        boolean executed = skill.execute(player, statEngine.getCachedStats(player.getUniqueId()).attackPower());
        if (executed) cooldownExpiry.put(player.getUniqueId(), now + skill.cooldownMillis());
        return executed;
    }

    public void clearCooldown(UUID uuid) {
        cooldownExpiry.remove(uuid);
    }

    private record SkillDefinition(String id, double multiplier, long cooldownMillis, boolean ranged, SkillAction action) {
        private static SkillDefinition forMaterial(Material material) {
            return switch (material) {
                case WOODEN_SWORD -> new SkillDefinition("WOODEN_SWEEP", 0.80D, 1200L, false, WeaponAbilityEngine::sweep);
                case STONE_SWORD -> new SkillDefinition("STONE_BREAKER", 1.00D, 1500L, false, WeaponAbilityEngine::breaker);
                case COPPER_SWORD -> new SkillDefinition("COPPER_SHOCK", 1.20D, 1800L, false, WeaponAbilityEngine::shock);
                case IRON_SWORD -> new SkillDefinition("IRON_HEAVY_STRIKE", 1.50D, 2200L, false, WeaponAbilityEngine::heavyStrike);
                case GOLDEN_SWORD -> new SkillDefinition("GOLDEN_RAPID_CUT", 0.70D, 700L, false, WeaponAbilityEngine::rapidCut);
                case DIAMOND_SWORD -> new SkillDefinition("DIAMOND_CLEAVE", 1.80D, 2800L, false, WeaponAbilityEngine::cleave);
                case NETHERITE_SWORD -> new SkillDefinition("NETHERITE_REND", 2.20D, 3500L, false, WeaponAbilityEngine::rend);
                case BOW -> new SkillDefinition("BOW_PIERCING_SHOT", 1.50D, 2200L, true, WeaponAbilityEngine::piercingShot);
                case CROSSBOW -> new SkillDefinition("CROSSBOW_BURST", 2.00D, 3000L, true, WeaponAbilityEngine::crossbowBurst);
                default -> null;
            };
        }

        private boolean execute(Player player, double attackPower) {
            return action.execute(player, Math.max(0.0D, attackPower * multiplier));
        }
    }

    @FunctionalInterface
    private interface SkillAction {
        boolean execute(Player player, double bonusDamage);
    }

    private static boolean sweep(Player player, double damage) {
        return areaDamage(player, damage, 3.0D, 1.5D, Particle.SWEEP_ATTACK);
    }

    private static boolean breaker(Player player, double damage) {
        Entity target = player.getTargetEntity(5, false);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return false;
        skillDamage(living, player, damage);
        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 10, 0.25, 0.25, 0.25);
        return true;
    }

    private static boolean shock(Player player, double damage) {
        return areaDamage(player, damage, 3.5D, 2.0D, Particle.ELECTRIC_SPARK);
    }

    private static boolean heavyStrike(Player player, double damage) {
        Entity target = player.getTargetEntity(6, false);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return false;
        skillDamage(living, player, damage);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 0.7F);
        return true;
    }

    private static boolean rapidCut(Player player, double damage) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(3.0D, 1.5D, 3.0D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                hits++;
            }
        }
        return hits > 0;
    }

    private static boolean cleave(Player player, double damage) {
        return areaDamage(player, damage, 4.0D, 2.0D, Particle.CRIT);
    }

    private static boolean rend(Player player, double damage) {
        Entity target = player.getTargetEntity(7, false);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return false;
        skillDamage(living, player, damage);
        living.setVelocity(living.getVelocity().setY(0.35D));
        living.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, living.getLocation().add(0, 1, 0), 16, 0.35, 0.35, 0.35);
        return true;
    }

    private static boolean piercingShot(Player player, double damage) {
        Entity target = player.getTargetEntity(18, false);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return false;
        skillDamage(living, player, damage);
        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 14, 0.2, 0.2, 0.2);
        return true;
    }

    private static boolean crossbowBurst(Player player, double damage) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(5.0D, 2.0D, 5.0D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                hits++;
            }
        }
        if (hits > 0) player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.7F, 1.4F);
        return hits > 0;
    }

    private static boolean areaDamage(Player player, double damage, double xz, double y, Particle particle) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(xz, y, xz)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                hits++;
            }
        }
        if (hits > 0) player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 16, xz * 0.5, 0.3, xz * 0.5);
        return hits > 0;
    }

    private static void skillDamage(LivingEntity target, Player player, double damage) {
        CombatDamageContext.runWeaponSkill(() -> target.damage(Math.max(0.1D, damage), player));
    }
}
