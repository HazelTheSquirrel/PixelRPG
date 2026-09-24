package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class StoryBehavior implements NpcBehavior {
    private final StoryManager storyManager;
    private final StoryNpcDialogue dialogue;
    private final DialogueEngine dialogueEngine;
    private final PlayerProfileManager profileManager;

    public StoryBehavior(StoryManager storyManager, StoryNpcDialogue dialogue, DialogueEngine dialogueEngine, PlayerProfileManager profileManager) {
        this.storyManager = storyManager;
        this.dialogue = dialogue;
        this.dialogueEngine = dialogueEngine;
        this.profileManager = profileManager;
    }

    @Override public NpcType type() { return NpcType.STORY; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (player == null || npc == null || !profileManager.isRegistered(player.getUniqueId())) {
            if (player != null) {
                dialogueEngine.openUnavailable(player, "Geschichte", "Du musst zuerst registriertes Rathausmitglied sein.");
            }
            return;
        }

        Optional<StoryChapter> next = storyManager.getNextChapterFor(player.getUniqueId());
        if (next.isEmpty()) {
            dialogue.begin(player, npc);
            return;
        }

        dialogue.openChapter(player, next.get());
    }
}
