package de.pixelrpg.rpg.stats;

import de.pixelrpg.rpg.api.CharacterStatType;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.core.StatisticType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatisticsService implements StatisticsAPI {
    private static final double BASE_HP = 100.0D;

    private final Map<UUID, EnumMap<StatisticType, Long>> stats = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> customStats = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<CharacterStatType, Double>> characterStats = new ConcurrentHashMap<>();

    @Override
    public long getStatistic(UUID id, StatisticType type) {
        if (id == null || type == null) {
            return 0L;
        }
        return stats.getOrDefault(id, new EnumMap<>(StatisticType.class)).getOrDefault(type, 0L);
    }

    @Override
    public void recordStatistic(UUID id, StatisticType type, long amount) {
        if (id == null || type == null || amount == 0L) {
            return;
        }
        stats.computeIfAbsent(id, ignored -> new EnumMap<>(StatisticType.class))
                .merge(type, amount, StatisticsService::saturatingAdd);
    }

    @Override
    public long getCustomStatistic(UUID id, String key) {
        if (id == null || key == null || key.isBlank()) {
            return 0L;
        }
        return customStats.getOrDefault(id, Map.of()).getOrDefault(normalize(key), 0L);
    }

    @Override
    public void recordCustomStatistic(UUID id, String key, long amount) {
        if (id == null || key == null || key.isBlank() || amount == 0L) {
            return;
        }
        customStats.computeIfAbsent(id, ignored -> new ConcurrentHashMap<>())
                .merge(normalize(key), amount, StatisticsService::saturatingAdd);
    }

    @Override
    public double getCharacterStat(UUID id, CharacterStatType type) {
        if (id == null || type == null) {
            return 0.0D;
        }
        if (type == CharacterStatType.HP) {
            return characterStats.getOrDefault(id, new EnumMap<>(CharacterStatType.class))
                    .getOrDefault(type, BASE_HP);
        }
        return characterStats.getOrDefault(id, new EnumMap<>(CharacterStatType.class))
                .getOrDefault(type, 0.0D);
    }

    public void setCharacterStat(UUID id, CharacterStatType type, double value) {
        if (id == null || type == null || !Double.isFinite(value)) {
            return;
        }
        characterStats.computeIfAbsent(id, ignored -> new EnumMap<>(CharacterStatType.class))
                .put(type, Math.max(0.0D, value));
    }

    public void clear(UUID id) {
        if (id == null) {
            return;
        }
        stats.remove(id);
        customStats.remove(id);
        characterStats.remove(id);
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static long saturatingAdd(long current, long amount) {
        if (amount > 0L && current > Long.MAX_VALUE - amount) {
            return Long.MAX_VALUE;
        }
        if (amount < 0L && current < Long.MIN_VALUE - amount) {
            return Long.MIN_VALUE;
        }
        return current + amount;
    }
}
