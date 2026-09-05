package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.WakeScheduler;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Single inventory source for COLLECT quest progress and collect-item turn-in. */
public final class QuestInventoryTracker implements Listener {
    private final QuestManager questManager;
    private final PlayerProfileManager profiles;
    private final ItemService itemService;
    private final WakeScheduler<UUID> refreshScheduler;

    public QuestInventoryTracker(QuestManager questManager) {
        this.questManager = questManager;
        this.profiles = PixelRPGPlugin.getInstance().getPlayerProfileManager();
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
        this.refreshScheduler = new WakeScheduler<>(PixelRPGPlugin.getInstance());
    }

    /** Recalculates all active COLLECT quests from one inventory traversal. */
    public void refresh(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElse(null);
        if (profile == null) return;

        Map<String, QuestProgress> collectQuests = new HashMap<>();
        for (var entry : profile.getActiveQuests().entrySet()) {
            Quest quest = questManager.getRepository().getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            collectQuests.put(quest.targetKey(), entry.getValue());
        }
        if (collectQuests.isEmpty()) return;

        Map<String, Integer> amounts = countTargets(player, collectQuests.keySet());
        for (var entry : collectQuests.entrySet()) {
            QuestProgress progress = entry.getValue();
            Quest quest = questManager.getRepository().getQuest(progress.getQuestId());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            int amount = Math.min(quest.requiredAmount(), amounts.getOrDefault(entry.getKey(), 0));
            if (progress.getCurrentAmount() == amount) continue;
            progress.setCurrentAmount(amount);
            player.sendActionBar(QuestText.objectiveWithProgress(quest, progress));
        }
    }

    /** Schedules one coalesced inventory refresh for the player. */
    public void scheduleRefresh(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        refreshScheduler.wake(uuid, () -> {
            Player current = PixelRPGPlugin.getInstance().getServer().getPlayer(uuid);
            if (current != null && current.isOnline()) refresh(current);
        });
    }

    /** Cancels pending inventory refreshes when the tracker is stopped. */
    public void shutdown() {
        refreshScheduler.clear();
    }

    // Inventory clicks can trigger several related mutations; one post-mutation refresh is sufficient.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleRefresh(player);
    }

    // Inventory drags may update several slots in one interaction; coalesce them into one refresh.
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleRefresh(player);
    }

    // Item pickup changes the player's collect state after the event completes.
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) scheduleRefresh(player);
    }

    // Dropping an item can invalidate one or more collect objectives.
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    // Changing the held slot can alter inventory-derived quest state without requiring an immediate scan.
    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    // Consuming an item may reduce collect progress and is refreshed after the mutation.
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    // A joined player needs one initial collect-progress calculation after profile activation.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    // Disconnecting a player removes queued inventory work immediately.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        refreshScheduler.cancel(event.getPlayer().getUniqueId());
    }

    // Completing a collect quest consumes its required items and then refreshes the remaining objectives.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Quest quest = questManager.getRepository().getQuest(event.getQuestId());
        if (quest == null || quest.type() != QuestType.COLLECT) return;
        remove(event.getPlayer(), quest.targetKey(), quest.requiredAmount());
        scheduleRefresh(event.getPlayer());
    }

    private Map<String, Integer> countTargets(Player player, Iterable<String> targetKeys) {
        Map<String, Target> targets = new HashMap<>();
        Map<String, Integer> amounts = new HashMap<>();
        for (String targetKey : targetKeys) targets.putIfAbsent(targetKey, Target.parse(targetKey));

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            Material material = item.getType();
            String itemId = null;
            for (var entry : targets.entrySet()) {
                Target target = entry.getValue();
                boolean matches;
                if (target.pixelRpgId() != null) {
                    if (itemId == null) itemId = itemService.getItemId(item).map(Target::canonicalItemId).orElse(null);
                    matches = target.pixelRpgId().equals(itemId);
                } else {
                    matches = target.material() != null && material == target.material();
                }
                if (matches) amounts.merge(entry.getKey(), item.getAmount(), Integer::sum);
            }
        }
        return amounts;
    }

    private void remove(Player player, String targetKey, int amount) {
        if (amount <= 0) return;
        Target target = Target.parse(targetKey);
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.isEmpty() || !target.matches(item, itemService)) continue;
            int remove = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            if (item.getAmount() <= 0) contents[slot] = null;
            remaining -= remove;
        }
        player.getInventory().setContents(contents);
    }

    private record Target(Material material, String pixelRpgId) {
        static Target parse(String raw) {
            String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("pixelrpg:")) {
                String body = value.substring("pixelrpg:".length()).replace('/', ':');
                while (body.contains("::")) body = body.replace("::", ":");
                return new Target(null, "pixelrpg:" + body);
            }
            Material material = Material.matchMaterial(value);
            return new Target(material, null);
        }

        boolean matches(ItemStack item, ItemService itemService) {
            if (pixelRpgId != null) {
                return itemService.getItemId(item)
                        .map(Target::canonicalItemId)
                        .filter(pixelRpgId::equals)
                        .isPresent();
            }
            return material != null && item.getType() == material;
        }

        private static String canonicalItemId(String raw) {
            String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (!value.startsWith("pixelrpg:")) return value;
            String body = value.substring("pixelrpg:".length()).replace('/', ':');
            while (body.contains("::")) body = body.replace("::", ":");
            return "pixelrpg:" + body;
        }
    }
}
