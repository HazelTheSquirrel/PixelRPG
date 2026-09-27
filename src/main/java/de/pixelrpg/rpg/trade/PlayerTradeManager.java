package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.economy.Money;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles short-lived, synchronous player-to-player item-for-gold trades with escrowed items. */
public final class PlayerTradeManager implements Listener, AutoCloseable {
    private static final long OFFER_TIMEOUT_TICKS = 20L * 60L;
    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final ItemService itemService;
    private final Map<UUID, TradeOffer> offersByBuyer = new HashMap<>();
    private final Map<UUID, TradeOffer> offersBySeller = new HashMap<>();

    public PlayerTradeManager(JavaPlugin plugin, PlayerProfileManager profiles, ItemService itemService) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.itemService = itemService;
    }

    public synchronized boolean offer(Player seller, Player buyer, double price) {
        if (seller == null || buyer == null || seller.equals(buyer)) return false;
        if (!profiles.isRegistered(seller.getUniqueId()) || !profiles.isRegistered(buyer.getUniqueId())) return false;
        if (!Double.isFinite(price) || price <= 0.0D) return false;
        long priceMinor = Money.fromMajor(price);
        if (priceMinor <= 0L) return false;
        if (offersBySeller.containsKey(seller.getUniqueId()) || offersByBuyer.containsKey(buyer.getUniqueId())) return false;
        ItemStack held = seller.getInventory().getItemInMainHand();
        if (!itemService.isEconomySafeItem(held)) return false;
        ItemStack escrow = held.clone();
        seller.getInventory().setItemInMainHand(null);
        TradeOffer offer = new TradeOffer(seller.getUniqueId(), buyer.getUniqueId(), escrow, priceMinor, seller.getLocation().clone());
        offersBySeller.put(seller.getUniqueId(), offer);
        offersByBuyer.put(buyer.getUniqueId(), offer);
        offer.expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(offer), OFFER_TIMEOUT_TICKS);
        seller.sendMessage(Component.text("Handelsangebot an " + buyer.getName() + " gesendet: " + format(priceMinor) + " Gold.", NamedTextColor.GREEN));
        buyer.sendMessage(Component.text(seller.getName() + " bietet dir " + escrow.getAmount() + "x " + displayName(escrow) + " für " + format(priceMinor) + " Gold. Nutze /pixelrpg trade accept.", NamedTextColor.GOLD));
        return true;
    }

    public synchronized boolean accept(Player buyer) {
        if (buyer == null) return false;
        TradeOffer offer = offersByBuyer.get(buyer.getUniqueId());
        if (offer == null) return false;
        Player seller = Bukkit.getPlayer(offer.sellerId());
        if (seller == null) {
            buyer.sendMessage(Component.text("Der Verkäufer ist nicht online. Das Angebot bleibt bis zur Rückkehr bestehen.", NamedTextColor.YELLOW));
            return false;
        }
        if (!profiles.isRegistered(buyer.getUniqueId()) || !profiles.isRegistered(seller.getUniqueId())) return false;
        if (!canFit(buyer, offer.item())) {
            buyer.sendMessage(Component.text("Dein Inventar hat nicht genug Platz.", NamedTextColor.RED));
            return false;
        }
        if (!profiles.getProfile(buyer.getUniqueId()).map(profile -> profile.removeMoney(Money.toMajor(offer.priceMinor()))).orElse(false)) {
            buyer.sendMessage(Component.text("Du hast nicht genug Gold.", NamedTextColor.RED));
            return false;
        }
        profiles.getProfile(seller.getUniqueId()).ifPresent(profile -> profile.addMoney(Money.toMajor(offer.priceMinor())));
        profiles.saveProfileAsync(buyer.getUniqueId());
        profiles.saveProfileAsync(seller.getUniqueId());
        removeOffer(offer);
        buyer.getInventory().addItem(offer.item().clone());
        buyer.sendMessage(Component.text("Handel abgeschlossen.", NamedTextColor.GREEN));
        seller.sendMessage(Component.text("Dein Handel mit " + buyer.getName() + " wurde abgeschlossen: " + format(offer.priceMinor()) + " Gold erhalten.", NamedTextColor.GREEN));
        return true;
    }

    public synchronized boolean decline(Player buyer) {
        TradeOffer offer = offersByBuyer.get(buyer == null ? null : buyer.getUniqueId());
        if (offer == null) return false;
        removeOffer(offer);
        returnItem(offer);
        Player seller = Bukkit.getPlayer(offer.sellerId());
        if (seller != null) seller.sendMessage(Component.text("Dein Handelsangebot wurde abgelehnt.", NamedTextColor.YELLOW));
        if (buyer != null) buyer.sendMessage(Component.text("Handelsangebot abgelehnt.", NamedTextColor.YELLOW));
        return true;
    }

    public synchronized boolean cancel(Player seller) {
        TradeOffer offer = offersBySeller.get(seller == null ? null : seller.getUniqueId());
        if (offer == null) return false;
        removeOffer(offer);
        returnItem(offer);
        if (seller != null) seller.sendMessage(Component.text("Handelsangebot zurückgezogen.", NamedTextColor.YELLOW));
        Player buyer = Bukkit.getPlayer(offer.buyerId());
        if (buyer != null) buyer.sendMessage(Component.text("Das Handelsangebot wurde zurückgezogen.", NamedTextColor.YELLOW));
        return true;
    }

    // Bricht offene Handelsangebote sicher ab, wenn ein beteiligter Spieler den Server verlässt.
    @EventHandler
    public synchronized void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TradeOffer offer = offersBySeller.get(uuid);
        if (offer == null) offer = offersByBuyer.get(uuid);
        if (offer == null) return;
        removeOffer(offer);
        returnItem(offer);
        Player other = Bukkit.getPlayer(offer.sellerId().equals(uuid) ? offer.buyerId() : offer.sellerId());
        if (other != null) other.sendMessage(Component.text("Der Handel wurde abgebrochen, weil ein Spieler den Server verlassen hat.", NamedTextColor.YELLOW));
    }

    private synchronized void expire(TradeOffer offer) {
        if (!offersBySeller.containsKey(offer.sellerId())) return;
        removeOffer(offer);
        returnItem(offer);
        Player seller = Bukkit.getPlayer(offer.sellerId());
        Player buyer = Bukkit.getPlayer(offer.buyerId());
        if (seller != null) seller.sendMessage(Component.text("Das Handelsangebot ist abgelaufen und wurde zurückgegeben.", NamedTextColor.YELLOW));
        if (buyer != null) buyer.sendMessage(Component.text("Das Handelsangebot ist abgelaufen.", NamedTextColor.YELLOW));
    }

    private void removeOffer(TradeOffer offer) {
        offersBySeller.remove(offer.sellerId(), offer);
        offersByBuyer.remove(offer.buyerId(), offer);
        if (offer.expiryTask != null) offer.expiryTask.cancel();
    }

    private void returnItem(TradeOffer offer) {
        Player seller = Bukkit.getPlayer(offer.sellerId());
        if (seller != null) {
            Map<Integer, ItemStack> leftovers = seller.getInventory().addItem(offer.item().clone());
            leftovers.values().forEach(item -> seller.getWorld().dropItemNaturally(seller.getLocation(), item));
            return;
        }
        Location location = offer.returnLocation();
        if (location.getWorld() != null) location.getWorld().dropItemNaturally(location, offer.item().clone());
    }

    private boolean canFit(Player player, ItemStack item) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        org.bukkit.inventory.Inventory test = Bukkit.createInventory(null, contents.length);
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if (current != null && !current.isEmpty()) test.setItem(slot, current.clone());
        }
        return test.addItem(item.clone()).isEmpty();
    }

    private String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return item.getType().key().value();
    }

    private String format(long minorUnits) {
        return String.format(java.util.Locale.ROOT, "%.2f", Money.toMajor(minorUnits));
    }

    @Override
    public synchronized void close() {
        for (TradeOffer offer : java.util.List.copyOf(offersBySeller.values())) {
            removeOffer(offer);
            returnItem(offer);
        }
        offersBySeller.clear();
        offersByBuyer.clear();
    }

    private static final class TradeOffer {
        private final UUID sellerId;
        private final UUID buyerId;
        private final ItemStack item;
        private final long priceMinor;
        private final Location returnLocation;
        private BukkitTask expiryTask;
        private TradeOffer(UUID sellerId, UUID buyerId, ItemStack item, long priceMinor, Location returnLocation) {
            this.sellerId = sellerId; this.buyerId = buyerId; this.item = item; this.priceMinor = priceMinor; this.returnLocation = returnLocation;
        }
        private UUID sellerId() { return sellerId; }
        private UUID buyerId() { return buyerId; }
        private ItemStack item() { return item; }
        private long priceMinor() { return priceMinor; }
        private Location returnLocation() { return returnLocation; }
    }
}
