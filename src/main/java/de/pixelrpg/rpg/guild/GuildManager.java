package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.player.PlayerProfileManager;
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
public final class GuildManager implements GuildAPI, AutoCloseable {
    public static final int MAX_MEMBERS = Guild.MAX_MEMBERS;
    public static final int CREATION_COST_GOLD = Guild.CREATION_COST_GOLD;
    public static final int MIN_CREATION_LEVEL = Guild.MIN_CREATION_LEVEL;
    private static volatile GuildManager instance;
    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File file;
    private final Map<UUID, GuildData> guilds = new HashMap<>();
    private final Map<UUID, UUID> memberGuilds = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private final ExecutorService persistenceExecutor;
    private boolean saveWorkerScheduled;
    private long stateRevision;
    private volatile boolean shuttingDown;

    public GuildManager(JavaPlugin plugin, PlayerProfileManager profiles) {
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
        instance = this;
    }

    public static GuildManager getInstance(JavaPlugin plugin, PlayerProfileManager profiles) {
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
        guilds.put(id, guild);
        memberGuilds.put(player.getUniqueId(), id);
        profiles.saveProfileAsync(player.getUniqueId());
        save();
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
        memberGuilds.remove(player.getUniqueId());
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

    public synchronized Optional<Guild> getGuildByName(String name) {
        return guilds.values().stream().filter(g -> g.name().equalsIgnoreCase(name)).findFirst().map(GuildData::snapshot);
    }

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

    private void wakeScoreboards(Set<UUID> changedPlayers) { if (!changedPlayers.isEmpty()) changedPlayers.forEach(profiles::notifyExternalStateChange); }

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
            snapshot.set(path + ".members", guild.members().stream().map(UUID::toString).toList());
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

    @Override public void close() { shutdown(); }

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
        NOT_IN_GUILD, NOT_LEADER, GUILD_FULL, TARGET_ALREADY_IN_GUILD, NO_INVITATION, GUILD_NOT_FOUND,
        LEADER_CANNOT_LEAVE
    }

    private record Invitation(UUID guildId, long createdAt) { }

    private static final class GuildData {
        private final UUID id;
        private final String name;
        private final UUID leaderId;
        private final LinkedHashSet<UUID> members;

        private GuildData(UUID id, String name, UUID leaderId, LinkedHashSet<UUID> members) {
            this.id = id;
            this.name = name;
            this.leaderId = leaderId;
            this.members = members;
        }

        private UUID id() { return id; }
        private String name() { return name; }
        private UUID leaderId() { return leaderId; }
        private LinkedHashSet<UUID> members() { return members; }
        private Guild snapshot() { return new Guild(id, name, leaderId, members.size()); }
    }
}
