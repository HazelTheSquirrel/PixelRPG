package de.pixelrpg.rpg.boss.patterns;

import de.pixelrpg.rpg.boss.BossAttackPattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public final class EnrageBuffPattern implements BossAttackPattern {
    @Override
    public String id() {
        return "ENRAGE_BUFF";
    }

    @Override
    public void execute(Plugin plugin, LivingEntity boss, Collection<Player> targets) {
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, true, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, true, true));
        boss.getWorld().spawnParticle(Particle.FLAME, boss.getLocation().add(0, 1, 0), 40, 0.8, 1.0, 0.8);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);

        Component message = Component.text("The boss enters a rage!", NamedTextColor.DARK_RED);
        targets.stream().filter(Player::isOnline).forEach(player -> player.sendMessage(message));
    }
}
