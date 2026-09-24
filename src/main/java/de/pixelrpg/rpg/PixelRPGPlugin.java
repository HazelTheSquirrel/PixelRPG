package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.bank.BankStorageService;
import de.pixelrpg.rpg.trade.TradeDepotManager;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.equipment.EquipmentService;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.gui.GUIListener;
import de.pixelrpg.rpg.npc.NpcChunkListener;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.item.FoodService;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.party.PartyDisconnectListener;
import de.pixelrpg.rpg.party.PartyCommand;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.quest.QuestLifecycleListener;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.quest.QuestService;
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
    private QuestRepository questRepository;
    private QuestService questService;
    private de.pixelrpg.rpg.guild.GuildManager guildManager;
    private BankStorageService bankStorageService;
    private TradeDepotManager tradeDepotManager;
    private NpcManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RPGKeys.init(this);

        profiles = new PlayerProfileManager(this);
        profiles.initialize(getConfig());
        getServer().getPluginManager().registerEvents(
                new PlayerProfileLifecycleListener(profiles), this);

        guildManager = new de.pixelrpg.rpg.guild.GuildManager(this, profiles);

        partyManager = new PartyManager(this);
        getServer().getPluginManager().registerEvents(new PartyDisconnectListener(partyManager), this);
        statEngine = new de.pixelrpg.rpg.stats.StatEngine(profiles);
        equipmentService = new EquipmentService(profiles, statEngine);
        statisticsService = new StatisticsService(statEngine);
        foodService = new FoodService(this);
        itemService = new ItemService(this, foodService);
        bankStorageService = new BankStorageService(this);
        tradeDepotManager = new TradeDepotManager(this, profiles, bankStorageService, itemService);
        npcManager = new NpcManager(this);
        npcManager.load();
        getServer().getPluginManager().registerEvents(new NpcChunkListener(npcManager), this);
        professionSystem = new ProfessionSystem(this, profiles);
        professionSystem.register();
        questRepository = new QuestRepository(this);
        questRepository.load();
        questService = new QuestService(this, questRepository, profiles, itemService);
        getServer().getPluginManager().registerEvents(new QuestLifecycleListener(questService, profiles), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(foodService, this);
        getServer().getPluginManager().registerEvents(equipmentService, this);

        register(PartyAPI.class, partyManager);
        getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> {
                    event.registrar().register("pixelrpgparty", new PartyCommand(partyManager, profiles));
                    event.registrar().register("pixelrpgquestlog", new de.pixelrpg.rpg.quest.QuestLogCommand(questService, profiles));
                });
        register(StatisticsAPI.class, statisticsService);
        register(ItemAPI.class, itemService);

        getLogger().info("PixelRPG clean-rebuild core enabled.");
    }

    @Override
    public void onDisable() {
        unregister(PartyAPI.class, partyManager);
        if (npcManager != null) npcManager.shutdown();
        if (tradeDepotManager != null) tradeDepotManager.shutdown();
        if (bankStorageService != null) bankStorageService.close();
        if (guildManager != null) guildManager.shutdown();
        if (partyManager != null) partyManager.shutdown();
        unregister(StatisticsAPI.class, statisticsService);
        unregister(ItemAPI.class, itemService);
        if (questService != null) questService.shutdown();
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
        questService = null;
        questRepository = null;
        guildManager = null;
        bankStorageService = null;
        tradeDepotManager = null;
        npcManager = null;
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

    public QuestService getQuestService() {
        return questService;
    }

    public de.pixelrpg.rpg.guild.GuildManager getGuildManager() {
        return guildManager;
    }

    public BankStorageService getBankStorageService() {
        return bankStorageService;
    }

    public TradeDepotManager getTradeDepotManager() {
        return tradeDepotManager;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }
}
