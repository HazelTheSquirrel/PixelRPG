package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
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

        if (npc != null) {
            switch (chapter.id()) {
                case "trail_ruins_archaeology" -> {
                    openArchaeologist(player, chapter, npc);
                    return;
                }
                case "campaign_stronghold" -> {
                    openStronghold(player, chapter, npc);
                    return;
                }
                case "campaign_dragon" -> {
                    openDragon(player, chapter, npc);
                    return;
                }
                case "campaign_end_city" -> {
                    openEndCity(player, chapter, npc);
                    return;
                }
                case "after_the_end" -> {
                    openAfterTheEnd(player, chapter, npc);
                    return;
                }
                default -> {
                }
            }
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
        // Story quests are accepted exclusively at the central reception. This NPC only advances active story steps or lore.
        Quest completionQuest = questManager == null || chapter.completionQuestId().isBlank() ? null : questManager.getRepository().getQuest(chapter.completionQuestId());
        if (npc != null && completionQuest != null && profile.hasActiveQuest(completionQuest.id())) {
            actions.add(dialogueEngine.actionButton(Component.text("Bericht erstatten"), NamedTextColor.AQUA, target -> {
                PlayerProfile current = profileManager.getProfile(target.getUniqueId()).orElse(null);
                if (current == null || !current.isRegistered()) return;
                if (questManager.completeQuestAtNpc(target, completionQuest.id(), npc.id())
                        && storyManager.isChapterArchived(target.getUniqueId(), chapter)) {
                    target.closeDialog();
                } else {
                    openChapter(target, chapter, npc);
                }
            }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(chapter.title(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void openStronghold(Player player, StoryChapter chapter, RPGNpc npc) {
        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text("Was wissen wir über das Portal?"), NamedTextColor.AQUA,
                target -> openStrongholdPortal(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Was könnte sie vertrieben haben?"), NamedTextColor.LIGHT_PURPLE,
                target -> openStrongholdEscape(target, chapter, npc)));
        addCompletionAction(actions, player, chapter, npc, "Bericht erstatten", NamedTextColor.GREEN);
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogueEngine.openMultiAction(
                player,
                Component.text("Oren – Die Verzweiflungstat", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Die Strongholds sind Ruinen einer unbekannten Vergangenheit. Sicher ist nur: In ihren Tiefen liegen die Endportale.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Warum jemand einen Weg in eine fremde Dimension baute, wissen wir nicht. Meine Theorie: Die Erbauer suchten einen Ausweg, als ihre eigene Welt ihnen keine Zukunft mehr bot.“",
                                NamedTextColor.WHITE))
                ),
                actions, 1);
    }

    private void addCompletionAction(List<ActionButton> actions, Player player, StoryChapter chapter,
                                      RPGNpc npc, String label, NamedTextColor color) {
        if (questManager == null || npc == null) return;
        Quest quest = chapter.completionQuestId().isBlank()
                ? null
                : questManager.getRepository().getQuest(chapter.completionQuestId());
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (quest == null || profile == null || !profile.hasActiveQuest(quest.id())) return;

        QuestProgress progress = profile.getActiveQuests().get(quest.id());
        if (progress == null || progress.getCurrentAmount() < quest.requiredAmount()) return;

        actions.add(dialogueEngine.actionButton(Component.text(label), color, target -> {
            if (questManager.completeQuestAtNpc(target, quest.id(), npc.id())
                    && storyManager.isChapterArchived(target.getUniqueId(), chapter)) {
                target.closeDialog();
            } else {
                openChapter(target, chapter, npc);
            }
        }));
    }

    private void openStrongholdPortal(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Das Tor zum Ende", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(
                        "„Das Portal ist kein Beweis für eine Fluchtgeschichte. Es beweist nur, dass jemand den Übergang vorbereitet hat. Alles darüber hinaus ist Rekonstruktion.“",
                        NamedTextColor.WHITE))),
                List.of(
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openStronghold(target, chapter, npc))
                ), 1);
    }

    private void openStrongholdEscape(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Die letzte Entscheidung", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Wenn Sculk, der Wither und die Kämpfe im Nether zeitlich zusammengehören, könnte das Portal ein letzter Versuch gewesen sein. Aber wir besitzen keinen Beweis für diese Reihenfolge.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Darum steht in meinem Bericht nicht: So war es. Dort steht: So könnte es gewesen sein.“",
                                NamedTextColor.LIGHT_PURPLE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openStronghold(target, chapter, npc))
                ), 1);
    }

    private void openDragon(Player player, StoryChapter chapter, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        Quest quest = questManager == null ? null : questManager.getRepository().getQuest(chapter.startQuestId());
        if (quest == null) return;

        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "„Du hast den Weg durch die Stronghold-Ruinen geöffnet. Vor dir liegt das Reich des Enderdrachen.“",
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "„Die Rolle des Drachen ist nicht vollständig erklärt. Minecraft zeigt uns seine Herrschaft über das Endzentrum – nicht, warum er dort ist.“",
                        NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        // The reception is the only place where this story quest can be accepted.
        if (active) {
            QuestProgress progress = profile.getActiveQuests().get(quest.id());
            if (progress != null && progress.getCurrentAmount() >= quest.requiredAmount()) {
                actions.add(dialogueEngine.actionButton(Component.text("Den Sieg melden"), NamedTextColor.GREEN,
                        target -> {
                            if (questManager.completeQuestAtNpc(target, quest.id(), npc.id())
                                    && storyManager.isChapterArchived(target.getUniqueId(), chapter)) target.closeDialog();
                            else openDragon(target, chapter, npc);
                        }));
            }
        }
        actions.add(dialogueEngine.actionButton(Component.text("Warum der Drache?"), NamedTextColor.LIGHT_PURPLE,
                target -> openDragonTheory(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("Der Drache", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openDragonTheory(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Eine offene Frage", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Der Drache bewacht das Zentrum des Endes. Nach seinem Tod öffnen sich die Wege zu den äußeren Inseln. Das ist beobachtbar.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Ob er ein Wächter, ein Gefangener oder einfach ein Bewohner dieser Dimension ist, können wir nicht beweisen. Unsere Chronik nennt alle drei Möglichkeiten.“",
                                NamedTextColor.LIGHT_PURPLE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openDragon(target, chapter, npc))
                ), 1);
    }

    private void openEndCity(Player player, StoryChapter chapter, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        Quest quest = questManager == null ? null : questManager.getRepository().getQuest(chapter.startQuestId());
        if (quest == null) return;

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Silex steht zwischen Purpur und Endstein. „Hier draußen fühlt sich die Welt nicht wie eine Fortsetzung an. Eher wie ein zurückgelassenes Kapitel.“",
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "„End Cities und End Ships existieren. Elytren existieren. Wer diese Bauwerke errichtete und warum sie verlassen wurden, wissen wir nicht.“",
                        NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text("Die Theorie über die Endermen"), NamedTextColor.LIGHT_PURPLE,
                target -> openEndermanTheory(target, chapter, npc)));
        actions.add(dialogueEngine.actionButton(Component.text("Die letzte Expedition"), NamedTextColor.AQUA,
                target -> openChapter(target, chapter, npc)));
        if (profile.hasActiveQuest(quest.id())) {
            QuestProgress progress = profile.getActiveQuests().get(quest.id());
            if (progress != null && progress.getCurrentAmount() >= quest.requiredAmount()) {
                actions.add(dialogueEngine.actionButton(Component.text("Bericht erstatten"), NamedTextColor.GREEN,
                        target -> {
                            if (questManager.completeQuestAtNpc(target, quest.id(), npc.id())
                                    && storyManager.isChapterArchived(target.getUniqueId(), chapter)) {
                                target.closeDialog();
                            } else {
                                openEndCity(target, chapter, npc);
                            }
                        }));
            }
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("Silex – Was hinter dem Ende bleibt", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openEndermanTheory(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Die Endermen-Frage", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Endermen sind im End heimisch. Chorus, Teleportation und ihre Beziehung zu den End-Inseln sind beobachtbar.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Unsere Chronik vermutet, dass eine frühere Bevölkerung sich über Generationen an das End angepasst haben könnte. Das ist unsere Theorie – keine bestätigte Minecraft-Geschichte.“",
                                NamedTextColor.LIGHT_PURPLE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Was bleibt von den Erbauern?"), NamedTextColor.YELLOW,
                                target -> openEndLegacy(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openEndCity(target, chapter, npc))
                ), 1);
    }

    private void openEndLegacy(Player player, StoryChapter chapter, RPGNpc npc) {
        dialogueEngine.openMultiAction(
                player,
                Component.text("Das zurückgelassene Erbe", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Vielleicht waren die End Cities ein Zufluchtsort. Vielleicht ein Außenposten. Vielleicht etwas völlig anderes.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Wir haben Strukturen, Gegenstände und Wege. Was fehlt, ist die Stimme ihrer Erbauer.“",
                                NamedTextColor.WHITE))
                ),
                List.of(
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                                target -> openEndCity(target, chapter, npc))
                ), 1);
    }

    private void openAfterTheEnd(Player player, StoryChapter chapter, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        Quest quest = questManager == null ? null : questManager.getRepository().getQuest(chapter.startQuestId());
        if (quest == null) return;

        QuestProgress progress = profile.getActiveQuests().get(quest.id());
        boolean ready = progress != null && progress.getCurrentAmount() >= quest.requiredAmount();

        dialogueEngine.openMultiAction(
                player,
                Component.text("Das letzte Archiv", NamedTextColor.GOLD),
                List.of(
                        DialogBody.plainMessage(Component.text(
                                "„Du hast Strongholds, das End, den Drachen und die äußeren Städte gesehen. Jetzt beginnt der schwierigste Teil: entscheiden, was wir wirklich wissen.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Sculk, Wither, Nether, Portale und End sind verbunden durch Spuren – nicht durch eine bestätigte Chronik. Die Lücken gehören zur Geschichte.“",
                                NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.text(
                                "„Schreibe den letzten Bericht. Nicht als Antwort. Als Vermächtnis für den nächsten Forscher.“",
                                NamedTextColor.WHITE))
                ),
                List.of(
                        ready
                                ? dialogueEngine.actionButton(Component.text("Das letzte Kapitel abschließen"), NamedTextColor.GREEN,
                                target -> {
                                    if (questManager.completeQuestAtNpc(target, quest.id(), npc.id())
                                            && storyManager.completeChapter(target, chapter)) {
                                        target.sendMessage(Component.text("Die Hauptkampagne ist abgeschlossen.", NamedTextColor.GOLD));
                                        target.closeDialog();
                                    } else {
                                        openAfterTheEnd(target, chapter, npc);
                                    }
                                })
                                : dialogueEngine.actionButton(Component.text("Die Chronik lesen"), NamedTextColor.YELLOW,
                                target -> openEndLegacy(target, chapter, npc)),
                        dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog)
                ), 1);
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
        addCompletionAction(actions, player, chapter, npc, "Bericht erstatten", NamedTextColor.GREEN);
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
                        dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> openArchaeologist(target, chapter, npc))
                ), 1);
    }
}
