package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.BankDialog;
import de.pixelrpg.rpg.dialogue.BankStorageService;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

public final class BankerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final BankStorageService bankStorage;
    private final LanguageManager lang;

    public BankerBehavior(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, BankStorageService bankStorage) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.bankStorage = bankStorage;
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

        new BankDialog(profileManager, dialogueEngine, bankStorage).open(player);
    }
}
