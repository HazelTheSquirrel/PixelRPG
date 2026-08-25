package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.dialogue.BankStorageService;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.TradeDepotGUI;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class TradeDepotManager {
    public static final long LISTING_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    public static final double SALE_FEE = 0.05D;

    private final JavaPlugin plugin;
    private final PlayerProfileManager profileManager;
    private final BankStorageService bankStorage;
    private final DialogueEngine dialogueEngine;
    private final ItemAPI itemAPI;
    private final File file;
    private final Map<UUID, TradeDepotListing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, Double> pendingPayouts = new ConcurrentHashMap<>();

    public TradeDepotManager(JavaPlugin plugin, PlayerProfileManager profileManager, BankStorageService bankStorage,
                             DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.bankStorage = bankStorage;
        this.dialogueEngine = dialogueEngine;
        this.itemAPI = Bukkit.getServicesManager().load(ItemAPI.class);
        this.file = new File(plugin.getDataFolder(), "trade-depot.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::expireListings, 20L * 60L, 20L * 60L);
    }

    public List<TradeDepotListing> listings() {
        expireListings();
        return listings.values().stream()
                .sorted(java.util.Comparator.comparingLong(TradeDepotListing::expiresAtMillis))
                .map(listing -> new TradeDepotListing(listing.id(), listing.sellerId(), listing.itemCopy(), listing.price(), listing.expiresAtMillis()))
                .toList();
    }

    public void open(Player player) {
        claimPendingPayout(player);
        expireListings();
        new TradeDepotGUI(player, this, profileManager).open(player);
    }

    public void claimPendingPayout(Player player) {
        UUID uuid = player.getUniqueId();
        Double amount = pendingPayouts.remove(uuid);
        if (amount == null || amount <= 0.0) return;
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null) {
            pendingPayouts.merge(uuid, amount, Double::sum);
            return;
        }
        profile.addMoney(amount);
        save();
        player.sendMessage(Component.text("Dir wurden " + format(amount) + " Gold aus Verkäufen gutgeschrieben.", NamedTextColor.GOLD));
    }

    public void openSellDialog(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isTradeableRpgItem(held)) {
            player.sendMessage(Component.text("Nur PixelRPG-Items können im Handelsdepot angeboten werden.", NamedTextColor.RED));
            return;
        }

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Das Item aus deiner Haupthand wird als Handelsware eingestellt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Laufzeit: 7 Tage · Verkaufsgebühr: 5 %", NamedTextColor.GRAY))
        );
        DialogInput input = DialogInput.text(
                "price", 260, Component.text("Verkaufspreis in Gold", NamedTextColor.WHITE),
                true, "", 16, null);
        dialogueEngine.openTextInputAction(player, Component.text("Handelsware einstellen", NamedTextColor.GOLD),
                body, input, Component.text("Einstellen"), NamedTextColor.GREEN,
                this::createListingFromResponse);
    }

    private void createListingFromResponse(Player player, DialogResponseView response) {
        String rawValue = response.getText("price");
        if (rawValue == null || rawValue.isBlank()) {
            player.sendMessage(Component.text("Bitte gib einen Verkaufspreis ein.", NamedTextColor.RED));
            return;
        }

        final double price;
        try {
            price = new BigDecimal(rawValue.trim().replace(',', '.')).doubleValue();
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("Der Verkaufspreis ist ungültig.", NamedTextColor.RED));
            return;
        }
        if (!Double.isFinite(price) || price <= 0.0D) {
            player.sendMessage(Component.text("Der Verkaufspreis muss größer als 0 Gold sein.", NamedTextColor.RED));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isTradeableRpgItem(held)) {
            player.sendMessage(Component.text("Nur PixelRPG-Items können im Handelsdepot angeboten werden.", NamedTextColor.RED));
            return;
        }

        ItemStack listed = held.clone();
        player.getInventory().setItemInMainHand(null);
        TradeDepotListing listing = new TradeDepotListing(UUID.randomUUID(), player.getUniqueId(), listed, price,
                System.currentTimeMillis() + LISTING_DURATION_MILLIS);
        listings.put(listing.id(), listing);
        save();
        player.sendMessage(Component.text("Handelsware für " + format(price) + " Gold eingestellt.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        open(player);
    }

    public boolean purchase(Player buyer, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || listing.expired(System.currentTimeMillis())) {
            expireListings();
            buyer.sendMessage(Component.text("Dieses Handelsangebot ist nicht mehr verfügbar.", NamedTextColor.RED));
            return false;
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) {
            buyer.sendMessage(Component.text("Du kannst dein eigenes Angebot nicht kaufen.", NamedTextColor.RED));
            return false;
        }

        PlayerProfile buyerProfile = profileManager.getProfile(buyer.getUniqueId()).orElse(null);
        if (buyerProfile == null) {
            buyer.sendMessage(Component.text("Dein Spielerprofil konnte nicht geladen werden.", NamedTextColor.RED));
            return false;
        }
        if (!canFit(buyer, listing.item())) {
            buyer.sendMessage(Component.text("Dein Inventar ist voll.", NamedTextColor.RED));
            return false;
        }
        if (!buyerProfile.removeMoney(listing.price())) {
            buyer.sendMessage(Component.text("Du hast nicht genug Gold.", NamedTextColor.RED));
            return false;
        }

        listings.remove(listingId);
        double sellerAmount = listing.price() * (1.0D - SALE_FEE);
        PlayerProfile sellerProfile = profileManager.getProfile(listing.sellerId()).orElse(null);
        if (sellerProfile != null) sellerProfile.addMoney(sellerAmount);
        else pendingPayouts.merge(listing.sellerId(), sellerAmount, Double::sum);

        buyer.getInventory().addItem(listing.itemCopy());
        save();
        buyer.sendMessage(Component.text("Gekauft für " + format(listing.price()) + " Gold.", NamedTextColor.GREEN));
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }

    public boolean cancel(Player seller, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || !listing.sellerId().equals(seller.getUniqueId())) return false;
        if (!canFit(seller, listing.item())) {
            seller.sendMessage(Component.text("Dein Inventar ist voll.", NamedTextColor.RED));
            return false;
        }
        listings.remove(listingId);
        seller.getInventory().addItem(listing.itemCopy());
        save();
        seller.sendMessage(Component.text("Handelsangebot zurückgenommen.", NamedTextColor.GREEN));
        return true;
    }

    public void shutdown() {
        save();
    }

    private void expireListings() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (TradeDepotListing listing : new ArrayList<>(listings.values())) {
            if (!listing.expired(now)) continue;
            if (!bankStorage.addTradeGoods(listing.sellerId(), listing.itemCopy())) continue;
            listings.remove(listing.id());
            changed = true;
        }
        if (changed) save();
    }

    private boolean isTradeableRpgItem(ItemStack item) {
        return item != null && !item.isEmpty() && itemAPI != null && itemAPI.isRPGItem(item);
    }

    private boolean canFit(Player player, ItemStack item) {
        var test = Bukkit.createInventory(null, 45);
        ItemStack[] source = player.getInventory().getStorageContents();
        for (int slot = 0; slot < source.length && slot < test.getSize(); slot++) {
            test.setItem(slot, source[slot] == null ? null : source[slot].clone());
        }
        return test.addItem(item.clone()).isEmpty();
    }

    private void load() {
        listings.clear();
        pendingPayouts.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("listings");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    UUID seller = UUID.fromString(root.getString(key + ".seller"));
                    double price = root.getDouble(key + ".price");
                    long expires = root.getLong(key + ".expires");
                    String encoded = root.getString(key + ".item");
                    if (encoded == null) continue;
                    ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
                    if (!item.isEmpty() && isTradeableRpgItem(item)) {
                        listings.put(id, new TradeDepotListing(id, seller, item, price, expires));
                    }
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Ignoring invalid trade depot listing " + key, exception);
                }
            }
        }
        var payouts = yaml.getConfigurationSection("pending-payouts");
        if (payouts != null) {
            for (String key : payouts.getKeys(false)) {
                try {
                    double amount = payouts.getDouble(key, 0.0);
                    if (amount > 0.0) pendingPayouts.put(UUID.fromString(key), amount);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid pending trade payout " + key);
                }
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (TradeDepotListing listing : listings.values()) {
            String path = "listings." + listing.id();
            yaml.set(path + ".seller", listing.sellerId().toString());
            yaml.set(path + ".price", listing.price());
            yaml.set(path + ".expires", listing.expiresAtMillis());
            yaml.set(path + ".item", Base64.getEncoder().encodeToString(listing.item().serializeAsBytes()));
        }
        for (Map.Entry<UUID, Double> payout : pendingPayouts.entrySet()) {
            yaml.set("pending-payouts." + payout.getKey(), payout.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save trade depot", exception);
        }
    }

    private String format(double amount) {
        return String.format("%.2f", amount);
    }
}
