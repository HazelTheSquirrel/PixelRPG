package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;

    public MobExperienceListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    // Zuständig für die Vergabe von Gilden-Erfahrungspunkten an registrierte
    // Spieler beim Töten von Monstern, proportional zur maximalen HP des Mobs.
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
        killer.sendActionBar(lang.get("xp.gained", "amount", String.valueOf(xpReward)));
    }
}