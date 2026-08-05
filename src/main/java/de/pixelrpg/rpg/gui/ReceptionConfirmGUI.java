package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ReceptionConfirmGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public ReceptionConfirmGUI(Player viewer, PlayerProfileManager profileManager) {
        super(9, PixelRPGPlugin.getInstance().getLanguageManager().get("reception.resign-title"));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        setItem(2, buildButton(Material.RED_DYE, "reception.yes-resign", NamedTextColor.RED), event -> {
            profileManager.leaveGuild(viewer);
            lang.send(viewer, "reception.left-guild");
            viewer.closeInventory();
        });

        setItem(6, buildButton(Material.GREEN_DYE, "reception.no-cancel", NamedTextColor.GREEN), event ->
                new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack buildButton(Material material, String labelKey, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(labelKey).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}