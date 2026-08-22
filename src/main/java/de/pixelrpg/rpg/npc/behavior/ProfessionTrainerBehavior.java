package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ProfessionDialog;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native-dialog profession trainer for learning professions and inspecting recipes. */
public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final DialogueEngine dialogueEngine;
    private final ProfessionDialog professionDialog;

    public ProfessionTrainerBehavior(PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     CraftingService craftingService,
                                     DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine);
    }

    @Override
    public NpcType type() {
        return NpcType.PROFESSION_TRAINER;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, "Berufslehrer", "Du musst zuerst Mitglied der Gilde sein.");
            return;
        }
        if (!npc.isProfessionTrainer()) {
            dialogueEngine.openUnavailable(player, "Berufslehrer", "Dieser NPC hat noch keinen Beruf zugewiesen.");
            return;
        }

        Profession profession = npc.profession();
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean learned = profile.hasLearnedProfession(profession);
        int level = professionService.getLevel(player.getUniqueId(), profession);

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text(
                learned ? "Beruf Level " + level + "/" + Profession.MAX_LEVEL : "Dieser Beruf ist noch nicht erlernt.",
                learned ? NamedTextColor.YELLOW : NamedTextColor.RED)));

        List<ActionButton> actions = new ArrayList<>();
        if (!learned) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Beruf erlernen"),
                    NamedTextColor.GREEN,
                    target -> {
                        professionService.learn(target, profession);
                        professionDialog.open(target);
                    }));
        } else {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Rezepte und Details öffnen"),
                    NamedTextColor.GREEN,
                    target -> professionDialog.openTrainerRecipes(target, profession)));
        }

        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(
                player,
                Component.text(profession.displayName() + "-Lehrer", NamedTextColor.GOLD),
                body,
                actions,
                1);
    }
}
