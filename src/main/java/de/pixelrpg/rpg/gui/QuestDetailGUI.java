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

import java.util.List;

public final class QuestDetailGUI extends AbstractGUI {

    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final Quest quest;
    private final LanguageManager lang;

    public QuestDetailGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager, Quest quest) {
        super(54, Component.text(quest.title(), NamedTextColor.GOLD));
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

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text(quest.title(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(List.of(
                Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" "),
                Component.text("Benötigtes Level: " + quest.requiredLevel(), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false),
                lang.get("quest.reward-label", "money", String.valueOf(quest.rewardMoney()), "exp", String.valueOf(quest.rewardExp()))
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(infoMeta);
        setItem(22, info);

        boolean hasActive = profile.hasActiveQuest(quest.id());
        boolean canComplete = hasActive && profile.getActiveQuests().get(quest.id()).getCurrentAmount() >= quest.requiredAmount();

        if (canComplete) {
            setItem(20, buildButton(Material.LIME_DYE, "quest.hand-in", NamedTextColor.GREEN), event -> {
                questManager.completeQuest(viewer, quest.id());
                viewer.closeInventory();
            });
        } else if (hasActive) {
            setItem(20, buildButton(Material.ORANGE_DYE, "quest.abandon", NamedTextColor.GOLD), event -> {
                questManager.abandonQuest(viewer, quest.id());
                viewer.closeInventory();
            });
        } else {
            setItem(20, buildButton(Material.LIME_DYE, "quest.accept", NamedTextColor.GREEN), event -> {
                boolean accepted = questManager.acceptQuest(viewer, quest);
                if (accepted) viewer.closeInventory();
                else open(viewer);
            });
        }

        setItem(49, buildButton(Material.ARROW, "common.back", NamedTextColor.RED), event -> viewer.closeInventory());
    }

    private ItemStack buildButton(Material material, String labelKey, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(labelKey).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
