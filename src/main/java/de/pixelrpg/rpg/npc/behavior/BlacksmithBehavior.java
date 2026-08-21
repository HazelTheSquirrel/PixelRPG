package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.gui.CraftingGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class BlacksmithBehavior implements NpcBehavior {
    private final BlacksmithGUI blacksmithGUI;
    private final CraftingGUI craftingGUI;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final LanguageManager lang;

    public BlacksmithBehavior(BlacksmithGUI blacksmithGUI, CraftingGUI craftingGUI,
                              PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.blacksmithGUI = blacksmithGUI;
        this.craftingGUI = craftingGUI;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.BLACKSMITH;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        if (npc.type() == NpcType.PROFESSION_TRAINER) {
            openProfessionTrainer(player, npc);
            return;
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Schmied", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(
                        "Ausrüstung verbessern, Gegenstände verwalten und neue Ausrüstung schmieden."))),
                List.of(
                        dialogueEngine.actionButton(
                                Component.text("Ausrüstung verwalten"),
                                NamedTextColor.GREEN,
                                target -> blacksmithGUI.open(target)
                        ),
                        dialogueEngine.actionButton(
                                Component.text("Schmieden"),
                                NamedTextColor.YELLOW,
                                target -> craftingGUI.open(target, Profession.BLACKSMITHING)
                        )
                ),
                2
        );
    }

    private void openProfessionTrainer(Player player, RPGNpc npc) {
        Profession profession = npc.profession();
        if (profession == null) {
            dialogueEngine.openUnavailable(player, "Handwerkslehrer", "Dieser NPC hat keine Profession zugewiesen.");
            return;
        }

        String displayName = displayName(profession);
        boolean craftingProfession = switch (profession) {
            case BLACKSMITHING, LEATHERWORKING, TAILORING, ALCHEMY, COOKING -> true;
            default -> false;
        };

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Ich bin dein Lehrer für " + displayName + ".", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Deine Profession steigt durch das Ausüben des entsprechenden Handwerks.", NamedTextColor.GRAY))
        );

        if (!craftingProfession) {
            dialogueEngine.openNotice(
                    player,
                    Component.text(displayName + "-Lehrer", NamedTextColor.BLUE),
                    Component.text("Dein Fortschritt wird automatisch gesammelt, wenn du " + displayName + " ausübst."),
                    Component.text("Schließen", NamedTextColor.GREEN)
            );
            return;
        }

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
