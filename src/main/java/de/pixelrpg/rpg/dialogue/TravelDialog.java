package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native travel dialog replacing the legacy inventory travel menu. */
public final class TravelDialog {
    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public TravelDialog(NpcManager npcManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    public void open(Player player, String currentNpcId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            dialogueEngine.openUnavailable(player, "Reisen", "Dein Spielerprofil konnte nicht geladen werden.");
            return;
        }

        List<RPGNpc> destinations = npcManager.getAll().stream()
                .filter(npc -> npc.type() == NpcType.TRAVEL)
                .filter(npc -> currentNpcId == null || !currentNpcId.equals(npc.id()))
                .filter(npc -> profile.hasUnlockedWaypoint(npc.id()))
                .toList();

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle einen freigeschalteten Reisepunkt.", NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text(
                        "Nicht entdeckte Orte werden automatisch freigeschaltet, sobald du sie besuchst.",
                        NamedTextColor.DARK_GRAY))
        );

        if (destinations.isEmpty()) {
            dialogueEngine.openNotice(
                    player,
                    Component.text("Reisen", NamedTextColor.GOLD),
                    Component.text("Du hast noch keinen weiteren Reisepunkt freigeschaltet.", NamedTextColor.GRAY),
                    Component.text("Schließen", NamedTextColor.GREEN));
            return;
        }

        List<ActionButton> actions = new ArrayList<>();
        for (RPGNpc destination : destinations) {
            actions.add(dialogueEngine.actionButton(
                    Component.text(destination.name()),
                    NamedTextColor.GREEN,
                    target -> teleport(target, destination)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogueEngine.openMultiAction(
                player,
                Component.text("Reisen", NamedTextColor.GOLD),
                body,
                actions,
                2);
    }

    private void teleport(Player player, RPGNpc destination) {
        player.teleportAsync(destination.location().clone().add(0, 1, 0)).thenAccept(success -> {
            if (!success) return;
            Bukkit.getScheduler().runTask(PixelRPGPlugin.getInstance(), () -> {
                player.closeDialog();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            });
        });
    }
}
