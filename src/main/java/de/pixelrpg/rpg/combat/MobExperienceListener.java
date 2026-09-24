package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.util.List;
import java.util.UUID;

public final class MobExperienceListener implements Listener {
    private static final double PARTY_XP_BONUS=.20D;
    private final GuildAPI guild; private final MobScalingConfig scaling; private final PartyAPI party;
    public MobExperienceListener(GuildAPI guild,MobScalingConfig scaling){this.guild=guild;this.scaling=scaling;RegisteredServiceProvider<PartyAPI> provider=Bukkit.getServicesManager().getRegistration(PartyAPI.class);party=provider==null?null:provider.getProvider();}
    // Zuständig für die Vergabe von Monster-XP inklusive Party-Bonus und Reichweitenprüfung.
    @EventHandler(priority=EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event){
        LivingEntity entity=event.getEntity();if(!(entity instanceof Monster))return;Player killer=entity.getKiller();if(killer==null||!guild.isRegistered(killer.getUniqueId()))return;
        Double original=entity.getPersistentDataContainer().get(RPGKeys.Combat.originalMaxHealth(),PersistentDataType.DOUBLE);Attribute max=Attribute.MAX_HEALTH;var instance=entity.getAttribute(max);double hp=original!=null?original:(instance!=null?instance.getBaseValue():20);long reward=Math.max(1,Math.round(hp*scaling.getXpPerMaxHealth()));
        List<Player> recipients=resolveRecipients(killer);if(recipients.isEmpty())return;if(party!=null&&party.isInParty(killer.getUniqueId()))reward=Math.max(reward,Math.round(reward*(1+PARTY_XP_BONUS)));
        long shared=reward/recipients.size(),remainder=reward%recipients.size();for(int i=0;i<recipients.size();i++){long amount=shared+(i<remainder?1:0);if(amount<=0)continue;guild.addExperience(recipients.get(i).getUniqueId(),amount);recipients.get(i).sendActionBar(Component.text("+"+amount+" EP",NamedTextColor.GREEN));}
    }
    private List<Player> resolveRecipients(Player killer){if(party==null||!party.isInParty(killer.getUniqueId()))return List.of(killer);double range=party.getShareRange(),max=range*range,dx;UUID id=killer.getUniqueId();return party.getMembers(id).stream().map(Bukkit::getPlayer).filter(p->p!=null&&p.isOnline()&&guild.isRegistered(p.getUniqueId())&&p.getWorld().equals(killer.getWorld())&&p.getLocation().distanceSquared(killer.getLocation())<=max).toList();}
}
