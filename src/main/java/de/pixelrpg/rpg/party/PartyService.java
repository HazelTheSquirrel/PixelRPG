package de.pixelrpg.rpg.party;
import de.pixelrpg.rpg.api.PartyAPI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class PartyService implements PartyAPI {
 private final Map<UUID,Set<UUID>> members=new ConcurrentHashMap<>(); private final Map<UUID,UUID> leaders=new ConcurrentHashMap<>(); private final double range;
 public PartyService(double range){this.range=Math.max(0,range);}
 public boolean isInParty(UUID id){return members.containsKey(id);} public Set<UUID> getPartyMembers(UUID id){return Set.copyOf(members.getOrDefault(id,Set.of()));} public UUID getPartyLeader(UUID id){return leaders.get(id);} public boolean isLeader(UUID id){return id.equals(leaders.get(id));} public boolean isWithinShareRange(UUID a,UUID b){return false;} public double getShareRange(){return range;}
}
