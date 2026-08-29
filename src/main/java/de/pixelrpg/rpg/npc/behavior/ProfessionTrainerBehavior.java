package de.pixelrpg.rpg.npc.behavior;

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

/** Native profession trainer dialog for learning one profession and opening that trainer's recipes. */
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
                                     DialogueEngine dialogueEngine,
                                     QuickActionsDialogService quickActions) {
        this.type = type;
        this.profession = profession;
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine, quickActions);
    }

    @Override
    public NpcType type() { return type; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, profession.displayName(), "Du musst zuerst Rathausmitglied sein.");
            return;
        }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (profile.hasLearnedProfession(profession)) {
            professionDialog.openTrainerRecipes(player, profession);
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text("Du hast den Beruf " + profession.displayName() + " noch nicht erlernt.", NamedTextColor.RED)));
        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text(profession.displayName() + " erlernen"), NamedTextColor.GREEN,
                target -> {
                    professionService.learn(target, profession);
                    professionDialog.openTrainerRecipes(target, profession);
                }));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1);
    }
}
