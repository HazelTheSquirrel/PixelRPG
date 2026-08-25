package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ProfessionDialog;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
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

/** Native profession trainer dialog for learning one profession and buying its recipes. */
public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final NpcType type;
    private final Profession profession;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final DialogueEngine dialogueEngine;
    private final ProfessionDialog professionDialog;

    public ProfessionTrainerBehavior(NpcType type, Profession profession,
                                     PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     DialogueEngine dialogueEngine) {
        this.type = type;
        this.profession = profession;
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(
                profileManager,
                dialogueEngine,
                new QuickActionsDialogService(profileManager, PixelRPGPlugin.getInstance().getStatEngine()));
    }

    @Override
    public NpcType type() {
        return type;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, profession.displayName(), "Du musst zuerst Rathausmitglied sein.");
            return;
        }

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean learned = profile.hasLearnedProfession(profession);
        int level = professionService.getLevel(player.getUniqueId(), profession);
        long experience = professionService.getExperience(player.getUniqueId(), profession);
        long next = level >= Profession.MAX_LEVEL ? 0L : ProfessionService.experienceForLevel(level + 1);

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text(
                learned
                        ? level >= Profession.MAX_LEVEL
                            ? profession.displayName() + " Level " + Profession.MAX_LEVEL + " – Meister"
                            : profession.displayName() + " Level " + level + "/" + Profession.MAX_LEVEL + " • " + experience + "/" + next + " EP"
                        : "Du hast den Beruf " + profession.displayName() + " noch nicht erlernt.",
                learned ? NamedTextColor.YELLOW : NamedTextColor.RED)));

        List<ActionButton> actions = new ArrayList<>();
        if (!learned) {
            actions.add(dialogueEngine.actionButton(Component.text(profession.displayName() + " erlernen"), NamedTextColor.GREEN,
                    target -> {
                        professionService.learn(target, profession);
                        onInteract(target, npc);
                    }));
        } else {
            actions.add(dialogueEngine.actionButton(Component.text("Rezepte kaufen / verwalten"), NamedTextColor.BLUE,
                    target -> professionDialog.openTrainerRecipes(target, profession)));
        }

        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1);
    }
}
