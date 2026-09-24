package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class StoryNpcDialogue {
    private final PlayerProfileManager profileManager;
    private final StoryManager storyManager;
    private final DialogueEngine dialogueEngine;
    private final QuestManager questManager;

    public StoryNpcDialogue(PlayerProfileManager profileManager, StoryManager storyManager, DialogueEngine dialogueEngine) {
        this(profileManager, storyManager, dialogueEngine, de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getQuestManager());
    }

    public StoryNpcDialogue(PlayerProfileManager profileManager, StoryManager storyManager,
                            DialogueEngine dialogueEngine, QuestManager questManager) {
        this.profileManager = profileManager;
        this.storyManager = storyManager;
        this.dialogueEngine = dialogueEngine;
        this.questManager = questManager;
    }

    public void openChapter(Player player, StoryChapter chapter) {
        openChapter(player, chapter, null);
    }

    public void openChapter(Player player, StoryChapter chapter, RPGNpc npc) {
        if (player == null || chapter == null) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !storyManager.isNextChapter(player.getUniqueId(), chapter)) return;

        if (npc != null && chapter.id().equals("trail_ruins_archaeology")) {
            openArchaeologist(player, chapter, npc);
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.GOLD)));
        for (String line : chapter.dialogueLines()) body.add(DialogBody.plainMessage(Component.text(line, NamedTextColor.WHITE)));

        List<ActionButton> actions = new ArrayList<>();
        Quest quest = questManager == null || chapter.startQuestId().isBlank() ? null : questManager.getRepository().getQuest(chapter.startQuestId());
        if (quest == null && chapter.completionQuestId().isBlank()) {
            actions.add(dialogueEngine.actionButton(Component.text("Die Reise beginnen"), NamedTextColor.GREEN, target -> {
                if (storyManager.completeChapter(target, chapter)) target.closeDialog();
                else openChapter(target, chapter, npc);
            }));
        }
        if (quest != null && !profile.hasActiveQuest(quest.id()) && !profile.hasCompletedQuest(quest.id())) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN, target -> {
                PlayerProfile current = profileManager.getProfile(target.getUniqueId()).orElse(null);
                if (current == null || !current.isRegistered()) return;
                Quest currentQuest = questManager.getRepository().getQuest(quest.id());
                if (currentQuest != null) questManager.acceptQuest(target, currentQuest);
                openChapter(target, chapter, npc);
            }));
        }

        Quest completionQuest = questManager == null || chapter.completionQuestId().isBlank() ? null : questManager.getRepository().getQuest(chapter.completionQuestId());
        if (npc != null && completionQuest != null && profile.hasActiveQuest(completionQuest.id())) {
            actions.add(dialogueEngine.actionButton(Component.text("Bericht erstatten"), NamedTextColor.AQUA, target -> {
                PlayerProfile current = profileManager.getProfile(target.getUniqueId()).orElse(null);
                if (current == null || !current.isRegistered()) return;
                if (questManager.completeQuestAtNpc(target, completionQuest.id(), npc.id())
                        && storyManager.completeChapter(target, chapter)) {
                    advanceAfterEndCity(target, chapter);
                    target.closeDialog();
                } else {
                    openChapter(target, chapter, npc);
                }
            }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(chapter.title(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void advanceAfterEndCity(Player player, StoryChapter chapter) {
        if (!"campaign_end_city".equals(chapter.id()) || questManager == null) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        Quest epilogue = questManager.getRepository().getQuest("story_campaign_after_the_end");
        if (epilogue == null || profile.hasActiveQuest(epilogue.id()) || profile.hasCompletedQuest(epilogue.id())) return;
        questManager.acceptQuest(player, epilogue);
    }

    private void openArchaeologist(Player player, StoryChapter chapter, RPGNpc npc) {
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Der Archäologe kniet zwischen freigelegten Pflastersteinen.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("„Trail Ruins sind vergrabene Siedlungen einer verlorenen Kultur. Suspicious Gravel kann durch vorsichtiges Bürsten Hinweise bewahren.“", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("„Was wir finden, ist Beleg. Was wir daraus schließen, ist Theorie.“", NamedTextColor.WHITE))
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text("Wer lebte hier?"), NamedTextColor.LIGHT_PURPLE, target -> openArchaeologistTheory(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Zur Untersuchung"), NamedTextColor.AQUA, target -> openChapter(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("Der Archäologe", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openArchaeologistTheory(Player player, StoryChapter chapter, RPGNpc npc) {
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("„Mojangs Quellen nennen Trail Ruins die Reste einer verlorenen Kultur. Mehr ist nicht sicher überliefert.“", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("„Ich vermute, dass ihre Markierungen später in anderen Ruinen wiederverwendet wurden. Nicht zwingend von derselben Kultur – vielleicht von denselben Ideen.“", NamedTextColor.LIGHT_PURPLE)),
                DialogBody.plainMessage(Component.text("„Wenn ich recht habe, sind diese Straßen der erste Faden eines Netzes, das bis unter den Stein führt.“", NamedTextColor.LIGHT_PURPLE))
        );
        dialogueEngine.openMultiAction(player, Component.text("Eine Theorie", NamedTextColor.GOLD), body,
                List.of(
                        dialogueEngine.actionButton(Component.text("Zur Untersuchung"), NamedTextColor.AQUA, target -> openChapter(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)
                ), 1);
    }
}
