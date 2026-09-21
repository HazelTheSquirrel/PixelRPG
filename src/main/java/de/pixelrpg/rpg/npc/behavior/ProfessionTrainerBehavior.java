package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueContext;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.DialogueTreeService;
import de.pixelrpg.rpg.dialogue.ProfessionDialog;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import org.bukkit.entity.Player;

public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final NpcType type;
    private final Profession profession;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final ProfessionDialog professionDialog;
    private final DialogueEngine dialogueEngine;
    private final DialogueTreeService dialogueTreeService;

    public ProfessionTrainerBehavior(NpcType type, Profession profession,
                                     PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     DialogueEngine dialogueEngine,
                                     QuickActionsDialogService quickActions,
                                     DialogueTreeService dialogueTreeService) {
        this.type = type;
        this.profession = profession;
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine, quickActions);
        this.dialogueTreeService = dialogueTreeService;
    }

    @Override
    public NpcType type() { return type; }

    @Override
    public void onInteract(DialogueContext context) {
        Player player = context.player();
        RPGNpc npc = context.npc();
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, profession.displayName(), "Du musst zuerst Rathausmitglied sein.");
            return;
        }
        dialogueTreeService.open(player, DialogueContext.forNpc(player, npc),
                "npc.profession." + profession.name().toLowerCase(),
                () -> openProfessionFunction(player));
    }

    private void openProfessionFunction(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (profile.hasLearnedProfession(profession)) {
            professionDialog.openProfession(player, profession);
            return;
        }
        if (professionService.learn(player, profession)) professionDialog.openTrainerRecipes(player, profession);
    }
}
