package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueContext;
import de.pixelrpg.rpg.dialogue.DialogueDomainState;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.DialogueTreeService;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcProfileStore;
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
    private final StoryNpcDialogue fallbackDialogue;
    private final DialogueEngine dialogueEngine;
    private final PlayerProfileManager profileManager;
    private final DialogueTreeService dialogueTreeService;
    private final NpcProfileStore profileStore;
    private final DialogueDomainState domainState;

    public StoryBehavior(
            StoryManager storyManager,
            StoryNpcDialogue fallbackDialogue,
            DialogueEngine dialogueEngine,
            PlayerProfileManager profileManager,
            DialogueTreeService dialogueTreeService,
            NpcProfileStore profileStore,
            DialogueDomainState domainState) {
        this.storyManager = storyManager;
        this.fallbackDialogue = fallbackDialogue;
        this.dialogueEngine = dialogueEngine;
        this.profileManager = profileManager;
        this.dialogueTreeService = dialogueTreeService;
        this.profileStore = profileStore;
        this.domainState = domainState;
    }

    @Override
    public NpcType type() {
        return NpcType.STORY;
    }

    // Zuständig für den storyabhängigen Einstieg eines Story-NPCs und das anschließende Öffnen des bestehenden Storysystems.
    @Override
    public void onInteract(DialogueContext context) {
        Player player = context.player();
        RPGNpc npc = context.npc();
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }

        Optional<StoryChapter> next = storyManager.getNextChapterFor(player.getUniqueId());
        Runnable chapterCompletion = () -> openNextChapterOrFallback(player, npc, next);

        String treeId = profileStore.getOrCreate(npc).dialogueTreeId();
        dialogueTreeService.open(
                player,
                context,
                treeId,
                chapterCompletion);
    }

    private void openNextChapterOrFallback(Player player, RPGNpc npc, Optional<StoryChapter> next) {
        if (next.isEmpty()) {
            fallbackDialogue.begin(player, npc);
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
                            target.openBook(StoryBookFactory.build(chapter));
                            storyManager.completeChapter(target, chapter);
                            target.sendMessage(Component.text(
                                    "Kapitel freigeschaltet: " + chapter.title(),
                                    NamedTextColor.GREEN));
                        })),
                1);
    }
}
