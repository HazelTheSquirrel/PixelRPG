// src/main/java/de/pixelrpg/rpg/combat/MobExperienceListener.java
package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobExperienceListener implements Listener {

    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;

    public MobExperienceListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) {
            return;
        }

        var maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getBaseValue() : 20.0;

        long xpReward = Math.round(maxHealth * scalingConfig.getXpPerMaxHealth());
        if (xpReward <= 0) {
            return;
        }

        guildAPI.addExperience(killer.getUniqueId(), xpReward);
        killer.sendActionBar(Component.text("+" + xpReward + " XP", NamedTextColor.YELLOW));
    }
}