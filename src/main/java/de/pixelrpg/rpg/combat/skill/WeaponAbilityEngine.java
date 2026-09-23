package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.combat.CombatDamageContext;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Executes the material-specific PixelRPG weapon abilities with one shared six-second cooldown. */
public final class WeaponAbilityEngine {
    private static final long COOLDOWN_MILLIS = 6_000L;

    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final Map<UUID, Long> cooldownExpiry = new ConcurrentHashMap<>();

    public WeaponAbilityEngine(PlayerProfileManager profileManager, StatEngine statEngine) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
    }

    /** Activates a melee weapon ability from a main-hand right click. */
    public boolean cast(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        SkillDefinition skill = skillFor(weapon.getType());
        if (skill == null || skill.ranged()) return false;
        return castInternal(player, weapon, skill);
    }

    /** Activates a bow or crossbow ability when the player releases the use button. */
    public boolean castReleased(Player player, ItemStack releasedItem, int ticksHeldFor) {
        if (releasedItem == null || releasedItem.isEmpty()) return false;
        SkillDefinition skill = skillFor(releasedItem.getType());
        if (skill == null || !skill.ranged() || ticksHeldFor < 2) return false;
        return castInternal(player, releasedItem, skill);
    }

    public boolean isRangedWeapon(Material material) {
        SkillDefinition skill = skillFor(material);
        return skill != null && skill.ranged();
    }

    public void clearCooldown(UUID uuid) {
        cooldownExpiry.remove(uuid);
    }

    private boolean castInternal(Player player, ItemStack weapon, SkillDefinition skill) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;
        if (!isPixelRPGWeapon(weapon)) return false;

        var pdc = weapon.getItemMeta().getPersistentDataContainer();
        Integer requiredLevel = pdc.get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
        if (requiredLevel == null) {
            requiredLevel = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        }
        if (requiredLevel != null && profile.getLevel() < requiredLevel) {
            player.sendActionBar(Component.text("Benötigt Level " + requiredLevel, NamedTextColor.RED));
            return true;
        }

        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);
        if (now < expiry) {
            long seconds = (expiry - now + 999L) / 1000L;
            player.sendActionBar(Component.text("Ability-Cooldown: " + seconds + "s", NamedTextColor.GRAY));
            return true;
        }

        double attackPower = Math.max(0.0D, statEngine.getCachedStats(player.getUniqueId()).attackPower());
        boolean executed = skill.action().execute(player, Math.max(0.5D, attackPower * skill.multiplier()));
        if (!executed) return false;

        cooldownExpiry.put(player.getUniqueId(), now + COOLDOWN_MILLIS);
        player.sendActionBar(Component.text(skill.displayName() + "  •  6s Cooldown", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.55F, skill.soundPitch());
        return true;
    }

    private static boolean isPixelRPGWeapon(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty() || !weapon.hasItemMeta()) return false;
        var pdc = weapon.getItemMeta().getPersistentDataContainer();
        return Boolean.TRUE.equals(pdc.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN));
    }

    private static SkillDefinition skillFor(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> new SkillDefinition("Holzklingenfeger", 0.80D, false, 0.9F, WeaponAbilityEngine::woodSweep);
            case STONE_SWORD -> new SkillDefinition("Steinbrecher", 1.00D, false, 0.8F, WeaponAbilityEngine::stoneBreaker);
            case COPPER_SWORD -> new SkillDefinition("Kupfersturm", 1.20D, false, 1.7F, WeaponAbilityEngine::copperSword);
            case IRON_SWORD -> new SkillDefinition("Eiserne Bastion", 1.45D, false, 0.7F, WeaponAbilityEngine::ironSword);
            case GOLDEN_SWORD -> new SkillDefinition("Goldene Klingenflut", 1.05D, false, 1.4F, WeaponAbilityEngine::goldSword);
            case DIAMOND_SWORD -> new SkillDefinition("Diamantspalter", 1.70D, false, 1.2F, WeaponAbilityEngine::diamondSword);
            case NETHERITE_SWORD -> new SkillDefinition("Netherit-Eruption", 2.10D, false, 0.5F, WeaponAbilityEngine::netheriteSword);
            case COPPER_AXE -> new SkillDefinition("Kupferstatische Axt", 1.25D, false, 1.7F, WeaponAbilityEngine::copperAxe);
            case IRON_AXE -> new SkillDefinition("Eisen-Erdbruch", 1.55D, false, 0.65F, WeaponAbilityEngine::ironAxe);
            case GOLDEN_AXE -> new SkillDefinition("Goldener Wirbel", 1.15D, false, 1.5F, WeaponAbilityEngine::goldAxe);
            case DIAMOND_AXE -> new SkillDefinition("Diamant-Henker", 1.85D, false, 1.1F, WeaponAbilityEngine::diamondAxe);
            case NETHERITE_AXE -> new SkillDefinition("Höllenspalter", 2.25D, false, 0.45F, WeaponAbilityEngine::netheriteAxe);
            case BOW -> new SkillDefinition("Kupferwind-Schuss", 1.40D, true, 1.6F, WeaponAbilityEngine::copperBow);
            case CROSSBOW -> new SkillDefinition("Eiserne Salve", 1.65D, true, 0.9F, WeaponAbilityEngine::ironCrossbow);
            default -> null;
        };
    }

    private static boolean woodSweep(Player player, double damage) {
        return areaDamage(player, damage, 2.8D, 1.5D, Particle.SWEEP_ATTACK, null);
    }

    private static boolean stoneBreaker(Player player, double damage) {
        LivingEntity target = target(player, 5.0D);
        if (target == null) return false;
        skillDamage(target, player, damage);
        target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.35D).setY(0.18D)));
        burst(target, Particle.CRIT, 14);
        return true;
    }

    private static boolean copperSword(Player player, double damage) {
        LivingEntity target = target(player, 6.0D);
        if (target == null) return false;
        skillDamage(target, player, damage);
        effect(target, PotionEffectType.SLOWNESS, 60, 0);
        player.getWorld().strikeLightningEffect(target.getLocation());
        burst(target, Particle.ELECTRIC_SPARK, 28);
        return true;
    }

    private static boolean ironSword(Player player, double damage) {
        LivingEntity target = target(player, 6.0D);
        if (target == null) return false;
        skillDamage(target, player, damage * 1.15D);
        target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.55D).setY(0.22D)));
        burst(target, Particle.CRIT, 20);
        return true;
    }

    private static boolean goldSword(Player player, double damage) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(3.5D, 1.5D, 3.5D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage * 0.75D);
                hits++;
            }
        }
        if (hits == 0) return false;
        effect(player, PotionEffectType.SPEED, 80, 1);
        burst(player, Particle.SWEEP_ATTACK, 24);
        return true;
    }

    private static boolean diamondSword(Player player, double damage) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(4.0D, 2.0D, 4.0D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                effect(monster, PotionEffectType.WEAKNESS, 80, 0);
                hits++;
            }
        }
        if (hits == 0) return false;
        burst(player, Particle.CRIT, 36);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        return true;
    }

    private static boolean netheriteSword(Player player, double damage) {
        int hits = areaDamageCount(player, damage * 1.10D, 4.0D, 2.0D);
        if (hits == 0) return false;
        for (Entity entity : player.getNearbyEntities(4.0D, 2.0D, 4.0D)) {
            if (entity instanceof Monster monster) monster.setFireTicks(Math.max(monster.getFireTicks(), 100));
        }
        burst(player, Particle.SOUL_FIRE_FLAME, 42);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 0.65F);
        return true;
    }

    private static boolean copperAxe(Player player, double damage) {
        return areaDamage(player, damage, 3.0D, 1.7D, Particle.ELECTRIC_SPARK, PotionEffectType.SLOWNESS);
    }

    private static boolean ironAxe(Player player, double damage) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(3.5D, 1.7D, 3.5D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage * 1.1D);
                monster.setVelocity(monster.getVelocity().setY(0.5D));
                hits++;
            }
        }
        if (hits == 0) return false;
        burst(player, Particle.CLOUD, 30);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_BIG_FALL, 0.8F, 0.8F);
        return true;
    }

    private static boolean goldAxe(Player player, double damage) {
        int hits = areaDamageCount(player, damage * 0.85D, 4.0D, 1.8D);
        if (hits == 0) return false;
        effect(player, PotionEffectType.SPEED, 100, 1);
        burst(player, Particle.SWEEP_ATTACK, 36);
        return true;
    }

    private static boolean diamondAxe(Player player, double damage) {
        LivingEntity target = target(player, 6.0D);
        if (target == null) return false;
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH) == null
                ? target.getHealth()
                : target.getAttribute(Attribute.MAX_HEALTH).getValue();
        double executionDamage = target.getHealth() <= maxHealth * 0.50D ? damage * 1.75D : damage * 1.15D;
        skillDamage(target, player, executionDamage);
        effect(target, PotionEffectType.SLOWNESS, 80, 0);
        burst(target, Particle.CRIT, 28);
        return true;
    }

    private static boolean netheriteAxe(Player player, double damage) {
        int hits = areaDamageCount(player, damage * 1.15D, 4.2D, 2.0D);
        if (hits == 0) return false;
        for (Entity entity : player.getNearbyEntities(4.2D, 2.0D, 4.2D)) {
            if (entity instanceof Monster monster) {
                monster.setFireTicks(Math.max(monster.getFireTicks(), 120));
                effect(monster, PotionEffectType.WEAKNESS, 80, 0);
            }
        }
        burst(player, Particle.SOUL_FIRE_FLAME, 48);
        return true;
    }

    private static boolean copperBow(Player player, double damage) {
        LivingEntity target = target(player, 22.0D);
        if (target == null) return false;
        skillDamage(target, player, damage);
        effect(target, PotionEffectType.GLOWING, 100, 0);
        player.getWorld().strikeLightningEffect(target.getLocation());
        burst(target, Particle.ELECTRIC_SPARK, 24);
        return true;
    }

    private static boolean ironCrossbow(Player player, double damage) {
        LivingEntity target = target(player, 24.0D);
        if (target == null) return false;
        int hits = 0;
        for (Entity entity : target.getNearbyEntities(2.2D, 1.5D, 2.2D)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage * 0.9D);
                hits++;
            }
        }
        skillDamage(target, player, damage * 1.15D);
        burst(target, Particle.CRIT, 26);
        return hits > 0 || !target.isDead();
    }

    private static boolean areaDamage(Player player, double damage, double radius, double y, Particle particle, PotionEffectType effect) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(radius, y, radius)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                if (effect != null) effect(monster, effect, 60, 0);
                hits++;
            }
        }
        if (hits == 0) return false;
        burst(player, particle, 30);
        return true;
    }

    private static int areaDamageCount(Player player, double damage, double radius, double y) {
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(radius, y, radius)) {
            if (entity instanceof Monster monster) {
                skillDamage(monster, player, damage);
                hits++;
            }
        }
        return hits;
    }

    private static LivingEntity target(Player player, double range) {
        Entity entity = player.getTargetEntity((int) Math.ceil(range), false);
        return entity instanceof Monster monster ? monster : null;
    }

    private static void skillDamage(LivingEntity target, Player player, double damage) {
        CombatDamageContext.runWeaponSkill(() -> target.damage(Math.max(0.1D, damage), player));
    }

    private static void effect(LivingEntity entity, PotionEffectType type, int duration, int amplifier) {
        entity.addPotionEffect(new PotionEffect(type, duration, amplifier, false, true, true));
    }

    private static void burst(Entity entity, Particle particle, int count) {
        entity.getWorld().spawnParticle(particle, entity.getLocation().add(0.0D, 1.0D, 0.0D), count, 0.55D, 0.45D, 0.55D, 0.02D);
    }

    private record SkillDefinition(String displayName, double multiplier, boolean ranged, float soundPitch, SkillAction action) {
    }

    @FunctionalInterface
    private interface SkillAction {
        boolean execute(Player player, double bonusDamage);
    }
}
