package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Objects;

/** Entry point for story NPC interactions and the persistent lore campaign. */
public final class StoryNpcDialogue {
    private final PlayerProfileManager profileManager;
    private final StoryDialogueManager storyDialogueManager;

    public StoryNpcDialogue(PlayerProfileManager profileManager,
                            StoryManager storyManager,
                            DialogueEngine dialogueEngine) {
        this.profileManager = Objects.requireNonNull(profileManager, "profileManager");
        this.storyDialogueManager = new StoryDialogueManager(
                Objects.requireNonNull(storyManager, "storyManager"),
                profileManager,
                Objects.requireNonNull(dialogueEngine, "dialogueEngine"));
    }

    public void begin(Player player, RPGNpc npc) {
        if (player == null || npc == null || !profileManager.isRegistered(player.getUniqueId())) {
            if (player != null) {
                player.sendMessage(Component.text(
                        "Du musst registriertes Rathausmitglied sein.",
                        NamedTextColor.RED));
            }
            return;
        }

        storyDialogueManager.openEpilogue(player);
    }

    public void openChapter(Player player, de.pixelrpg.rpg.story.StoryChapter chapter) {
        storyDialogueManager.openChapter(player, chapter);
    }

    public void openChapter(Player player, de.pixelrpg.rpg.story.StoryChapter chapter, RPGNpc npc) {
        storyDialogueManager.openChapter(player, chapter, npc);
    }
}
