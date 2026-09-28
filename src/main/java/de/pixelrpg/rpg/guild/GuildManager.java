package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.command.GuildAcceptCommand;
import de.pixelrpg.rpg.command.GuildInfoCommand;
import de.pixelrpg.rpg.command.GuildInviteCommand;
import de.pixelrpg.rpg.command.GuildLeaveCommand;
import de.pixelrpg.rpg.command.PaperBasicCommandAdapter;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.economy.Money;
import de.pixelrpg.rpg.scoreboard.ScoreboardService;
import de.pixelrpg.rpg.region.PixelRegion;
import de.pixelrpg.rpg.region.RegionType;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Owns guild creation, membership, invitations and persistent guild metadata. */
public final class GuildManager implements GuildAPI {
    public static final int MAX_MEMBERS = Guild.MAX_MEMBERS;
    public static final int CREATION_COST_GOLD = Guild.CREATION_COST_GOLD;
    public static final int MIN_CREATION_LEVEL = Guild.MIN_CREATION_LEVEL;
    private static volatile GuildManager instance;
    private final PixelRPGPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File file;
    private final Map<UUID, GuildData> guilds = new HashMap<>();
    private final Map<UUID, UUID> memberGuilds = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private final ExecutorService persistenceExecutor;
    private boolean saveWorkerScheduled;
    private long stateRevision;
    private volatile boolean shuttingDown;

