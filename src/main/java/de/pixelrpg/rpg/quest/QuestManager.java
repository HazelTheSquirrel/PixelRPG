package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.lang.LanguageManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestManager {
    public static final int MAX_ACTIVE_QUESTS = 5;

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final de.pixelrpg.rpg.api.GuildAPI guildAPI;
    private final GlobalEventState globalEventState;
    private final double partyShareRange;
    private final LanguageManager lang;
    private final Map<UUID, Map<String, Long>> questTimers = new ConcurrentHashMap<>();
    private BukkitTask timerTask;

    public QuestManager(Plugin plugin, QuestRepository questRepository, PlayerProfileManager profileManager,
                        de.pixelrpg.rpg.api.GuildAPI guildAPI, GlobalEventState globalEventState, double partyShareRange) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.guildAPI = guildAPI;
        this.globalEventState = globalEventState;
        this.partyShareRange = partyShareRange;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void startTimerCheckTask() {
        if (timerTask != null) return;
        timerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Map<String, Long>> playerEntry : new HashMap<>(questTimers).entrySet()) {
                UUID uuid = playerEntry.getKey();
                for (Map.Entry<String, Long> questEntry : new HashMap<>(playerEntry.getValue()).entrySet()) {
                    if (now < questEntry.getValue()) continue;
                    playerEntry.getValue().remove(questEntry.getKey());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        profileManager.getProfile(uuid).ifPresent(profile -> profile.removeActiveQuest(questEntry.getKey()));
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) lang.send(player, "quest.expired");
                    });
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        questTimers.clear();
    }

    public boolean canAccept(PlayerProfile profile, Quest quest) {
        if (!profile.isRegisteredInGuild()) return false;
        if (profile.hasCompletedQuest(quest.id())) return false;
        if (!questRepository.prerequisitesMet(profile, quest.id())) return false;
        return profile.getLevel() >= quest.requiredLevel();
    }

    public boolean acceptQuest(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || !canAccept(profile, quest) || profile.hasActiveQuest(quest.id())) return false;
        if (profile.getActiveQuests().size() >= MAX_ACTIVE_QUESTS) {
            player.sendMessage(Component.text("Du kannst maximal 5 Quests gleichzeitig aktiv haben.", NamedTextColor.RED));
            return false;
        }

        long expiry = quest.hasTimeLimit() ? System.currentTimeMillis() + quest.durationMinutes() * 60_000L : 0L;
        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));
        if (quest.hasTimeLimit()) {
            questTimers.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(quest.id(), expiry);
            lang.send(player, "quest.time-limit", "minutes", String.valueOf(quest.durationMinutes()));
        }

        lang.send(player, "quest.accepted", "title", quest.title());
        if (quest.type() == QuestType.GLOBAL_EVENT && globalEventState.getProgress(quest.id()) >= quest.requiredAmount()) {
            profile.getActiveQuests().get(quest.id()).setCurrentAmount(quest.requiredAmount());
            grantCompletion(player, profile, quest);
        }
        return true;
    }

    public boolean abandonQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || !profile.hasActiveQuest(questId)) return false;
        profile.removeActiveQuest(questId);
        removeTimer(player.getUniqueId(), questId);
        lang.send(player, "quest.abandoned");
        return true;
    }

    public boolean completeQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        Quest quest = questRepository.getQuest(questId);
        if (profile == null || !profile.isRegisteredInGuild() || quest == null || !profile.hasActiveQuest(questId)) return false;
        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress.getCurrentAmount() < quest.requiredAmount()) {
            lang.send(player, "quest.requirements-not-met");
            return false;
        }
        if (quest.type() == QuestType.GLOBAL_EVENT) return grantCompletion(player, profile, quest);
        if (!isAtQuestGiver(player, quest)) {
            lang.send(player, "quest.requirements-not-met");
            return false;
        }
        return grantCompletion(player, profile, quest);
    }

    private boolean isAtQuestGiver(Player player, Quest quest) {
        if (quest.questGiverNpcId() == null || quest.questGiverNpcId().isBlank()) return false;
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
                player.sendMessage(Component.text("Begleiter freigeschaltet: ", NamedTextColor.WHITE)
                        .append(Component.text(quest.rewardCompanionId(), NamedTextColor.YELLOW)));
            }
        }
        lang.send(player, "quest.completed", "title", quest.title());
        player.showTitle(Title.title(
                Component.text(quest.title(), NamedTextColor.YELLOW),
                Component.text(" "),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(300))));
        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, quest.id()));
        return true;
    }

    private void giveRewardItem(Player player, String definition, int fallbackLevel) {
        String[] parts = definition.split("\\|", -1);
        try {
            Material material = Material.valueOf(parts[0].trim().toUpperCase());
            if (parts.length == 1) {
                player.getInventory().addItem(new ItemStack(material));
                return;
            }
            ItemRarity rarity = ItemRarity.valueOf(parts[1].trim().toUpperCase());
            int itemLevel = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : fallbackLevel;
            RPGItemBuilder.createItem(material, rarity, Math.max(1, itemLevel)).ifPresent(item -> player.getInventory().addItem(item));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid quest reward item '" + definition + "'. Use MATERIAL or MATERIAL|RARITY|ITEM_LEVEL.");
        }
    }

    public void progressHuntQuests(Player killer, String mobTypeKey) {
        if (!isRegistered(killer.getUniqueId())) return;
        propagateToParty(killer, profile -> applyHuntProgress(profile, mobTypeKey), killer.getLocation());
    }

    private void applyHuntProgress(PlayerProfile profile, String mobTypeKey) {
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.HUNT || !quest.targetKey().equalsIgnoreCase(mobTypeKey)) continue;
            incrementProgress(profile, entry.getValue(), quest);
        }
    }

    public void checkInventoryQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            Material material = Material.matchMaterial(quest.targetKey());
            if (material == null || !material.isItem()) continue;
            int amount = 0;
            for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == material) amount += item.getAmount();
            entry.getValue().setCurrentAmount(Math.min(quest.requiredAmount(), amount));
        }
    }

    public void checkReachLocationQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.REACH_LOCATION || entry.getValue().getCurrentAmount() >= quest.requiredAmount()) continue;
            Location target = resolveNavigationLocation(player.getLocation(), quest);
            if (target == null || !player.getWorld().equals(target.getWorld())) continue;
            if (player.getLocation().distanceSquared(target) <= 64.0D) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                lang.send(player, "quest.location-reached", "title", quest.title());
            }
        }
    }

    /** Escort quests are intentionally not part of the final quest type pool. */
    public void checkEscortQuests(Player player) {
        // Intentionally empty: ESCORT is not an active quest type.
    }

    /** Updates TALK_TO_NPC quests when the configured NPC is interacted with. */
    public void progressTalkToNpc(Player player, String npcId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || npcId == null || npcId.isBlank()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
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
            if (updated >= quest.requiredAmount()) completeGlobalEvent(quest);
        }
    }

    private void completeGlobalEvent(Quest quest) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profileManager.getProfile(online.getUniqueId()).orElse(null);
            if (profile == null || !profile.isRegisteredInGuild() || !profile.hasActiveQuest(quest.id())) continue;
            profile.getActiveQuests().get(quest.id()).setCurrentAmount(quest.requiredAmount());
            grantCompletion(online, profile, quest);
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
            Biome[] biomes = quest.targetBiomeKeys().stream()
                    .map(org.bukkit.NamespacedKey::fromString)
                    .filter(java.util.Objects::nonNull)
                    .map(biomeRegistry::get)
                    .filter(java.util.Objects::nonNull)
                    .toArray(Biome[]::new);
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
        if (player != null && player.isOnline()) player.sendActionBar(lang.get("quest.progress", "current", String.valueOf(next), "required", String.valueOf(quest.requiredAmount())));
    }

    private void propagateToParty(Player source, java.util.function.Consumer<PlayerProfile> action, Location referenceLocation) {
        if (!isRegistered(source.getUniqueId())) return;
        de.pixelrpg.rpg.api.PartyAPI partyAPI = Bukkit.getServicesManager().load(de.pixelrpg.rpg.api.PartyAPI.class);
        if (partyAPI == null || !partyAPI.isInParty(source.getUniqueId())) {
            profileManager.getProfile(source.getUniqueId()).filter(PlayerProfile::isRegisteredInGuild).ifPresent(action);
            return;
        }
        Set<UUID> members = partyAPI.getPartyMembers(source.getUniqueId());
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) continue;
            if (!member.getWorld().equals(referenceLocation.getWorld()) || member.getLocation().distance(referenceLocation) > partyShareRange) continue;
            profileManager.getProfile(memberUuid).filter(PlayerProfile::isRegisteredInGuild).ifPresent(action);
        }
    }

    private void removeTimer(UUID uuid, String questId) {
        Map<String, Long> timers = questTimers.get(uuid);
        if (timers != null) timers.remove(questId);
    }

    private boolean isRegistered(UUID uuid) {
        return profileManager.getProfile(uuid).map(PlayerProfile::isRegisteredInGuild).orElse(false);
    }

    public QuestRepository getRepository() { return questRepository; }
}
