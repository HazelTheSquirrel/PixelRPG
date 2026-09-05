package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class QuestManager {
    public static final int MAX_ACTIVE_QUESTS = 5;

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final GlobalEventState globalEventState;
    private final double partyShareRange;
    private final ItemService itemService;
    private final Map<QuestTimerKey, BukkitTask> questExpiryTasks = new ConcurrentHashMap<>();

    public QuestManager(Plugin plugin, QuestRepository questRepository, PlayerProfileManager profileManager,
                        de.pixelrpg.rpg.api.GuildAPI guildAPI, GlobalEventState globalEventState, double partyShareRange) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.globalEventState = globalEventState;
        this.partyShareRange = partyShareRange;
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
        if (this.itemService == null) throw new IllegalStateException("ItemService must be initialized before QuestManager.");
    }

    /** Schedules persisted timed quests after a player has joined and their profile is active. */
    public void restoreTimers(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            if (progress.hasExpiry()) {
                Quest quest = questRepository.getQuest(progress.getQuestId());
                if (quest != null) scheduleExpiry(player.getUniqueId(), quest.id(), progress.getExpiryTimestampMillis());
            }
        }
    }

    public void shutdown() {
        questExpiryTasks.values().forEach(BukkitTask::cancel);
        questExpiryTasks.clear();
    }

    public boolean canAccept(PlayerProfile profile, Quest quest) {
        if (!profile.isRegistered()) return false;
        if (profile.hasCompletedQuest(quest.id())) return false;
        if (!questRepository.prerequisitesMet(profile, quest.id())) return false;
        if (profile.getLevel() < quest.requiredLevel()) return false;
        return !quest.isProfessionQuest() || (profile.hasLearnedProfession(quest.profession())
                && getProfessionLevel(profile, quest) >= quest.requiredProfessionLevel());
    }

    private int getProfessionLevel(PlayerProfile profile, Quest quest) {
        return PixelRPGPlugin.getInstance().getProfessionSystem().professionService()
                .getLevel(profile.getUuid(), quest.profession());
    }

    public boolean acceptQuest(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !canAccept(profile, quest) || profile.hasActiveQuest(quest.id())) return false;
        if (profile.getActiveQuests().size() >= MAX_ACTIVE_QUESTS) {
            player.sendMessage(Component.text("Du kannst maximal " + MAX_ACTIVE_QUESTS + " Quests gleichzeitig aktiv haben.", NamedTextColor.RED));
            return false;
        }
        long expiry = quest.hasTimeLimit() ? System.currentTimeMillis() + quest.durationMinutes() * 60_000L : 0L;
        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));
        if (quest.hasTimeLimit()) {
            scheduleExpiry(player.getUniqueId(), quest.id(), expiry);
            player.sendMessage(Component.text("Zeitlimit: " + quest.durationMinutes() + " Minuten!", NamedTextColor.YELLOW));
        }
        player.sendMessage(Component.text("Quest angenommen: ").color(NamedTextColor.GREEN)
                .append(Component.text(QuestText.titlePlain(player, quest), NamedTextColor.YELLOW)));
        return true;
    }

    private void scheduleExpiry(UUID playerId, String questId, long expiryMillis) {
        QuestTimerKey key = new QuestTimerKey(playerId, questId);
        BukkitTask old = questExpiryTasks.remove(key);
        if (old != null) old.cancel();
        long remainingMillis = expiryMillis - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            expireQuest(key);
            return;
        }
        long delayTicks = Math.max(1L, (remainingMillis + 49L) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            questExpiryTasks.remove(key);
            expireQuest(key);
        }, delayTicks);
        questExpiryTasks.put(key, task);
    }

    private void expireQuest(QuestTimerKey key) {
        PlayerProfile profile = profileManager.getProfile(key.playerId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !profile.hasActiveQuest(key.questId())) return;
        QuestProgress progress = profile.getActiveQuests().get(key.questId());
        if (progress == null || !progress.isExpired()) {
            if (progress != null) scheduleExpiry(key.playerId(), key.questId(), progress.getExpiryTimestampMillis());
            return;
        }
        profile.removeActiveQuest(key.questId());
        Player player = Bukkit.getPlayer(key.playerId());
        if (player != null && player.isOnline()) player.sendMessage(Component.text("Dein Quest-Vertrag ist abgelaufen!", NamedTextColor.RED));
    }

    public boolean abandonQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || !profile.hasActiveQuest(questId)) return false;
        profile.removeActiveQuest(questId);
        removeTimer(player.getUniqueId(), questId);
        player.sendMessage(Component.text("Quest abgebrochen.", NamedTextColor.YELLOW));
        return true;
    }

    public boolean completeQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        Quest quest = questRepository.getQuest(questId);
        if (profile == null || !profile.isRegistered() || quest == null || !profile.hasActiveQuest(questId)) return false;
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress.isExpired()) {
            profile.removeActiveQuest(questId);
            removeTimer(player.getUniqueId(), quest.id());
            player.sendMessage(Component.text("Dein Quest-Vertrag ist abgelaufen!", NamedTextColor.RED));
            return false;
        }
        if (progress.getCurrentAmount() < quest.requiredAmount()) {
            player.sendMessage(Component.text("Anforderungen noch nicht erfüllt.", NamedTextColor.RED));
            return false;
        }
        if (!isAtQuestGiver(player, quest)) {
            player.sendMessage(Component.text("Anforderungen noch nicht erfüllt.", NamedTextColor.RED));
            return false;
        }
        return grantCompletion(player, profile, quest);
    }

    private boolean isAtQuestGiver(Player player, Quest quest) {
        if (quest.isProfessionQuest()) return true;
        if (quest.questGiverNpcId() == null || quest.questGiverNpcId().isBlank()) {
            return PixelRPGPlugin.getInstance().getNpcManager().getAll().stream()
                    .filter(npc -> npc.type() == NpcType.QUEST)
                    .map(RPGNpc::location)
                    .filter(location -> location.getWorld() != null && player.getWorld().equals(location.getWorld()))
                    .anyMatch(location -> player.getLocation().distanceSquared(location) <= 36.0D);
        }
        return PixelRPGPlugin.getInstance().getNpcManager().getById(quest.questGiverNpcId())
                .map(RPGNpc::location)
                .filter(location -> location.getWorld() != null && player.getWorld().equals(location.getWorld()))
                .map(location -> player.getLocation().distanceSquared(location) <= 36.0D)
                .orElse(false);
    }

    private boolean grantCompletion(Player player, PlayerProfile profile, Quest quest) {
        profile.removeActiveQuest(quest.id());
        profile.markQuestCompleted(quest.id());
        removeTimer(player.getUniqueId(), quest.id());
        if (quest.rewardMoney() > 0.0D) profile.addMoney(quest.rewardMoney());
        if (quest.rewardExp() > 0L) profileManager.addExperience(player.getUniqueId(), quest.rewardExp());
        for (String rewardDefinition : quest.rewardItemMaterials()) giveRewardItem(player, rewardDefinition, quest.requiredLevel());
        if (quest.rewardsCompanion()) {
            var companionService = PixelRPGPlugin.getInstance().getCompanionService();
            if (companionService != null && companionService.unlockDefinition(player.getUniqueId(), quest.rewardCompanionId())) {
                player.sendMessage(Component.text("Begleiter freigeschaltet: " + quest.rewardCompanionId(), NamedTextColor.GREEN));
            }
        }
        player.sendMessage(Component.text("Quest abgeschlossen: ").color(NamedTextColor.GREEN)
                .append(Component.text(QuestText.titlePlain(player, quest), NamedTextColor.YELLOW)));
        player.showTitle(Title.title(QuestText.title(player, quest).color(NamedTextColor.YELLOW), Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(300))));
        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, quest.id()));
        return true;
    }

    private void giveRewardItem(Player player, String definition, int fallbackLevel) {
        String[] parts = definition.split("\\|", -1);
        try {
            String rewardId = parts[0].trim();
            if (rewardId.toLowerCase(Locale.ROOT).startsWith("pixelrpg:")) {
                int itemLevel = parts.length >= 2 ? Integer.parseInt(parts[1].trim()) : fallbackLevel;
                itemService.createItem(canonicalItemId(rewardId), Math.max(1, itemLevel)).ifPresent(item -> player.getInventory().addItem(item));
                return;
            }
            Material material = Material.matchMaterial(rewardId);
            if (material == null || material.isAir()) throw new IllegalArgumentException("Unknown material");
            if (parts.length == 1) { player.getInventory().addItem(new ItemStack(material)); return; }
            ItemRarity rarity = ItemRarity.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            int itemLevel = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : fallbackLevel;
            itemService.createVanillaReward(material, rarity, Math.max(1, itemLevel)).ifPresent(item -> player.getInventory().addItem(item));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid quest reward item '" + definition + "'. Use MATERIAL, MATERIAL|RARITY|ITEM_LEVEL or pixelrpg:item-id|ITEM_LEVEL.");
        }
    }

    public void progressHuntQuests(Player killer, String mobTypeKey) {
        if (!isRegistered(killer.getUniqueId())) return;
        propagateToParty(killer, profile -> applyHuntProgress(profile, mobTypeKey), killer.getLocation());
    }

    private void applyHuntProgress(PlayerProfile profile, String mobTypeKey) {
        for (var entry : profile.getActiveQuests().entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.HUNT || !quest.targetKey().equalsIgnoreCase(mobTypeKey)) continue;
            incrementProgress(profile, entry.getValue(), quest);
        }
    }

    /** Recalculates COLLECT progress from the player's current inventory. */
    public void checkInventoryQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        for (var entry : profile.getActiveQuests().entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            int amount = countQuestItems(player, quest.targetKey());
            int next = Math.min(quest.requiredAmount(), amount);
            QuestProgress progress = entry.getValue();
            if (progress.getCurrentAmount() == next) continue;
            progress.setCurrentAmount(next);
            player.sendActionBar(QuestText.objectiveWithProgress(player, quest, progress));
        }
    }

    private int countQuestItems(Player player, String targetKey) {
        Material material = Material.matchMaterial(targetKey);
        String normalizedTarget = canonicalItemId(targetKey);
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            if (material != null && item.getType() == material && !isSpecificRpgItemTarget(normalizedTarget)) { amount += item.getAmount(); continue; }
            if (itemService.getItemId(item).map(id -> canonicalItemId(id).equals(normalizedTarget)).orElse(false)) amount += item.getAmount();
        }
        return amount;
    }

    private boolean isSpecificRpgItemTarget(String normalizedTarget) { return normalizedTarget.startsWith("pixelrpg:"); }

    private String canonicalItemId(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("pixelrpg:")) return normalized;
        String body = normalized.substring("pixelrpg:".length());
        int slash = body.indexOf('/');
        if (slash > 0) body = body.substring(0, slash) + ":" + body.substring(slash + 1);
        return "pixelrpg:" + body;
    }

    public void checkReachLocationQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        for (var entry : profile.getActiveQuests().entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.REACH_LOCATION || entry.getValue().getCurrentAmount() >= quest.requiredAmount()) continue;
            Location target = resolveNavigationLocation(player.getLocation(), quest);
            if (target == null || !player.getWorld().equals(target.getWorld())) continue;
            if (player.getLocation().distanceSquared(target) <= 64.0D) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                player.sendMessage(Component.text("Ort erreicht: ").color(NamedTextColor.GREEN).append(Component.text(QuestText.titlePlain(player, quest), NamedTextColor.YELLOW)));
            }
        }
    }

    /** Updates TALK_TO_NPC quests when the configured NPC is interacted with. */
    public void progressTalkToNpc(Player player, String npcId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered() || npcId == null || npcId.isBlank()) return;
        for (var entry : profile.getActiveQuests().entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.TALK_TO_NPC || !quest.targetKey().equalsIgnoreCase(npcId)) continue;
            entry.getValue().setCurrentAmount(quest.requiredAmount());
        }
    }

    public void progressGlobalEvent(String targetKey) {
        for (Quest quest : questRepository.getQuestsByType(QuestType.GLOBAL_EVENT)) {
            if (!quest.targetKey().equalsIgnoreCase(targetKey)) continue;
            int current = globalEventState.getProgress(quest.id());
            if (current >= quest.requiredAmount()) continue;
            int updated = globalEventState.addProgress(quest.id(), 1);
            if (updated >= quest.requiredAmount()) notifyGlobalEventCompleted(quest);
        }
    }

    private void notifyGlobalEventCompleted(Quest quest) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profileManager.getProfile(online.getUniqueId()).orElse(null);
            if (profile == null || !profile.isRegistered() || !profile.hasActiveQuest(quest.id())) continue;
            profile.getActiveQuests().get(quest.id()).setCurrentAmount(quest.requiredAmount());
            online.sendMessage(Component.text("Fortschritt: " + quest.requiredAmount() + "/" + quest.requiredAmount(), NamedTextColor.GREEN));
        }
    }

    public int getGlobalEventProgress(String questId) { return globalEventState.getProgress(questId); }

    private Location resolveNavigationLocation(Location origin, Quest quest) {
        if (quest.targetStructureKey() != null && !quest.targetStructureKey().isBlank()) {
            var structureRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
            var key = org.bukkit.NamespacedKey.fromString(quest.targetStructureKey());
            if (key != null) {
                Structure structure = structureRegistry.get(key);
                if (structure != null) {
                    var result = origin.getWorld().locateNearestStructure(origin, structure, quest.navigationRadius(), false);
                    if (result != null) return result.getLocation();
                }
            }
        }
        if (!quest.targetBiomeKeys().isEmpty()) {
            var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
            Biome[] biomes = quest.targetBiomeKeys().stream().map(org.bukkit.NamespacedKey::fromString).filter(java.util.Objects::nonNull).map(biomeRegistry::get).filter(java.util.Objects::nonNull).toArray(Biome[]::new);
            if (biomes.length > 0) {
                var result = origin.getWorld().locateNearestBiome(origin, quest.navigationRadius(), biomes);
                if (result != null) return result.getLocation();
            }
        }
        return quest.reachLocation();
    }

    private void incrementProgress(PlayerProfile profile, QuestProgress progress, Quest quest) {
        if (progress.getCurrentAmount() >= quest.requiredAmount()) return;
        int next = Math.min(quest.requiredAmount(), progress.getCurrentAmount() + 1);
        progress.setCurrentAmount(next);
        Player player = Bukkit.getPlayer(profile.getUuid());
        if (player != null && player.isOnline()) player.sendActionBar(QuestText.objectiveWithProgress(player, quest, progress));
    }

    private void propagateToParty(Player source, Consumer<PlayerProfile> action, Location referenceLocation) {
        if (!isRegistered(source.getUniqueId())) return;
        de.pixelrpg.rpg.api.PartyAPI partyAPI = Bukkit.getServicesManager().load(de.pixelrpg.rpg.api.PartyAPI.class);
        if (partyAPI == null || !partyAPI.isInParty(source.getUniqueId())) {
            profileManager.getProfile(source.getUniqueId()).filter(PlayerProfile::isRegistered).ifPresent(action);
            return;
        }
        Set<UUID> members = partyAPI.getPartyMembers(source.getUniqueId());
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) continue;
            if (!member.getWorld().equals(referenceLocation.getWorld()) || member.getLocation().distance(referenceLocation) > partyShareRange) continue;
            profileManager.getProfile(memberUuid).filter(PlayerProfile::isRegistered).ifPresent(action);
        }
    }

    private void removeTimer(UUID uuid, String questId) {
        BukkitTask task = questExpiryTasks.remove(new QuestTimerKey(uuid, questId));
        if (task != null) task.cancel();
    }

    private boolean isRegistered(UUID uuid) { return profileManager.getProfile(uuid).map(PlayerProfile::isRegistered).orElse(false); }
    public QuestRepository getRepository() { return questRepository; }

    private record QuestTimerKey(UUID playerId, String questId) { }
}
