package de.pixelrpg.rpg.shop;

import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.NpcType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class ShopNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final ShopDialogService dialogs;

    public ShopNpcListener(NpcRuntimeManager npcs, ShopDialogService dialogs) {
        this.npcs = npcs;
        this.dialogs = dialogs;
    }

    // Opens the native shop dialog for a SHOP NPC on main-hand interaction.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.SHOP) return;
        event.setCancelled(true);
        dialogs.open(event.getPlayer(), npc.id());
    }
}
