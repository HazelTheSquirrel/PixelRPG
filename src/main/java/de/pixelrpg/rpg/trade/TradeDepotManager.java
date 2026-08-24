package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.dialogue.BankStorageService;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.gui.TradeDepotGUI;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
    private final LanguageManager lang;
    private final ItemAPI itemAPI;
    private final File file;
    private final Map<UUID, TradeDepotListing> listings = new ConcurrentHashMap<>();

    public TradeDepotManager(JavaPlugin plugin, PlayerProfileManager profileManager, BankStorageService bankStorage,
                             DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.bankStorage = bankStorage;
        this.dialogueEngine = dialogueEngine;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
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
        expireListings();
        new TradeDepotGUI(player, this, profileManager).open(player);
    }

    public void openSellDialog(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty() || itemAPI == null || !itemAPI.isRPGItem(held)) {
            lang.send(player, "trade.invalid-item");
            return;
        }

        long now = System.currentTimeMillis();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Das Item aus deiner Haupthand wird als Handelsware eingestellt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Laufzeit: 7 Tage · Verkaufsgebühr: 5 %", NamedTextColor.GRAY))
        );
        DialogInput input = DialogInput.numberRange(
                "price", 260, Component.text("Verkaufspreis", NamedTextColor.WHITE),
                "%s Gold", 1.0f, Float.MAX_VALUE, 1.0f, 1.0f);
        dialogueEngine.openNumberRangeAction(player, Component.text("Handelsware einstellen", NamedTextColor.GOLD),
                body, input, Component.text("Einstellen"), NamedTextColor.GREEN,
                (target, response) -> createListingFromResponse(target, response, now));
    }

    private void createListingFromResponse(Player player, DialogResponseView response, long ignoredNow) {
        Float value = response.getFloat("price");
        if (value == null || !Float.isFinite(value) || value <= 0.0f) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty() || itemAPI == null || !itemAPI.isRPGItem(held)) {
            lang.send(player, "trade.invalid-item");
            return;
        }

        double price = value.doubleValue();
        ItemStack listed = held.clone();
        player.getInventory().setItemInMainHand(null);
        TradeDepotListing listing = new TradeDepotListing(UUID.randomUUID(), player.getUniqueId(), listed, price,
                System.currentTimeMillis() + LISTING_DURATION_MILLIS);
        listings.put(listing.id(), listing);
        save();
        lang.send(player, "trade.listed", "price", format(price));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        open(player);
    }

    public boolean purchase(Player buyer, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || listing.expired(System.currentTimeMillis())) {
            expireListings();
            lang.send(buyer, "trade.unavailable");
            return false;
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) {
            lang.send(buyer, "trade.own-listing");
            return false;
        }

        PlayerProfile buyerProfile = profileManager.getProfile(buyer.getUniqueId()).orElse(null);
        PlayerProfile sellerProfile = profileManager.getProfile(listing.sellerId()).orElse(null);
        if (buyerProfile == null || sellerProfile == null) {
            lang.send(buyer, "trade.unavailable");
            return false;
        }
        if (!canFit(buyer, listing.item())) {
            lang.send(buyer, "trade.inventory-full");
            return false;
        }
        if (!buyerProfile.removeMoney(listing.price())) {
            lang.send(buyer, "trade.insufficient-gold");
            return false;
        }

        listings.remove(listingId);
        double sellerAmount = listing.price() * (1.0D - SALE_FEE);
        sellerProfile.addMoney(sellerAmount);
        buyer.getInventory().addItem(listing.itemCopy());
        save();
        lang.send(buyer, "trade.purchased", "price", format(listing.price()));
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }

    public boolean cancel(Player seller, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || !listing.sellerId().equals(seller.getUniqueId())) return false;
        if (!canFit(seller, listing.item())) {
            lang.send(seller, "trade.inventory-full");
            return false;
        }
        listings.remove(listingId);
        seller.getInventory().addItem(listing.itemCopy());
        save();
        lang.send(seller, "trade.cancelled");
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

    private boolean canFit(Player player, ItemStack item) {
        var test = Bukkit.createInventory(null, player.getInventory().getSize());
        test.setContents(player.getInventory().getContents());
        return test.addItem(item.clone()).isEmpty();
    }

    private void load() {
        listings.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("listings");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                UUID seller = UUID.fromString(root.getString(key + ".seller"));
                double price = root.getDouble(key + ".price");
                long expires = root.getLong(key + ".expires");
                String encoded = root.getString(key + ".item");
                if (encoded == null) continue;
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
                if (!item.isEmpty()) listings.put(id, new TradeDepotListing(id, seller, item, price, expires));
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Ignoring invalid trade depot listing " + key, exception);
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
