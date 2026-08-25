package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class StoryNpcDialogue {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final DialogueProgressStore progressStore;

    public StoryNpcDialogue(PlayerProfileManager profileManager) {
        this(profileManager, new DialogueEngine());
    }

    public StoryNpcDialogue(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.progressStore = new DialogueProgressStore(PixelRPGPlugin.getInstance());
        this.progressStore.load();
    }

    public StoryNpcDialogue(PlayerProfileManager profileManager, DialogueEngine dialogueEngine,
                            DialogueProgressStore progressStore) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.progressStore = progressStore;
    }

    public void begin(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du bist noch nicht für PixelRPG registriert.", NamedTextColor.RED));
            return;
        }

        progressStore.markSeen(player.getUniqueId(), "story.npc." + npc.id());
        dialogueEngine.openUnavailable(
                player,
                "Geschichte",
                "Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.");
    }
}
