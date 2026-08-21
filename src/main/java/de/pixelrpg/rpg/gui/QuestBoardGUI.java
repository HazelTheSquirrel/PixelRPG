package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final int categoryLevel;
    private final LanguageManager lang;

    public QuestBoardGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager, int categoryLevel) {
        super(54, Component.text("Quests – Level " + categoryLevel, NamedTextColor.GOLD));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.categoryLevel = categoryLevel;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;

        List<Quest> quests = questManager.getRepository().getQuestsByCategoryLevel(categoryLevel).stream()
                .filter(q -> !profile.hasCompletedQuest(q.id()))
                .toList();

        int slot = 0;
        for (Quest quest : quests) {
            if (slot >= 45) break;
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
        lore.add(Component.text("Benötigtes Level: " + quest.requiredLevel(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        boolean hasActive = profile.hasActiveQuest(quest.id());
        if (hasActive) {
            var progress = profile.getActiveQuests().get(quest.id());
            lore.add(lang.get("quest.progress", "current", String.valueOf(progress.getCurrentAmount()),
                            "required", String.valueOf(quest.requiredAmount()))
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else if (!questManager.canAccept(profile, quest)) {
            lore.add(Component.text("Level zu niedrig.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(lang.get("quest.click-for-details").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
