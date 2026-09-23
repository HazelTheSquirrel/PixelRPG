package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.content.ContentCatalogService;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryService;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public final class StoryNpcDialogue {
    private final StoryService storyService;
    private final NpcRuntimeManager npcRuntime;
    private final DialogueEngine dialogueEngine;
    private final StoryBookFactory bookFactory;

    public StoryNpcDialogue(
            StoryService storyService,
            NpcRuntimeManager npcRuntime,
            DialogueEngine dialogueEngine,
            StoryBookFactory bookFactory
    ) {
        this.storyService = storyService;
        this.npcRuntime = npcRuntime;
        this.dialogueEngine = dialogueEngine;
        this.bookFactory = bookFactory;
    }

    public void open(Player player, RPGNpc npc) {
        if (!storyService.isRegistered(player)) {
            dialogueEngine.openUnavailable(player, "Geschichte",
                    "Du bist noch nicht für PixelRPG registriert.");
            return;
        }

        Optional<StoryChapter> next = storyService.nextChapter(player.getUniqueId());
        if (next.isEmpty()) {
            dialogueEngine.openUnavailable(player, "Geschichte",
                    "Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.");
            return;
        }

        StoryChapter chapter = next.get();
        dialogueEngine.openMultiAction(
                player,
                Component.text("Geschichte", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.YELLOW))),
                List.of(dialogueEngine.actionButton(
                        Component.text("Kapitel lesen"),
                        NamedTextColor.GREEN,
                        target -> {
                            target.openBook(bookFactory.build(chapter));
                            if (!storyService.completeChapter(target.getUniqueId(), chapter)) {
                                target.closeDialog();
                            }
                        }
                )),
                1
        );
    }
}
