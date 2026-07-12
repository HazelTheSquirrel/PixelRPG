// src/main/java/de/pixelrpg/rpg/gui/ShopGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopEntry;
import de.pixelrpg.rpg.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopGUI extends AbstractGUI {

    private final Player viewer;
    private final String npcId;
    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;

    public ShopGUI(Player viewer, String npcId, ShopManager shopManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Shop", NamedTextColor.DARK_GREEN));
        this.viewer = viewer;
        this.npcId = npcId;
        this.shopManager = shopManager;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        List<ShopEntry> entries = shopManager.getEntries(npcId);
        int slot = 0;

        for (ShopEntry entry : entries) {
            if (slot >= 54) {
                break;
            }

            ItemStack display = entry.item().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.text(" "));
            lore.add(Component.text("Price: " + entry.price() + " Gold", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to buy", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);

            setItem(slot, display, event -> {
                PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
                if (profile == null) {
                    return;
                }
                if (!profile.removeMoney(entry.price())) {
                    viewer.sendMessage(Component.text("You do not have enough gold.", NamedTextColor.RED));
                    return;
                }

                viewer.getInventory().addItem(entry.item().clone()).values()
                        .forEach(remainder -> viewer.getWorld().dropItemNaturally(viewer.getLocation(), remainder));
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
                viewer.sendMessage(Component.text("Purchased!", NamedTextColor.GREEN));
            });

            slot++;
        }
    }
}