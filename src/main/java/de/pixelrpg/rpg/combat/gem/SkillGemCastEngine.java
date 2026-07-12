// src/main/java/de/pixelrpg/rpg/combat/gem/SkillGemCastEngine.java (VOLLSTÄNDIG, ersetzt alte Datei — ein Skill, generisches Ausführungssystem)
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillGemCastEngine {

    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final GemRepository gemRepository;
    private final Map<UUID, Long> cooldownExpiry = new ConcurrentHashMap<>();

    public SkillGemCastEngine(PlayerProfileManager profileManager, StatEngine statEngine, GemRepository gemRepository) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.gemRepository = gemRepository;
    }

    public void cast(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            return;
        }
        if (!profile.getRank().isAtLeast(Rank.C)) {
            player.sendActionBar(Component.text("Skills unlock at Rank C.", NamedTextColor.RED));
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        List<String> socketedGemIds = de.pixelrpg.rpg.combat.gem.GemSocketService.readSockets(weapon);

        String activeGemId = socketedGemIds.stream()
                .filter(gemRepository::isActiveGem)
                .findFirst()
                .orElse(null);

        if (activeGemId == null) {
            player.sendActionBar(Component.text("Socket an Active Skill Gem into your weapon.", NamedTextColor.GRAY));
            return;
        }

        ActiveSkillGemDefinition definition = gemRepository.getActive(activeGemId).orElse(null);
        if (definition == null) {
            return;
        }

        if (definition.ownerClass() != profile.getPlayerClass()) {
            player.sendActionBar(Component.text(
                    definition.displayName() + " requires the " + definition.ownerClass().name() + " class.", NamedTextColor.RED));
            return;
        }

        SupportGemModifiers aggregatedModifiers = SupportGemModifiers.NEUTRAL;
        for (String gemId : socketedGemIds) {
            SupportGemModifiers mods = readSupportModifiers(gemId);
            if (mods != null) {
                aggregatedModifiers = aggregatedModifiers.combine(mods);
            }
        }

        long cooldownMillis = (long) (definition.cooldownMillis() * aggregatedModifiers.cooldownMultiplier());
        Long expiry = cooldownExpiry.get(uuid);
        long now = System.currentTimeMillis();
        if (expiry != null && now < expiry) {
            long remainingSeconds = (expiry - now) / 1000L + 1L;
            player.sendActionBar(Component.text("On cooldown: " + remainingSeconds + "s", NamedTextColor.RED));
            return;
        }

        StatEngine.CachedStats stats = statEngine.getCachedStats(uuid);
        GemCastContext context = new GemCastContext(player, stats, aggregatedModifiers);
        GenericActiveGemEffect effect = new GenericActiveGemEffect(definition);
        List<LivingEntity> hitTargets = effect.execute(context);
        applySupportPostEffects(player, hitTargets, aggregatedModifiers);

        cooldownExpiry.put(uuid, now + cooldownMillis);
    }

    private SupportGemModifiers readSupportModifiers(String gemId) {
        return SupportGemRegistry.get(gemId);
    }

    private void applySupportPostEffects(Player caster, List<LivingEntity> hitTargets, SupportGemModifiers modifiers) {
        if (hitTargets.isEmpty()) {
            return;
        }
        double totalLifesteal = 0.0;

        for (LivingEntity target : hitTargets) {
            if (modifiers.applyBurn()) {
                target.setFireTicks(Math.max(target.getFireTicks(), 100));
            }
            if (modifiers.applySlow()) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true));
            }
            if (modifiers.lifestealPercent() > 0.0) {
                totalLifesteal += modifiers.lifestealPercent();
            }
            if (modifiers.aoeRadiusBonus() > 0.0) {
                for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(
                        modifiers.aoeRadiusBonus(), modifiers.aoeRadiusBonus(), modifiers.aoeRadiusBonus())) {
                    if (nearby instanceof Monster monster && !hitTargets.contains(monster)) {
                        monster.damage(3.0, caster);
                        if (modifiers.applyBurn()) {
                            monster.setFireTicks(Math.max(monster.getFireTicks(), 100));
                        }
                    }
                }
            }
        }

        if (totalLifesteal > 0.0) {
            var healthAttribute = caster.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = healthAttribute != null ? healthAttribute.getValue() : caster.getHealth();
            double healAmount = totalLifesteal / 100.0 * hitTargets.size() * 2.0;
            caster.setHealth(Math.min(maxHealth, caster.getHealth() + healAmount));
        }
    }
}