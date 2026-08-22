package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
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

    public BankDialog(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, BankStorageService bankStorage) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.bankStorage = bankStorage;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
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
                        NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text("Einzahlen"), NamedTextColor.GREEN, this::openDepositSelector));
        actions.add(dialogueEngine.actionButton(Component.text("Auszahlen"), NamedTextColor.YELLOW, this::openWithdrawSelector));
        actions.add(dialogueEngine.actionButton(Component.text("Bankfach öffnen"), NamedTextColor.AQUA, this::openBankCompartment));

        dialogueEngine.openMultiAction(player, Component.text("Bank", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openDepositSelector(Player player) {
        long maxAmount = getInventoryCurrencyAmount(player);
        if (maxAmount <= 0L) {
            lang.send(player, "bank.no-currency");
            return;
        }

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle den Betrag, den du einzahlen möchtest.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Verfügbar: " + maxAmount + " Gold", NamedTextColor.GOLD))
        );
        DialogInput input = DialogInput.numberRange(
                "amount", 260, Component.text("Betrag", NamedTextColor.WHITE),
                "%s Gold", 1.0f, (float) maxAmount, 1.0f, 1.0f);

        dialogueEngine.openNumberRangeAction(player, Component.text("Geld einzahlen", NamedTextColor.GOLD),
                body, input, Component.text("Einzahlen"), NamedTextColor.GREEN,
                (target, response) -> depositFromResponse(target, response));
    }

    private void openWithdrawSelector(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        long maxAmount = (long) Math.floor(profile.getMoney());
        if (maxAmount <= 0L) {
            lang.send(player, "bank.balance-empty");
            return;
        }

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle den Betrag, den du auszahlen möchtest.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Kontostand: " + format(profile.getMoney()) + " Gold", NamedTextColor.GOLD))
        );
        DialogInput input = DialogInput.numberRange(
                "amount", 260, Component.text("Betrag", NamedTextColor.WHITE),
                "%s Gold", 1.0f, (float) maxAmount, 1.0f, 1.0f);

        dialogueEngine.openNumberRangeAction(player, Component.text("Geld auszahlen", NamedTextColor.GOLD),
                body, input, Component.text("Auszahlen"), NamedTextColor.YELLOW,
                (target, response) -> withdrawFromResponse(target, response));
    }

    private void depositFromResponse(Player player, DialogResponseView response) {
        Float value = response.getFloat("amount");
        if (value == null || !Float.isFinite(value)) return;
        long amount = Math.max(1L, (long) value.floatValue());
        deposit(player, amount);
    }

    private void withdrawFromResponse(Player player, DialogResponseView response) {
        Float value = response.getFloat("amount");
        if (value == null || !Float.isFinite(value)) return;
        long amount = Math.max(1L, (long) value.floatValue());
        withdraw(player, amount);
    }

    private long getInventoryCurrencyAmount(Player player) {
        double total = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (GuildCurrencyItemFactory.isCurrency(item)) {
                total += GuildCurrencyItemFactory.readAmount(item) * item.getAmount();
            }
        }
        return Math.max(0L, (long) Math.floor(total));
    }

    private void deposit(Player player, long amount) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        double removed = removeCurrencyFromInventory(player, amount);
        if (removed <= 0.0) {
            lang.send(player, "bank.no-currency");
            return;
        }
        profile.addMoney(removed);
        lang.send(player, "bank.deposit-success", "amount", format(removed));
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
        if (!profile.removeMoney(amount)) {
            lang.send(player, "bank.insufficient");
            return;
        }
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
