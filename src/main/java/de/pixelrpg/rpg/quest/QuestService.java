package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.core.WakeScheduler;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class QuestService {
    private static final int MAX_ACTIVE_QUESTS = 5;

    private final Plugin plugin;
    private final QuestRepository repository;
    private final PlayerProfileManager profiles;
    private final ItemService items;
    private final WakeScheduler<QuestTimerKey> expiryScheduler;

    public QuestService(Plugin plugin, QuestRepository repository,
                        PlayerProfileManager profiles, ItemService items) {
        this.plugin = plugin;
        this.repository = repository;
        this.profiles = profiles;
        this.items = items;
        this.expiryScheduler = new WakeScheduler<>(plugin);
    }

    public void restoreTimers(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            if (progress.hasExpiry()) scheduleExpiry(player.getUniqueId(), progress.getQuestId(), progress.getExpiryTimestampMillis());
        }
    }

    public boolean canAccept(PlayerProfile profile, Quest quest) {
        if (profile == null || quest == null || !profile.isRegistered()) return false;
        if (profile.hasCompletedQuest(quest.id()) || profile.hasActiveQuest(quest.id())) return false;
        if (!repository.prerequisitesMet(profile, quest.id())) return false;
        if (profile.getLevel() < quest.requiredLevel()) return false;
        return !quest.isProfessionQuest()
                || profile.hasLearnedProfession(quest.profession());
    }

    public boolean accept(Player player, String questId) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        Quest quest = repository.getQuest(questId);
        if (!canAccept(profile, quest)) return false;
        if (profile.getActiveQuests().size() >= MAX_ACTIVE_QUESTS) return false;

        long expiry = quest.hasTimeLimit()
                ? System.currentTimeMillis() + quest.durationMinutes() * 60_000L : 0L;
        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));
        profiles.saveProfileAsync(player.getUniqueId());
        if (expiry > 0L) scheduleExpiry(player.getUniqueId(), quest.id(), expiry);
        return true;
    }

    public boolean abandon(Player player, String questId) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || !profile.hasActiveQuest(questId)) return false;
        profile.removeActiveQuest(questId);
        expiryScheduler.cancel(new QuestTimerKey(player.getUniqueId(), questId));
        profiles.saveProfileAsync(player.getUniqueId());
        return true;
    }

    public void progressHunt(Player source, String entityType) {
        propagate(source, profile -> {
            for (Map.Entry<String, QuestProgress> entry : profile.getActiveQuests().entrySet()) {
                Quest quest = repository.getQuest(entry.getKey());
                if (quest == null || quest.type() != QuestType.HUNT
                        || !quest.targetKey().equalsIgnoreCase(entityType)) continue;
                increment(entry.getValue(), quest);
            }
        });
    }

    public void checkCollect(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        for (Map.Entry<String, QuestProgress> entry : profile.getActiveQuests().entrySet()) {
            Quest quest = repository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            entry.getValue().setCurrentAmount(Math.min(quest.requiredAmount(),
                    countItems(player, quest.targetKey())));
        }
        profiles.saveProfileAsync(player.getUniqueId());
    }

    public void progressTalk(Player player, String npcId) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        for (Map.Entry<String, QuestProgress> entry : profile.getActiveQuests().entrySet()) {
            Quest quest = repository.getQuest(entry.getKey());
            if (quest != null && quest.type() == QuestType.TALK_TO_NPC
                    && quest.targetKey().equalsIgnoreCase(npcId)) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
            }
        }
        profiles.saveProfileAsync(player.getUniqueId());
    }

    public boolean complete(Player player, String questId) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        Quest quest = repository.getQuest(questId);
        if (profile == null || quest == null || !profile.hasActiveQuest(questId)) return false;
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress == null || progress.isExpired() || progress.getCurrentAmount() < quest.requiredAmount()) return false;

        profile.removeActiveQuest(questId);
        profile.markQuestCompleted(quest.id());
        if (quest.rewardMoney() > 0.0D) profile.addMoney(quest.rewardMoney());
        if (quest.rewardExp() > 0L) profiles.addExperience(player.getUniqueId(), quest.rewardExp());

        for (String reward : quest.rewardItemMaterials()) giveReward(player, reward, quest.requiredLevel());
        profiles.saveProfileAsync(player.getUniqueId());
        expiryScheduler.cancel(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, quest.id()));
        return true;
    }

    public void shutdown() {
        expiryScheduler.clear();
    }

    public QuestRepository repository() {
        return repository;
    }

    private void scheduleExpiry(UUID playerId, String questId, long expiryMillis) {
        long remaining = expiryMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            expire(playerId, questId);
            return;
        }
        expiryScheduler.wakeLater(new QuestTimerKey(playerId, questId), Math.max(1L, (remaining + 49L) / 50L),
                () -> expire(playerId, questId));
    }

    private void expire(UUID playerId, String questId) {
        PlayerProfile profile = profiles.get(playerId);
        if (profile == null) return;
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress == null) return;
        if (!progress.isExpired()) {
            scheduleExpiry(playerId, questId, progress.getExpiryTimestampMillis());
            return;
        }
        profile.removeActiveQuest(questId);
        profiles.saveProfileAsync(playerId);
    }

    private void increment(QuestProgress progress, Quest quest) {
        if (progress.getCurrentAmount() < quest.requiredAmount()) {
            progress.setCurrentAmount(Math.min(quest.requiredAmount(), progress.getCurrentAmount() + 1));
        }
    }

    private int countItems(Player player, String target) {
        Material material = Material.matchMaterial(target);
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            if (material != null && item.getType() == material) amount += item.getAmount();
            else if (items.getItemId(item).map(id -> id.equalsIgnoreCase(target)).orElse(false)) amount += item.getAmount();
        }
        return amount;
    }

    private void giveReward(Player player, String definition, int fallbackLevel) {
        String[] parts = definition.split("\\|", -1);
        String id = parts[0].trim();
        if (id.toLowerCase(Locale.ROOT).startsWith("pixelrpg:")) {
            int level = parts.length > 1 ? Integer.parseInt(parts[1]) : fallbackLevel;
            items.createItem(id, Math.clamp(level, 1, 99)).ifPresent(stack -> player.getInventory().addItem(stack));
            return;
        }
        Material material = Material.matchMaterial(id);
        if (material == null || material.isAir()) return;
        if (parts.length == 1) {
            player.getInventory().addItem(new ItemStack(material));
            return;
        }
        ItemRarity rarity = ItemRarity.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
        int level = parts.length > 2 ? Integer.parseInt(parts[2]) : fallbackLevel;
        items.createItem(material, rarity, Math.clamp(level, 1, 99))
                .ifPresent(stack -> player.getInventory().addItem(stack));
    }

    private record QuestTimerKey(UUID playerId, String questId) { }

    private void propagate(Player source, java.util.function.Consumer<PlayerProfile> action) {
        PartyAPI party = Bukkit.getServicesManager().load(PartyAPI.class);
        if (party == null || !party.isInParty(source.getUniqueId())) {
            PlayerProfile profile = profiles.get(source.getUniqueId());
            if (profile != null) action.accept(profile);
            return;
        }
        for (UUID member : party.getPartyMembers(source.getUniqueId())) {
            Player online = Bukkit.getPlayer(member);
            if (online == null || !online.isOnline()) continue;
            if (!online.getWorld().equals(source.getWorld())
                    || online.getLocation().distanceSquared(source.getLocation()) > party.getShareRange() * party.getShareRange()) continue;
            PlayerProfile profile = profiles.get(member);
            if (profile != null) action.accept(profile);
        }
    }
}
