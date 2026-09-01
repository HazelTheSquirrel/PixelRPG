package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;

/** Single inventory source for COLLECT quest progress and collect-item turn-in. */
public final class QuestInventoryTracker implements Listener {
    private final QuestManager questManager;
    private final PlayerProfileManager profiles;
    private final ItemService itemService;

    public QuestInventoryTracker(QuestManager questManager) {
        this.questManager = questManager;
        this.profiles = PixelRPGPlugin.getInstance().getPlayerProfileManager();
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
    }

    /** Recalculates all active COLLECT quests from the player's real inventory. */
    public void refresh(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).filter(PlayerProfile::isRegistered).orElse(null);
        if (profile == null) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questManager.getRepository().getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            QuestProgress progress = entry.getValue();
            int amount = Math.min(quest.requiredAmount(), count(player, quest.targetKey()));
            if (progress.getCurrentAmount() == amount) continue;
            progress.setCurrentAmount(amount);
            player.sendActionBar(QuestText.objectiveWithProgress(quest, progress));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) refresh(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) refresh(player);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) refresh(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    /** Consumes exactly the required COLLECT items after the quest has been successfully turned in. */
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Quest quest = questManager.getRepository().getQuest(event.getQuestId());
        if (quest == null || quest.type() != QuestType.COLLECT) return;
        remove(event.getPlayer(), quest.targetKey(), quest.requiredAmount());
    }

    private int count(Player player, String targetKey) {
        Target target = Target.parse(targetKey);
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            if (target.matches(item, itemService)) amount += item.getAmount();
        }
        return amount;
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
