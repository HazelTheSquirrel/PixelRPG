package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native dialog showing the player's four professions and their level-99 progression. */
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
        body.add(DialogBody.plainMessage(Component.text("Vier Berufe, ein gemeinsamer Wirtschaftskreislauf", NamedTextColor.AQUA)));
        for (Profession profession : Profession.values()) {
            int level = profile.getProfessionLevel(profession);
            long experience = profile.getProfessionExperience(profession);
            long next = level >= Profession.MAX_LEVEL ? 0L : ProfessionService.experienceForLevel(level + 1);
            long progress = Math.max(0L, next - experience);
            body.add(DialogBody.plainMessage(Component.text()
                    .append(profession.displayComponent())
                    .append(Component.text("  •  Level ", NamedTextColor.GRAY))
                    .append(Component.text(level + "/" + Profession.MAX_LEVEL, NamedTextColor.YELLOW))
                    .append(Component.text("  •  XP ", NamedTextColor.GRAY))
                    .append(Component.text(experience, NamedTextColor.GREEN))
                    .append(Component.text(level >= Profession.MAX_LEVEL ? "  •  MAX" : "  •  bis nächstes Level: " + progress, NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text(profession.description(), NamedTextColor.GRAY))
                    .build()));
        }

        List<ActionButton> actions = List.of(dialogueEngine.actionButton(
                Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), body, actions, 1);
    }
}
