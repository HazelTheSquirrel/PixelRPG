package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Owns guild membership, invitations and persistent guild metadata. */
public final class GuildManager implements GuildAPI, AutoCloseable {
    private static final long INVITE_TIMEOUT_MILLIS = 60_000L;

    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File storageFile;
    private final ExecutorService ioExecutor;
    private final Map<UUID, GuildState> guilds = new java.util.HashMap<>();
    private final Map<UUID, UUID> guildByMember = new java.util.HashMap<>();
    private final Map<UUID, Invitation> invitations = new java.util.HashMap<>();
    private boolean saveScheduled;
    private long stateRevision;
    private boolean shuttingDown;

    public GuildManager(JavaPlugin plugin, PlayerProfileManager profiles) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.storageFile = new File(plugin.getDataFolder(), "guilds.yml");
        this.ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        load();
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        registerCommands();
    }

    public synchronized Result createGuild(Player player, String rawName) {
        if (shuttingDown || player == null || !profiles.isRegistered(player.getUniqueId())) return Result.NOT_REGISTERED;
        UUID playerId = player.getUniqueId();
        if (guildByMember.containsKey(playerId)) return Result.ALREADY_IN_GUILD;
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null || profile.level() < Guild.MIN_CREATION_LEVEL) return Result.LEVEL_TOO_LOW;

        String name = normalizeName(rawName);
        if (name == null) return Result.INVALID_NAME;
        if (guilds.values().stream().anyMatch(g -> g.name.equalsIgnoreCase(name))) return Result.NAME_TAKEN;
        if (!profile.removeMoney(Guild.CREATION_COST_GOLD)) return Result.INSUFFICIENT_GOLD;

        UUID id = UUID.randomUUID();
        GuildState state = new GuildState(id, name, playerId, new LinkedHashSet<>(Set.of(playerId)));
        guilds.put(id, state);
        guildByMember.put(playerId, id);
        profiles.saveProfileAsync(playerId);
        scheduleSave();
        profiles.notifyExternalStateChange(playerId);
        return Result.SUCCESS;
    }

    public synchronized Result invite(Player leader, Player target) {
        if (shuttingDown || leader == null || target == null) return Result.NOT_IN_GUILD;
        GuildState guild = stateForMember(leader.getUniqueId());
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId.equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.members.size() >= Guild.MAX_MEMBERS) return Result.GUILD_FULL;
        if (guildByMember.containsKey(target.getUniqueId())) return Result.TARGET_ALREADY_IN_GUILD;
        invitations.put(target.getUniqueId(), new Invitation(guild.id, System.currentTimeMillis() + INVITE_TIMEOUT_MILLIS));
        target.sendMessage(Component.text("Du wurdest in die Gilde „" + guild.name + "“ eingeladen. Nutze /gildeannehmen, um beizutreten.", NamedTextColor.GOLD));
        leader.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN));
        return Result.SUCCESS;
    }

    public synchronized Result acceptInvitation(Player player) {
        if (shuttingDown || player == null) return Result.NO_INVITATION;
        Invitation invitation = invitations.remove(player.getUniqueId());
        if (invitation == null || invitation.expiresAt < System.currentTimeMillis()) return Result.NO_INVITATION;
        GuildState guild = guilds.get(invitation.guildId);
        if (guild == null) return Result.GUILD_NOT_FOUND;
        if (guildByMember.containsKey(player.getUniqueId())) return Result.ALREADY_IN_GUILD;
        if (guild.members.size() >= Guild.MAX_MEMBERS) return Result.GUILD_FULL;
        guild.members.add(player.getUniqueId());
        guildByMember.put(player.getUniqueId(), guild.id);
        scheduleSave();
        profiles.notifyExternalStateChange(player.getUniqueId());
        return Result.SUCCESS;
    }

    public synchronized Result leave(Player player) {
        if (shuttingDown || player == null) return Result.NOT_IN_GUILD;
        GuildState guild = stateForMember(player.getUniqueId());
        if (guild == null) return Result.NOT_IN_GUILD;
        if (guild.leaderId.equals(player.getUniqueId())) return Result.LEADER_CANNOT_LEAVE;
        guild.members.remove(player.getUniqueId());
        guildByMember.remove(player.getUniqueId());
        scheduleSave();
        profiles.notifyExternalStateChange(player.getUniqueId());
        return Result.SUCCESS;
    }

    public synchronized Result disband(Player player) {
        if (shuttingDown || player == null) return Result.NOT_IN_GUILD;
        GuildState guild = stateForMember(player.getUniqueId());
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId.equals(player.getUniqueId())) return Result.NOT_LEADER;
        Set<UUID> members = Set.copyOf(guild.members);
        for (UUID member : members) guildByMember.remove(member);
        guilds.remove(guild.id);
        scheduleSave();
        members.forEach(profiles::notifyExternalStateChange);
        return Result.SUCCESS;
    }

    public synchronized Optional<Guild> getGuild(UUID playerId) {
        GuildState guild = stateForMember(playerId);
        return guild == null ? Optional.empty() : Optional.of(guild.snapshot());
    }

    public synchronized Optional<Guild> getGuildByName(String name) {
        if (name == null) return Optional.empty();
        return guilds.values().stream()
                .filter(guild -> guild.name.equalsIgnoreCase(name.trim()))
                .findFirst()
                .map(GuildState::snapshot);
    }

    public synchronized Set<UUID> getMembers(UUID guildId) {
        GuildState guild = guilds.get(guildId);
        return guild == null ? Set.of() : Set.copyOf(guild.members);
    }

    public synchronized boolean isMember(UUID guildId, UUID playerId) {
        GuildState guild = guilds.get(guildId);
        return guild != null && guild.members.contains(playerId);
    }

    @Override
    public synchronized boolean isRegistered(UUID uuid) {
        return uuid != null && guildByMember.containsKey(uuid);
    }

    @Override
    public synchronized int getLevel(UUID uuid) {
        return isRegistered(uuid) ? 1 : 0;
    }

    @Override
    public long getExperience(UUID uuid) {
        return 0L;
    }

    @Override
    public void addExperience(UUID uuid, long amount) {
        // Guild experience is not represented by the main reference data model.
    }

    private GuildState stateForMember(UUID playerId) {
        UUID guildId = guildByMember.get(playerId);
        return guildId == null ? null : guilds.get(guildId);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 24) return null;
        if (!name.matches("[A-Za-z0-9ÄÖÜäöüß _-]+")) return null;
        return name;
    }

    private void registerCommands() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("gildenerstellen", new GuildCreateCommand(this));
            event.registrar().register("gildeneinladen", new GuildInviteCommand(this));
            event.registrar().register("gildeannehmen", new GuildAcceptCommand(this));
            event.registrar().register("gildeverlassen", new GuildLeaveCommand(this));
            event.registrar().register("gildeinfo", new GuildInfoCommand(this));
            event.registrar().register("gildeauflösen", new GuildDisbandCommand(this));
        });
    }

    private void load() {
        if (!storageFile.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(storageFile);
        var section = data.getConfigurationSection("guilds");
        if (section == null) return;
        for (String idText : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idText);
                String name = section.getString(idText + ".name");
                UUID leader = UUID.fromString(Objects.requireNonNull(section.getString(idText + ".leader")));
                LinkedHashSet<UUID> members = new LinkedHashSet<>();
                for (String member : section.getStringList(idText + ".members")) members.add(UUID.fromString(member));
                if (members.isEmpty()) members.add(leader);
                if (members.size() > Guild.MAX_MEMBERS || !members.contains(leader) || name == null) continue;
                GuildState state = new GuildState(id, name, leader, members);
                guilds.put(id, state);
                members.forEach(member -> guildByMember.put(member, id));
            } catch (Exception exception) {
                plugin.getLogger().warning("Ignoring malformed guild entry " + idText + ".");
            }
        }
    }

    private synchronized void scheduleSave() {
        if (shuttingDown || saveScheduled) {
            stateRevision++;
            return;
        }
        stateRevision++;
        saveScheduled = true;
        ioExecutor.execute(this::drainSaves);
    }

    private void drainSaves() {
        while (true) {
            final YamlConfiguration snapshot;
            final long revision;
            synchronized (this) {
                revision = stateRevision;
                snapshot = snapshot();
            }
            writeAtomically(snapshot);
            synchronized (this) {
                if (revision == stateRevision || shuttingDown) {
                    saveScheduled = false;
                    return;
                }
            }
        }
    }

    private synchronized YamlConfiguration snapshot() {
        YamlConfiguration data = new YamlConfiguration();
        for (GuildState guild : guilds.values()) {
            String path = "guilds." + guild.id;
            data.set(path + ".name", guild.name);
            data.set(path + ".leader", guild.leaderId.toString());
            data.set(path + ".members", guild.members.stream().map(UUID::toString).toList());
        }
        return data;
    }

    private void writeAtomically(YamlConfiguration data) {
        Path target = storageFile.toPath();
        Path temp = target.resolveSibling(storageFile.getName() + ".tmp");
        try {
            storageFile.getParentFile().mkdirs();
            data.save(temp.toFile());
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save guilds.yml.", exception);
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    public void shutdown() {
        synchronized (this) {
            if (shuttingDown) return;
            shuttingDown = true;
        }
        try {
            ioExecutor.shutdown();
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) ioExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            guilds.clear();
            guildByMember.clear();
            invitations.clear();
            saveScheduled = false;
        }
        Bukkit.getServicesManager().unregister(GuildAPI.class, this);
    }

    private record Invitation(UUID guildId, long expiresAt) { }

    private static final class GuildState {
        private final UUID id;
        private final String name;
        private final UUID leaderId;
        private final LinkedHashSet<UUID> members;

        private GuildState(UUID id, String name, UUID leaderId, LinkedHashSet<UUID> members) {
            this.id = id;
            this.name = name;
            this.leaderId = leaderId;
            this.members = members;
        }

        private Guild snapshot() {
            return new Guild(id, name, leaderId, members.size());
        }
    }

    private abstract static class PlayerCommand implements BasicCommand {
        protected final GuildManager guilds;
        protected PlayerCommand(GuildManager guilds) { this.guilds = guilds; }
        protected Player player(CommandSourceStack source) {
            return source.getSender() instanceof Player p ? p : null;
        }
        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) { return List.of(); }
        @Override public String permission() { return "rpg.member"; }
    }

    private static final class GuildCreateCommand extends PlayerCommand {
        private GuildCreateCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            if (args.length != 1) { player.sendMessage(Component.text("Nutze: /gildenerstellen <Name>", NamedTextColor.YELLOW)); return; }
            switch (guilds.createGuild(player, args[0])) {
                case SUCCESS -> player.sendMessage(Component.text("Die Gilde wurde erstellt.", NamedTextColor.GREEN));
                case LEVEL_TOO_LOW -> player.sendMessage(Component.text("Du benötigst mindestens Level 20.", NamedTextColor.RED));
                case INSUFFICIENT_GOLD -> player.sendMessage(Component.text("Du benötigst 2.500 Gold.", NamedTextColor.RED));
                case NAME_TAKEN -> player.sendMessage(Component.text("Dieser Gildenname ist bereits vergeben.", NamedTextColor.RED));
                case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
                case INVALID_NAME -> player.sendMessage(Component.text("Der Gildenname ist ungültig.", NamedTextColor.RED));
                default -> player.sendMessage(Component.text("Die Gilde konnte nicht erstellt werden.", NamedTextColor.RED));
            }
        }
    }

    private static final class GuildInviteCommand extends PlayerCommand {
        private GuildInviteCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            if (args.length != 1) { player.sendMessage(Component.text("Nutze: /gildeneinladen <Spieler>", NamedTextColor.YELLOW)); return; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { player.sendMessage(Component.text("Dieser Spieler ist nicht online.", NamedTextColor.RED)); return; }
            switch (guilds.invite(player, target)) {
                case SUCCESS -> { }
                case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
                case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister darf Spieler einladen.", NamedTextColor.RED));
                case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde hat bereits 50 Mitglieder.", NamedTextColor.RED));
                case TARGET_ALREADY_IN_GUILD -> player.sendMessage(Component.text("Dieser Spieler ist bereits in einer Gilde.", NamedTextColor.RED));
                default -> player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED));
            }
        }
        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
            if (args.length != 1) return List.of();
            String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
                    .sorted().toList();
        }
    }

    private static final class GuildAcceptCommand extends PlayerCommand {
        private GuildAcceptCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            switch (guilds.acceptInvitation(player)) {
                case SUCCESS -> player.sendMessage(Component.text("Du bist der Gilde beigetreten.", NamedTextColor.GREEN));
                case NO_INVITATION -> player.sendMessage(Component.text("Du hast keine offene Gildeneinladung.", NamedTextColor.RED));
                case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde ist inzwischen voll.", NamedTextColor.RED));
                case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
                default -> player.sendMessage(Component.text("Die Einladung konnte nicht angenommen werden.", NamedTextColor.RED));
            }
        }
    }

    private static final class GuildLeaveCommand extends PlayerCommand {
        private GuildLeaveCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            switch (guilds.leave(player)) {
                case SUCCESS -> player.sendMessage(Component.text("Du hast die Gilde verlassen.", NamedTextColor.YELLOW));
                case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
                case LEADER_CANNOT_LEAVE -> player.sendMessage(Component.text("Der Gildenmeister kann die Gilde nicht verlassen.", NamedTextColor.RED));
                default -> player.sendMessage(Component.text("Du konntest die Gilde nicht verlassen.", NamedTextColor.RED));
            }
        }
    }

    private static final class GuildInfoCommand extends PlayerCommand {
        private GuildInfoCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
            if (guild == null) { player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED)); return; }
            player.sendMessage(Component.text("Gilde: " + guild.name() + " | Mitglieder: " + guild.memberCount() + "/50 | Rolle: "
                    + (guild.isLeader(player.getUniqueId()) ? "Gildenmeister" : "Mitglied"), NamedTextColor.GOLD));
        }
    }

    private static final class GuildDisbandCommand extends PlayerCommand {
        private GuildDisbandCommand(GuildManager guilds) { super(guilds); }
        @Override public void execute(CommandSourceStack source, String[] args) {
            Player player = player(source);
            if (player == null) return;
            switch (guilds.disband(player)) {
                case SUCCESS -> player.sendMessage(Component.text("Die Gilde wurde aufgelöst.", NamedTextColor.YELLOW));
                case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
                case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister darf die Gilde auflösen.", NamedTextColor.RED));
                default -> player.sendMessage(Component.text("Die Gilde konnte nicht aufgelöst werden.", NamedTextColor.RED));
            }
        }
    }

    public enum Result {
        SUCCESS, NOT_REGISTERED, ALREADY_IN_GUILD, LEVEL_TOO_LOW, INVALID_NAME, NAME_TAKEN,
        INSUFFICIENT_GOLD, NOT_IN_GUILD, NOT_LEADER, GUILD_FULL, TARGET_ALREADY_IN_GUILD,
        NO_INVITATION, GUILD_NOT_FOUND, LEADER_CANNOT_LEAVE
    }
}