    public GuildManager(PixelRPGPlugin plugin, PlayerProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "guilds.yml");
        persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-GuildIO");
            thread.setDaemon(true);
            return thread;
        });
        load();
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        registerCommands();
        instance = this;
    }

    public static GuildManager getInstance(PixelRPGPlugin plugin, PlayerProfileManager profiles) {
        GuildManager current = instance;
        if (current != null) return current;
        synchronized (GuildManager.class) {
            if (instance == null) instance = new GuildManager(plugin, profiles);
            return instance;
        }
    }

    public static GuildManager getInstance() {
        return Objects.requireNonNull(instance, "GuildManager not initialized");
    }

    private void registerCommands() {
        GuildInviteCommand invite = new GuildInviteCommand(this);
        GuildAcceptCommand accept = new GuildAcceptCommand(this);
        GuildLeaveCommand leave = new GuildLeaveCommand(this);
        GuildInfoCommand info = new GuildInfoCommand(this);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("gildeneinladen", new PaperBasicCommandAdapter("gildeneinladen", invite, invite, "rpg.member"));
            event.registrar().register("gildeannehmen", new PaperBasicCommandAdapter("gildeannehmen", accept, null, "rpg.member"));
            event.registrar().register("gildeverlassen", new PaperBasicCommandAdapter("gildeverlassen", leave, null, "rpg.member"));
            event.registrar().register("gildeinfo", new PaperBasicCommandAdapter("gildeinfo", info, null, "rpg.member"));
        });
    }

    public synchronized Result createGuild(Player player, String name) {
        if (shuttingDown || player == null || !profiles.isRegistered(player.getUniqueId())) return Result.NOT_REGISTERED;
        if (getGuild(player.getUniqueId()).isPresent()) return Result.ALREADY_IN_GUILD;
        if (profiles.getProfile(player.getUniqueId()).map(p -> p.getLevel() < MIN_CREATION_LEVEL).orElse(true)) return Result.LEVEL_TOO_LOW;
        if (!isValidName(name)) return Result.INVALID_NAME;
        String normalized = normalize(name);
        if (guilds.values().stream().anyMatch(g -> g.name().equalsIgnoreCase(normalized))) return Result.NAME_TAKEN;
        var profile = profiles.getProfile(player.getUniqueId()).orElseThrow();
        if (!profile.removeMoney(CREATION_COST_GOLD)) return Result.INSUFFICIENT_GOLD;
        UUID id = UUID.randomUUID();
        GuildData guild = new GuildData(id, normalized, player.getUniqueId(), new LinkedHashSet<>(Set.of(player.getUniqueId())));
        guild.cityUpgradeCooldownUntil = System.currentTimeMillis() + 24L * 3_600_000L;
        guilds.put(id, guild);
        memberGuilds.put(player.getUniqueId(), id);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
        GuildTerritoryManager territoryManager = plugin.getGuildTerritoryManager();
        if (territoryManager != null) territoryManager.grantInitialMarkers(player, id);
        wakeScoreboards(Set.of(player.getUniqueId()));
        return Result.SUCCESS;
    }

    public synchronized Result invite(Player leader, Player target) {
        if (shuttingDown || leader == null || target == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.members().size() >= MAX_MEMBERS) return Result.GUILD_FULL;
        if (getGuild(target.getUniqueId()).isPresent()) return Result.TARGET_ALREADY_IN_GUILD;
        invitations.put(target.getUniqueId(), new Invitation(guild.id(), System.currentTimeMillis()));
        target.sendMessage(Component.text("Du wurdest in die Gilde „" + guild.name() + "“ eingeladen. Nutze /gildeannehmen, um beizutreten.", NamedTextColor.GOLD));
        leader.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN));
        return Result.SUCCESS;
    }

    public synchronized Result acceptInvitation(Player player) {
        if (shuttingDown || player == null) return Result.NO_INVITATION;
        Invitation invitation = invitations.remove(player.getUniqueId());
        if (invitation == null) return Result.NO_INVITATION;
        GuildData guild = guilds.get(invitation.guildId());
        if (guild == null) return Result.GUILD_NOT_FOUND;
        if (getGuild(player.getUniqueId()).isPresent()) return Result.ALREADY_IN_GUILD;
        if (guild.members().size() >= MAX_MEMBERS) return Result.GUILD_FULL;
        guild.members().add(player.getUniqueId());
        memberGuilds.put(player.getUniqueId(), guild.id());
        syncCityMember(guild, player.getUniqueId());
        save();
        wakeScoreboards(Set.of(player.getUniqueId()));
        return Result.SUCCESS;
    }

    public synchronized Result leave(Player player) {
        if (shuttingDown || player == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (guild.leaderId().equals(player.getUniqueId())) return Result.LEADER_CANNOT_LEAVE;
        guild.members().remove(player.getUniqueId());
        if (player.getUniqueId().equals(guild.deputyId)) guild.deputyId = null;
        memberGuilds.remove(player.getUniqueId());
        syncCityMember(guild, player.getUniqueId());
        save();
        wakeScoreboards(Set.of(player.getUniqueId()));
        return Result.SUCCESS;
    }

    public synchronized Optional<Guild> getGuild(UUID playerId) {
        if (playerId == null) return Optional.empty();
        UUID guildId = memberGuilds.get(playerId);
        if (guildId == null) return Optional.empty();
        GuildData data = guilds.get(guildId);
        return data == null ? Optional.empty() : Optional.of(data.snapshot());
    }

    public synchronized Optional<Guild> getInvitationGuild(UUID playerId) {
        Invitation invitation = invitations.get(playerId);
        if (invitation == null) return Optional.empty();
        GuildData guild = guilds.get(invitation.guildId());
        return guild == null ? Optional.empty() : Optional.of(guild.snapshot());
    }

    public synchronized boolean hasInvitation(UUID playerId) {
        return getInvitationGuild(playerId).isPresent();
    }

    public synchronized void declineInvitation(UUID playerId) {
        invitations.remove(playerId);
    }

    public synchronized void advanceCityLevel(UUID guildId, int newLevel, long cooldownUntil) {
        GuildData guild = guilds.get(guildId); if (guild == null) return;
        guild.cityLevel = Math.clamp(newLevel, 1, Guild.MAX_CITY_LEVEL); guild.cityUpgradeCooldownUntil = Math.max(0L, cooldownUntil); save();
    }

    public synchronized void setCityUpgradeCooldown(UUID guildId, long cooldownUntil) { GuildData g=guilds.get(guildId); if(g!=null){g.cityUpgradeCooldownUntil=Math.max(0L,cooldownUntil);save();} }

    public synchronized Result requestCombatMode(Player requester, KingdomCombatMode targetMode) {
        if (requester == null || targetMode == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(requester.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.canManageTerritory(requester.getUniqueId())) return Result.NOT_LEADER;
        if (guild.combatMode == targetMode) return Result.COMBAT_MODE_ALREADY_ACTIVE;
        long now = System.currentTimeMillis();
        if (guild.combatModeChangeAt > now) return Result.COMBAT_MODE_PENDING;
        if (guild.combatModeCooldownUntil > now) return Result.COMBAT_MODE_COOLDOWN;
        long delay = Math.max(1L, plugin.getConfig().getLong("kingdom.combat-mode.switch-delay-seconds", 300L));
        long cooldown = Math.max(1L, plugin.getConfig().getLong("kingdom.combat-mode.cooldown-seconds", 1800L));
        guild.combatModeChangeAt = now + delay * 1000L;
        save();
        notifyGuild(guild, Component.text("Königreich „" + guild.name + "“ wechselt in " + delay + " Sekunden auf " + targetMode.name() + ".", NamedTextColor.YELLOW));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (GuildManager.this) {
                GuildData current = guilds.get(guild.id);
                if (current == null) return;
                long currentNow = System.currentTimeMillis();
                if (currentNow < current.combatModeChangeAt) return;
                var combat = plugin.getCombatStateService();
                boolean blocked = combat != null && current.members().stream().anyMatch(combat::isInCombat);
                if (blocked) {
                    current.combatModeChangeAt = 0L;
                    save();
                    notifyGuild(current, Component.text("Der PvP/PvE-Wechsel wurde abgebrochen, weil sich ein Mitglied noch im aktiven Kampf befindet.", NamedTextColor.RED));
                    return;
                }
                current.combatMode = targetMode;
                current.combatModeChangeAt = 0L;
                current.combatModeCooldownUntil = currentNow + cooldown * 1000L;
                save();
                notifyGuild(current, Component.text("Königreich „" + current.name + "“ ist jetzt " + targetMode.name() + ".", NamedTextColor.GREEN));
            }
        }, delay * 20L);
        return Result.SUCCESS;
    }

    public synchronized KingdomCombatMode effectiveCombatMode(UUID guildId) {
        GuildData guild = guilds.get(guildId);
        return guild == null ? KingdomCombatMode.PVE : guild.combatMode;
    }

    private void notifyGuild(GuildData guild, Component message) {
        guild.members().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(player -> player.sendMessage(message));
    }

    public synchronized void setCombatMode(UUID guildId, KingdomCombatMode mode, long changeAt, long cooldownUntil) { GuildData g=guilds.get(guildId); if(g!=null){g.combatMode=Objects.requireNonNull(mode);g.combatModeChangeAt=Math.max(0L,changeAt);g.combatModeCooldownUntil=Math.max(0L,cooldownUntil);save();} }
    public synchronized Optional<Guild> getGuildById(UUID guildId) {
        GuildData guild = guilds.get(guildId);
        return guild == null ? Optional.empty() : Optional.of(guild.snapshot());
    }

    public synchronized Optional<UUID> getDeputyId(UUID playerId) {
        GuildData guild = guilds.get(memberGuilds.get(playerId));
        return guild == null ? Optional.empty() : Optional.ofNullable(guild.deputyId);
    }

    public synchronized Result appointDeputy(Player leader, Player target) {
        return target == null ? Result.TARGET_NOT_MEMBER : appointDeputy(leader, target.getUniqueId());
    }

    public synchronized Result appointDeputy(Player leader, UUID targetId) {
        if (shuttingDown || leader == null || targetId == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (!guild.members().contains(targetId)) return Result.TARGET_NOT_MEMBER;
        if (targetId.equals(guild.leaderId())) return Result.INVALID_DEPUTY;
        guild.deputyId = targetId;
        save();
        wakeScoreboards(Set.of(leader.getUniqueId(), targetId));
        return Result.SUCCESS;
    }

    public synchronized Result removeDeputy(Player leader) {
        if (shuttingDown || leader == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.deputyId == null) return Result.NO_DEPUTY;
        UUID previous = guild.deputyId;
        guild.deputyId = null;
        save();
        wakeScoreboards(Set.of(leader.getUniqueId(), previous));
        return Result.SUCCESS;
    }

    public synchronized Optional<Guild> getGuildByName(String name) {
        return guilds.values().stream().filter(g -> g.name().equalsIgnoreCase(name)).findFirst().map(GuildData::snapshot);
    }

    public synchronized double getTreasury(UUID playerId) {
        GuildData guild = guilds.get(memberGuilds.get(playerId));
        return guild == null ? 0.0D : Money.toMajor(guild.treasuryMinorUnits());
    }

    public synchronized boolean depositToTreasury(Player player, double amount) {
        if (shuttingDown || player == null || !Double.isFinite(amount) || amount <= 0.0D) return false;
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null) return false;
        long minor = Money.fromMajor(amount);
        if (minor <= 0L) return false;
        var profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.removeMoney(amount)) return false;
        guild.addTreasury(minor);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
        return true;
    }

    public synchronized boolean chargeTreasury(UUID guildId, double amount) {
        if (shuttingDown || guildId == null || !Double.isFinite(amount) || amount <= 0.0D) return false;
        GuildData guild = guilds.get(guildId);
        if (guild == null) return false;
        long minor = Money.fromMajor(amount);
        if (minor <= 0L || guild.treasuryMinorUnits() < minor) return false;
        guild.removeTreasury(minor);
        save();
        return true;
    }

    public synchronized boolean withdrawFromTreasury(Player player, double amount) {
        if (shuttingDown || player == null || !Double.isFinite(amount) || amount <= 0.0D) return false;
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null || !guild.leaderId().equals(player.getUniqueId())) return false;
        long minor = Money.fromMajor(amount);
        if (minor <= 0L || guild.treasuryMinorUnits() < minor) return false;
        guild.removeTreasury(minor);
        var profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            guild.addTreasury(minor);
            return false;
        }
        profile.addMoney(amount);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
        return true;
    }

    public synchronized Result claimCity(Player leader, String selector) {
        if (shuttingDown || leader == null || selector == null || selector.isBlank()) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.cityRegionId != null) return Result.CITY_ALREADY_CLAIMED;
        var regionManager = plugin.getRegionManager();
        if (regionManager == null) return Result.CITY_REGION_NOT_FOUND;
        PixelRegion region = resolveRegion(regionManager, selector);
        if (region == null || region.isGlobal()) return Result.CITY_REGION_NOT_FOUND;
        if (region.type() != RegionType.GUILD_CITY) return Result.NOT_GUILD_CITY;
        if (region.ownerId() != null && !region.ownerId().equals(leader.getUniqueId())) return Result.CITY_OWNED;
        if (guilds.values().stream().anyMatch(other -> region.id().equals(other.cityRegionId))) return Result.CITY_OWNED;
        region.setOwner(leader.getUniqueId());
        guild.members().forEach(region::addMember);
        guild.cityRegionId = region.id();
        regionManager.save();
        save();
        return Result.SUCCESS;
    }

    public synchronized Result releaseCity(Player leader) {
        if (shuttingDown || leader == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.cityRegionId == null) return Result.NO_CITY;
        releaseCityInternal(guild);
        save();
        return Result.SUCCESS;
    }

    public synchronized Optional<UUID> getCityRegionId(UUID playerId) {
        GuildData guild = guilds.get(memberGuilds.get(playerId));
        return guild == null ? Optional.empty() : Optional.ofNullable(guild.cityRegionId);
    }

    private PixelRegion resolveRegion(de.pixelrpg.rpg.region.RegionManager regions, String selector) {
        try {
            UUID id = UUID.fromString(selector);
            return regions.get(id).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return regions.all().stream().filter(region -> region.name().equalsIgnoreCase(selector)).findFirst().orElse(null);
        }
    }

    private void syncCityMember(GuildData guild, UUID playerId) {
        if (guild.cityRegionId == null) return;
        var regions = plugin.getRegionManager();
        if (regions == null) return;
        PixelRegion region = regions.get(guild.cityRegionId).orElse(null);
        if (region == null) return;
        if (guild.members().contains(playerId)) region.addMember(playerId);
        else region.removeMember(playerId);
        regions.save();
    }

    private void releaseCityInternal(GuildData guild) {
        if (guild.cityRegionId == null) return;
        var regions = plugin.getRegionManager();
        if (regions != null) {
            PixelRegion region = regions.get(guild.cityRegionId).orElse(null);
            if (region != null) {
                region.clearOwner();
                for (UUID member : guild.members()) region.removeMember(member);
                regions.save();
            }
        }
        guild.cityRegionId = null;
    }

    public synchronized java.util.Collection<Guild> allGuilds() { return guilds.values().stream().map(GuildData::snapshot).toList(); }

    public synchronized Set<UUID> getMembers(UUID guildId) {
        GuildData guild = guilds.get(guildId);
        return guild == null ? Set.of() : Set.copyOf(guild.members());
    }

    public synchronized boolean isMember(UUID guildId, UUID playerId) {
        GuildData guild = guilds.get(guildId);
        return guild != null && guild.members().contains(playerId);
    }

    public synchronized Result disband(Player player) {
        if (shuttingDown || player == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(player.getUniqueId())) return Result.NOT_LEADER;
        if (guild.treasuryMinorUnits() > 0L) return Result.TREASURY_NOT_EMPTY;
        releaseCityInternal(guild);
        GuildTerritoryManager territoryManager = plugin.getGuildTerritoryManager();
        if (territoryManager != null) territoryManager.removeGuildTerritory(guild.id());
        Set<UUID> changedMembers = Set.copyOf(guild.members());
        guild.members().forEach(memberGuilds::remove);
        guilds.remove(guild.id());
        save();
        wakeScoreboards(changedMembers);
        return Result.SUCCESS;
    }

    @Override
    public synchronized boolean isRegistered(UUID uuid) { return getGuild(uuid).isPresent(); }

    @Override
    public synchronized int getLevel(UUID uuid) { return getGuild(uuid).isPresent() ? 1 : 0; }

    @Override
    public long getExperience(UUID uuid) { return 0L; }

    @Override
    public void addExperience(UUID uuid, long amount) { }

    private void wakeScoreboards(Set<UUID> changedPlayers) {
        if (changedPlayers.isEmpty()) return;
        ScoreboardService scoreboardService = PixelRPGPlugin.getInstance().getScoreboardService();
        for (UUID playerId : changedPlayers) {
            if (scoreboardService != null) scoreboardService.markGuildEntryDirty(playerId);
            profiles.notifyExternalStateChange(playerId);
        }
    }

    private boolean isValidName(String name) {
        return name != null && name.trim().length() >= 3 && name.trim().length() <= 24 && name.trim().matches("[A-Za-z0-9ÄÖÜäöüß _-]+");
    }

    private String normalize(String name) { return name.trim().replaceAll("\\s+", " "); }

    private synchronized void load() {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("guilds");
        if (section == null) return;
        for (String idText : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idText);
                String name = section.getString(idText + ".name");
                UUID leader = UUID.fromString(Objects.requireNonNull(section.getString(idText + ".leader")));
                LinkedHashSet<UUID> members = new LinkedHashSet<>();
                for (String text : section.getStringList(idText + ".members")) members.add(UUID.fromString(text));
                if (members.isEmpty()) members.add(leader);
                GuildData guild = new GuildData(id, name, leader, members);
                String deputyText = section.getString(idText + ".deputy");
                if (deputyText != null) {
                    try { guild.deputyId = UUID.fromString(deputyText); } catch (IllegalArgumentException ignored) { guild.deputyId = null; }
                }
                if (guild.deputyId != null && (!members.contains(guild.deputyId) || guild.deputyId.equals(leader))) guild.deputyId = null;
                guild.treasuryMinorUnits = Money.fromMajor(section.getDouble(idText + ".treasury", 0.0D));
                guild.cityLevel = Math.clamp(section.getInt(idText + ".city-level", 1), 1, Guild.MAX_CITY_LEVEL);
                guild.cityUpgradeCooldownUntil = Math.max(0L, section.getLong(idText + ".city-upgrade-cooldown-until", 0L));
                try { guild.combatMode = KingdomCombatMode.valueOf(section.getString(idText + ".combat-mode", "PVE").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { guild.combatMode = KingdomCombatMode.PVE; }
                guild.combatModeChangeAt = Math.max(0L, section.getLong(idText + ".combat-mode-change-at", 0L));
                guild.combatModeCooldownUntil = Math.max(0L, section.getLong(idText + ".combat-mode-cooldown-until", 0L));
                String cityRegion = section.getString(idText + ".city-region");
                if (cityRegion != null) { try { guild.cityRegionId = UUID.fromString(cityRegion); } catch (IllegalArgumentException ignored) { guild.cityRegionId = null; } }
                guilds.put(id, guild);
                members.forEach(member -> memberGuilds.put(member, id));
            } catch (Exception exception) {
                plugin.getLogger().warning("Skipping malformed guild definition " + idText + ".");
            }
        }
    }

    private synchronized void save() {
        if (shuttingDown || persistenceExecutor.isShutdown()) return;
        stateRevision++;
        if (saveWorkerScheduled) return;
        saveWorkerScheduled = true;
        persistenceExecutor.execute(this::drainSaves);
    }

    private void drainSaves() {
        while (true) {
            YamlConfiguration snapshot;
            long revision;
            synchronized (this) {
                revision = stateRevision;
                snapshot = createSnapshot();
            }
            writeAtomically(snapshot);
            synchronized (this) {
                if (revision == stateRevision) {
                    saveWorkerScheduled = false;
                    return;
                }
            }
        }
    }

    private YamlConfiguration createSnapshot() {
        YamlConfiguration snapshot = new YamlConfiguration();
        for (GuildData guild : guilds.values()) {
            String path = "guilds." + guild.id();
            snapshot.set(path + ".name", guild.name());
            snapshot.set(path + ".leader", guild.leaderId().toString());
            if (guild.deputyId != null) snapshot.set(path + ".deputy", guild.deputyId.toString());
            snapshot.set(path + ".members", guild.members().stream().map(UUID::toString).toList());
            snapshot.set(path + ".treasury", Money.toMajor(guild.treasuryMinorUnits()));
            if (guild.cityRegionId != null) snapshot.set(path + ".city-region", guild.cityRegionId.toString());
            snapshot.set(path + ".city-level", guild.cityLevel);
            snapshot.set(path + ".city-upgrade-cooldown-until", guild.cityUpgradeCooldownUntil);
            snapshot.set(path + ".combat-mode", guild.combatMode.name());
            snapshot.set(path + ".combat-mode-change-at", guild.combatModeChangeAt);
            snapshot.set(path + ".combat-mode-cooldown-until", guild.combatModeCooldownUntil);
        }
        return snapshot;
    }

    private void writeAtomically(YamlConfiguration snapshot) {
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            snapshot.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save guilds.yml", exception);
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (shuttingDown) return;
            shuttingDown = true;
        }
        persistenceExecutor.shutdown();
        try {
            if (!persistenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) persistenceExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            saveWorkerScheduled = false;
            guilds.clear();
            memberGuilds.clear();
            invitations.clear();
        }
        Bukkit.getServicesManager().unregister(GuildAPI.class, this);
        if (instance == this) instance = null;
    }

    public enum Result {
        SUCCESS, NOT_REGISTERED, ALREADY_IN_GUILD, LEVEL_TOO_LOW, INVALID_NAME, NAME_TAKEN, INSUFFICIENT_GOLD,
        NOT_IN_GUILD, NOT_LEADER, GUILD_FULL, TARGET_ALREADY_IN_GUILD, TARGET_NOT_MEMBER, INVALID_DEPUTY, NO_DEPUTY, NO_INVITATION, GUILD_NOT_FOUND,
        LEADER_CANNOT_LEAVE, TREASURY_NOT_EMPTY, CITY_ALREADY_CLAIMED, CITY_REGION_NOT_FOUND, NOT_GUILD_CITY, CITY_OWNED, NO_CITY, COMBAT_MODE_ALREADY_ACTIVE, COMBAT_MODE_PENDING, COMBAT_MODE_COOLDOWN
    }

    private record Invitation(UUID guildId, long createdAt) { }

    private static final class GuildData {
        private final UUID id;
        private final String name;
        private final UUID leaderId;
        private UUID deputyId;
        private final LinkedHashSet<UUID> members;
        private long treasuryMinorUnits;
        private UUID cityRegionId;
        private int cityLevel;
        private long cityUpgradeCooldownUntil;
        private KingdomCombatMode combatMode;
        private long combatModeChangeAt;
        private long combatModeCooldownUntil;

        private GuildData(UUID id, String name, UUID leaderId, LinkedHashSet<UUID> members) {
            this.id = id;
            this.name = name;
            this.leaderId = leaderId;
            this.deputyId = null;
            this.members = members;
            this.treasuryMinorUnits = 0L;
            this.cityRegionId = null;
            this.cityLevel = 1;
            this.cityUpgradeCooldownUntil = 0L;
            this.combatMode = KingdomCombatMode.PVE;
            this.combatModeChangeAt = 0L;
            this.combatModeCooldownUntil = 0L;
        }

        private UUID id() { return id; }
        private String name() { return name; }
        private UUID leaderId() { return leaderId; }
        private LinkedHashSet<UUID> members() { return members; }
        private Guild snapshot() { return new Guild(id, name, leaderId, deputyId, members.size(), treasuryMinorUnits, cityRegionId, cityLevel, cityUpgradeCooldownUntil, combatMode, combatModeChangeAt, combatModeCooldownUntil); }
        private long treasuryMinorUnits() { return treasuryMinorUnits; }
        private void addTreasury(long amount) { treasuryMinorUnits = amount > Long.MAX_VALUE - treasuryMinorUnits ? Long.MAX_VALUE : treasuryMinorUnits + amount; }
        private void removeTreasury(long amount) { treasuryMinorUnits -= amount; }
    }
}
