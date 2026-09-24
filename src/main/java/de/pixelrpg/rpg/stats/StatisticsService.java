package de.pixelrpg.rpg.stats;
import de.pixelrpg.rpg.api.*; import de.pixelrpg.rpg.core.StatisticType; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class StatisticsService implements StatisticsAPI {
 private final Map<UUID,EnumMap<StatisticType,Long>> stats=new ConcurrentHashMap<>();
 public long getStatistic(UUID id,StatisticType t){return stats.getOrDefault(id,new EnumMap<>(StatisticType.class)).getOrDefault(t,0L);}
 public void recordStatistic(UUID id,StatisticType t,long n){stats.computeIfAbsent(id,k->new EnumMap<>(StatisticType.class)).merge(t,n,Long::sum);}
 public long getCustomStatistic(UUID id,String key){return 0;}
 public void recordCustomStatistic(UUID id,String key,long n){}
 public double getCharacterStat(UUID id,CharacterStatType t){return t==CharacterStatType.HP?20:0;}
}
