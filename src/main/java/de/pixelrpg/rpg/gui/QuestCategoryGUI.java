// src/main/java/de/pixelrpg/rpg/gui/QuestCategoryGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class QuestCategoryGUI extends AbstractGUI {

    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestCategoryGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Quest Board", NamedTextColor.DARK_AQUA));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        int[] slots = {19, 21, 23, 25, 29, 31, 33};
        Rank[] ranks = Rank.values();

        for (int i = 0; i < ranks.length && i < slots.length; i++) {
            Rank rank = ranks[i];
            int questCount = questManager.getRepository().getQuestsByCategory(rank).size();

            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Rank " + rank.name(), rank.getColor())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(questCount + " quest(s) available.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Click to browse.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);

            setItem(slots[i], item, event ->
                    new QuestBoardGUI(viewer, questManager, profileManager, rank).open(viewer));
        }
    }
}