package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryService;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StoryNpcDialogue {
    private final StoryService storyService;
    private final DialogueEngine dialogueEngine;

    public StoryNpcDialogue(StoryService storyService, DialogueEngine dialogueEngine) {
        this.storyService = Objects.requireNonNull(storyService, "storyService");
        this.dialogueEngine = Objects.requireNonNull(dialogueEngine, "dialogueEngine");
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
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.YELLOW)));
        for (String line : chapter.dialogueLines()) {
            body.add(DialogBody.plainMessage(Component.text(line)));
        }
        if (chapter.expReward() > 0L) {
            body.add(DialogBody.plainMessage(
                    Component.text("Belohnung: " + chapter.expReward() + " XP", NamedTextColor.GREEN)));
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Geschichte", NamedTextColor.GOLD),
                body,
                List.of(dialogueEngine.actionButton(
                        Component.text("Kapitel abschließen"),
                        NamedTextColor.GREEN,
                        target -> storyService.completeChapter(target.getUniqueId(), chapter)
                )),
                1
        );
    }
}
