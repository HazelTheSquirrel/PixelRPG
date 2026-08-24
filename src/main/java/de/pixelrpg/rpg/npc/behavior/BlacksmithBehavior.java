package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ProfessionDialog;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.gui.CraftingGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import de.pixelrpg.rpg.quest.QuestManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native-dialog blacksmith NPC with blacksmith quests, profession learning and recipe access. */
public final class BlacksmithBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final DialogueEngine dialogueEngine;
    private final ProfessionDialog professionDialog;
    private final QuestManager questManager;

    public BlacksmithBehavior(PlayerProfileManager profileManager,
                              ProfessionService professionService,
                              CraftingService craftingService,
                              QuestManager questManager,
                              DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine);
        this.questManager = questManager;
    }

    /** Compatibility constructor for the existing plugin bootstrap; legacy GUI parameters remain accepted. */
    public BlacksmithBehavior(BlacksmithGUI ignoredBlacksmithGUI, CraftingGUI ignoredCraftingGUI,
                              PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this(profileManager,
                PixelRPGPlugin.getInstance().getProfessionSystem().professionService(),
                PixelRPGPlugin.getInstance().getProfessionSystem().craftingService(),
                PixelRPGPlugin.getInstance().getQuestManager(),
                dialogueEngine);
    }

    @Override
    public NpcType type() {
        return NpcType.BLACKSMITH;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, "Schmied", "Du musst zuerst Rathausmitglied sein.");
            return;
        }

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        int level = professionService.getLevel(player.getUniqueId(), Profession.BLACKSMITH);
        long experience = professionService.getExperience(player.getUniqueId(), Profession.BLACKSMITH);
        long next = level >= Profession.MAX_LEVEL ? 0L : ProfessionService.experienceForLevel(level + 1);
        boolean learned = profile.hasLearnedProfession(Profession.BLACKSMITH);

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Waffen, Rüstung und Werkzeuge aus Meisterhand.", NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text(
                learned
                        ? level >= Profession.MAX_LEVEL
                            ? "Schmied Level " + Profession.MAX_LEVEL + " – Meister"
                            : "Schmied Level " + level + "/" + Profession.MAX_LEVEL + " • " + experience + "/" + next + " EP"
                        : "Du hast den Beruf Schmied noch nicht erlernt.",
                learned ? NamedTextColor.YELLOW : NamedTextColor.RED)));

        List<ActionButton> actions = new ArrayList<>();
        if (questManager != null) {
            actions.add(dialogueEngine.actionButton(Component.text("Schmiedequests"), NamedTextColor.YELLOW,
                    target -> new QuestBehavior(questManager, profileManager, dialogueEngine).onInteract(target, npc)));
        }

        if (!learned) {
            actions.add(dialogueEngine.actionButton(Component.text("Schmied erlernen"), NamedTextColor.GREEN,
                    target -> {
                        professionService.learn(target, Profession.BLACKSMITH);
                        onInteract(target, npc);
                    }));
        } else {
            actions.add(dialogueEngine.actionButton(Component.text("Rezepte kaufen / verwalten"), NamedTextColor.BLUE,
                    target -> professionDialog.openTrainerRecipes(target, Profession.BLACKSMITH)));
        }

        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogueEngine.openMultiAction(
                player,
                Component.text("Schmied", NamedTextColor.GOLD),
                body,
                actions,
                1);
    }
}
