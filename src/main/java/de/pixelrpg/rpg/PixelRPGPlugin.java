package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.bank.BankStorageService;
import de.pixelrpg.rpg.trade.TradeDepotManager;
import de.pixelrpg.rpg.shop.ShopManager;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryManager;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehaviorRegistry;
import de.pixelrpg.rpg.npc.NpcInteractListener;
import de.pixelrpg.rpg.npc.behavior.FillerBehavior;
import de.pixelrpg.rpg.npc.behavior.ShopBehavior;
import de.pixelrpg.rpg.npc.behavior.StoryBehavior;
import de.pixelrpg.rpg.npc.behavior.TravelBehavior;
import de.pixelrpg.rpg.combat.CombatDamageListener;
import de.pixelrpg.rpg.combat.MobExperienceListener;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.gui.ShopEditorGUI;
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
    private ShopManager shopManager;
    private ShopEditorGUI shopEditorGUI;
    private StoryManager storyManager;
    private NpcBehaviorRegistry npcBehaviorRegistry;
    private MobScalingConfig mobScalingConfig;
    private CombatDamageListener combatDamageListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RPGKeys.init(this);
        StoryBookFactory.load(getConfig());

        profiles = new PlayerProfileManager(this);
        profiles.initialize(getConfig());
        getServer().getPluginManager().registerEvents(
                new PlayerProfileLifecycleListener(profiles), this);

        guildManager = new de.pixelrpg.rpg.guild.GuildManager(this, profiles);

        partyManager = new PartyManager(this);
        getServer().getPluginManager().registerEvents(new PartyDisconnectListener(partyManager), this);
        statEngine = new de.pixelrpg.rpg.stats.StatEngine(profiles);
        equipmentService = new EquipmentService(this, profiles, statEngine);
        statisticsService = new StatisticsService(statEngine);
        foodService = new FoodService(this);
        itemService = new ItemService(this, foodService);
        bankStorageService = new BankStorageService(this);
        tradeDepotManager = new TradeDepotManager(this, profiles, bankStorageService, itemService);
        shopManager = new ShopManager(this);
        shopManager.load();
        shopEditorGUI = new ShopEditorGUI(shopManager);
        storyManager = new StoryManager(this, profiles);
        storyManager.load();
        npcManager = new NpcManager(this);
        npcManager.load();
        getServer().getPluginManager().registerEvents(new NpcChunkListener(npcManager), this);
        DialogueEngine dialogueEngine = new DialogueEngine();
        npcBehaviorRegistry = new NpcBehaviorRegistry();
        npcBehaviorRegistry.register(new ShopBehavior(shopManager, profiles, dialogueEngine));
        npcBehaviorRegistry.register(new StoryBehavior(storyManager, profiles, dialogueEngine));
        npcBehaviorRegistry.register(new TravelBehavior(npcManager, profiles, dialogueEngine));
        npcBehaviorRegistry.register(new FillerBehavior(questService, profiles, dialogueEngine));
        getServer().getPluginManager().registerEvents(new NpcInteractListener(npcManager, npcBehaviorRegistry, questService), this);
        professionSystem = new ProfessionSystem(this, profiles);
        professionSystem.register();
        questRepository = new QuestRepository(this);
        questRepository.load();
        questService = new QuestService(this, questRepository, profiles, itemService);
        mobScalingConfig = new MobScalingConfig();
        mobScalingConfig.load(getConfig());
        combatDamageListener = new CombatDamageListener(this, profiles, profiles, statEngine,
                getConfig().getDouble("combat.boss-max-hit-percent-of-max-hp", 0.12D));
        getServer().getPluginManager().registerEvents(combatDamageListener, this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(profiles, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new QuestLifecycleListener(questService, profiles), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(shopEditorGUI, this);
        getServer().getPluginManager().registerEvents(foodService, this);
        getServer().getPluginManager().registerEvents(equipmentService, this);

        register(PartyAPI.class, partyManager);
        getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> {
                    event.registrar().register("pixelrpgparty", new PartyCommand(partyManager, profiles));
                    event.registrar().register("pixelrpgquestlog", new de.pixelrpg.rpg.quest.QuestLogCommand(questService, profiles, itemService));
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
        if (shopManager != null) shopManager.shutdown();
        if (bankStorageService != null) bankStorageService.close();
        if (guildManager != null) guildManager.shutdown();
        if (partyManager != null) partyManager.shutdown();
        unregister(StatisticsAPI.class, statisticsService);
        unregister(ItemAPI.class, itemService);
        if (combatDamageListener != null) combatDamageListener.shutdown();
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
        npcBehaviorRegistry = null;
        mobScalingConfig = null;
        combatDamageListener = null;
        shopManager = null;
        storyManager = null;
        shopEditorGUI = null;
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
