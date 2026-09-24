package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ReceptionDialog;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class ReceptionBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final PartyManager partyManager;

    public ReceptionBehavior(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.partyManager = partyManager;
    }

    @Override
    public NpcType type() { return NpcType.RECEPTION; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        onInteract(player, npc, Player::closeDialog);
    }

    @Override
    public void onInteract(Player player, RPGNpc npc, Consumer<Player> backAction) {

        GuildManager guildManager = GuildManager.getInstance(de.pixelrpg.rpg.PixelRPGPlugin.getInstance(), profileManager);
        new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager, backAction).open();
    }
}
