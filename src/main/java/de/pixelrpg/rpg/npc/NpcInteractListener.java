package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.quest.QuestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractListener implements Listener {
    private final NpcManager npcManager;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final QuestManager questManager;
    private final NpcDialogueService npcDialogueService;

    public NpcInteractListener(NpcManager npcManager, NpcBehaviorRegistry behaviorRegistry,
                               QuestManager questManager, NpcDialogueService npcDialogueService) {
        this.npcManager = npcManager;
        this.behaviorRegistry = behaviorRegistry;
        this.questManager = questManager;
        this.npcDialogueService = npcDialogueService;
    }

    // Zuständig für die primäre Interaktion mit PixelRPG-NPC-Mannequins und NPC-bezogene Questfortschritte.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        npcManager.getByEntity(event.getRightClicked().getUniqueId()).ifPresent(npc -> {
            questManager.progressTalkToNpc(event.getPlayer(), npc.id());
            if (npc.type() == NpcType.FILLER) return;

            event.setCancelled(true);

            // Story-NPCs behalten ausschließlich ihre questgesteuerte Story-Dialoglogik.
            if (npc.type() == NpcType.STORY) {
                behaviorRegistry.get(npc.type()).ifPresent(behavior -> behavior.onInteract(event.getPlayer(), npc));
                return;
            }

            if (npcDialogueService.open(event.getPlayer(), npc)) return;
            behaviorRegistry.get(npc.type()).ifPresent(behavior -> behavior.onInteract(event.getPlayer(), npc));
        });
    }

    // Zuständig dafür, dass PixelRPG-NPC-Mannequins keinen normalen Entity-Schaden erhalten.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (npcManager.getByEntity(event.getEntity().getUniqueId()).isPresent()) event.setCancelled(true);
    }
}
