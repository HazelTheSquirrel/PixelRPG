package de.pixelrpg.rpg.party;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.core.WakeScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Party state is event-driven; invite expiry and empty-party cleanup use exact deadline wake-ups instead of polling. */
public final class PartyManager implements PartyAPI, AutoCloseable {
    public static final int MAX_MEMBERS = Party.MAX_MEMBERS;
    public static final long INVITE_TIMEOUT_MILLIS = 60_000L;
    public static final long EMPTY_CLEANUP_MILLIS = 30L * 60L * 1_000L;
    private static final double SHARE_RANGE = 50.0D;

    private final PixelRPGPlugin plugin;
    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyIdByMember = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();
    private final File storageFile;
    private final ExecutorService persistenceExecutor;
    private boolean saveWorkerScheduled;
    private long stateRevision;
    private final WakeScheduler<UUID> inviteWakeScheduler;
    private final WakeScheduler<UUID> emptyPartyWakeScheduler;
    private volatile boolean shuttingDown;

    public PartyManager(PixelRPGPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "parties.yml");
        this.persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> { Thread thread = new Thread(runnable, "PixelRPG-PartyIO"); thread.setDaemon(true); return thread; });
        this.inviteWakeScheduler = new WakeScheduler<>(plugin);
        this.emptyPartyWakeScheduler = new WakeScheduler<>(plugin);
        load();
    }

    public Optional<Party> getParty(UUID member) { UUID partyId = partyIdByMember.get(member); return partyId == null ? Optional.empty() : Optional.ofNullable(partiesById.get(partyId)); }

    public Party createParty(UUID leader) {
        if (shuttingDown) throw new IllegalStateException("PartyManager is shutting down");
        getParty(leader).ifPresent(this::disbandParty);
        Party party = new Party(UUID.randomUUID(), leader);
        partiesById.put(party.getId(), party); partyIdByMember.put(leader, party.getId()); save(); wakeScoreboards(Set.of(leader)); return party;
    }

    public boolean addInvite(UUID target, UUID leader) {
        if (shuttingDown) return false;
        Party party = getParty(leader).orElse(null);
        if (party == null || !party.isLeader(leader) || party.isFull() || party.isMember(target) || getParty(target).isPresent()) return false;
        pendingInvites.put(target, new PendingInvite(leader, System.currentTimeMillis() + INVITE_TIMEOUT_MILLIS));
        inviteWakeScheduler.cancel(target);
        inviteWakeScheduler.wakeLater(target, Math.max(1L, INVITE_TIMEOUT_MILLIS / 50L), () -> expireInvite(target));
        return true;
    }

    public Optional<UUID> getInviteLeader(UUID target) {
        PendingInvite invite = pendingInvites.get(target);
        if (invite == null || invite.expiresAt() <= System.currentTimeMillis()) { expireInvite(target); return Optional.empty(); }
        return Optional.of(invite.leader());
    }

    public void removeInvite(UUID target) { pendingInvites.remove(target); inviteWakeScheduler.cancel(target); }

    private void expireInvite(UUID target) { PendingInvite invite = pendingInvites.get(target); if (invite != null && invite.expiresAt() <= System.currentTimeMillis()) pendingInvites.remove(target, invite); inviteWakeScheduler.cancel(target); }

    public boolean acceptInvite(Player player) {
        UUID target = player.getUniqueId(); UUID leaderUuid = getInviteLeader(target).orElse(null); if (leaderUuid == null) return false;
        pendingInvites.remove(target); inviteWakeScheduler.cancel(target);
        Party party = getParty(leaderUuid).orElse(null); if (party == null || party.isFull() || getParty(target).isPresent()) return false;
        party.addMember(target); partyIdByMember.put(target, party.getId());
        broadcastToParty(party, Component.text(player.getName() + " ist der Gruppe beigetreten!", NamedTextColor.GREEN), target); save(); wakeScoreboards(party.getMembers()); return true;
    }

    public boolean transferLeadership(Player leader, UUID target) {
        Party party = getParty(leader.getUniqueId()).orElse(null); if (party == null || !party.isLeader(leader.getUniqueId()) || !party.isMember(target) || leader.getUniqueId().equals(target)) return false;
        party.setLeader(target); save(); wakeScoreboards(party.getMembers()); Player targetPlayer = Bukkit.getPlayer(target); if (targetPlayer != null) targetPlayer.sendMessage(Component.text("Du bist jetzt der Gruppenanführer.", NamedTextColor.GOLD)); return true;
    }

    public boolean kick(Player leader, UUID target) {
        Party party = getParty(leader.getUniqueId()).orElse(null); if (party == null || !party.isLeader(leader.getUniqueId()) || target.equals(leader.getUniqueId()) || !party.isMember(target)) return false;
        party.removeMember(target); partyIdByMember.remove(target); pendingInvites.remove(target); inviteWakeScheduler.cancel(target);
        Player targetPlayer = Bukkit.getPlayer(target); if (targetPlayer != null) targetPlayer.sendMessage(Component.text("Du wurdest aus der Gruppe entfernt.", NamedTextColor.RED));
        broadcastToParty(party, Component.text(name(target) + " wurde aus der Gruppe entfernt.", NamedTextColor.YELLOW), null); save(); Set<UUID> affected = new HashSet<>(party.getMembers()); affected.add(target); wakeScoreboards(affected); return true;
    }

    public void leaveParty(Player player) {
        UUID uuid = player.getUniqueId(); Party party = getParty(uuid).orElse(null); if (party == null) return;
        boolean wasLeader = party.isLeader(uuid); party.removeMember(uuid); partyIdByMember.remove(uuid);
        if (party.isEmpty()) { partiesById.remove(party.getId()); emptyPartyWakeScheduler.cancel(party.getId()); save(); wakeScoreboards(Set.of(uuid)); return; }
        if (wasLeader) { UUID newLeader = party.promoteNextLeader(); if (newLeader != null) notifyLeader(newLeader); }
        broadcastToParty(party, Component.text(player.getName() + " hat die Gruppe verlassen.", NamedTextColor.YELLOW), null); save(); Set<UUID> affected = new HashSet<>(party.getMembers()); affected.add(uuid); wakeScoreboards(affected);
    }

    public void disbandParty(Party party) {
        if (party == null) return;
        Set<UUID> affected = new HashSet<>(party.getMembers());
        for (UUID member : affected) { partyIdByMember.remove(member); Player memberPlayer = Bukkit.getPlayer(member); if (memberPlayer != null) memberPlayer.sendMessage(Component.text("Die Gruppe wurde aufgelöst.", NamedTextColor.RED)); }
        partiesById.remove(party.getId()); emptyPartyWakeScheduler.cancel(party.getId()); save(); wakeScoreboards(affected);
    }

    /** Handles a disconnect without removing the offline member from the persistent party. */
    public void handleDisconnect(UUID uuid) {
        pendingInvites.entrySet().removeIf(entry -> entry.getKey().equals(uuid) || entry.getValue().leader().equals(uuid));
        PendingInvite ownInvite = pendingInvites.remove(uuid); if (ownInvite != null) inviteWakeScheduler.cancel(uuid);
        Party party = getParty(uuid).orElse(null); if (party == null) return;
        boolean hasOnlineMember = party.getMembers().stream().anyMatch(member -> { Player player = Bukkit.getPlayer(member); return player != null && player.isOnline(); });
        if (!hasOnlineMember) scheduleEmptyCleanup(party.getId());
    }

    private void scheduleEmptyCleanup(UUID partyId) { emptyPartyWakeScheduler.cancel(partyId); emptyPartyWakeScheduler.wakeLater(partyId, Math.max(1L, EMPTY_CLEANUP_MILLIS / 50L), () -> cleanupEmptyParty(partyId)); }
    private void cleanupEmptyParty(UUID partyId) {
        Party party = partiesById.get(partyId); if (party == null) return;
        boolean hasOnlineMember = party.getMembers().stream().anyMatch(member -> { Player player = Bukkit.getPlayer(member); return player != null && player.isOnline(); });
        if (hasOnlineMember) return;
        Set<UUID> affected = new HashSet<>(party.getMembers()); partiesById.remove(partyId, party); party.getMembers().forEach(partyIdByMember::remove); save(); wakeScoreboards(affected);
    }

    public void broadcastToParty(Party party, Component message, UUID exclude) { for (UUID member : party.getMembers()) { if (exclude != null && exclude.equals(member)) continue; Player player = Bukkit.getPlayer(member); if (player != null && player.isOnline()) player.sendMessage(message); } }
    private void notifyLeader(UUID uuid) { Player player = Bukkit.getPlayer(uuid); if (player != null) player.sendMessage(Component.text("Du bist jetzt der Gruppenanführer.", NamedTextColor.GOLD)); }
    private String name(UUID uuid) { OfflinePlayer player = Bukkit.getOfflinePlayer(uuid); return player.getName() == null ? uuid.toString() : player.getName(); }
    /** Marks only players whose sidebar party line can have changed. */
    private void wakeScoreboards(Iterable<UUID> affectedPlayers) { for (UUID playerId : affectedPlayers) plugin.getPlayerProfileManager().notifyExternalStateChange(playerId); }

    private void load() {
        if (!storageFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        for (String idText : config.getStringList("parties.ids")) {
            try { UUID id=UUID.fromString(idText); UUID leader=UUID.fromString(config.getString("parties."+id+".leader","")); Set<UUID> members=new java.util.LinkedHashSet<>(); for(String member:config.getStringList("parties."+id+".members")) members.add(UUID.fromString(member)); if(members.isEmpty()||!members.contains(leader)||members.size()>Party.MAX_MEMBERS)continue; Party party=new Party(id,leader,members); partiesById.put(id,party); for(UUID member:party.getMembers())partyIdByMember.put(member,id); }
            catch(IllegalArgumentException ignored){plugin.getLogger().warning("Ignoring invalid persisted party entry: "+idText);}
        }
    }

    /** Marks the current state revision dirty and schedules at most one persistence worker. */
    private synchronized void save() {
        if (shuttingDown || persistenceExecutor.isShutdown()) return;
        stateRevision++;
        if (saveWorkerScheduled) return;
        saveWorkerScheduled = true;
        persistenceExecutor.execute(this::drainSaves);
    }

    /** Writes the newest party snapshot and skips obsolete intermediate persistence states. */
    private void drainSaves() {
        while (true) {
            final YamlConfiguration snapshot;
            final long capturedRevision;
            synchronized (this) {
                capturedRevision = stateRevision;
                snapshot = createSnapshot();
            }
            writeAtomically(snapshot);
            synchronized (this) {
                if (capturedRevision == stateRevision || shuttingDown) {
                    saveWorkerScheduled = false;
                    return;
                }
            }
        }
    }

    private YamlConfiguration createSnapshot() {
        YamlConfiguration snapshot = new YamlConfiguration(); List<String> ids = new ArrayList<>();
        for (Party party : partiesById.values()) { ids.add(party.getId().toString()); String path="parties."+party.getId(); snapshot.set(path+".leader",party.getLeader().toString()); snapshot.set(path+".members",party.getMembers().stream().map(UUID::toString).toList()); }
        snapshot.set("parties.ids",ids); return snapshot;
    }

    private void writeAtomically(YamlConfiguration snapshot){Path target=storageFile.toPath();Path temporary=target.resolveSibling(storageFile.getName()+".tmp");try{storageFile.getParentFile().mkdirs();snapshot.save(temporary.toFile());try{Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException exception){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException exception){try{Files.deleteIfExists(temporary);}catch(IOException ignored){}plugin.getLogger().warning("Could not save parties.yml: "+exception.getMessage());}}

    @Override public void close(){ shutdown(); }

    public void shutdown(){
        synchronized (this) {
            if (shuttingDown) return;
            shuttingDown=true;
        }
        inviteWakeScheduler.clear(); emptyPartyWakeScheduler.clear(); persistenceExecutor.shutdown();
        try{if(!persistenceExecutor.awaitTermination(10,TimeUnit.SECONDS))persistenceExecutor.shutdownNow();}catch(InterruptedException exception){persistenceExecutor.shutdownNow();Thread.currentThread().interrupt();}
        pendingInvites.clear(); partyIdByMember.clear(); partiesById.clear();
    }
    @Override public boolean isInParty(UUID uuid){return getParty(uuid).map(p->p.getMembers().size()>1).orElse(false);}
    @Override public Set<UUID> getPartyMembers(UUID uuid){return getParty(uuid).map(p->Set.copyOf(p.getMembers())).orElse(Set.of(uuid));}
    @Override public UUID getPartyLeader(UUID uuid){return getParty(uuid).map(Party::getLeader).orElse(uuid);}
    @Override public boolean isLeader(UUID uuid){return getParty(uuid).map(p->p.isLeader(uuid)).orElse(false);}
    @Override public boolean isWithinShareRange(UUID source,UUID target){Player a=Bukkit.getPlayer(source);Player b=Bukkit.getPlayer(target);if(a==null||b==null||!a.isOnline()||!b.isOnline()||!a.getWorld().equals(b.getWorld()))return false;return a.getLocation().distanceSquared(b.getLocation())<=SHARE_RANGE*SHARE_RANGE;}
    @Override public double getShareRange(){return SHARE_RANGE;}
    private record PendingInvite(UUID leader,long expiresAt){}
}
