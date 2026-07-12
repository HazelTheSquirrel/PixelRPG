// src/main/java/de/pixelrpg/rpg/api/PixelRPGProvider.java
package de.pixelrpg.rpg.api;

import org.bukkit.Bukkit;

public final class PixelRPGProvider {

    private PixelRPGProvider() {
    }

    public static int getApiVersion() {
        return ApiVersion.CURRENT;
    }

    public static GuildAPI getGuildAPI() {
        return require(GuildAPI.class);
    }

    public static EconomyAPI getEconomyAPI() {
        return require(EconomyAPI.class);
    }

    public static ItemAPI getItemAPI() {
        return require(ItemAPI.class);
    }

    public static PartyAPI getPartyAPI() {
        return require(PartyAPI.class);
    }

    public static StatisticsAPI getStatisticsAPI() {
        return require(StatisticsAPI.class);
    }

    public static AchievementAPI getAchievementAPI() {
        return require(AchievementAPI.class);
    }

    public static TitleAPI getTitleAPI() {
        return require(TitleAPI.class);
    }

    private static <T> T require(Class<T> type) {
        T service = Bukkit.getServicesManager().load(type);
        if (service == null) {
            throw new IllegalStateException("PixelRPG " + type.getSimpleName() + " is not registered yet.");
        }
        return service;
    }
}