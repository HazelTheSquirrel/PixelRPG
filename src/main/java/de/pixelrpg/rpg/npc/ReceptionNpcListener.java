package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/** Connects RECEPTION NPCs to player registration and profile controls. */
public final class ReceptionNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogs;

    public ReceptionNpcListener(NpcRuntimeManager npcs, PlayerProfileManager profiles, DialogueEngine dialogs) {
        this.npcs = npcs;
        this.profiles = profiles;
        this.dialogs = dialogs;
    }

    // Opens the registration dialog for a main-hand interaction with a RECEPTION NPC.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.RECEPTION) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void open(Player player) {
        boolean registered = profiles.getProfile(player.getUniqueId()).map(p -> p.isRegistered()).orElse(false);
        ActionButton action = dialogs.actionButton(
                Component.text(registered ? "Registrierung aufheben" : "Registrieren"),
                registered ? NamedTextColor.RED : NamedTextColor.GREEN,
                target -> {
                    if (registered) profiles.unregisterPlayer(target);
                    else profiles.registerPlayer(target);
                    profiles.saveProfileAsync(target.getUniqueId());
                    open(target);
                });
        dialogs.openMultiAction(player, Component.text("RPG-Registrierung", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Willkommen. Hier verwaltest du dein PixelRPG-Profil.")),
                        DialogBody.plainMessage(Component.text(registered ? "Status: registriert" : "Status: nicht registriert",
                                registered ? NamedTextColor.GREEN : NamedTextColor.RED))),
                List.of(action, dialogs.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)), 1);
    }
}
