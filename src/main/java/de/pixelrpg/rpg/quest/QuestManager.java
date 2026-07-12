// src/main/java/de/pixelrpg/rpg/quest/QuestManager.java
package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestManager {

    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final de.pixelrpg.rpg.api.GuildAPI guildAPI;
    private final GlobalEventState globalEventState;
    private final double partyShareRange;

    private final Map<UUID, Map<String, Long>> questTimers = new ConcurrentHashMap<>();

    public QuestManager(Plugin plugin, QuestRepository questRepository, PlayerProfileManager profileManager,
                         de.pixelrpg.rpg.api.GuildAPI guildAPI, GlobalEventState globalEventState, double partyShareRange) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.guildAPI = guildAPI;
        this.globalEventState = globalEventState;
        this.partyShareRange = partyShareRange;
    }

    public void startTimerCheckTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Map<String, Long>> playerEntry : new HashMap<>(questTimers).entrySet()) {
                UUID uuid = playerEntry.getKey();
                for (Map.Entry<String, Long> questEntry : new HashMap<>(playerEntry.getValue()).entrySet()) {
                    if (now < questEntry.getValue()) {
                        continue;
                    }
                    String questId = questEntry.getKey();
                    playerEntry.getValue().remove(questId);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        profileManager.getProfile(uuid).ifPresent(profile -> profile.removeActiveQuest(questId));
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("Your quest contract has expired!", NamedTextColor.RED));
                        }
                    });
                }
            }
        }, 20L, 20L);
    }

    public boolean canAccept(PlayerProfile profile, Quest quest) {
        if (!profile.isRegisteredInGuild()) {
            return false;
        }
        if (profile.hasCompletedQuest(quest.id())) {
            return false;
        }
        return profile.getRank().isAtLeast(quest.requiredRank());
    }

    public boolean acceptQuest(Player player, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !canAccept(profile, quest) || profile.hasActiveQuest(quest.id())) {
            return false;
        }

        long expiry = quest.hasTimeLimit()
                ? System.currentTimeMillis() + (quest.durationMinutes() * 60_000L)
                : 0L;

        profile.startQuest(new QuestProgress(quest.id(), 0, expiry));

        if (quest.hasTimeLimit()) {
            questTimers.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(quest.id(), expiry);
            player.sendMessage(Component.text("Time limit: " + quest.durationMinutes() + " minutes!", NamedTextColor.YELLOW));
        }

        player.sendMessage(Component.text("Quest accepted: ", NamedTextColor.GREEN)
                .append(Component.text(quest.title(), NamedTextColor.YELLOW)));
        return true;
    }

    public boolean abandonQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.hasActiveQuest(questId)) {
            return false;
        }
        profile.removeActiveQuest(questId);
        Map<String, Long> timers = questTimers.get(player.getUniqueId());
        if (timers != null) {
            timers.remove(questId);
        }
        player.sendMessage(Component.text("Quest abandoned.", NamedTextColor.GOLD));
        return true;
    }

    public boolean completeQuest(Player player, String questId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        Quest quest = questRepository.getQuest(questId);
        if (profile == null || quest == null || !profile.hasActiveQuest(questId)) {
            return false;
        }

        QuestProgress progress = profile.getActiveQuests().get(questId);
        if (progress.getCurrentAmount() < quest.requiredAmount()) {
            player.sendMessage(Component.text("Requirements not met yet.", NamedTextColor.RED));
            return false;
        }

        profile.removeActiveQuest(questId);
        profile.markQuestCompleted(questId);

        Map<String, Long> timers = questTimers.get(player.getUniqueId());
        if (timers != null) {
            timers.remove(questId);
        }

        if (quest.rewardMoney() > 0) {
            profile.addMoney(quest.rewardMoney());
        }
        if (quest.rewardExp() > 0) {
            profileManager.addExperience(player.getUniqueId(), quest.rewardExp());
        }
        for (String materialName : quest.rewardItemMaterials()) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                player.getInventory().addItem(new ItemStack(material));
            } catch (IllegalArgumentException ignored) {
            }
        }

        player.sendMessage(Component.text("Quest completed: ", NamedTextColor.GREEN)
                .append(Component.text(quest.title(), NamedTextColor.YELLOW)));

        Title title = Title.title(
                Component.text("Quest Complete!", NamedTextColor.GOLD),
                Component.text(quest.title(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(300))
        );
        player.showTitle(title);

        Bukkit.getPluginManager().callEvent(new QuestCompletedEvent(player, questId));
        return true;
    }

    public void progressHuntQuests(Player killer, String mobTypeKey) {
        propagateToParty(killer, profile -> applyHuntProgress(profile, mobTypeKey), killer.getLocation());
    }

    private void applyHuntProgress(PlayerProfile profile, String mobTypeKey) {
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.HUNT) {
                continue;
            }
            if (!quest.targetKey().equalsIgnoreCase(mobTypeKey)) {
                continue;
            }
            incrementProgress(profile, entry.getValue(), quest);
        }
    }

    public void checkInventoryQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.COLLECT) {
                continue;
            }
            try {
                Material material = Material.valueOf(quest.targetKey().toUpperCase());
                int amount = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) {
                        amount += item.getAmount();
                    }
                }
                entry.getValue().setCurrentAmount(Math.min(quest.requiredAmount(), amount));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

