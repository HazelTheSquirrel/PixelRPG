package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.bank.BankStorageService;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

/** Owns trade-depot listings, seven-day expiry, sale fees and seller payouts. */
public final class TradeDepotManager implements AutoCloseable {
    public static final long LISTING_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    public static final double SALE_FEE = 0.05D;

    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final BankStorageService bank;
    private final ItemService items;
    private final File file;
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, TradeDepotListing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, Double> pendingPayouts = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private volatile List<UUID> sortedIds = List.of();
    private volatile boolean closed;

    public TradeDepotManager(JavaPlugin plugin, PlayerProfileManager profiles, BankStorageService bank, ItemService items) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.bank = bank;
        this.items = items;
        this.file = new File(plugin.getDataFolder(), "trade-depot.yml");
        load();
    }

    public List<TradeDepotListing> listings() {
        List<UUID> ids = sortedIds;
        List<TradeDepotListing> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            TradeDepotListing listing = listings.get(id);
            if (listing != null) result.add(listing);
        }
        return List.copyOf(result);
    }

    public synchronized boolean createListing(Player seller, int inventorySlot, ItemStack expectedItem, double price) {
        if (closed || seller == null || expectedItem == null || !Double.isFinite(price) || price <= 0.0D) return false;
        if (!items.isEconomySafeItem(expectedItem)) return false;
        ItemStack[] storage = seller.getInventory().getStorageContents();
        if (inventorySlot < 0 || inventorySlot >= storage.length) return false;
        ItemStack current = storage[inventorySlot];
        if (current == null || !current.isSimilar(expectedItem) || current.getAmount() != expectedItem.getAmount()) return false;
        if (!bank.canFitTradeGoods(seller.getUniqueId(), current)) return false;

        storage[inventorySlot] = null;
        seller.getInventory().setStorageContents(storage);
        TradeDepotListing listing = new TradeDepotListing(UUID.randomUUID(), seller.getUniqueId(), current,
                price, System.currentTimeMillis() + LISTING_DURATION_MILLIS);
        listings.put(listing.id(), listing);
        scheduleExpiry(listing);
        invalidate();
        save();
        return true;
    }

    public synchronized boolean purchase(Player buyer, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || listing.expired(System.currentTimeMillis())) {
            if (listing != null) expire(listingId);
            return false;
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) return false;
        PlayerProfile buyerProfile = profiles.getProfile(buyer.getUniqueId()).orElse(null);
        if (buyerProfile == null || !bank.canFitTradeGoods(buyer.getUniqueId(), listing.item())) return false;
        if (!buyerProfile.removeMoney(listing.price())) return false;

        double sellerPayout = listing.price() * (1.0D - SALE_FEE);
        PlayerProfile sellerProfile = profiles.getProfile(listing.sellerId()).orElse(null);
        if (sellerProfile != null) sellerProfile.addMoney(sellerPayout);
        else pendingPayouts.merge(listing.sellerId(), sellerPayout, Double::sum);

        if (!bank.addTradeGoods(buyer.getUniqueId(), listing.item())) {
            buyerProfile.addMoney(listing.price());
            if (sellerProfile != null) sellerProfile.removeMoney(sellerPayout);
            else pendingPayouts.computeIfPresent(listing.sellerId(), (id, amount) -> amount <= sellerPayout ? null : amount - sellerPayout);
            return false;
        }

        listings.remove(listingId);
        cancelExpiry(listingId);
        invalidate();
        profiles.saveProfileAsync(buyer.getUniqueId());
        if (sellerProfile != null) profiles.saveProfileAsync(sellerProfile.uniqueId());
        save();
        return true;
    }

    public synchronized boolean cancel(Player seller, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || !listing.sellerId().equals(seller.getUniqueId())) return false;
        if (!bank.addTradeGoods(seller.getUniqueId(), listing.item())) return false;
        listings.remove(listingId);
        cancelExpiry(listingId);
        invalidate();
        save();
        return true;
    }

    public synchronized double claimPendingPayout(Player player) {
        double amount = pendingPayouts.getOrDefault(player.getUniqueId(), 0.0D);
        if (amount <= 0.0D) return 0.0D;
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return 0.0D;
        pendingPayouts.remove(player.getUniqueId());
        profile.addMoney(amount);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
        player.sendMessage(Component.text("Dir wurden " + String.format(java.util.Locale.ROOT, "%.2f", amount)
                + " Gold aus Verkäufen gutgeschrieben.", NamedTextColor.GOLD));
        return amount;
    }

    public void shutdown() {
        if (closed) return;
        closed = true;
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        save();
        ioExecutor.close();
    }

    @Override public void close() { shutdown(); }

    private void scheduleExpiry(TradeDepotListing listing) {
        cancelExpiry(listing.id());
        long remaining = listing.expiresAtMillis() - System.currentTimeMillis();
        if (remaining <= 0L) { expire(listing.id()); return; }
        long ticks = Math.max(1L, (remaining + 49L) / 50L);
        expiryTasks.put(listing.id(), Bukkit.getScheduler().runTaskLater(plugin, () -> expire(listing.id()), ticks));
    }

    private synchronized void expire(UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null) { cancelExpiry(listingId); return; }
        if (!listing.expired(System.currentTimeMillis())) { scheduleExpiry(listing); return; }
        if (!bank.addTradeGoods(listing.sellerId(), listing.item())) {
            expiryTasks.put(listingId, Bukkit.getScheduler().runTaskLater(plugin, () -> expire(listingId), 200L));
            return;
        }
        listings.remove(listingId);
        cancelExpiry(listingId);
        invalidate();
        save();
    }

    private void cancelExpiry(UUID id) {
        BukkitTask task = expiryTasks.remove(id);
        if (task != null) task.cancel();
    }

    private void invalidate() {
        List<UUID> ids = new ArrayList<>(listings.keySet());
        ids.sort(Comparator.comparingLong(id -> listings.get(id).expiresAtMillis()));
        sortedIds = List.copyOf(ids);
    }

    private void load() {
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
                    if (!items.isEconomySafeItem(item)) continue;
                    TradeDepotListing listing = new TradeDepotListing(id, seller, item, price, expires);
                    listings.put(id, listing);
                    scheduleExpiry(listing);
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Ignoring invalid trade depot listing " + key, exception);
                }
            }
        }
        var payouts = yaml.getConfigurationSection("pending-payouts");
        if (payouts != null) for (String key : payouts.getKeys(false)) {
            try {
                double amount = payouts.getDouble(key, 0.0D);
                if (amount > 0.0D) pendingPayouts.put(UUID.fromString(key), amount);
            } catch (IllegalArgumentException ignored) { }
        }
        invalidate();
    }

    private void save() {
        if (closed) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (TradeDepotListing listing : listings.values()) {
            String path = "listings." + listing.id();
            yaml.set(path + ".seller", listing.sellerId().toString());
            yaml.set(path + ".price", listing.price());
            yaml.set(path + ".expires", listing.expiresAtMillis());
            yaml.set(path + ".item", Base64.getEncoder().encodeToString(listing.item().serializeAsBytes()));
        }
        for (Map.Entry<UUID, Double> entry : pendingPayouts.entrySet()) {
            yaml.set("pending-payouts." + entry.getKey(), entry.getValue());
        }
        String content = yaml.saveToString();
        ioExecutor.execute(() -> {
            try { Files.writeString(file.toPath(), content); }
            catch (java.io.IOException exception) { plugin.getLogger().log(Level.SEVERE, "Could not save trade depot.", exception); }
        });
    }
}
