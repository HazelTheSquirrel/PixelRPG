package de.pixelrpg.rpg.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractListener implements Listener {
    private final NpcManager npcManager;
    private final NpcBehaviorRegistry behaviorRegistry;

    public NpcInteractListener(NpcManager npcManager, NpcBehaviorRegistry behaviorRegistry) {
        this.npcManager = npcManager;
        this.behaviorRegistry = behaviorRegistry;
    }

    // Zuständig für die primäre Interaktion mit PixelRPG-NPC-Mannequins.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        npcManager.getByEntity(event.getRightClicked().getUniqueId()).ifPresent(npc ->
                behaviorRegistry.get(npc.type()).ifPresent(behavior -> {
                    event.setCancelled(true);
                    behavior.onInteract(event.getPlayer(), npc);
                }));
    }

    // Zuständig dafür, dass PixelRPG-NPC-Mannequins keinen normalen Entity-Schaden erhalten.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (npcManager.getByEntity(event.getEntity().getUniqueId()).isPresent()) event.setCancelled(true);
    }
}
