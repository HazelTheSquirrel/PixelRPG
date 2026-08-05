package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
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
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public StoryBehavior(StoryManager storyManager, PlayerProfileManager profileManager) {
        this.storyManager = storyManager;
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
        if (next.isEmpty()) {
            lang.send(player, "story.caught-up");
            return;
        }

        StoryChapter chapter = next.get();
        player.openBook(StoryBookFactory.build(chapter));
        storyManager.completeChapter(player, chapter);
        lang.send(player, "story.chapter-unlocked", "title", chapter.title());
    }
}