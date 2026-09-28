package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/** Signs an unsigned Meisterbrief only when the player physically returns to the central spawn. */
public final class MeisterbriefSpawnListener implements Listener {
    private final MeisterbriefService service;
    public MeisterbriefSpawnListener(MeisterbriefService service){this.service=service;}

    // Zuständig für die zentrale Spawn-Unterschrift des Meisterbriefs.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event){
        if(!event.getAction().isRightClick())return;
        if(!service.isBrief(event.getItem()))return;
        if(service.signAtSpawn(event.getPlayer())) event.getPlayer().sendMessage(Component.text("Der Meisterbrief wurde am zentralen Spawn unterschrieben.",NamedTextColor.GREEN));
    }
}
