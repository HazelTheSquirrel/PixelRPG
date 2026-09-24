package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.equipment.EquipmentService;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.FoodService;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.party.PartyDisconnectListener;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.player.PlayerProfileLifecycleListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatisticsService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelRPGPlugin extends JavaPlugin {
    private PlayerProfileManager profiles;
    private PartyManager partyManager;
    private StatisticsService statisticsService;
    private de.pixelrpg.rpg.stats.StatEngine statEngine;
    private EquipmentService equipmentService;
    private ItemService itemService;
    private FoodService foodService;
    private ProfessionSystem professionSystem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RPGKeys.init(this);

        profiles = new PlayerProfileManager(this);
        profiles.initialize(getConfig());
        getServer().getPluginManager().registerEvents(
                new PlayerProfileLifecycleListener(profiles), this);

        partyManager = new PartyManager(this);
        getServer().getPluginManager().registerEvents(new PartyDisconnectListener(partyManager), this);
        statEngine = new de.pixelrpg.rpg.stats.StatEngine(profiles);
        equipmentService = new EquipmentService(profiles, statEngine);
        statisticsService = new StatisticsService(statEngine);
        foodService = new FoodService(this);
        itemService = new ItemService(this, foodService);
        professionSystem = new ProfessionSystem(this, profiles);
        professionSystem.register();
        getServer().getPluginManager().registerEvents(foodService, this);
        getServer().getPluginManager().registerEvents(equipmentService, this);

        register(PartyAPI.class, partyManager);
        register(StatisticsAPI.class, statisticsService);
        register(ItemAPI.class, itemService);

        getLogger().info("PixelRPG clean-rebuild core enabled.");
    }

    @Override
    public void onDisable() {
        unregister(PartyAPI.class, partyManager);
        if (partyManager != null) partyManager.shutdown();
        unregister(StatisticsAPI.class, statisticsService);
        unregister(ItemAPI.class, itemService);
        if (profiles != null) {
            profiles.shutdown();
            profiles = null;
        }
        partyManager = null;
        statisticsService = null;
        statEngine = null;
        equipmentService = null;
        itemService = null;
        foodService = null;
        professionSystem = null;
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

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public ProfessionSystem getProfessionSystem() {
        return professionSystem;
    }
}
