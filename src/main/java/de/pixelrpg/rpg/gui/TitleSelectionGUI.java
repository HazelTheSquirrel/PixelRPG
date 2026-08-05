package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
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

import java.util.List;

public final class TitleSelectionGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public TitleSelectionGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("title-select.gui-title"));
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

        setItem(0, buildOption("title-select.no-title", profile.getSelectedTitle() == null), event -> {
            profileManager.selectTitle(viewer.getUniqueId(), null);
            PixelRPGPlugin.getInstance().getTitleDisplayService().apply(viewer);
            open(viewer);
        });

        int slot = 1;
        for (String title : profile.getUnlockedTitles()) {
            if (slot >= 45) {
                break;
            }
            boolean selected = title.equals(profile.getSelectedTitle());
            setItem(slot, buildOptionRaw(title, selected), event -> {
                profileManager.selectTitle(viewer.getUniqueId(), title);
                PixelRPGPlugin.getInstance().getTitleDisplayService().apply(viewer);
                open(viewer);
            });
            slot++;
        }

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack buildOption(String labelKey, boolean selected) {
        ItemStack item = new ItemStack(selected ? Material.NAME_TAG : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(labelKey)
                .color(selected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        if (selected) {
            meta.lore(List.of(lang.get("title-select.currently-selected")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        item.setItemMeta(meta);
        return item;
    }

    // Für dynamisch freigeschaltete Titel (aus Achievements), die keinen
    // Sprachkey haben, sondern als Klartext im Profil gespeichert sind.
    private ItemStack buildOptionRaw(String label, boolean selected) {
        ItemStack item = new ItemStack(selected ? Material.NAME_TAG : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, selected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        if (selected) {
            meta.lore(List.of(lang.get("title-select.currently-selected")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        }
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