package de.pixelrpg.rpg;
import de.pixelrpg.rpg.api.*; import de.pixelrpg.rpg.economy.EconomyService; import de.pixelrpg.rpg.guild.GuildService; import de.pixelrpg.rpg.item.ItemService; import de.pixelrpg.rpg.party.PartyService; import de.pixelrpg.rpg.player.*; import de.pixelrpg.rpg.stats.StatisticsService; import org.bukkit.Bukkit; import org.bukkit.plugin.ServicePriority; import org.bukkit.plugin.java.JavaPlugin;
public final class PixelRPGPlugin extends JavaPlugin {
 private PlayerProfileManager profiles;
 @Override public void onEnable(){saveDefaultConfig();profiles=new PlayerProfileManager(this);profiles.initialize(getConfig());getServer().getPluginManager().registerEvents(new PlayerProfileLifecycleListener(profiles),this);register(EconomyAPI.class,new EconomyService(profiles));register(GuildAPI.class,new GuildService());register(PartyAPI.class,new PartyService(getConfig().getDouble("quests.party-share-range",24)));register(StatisticsAPI.class,new StatisticsService());register(ItemAPI.class,new ItemService(this));getLogger().info("PixelRPG clean-rebuild foundation enabled.");}
 @Override public void onDisable(){if(profiles!=null)profiles.close();}
 private <T> void register(Class<T> type,T service){Bukkit.getServicesManager().register(type,service,this,ServicePriority.Normal);}
}
