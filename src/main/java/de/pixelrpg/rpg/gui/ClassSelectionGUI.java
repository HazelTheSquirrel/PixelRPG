// src/main/java/de/pixelrpg/rpg/gui/ClassSelectionGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — 54 Slots + Back-Button)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.player.PlayerClass;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ClassSelectionGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;

    public ClassSelectionGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, Component.text("Choose your Class", NamedTextColor.DARK_PURPLE));
        this.viewer = viewer;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        setClassItem(19, Material.IRON_SWORD, PlayerClass.WARRIOR, profile,
                "Tank: high armor and health, protects the party.");
        setClassItem(21, Material.BOW, PlayerClass.RANGER, profile,
                "DPS: strong ranged damage and mobility.");
        setClassItem(23, Material.GOLDEN_APPLE, PlayerClass.HEALER, profile,
                "Support: powerful healing for the group.");
        setClassItem(25, Material.BLAZE_ROD, PlayerClass.MAGE, profile,
                "Glass Cannon: devastating spell damage, fragile.");
        setClassItem(31, Material.ENDER_PEARL, PlayerClass.ROGUE, profile,
                "Assassin: high crit chance and burst damage.");

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private void setClassItem(int slot, Material material, PlayerClass targetClass, PlayerProfile profile, String description) {
        boolean rankMet = profile.getRank().isAtLeast(Rank.C);
        boolean hasClass = profile.getPlayerClass() != PlayerClass.NONE;
        boolean isThisClass = profile.getPlayerClass() == targetClass;
        boolean respecRankMet = profile.getRank().isAtLeast(profileManager.getRespecMinRank());

        ItemStack item = new ItemStack(rankMet ? material : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(targetClass.displayName().decoration(TextDecoration.ITALIC, false));

        Component lore1 = Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
        Component lore2;
        if (isThisClass) {
            lore2 = Component.text("Currently practicing this class.", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false);
        } else if (!rankMet) {
            lore2 = Component.text("Requires Rank C or higher.", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false);
        } else if (hasClass) {
            lore2 = respecRankMet
                    ? Component.text("Respec cost: " + profileManager.getRespecCost() + " Gold", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
                    : Component.text("Respec requires Rank " + profileManager.getRespecMinRank().name() + ".", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false);
        } else {
            lore2 = Component.text("Click to permanently choose this class.", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false);
        }

        meta.lore(List.of(lore1, lore2));
        item.setItemMeta(meta);

        setItem(slot, item, event -> {
            if (!rankMet) {
                viewer.sendMessage(Component.text("Profession selection unlocks at Rank C.", NamedTextColor.RED));
                return;
            }

            if (!hasClass) {
                boolean success = profileManager.selectClass(viewer, targetClass);
                if (success) {
                    viewer.sendMessage(Component.text("You are now a ", NamedTextColor.GREEN)
                            .append(targetClass.displayName()).append(Component.text("!", NamedTextColor.GREEN)));
                    viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    viewer.closeInventory();
                } else {
                    viewer.sendMessage(Component.text("Could not select this class.", NamedTextColor.RED));
                }
                return;
            }

            if (isThisClass) {
                viewer.sendMessage(Component.text("You are already this class.", NamedTextColor.YELLOW));
                return;
            }

            PlayerProfileManager.RespecResult result = profileManager.respecClass(viewer, targetClass);
            switch (result) {
                case SUCCESS -> {
                    viewer.sendMessage(Component.text("You respecced into ", NamedTextColor.GREEN)
                            .append(targetClass.displayName()).append(Component.text("!", NamedTextColor.GREEN)));
                    viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    viewer.closeInventory();
                }
                case RANK_TOO_LOW -> viewer.sendMessage(Component.text(
                        "Respec requires Rank " + profileManager.getRespecMinRank().name() + ".", NamedTextColor.RED));
                case INSUFFICIENT_FUNDS -> viewer.sendMessage(Component.text(
                        "You need " + profileManager.getRespecCost() + " gold to respec.", NamedTextColor.RED));
                case SAME_CLASS -> viewer.sendMessage(Component.text("You are already this class.", NamedTextColor.YELLOW));
                case NOT_REGISTERED -> viewer.sendMessage(Component.text("You must be a guild member.", NamedTextColor.RED));
            }
        });
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}