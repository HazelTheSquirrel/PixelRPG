// src/main/java/de/pixelrpg/rpg/party/PartyManager.java
package de.pixelrpg.rpg.party;

import de.pixelrpg.rpg.api.PartyAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyManager implements PartyAPI {

    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyIdByMember = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    public Optional<Party> getParty(UUID member) {
        UUID partyId = partyIdByMember.get(member);
        return partyId != null ? Optional.ofNullable(partiesById.get(partyId)) : Optional.empty();
    }

    public Party createParty(UUID leader) {
        Party existing = getParty(leader).orElse(null);
        if (existing != null) {
            disbandParty(existing);
        }

        UUID id = UUID.randomUUID();
        Party party = new Party(id, leader);
        partiesById.put(id, party);
        partyIdByMember.put(leader, id);
        return party;
    }

    public void addInvite(UUID target, UUID leader) {
        pendingInvites.put(target, leader);
    }

    public Optional<UUID> getInviteLeader(UUID target) {
        return Optional.ofNullable(pendingInvites.get(target));
    }

    public void removeInvite(UUID target) {
        pendingInvites.remove(target);
    }

    public boolean acceptInvite(Player player) {
        UUID leaderUuid = pendingInvites.remove(player.getUniqueId());
        if (leaderUuid == null) {
            return false;
        }

        Party party = getParty(leaderUuid).orElse(null);
        if (party == null || party.isFull()) {
            return false;
        }
        if (getParty(player.getUniqueId()).isPresent()) {
            return false;
        }

        party.addMember(player.getUniqueId());
        partyIdByMember.put(player.getUniqueId(), party.getId());

        Component joinMessage = Component.text(player.getName() + " joined the party!", NamedTextColor.GREEN);
        broadcastToParty(party, joinMessage, player.getUniqueId());
        return true;
    }

    public void leaveParty(Player player) {
        Party party = getParty(player.getUniqueId()).orElse(null);
        if (party == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean wasLeader = party.isLeader(uuid);

        party.removeMember(uuid);
        partyIdByMember.remove(uuid);

        if (party.isEmpty()) {
            partiesById.remove(party.getId());
            return;
        }

        if (wasLeader) {
            UUID newLeader = party.promoteNextLeader();
            if (newLeader != null) {
                Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
                if (newLeaderPlayer != null) {
                    newLeaderPlayer.sendMessage(Component.text("You are now the party leader.", NamedTextColor.GOLD));
                }
            }
        }

        broadcastToParty(party, Component.text(player.getName() + " left the party.", NamedTextColor.RED), null);
    }

    public void disbandParty(Party party) {
        for (UUID member : new HashSet<>(party.getMembers())) {
            partyIdByMember.remove(member);
            Player memberPlayer = Bukkit.getPlayer(member);
            if (memberPlayer != null) {
                memberPlayer.sendMessage(Component.text("The party has been disbanded.", NamedTextColor.RED));
            }
        }
        partiesById.remove(party.getId());
    }

    public void handleDisconnect(UUID uuid) {
        Party party = getParty(uuid).orElse(null);
        if (party == null) {
            return;
        }

        boolean wasLeader = party.isLeader(uuid);
        party.removeMember(uuid);
        partyIdByMember.remove(uuid);

        if (party.isEmpty()) {
            partiesById.remove(party.getId());
            return;
        }

        if (wasLeader) {
            UUID newLeader = party.promoteNextLeader();
            if (newLeader != null) {
                Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
                if (newLeaderPlayer != null) {
                    newLeaderPlayer.sendMessage(Component.text("You are now the party leader.", NamedTextColor.GOLD));
                }
            }
        }
    }

    public void broadcastToParty(Party party, Component message, UUID exclude) {
        for (UUID member : party.getMembers()) {
            if (exclude != null && exclude.equals(member)) {
                continue;
            }
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    // PartyAPI

    @Override
    public boolean isInParty(UUID uuid) {
        Optional<Party> party = getParty(uuid);
        return party.isPresent() && party.get().getMembers().size() > 1;
    }

    @Override
    public Set<UUID> getPartyMembers(UUID uuid) {
        return getParty(uuid).map(party -> (Set<UUID>) new HashSet<>(party.getMembers())).orElse(Set.of(uuid));
    }
}