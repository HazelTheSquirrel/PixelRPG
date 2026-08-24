package de.pixelrpg.rpg.party;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.lang.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyManager implements PartyAPI {
    public static final long INVITE_TIMEOUT_MILLIS = 60_000L;
    public static final long EMPTY_CLEANUP_MILLIS = 30L * 60L * 1_000L;
    private static final double SHARE_RANGE = 50.0D;

    private final PixelRPGPlugin plugin;
    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyIdByMember = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, Long> emptySince = new ConcurrentHashMap<>();
    private final LanguageManager lang;
    private final File storageFile;
    private BukkitTask maintenanceTask;

    public PartyManager(PixelRPGPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.storageFile = new File(plugin.getDataFolder(), "parties.yml");
        load();
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::maintenance, 20L, 20L * 30L);
    }

    public Optional<Party> getParty(UUID member) {
        UUID partyId = partyIdByMember.get(member);
        return partyId == null ? Optional.empty() : Optional.ofNullable(partiesById.get(partyId));
    }

    public Party createParty(UUID leader) {
        getParty(leader).ifPresent(this::disbandParty);
        Party party = new Party(UUID.randomUUID(), leader);
        partiesById.put(party.getId(), party);
        partyIdByMember.put(leader, party.getId());
        save();
        return party;
    }

    public boolean addInvite(UUID target, UUID leader) {
        Party party = getParty(leader).orElse(null);
        if (party == null || !party.isLeader(leader) || party.isFull() || party.isMember(target)) return false;
        if (getParty(target).isPresent()) return false;
        pendingInvites.put(target, new PendingInvite(leader, System.currentTimeMillis() + INVITE_TIMEOUT_MILLIS));
        return true;
    }

    public Optional<UUID> getInviteLeader(UUID target) {
        PendingInvite invite = pendingInvites.get(target);
        if (invite == null || invite.expiresAt < System.currentTimeMillis()) {
            pendingInvites.remove(target);
            return Optional.empty();
        }
        return Optional.of(invite.leader);
    }

    public void removeInvite(UUID target) { pendingInvites.remove(target); }

    public boolean acceptInvite(Player player) {
        UUID target = player.getUniqueId();
        UUID leaderUuid = getInviteLeader(target).orElse(null);
        if (leaderUuid == null) return false;
        pendingInvites.remove(target);
        Party party = getParty(leaderUuid).orElse(null);
        if (party == null || party.isFull() || getParty(target).isPresent()) return false;
        party.addMember(target);
        partyIdByMember.put(target, party.getId());
        emptySince.remove(party.getId());
        broadcastToParty(party, lang.get("party.member-joined", "player", player.getName()), target);
        save();
        return true;
    }

    public boolean transferLeadership(Player leader, UUID target) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null || !party.isLeader(leader.getUniqueId()) || !party.isMember(target)) return false;
        party.setLeader(target);
        save();
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) lang.send(targetPlayer, "party.now-leader");
        return true;
    }

    public boolean kick(Player leader, UUID target) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null || !party.isLeader(leader.getUniqueId()) || target.equals(leader.getUniqueId()) || !party.isMember(target)) return false;
        party.removeMember(target);
        partyIdByMember.remove(target);
        pendingInvites.remove(target);
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) lang.send(targetPlayer, "party.kicked");
        broadcastToParty(party, lang.get("party.member-kicked", "player", name(target)), null);
        save();
        return true;
    }

    public void leaveParty(Player player) {
        UUID uuid = player.getUniqueId();
        Party party = getParty(uuid).orElse(null);
        if (party == null) return;
        boolean wasLeader = party.isLeader(uuid);
        party.removeMember(uuid);
        partyIdByMember.remove(uuid);
        if (party.isEmpty()) {
            partiesById.remove(party.getId());
            emptySince.remove(party.getId());
            save();
            return;
        }
        if (wasLeader) {
            UUID newLeader = party.promoteNextLeader();
            if (newLeader != null) notifyLeader(newLeader);
        }
        broadcastToParty(party, lang.get("party.member-left", "player", player.getName()), null);
        save();
    }

    public void disbandParty(Party party) {
        for (UUID member : new HashSet<>(party.getMembers())) {
            partyIdByMember.remove(member);
            Player memberPlayer = Bukkit.getPlayer(member);
            if (memberPlayer != null) lang.send(memberPlayer, "party.disbanded");
        }
        partiesById.remove(party.getId());
        emptySince.remove(party.getId());
        save();
    }

    public void handleDisconnect(UUID uuid) {
        pendingInvites.entrySet().removeIf(entry -> entry.getKey().equals(uuid) || entry.getValue().leader.equals(uuid));
        Party party = getParty(uuid).orElse(null);
        if (party == null) return;
        boolean wasLeader = party.isLeader(uuid);
        party.removeMember(uuid);
        partyIdByMember.remove(uuid);
        if (party.isEmpty()) {
            emptySince.putIfAbsent(party.getId(), System.currentTimeMillis());
        } else if (wasLeader) {
            UUID newLeader = party.promoteNextLeader();
            if (newLeader != null) notifyLeader(newLeader);
        }
        save();
    }

    public void broadcastToParty(Party party, net.kyori.adventure.text.Component message, UUID exclude) {
        for (UUID member : party.getMembers()) {
            if (exclude != null && exclude.equals(member)) continue;
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) player.sendMessage(message);
        }
    }

    private void notifyLeader(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) lang.send(player, "party.now-leader");
    }

    private String name(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private void maintenance() {
        long now = System.currentTimeMillis();
        pendingInvites.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now || getParty(entry.getValue().leader).isEmpty());
        boolean changed = false;
        for (Party party : new ArrayList<>(partiesById.values())) {
            boolean hasOnlineMember = party.getMembers().stream().anyMatch(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                return player != null && player.isOnline();
            });
            if (hasOnlineMember) {
                emptySince.remove(party.getId());
                continue;
            }
            long since = emptySince.computeIfAbsent(party.getId(), ignored -> now);
            if (now - since >= EMPTY_CLEANUP_MILLIS) {
                partiesById.remove(party.getId());
                party.getMembers().forEach(partyIdByMember::remove);
                emptySince.remove(party.getId());
                changed = true;
            }
        }
        if (changed) save();
    }

    private void load() {
        if (!storageFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        for (String idText : config.getStringList("parties.ids")) {
            try {
                UUID id = UUID.fromString(idText);
                UUID leader = UUID.fromString(config.getString("parties." + id + ".leader", ""));
                Set<UUID> members = new java.util.LinkedHashSet<>();
                for (String member : config.getStringList("parties." + id + ".members")) members.add(UUID.fromString(member));
                if (members.isEmpty() || !members.contains(leader)) continue;
                Party party = new Party(id, leader, members);
                if (party.getMembers().size() > Party.MAX_MEMBERS) continue;
                partiesById.put(id, party);
                for (UUID member : party.getMembers()) partyIdByMember.put(member, id);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid persisted party entry: " + idText);
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        for (Party party : partiesById.values()) {
            ids.add(party.getId().toString());
            String path = "parties." + party.getId();
            config.set(path + ".leader", party.getLeader().toString());
            config.set(path + ".members", party.getMembers().stream().map(UUID::toString).toList());
        }
        config.set("parties.ids", ids);
        try {
            storageFile.getParentFile().mkdirs();
            config.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save parties.yml: " + exception.getMessage());
        }
    }

    public void shutdown() {
        if (maintenanceTask != null) maintenanceTask.cancel();
        save();
        pendingInvites.clear();
        partyIdByMember.clear();
        partiesById.clear();
        emptySince.clear();
    }

    @Override public boolean isInParty(UUID uuid) { return getParty(uuid).map(p -> p.getMembers().size() > 1).orElse(false); }
    @Override public Set<UUID> getPartyMembers(UUID uuid) { return getParty(uuid).map(p -> Set.copyOf(p.getMembers())).orElse(Set.of(uuid)); }
    @Override public UUID getPartyLeader(UUID uuid) { return getParty(uuid).map(Party::getLeader).orElse(uuid); }
    @Override public boolean isLeader(UUID uuid) { return getParty(uuid).map(p -> p.isLeader(uuid)).orElse(false); }
    @Override public boolean isWithinShareRange(UUID source, UUID target) {
        Player a = Bukkit.getPlayer(source);
        Player b = Bukkit.getPlayer(target);
        if (a == null || b == null || !a.isOnline() || !b.isOnline() || !a.getWorld().equals(b.getWorld())) return false;
        return a.getLocation().distanceSquared(b.getLocation()) <= SHARE_RANGE * SHARE_RANGE;
    }
    @Override public double getShareRange() { return SHARE_RANGE; }

    private record PendingInvite(UUID leader, long expiresAt) {}
}
