package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Connects TRAVEL NPCs to waypoint discovery and safe native-dialog teleportation. */
public final class TravelNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogs;

    public TravelNpcListener(NpcRuntimeManager npcs, PlayerProfileManager profiles, DialogueEngine dialogs) {
        this.npcs = npcs;
        this.profiles = profiles;
        this.dialogs = dialogs;
    }

    // Unlocks the interacted travel point and opens all previously discovered destinations.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.TRAVEL) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogs.openUnavailable(player, "Reisen", "Du musst zuerst registriertes Rathausmitglied sein.");
            return;
        }
        if (!profile.hasUnlockedWaypoint(npc.id())) {
            profile.unlockWaypoint(npc.id());
            profiles.saveProfileAsync(player.getUniqueId());
        }
        List<RPGNpc> destinations = npcs.getAll().stream()
                .filter(other -> other.type() == NpcType.TRAVEL)
                .filter(other -> !other.id().equals(npc.id()))
                .filter(other -> profile.hasUnlockedWaypoint(other.id()))
                .sorted(Comparator.comparing(RPGNpc::name))
                .toList();
        if (destinations.isEmpty()) {
            dialogs.openNotice(player, Component.text("Reisen", NamedTextColor.GOLD),
                    Component.text("Dieser Reisepunkt wurde freigeschaltet. Besuche weitere Reise-NPCs, um sie ebenfalls freizuschalten."),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        for (RPGNpc destination : destinations) {
            actions.add(dialogs.actionButton(Component.text(destination.name()), NamedTextColor.GREEN,
                    target -> teleport(target, destination)));
        }
        dialogs.openMultiAction(player, Component.text("Reisen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Freigeschaltete Reisepunkte"))), actions, 2);
    }

    private void teleport(Player player, RPGNpc destination) {
        if (destination.location().getWorld() == null) return;
        player.teleportAsync(destination.location().clone().add(0.5D, 1.0D, 0.5D)).thenAccept(success -> {
            if (success) player.closeDialog();
        });
    }
}
