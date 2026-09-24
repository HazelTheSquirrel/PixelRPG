package de.pixelrpg.rpg.api;
import de.pixelrpg.rpg.core.StatisticType;
import java.util.UUID;
public interface StatisticsAPI { long getStatistic(UUID uuid,StatisticType type); void recordStatistic(UUID uuid,StatisticType type,long amount); long getCustomStatistic(UUID uuid,String key); void recordCustomStatistic(UUID uuid,String key,long amount); double getCharacterStat(UUID uuid,CharacterStatType type); }
