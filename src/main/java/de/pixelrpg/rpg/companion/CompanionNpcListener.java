package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Opens the companion management dialog when a dedicated companion NPC is interacted with. */
public final class CompanionNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final CompanionDialogService dialogs;
    public CompanionNpcListener(NpcRuntimeManager npcs,CompanionDialogService dialogs){this.npcs=npcs;this.dialogs=dialogs;}
    // Opens the companion management dialog for NPCs whose type is explicitly COMPANION.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event){
        if(!(event.getPlayer() instanceof Player player))return;
        RPGNpc npc=npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if(npc==null || npc.type().name().equalsIgnoreCase("COMPANION"))return;
        event.setCancelled(true); dialogs.open(player);
    }
}
