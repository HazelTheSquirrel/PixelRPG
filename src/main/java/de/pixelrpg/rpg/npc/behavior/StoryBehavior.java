// src/main/java/de/pixelrpg/rpg/npc/behavior/StoryBehavior.java (VOLLSTÄNDIG, ersetzt alte Datei — zurück zum funktionierenden Buch-System, kein Dialog-Bezug)
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class StoryBehavior implements NpcBehavior {

    private final StoryManager storyManager;
    private final PlayerProfileManager profileManager;

    public StoryBehavior(StoryManager storyManager, PlayerProfileManager profileManager) {
        this.storyManager = storyManager;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.STORY;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return;
        }

        Optional<StoryChapter> next = storyManager.getNextChapterFor(player.getUniqueId());
        if (next.isEmpty()) {
            player.sendMessage(Component.text("You have caught up with the story so far. Check back later!", NamedTextColor.YELLOW));
            return;
        }

        StoryChapter chapter = next.get();
        player.openBook(StoryBookFactory.build(chapter));
        storyManager.completeChapter(player, chapter);
        player.sendMessage(Component.text("Chapter unlocked: ", NamedTextColor.GOLD)
                .append(Component.text(chapter.title(), NamedTextColor.YELLOW)));
    }
}