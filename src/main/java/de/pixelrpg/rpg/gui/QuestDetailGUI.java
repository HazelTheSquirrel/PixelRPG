// src/main/java/de/pixelrpg/rpg/gui/QuestDetailGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — Back geht zur Rang-Kategorie zurück)
package de.pixelrpg.rpg.gui;

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

    public QuestDetailGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager, Quest quest) {
        super(54, Component.text(quest.title(), NamedTextColor.GOLD));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.quest = quest;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text(quest.title(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(List.of(
                Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" "),
                Component.text("Reward: " + quest.rewardMoney() + " Gold, " + quest.rewardExp() + " XP", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(infoMeta);
        setItem(22, info);

        boolean hasActive = profile.hasActiveQuest(quest.id());
        boolean canComplete = hasActive && profile.getActiveQuests().get(quest.id()).getCurrentAmount() >= quest.requiredAmount();

        if (canComplete) {
            setItem(20, buildButton(Material.LIME_DYE, "Hand In Quest", NamedTextColor.GREEN), event -> {
                questManager.completeQuest(viewer, quest.id());
                viewer.closeInventory();
            });
        } else if (hasActive) {
            setItem(20, buildButton(Material.ORANGE_DYE, "Abandon Quest", NamedTextColor.GOLD), event -> {
                questManager.abandonQuest(viewer, quest.id());
                viewer.closeInventory();
            });
        } else {
            setItem(20, buildButton(Material.LIME_DYE, "Accept Quest", NamedTextColor.GREEN), event -> {
                questManager.acceptQuest(viewer, quest);
                viewer.closeInventory();
            });
        }

        setItem(49, buildButton(Material.ARROW, "Back", NamedTextColor.RED), event ->
                new QuestBoardGUI(viewer, questManager, profileManager, quest.category()).open(viewer));
    }

    private ItemStack buildButton(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}