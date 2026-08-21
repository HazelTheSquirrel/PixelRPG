package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.CraftingGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingGUI craftingGUI;
    private final DialogueEngine dialogueEngine;

    public ProfessionTrainerBehavior(PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     CraftingGUI craftingGUI,
                                     DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.craftingGUI = craftingGUI;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() {
        return NpcType.PROFESSION_TRAINER;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst zuerst Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        if (!npc.isProfessionTrainer()) {
            dialogueEngine.openUnavailable(player, "Handwerkslehrer", "Dieser NPC wurde noch keiner Profession zugewiesen.");
            return;
        }

        Profession profession = npc.profession();
        int level = professionService.getLevel(player.getUniqueId(), profession);
        long experience = professionService.getExperience(player.getUniqueId(), profession);
        long nextLevel = ProfessionService.experienceForLevel(Math.min(Profession.MAX_LEVEL, level + 1));
        String displayName = displayName(profession);
        String progressText = level >= Profession.MAX_LEVEL
                ? "Level 100 – Meister"
                : "Level " + level + " • " + experience + "/" + nextLevel + " EP";

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Ich bilde dich in " + displayName + " aus.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(progressText, level >= Profession.MAX_LEVEL ? NamedTextColor.GREEN : NamedTextColor.GRAY))
        );

        if (isCraftingProfession(profession)) {
            dialogueEngine.openMultiAction(
                    player,
                    Component.text(displayName + "-Lehrer", NamedTextColor.BLUE),
                    body,
                    List.of(dialogueEngine.actionButton(
                            Component.text("Handwerk öffnen"),
                            NamedTextColor.GREEN,
                            target -> craftingGUI.open(target, profession)
                    )),
                    1
            );
            return;
        }

        dialogueEngine.openNotice(
                player,
                Component.text(displayName + "-Lehrer", NamedTextColor.BLUE),
                Component.text("Deine Profession steigt durch das Ausüben des entsprechenden Handwerks.\n\n" + progressText),
                Component.text("Schließen", NamedTextColor.GREEN)
        );
    }

    private boolean isCraftingProfession(Profession profession) {
        return switch (profession) {
            case BLACKSMITHING, LEATHERWORKING, TAILORING, ALCHEMY, COOKING -> true;
            default -> false;
        };
    }

    private String displayName(Profession profession) {
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
