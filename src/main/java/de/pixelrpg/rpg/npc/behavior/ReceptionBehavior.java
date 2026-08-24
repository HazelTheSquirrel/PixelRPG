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

public final class ReceptionBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final PartyManager partyManager;
    private final GuildManager guildManager;

    public ReceptionBehavior(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager, GuildManager guildManager) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.partyManager = partyManager;
        this.guildManager = guildManager;
    }

    @Override
    public NpcType type() {
        return NpcType.RECEPTION;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open();
    }
}
