package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ReceptionDialog;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.quest.QuestText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class QuestLogGUI extends AbstractGUI {
    private final Player viewer;
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public QuestLogGUI(Player viewer, QuestManager questManager, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("quest.log-title"));
        this.viewer = viewer;
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (profile.getActiveQuests().isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(lang.get("quest.no-active").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            empty.setItemMeta(meta);
        
            setItem(22, empty);
        } else {
            int slot = 0;
            for (QuestProgress progress : profile.getActiveQuests().values()) {
                if (slot >= 45) break;
                Quest quest = questManager.getRepository().getQuest(progress.getQuestId());
                if (quest == null) continue;

                Component questTitle = QuestText.title(quest)
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false);
                ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(questTitle);
                meta.itemName(questTitle);

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(" "));
                lore.add(QuestText.objective(quest).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                if (quest.type() == de.pixelrpg.rpg.quest.QuestType.COLLECT) {
                    lore.add(QuestText.requiredItem(quest).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(lang.get("quest.progress", "current", String.valueOf(progress.getCurrentAmount()), "required", String.valueOf(quest.requiredAmount())).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(" "));
                lore.add(Component.text("Das Questziel ist hier jederzeit sichtbar.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                lore.add(progress.hasExpiry()
                        ? lang.get("quest.time-limited").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                        : lang.get("quest.no-time-limit").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
                setItem(slot, item, event -> new QuestDetailGUI(viewer, questManager, profileManager, quest).open(viewer));
                slot++;
            }
        }
        setItem(49, backButton(), event -> new ReceptionDialog(viewer, profileManager, new DialogueEngine()).open());
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
