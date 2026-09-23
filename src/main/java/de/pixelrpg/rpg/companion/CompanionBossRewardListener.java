package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Unlocks data-configured companion rewards after eligible boss defeats. */
public final class CompanionBossRewardListener implements Listener {
    private final Plugin plugin; private final CompanionService service;
    public CompanionBossRewardListener(Plugin plugin,CompanionService service){this.plugin=plugin;this.service=service;}
    // Unlocks each configured boss companion for online participants and reports the reward.
    @EventHandler
    public void onBossDefeated(BossDefeatedEvent event){
        for(var playerId:event.getParticipants()){
            Player player=plugin.getServer().getPlayer(playerId);
            if(player!=null&&service.unlockFromBoss(playerId,event.getBossId()))
                player.sendMessage(Component.text("Neuer Begleiter freigeschaltet!",NamedTextColor.GREEN));
        }
    }
}
