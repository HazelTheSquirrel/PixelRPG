package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.BankGUI;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class BankerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final LanguageManager lang;

    public BankerBehavior(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.lang = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.BANKER;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Bank", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Verwalte dein PixelRPG-Bankkonto."))),
                List.of(dialogueEngine.actionButton(
                        Component.text("Bank öffnen"),
                        NamedTextColor.GREEN,
                        target -> new BankGUI(target, profileManager).open(target)
                )),
                1
        );
    }
}
