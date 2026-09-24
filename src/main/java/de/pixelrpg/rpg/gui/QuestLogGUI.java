package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestService;
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

/** Inventory quest log for active and available quest definitions. */
public final class QuestLogGUI extends AbstractGUI {
    private final Player viewer;
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final ItemService itemService;

    public QuestLogGUI(Player viewer, QuestService quests, PlayerProfileManager profiles, ItemService itemService) {
        super(54, Component.text("Questlog", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.quests = quests;
        this.profiles = profiles;
        this.itemService = itemService;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profiles.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Quest> definitions = quests.repository().getAllQuests().stream()
                .sorted(Comparator.comparingInt((Quest q) -> statusOrder(profile, q))
                        .thenComparing(Quest::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (definitions.isEmpty()) {
            setItem(22, named(Material.BARRIER, "Keine Quests verfügbar", NamedTextColor.RED));
        } else {
            for (int slot = 0; slot < Math.min(45, definitions.size()); slot++) {
                Quest quest = definitions.get(slot);
                setItem(slot, questItem(profile, quest),
                        event -> new QuestDetailGUI(viewer, quests, profiles, quest).open(viewer));
            }
        }
        setItem(49, named(Material.ARROW, "Schließen", NamedTextColor.RED), event -> viewer.closeInventory());
    }

    private ItemStack questItem(PlayerProfile profile, Quest quest) {
        QuestStatus status = status(profile, quest);
        Material material = quest.type() == QuestType.COLLECT ? Material.BUNDLE : Material.WRITTEN_BOOK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(quest.title(), status.color()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(status.label(), status.color()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Ziel: " + quest.targetKey(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Menge: " + quest.requiredAmount(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Benötigtes Level: " + quest.requiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        var progress = profile.getActiveQuests().get(quest.id());
        if (progress != null) {
            lore.add(Component.text("Fortschritt: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private QuestStatus status(PlayerProfile profile, Quest quest) {
        if (profile.hasCompletedQuest(quest.id())) return QuestStatus.COMPLETED;
        if (profile.hasActiveQuest(quest.id())) return QuestStatus.ACTIVE;
        return quests.canAccept(profile, quest) ? QuestStatus.AVAILABLE : QuestStatus.UNAVAILABLE;
    }

    private int statusOrder(PlayerProfile profile, Quest quest) { return status(profile, quest).ordinal(); }

    private ItemStack named(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
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
        QuestStatus(String label, NamedTextColor color) { this.label = label; this.color = color; }
        String label() { return label; }
        NamedTextColor color() { return color; }
    }
}
