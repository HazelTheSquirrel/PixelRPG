package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native dialog showing the player's profession levels and progress. */
public final class ProfessionDialog {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public ProfessionDialog(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    public void open(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            dialogueEngine.openUnavailable(player, "PixelRPG – Berufe", "Du bist noch nicht registriert.");
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Deine Berufe", NamedTextColor.AQUA)));
        for (Profession profession : Profession.values()) {
            int level = profile.getProfessionLevel(profession);
            long experience = profile.getProfessionExperience(profession);
            body.add(DialogBody.plainMessage(Component.text()
                    .append(Component.text(format(profession), NamedTextColor.WHITE))
                    .append(Component.text("  •  Level ", NamedTextColor.GRAY))
                    .append(Component.text(level, NamedTextColor.YELLOW))
                    .append(Component.text("  •  XP ", NamedTextColor.GRAY))
                    .append(Component.text(experience, NamedTextColor.GREEN))
                    .build()));
        }

        List<ActionButton> actions = List.of(dialogueEngine.actionButton(
                Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), body, actions, 1);
    }

    private String format(Profession profession) {
        return switch (profession) {
            case MINING -> "Bergbau";
            case WOODCUTTING -> "Holzfällen";
            case HERBALISM -> "Kräuterkunde";
            case SKINNING -> "Kürschnerei";
            case FISHING -> "Angeln";
            case BLACKSMITHING -> "Schmiedekunst";
            case LEATHERWORKING -> "Lederverarbeitung";
            case TAILORING -> "Schneiderei";
            case ALCHEMY -> "Alchemie";
            case COOKING -> "Kochkunst";
        };
    }
}
