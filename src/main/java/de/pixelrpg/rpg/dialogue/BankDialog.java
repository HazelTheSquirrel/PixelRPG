package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Native bank dialog with account management and a persistent personal bank compartment. */
public final class BankDialog {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final BankStorageService bankStorage;
    private final LanguageManager lang;
    private final long quickAmount;

    public BankDialog(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, BankStorageService bankStorage) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.bankStorage = bankStorage;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
        this.quickAmount = Math.max(1L, PixelRPGPlugin.getInstance().getConfig()
                .getLong("economy.bank.quick-amount", 10L));
    }

    public void open(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            dialogueEngine.openUnavailable(player, "Bank", "Dein Spielerprofil konnte nicht geladen werden.");
            return;
        }

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Kontostand: ", NamedTextColor.GRAY)
                        .append(Component.text(format(profile.getMoney()) + " Gold", NamedTextColor.GOLD))),
                DialogBody.plainMessage(Component.text(
                        "Dein persönliches Bankfach besitzt zwei Seiten und bleibt dauerhaft erhalten.",
                        NamedTextColor.DARK_GRAY))
        );

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text("Einzahlen: " + quickAmount + " Gold"), NamedTextColor.GREEN, target -> deposit(target, quickAmount)));
        actions.add(dialogueEngine.actionButton(Component.text("Alles einzahlen"), NamedTextColor.GREEN, this::depositAll));
        actions.add(dialogueEngine.actionButton(Component.text("Auszahlen: " + quickAmount + " Gold"), NamedTextColor.YELLOW, target -> withdraw(target, quickAmount)));
        actions.add(dialogueEngine.actionButton(Component.text("Alles auszahlen"), NamedTextColor.YELLOW, this::withdrawAll));
        actions.add(dialogueEngine.actionButton(Component.text("Bankfach öffnen"), NamedTextColor.AQUA, this::openBankCompartment));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogueEngine.openMultiAction(player, Component.text("Bank", NamedTextColor.GOLD), body, actions, 2);
    }

    private void deposit(Player player, long amount) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        double removed = removeCurrencyFromInventory(player, amount);
        if (removed <= 0.0) { lang.send(player, "bank.no-currency"); return; }
        profile.addMoney(removed);
        lang.send(player, "bank.deposit-success", "amount", format(removed));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        open(player);
    }

    private void depositAll(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (GuildCurrencyItemFactory.isCurrency(item)) total += GuildCurrencyItemFactory.readAmount(item) * item.getAmount();
        }
        if (total <= 0.0) { lang.send(player, "bank.no-currency"); return; }
        removeCurrencyFromInventory(player, total);
        profile.addMoney(total);
        lang.send(player, "bank.deposit-success", "amount", format(total));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        open(player);
    }

    private double removeCurrencyFromInventory(Player player, double maxAmount) {
        double removed = 0.0;
        for (int slot = 0; slot < player.getInventory().getSize() && removed < maxAmount; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!GuildCurrencyItemFactory.isCurrency(item)) continue;
            double perItemValue = GuildCurrencyItemFactory.readAmount(item);
            if (perItemValue <= 0.0) continue;
            int available = item.getAmount();
            int neededUnits = (int) Math.ceil((maxAmount - removed) / perItemValue);
            int takeUnits = Math.min(available, Math.max(neededUnits, 0));
            if (takeUnits <= 0) continue;
            removed += perItemValue * takeUnits;
            if (takeUnits >= available) player.getInventory().setItem(slot, null);
            else item.setAmount(available - takeUnits);
        }
        return removed;
    }

    private void withdraw(Player player, long amount) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (!profile.removeMoney(amount)) { lang.send(player, "bank.insufficient"); return; }
        giveCurrency(player, amount);
        open(player);
    }

    private void withdrawAll(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        long amount = (long) Math.floor(profile.getMoney());
        if (amount <= 0L) { lang.send(player, "bank.balance-empty"); return; }
        profile.removeMoney(amount);
        giveCurrency(player, amount);
        open(player);
    }

    private void giveCurrency(Player player, long amount) {
        List<ItemStack> stacks = GuildCurrencyItemFactory.createStacks(amount);
        for (ItemStack stack : stacks) {
            player.getInventory().addItem(stack).values().forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        lang.send(player, "bank.withdrew-stacks", "amount", String.valueOf(amount), "stacks", String.valueOf(stacks.size()));
    }

    private void openBankCompartment(Player player) {
        player.closeDialog();
        ItemStack[] contents = bankStorage.load(player.getUniqueId());
        BankInventoryHolder holder = new BankInventoryHolder(player.getUniqueId(), 0);
        var inventory = org.bukkit.Bukkit.createInventory(holder, BankStorageService.PAGE_SIZE,
                Component.text("Bankfach – Seite 1", NamedTextColor.GOLD));
        holder.inventory(inventory);
        for (int slot = 0; slot < BankStorageService.PAGE_SIZE; slot++) inventory.setItem(slot, contents[slot]);
        inventory.setItem(53, createNavigationHead("MHF_ArrowRight", "Weiter"));
        player.openInventory(inventory);
    }

    private ItemStack createNavigationHead(String profileName, String displayName) {
        ItemStack item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(org.bukkit.Bukkit.createProfile(profileName));
        meta.displayName(Component.text(displayName, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }

    private String format(double amount) {
        return String.format("%.2f", amount);
    }
}
