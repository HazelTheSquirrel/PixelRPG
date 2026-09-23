package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TradeDepotService implements AutoCloseable {
    public static final long LISTING_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    public static final double SALE_FEE = 0.05D;
    private final Plugin plugin;
    private final PlayerProfileManager profiles;
    private final ItemService items;
    private final TradeDepotRepository repository;
    private final TradeGoodsRepository goodsRepository;
    private final Map<UUID, TradeDepotListing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, Double> pendingPayouts = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();

    public TradeDepotService(Plugin plugin, PlayerProfileManager profiles, ItemService items,
                             TradeDepotRepository repository, TradeGoodsRepository goodsRepository) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.items = items;
        this.repository = repository;
        this.goodsRepository = goodsRepository;
    }

    public void replace(TradeDepotRepository.Snapshot snapshot) {
        listings.clear();
        listings.putAll(snapshot.listings());
        pendingPayouts.clear();
        pendingPayouts.putAll(snapshot.pendingPayouts());
        listings.values().forEach(this::scheduleExpiry);
    }

    public List<TradeDepotListing> listings() {
        return listings.values().stream()
                .sorted(Comparator.comparingLong(TradeDepotListing::expiresAtMillis))
                .map(listing -> new TradeDepotListing(listing.id(), listing.sellerId(), listing.itemCopy(), listing.price(), listing.expiresAtMillis()))
                .toList();
    }

    public boolean purchase(Player buyer, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || listing.expired(System.currentTimeMillis())) {
            expire(listingId);
            return false;
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) return false;
        PlayerProfile profile = profiles.getProfile(buyer.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;
        if (!canStore(goodsRepository.get(buyer.getUniqueId()), listing.item())) return false;
        if (!profile.removeMoney(listing.price())) return false;

        listings.remove(listingId);
        cancelExpiry(listingId);
        double payout = listing.price() * (1.0D - SALE_FEE);
        PlayerProfile seller = profiles.getProfile(listing.sellerId()).orElse(null);
        if (seller != null) {
            seller.addMoney(payout);
            profiles.saveProfileAsync(seller.getUuid());
        } else {
            pendingPayouts.merge(listing.sellerId(), payout, Double::sum);
        }
        addGoods(buyer.getUniqueId(), listing.itemCopy());
        profiles.saveProfileAsync(buyer.getUniqueId());
        save();
        return true;
    }

    public boolean cancel(Player seller, UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null || !listing.sellerId().equals(seller.getUniqueId())) return false;
        if (!canStore(goodsRepository.get(seller.getUniqueId()), listing.item())) return false;
        listings.remove(listingId);
        cancelExpiry(listingId);
        addGoods(seller.getUniqueId(), listing.itemCopy());
        save();
        return true;
    }

    public boolean createListing(Player seller, int inventorySlot, double price) {
        if (!Double.isFinite(price) || price <= 0.0D || inventorySlot < 0 || inventorySlot >= 36) return false;
        ItemStack current = seller.getInventory().getItem(inventorySlot);
        if (!items.isEconomySafeItem(current)) return false;
        PlayerProfile profile = profiles.getProfile(seller.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;
        ItemStack listed = current.clone();
        seller.getInventory().setItem(inventorySlot, null);
        TradeDepotListing listing = new TradeDepotListing(UUID.randomUUID(), seller.getUniqueId(), listed, price,
                System.currentTimeMillis() + LISTING_DURATION_MILLIS);
        listings.put(listing.id(), listing);
        scheduleExpiry(listing);
        save();
        return true;
    }

    public double claimPayout(Player player) {
        Double amount = pendingPayouts.remove(player.getUniqueId());
        if (amount == null || amount <= 0.0D) return 0.0D;
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            pendingPayouts.merge(player.getUniqueId(), amount, Double::sum);
            return 0.0D;
        }
        profile.addMoney(amount);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
        return amount;
    }

    public int claimTradeGoods(Player player) {
        ItemStack[] contents = goodsRepository.get(player.getUniqueId());
        int moved = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty()) continue;
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            if (leftovers.isEmpty()) {
                contents[slot] = null;
                moved++;
            } else {
                ItemStack remaining = leftovers.get(0);
                contents[slot] = remaining;
            }
        }
        goodsRepository.set(player.getUniqueId(), contents);
        return moved;
    }

    public ItemStack[] tradeGoods(Player player) {
        return goodsRepository.get(player.getUniqueId());
    }

    private void addGoods(UUID playerId, ItemStack item) {
        ItemStack[] contents = goodsRepository.get(playerId);
        int remaining = item.getAmount();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack current = contents[slot];
            if (current == null || current.isEmpty()) {
                ItemStack placed = item.clone();
                placed.setAmount(remaining);
                contents[slot] = placed;
                remaining = 0;
                break;
            }
            if (!current.isSimilar(item) || current.getAmount() >= current.getMaxStackSize()) continue;
            int moved = Math.min(remaining, current.getMaxStackSize() - current.getAmount());
            current.setAmount(current.getAmount() + moved);
            remaining -= moved;
        }
        if (remaining > 0) throw new IllegalStateException("Trade goods storage is full");
        goodsRepository.set(playerId, contents);
    }

    private boolean canStore(ItemStack[] contents, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack current : contents) {
            if (current == null || current.isEmpty()) return true;
            if (current.isSimilar(item)) remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }

    private void scheduleExpiry(TradeDepotListing listing) {
        cancelExpiry(listing.id());
        long delay = Math.max(1L, (listing.expiresAtMillis() - System.currentTimeMillis() + 49L) / 50L);
        expiryTasks.put(listing.id(), plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(listing.id()), delay));
    }

    private void expire(UUID listingId) {
        TradeDepotListing listing = listings.get(listingId);
        if (listing == null) return;
        if (!listing.expired(System.currentTimeMillis())) {
            scheduleExpiry(listing);
            return;
        }
        try {
            addGoods(listing.sellerId(), listing.itemCopy());
        } catch (IllegalStateException exception) {
            scheduleExpiry(listing);
            return;
        }
        listings.remove(listingId);
        cancelExpiry(listingId);
        save();
    }

    private void cancelExpiry(UUID listingId) {
        BukkitTask task = expiryTasks.remove(listingId);
        if (task != null) task.cancel();
    }

    private void save() {
        repository.saveAsync(listings, pendingPayouts);
    }

    @Override
    public void close() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        repository.close();
        goodsRepository.close();
    }
}
