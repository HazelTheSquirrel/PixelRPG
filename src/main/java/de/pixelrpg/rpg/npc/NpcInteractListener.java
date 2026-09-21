package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.dialogue.DialogueDomainState;
import de.pixelrpg.rpg.dialogue.DialogueContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractListener implements Listener {
    private final NpcManager npcManager;
    private final NpcBehaviorRegistry behaviorRegistry;
    private final QuestManager questManager;
    private final DialogueDomainState domainState;

    public NpcInteractListener(NpcManager npcManager, NpcBehaviorRegistry behaviorRegistry, QuestManager questManager, DialogueDomainState domainState) {
        this.npcManager = npcManager;
        this.behaviorRegistry = behaviorRegistry;
        this.questManager = questManager;
        this.domainState = domainState;
    }

    // Zuständig für die primäre Interaktion mit PixelRPG-NPC-Mannequins und NPC-bezogene Questfortschritte.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        npcManager.getByEntity(event.getRightClicked().getUniqueId()).ifPresent(npc -> {
            questManager.progressTalkToNpc(event.getPlayer(), npc.id());
            behaviorRegistry.get(npc.type()).ifPresent(behavior -> {
                event.setCancelled(true);
                behavior.onInteract(DialogueContext.forNpc(event.getPlayer(), npc, domainState));
            });
        });
    }

    // Zuständig dafür, dass PixelRPG-NPC-Mannequins keinen normalen Entity-Schaden erhalten.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (npcManager.getByEntity(event.getEntity().getUniqueId()).isPresent()) event.setCancelled(true);
    }
}
