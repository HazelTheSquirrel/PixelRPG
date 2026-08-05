package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.lang.LanguageManager;
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

    private final Player viewer;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;
    private final double quickAmount;

    public BankGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("bank.gui-title"));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
        this.quickAmount = PixelRPGPlugin.getInstance().getConfig().getDouble("economy.bank.quick-amount", 10.0);
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        setItem(13, buildBalanceItem(profile));

        setItem(20, buildActionItem(Material.LIME_DYE, "bank.deposit-button", NamedTextColor.GREEN,
                        lang.get("bank.deposit-left", "amount", String.valueOf((long) quickAmount)),
                        lang.get("bank.deposit-right")),
                event -> {
                    if (event.getClick().isRightClick()) {
                        depositAll(profile);
                    } else {
                        depositAmount(profile, quickAmount);
                    }
                });

        setItem(24, buildActionItem(Material.RED_DYE, "bank.withdraw-button", NamedTextColor.RED,
                        lang.get("bank.withdraw-left", "amount", String.valueOf((long) quickAmount)),
                        lang.get("bank.withdraw-right")),
                event -> {
                    if (event.getClick().isRightClick()) {
                        withdrawAll(profile);
                    } else {
                        withdrawAmount(profile, quickAmount);
                    }
                });

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private void depositAmount(PlayerProfile profile, double amount) {
        double removed = removeCurrencyFromInventory(amount);
        if (removed <= 0.0) {
            lang.send(viewer, "bank.no-currency");
            return;
        }
        profile.addMoney(removed);
        lang.send(viewer, "bank.deposit-success", "amount", String.format("%.2f", removed));
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
            lang.send(viewer, "bank.no-currency");
            return;
        }
        removeCurrencyFromInventory(total);
        profile.addMoney(total);
        lang.send(viewer, "bank.deposit-success", "amount", String.format("%.2f", total));
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
            lang.send(viewer, "bank.insufficient");
            return;
        }
        giveCurrency((long) amount);
        open(viewer);
    }

    private void withdrawAll(PlayerProfile profile) {
        long wholeAmount = (long) Math.floor(profile.getMoney());
        if (wholeAmount <= 0L) {
            lang.send(viewer, "bank.balance-empty");
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
        lang.send(viewer, "bank.withdrew-stacks", "amount", String.valueOf(amount), "stacks", String.valueOf(stacks.size()));
    }

    private ItemStack buildBalanceItem(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("bank.balance-title").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lang.get("bank.balance-value", "amount", String.format("%.2f", profile.getMoney()))
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionItem(Material material, String nameKey, NamedTextColor color, Component... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(nameKey).color(color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (Component line : loreLines) {
            lore.add(line.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
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