package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.FoodService;
import de.pixelrpg.rpg.party.PartyService;
import de.pixelrpg.rpg.player.PlayerProfileLifecycleListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatisticsService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelRPGPlugin extends JavaPlugin {
    private PlayerProfileManager profiles;
    private PartyService partyService;
    private StatisticsService statisticsService;
    private ItemService itemService;
    private FoodService foodService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RPGKeys.init(this);

        profiles = new PlayerProfileManager(this);
        profiles.initialize(getConfig());
        getServer().getPluginManager().registerEvents(
                new PlayerProfileLifecycleListener(profiles), this);

        partyService = new PartyService(getConfig().getDouble("quests.party-share-range", 24.0D));
        statisticsService = new StatisticsService();
        foodService = new FoodService(this);
        itemService = new ItemService(this, foodService);
        getServer().getPluginManager().registerEvents(foodService, this);

        register(PartyAPI.class, partyService);
        register(StatisticsAPI.class, statisticsService);
        register(ItemAPI.class, itemService);

        getLogger().info("PixelRPG clean-rebuild core enabled.");
    }

    @Override
    public void onDisable() {
        unregister(PartyAPI.class, partyService);
        unregister(StatisticsAPI.class, statisticsService);
        unregister(ItemAPI.class, itemService);
        if (profiles != null) {
            profiles.shutdown();
            profiles = null;
        }
        partyService = null;
        statisticsService = null;
        itemService = null;
        foodService = null;
    }

    private <T> void register(Class<T> type, T service) {
        Bukkit.getServicesManager().register(type, service, this, ServicePriority.Normal);
    }

    private <T> void unregister(Class<T> type, T service) {
        if (service != null) {
            Bukkit.getServicesManager().unregister(type, service);
        }
    }

    public PlayerProfileManager getPlayerProfileManager() {
        return profiles;
    }

    public PartyService getPartyService() {
        return partyService;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public ItemService getItemService() {
        return itemService;
    }
}
