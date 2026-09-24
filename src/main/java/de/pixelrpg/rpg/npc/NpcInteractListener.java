package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.quest.QuestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractListener implements Listener {
    private final NpcManager npcManager;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final QuestService quests;
    public NpcInteractListener(NpcManager npcManager, NpcBehaviorRegistry behaviorRegistry, QuestService quests) {
        this.npcManager = npcManager;
        this.behaviorRegistry = behaviorRegistry;
        this.quests = quests;
    }
    // Zuständig für die primäre Interaktion mit PixelRPG-NPC-Mannequins und NPC-Questfortschritt.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        npcManager.getByEntity(event.getRightClicked().getUniqueId()).ifPresent(npc -> {
            quests.progressTalk(event.getPlayer(), npc.id());
            behaviorRegistry.get(npc.type()).ifPresent(behavior -> {
                event.setCancelled(true);
                behavior.onInteract(event.getPlayer(), npc);
            });
        });
    }
    // Zuständig dafür, dass PixelRPG-NPC-Mannequins keinen normalen Entity-Schaden erhalten.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (npcManager.getByEntity(event.getEntity().getUniqueId()).isPresent()) event.setCancelled(true);
    }
}
