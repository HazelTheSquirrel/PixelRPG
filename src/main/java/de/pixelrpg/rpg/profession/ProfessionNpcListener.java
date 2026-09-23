package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
/** Connects profession NPCs to the native profession dialog without coupling NPC runtime to profession rules. */
public final class ProfessionNpcListener implements Listener{
 private final NpcRuntimeManager npcs;private final ProfessionDialogService dialogs;
 public ProfessionNpcListener(NpcRuntimeManager npcs,ProfessionDialogService dialogs){this.npcs=npcs;this.dialogs=dialogs;}
 // Öffnet ausschließlich bei einem bekannten Berufs-NPC den nativen Berufs-Dialog.
 @EventHandler public void onInteract(PlayerInteractEntityEvent event){if(event.getHand()!=EquipmentSlot.HAND)return;npcs.getByEntity(event.getRightClicked().getUniqueId()).filter(n->n.type().name().startsWith("PROFESSION_")).ifPresent(n->{event.setCancelled(true);dialogs.openTrainer(event.getPlayer(),n);});}
}
