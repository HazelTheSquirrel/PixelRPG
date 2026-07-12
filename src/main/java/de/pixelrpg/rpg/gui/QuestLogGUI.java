// src/main/java/de/pixelrpg/rpg/gui/QuestLogGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — 54 Slots, Back-Button slot 49)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class QuestLogGUI extends AbstractGUI {

    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestLogGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Quest Log", NamedTextColor.DARK_AQUA));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        if (profile.getActiveQuests().isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("No active quests", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            empty.setItemMeta(meta);
            setItem(22, empty);
        } else {
            int slot = 0;
            for (QuestProgress progress : profile.getActiveQuests().values()) {
                if (slot >= 45) {
                    break;
                }
                Quest quest = questManager.getRepository().getQuest(progress.getQuestId());
                if (quest == null) {
                    continue;
                }

                ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text(quest.title(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Progress: " + progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false),
                        progress.hasExpiry()
                                ? Component.text("Time-limited quest.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                                : Component.text("No time limit.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);

                setItem(slot, item, event ->
                        new QuestDetailGUI(viewer, questManager, profileManager, quest).open(viewer));
                slot++;
            }
        }

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}