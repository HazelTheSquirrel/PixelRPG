package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;


/** Awards companion progression and protects active companion runtime entities from vanilla damage. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService service;
    public CompanionExperienceListener(Plugin plugin,CompanionService service){this.service=service;}

    // Awards the configured mob-kill experience to the active companion of the killer.
    @EventHandler
    public void onMobDeath(EntityDeathEvent event){
        Player killer=event.getEntity().getKiller();
        if(killer==null)return;
        if(service.getOwnerOfEntity(event.getEntity().getUniqueId())!=null)return;
        service.awardExperience(killer.getUniqueId(),service.registry().mobKillXp());
    }

    // Prevents ordinary active companions from taking vanilla damage.
    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof LivingEntity living))return;
        if(service.getOwnerOfEntity(living.getUniqueId())==null)return;
        String id=service.companionId(living);
        if(id==null || service.definition(id).rarity().isUnique())return;
        event.setCancelled(true);
    }

    // Awards the configured quest-completion experience to the active companion.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event){
        service.awardExperience(event.playerId(),service.registry().questXp());
    }

}
