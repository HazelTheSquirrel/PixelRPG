package de.pixelrpg.rpg.shop;

import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService implements AutoCloseable {
    private final PlayerProfileManager profiles;
    private final ItemService items;
    private final ShopRepository repository;
    private final Map<String, List<ShopEntry>> shops = new ConcurrentHashMap<>();

    public ShopService(Plugin plugin, PlayerProfileManager profiles, ItemService items, ShopRepository repository) {
        this.profiles = profiles;
        this.items = items;
        this.repository = repository;
    }

    public void replace(Map<String, List<ShopEntry>> definitions) {
        shops.clear();
        definitions.forEach((id, entries) -> shops.put(id, List.copyOf(entries)));
    }

    public List<ShopEntry> entries(String npcId) {
        return shops.getOrDefault(npcId, List.of());
    }

    public void replaceEntries(String npcId, List<ShopEntry> entries) {
        List<ShopEntry> snapshot = entries.stream().map(entry -> new ShopEntry(entry.item(), entry.buyPrice(), entry.sellPrice())).toList();
        shops.put(npcId, List.copyOf(snapshot));
        repository.saveAsync(Map.copyOf(shops));
    }

    public boolean purchase(Player player, ShopEntry entry) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;
        if (!canFit(player, entry.item())) return false;
        long price = de.pixelrpg.rpg.economy.Money.fromMajor(entry.buyPrice());
        if (profile.getMoneyMinorUnits() < price) return false;
        profile.setMoneyMinorUnits(profile.getMoneyMinorUnits() - price);
        player.getInventory().addItem(entry.item().clone());
        profiles.saveProfileAsync(player.getUniqueId());
        return true;
    }

    public boolean sell(Player player, ShopEntry entry) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;
        if (!removeOneMatching(player, entry.item())) return false;
        profile.addMoney(entry.sellPrice());
        profiles.saveProfileAsync(player.getUniqueId());
        return true;
    }

    public void addEntry(String npcId, ShopEntry entry) {
        List<ShopEntry> updated = new ArrayList<>(entries(npcId));
        updated.add(entry);
        replaceEntries(npcId, updated);
    }

    public boolean removeEntry(String npcId, int index) {
        List<ShopEntry> existing = shops.get(npcId);
        if (existing == null || index < 0 || index >= existing.size()) return false;
        List<ShopEntry> updated = new ArrayList<>(existing);
        updated.remove(index);
        replaceEntries(npcId, updated);
        return true;
    }

    public boolean isTradeable(ItemStack item) {
        return item != null && !item.isEmpty() && items.isEconomySafeItem(item);
    }

    private boolean canFit(Player player, ItemStack incoming) {
        int remaining = incoming.getAmount();
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.isEmpty()) {
                remaining -= incoming.getMaxStackSize();
            } else if (current.isSimilar(incoming)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private boolean removeOneMatching(Player player, ItemStack template) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if (current == null || current.isEmpty() || !current.isSimilar(template)) continue;
            if (current.getAmount() == 1) player.getInventory().setItem(slot, null);
            else {
                ItemStack updated = current.clone();
                updated.setAmount(updated.getAmount() - 1);
                player.getInventory().setItem(slot, updated);
            }
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        repository.close();
    }
}
