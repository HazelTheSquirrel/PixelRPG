package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.command.GuildAcceptCommand;
import de.pixelrpg.rpg.command.GuildInfoCommand;
import de.pixelrpg.rpg.command.GuildInviteCommand;
import de.pixelrpg.rpg.command.GuildLeaveCommand;
import de.pixelrpg.rpg.command.PaperBasicCommandAdapter;
import de.pixelrpg.rpg.player.PlayerProfileManager;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Owns guild creation, membership, invitations and persistent guild metadata. */
public final class GuildManager implements GuildAPI {
    public static final int MAX_MEMBERS = Guild.MAX_MEMBERS;
    public static final int CREATION_COST_GOLD = Guild.CREATION_COST_GOLD;
    public static final int MIN_CREATION_LEVEL = Guild.MIN_CREATION_LEVEL;
    private static volatile GuildManager instance;

    private final JavaPlugin plugin;
    private final PlayerProfileManager profiles;
    private final File file;
    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberGuilds = new ConcurrentHashMap<>();
    private final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();

    public GuildManager(JavaPlugin plugin, PlayerProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
        load();
        Bukkit.getServicesManager().register(GuildAPI.class, this, plugin, ServicePriority.Normal);
        registerCommands();
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

    public static GuildManager getInstance() { return Objects.requireNonNull(instance, "GuildManager not initialized"); }

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
        if (player == null || !profiles.isRegistered(player.getUniqueId())) return Result.NOT_REGISTERED;
        if (getGuild(player.getUniqueId()).isPresent()) return Result.ALREADY_IN_GUILD;
        if (profiles.getProfile(player.getUniqueId()).map(p -> p.getLevel() < MIN_CREATION_LEVEL).orElse(true)) return Result.LEVEL_TOO_LOW;
        if (!isValidName(name)) return Result.INVALID_NAME;
        String normalized = normalize(name);
        if (guilds.values().stream().anyMatch(g -> g.name().equalsIgnoreCase(normalized))) return Result.NAME_TAKEN;
        var profile = profiles.getProfile(player.getUniqueId()).orElseThrow();
        if (!profile.removeMoney(CREATION_COST_GOLD)) return Result.INSUFFICIENT_GOLD;
        UUID id = UUID.randomUUID();
        GuildData guild = new GuildData(id, normalized, player.getUniqueId(), new LinkedHashSet<>(Set.of(player.getUniqueId())));
        guilds.put(id, guild); memberGuilds.put(player.getUniqueId(), id); save(); return Result.SUCCESS;
    }

