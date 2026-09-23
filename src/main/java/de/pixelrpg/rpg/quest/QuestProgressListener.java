package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;

/** Connects runtime events to the rebuild quest domain without embedding quest rules in NPC/runtime code. */
public final class QuestProgressListener implements Listener {
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final ItemService items;

    public QuestProgressListener(QuestService quests, PlayerProfileManager profiles, ItemService items) {
        this.quests = quests;
        this.profiles = profiles;
        this.items = items;
    }

    // Advances HUNT objectives from real living-entity kills.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = entity.getKiller();
        if (player == null) return;
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        String target = entity.getType().name().toUpperCase(Locale.ROOT);
        boolean changed = false;
        for (QuestDefinition quest : quests.repository().all()) {
            if (quest.type() != QuestType.HUNT || !quest.targetKey().equalsIgnoreCase(target)) continue;
            if (profile.hasActiveQuest(quest.id())) {
                changed |= quests.incrementProgress(profile, quest.id(), 1);
            }
        }
        if (changed) profiles.saveProfileAsync(player.getUniqueId());
    }

    // Recalculates COLLECT objectives after an item pickup.
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) refresh(player);
    }

    // Recalculates COLLECT objectives after an inventory click.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) refresh(player);
    }

    // Recalculates COLLECT objectives after an inventory drag.
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) refresh(player);
    }

    // Recalculates COLLECT objectives after an item drop.
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        refresh(event.getPlayer());
    }

    // Recalculates COLLECT objectives after consuming an item.
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        refresh(event.getPlayer());
    }

    // Restores inventory-derived quest progress after profile activation.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    // Persists quest mutations produced by quest-domain completion events.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        profiles.saveProfileAsync(event.playerId());
    }

    private void refresh(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        boolean changed = false;
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            QuestDefinition quest = quests.repository().get(progress.getQuestId());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            int next = Math.min(quest.requiredAmount(), count(player, quest.targetKey()));
            if (progress.getCurrentAmount() != next) {
                progress.setCurrentAmount(next);
                changed = true;
            }
        }
        if (changed) profiles.saveProfileAsync(player.getUniqueId());
    }

    private int count(Player player, String targetKey) {
        String normalized = targetKey == null ? "" : targetKey.trim().toLowerCase(Locale.ROOT);
        int total = 0;
        for (var item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            if (normalized.startsWith("pixelrpg:")) {
                String id = items.getItemId(item).orElse("").toLowerCase(Locale.ROOT);
                if (canonical(id).equals(canonical(normalized))) total += item.getAmount();
            } else if (item.getType().name().equalsIgnoreCase(normalized)
                    || item.getType().getKey().asString().equalsIgnoreCase(normalized)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static String canonical(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("pixelrpg:")) return normalized;
        return "pixelrpg:" + normalized.substring("pixelrpg:".length()).replace('/', ':');
    }
}
