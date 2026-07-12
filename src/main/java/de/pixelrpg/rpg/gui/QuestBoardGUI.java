// src/main/java/de/pixelrpg/rpg/gui/QuestBoardGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — nach Rang-Kategorie gefiltert, Zurück zur Kategorie-Auswahl)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class QuestBoardGUI extends AbstractGUI {

    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final Rank category;

    public QuestBoardGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager, Rank category) {
        super(54, Component.text("Quest Board - Rank " + category.name(), NamedTextColor.DARK_AQUA));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.category = category;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        List<Quest> quests = questManager.getRepository().getQuestsByCategory(category).stream()
                .filter(q -> !profile.hasCompletedQuest(q.id()))
                .toList();

        int slot = 0;
        for (Quest quest : quests) {
            if (slot >= 45) {
                break;
            }
            setItem(slot, buildQuestItem(quest, profile), event ->
                    new QuestDetailGUI(viewer, questManager, profileManager, quest).open(viewer));
            slot++;
        }

        setItem(49, backButton(), event -> new QuestCategoryGUI(viewer, questManager, profileManager).open(viewer));
    }

    private ItemStack buildQuestItem(Quest quest, PlayerProfile profile) {
        Material material = switch (quest.type()) {
            case HUNT -> Material.IRON_SWORD;
            case COLLECT -> Material.CHEST;
            case ESCORT -> Material.SADDLE;
            case REACH_LOCATION -> Material.COMPASS;
            case GLOBAL_EVENT -> Material.NETHER_STAR;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(quest.title(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));
        lore.add(Component.text("Required Rank: " + quest.requiredRank().name(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        boolean hasActive = profile.hasActiveQuest(quest.id());
        if (hasActive) {
            var progress = profile.getActiveQuests().get(quest.id());
            lore.add(Component.text("Progress: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else if (!questManager.canAccept(profile, quest)) {
            lore.add(Component.text("Rank too low to accept.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Click for details.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}