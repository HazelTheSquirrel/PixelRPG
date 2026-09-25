package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class MobExperienceListener implements Listener {
    private static final double PARTY_XP_BONUS = 0.20D;
    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final PartyAPI partyAPI;

    public MobExperienceListener(GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
        RegisteredServiceProvider<PartyAPI> provider = Bukkit.getServicesManager().getRegistration(PartyAPI.class);
        this.partyAPI = provider != null ? provider.getProvider() : null;
    }

    // Zuständig für die Vergabe von Monster-XP inklusive Party-Bonus und Reichweitenprüfung.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        if (entity.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;

        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) return;
        var pdc = entity.getPersistentDataContainer();
        Double originalMaxHealth = pdc.get(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE);
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double fixedMaxHealth = originalMaxHealth != null ? originalMaxHealth : (maxHealthAttribute != null ? maxHealthAttribute.getBaseValue() : 20.0D);
        long xpReward = Math.max(1L, Math.round(fixedMaxHealth * scalingConfig.getXpPerMaxHealth()));
        List<Player> recipients = resolveRecipients(killer);
        if (recipients.isEmpty()) return;
        if (partyAPI != null && partyAPI.isInParty(killer.getUniqueId())) xpReward = Math.max(xpReward, Math.round(xpReward * (1.0D + PARTY_XP_BONUS)));
        long sharedXp = xpReward / recipients.size();
        long remainder = xpReward % recipients.size();
        for (int index = 0; index < recipients.size(); index++) {
            Player recipient = recipients.get(index);
            long amount = sharedXp + (index < remainder ? 1L : 0L);
            if (amount <= 0L) continue;
            guildAPI.addExperience(recipient.getUniqueId(), amount);
            recipient.sendActionBar(Component.text("+" + amount + " EP", NamedTextColor.GREEN));
        }
    }

    private List<Player> resolveRecipients(Player killer) {
        if (partyAPI == null || !partyAPI.isInParty(killer.getUniqueId())) return List.of(killer);
        double range = partyAPI.getShareRange();
        double maxDistanceSquared = range * range;
        UUID killerUuid = killer.getUniqueId();
        List<Player> recipients = new ArrayList<>();
        for (UUID memberUuid : partyAPI.getPartyMembers(killerUuid)) {
            if (!guildAPI.isRegistered(memberUuid)) continue;
            Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) continue;
            if (!member.getWorld().equals(killer.getWorld())) continue;
            if (member.getLocation().distanceSquared(killer.getLocation()) > maxDistanceSquared) continue;
            recipients.add(member);
        }
        if (recipients.stream().noneMatch(player -> player.getUniqueId().equals(killerUuid))) recipients.add(killer);
        recipients.sort(Comparator.comparing(player -> !player.getUniqueId().equals(killerUuid)));
        return recipients;
    }
}