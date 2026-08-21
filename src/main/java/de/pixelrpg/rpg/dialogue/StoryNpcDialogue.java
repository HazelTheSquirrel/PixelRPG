package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class StoryNpcDialogue {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public StoryNpcDialogue(PlayerProfileManager profileManager) {
        this(profileManager, new DialogueEngine());
    }

    public StoryNpcDialogue(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    public void begin(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du bist noch nicht für PixelRPG registriert.", NamedTextColor.RED));
            return;
        }

        dialogueEngine.openUnavailable(
                player,
                "Geschichte",
                "Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.");
    }
}
