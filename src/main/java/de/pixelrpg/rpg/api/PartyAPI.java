package de.pixelrpg.rpg.api;
import java.util.Set;
import java.util.UUID;
public interface PartyAPI { boolean isInParty(UUID uuid); Set<UUID> getPartyMembers(UUID uuid); UUID getPartyLeader(UUID uuid); boolean isLeader(UUID uuid); boolean isWithinShareRange(UUID source,UUID target); double getShareRange(); }
