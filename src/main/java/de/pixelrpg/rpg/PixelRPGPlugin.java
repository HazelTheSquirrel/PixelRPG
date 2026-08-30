package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.boss.BiomeBossSpawnTask;
import de.pixelrpg.rpg.boss.BossAttackPatternRegistry;
import de.pixelrpg.rpg.boss.BossCombustListener;
import de.pixelrpg.rpg.boss.BossDamageContributionListener;
import de.pixelrpg.rpg.boss.BossDeathListener;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.boss.patterns.EnrageBuffPattern;
import de.pixelrpg.rpg.boss.patterns.ProjectileVolleyPattern;
import de.pixelrpg.rpg.boss.patterns.SlamAttackPattern;
import de.pixelrpg.rpg.boss.patterns.SummonAddsPattern;
import de.pixelrpg.rpg.command.PaperBasicCommandAdapter;
import de.pixelrpg.rpg.command.RootCommand;
import de.pixelrpg.rpg.command.impl.BossSubCommand;
import de.pixelrpg.rpg.command.impl.CompanionSubCommand;
import de.pixelrpg.rpg.command.impl.NpcSubCommand;
import de.pixelrpg.rpg.command.impl.PartySubCommand;
import de.pixelrpg.rpg.command.impl.QuestAdminSubCommand;
import de.pixelrpg.rpg.command.impl.QuestLogCommand;
import de.pixelrpg.rpg.command.impl.ShopSubCommand;
import de.pixelrpg.rpg.combat.CombatDamageListener;
import de.pixelrpg.rpg.combat.MobExperienceListener;
import de.pixelrpg.rpg.combat.SoulboundDeathListener;
import de.pixelrpg.rpg.combat.loot.LootDropListener;
import de.pixelrpg.rpg.combat.scaling.MobLevelScalingListener;
import de.pixelrpg.rpg.combat.scaling.MobNameplateListener;
import de.pixelrpg.rpg.combat.scaling.MobNameplateService;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.combat.skill.SkillInputListener;
import de.pixelrpg.rpg.combat.skill.WeaponAbilityEngine;
import de.pixelrpg.rpg.companion.CompanionExperienceListener;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.dialogue.DialogueCommand;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogListener;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.economy.GuildCurrencyPickupListener;
import de.pixelrpg.rpg.equipment.EquipmentService;
import de.pixelrpg.rpg.gui.CraftingGUI;
import de.pixelrpg.rpg.gui.GUIListener;
import de.pixelrpg.rpg.gui.ShopEditorGUI;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.npc.NpcBehaviorRegistry;
import de.pixelrpg.rpg.npc.NpcChunkListener;
import de.pixelrpg.rpg.npc.NpcInteractListener;
import de.pixelrpg.rpg.npc.NpcLookTask;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.behavior.BankerBehavior;
import de.pixelrpg.rpg.npc.behavior.FillerBehavior;
import de.pixelrpg.rpg.npc.behavior.ProfessionTrainerBehavior;
import de.pixelrpg.rpg.npc.behavior.QuestBehavior;
import de.pixelrpg.rpg.npc.behavior.ReceptionBehavior;
import de.pixelrpg.rpg.npc.behavior.ShopBehavior;
import de.pixelrpg.rpg.npc.behavior.StoryBehavior;
import de.pixelrpg.rpg.npc.behavior.TravelBehavior;
import de.pixelrpg.rpg.party.PartyDisconnectListener;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileLifecycleListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.quest.GlobalEventState;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestMobKillListener;
import de.pixelrpg.rpg.quest.QuestPassiveCheckTask;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.scoreboard.PlaytimeTracker;
import de.pixelrpg.rpg.scoreboard.ScoreboardService;
import de.pixelrpg.rpg.shop.ShopManager;
import de.pixelrpg.rpg.stats.MobKillStatisticListener;
import de.pixelrpg.rpg.stats.PlayerDeathStatisticListener;
import de.pixelrpg.rpg.stats.QuestBossStatisticListener;
import de.pixelrpg.rpg.stats.RPGStatsListener;
import de.pixelrpg.rpg.stats.StatEngine;
import de.pixelrpg.rpg.stats.StatisticsService;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryManager;
import de.pixelrpg.rpg.travel.GuildCompassListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelRPGPlugin extends JavaPlugin {
    private static PixelRPGPlugin instance;
    private PlayerProfileManager playerProfileManager;
    private StatEngine statEngine;
    private ProfessionSystem professionSystem;
    private ItemEconomyConfig itemEconomyConfig;
    private ItemService itemService;
    private EquipmentService equipmentService;
    private CraftingGUI craftingGUI;
    private MobScalingConfig mobScalingConfig;
    private MobNameplateService mobNameplateService;
    private MobLevelScalingListener mobLevelScalingListener;
    private NpcManager npcManager;
    private NpcBehaviorRegistry npcBehaviorRegistry;
    private NpcLookTask npcLookTask;
    private ShopManager shopManager;
    private ShopEditorGUI shopEditorGUI;
    private StoryManager storyManager;
    private PartyManager partyManager;
    private QuestRepository questRepository;
    private QuestManager questManager;
    private QuestPassiveCheckTask questPassiveCheckTask;
    private GlobalEventState globalEventState;
    private BossRepository bossRepository;
    private BossManager bossManager;
    private BiomeBossSpawnTask biomeBossSpawnTask;
    private StatisticsService statisticsService;
    private ScoreboardService scoreboardService;
    private PlaytimeTracker playtimeTracker;
    private CompanionService companionService;
    private CombatDamageListener combatDamageListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RPGKeys.init(this);
        StoryBookFactory.load(getConfig());
        GuildCurrencyItemFactory.configureMaxStackSize(getConfig().getInt("economy.currency.max-stack-size", 64));
        playerProfileManager = new PlayerProfileManager(this);
        playerProfileManager.initialize(getConfig());
        getServer().getPluginManager().registerEvents(new PlayerProfileLifecycleListener(playerProfileManager), this);
        statEngine = new StatEngine(playerProfileManager);
        professionSystem = new ProfessionSystem(this, playerProfileManager);
        professionSystem.register();
        WeaponAbilityEngine weaponAbilityEngine = new WeaponAbilityEngine(playerProfileManager, statEngine);
        itemEconomyConfig = new ItemEconomyConfig();
        itemEconomyConfig.load(getConfig());
        RPGItemBuilder.configureScaling(this);
        itemService = new ItemService(this);
        equipmentService = new EquipmentService(playerProfileManager, statEngine, itemService);
        Bukkit.getServicesManager().register(de.pixelrpg.rpg.api.ItemAPI.class, itemService, this, ServicePriority.Normal);
        craftingGUI = new CraftingGUI(professionSystem.craftingService(), playerProfileManager);
        mobScalingConfig = new MobScalingConfig();
        mobScalingConfig.load(getConfig());
        mobNameplateService = new MobNameplateService(this, mobScalingConfig);
        shopManager = new ShopManager(this);
        shopManager.load();
        shopEditorGUI = new ShopEditorGUI(shopManager);
        storyManager = new StoryManager(this, playerProfileManager);
        storyManager.load();
        partyManager = new PartyManager(this);
        Bukkit.getServicesManager().register(de.pixelrpg.rpg.api.PartyAPI.class, partyManager, this, ServicePriority.Normal);
        questRepository = new QuestRepository(this);
        questRepository.load();
        globalEventState = new GlobalEventState(this);
        globalEventState.load();
        questManager = new QuestManager(this, questRepository, playerProfileManager, playerProfileManager, globalEventState, partyManager.getShareRange());
        questManager.startTimerCheckTask();

        BossAttackPatternRegistry patternRegistry = new BossAttackPatternRegistry();
        patternRegistry.register(new SlamAttackPattern());
        patternRegistry.register(new SummonAddsPattern());
        patternRegistry.register(new ProjectileVolleyPattern());
        patternRegistry.register(new EnrageBuffPattern());
        bossRepository = new BossRepository(this);
        bossRepository.load();
        bossManager = new BossManager(this, patternRegistry, playerProfileManager, partyManager, playerProfileManager,
                itemService, mobScalingConfig,
                getConfig().getDouble("bosses.bar-radius", 60.0D),
                getConfig().getInt("bosses.bar-update-interval-ticks", 20),
                getConfig().getInt("bosses.phase-check-interval-ticks", 10));
        biomeBossSpawnTask = new BiomeBossSpawnTask(this, bossRepository, bossManager,
                getConfig().getDouble("bosses.biome-spawn.spawn-radius", 80.0D),
                getConfig().getInt("bosses.biome-spawn.check-interval-seconds", 60),
                getConfig().getInt("bosses.biome-spawn.max-concurrent", 4));
        biomeBossSpawnTask.start();

        statisticsService = new StatisticsService(playerProfileManager);
        Bukkit.getServicesManager().register(StatisticsAPI.class, statisticsService, this, ServicePriority.Normal);
        scoreboardService = new ScoreboardService(this, playerProfileManager, getConfig().getInt("scoreboard.update-interval-ticks", 20));
        scoreboardService.startTask();
        playtimeTracker = new PlaytimeTracker(this, playerProfileManager);
        playtimeTracker.startAutosaveTask(getConfig().getInt("statistics.autosave-interval-ticks", 6000));
        npcManager = new NpcManager(this);
        npcManager.loadAll();
        getServer().getPluginManager().registerEvents(new NpcChunkListener(npcManager), this);
        npcLookTask = new NpcLookTask(this, npcManager, getConfig().getDouble("npc.look-radius", 8.0), getConfig().getInt("npc.look-interval-ticks", 5));
        npcLookTask.start();
        DialogueEngine dialogueEngine = new DialogueEngine();
        StoryNpcDialogue storyNpcDialogue = new StoryNpcDialogue(playerProfileManager, dialogueEngine);
        QuickActionsDialogService quickActions = new QuickActionsDialogService(playerProfileManager, statEngine, questManager, itemService);
        companionService = new CompanionService(this);
        npcBehaviorRegistry = new NpcBehaviorRegistry();
        npcBehaviorRegistry.register(new ReceptionBehavior(playerProfileManager, dialogueEngine, partyManager));
        npcBehaviorRegistry.register(new QuestBehavior(questManager, playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new ShopBehavior(shopManager, playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new TravelBehavior(npcManager, playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new StoryBehavior(storyManager, storyNpcDialogue, dialogueEngine, playerProfileManager));
        npcBehaviorRegistry.register(new BankerBehavior(playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new FillerBehavior(questManager, playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new ProfessionTrainerBehavior(NpcType.PROFESSION_BLACKSMITH, Profession.BLACKSMITH,
                playerProfileManager, professionSystem.professionService(), dialogueEngine, quickActions));
        npcBehaviorRegistry.register(new ProfessionTrainerBehavior(NpcType.PROFESSION_PROVISIONER, Profession.PROVISIONER,
                playerProfileManager, professionSystem.professionService(), dialogueEngine, quickActions));
        npcBehaviorRegistry.register(new ProfessionTrainerBehavior(NpcType.PROFESSION_SCHOLAR, Profession.SCHOLAR,
                playerProfileManager, professionSystem.professionService(), dialogueEngine, quickActions));
        npcBehaviorRegistry.register(new ProfessionTrainerBehavior(NpcType.PROFESSION_ALCHEMIST, Profession.ALCHEMIST,
                playerProfileManager, professionSystem.professionService(), dialogueEngine, quickActions));

        getServer().getPluginManager().registerEvents(equipmentService, this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(craftingGUI, this);
        getServer().getPluginManager().registerEvents(new RPGStatsListener(statEngine, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(weaponAbilityEngine), this);
        getServer().getPluginManager().registerEvents(shopEditorGUI, this);
        getServer().getPluginManager().registerEvents(new LootDropListener(playerProfileManager, itemEconomyConfig), this);
        mobLevelScalingListener = new MobLevelScalingListener(this, playerProfileManager, mobScalingConfig);
        getServer().getPluginManager().registerEvents(mobLevelScalingListener, this);
        mobLevelScalingListener.start();
        getServer().getPluginManager().registerEvents(new MobNameplateListener(mobNameplateService, playerProfileManager), this);
        combatDamageListener = new CombatDamageListener(playerProfileManager, playerProfileManager, statEngine);
        getServer().getPluginManager().registerEvents(combatDamageListener, this);
        getServer().getPluginManager().registerEvents(new BossDamageContributionListener(bossManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new BossCombustListener(), this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(playerProfileManager, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new NpcInteractListener(npcManager, npcBehaviorRegistry, questManager), this);
        getServer().getPluginManager().registerEvents(new QuestMobKillListener(questManager), this);
        getServer().getPluginManager().registerEvents(new PartyDisconnectListener(partyManager), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(bossManager), this);
        getServer().getPluginManager().registerEvents(new MobKillStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(new QuestBossStatisticListener(statisticsService), this);
        getServer().getPluginManager().registerEvents(new GuildCurrencyPickupListener(playerProfileManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new GuildCompassListener(npcManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new SoulboundDeathListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(scoreboardService, this);
        getServer().getPluginManager().registerEvents(playtimeTracker, this);
        getServer().getPluginManager().registerEvents(new QuickActionsDialogListener(quickActions, companionService), this);
        getServer().getPluginManager().registerEvents(new CompanionExperienceListener(companionService), this);
        questPassiveCheckTask = new QuestPassiveCheckTask(this, questManager);
        questPassiveCheckTask.start();

        RootCommand rootCommand = new RootCommand(this, itemService);
        rootCommand.register(new CompanionSubCommand(companionService));
        rootCommand.register(new NpcSubCommand(npcManager));
        rootCommand.register(new ShopSubCommand(shopManager, shopEditorGUI, npcManager));
        rootCommand.register(new QuestAdminSubCommand(questManager));
        rootCommand.register(new BossSubCommand(bossRepository, bossManager));
        PartySubCommand partyCommand = new PartySubCommand(partyManager, playerProfileManager);
        PaperBasicCommandAdapter rpgAdminCommand = new PaperBasicCommandAdapter("pixelrpgadmin", rootCommand, rootCommand, "rpg.admin");
        PaperBasicCommandAdapter partyAdapter = new PaperBasicCommandAdapter("pixelrpgparty", partyCommand, partyCommand, "rpg.member");
        PaperBasicCommandAdapter questLogAdapter = new PaperBasicCommandAdapter("pixelrpgquestlog", new QuestLogCommand(questManager, playerProfileManager), null, "rpg.member");
        PaperBasicCommandAdapter dialogueAdapter = new PaperBasicCommandAdapter("pixelrpgdialogue", new DialogueCommand(playerProfileManager, dialogueEngine), null, "rpg.member");

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("pixelrpgadmin", rpgAdminCommand);
            event.registrar().register("pixelrpgparty", partyAdapter);
            event.registrar().register("pixelrpgquestlog", questLogAdapter);
            event.registrar().register("pixelrpgdialogue", dialogueAdapter);
        });
    }

    @Override
    public void onDisable() {
        if (questPassiveCheckTask != null) questPassiveCheckTask.stop();
        if (biomeBossSpawnTask != null) biomeBossSpawnTask.stop();
        if (mobLevelScalingListener != null) mobLevelScalingListener.shutdown();
        if (playtimeTracker != null) playtimeTracker.shutdown();
        if (scoreboardService != null) scoreboardService.shutdown();
        if (questManager != null) questManager.shutdown();
        if (bossManager != null) bossManager.shutdown();
        if (npcLookTask != null) npcLookTask.stop();
        if (playerProfileManager != null) playerProfileManager.shutdown();
        if (instance == this) instance = null;
    }

    public static PixelRPGPlugin getInstance() { return instance; }

    public PlayerProfileManager getPlayerProfileManager() { return playerProfileManager; }
    public StatEngine getStatEngine() { return statEngine; }
    public ProfessionSystem getProfessionSystem() { return professionSystem; }
    public ItemEconomyConfig getItemEconomyConfig() { return itemEconomyConfig; }
    public ItemService getItemService() { return itemService; }
    public EquipmentService getEquipmentService() { return equipmentService; }
    public NpcManager getNpcManager() { return npcManager; }
    public ShopManager getShopManager() { return shopManager; }
    public StoryManager getStoryManager() { return storyManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public QuestRepository getQuestRepository() { return questRepository; }
    public QuestManager getQuestManager() { return questManager; }
    public GlobalEventState getGlobalEventState() { return globalEventState; }
    public BossRepository getBossRepository() { return bossRepository; }
    public BossManager getBossManager() { return bossManager; }
    public StatisticsService getStatisticsService() { return statisticsService; }
    public ScoreboardService getScoreboardService() { return scoreboardService; }
    public CompanionService getCompanionService() { return companionService; }
}
