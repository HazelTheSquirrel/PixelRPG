package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Objects;

public final class StoryNpcInteractionListener implements Listener {
    private final NpcRuntimeManager npcRuntime;
    private final StoryNpcDialogue dialogue;

    public StoryNpcInteractionListener(NpcRuntimeManager npcRuntime, StoryNpcDialogue dialogue) {
        this.npcRuntime = Objects.requireNonNull(npcRuntime, "npcRuntime");
        this.dialogue = Objects.requireNonNull(dialogue, "dialogue");
    }

    // Opens the story dialog when the player interacts with a story NPC.
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().isValid()) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        npcRuntime.getByEntity(event.getRightClicked().getUniqueId()).ifPresent(npc -> {
            if (!npc.type().equals(de.pixelrpg.rpg.npc.NpcType.STORY)) return;
            event.setCancelled(true);
            dialogue.open(event.getPlayer(), npc);
        });
    }
}
