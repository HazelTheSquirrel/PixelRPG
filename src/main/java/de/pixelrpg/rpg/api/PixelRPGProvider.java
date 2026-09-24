package de.pixelrpg.rpg.api;
import org.bukkit.Bukkit;
public final class PixelRPGProvider {
    private PixelRPGProvider(){}
    public static int getApiVersion(){return ApiVersion.CURRENT;}
    public static GuildAPI getGuildAPI(){return require(GuildAPI.class);}
    public static EconomyAPI getEconomyAPI(){return require(EconomyAPI.class);}
    public static ItemAPI getItemAPI(){return require(ItemAPI.class);}
    public static PartyAPI getPartyAPI(){return require(PartyAPI.class);}
    public static StatisticsAPI getStatisticsAPI(){return require(StatisticsAPI.class);}
    private static <T>T require(Class<T> type){T value=Bukkit.getServicesManager().load(type);if(value==null)throw new IllegalStateException("PixelRPG "+type.getSimpleName()+" is not registered yet.");return value;}
}
