package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
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
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        Optional<StoryChapter> next = storyManager.getNextChapterFor(player.getUniqueId());
        if (next.isEmpty()) {
            dialogue.begin(player, npc);
            return;
        }
        StoryChapter chapter = next.get();
        dialogueEngine.openMultiAction(
                player,
                Component.text("Geschichte", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.YELLOW))),
                List.of(dialogueEngine.actionButton(Component.text("Kapitel lesen"), NamedTextColor.GREEN, target -> {
                    target.openBook(StoryBookFactory.build(chapter));
                    storyManager.completeChapter(target, chapter);
                    target.sendMessage(Component.text("Kapitel freigeschaltet: " + chapter.title(), NamedTextColor.GREEN));
                })),
                1
        );
    }
}
