package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestText;
import de.pixelrpg.rpg.quest.QuestType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** Native quest-board dialog. Empty questGiverNpcId values are intentionally offered by every QUEST NPC. */
public final class QuestBehaviorV2 implements NpcBehavior {
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public QuestBehaviorV2(QuestManager questManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() { return NpcType.QUEST; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst zuerst Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        openQuestRanges(player, npc);
    }

    private boolean belongsToNpc(Quest quest, RPGNpc npc) {
        return quest.questGiverNpcId().isBlank() || npc.id().equalsIgnoreCase(quest.questGiverNpcId());
    }

    private void openQuestRanges(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        List<Integer> levels = new TreeSet<>(questManager.getRepository().getAllQuests().stream()
                .filter(quest -> quest.type() != QuestType.GLOBAL_EVENT)
                .filter(quest -> belongsToNpc(quest, npc))
                .map(Quest::categoryLevel).toList()).stream().toList();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Quests dieses Auftraggebers", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Dein Level: " + profile.getLevel(), NamedTextColor.AQUA)));
        List<ActionButton> actions = new ArrayList<>();
        for (int index = 0; index < levels.size(); index++) {
            int start = levels.get(index);
            int end = index + 1 < levels.size() ? levels.get(index + 1) - 1 : 99;
            actions.add(dialogueEngine.actionButton(Component.text("Level " + start + "–" + end),
                    profile.getLevel() >= start ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openQuestCategory(target, npc, start, end)));
        }
        if (actions.isEmpty()) {
            dialogueEngine.openNotice(player, Component.text("Questgeber", NamedTextColor.GOLD),
                    Component.text("Dieser NPC hat aktuell keine Quests.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        dialogueEngine.openMultiAction(player, Component.text("Questgeber", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openQuestCategory(Player player, RPGNpc npc, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        List<Quest> quests = questManager.getRepository().getAllQuests().stream()
                .filter(quest -> quest.type() != QuestType.GLOBAL_EVENT)
                .filter(quest -> belongsToNpc(quest, npc))
                .filter(quest -> quest.categoryLevel() == start)
                .sorted(Comparator.comparingInt(Quest::requiredLevel).thenComparing(Quest::title)).toList();
        List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text("Level " + start + "–" + end, NamedTextColor.AQUA)));
        List<ActionButton> actions = new ArrayList<>();
        for (Quest quest : quests) {
            boolean active = profile.hasActiveQuest(quest.id());
            boolean available = questManager.canAccept(profile, quest);
            actions.add(dialogueEngine.actionButton(Component.text((active ? "[Aktiv] " : "") + quest.title()),
                    active ? NamedTextColor.YELLOW : available ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openQuestDetails(target, npc, quest, start, end)));
        }
        if (actions.isEmpty()) body = List.of(DialogBody.plainMessage(Component.text("Keine Quests verfügbar.", NamedTextColor.WHITE)));
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> openQuestRanges(target, npc)));
        dialogueEngine.openMultiAction(player, Component.text("Questbereich", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openQuestDetails(Player player, RPGNpc npc, Quest quest, int start, int end) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(quest.description(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(QuestText.objective(quest)));
        if (active) body.add(DialogBody.plainMessage(Component.text("Fortschritt: " + profile.getActiveQuests().get(quest.id()).getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN)));
        body.add(DialogBody.plainMessage(Component.text("Belohnung: " + quest.rewardMoney() + " Gold, " + quest.rewardExp() + " EP", NamedTextColor.GOLD)));
        List<ActionButton> actions = new ArrayList<>();
        if (!active && !completed && questManager.canAccept(profile, quest)) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN, target -> {
                questManager.acceptQuest(target, quest);
                openQuestCategory(target, npc, start, end);
            }));
        }
        if (active) {
            int current = profile.getActiveQuests().get(quest.id()).getCurrentAmount();
            if (current >= quest.requiredAmount()) actions.add(dialogueEngine.actionButton(Component.text("Quest abgeben"), NamedTextColor.GREEN, target -> {
                questManager.completeQuest(target, quest.id());
                openQuestCategory(target, npc, start, end);
            }));
            actions.add(dialogueEngine.actionButton(Component.text("Quest abbrechen"), NamedTextColor.RED, target -> {
                questManager.abandonQuest(target, quest.id());
                openQuestCategory(target, npc, start, end);
            }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> openQuestCategory(target, npc, start, end)));
        dialogueEngine.openMultiAction(player, Component.text(quest.title(), NamedTextColor.GOLD), body, actions, 2);
    }
}
