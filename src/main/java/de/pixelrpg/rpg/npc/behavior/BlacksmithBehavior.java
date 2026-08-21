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
}
