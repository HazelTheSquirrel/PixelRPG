package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
                        profileManager.getProfile(uuid).filter(PlayerProfile::isRegisteredInGuild)
                                .ifPresent(profile -> profile.removeActiveQuest(questEntry.getKey()));
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

    /** A quest becomes available the configured number of player levels before its recommended level. */
    public boolean canAccept(PlayerProfile profile, Quest quest) {
        if (!profile.isRegisteredInGuild()) return false;
        if (profile.hasCompletedQuest(quest.id())) return false;
        if (!questRepository.prerequisitesMet(profile, quest.id())) return false;
        int minimumLevel = Math.max(1, quest.requiredLevel() - questRepository.unlockEarlyLevels());
        return profile.getLevel() >= minimumLevel;
    }

    public boolean acceptQuest(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || !canAccept(profile, quest) || profile.hasActiveQuest(quest.id())) return false;
        if (profile.getActiveQuests().size() >= questRepository.maxActiveQuests()) {
            lang.send(player, "quest.max-active");
            return false;
        }
        long expiry = quest.hasTimeLimit() ? System.currentTimeMillis() + quest.durationMinutes() * 60_000L : 0L;
        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));
        if (quest.hasTimeLimit()) {
            questTimers.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(quest.id(), expiry);
            lang.send(player, "quest.time-limit", "minutes", String.valueOf(quest.durationMinutes()));
        }
        lang.send(player, "quest.accepted", "title", quest.title());
        return true;
    }

    public boolean abandonQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || !profile.hasActiveQuest(questId)) return false;
        profile.removeActiveQuest(questId);
        Map<String, Long> timers = questTimers.get(player.getUniqueId());
        if (timers != null) timers.remove(questId);
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

        profile.removeActiveQuest(questId);
        profile.markQuestCompleted(questId);
        Map<String, Long> timers = questTimers.get(player.getUniqueId());
        if (timers != null) timers.remove(questId);
        if (quest.rewardMoney() > 0) profile.addMoney(quest.rewardMoney());
        if (quest.rewardExp() > 0) profileManager.addExperience(player.getUniqueId(), quest.rewardExp());

        for (String materialName : quest.rewardItemMaterials()) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                player.getInventory().addItem(new ItemStack(material));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (quest.rewardsCompanion()) {
            var companionService = PixelRPGPlugin.getInstance().getCompanionService();
            if (companionService != null && companionService.unlockDefinition(player.getUniqueId(), quest.rewardCompanionId())) {
                player.sendMessage(Component.text("Begleiter freigeschaltet: ", NamedTextColor.WHITE)
                        .append(Component.text(quest.rewardCompanionId(), NamedTextColor.YELLOW)));
            } else {
                plugin.getLogger().warning("Quest '" + quest.id() + "' rewards unknown or non-player companion '" + quest.rewardCompanionId() + "'.");
            }
        }

        lang.send(player, "quest.completed", "title", quest.title());
        player.showTitle(Title.title(
                Component.text(quest.title(), NamedTextColor.YELLOW),
                Component.text(" "),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(300))
        ));
        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, questId));
        return true;
    }

    public void progressHuntQuests(Player killer, String mobTypeKey) {
        if (!isRegistered(killer.getUniqueId())) return;
        propagateToParty(killer, profile -> applyHuntProgress(profile, mobTypeKey), killer.getLocation());
    }

    private void applyHuntProgress(PlayerProfile profile, String mobTypeKey) {
        if (!profile.isRegisteredInGuild()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.HUNT || !quest.targetKey().equalsIgnoreCase(mobTypeKey)) continue;
            incrementProgress(profile, entry.getValue(), quest);
        }
    }

    public void checkInventoryQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || profile.getActiveQuests().isEmpty()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) continue;
            try {
                Material material = Material.valueOf(quest.targetKey().toUpperCase());
                int amount = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) amount += item.getAmount();
                }
                entry.getValue().setCurrentAmount(Math.min(quest.requiredAmount(), amount));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void checkReachLocationQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || profile.getActiveQuests().isEmpty()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.REACH_LOCATION) continue;
            Location target = resolveQuestLocation(quest);
            if (target == null || entry.getValue().getCurrentAmount() >= quest.requiredAmount()) continue;
            boolean reached = quest.reachRadius() <= 0.0
                    ? isExactBlock(player.getLocation(), target)
                    : isWithinRadius(player.getLocation(), target, quest.reachRadius());
            if (reached) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                lang.send(player, "quest.location-reached", "title", quest.title());
            }
        }
    }

    /** Checks escort quests against their configured destination or stable target NPC. */
    public void checkEscortQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || profile.getActiveQuests().isEmpty()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.ESCORT || entry.getValue().getCurrentAmount() >= quest.requiredAmount()) continue;
            Location destination = resolveQuestLocation(quest);
            if (destination != null && isWithinRadius(player.getLocation(), destination, Math.max(3.0D, quest.reachRadius()))) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                lang.send(player, "quest.escort-delivered");
            }
        }
    }

    /** Marks NPC dialogue quests complete when the configured NPC id is interacted with. */
    public void progressTalkToNpc(Player player, String npcId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild() || npcId == null || npcId.isBlank()) return;
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.TALK_TO_NPC) continue;
            if (!quest.targetKey().equalsIgnoreCase(npcId)) continue;
            if (entry.getValue().getCurrentAmount() >= quest.requiredAmount()) continue;
            entry.getValue().setCurrentAmount(quest.requiredAmount());
            lang.send(player, "quest.location-reached", "title", quest.title());
        }
    }

    public void progressEscortQuest(Player player, String escortKey) {
        checkEscortQuests(player);
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
            if (!guildAPI.isRegistered(online.getUniqueId())) continue;
            PlayerProfile profile = profileManager.getProfile(online.getUniqueId()).orElse(null);
            if (profile != null && profile.hasActiveQuest(quest.id())) {
                QuestProgress progress = profile.getActiveQuests().get(quest.id());
                progress.setCurrentAmount(quest.requiredAmount());
                completeQuest(online, quest.id());
                continue;
            }
            lang.send(online, "quest.completed", "title", quest.title());
            online.showTitle(Title.title(
                    Component.text(quest.title(), NamedTextColor.GOLD),
                    Component.text(" "),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
            ));
        }
    }

    public int getGlobalEventProgress(String questId) { return globalEventState.getProgress(questId); }

    private Location resolveQuestLocation(Quest quest) {
        if (quest.reachLocation() != null) return quest.reachLocation();
        if (quest.escortDestination() != null) return quest.escortDestination();
        String key = quest.targetKey();
        if (key == null || key.isBlank()) return null;
        String id = key;
        int separator = key.indexOf(':');
        if (separator >= 0) id = key.substring(separator + 1);
        return PixelRPGPlugin.getInstance().getNpcManager().getById(id).map(RPGNpc::location).orElse(null);
    }

    private void incrementProgress(PlayerProfile profile, QuestProgress progress, Quest quest) {
        if (!profile.isRegisteredInGuild() || progress.getCurrentAmount() >= quest.requiredAmount()) return;
        int next = Math.min(quest.requiredAmount(), progress.getCurrentAmount() + 1);
        progress.setCurrentAmount(next);
        Player player = Bukkit.getPlayer(profile.getUuid());
        if (player != null && player.isOnline()) {
            player.sendActionBar(lang.get("quest.progress", "current", String.valueOf(next), "required", String.valueOf(quest.requiredAmount())));
        }
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
            if (!member.getWorld().equals(referenceLocation.getWorld())) continue;
            if (member.getLocation().distance(referenceLocation) > partyShareRange) continue;
            profileManager.getProfile(memberUuid).filter(PlayerProfile::isRegisteredInGuild).ifPresent(action);
        }
    }

    private boolean isRegistered(UUID uuid) {
        return profileManager.getProfile(uuid).map(PlayerProfile::isRegisteredInGuild).orElse(false);
    }

    private boolean isExactBlock(Location a, Location b) {
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private boolean isWithinRadius(Location a, Location b, double radius) {
        return a.getWorld().equals(b.getWorld()) && a.distance(b) <= radius;
    }

    public QuestRepository getRepository() { return questRepository; }
}
