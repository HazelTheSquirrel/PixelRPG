package de.pixelrpg.rpg.guild;
import de.pixelrpg.rpg.api.GuildAPI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class GuildService implements GuildAPI {
 private final Map<UUID,Long> xp=new ConcurrentHashMap<>();
 public boolean isRegistered(UUID id){return xp.containsKey(id);} public int getLevel(UUID id){return Math.max(1,(int)Math.floor(Math.sqrt(getExperience(id)/100.0D))+1);} public long getExperience(UUID id){return xp.getOrDefault(id,0L);} public void addExperience(UUID id,long amount){if(amount<0)throw new IllegalArgumentException("amount must be non-negative");xp.merge(id,amount,Long::sum);}
}
