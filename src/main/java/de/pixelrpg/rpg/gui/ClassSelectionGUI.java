package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;

    public ClassSelectionGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("class.gui-title"));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        setClassItem(19, Material.IRON_SWORD, PlayerClass.WARRIOR, profile, "class.tank-desc");
        setClassItem(21, Material.BOW, PlayerClass.RANGER, profile, "class.dps-ranged-desc");
        setClassItem(23, Material.GOLDEN_APPLE, PlayerClass.HEALER, profile, "class.support-desc");
        setClassItem(25, Material.BLAZE_ROD, PlayerClass.MAGE, profile, "class.glass-cannon-desc");
        setClassItem(31, Material.ENDER_PEARL, PlayerClass.ROGUE, profile, "class.assassin-desc");

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private void setClassItem(int slot, Material material, PlayerClass targetClass, PlayerProfile profile, String descriptionKey) {
        boolean rankMet = profile.getRank().isAtLeast(Rank.C);
        boolean hasClass = profile.getPlayerClass() != PlayerClass.NONE;
        boolean isThisClass = profile.getPlayerClass() == targetClass;
        boolean respecRankMet = profile.getRank().isAtLeast(profileManager.getRespecMinRank());

        ItemStack item = new ItemStack(rankMet ? material : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(targetClass.displayName().decoration(TextDecoration.ITALIC, false));

        Component lore1 = lang.get(descriptionKey).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
        Component lore2;
        if (isThisClass) {
            lore2 = lang.get("class.currently-practicing").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
        } else if (!rankMet) {
            lore2 = lang.get("class.requires-rank-c").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
        } else if (hasClass) {
            lore2 = respecRankMet
                    ? lang.get("class.respec-cost-label", "cost", String.valueOf(profileManager.getRespecCost()))
                            .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                    : lang.get("class.respec-rank-low", "rank", profileManager.getRespecMinRank().name())
                            .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
        } else {
            lore2 = lang.get("class.click-to-choose").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);
        }

        meta.lore(List.of(lore1, lore2));
        item.setItemMeta(meta);

        setItem(slot, item, event -> {
            if (!rankMet) {
                lang.send(viewer, "class.unlock-requirement");
                return;
            }

            if (!hasClass) {
                boolean success = profileManager.selectClass(viewer, targetClass);
                if (success) {
                    viewer.sendMessage(lang.get("class.chosen", "class",
                            plainClassName(targetClass)));
                    viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    viewer.closeInventory();
                } else {
                    lang.send(viewer, "class.could-not-select");
                }
                return;
            }

            if (isThisClass) {
                lang.send(viewer, "class.same-class");
                return;
            }

            PlayerProfileManager.RespecResult result = profileManager.respecClass(viewer, targetClass);
            switch (result) {
                case SUCCESS -> {
                    viewer.sendMessage(lang.get("class.respec-success", "class", plainClassName(targetClass)));
                    viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    viewer.closeInventory();
                }
                case RANK_TOO_LOW -> lang.send(viewer, "class.respec-rank-low",
                        "rank", profileManager.getRespecMinRank().name());
                case INSUFFICIENT_FUNDS -> lang.send(viewer, "class.respec-insufficient",
                        "cost", String.valueOf(profileManager.getRespecCost()));
                case SAME_CLASS -> lang.send(viewer, "class.same-class");
                case NOT_REGISTERED -> lang.send(viewer, "common.not-registered");
            }
        });
    }

    private String plainClassName(PlayerClass playerClass) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(playerClass.displayName());
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}