package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
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
import java.util.List;

public final class QuestDetailGUI extends AbstractGUI {
    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final Quest quest;
    private final LanguageManager lang;

    public QuestDetailGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager, Quest quest) {
        super(54, QuestText.title(viewer, quest));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.quest = quest;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;

        Component questTitle = QuestText.title(viewer, quest).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);
        ItemStack info = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(questTitle);
        meta.itemName(questTitle);

        List<Component> lore = new ArrayList<>();
        lore.add(QuestText.description(viewer, quest).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(QuestText.objective(viewer, quest).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        if (quest.type() == QuestType.COLLECT) {
            lore.add(QuestText.requiredItem(viewer, quest).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        if (profile.hasActiveQuest(quest.id())) {
            var progress = profile.getActiveQuests().get(quest.id());
            lore.add(lang.get(viewer, "quest.progress", "current", String.valueOf(progress.getCurrentAmount()), "required", String.valueOf(quest.requiredAmount()))
                    .color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(lang.get(viewer, "quest.detail-hint").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        info.setItemMeta(meta);
        setItem(22, info);

        if (profile.hasActiveQuest(quest.id())) {
            ItemStack abandon = new ItemStack(Material.BARRIER);
            ItemMeta abandonMeta = abandon.getItemMeta();
            abandonMeta.displayName(lang.get(viewer, "quest.abandon").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            abandonMeta.lore(List.of(lang.get(viewer, "quest.abandon-desc").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            abandon.setItemMeta(abandonMeta);
            setItem(31, abandon, event -> {
                questManager.abandonQuest(viewer, quest.id());
                open(viewer);
            });
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(lang.get(viewer, "common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        setItem(49, back, event -> new QuestLogGUI(viewer, questManager, profileManager).open(viewer));
    }
}
