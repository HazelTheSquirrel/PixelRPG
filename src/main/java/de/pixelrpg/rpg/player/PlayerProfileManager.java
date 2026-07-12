// src/main/java/de/pixelrpg/rpg/player/PlayerProfileManager.java
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.TitleAPI;
import de.pixelrpg.rpg.api.events.PlayerClassChangeEvent;
import de.pixelrpg.rpg.api.events.PlayerJoinGuildEvent;
import de.pixelrpg.rpg.api.events.PlayerLeaveGuildEvent;
import de.pixelrpg.rpg.api.events.PlayerRankUpEvent;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.leaderboard.LeaderboardEntry;
import de.pixelrpg.rpg.leaderboard.LeaderboardType;
import de.pixelrpg.rpg.storage.DatabaseManager;
import de.pixelrpg.rpg.storage.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerProfileManager implements GuildAPI, EconomyAPI, TitleAPI {

    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> loadingCache = new ConcurrentHashMap<>();

    private PlayerProfileRepository repository;
    private DatabaseManager databaseManager;
    private StorageType storageType;
    private double respecCost = 300.0;
    private Rank respecMinRank = Rank.B;

    public PlayerProfileManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize(FileConfiguration config) {
        this.storageType = StorageType.fromString(config.getString("storage.type", "YAML"));
        this.respecCost = config.getDouble("classes.respec-cost", 300.0);
        this.respecMinRank = parseRank(config.getString("classes.respec-min-rank", "B"));

        try {
            if (storageType == StorageType.MYSQL) {
                databaseManager = new DatabaseManager();
                databaseManager.connect(config);
                repository = new MySQLPlayerProfileRepository(databaseManager);
            } else {
                repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            }
            repository.init();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize player storage, falling back to YAML.", e);
            repository = new YamlPlayerProfileRepository(plugin.getDataFolder());
            try {
                repository.init();
            } catch (Exception ignored) {
            }
        }

        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(EconomyAPI.class, this, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(TitleAPI.class, this, plugin, ServicePriority.Normal);
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.B;
        }
    }

    public void shutdown() {
        for (PlayerProfile profile : activeProfiles.values()) {
            persistSync(profile);
        }
        if (repository != null) {
            repository.shutdown();
        }
    }

    public void loadForPreLogin(UUID uuid) {
        PlayerProfile profile;
        try {
            profile = repository.load(uuid).orElseGet(() -> new PlayerProfile(uuid));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load profile for " + uuid, e);
            profile = new PlayerProfile(uuid);
        }
        loadingCache.put(uuid, profile);
    }

    public void activateOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = loadingCache.remove(uuid);
        if (profile == null) {
            profile = new PlayerProfile(uuid);
        }
        activeProfiles.put(uuid, profile);
    }

    public void deactivateOnQuit(UUID uuid) {
        PlayerProfile profile = activeProfiles.remove(uuid);
        if (profile != null) {
            persistAsync(profile);
        }
    }

    public Optional<PlayerProfile> getProfile(UUID uuid) {
        return Optional.ofNullable(activeProfiles.get(uuid));
    }

    public void registerToGuild(Player player) {
        PlayerProfile profile = activeProfiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
        if (profile.isRegisteredInGuild()) {
            return;
        }
        profile.setRegisteredInGuild(true);
        persistAsync(profile);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Bukkit.getPluginManager().callEvent(new PlayerJoinGuildEvent(player));
    }

    public void leaveGuild(Player player) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) {
            return;
        }
        profile.resetProgress();
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerLeaveGuildEvent(player));
    }

    public boolean selectClass(Player player, PlayerClass newClass) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) {
            return false;
        }
        if (profile.getPlayerClass() != PlayerClass.NONE) {
            return false;
        }
        if (!profile.getRank().isAtLeast(Rank.C)) {
            return false;
        }
        if (newClass == PlayerClass.NONE) {
            return false;
        }

        PlayerClass oldClass = profile.getPlayerClass();
        profile.setPlayerClass(newClass);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerClassChangeEvent(player, oldClass, newClass));
        return true;
    }

    public enum RespecResult {
        SUCCESS,
        NOT_REGISTERED,
        RANK_TOO_LOW,
        INSUFFICIENT_FUNDS,
        SAME_CLASS
    }

    public RespecResult respecClass(Player player, PlayerClass newClass) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) {
            return RespecResult.NOT_REGISTERED;
        }
        if (profile.getPlayerClass() == newClass) {
            return RespecResult.SAME_CLASS;
        }
        if (!profile.getRank().isAtLeast(respecMinRank)) {
            return RespecResult.RANK_TOO_LOW;
        }
        if (!profile.removeMoney(respecCost)) {
            return RespecResult.INSUFFICIENT_FUNDS;
        }

        PlayerClass oldClass = profile.getPlayerClass();
        profile.setPlayerClass(newClass);
        persistAsync(profile);
        Bukkit.getPluginManager().callEvent(new PlayerClassChangeEvent(player, oldClass, newClass));
        return RespecResult.SUCCESS;
    }

    public double getRespecCost() {
        return respecCost;
    }

    public Rank getRespecMinRank() {
        return respecMinRank;
    }

    public AttributePurchaseResult purchaseAttribute(Player player, PlayerAttribute attribute) {
        PlayerProfile profile = activeProfiles.get(player.getUniqueId());
        if (profile == null || !profile.isRegisteredInGuild()) {
            return AttributePurchaseResult.NOT_REGISTERED;
        }

        int current = profile.getAttributePoints(attribute);
        if (current >= attribute.getMaxPoints()) {
            return AttributePurchaseResult.MAX_REACHED;
        }

        Rank required = AttributeConfig.rankRequirementForPoint(attribute, current);
        if (!profile.getRank().isAtLeast(required)) {
            return AttributePurchaseResult.RANK_TOO_LOW;
        }

        double cost = AttributeConfig.costFor(attribute, current, profile.getPlayerClass());
        if (!profile.removeMoney(cost)) {
            return AttributePurchaseResult.INSUFFICIENT_FUNDS;
        }

        profile.addAttributePoint(attribute);
        persistAsync(profile);
        return AttributePurchaseResult.SUCCESS;
    }

    public void unlockWaypoint(UUID uuid, String waypointId) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null) {
            return;
        }
        profile.unlockWaypoint(waypointId);
        persistAsync(profile);
    }

    public void saveProfileAsync(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile != null) {
            persistAsync(profile);
        }
    }

    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type, int limit) throws Exception {
        return switch (type) {
            case EXPERIENCE -> repository.getTopByExperience(limit);
            case MONEY -> repository.getTopByMoney(limit);
            default -> repository.getTopByStatistic(type.getStatisticType().name(), limit);
        };
    }

    private void persistAsync(PlayerProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persistSync(profile));
    }

    private void persistSync(PlayerProfile profile) {
        if (!profile.isDirty()) {
            return;
        }
        try {
            repository.save(profile);
            profile.markClean();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save profile for " + profile.getUuid(), e);
        }
    }

    @Override
    public boolean isRegistered(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null && profile.isRegisteredInGuild();
    }

    @Override
    public Rank getRank(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getRank() : Rank.F;
    }

    @Override
    public PlayerClass getPlayerClass(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getPlayerClass() : PlayerClass.NONE;
    }

    @Override
    public boolean hasSelectedClass(UUID uuid) {
        return getPlayerClass(uuid) != PlayerClass.NONE;
    }

    @Override
    public long getExperience(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getExperience() : 0L;
    }

    @Override
    public void addExperience(UUID uuid, long amount) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.isRegisteredInGuild()) {
            return;
        }
        Rank before = profile.getRank();
        profile.addExperience(amount);
        Rank after = profile.getRank();
        persistAsync(profile);
        if (before != after) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.getPluginManager().callEvent(new PlayerRankUpEvent(player, before, after));
            }
        }
    }

    @Override
    public int getAttributePoints(UUID uuid, PlayerAttribute attribute) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getAttributePoints(attribute) : 0;
    }

    @Override
    public double getBalance(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getMoney() : 0.0;
    }

    @Override
    public void deposit(UUID uuid, double amount) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.isRegisteredInGuild()) {
            return;
        }
        profile.addMoney(amount);
        persistAsync(profile);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null || !profile.isRegisteredInGuild()) {
            return false;
        }
        boolean success = profile.removeMoney(amount);
        if (success) {
            persistAsync(profile);
        }
        return success;
    }

    @Override
    public String getSelectedTitle(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getSelectedTitle() : null;
    }

    @Override
    public Set<String> getUnlockedTitles(UUID uuid) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null ? profile.getUnlockedTitles() : Set.of();
    }

    @Override
    public boolean hasTitle(UUID uuid, String title) {
        PlayerProfile profile = activeProfiles.get(uuid);
        return profile != null && profile.hasTitle(title);
    }

    @Override
    public void unlockTitle(UUID uuid, String title) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null) {
            return;
        }
        profile.unlockTitle(title);
        persistAsync(profile);
    }

    @Override
    public boolean selectTitle(UUID uuid, String title) {
        PlayerProfile profile = activeProfiles.get(uuid);
        if (profile == null) {
            return false;
        }
        boolean success = profile.setSelectedTitle(title);
        if (success) {
            persistAsync(profile);
        }
        return success;
    }

    public enum AttributePurchaseResult {
        SUCCESS,
        NOT_REGISTERED,
        MAX_REACHED,
        RANK_TOO_LOW,
        INSUFFICIENT_FUNDS
    }
}