// src/main/java/de/pixelrpg/rpg/quest/QuestManager.java (Ausschnitt: checkReachLocationQuests ersetzen für exakte Koordinaten)
    public void checkReachLocationQuests(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.REACH_LOCATION || quest.reachLocation() == null) {
                continue;
            }
            if (entry.getValue().getCurrentAmount() >= quest.requiredAmount()) {
                continue;
            }
            boolean reached = quest.reachRadius() <= 0.0
                    ? isExactBlock(player.getLocation(), quest.reachLocation())
                    : isWithinRadius(player.getLocation(), quest.reachLocation(), quest.reachRadius());
            if (reached) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                player.sendMessage(Component.text("Location reached: ", NamedTextColor.GREEN)
                        .append(Component.text(quest.title(), NamedTextColor.YELLOW)));
            }
        }
    }

    private boolean isExactBlock(Location a, Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    public void progressEscortQuest(Player player, String escortKey) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }
        for (var entry : new HashMap<>(profile.getActiveQuests()).entrySet()) {
            Quest quest = questRepository.getQuest(entry.getKey());
            if (quest == null || quest.type() != QuestType.ESCORT) {
                continue;
            }
            if (!quest.targetKey().equalsIgnoreCase(escortKey)) {
                continue;
            }
            if (quest.escortDestination() != null
                    && isWithinRadius(player.getLocation(), quest.escortDestination(), 3.0)) {
                entry.getValue().setCurrentAmount(quest.requiredAmount());
                player.sendMessage(Component.text("Escort target delivered!", NamedTextColor.GREEN));
            }
        }
    }

    public void progressGlobalEvent(String targetKey) {
        for (Quest quest : questRepository.getQuestsByType(QuestType.GLOBAL_EVENT)) {
            if (!quest.targetKey().equalsIgnoreCase(targetKey)) {
                continue;
            }
            int current = globalEventState.getProgress(quest.id());
            if (current >= quest.requiredAmount()) {
                continue;
            }
            int updated = globalEventState.addProgress(quest.id(), 1);
            if (updated >= quest.requiredAmount()) {
                completeGlobalEvent(quest);
            }
        }
    }

    private void completeGlobalEvent(Quest quest) {
        Component announcement = Component.text("[CALAMITY CONQUERED] ", NamedTextColor.DARK_RED)
                .append(Component.text(quest.title() + " has been defeated by the server!", NamedTextColor.GOLD));

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(announcement);
            online.showTitle(Title.title(
                    Component.text("CALAMITY CONQUERED", NamedTextColor.DARK_RED),
                    Component.text(quest.title(), NamedTextColor.GOLD),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
            ));
            if (guildAPI.isRegistered(online.getUniqueId())) {
                profileManager.addExperience(online.getUniqueId(), quest.rewardExp());
            }
        }
    }

    public int getGlobalEventProgress(String questId) {
        return globalEventState.getProgress(questId);
    }

    private void incrementProgress(PlayerProfile profile, QuestProgress progress, Quest quest) {
        if (progress.getCurrentAmount() >= quest.requiredAmount()) {
            return;
        }
        int next = Math.min(quest.requiredAmount(), progress.getCurrentAmount() + 1);
        progress.setCurrentAmount(next);

        Player player = Bukkit.getPlayer(profile.getUuid());
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.text(quest.title() + ": " + next + "/" + quest.requiredAmount(), NamedTextColor.YELLOW));
        }
    }

    private void propagateToParty(Player source, java.util.function.Consumer<PlayerProfile> action, Location referenceLocation) {
        de.pixelrpg.rpg.api.PartyAPI partyAPI = Bukkit.getServicesManager().load(de.pixelrpg.rpg.api.PartyAPI.class);

        if (partyAPI == null || !partyAPI.isInParty(source.getUniqueId())) {
            profileManager.getProfile(source.getUniqueId()).ifPresent(action);
            return;
        }

        Set<UUID> members = partyAPI.getPartyMembers(source.getUniqueId());
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member == null || !member.isOnline()) {
                continue;
            }
            if (!member.getWorld().equals(referenceLocation.getWorld())) {
                continue;
            }
            if (member.getLocation().distance(referenceLocation) > partyShareRange) {
                continue;
            }
            profileManager.getProfile(memberUuid).ifPresent(action);
        }
    }

    private boolean isWithinRadius(Location a, Location b, double radius) {
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return a.distance(b) <= radius;
    }

    public QuestRepository getRepository() {
        return questRepository;
    }
}