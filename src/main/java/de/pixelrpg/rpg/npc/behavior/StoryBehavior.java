// src/main/java/de/pixelrpg/rpg/npc/behavior/StoryBehavior.java (VOLLSTÄNDIG, ersetzt alte Datei — bestehender Buch-Ablauf unangetastet, neuer Zweig nur wenn kein Kapitel fällig ist)
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogueTest;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class StoryBehavior implements NpcBehavior {

    private final StoryManager storyManager;
    private final StoryNpcDialogueTest dialogueTest;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public StoryBehavior(StoryManager storyManager, StoryNpcDialogueTest dialogueTest, PlayerProfileManager profileManager) {
        this.storyManager = storyManager;
        this.dialogueTest = dialogueTest;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.STORY;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        Optional<StoryChapter> next = storyManager.getNextChapterFor(player.getUniqueId());
        if (next.isPresent()) {
            StoryChapter chapter = next.get();
            player.openBook(StoryBookFactory.build(chapter));
            storyManager.completeChapter(player, chapter);
            lang.send(player, "story.chapter-unlocked", "title", chapter.title());
            return;
        }

        // Kein neues Buch-Kapitel fällig: ab hier übernimmt testweise das native
        // Paper Dialog-System (ersetzt die bisherige "story.caught-up"-Zeile).
        dialogueTest.begin(player, npc);
    }
}