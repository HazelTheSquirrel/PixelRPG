package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    public void cast(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta()) return;
        var pdc = weapon.getItemMeta().getPersistentDataContainer();
        if (!Boolean.TRUE.equals(pdc.get(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN))) return;

        String category = pdc.get(RPGKeys.Item.category(), PersistentDataType.STRING);
        if (!ItemCategory.WEAPON.name().equals(category)) return;

        String abilityId = pdc.get(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING);
        if (abilityId == null || abilityId.isBlank()) return;

        long cooldown = pdc.getOrDefault(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, 0L);
        long now = System.currentTimeMillis();
        Long expiry = cooldownExpiry.get(player.getUniqueId());
        if (expiry != null && now < expiry) {
            long seconds = (expiry - now + 999L) / 1000L;
            player.sendActionBar(Component.text("Ability cooldown: " + seconds + "s", NamedTextColor.GRAY));
            return;
        }

        double manaCost = Math.max(0.0, pdc.getOrDefault(RPGKeys.Item.weaponAbilityManaCost(), PersistentDataType.DOUBLE, 0.0D));
        if (manaCost > 0.0 && profile.getCurrentMana() + 1.0E-9 < manaCost) {
            player.sendActionBar(Component.text("Nicht genug Mana: " + format(profile.getCurrentMana()) + "/" + format(statEngine.getMaxMana(player)), NamedTextColor.BLUE));
            return;
        }

        double weaponDamage = pdc.getOrDefault(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, 0.0D);
        boolean executed = switch (abilityId) {
            case "HEAVY_STRIKE" -> heavyStrike(player, weaponDamage);
            case "WHIRLWIND" -> whirlwind(player, weaponDamage);
            case "ARCANE_BURST" -> arcaneBurst(player, weaponDamage);
            default -> false;
        };

        if (!executed) return;
        if (manaCost > 0.0) statEngine.consumeMana(player, manaCost);
        if (cooldown > 0L) cooldownExpiry.put(player.getUniqueId(), now + cooldown);
    }

    private boolean heavyStrike(Player player, double weaponDamage) {
        Entity target = player.getTargetEntity(8, false);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return false;
        double damage = 8.0 + weaponDamage + statEngine.getCachedStats(player.getUniqueId()).bonusDamage() * 1.5;
        living.damage(damage, player);
        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.8f);
        return true;
    }

    private boolean whirlwind(Player player, double weaponDamage) {
        double damage = 5.0 + weaponDamage + statEngine.getCachedStats(player.getUniqueId()).bonusDamage();
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(3.5, 2.0, 3.5)) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                hits++;
            }
        }
        if (hits == 0) return false;
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 8, 1.5, 0.2, 1.5);
        return true;
    }

    private boolean arcaneBurst(Player player, double weaponDamage) {
        double damage = 10.0 + weaponDamage + statEngine.getCachedStats(player.getUniqueId()).bonusDamage() * 1.75;
        int hits = 0;
        for (Entity entity : player.getNearbyEntities(5.0, 3.0, 5.0)) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                hits++;
            }
        }
        if (hits == 0) return false;
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 2.5, 1.0, 2.5);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
        return true;
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
