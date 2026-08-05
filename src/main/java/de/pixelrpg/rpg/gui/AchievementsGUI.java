package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.achievement.AchievementDefinition;
import de.pixelrpg.rpg.achievement.AchievementRepository;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AchievementsGUI extends AbstractGUI {

    private final Player viewer;
    private final AchievementRepository repository;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public AchievementsGUI(Player viewer, AchievementRepository repository, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("achievement.gui-title"));
        this.viewer = viewer;
        this.repository = repository;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        int slot = 0;
        for (AchievementDefinition definition : repository.getAll()) {
            if (slot >= 45) {
                break;
            }

            boolean unlocked = profile.hasAchievement(definition.id());

            ItemStack item = new ItemStack(unlocked ? Material.EMERALD : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(definition.displayName(), unlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(definition.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(" "));
            if (definition.rewardTitle() != null) {
                lore.add(lang.get("achievement.reward-title", "title", definition.rewardTitle())
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (definition.rewardMoney() > 0) {
                lore.add(lang.get("achievement.reward-money", "money", String.valueOf(definition.rewardMoney()))
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (definition.rewardExp() > 0) {
                lore.add(lang.get("achievement.reward-exp", "exp", String.valueOf(definition.rewardExp()))
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(lang.get(unlocked ? "achievement.unlocked-status" : "achievement.locked")
                    .color(unlocked ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);

            setItem(slot, item);
            slot++;
        }

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}