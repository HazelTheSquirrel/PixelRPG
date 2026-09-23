package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.NpcType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Connects QUEST NPC runtime entities to the native quest dialog. */
public final class QuestNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final QuestNpcDialogService dialogs;

    public QuestNpcListener(NpcRuntimeManager npcs, QuestNpcDialogService dialogs) {
        this.npcs = npcs;
        this.dialogs = dialogs;
    }

    // Opens the quest dialog for a main-hand interaction with a QUEST NPC.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.QUEST) return;
        event.setCancelled(true);
        dialogs.open(event.getPlayer());
    }
}
