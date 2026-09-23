package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

/** Bridges NPC interaction into registered native dialogue trees without owning NPC domain logic. */
public final class NpcDialogueListener implements Listener {
    private final NpcRuntimeManager npcRuntime;
    private final DialogueTreeService dialogueTreeService;

    public NpcDialogueListener(NpcRuntimeManager npcRuntime, DialogueTreeService dialogueTreeService) {
        this.npcRuntime = Objects.requireNonNull(npcRuntime, "npcRuntime");
        this.dialogueTreeService = Objects.requireNonNull(dialogueTreeService, "dialogueTreeService");
    }

    // Opens a registered native dialogue tree for the interacted PixelRPG NPC.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        RPGNpc npc = npcRuntime.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null) return;

        String treeId = "npc:" + npc.id();
        if (!dialogueTreeService.hasTree(treeId)) return;

        event.setCancelled(true);
        dialogueTreeService.open(event.getPlayer(), treeId);
    }
}
