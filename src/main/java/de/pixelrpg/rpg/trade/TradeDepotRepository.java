package de.pixelrpg.rpg.trade;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class TradeDepotRepository implements AutoCloseable {
    public record Snapshot(Map<UUID, TradeDepotListing> listings, Map<UUID, Double> pendingPayouts) {}

    private final Plugin plugin;
    private final Path file;
    private final ExecutorService io;
    private CompletableFuture<Void> writeChain = CompletableFuture.completedFuture(null);

    public TradeDepotRepository(Plugin plugin, Path file, ExecutorService io) {
        this.plugin = plugin;
        this.file = file;
        this.io = io;
    }

    public CompletableFuture<Snapshot> loadAsync() {
        return CompletableFuture.supplyAsync(this::loadSnapshot, io);
    }

    public synchronized CompletableFuture<Void> saveAsync(Map<UUID, TradeDepotListing> listings, Map<UUID, Double> payouts) {
        Map<UUID, TradeDepotListing> listingCopy = new LinkedHashMap<>();
        listings.forEach((id, listing) -> listingCopy.put(id, new TradeDepotListing(id, listing.sellerId(), listing.itemCopy(), listing.price(), listing.expiresAtMillis())));
        Map<UUID, Double> payoutCopy = Map.copyOf(payouts);
        writeChain = writeChain.handle((ignored, failure) -> null)
                .thenRunAsync(() -> writeSnapshot(listingCopy, payoutCopy), io);
        return writeChain;
    }

    private Snapshot loadSnapshot() {
        if (!Files.exists(file)) return new Snapshot(Map.of(), Map.of());
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            Map<UUID, TradeDepotListing> listings = new LinkedHashMap<>();
            ConfigurationSection root = yaml.getConfigurationSection("listings");
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
                        listings.put(id, new TradeDepotListing(id, seller, item, price, expires));
                    } catch (RuntimeException exception) {
                        plugin.getLogger().warning("Ignoring invalid trade depot listing " + key);
                    }
                }
            }
            Map<UUID, Double> payouts = new LinkedHashMap<>();
            ConfigurationSection payoutSection = yaml.getConfigurationSection("pending-payouts");
            if (payoutSection != null) {
                for (String key : payoutSection.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(key);
                        double amount = payoutSection.getDouble(key, 0.0D);
                        if (Double.isFinite(amount) && amount > 0.0D) payouts.put(id, amount);
                    } catch (IllegalArgumentException ignored) { }
                }
            }
            return new Snapshot(listings, payouts);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load trade-depot.yml.", exception);
            throw exception;
        }
    }

    private void writeSnapshot(Map<UUID, TradeDepotListing> listings, Map<UUID, Double> payouts) {
        YamlConfiguration yaml = new YamlConfiguration();
        listings.values().forEach(listing -> {
            String path = "listings." + listing.id();
            yaml.set(path + ".seller", listing.sellerId().toString());
            yaml.set(path + ".price", listing.price());
            yaml.set(path + ".expires", listing.expiresAtMillis());
            yaml.set(path + ".item", Base64.getEncoder().encodeToString(listing.item().serializeAsBytes()));
        });
        payouts.forEach((id, amount) -> yaml.set("pending-payouts." + id, amount));
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            yaml.save(temp.toFile());
            try {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save trade-depot.yml.", exception);
        }
    }

    @Override
    public synchronized void close() {
        try { writeChain.get(); }
        catch (Exception exception) { plugin.getLogger().log(Level.SEVERE, "Trade depot persistence did not flush cleanly.", exception); }
        io.shutdown();
    }
}
