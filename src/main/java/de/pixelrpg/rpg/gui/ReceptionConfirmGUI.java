// src/main/java/de/pixelrpg/rpg/gui/ReceptionConfirmGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ReceptionConfirmGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;

    public ReceptionConfirmGUI(Player viewer, PlayerProfileManager profileManager) {
        super(9, Component.text("Confirm Resignation?", NamedTextColor.DARK_RED));
        this.viewer = viewer;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        setItem(2, buildButton(Material.RED_DYE, "Yes, resign irrevocably", NamedTextColor.RED), event -> {
            profileManager.leaveGuild(viewer);
            viewer.sendMessage(Component.text("You left the Guild. Your progress has been wiped.", NamedTextColor.RED));
            viewer.closeInventory();
        });

        setItem(6, buildButton(Material.GREEN_DYE, "No, cancel", NamedTextColor.GREEN), event ->
                new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack buildButton(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}