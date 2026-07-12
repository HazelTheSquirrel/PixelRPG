// src/main/java/de/pixelrpg/rpg/gui/BankGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — 54 Slots, Back-Button, Vollauszahlung als saubere Stacks)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
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

import java.util.ArrayList;
import java.util.List;

public final class BankGUI extends AbstractGUI {

    private static final double QUICK_AMOUNT = 10.0;

    private final Player viewer;
    private final PlayerProfileManager profileManager;

    public BankGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, Component.text("Guild Bank", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        setItem(13, buildBalanceItem(profile));

        setItem(20, buildActionItem(Material.LIME_DYE, "Deposit Gold", NamedTextColor.GREEN,
                        "Left-click: deposit " + (long) QUICK_AMOUNT + " Gold",
                        "Right-click: deposit all carried Gold"),
                event -> {
                    if (event.getClick().isRightClick()) {
                        depositAll(profile);
                    } else {
                        depositAmount(profile, QUICK_AMOUNT);
                    }
                });

        setItem(24, buildActionItem(Material.RED_DYE, "Withdraw Gold", NamedTextColor.RED,
                        "Left-click: withdraw " + (long) QUICK_AMOUNT + " Gold",
                        "Right-click: withdraw entire balance"),
                event -> {
                    if (event.getClick().isRightClick()) {
                        withdrawAll(profile);
                    } else {
                        withdrawAmount(profile, QUICK_AMOUNT);
                    }
                });

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private void depositAmount(PlayerProfile profile, double amount) {
        double removed = removeCurrencyFromInventory(amount);
        if (removed <= 0.0) {
            viewer.sendMessage(Component.text("You are not carrying any Guild Gold.", NamedTextColor.RED));
            return;
        }
        profile.addMoney(removed);
        viewer.sendMessage(Component.text("Deposited " + String.format("%.2f", removed) + " gold.", NamedTextColor.GREEN));
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        open(viewer);
    }

    private void depositAll(PlayerProfile profile) {
        double total = 0.0;
        for (ItemStack item : viewer.getInventory().getContents()) {
            if (GuildCurrencyItemFactory.isCurrency(item)) {
                total += GuildCurrencyItemFactory.readAmount(item) * item.getAmount();
            }
        }
        if (total <= 0.0) {
            viewer.sendMessage(Component.text("You are not carrying any Guild Gold.", NamedTextColor.RED));
            return;
        }
        removeCurrencyFromInventory(total);
        profile.addMoney(total);
        viewer.sendMessage(Component.text("Deposited " + String.format("%.2f", total) + " gold.", NamedTextColor.GREEN));
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        open(viewer);
    }

    private double removeCurrencyFromInventory(double maxAmount) {
        double removed = 0.0;
        for (int i = 0; i < viewer.getInventory().getSize() && removed < maxAmount; i++) {
            ItemStack item = viewer.getInventory().getItem(i);
            if (!GuildCurrencyItemFactory.isCurrency(item)) {
                continue;
            }
            double perItemValue = GuildCurrencyItemFactory.readAmount(item);
            int available = item.getAmount();
            int neededUnits = (int) Math.ceil((maxAmount - removed) / perItemValue);
            int takeUnits = Math.min(available, Math.max(neededUnits, 0));
            if (takeUnits <= 0) {
                continue;
            }
            removed += perItemValue * takeUnits;
            if (takeUnits >= available) {
                viewer.getInventory().setItem(i, null);
            } else {
                item.setAmount(available - takeUnits);
            }
        }
        return removed;
    }

    private void withdrawAmount(PlayerProfile profile, double amount) {
        if (!profile.removeMoney(amount)) {
            viewer.sendMessage(Component.text("Insufficient funds.", NamedTextColor.RED));
            return;
        }
        giveCurrency((long) amount);
        open(viewer);
    }

    private void withdrawAll(PlayerProfile profile) {
        long wholeAmount = (long) Math.floor(profile.getMoney());
        if (wholeAmount <= 0L) {
            viewer.sendMessage(Component.text("Your balance is empty.", NamedTextColor.RED));
            return;
        }
        profile.removeMoney(wholeAmount);
        giveCurrency(wholeAmount);
        open(viewer);
    }

    private void giveCurrency(long amount) {
        List<ItemStack> stacks = GuildCurrencyItemFactory.createStacks(amount);
        for (ItemStack stack : stacks) {
            viewer.getInventory().addItem(stack).values()
                    .forEach(remainder -> viewer.getWorld().dropItemNaturally(viewer.getLocation(), remainder));
        }
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        viewer.sendMessage(Component.text("Withdrew " + amount + " gold as " + stacks.size() + " stack(s).", NamedTextColor.GREEN));
    }

    private ItemStack buildBalanceItem(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Guild Account Balance", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(String.format("%.2f Gold", profile.getMoney()), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionItem(Material material, String name, NamedTextColor color, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}