package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
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

    private static final int[] CATEGORY_LEVELS = {1, 11, 21, 31, 41, 51, 61};
    private static final int[] SLOTS = {19, 21, 23, 25, 29, 31, 33};

    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public QuestCategoryGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("quest.board-title"));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        for (int i = 0; i < CATEGORY_LEVELS.length; i++) {
            int level = CATEGORY_LEVELS[i];
            int questCount = questManager.getRepository().getQuestsByCategoryLevel(level).size();

            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Level " + level, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    lang.get("quest.quest-count", "count", String.valueOf(questCount))
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    lang.get("quest.click-to-browse").color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);

            setItem(SLOTS[i], item, event ->
                    new QuestBoardGUI(viewer, questManager, profileManager, level).open(viewer));
        }
    }
}