    public synchronized Result invite(Player leader, Player target) {
        if (leader == null || target == null) return Result.NOT_IN_GUILD;
        GuildData guild = guilds.get(memberGuilds.get(leader.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(leader.getUniqueId())) return Result.NOT_LEADER;
        if (guild.members().size() >= MAX_MEMBERS) return Result.GUILD_FULL;
        if (getGuild(target.getUniqueId()).isPresent()) return Result.TARGET_ALREADY_IN_GUILD;
        invitations.put(target.getUniqueId(), new Invitation(guild.id(), System.currentTimeMillis()));
        target.sendMessage(Component.text("Du wurdest in die Gilde „" + guild.name() + "“ eingeladen. Nutze /gildeannehmen, um beizutreten.", NamedTextColor.GOLD));
        leader.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN)); return Result.SUCCESS;
    }

    public synchronized Result acceptInvitation(Player player) {
        Invitation invitation = invitations.remove(player.getUniqueId());
        if (invitation == null) return Result.NO_INVITATION;
        GuildData guild = guilds.get(invitation.guildId());
        if (guild == null) return Result.GUILD_NOT_FOUND;
        if (getGuild(player.getUniqueId()).isPresent()) return Result.ALREADY_IN_GUILD;
        if (guild.members().size() >= MAX_MEMBERS) return Result.GUILD_FULL;
        guild.members().add(player.getUniqueId()); memberGuilds.put(player.getUniqueId(), guild.id()); save(); return Result.SUCCESS;
    }

    public synchronized Result leave(Player player) {
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (guild.leaderId().equals(player.getUniqueId())) return Result.LEADER_CANNOT_LEAVE;
        guild.members().remove(player.getUniqueId()); memberGuilds.remove(player.getUniqueId()); save(); return Result.SUCCESS;
    }

    public Optional<Guild> getGuild(UUID playerId) {
        if (playerId == null) return Optional.empty();
        UUID guildId = memberGuilds.get(playerId);
        if (guildId == null) return Optional.empty();
        GuildData data = guilds.get(guildId);
        return data == null ? Optional.empty() : Optional.of(data.snapshot());
    }
    public Optional<Guild> getGuildByName(String name) { return guilds.values().stream().filter(g -> g.name().equalsIgnoreCase(name)).findFirst().map(GuildData::snapshot); }
    public Set<UUID> getMembers(UUID guildId) { GuildData guild = guilds.get(guildId); return guild == null ? Set.of() : Set.copyOf(guild.members()); }
    public boolean isMember(UUID guildId, UUID playerId) { GuildData guild = guilds.get(guildId); return guild != null && guild.members().contains(playerId); }

    public synchronized Result disband(Player player) {
        GuildData guild = guilds.get(memberGuilds.get(player.getUniqueId()));
        if (guild == null) return Result.NOT_IN_GUILD;
        if (!guild.leaderId().equals(player.getUniqueId())) return Result.NOT_LEADER;
        guild.members().forEach(memberGuilds::remove); guilds.remove(guild.id()); save(); return Result.SUCCESS;
    }

    @Override public boolean isRegistered(UUID uuid) { return getGuild(uuid).isPresent(); }
    @Override public int getLevel(UUID uuid) { return getGuild(uuid).isPresent() ? 1 : 0; }
    @Override public long getExperience(UUID uuid) { return 0L; }
    @Override public void addExperience(UUID uuid, long amount) { }

    private boolean isValidName(String name) { return name != null && name.trim().length() >= 3 && name.trim().length() <= 24 && name.trim().matches("[A-Za-z0-9ÄÖÜäöüß _-]+"); }
    private String normalize(String name) { return name.trim().replaceAll("\\s+", " "); }

    private void load() {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("guilds"); if (section == null) return;
        for (String idText : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idText); String name = section.getString(idText + ".name");
                UUID leader = UUID.fromString(Objects.requireNonNull(section.getString(idText + ".leader")));
                LinkedHashSet<UUID> members = new LinkedHashSet<>();
                for (String text : section.getStringList(idText + ".members")) members.add(UUID.fromString(text));
                if (members.isEmpty()) members.add(leader);
                GuildData guild = new GuildData(id, name, leader, members); guilds.put(id, guild); members.forEach(member -> memberGuilds.put(member, id));
            } catch (Exception exception) { plugin.getLogger().warning("Skipping malformed guild definition " + idText + "."); }
        }
    }

    private synchronized void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (GuildData guild : guilds.values()) {
            String path = "guilds." + guild.id(); data.set(path + ".name", guild.name()); data.set(path + ".leader", guild.leaderId().toString()); data.set(path + ".members", guild.members().stream().map(UUID::toString).toList());
        }
        try { data.save(file); } catch (IOException exception) { plugin.getLogger().severe("Could not save guilds.yml: " + exception.getMessage()); }
    }

    public enum Result { SUCCESS, NOT_REGISTERED, ALREADY_IN_GUILD, LEVEL_TOO_LOW, INVALID_NAME, NAME_TAKEN, INSUFFICIENT_GOLD, NOT_IN_GUILD, NOT_LEADER, GUILD_FULL, TARGET_ALREADY_IN_GUILD, NO_INVITATION, GUILD_NOT_FOUND, LEADER_CANNOT_LEAVE }
    private record Invitation(UUID guildId, long createdAt) { }
    private static final class GuildData {
        private final UUID id; private final String name; private final UUID leaderId; private final LinkedHashSet<UUID> members;
        private GuildData(UUID id, String name, UUID leaderId, LinkedHashSet<UUID> members) { this.id=id; this.name=name; this.leaderId=leaderId; this.members=members; }
        private UUID id(){return id;} private String name(){return name;} private UUID leaderId(){return leaderId;} private LinkedHashSet<UUID> members(){return members;}
        private Guild snapshot(){return new Guild(id,name,leaderId,members.size());}
    }
}
