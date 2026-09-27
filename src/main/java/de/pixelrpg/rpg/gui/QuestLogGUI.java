package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ReceptionDialog;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.quest.QuestText;
import de.pixelrpg.rpg.quest.QuestType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class QuestLogGUI extends AbstractGUI {
    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestLogGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Questlog", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Quest> quests = questManager.getRepository().getAllQuests().stream()
                .sorted(Comparator.comparingInt((Quest quest) -> statusOrder(profile, quest))
                        .thenComparing(QuestText::titlePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (quests.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("Keine Quests verfügbar", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            empty.setItemMeta(meta);
            setItem(22, empty);
        } else {
            int slot = 0;
            for (Quest quest : quests) {
                if (slot >= 45) break;
                QuestProgress progress = profile.getActiveQuests().get(quest.id());
                QuestStatus status = status(profile, quest);
                NamedTextColor statusColor = status.color();

                ItemStack item = questItem(quest, progress, status, statusColor);
                setItem(slot++, item, event -> new QuestDetailGUI(viewer, questManager, profileManager, quest).open(viewer));
            }
        }
        setItem(49, backButton(), event -> new ReceptionDialog(viewer, profileManager, new DialogueEngine()).open());
    }

    private ItemStack questItem(Quest quest, QuestProgress progress, QuestStatus status, NamedTextColor color) {
        ItemStack item = new ItemStack(quest.type() == QuestType.COLLECT ? Material.BUNDLE : Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        Component title = QuestText.title(viewer, quest).color(color).decoration(TextDecoration.ITALIC, false);
        meta.displayName(title);
        meta.itemName(title);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(status.label(), color).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(QuestText.description(viewer, quest).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(QuestText.objective(viewer, quest).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        if (quest.type() == QuestType.COLLECT) {
            lore.add(QuestText.requiredItem(viewer, quest).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        if (progress != null) {
            lore.add(Component.text("Fortschritt: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            if (progress.hasExpiry()) lore.add(Component.text("Zeitlich begrenzte Quest.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        if (status == QuestStatus.UNAVAILABLE) {
            lore.add(Component.text("Voraussetzungen noch nicht erfüllt.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Für Details klicken.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private QuestStatus status(PlayerProfile profile, Quest quest) {
        if (profile.hasCompletedQuest(quest.id())) return QuestStatus.COMPLETED;
        if (profile.hasActiveQuest(quest.id())) return QuestStatus.ACTIVE;
        return questManager.canAccept(profile, quest) ? QuestStatus.AVAILABLE : QuestStatus.UNAVAILABLE;
    }

    private int statusOrder(PlayerProfile profile, Quest quest) {
        return status(profile, quest).ordinal();
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Zurück", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private enum QuestStatus {
        AVAILABLE("Verfügbar", NamedTextColor.GREEN),
        ACTIVE("Angenommen", NamedTextColor.YELLOW),
        UNAVAILABLE("Nicht verfügbar", NamedTextColor.RED),
        COMPLETED("Abgeschlossen", NamedTextColor.GRAY);

        private final String label;
        private final NamedTextColor color;

        QuestStatus(String label, NamedTextColor color) {
            this.label = label;
            this.color = color;
        }

        String label() { return label; }
        NamedTextColor color() { return color; }
    }
}
