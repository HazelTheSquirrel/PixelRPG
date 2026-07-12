// src/main/java/de/pixelrpg/rpg/PixelRPGPlugin.java (VOLLSTÄNDIG, ersetzt alte Datei — konsolidiert)
package de.pixelrpg.rpg;

import de.pixelrpg.rpg.achievement.AchievementManager;
import de.pixelrpg.rpg.achievement.AchievementRepository;
import de.pixelrpg.rpg.achievement.AchievementTriggerListener;
import de.pixelrpg.rpg.achievement.MobKillStatisticListener;
import de.pixelrpg.rpg.achievement.PlayerDeathStatisticListener;
import de.pixelrpg.rpg.achievement.StatisticsService;
import de.pixelrpg.rpg.api.AchievementAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.boss.BossAttackPatternRegistry;
import de.pixelrpg.rpg.boss.BossDeathListener;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.boss.patterns.EnrageBuffPattern;
import de.pixelrpg.rpg.boss.patterns.ProjectileVolleyPattern;
import de.pixelrpg.rpg.boss.patterns.SlamAttackPattern;
import de.pixelrpg.rpg.boss.patterns.SummonAddsPattern;
import de.pixelrpg.rpg.combat.CombatDamageListener;
import de.pixelrpg.rpg.combat.ElytraPermissionListener;
import de.pixelrpg.rpg.combat.EquipmentAuraListener;
import de.pixelrpg.rpg.combat.MobExperienceListener;
import de.pixelrpg.rpg.combat.MobNameplateListener;
import de.pixelrpg.rpg.combat.MobNameplateService;
import de.pixelrpg.rpg.combat.SoulboundDeathListener;
import de.pixelrpg.rpg.combat.SoulslikeDeathScreenListener;
import de.pixelrpg.rpg.combat.gem.GemRepository;
import de.pixelrpg.rpg.combat.gem.SkillGemCastEngine;
import de.pixelrpg.rpg.combat.loot.LootDropListener;
import de.pixelrpg.rpg.combat.scaling.MobRankScalingListener;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.combat.scaling.RegionDangerProvider;
import de.pixelrpg.rpg.combat.skill.PassiveGemRecalcTask;
import de.pixelrpg.rpg.combat.skill.SkillInputListener;
import de.pixelrpg.rpg.command.RootCommand;
import de.pixelrpg.rpg.command.impl.AchievementAdminSubCommand;
import de.pixelrpg.rpg.command.impl.BlacksmithSubCommand;
import de.pixelrpg.rpg.command.impl.BossSubCommand;
import de.pixelrpg.rpg.command.impl.DungeonSubCommand;
import de.pixelrpg.rpg.command.impl.FusionSubCommand;
import de.pixelrpg.rpg.command.impl.NpcSubCommand;
import de.pixelrpg.rpg.command.impl.PartySubCommand;
import de.pixelrpg.rpg.command.impl.QuestAdminSubCommand;
import de.pixelrpg.rpg.command.impl.QuestLogCommand;
import de.pixelrpg.rpg.command.impl.RegionSubCommand;
import de.pixelrpg.rpg.command.impl.ShopSubCommand;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.dungeon.DungeonBossDeathListener;
import de.pixelrpg.rpg.dungeon.DungeonInstanceManager;
import de.pixelrpg.rpg.dungeon.DungeonRepository;
import de.pixelrpg.rpg.dungeon.DungeonSelectionManager;
import de.pixelrpg.rpg.dungeon.DungeonWandListener;
import de.pixelrpg.rpg.economy.GuildCurrencyPickupListener;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.gui.GUIListener;
import de.pixelrpg.rpg.gui.ItemFusionGUI;
import de.pixelrpg.rpg.gui.ShopEditorGUI;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehaviorRegistry;
import de.pixelrpg.rpg.npc.NpcChunkListener;
import de.pixelrpg.rpg.npc.NpcInteractListener;
import de.pixelrpg.rpg.npc.NpcLookTask;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.behavior.BlacksmithBehavior;
import de.pixelrpg.rpg.npc.behavior.QuestBehavior;
import de.pixelrpg.rpg.npc.behavior.ReceptionBehavior;
import de.pixelrpg.rpg.npc.behavior.ShopBehavior;
import de.pixelrpg.rpg.npc.behavior.StoryBehavior;
import de.pixelrpg.rpg.npc.behavior.TravelBehavior;
import de.pixelrpg.rpg.party.PartyDisconnectListener;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.AttributeConfig;
import de.pixelrpg.rpg.player.GuildJoinLeaveListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.player.TitleDisplayJoinListener;
import de.pixelrpg.rpg.player.TitleDisplayService;
import de.pixelrpg.rpg.quest.GlobalEventState;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestMobKillListener;
import de.pixelrpg.rpg.quest.QuestPassiveCheckTask;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.region.PlayerRegionTracker;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionSelectionManager;
import de.pixelrpg.rpg.region.RegionWandListener;
import de.pixelrpg.rpg.region.YamlRegionRepository;
import de.pixelrpg.rpg.region.biome.BiomeClusterManager;
import de.pixelrpg.rpg.region.biome.BiomeNameRepository;
import de.pixelrpg.rpg.scoreboard.PlaytimeTracker;
import de.pixelrpg.rpg.scoreboard.ScoreboardService;
import de.pixelrpg.rpg.shop.ShopManager;
import de.pixelrpg.rpg.stats.RPGStatsListener;
import de.pixelrpg.rpg.stats.StatEngine;
import de.pixelrpg.rpg.story.StoryManager;
import de.pixelrpg.rpg.travel.GuildCompassListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelRPGPlugin extends JavaPlugin {

    private static PixelRPGPlugin instance;

    private PlayerProfileManager playerProfileManager;
    private StatEngine statEngine;
    private ItemEconomyConfig itemEconomyConfig;
    private ItemService itemService;
    private BlacksmithGUI blacksmithGUI;
    private ItemFusionGUI itemFusionGUI;
    private MobScalingConfig mobScalingConfig;
    private MobNameplateService mobNameplateService;
    private RegionManager regionManager;
    private RegionSelectionManager regionSelectionManager;
    private NpcManager npcManager;
    private NpcBehaviorRegistry npcBehaviorRegistry;
    private ShopManager shopManager;
    private ShopEditorGUI shopEditorGUI;
    private StoryManager storyManager;
    private PartyManager partyManager;
    private QuestRepository questRepository;
    private QuestManager questManager;
    private GlobalEventState globalEventState;
    private DungeonRepository dungeonRepository;
    private DungeonSelectionManager dungeonSelectionManager;
    private DungeonInstanceManager dungeonInstanceManager;
    private BossRepository bossRepository;
    private BossManager bossManager;
    private AchievementRepository achievementRepository;
    private AchievementManager achievementManager;
    private StatisticsService statisticsService;
    private ScoreboardService scoreboardService;
    private PlaytimeTracker playtimeTracker;
    private LanguageManager languageManager;
    private TitleDisplayService titleDisplayService;
    private EquipmentAuraListener equipmentAuraListener;
    private GemRepository gemRepository;
    private SkillGemCastEngine skillGemCastEngine;
    private BiomeNameRepository biomeNameRepository;
    private BiomeClusterManager biomeClusterManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        RPGKeys.init(this);

        this.languageManager = new LanguageManager(this);
        languageManager.load(getConfig().getString("language.default", "en"));

        this.playerProfileManager = new PlayerProfileManager(this);
        this.playerProfileManager.initialize(getConfig());

        this.titleDisplayService = new TitleDisplayService(playerProfileManager);

        this.statEngine = new StatEngine(playerProfileManager);

        this.gemRepository = new GemRepository(this);
        gemRepository.load();
        this.skillGemCastEngine = new SkillGemCastEngine(playerProfileManager, statEngine, gemRepository);
        new PassiveGemRecalcTask(this, statEngine).start();

        this.itemEconomyConfig = new ItemEconomyConfig();
        itemEconomyConfig.load(getConfig());
        RPGItemBuilder.configureChances(
                getConfig().getDouble("items.blessing-chance", 0.12),
                getConfig().getDouble("items.curse-chance", 0.10));

        this.itemService = new ItemService();
        Bukkit.getServicesManager().register(
                de.pixelrpg.rpg.api.ItemAPI.class, itemService, this, ServicePriority.Normal);

        this.blacksmithGUI = new BlacksmithGUI(playerProfileManager, itemEconomyConfig);
        this.itemFusionGUI = new ItemFusionGUI(playerProfileManager, itemEconomyConfig);

        this.mobScalingConfig = new MobScalingConfig();
        mobScalingConfig.load(getConfig());

        this.regionManager = new RegionManager(this, new YamlRegionRepository(getDataFolder()));
        regionManager.load();
        Bukkit.getServicesManager().register(
                RegionDangerProvider.class, regionManager, this, ServicePriority.Normal);
        this.regionSelectionManager = new RegionSelectionManager();

        this.biomeNameRepository = new BiomeNameRepository(this);
        biomeNameRepository.load();
        this.biomeClusterManager = new BiomeClusterManager(this, biomeNameRepository);
        biomeClusterManager.load();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> biomeClusterManager.saveIfDirty(), 200L, 200L);

        this.mobNameplateService = new MobNameplateService(this, mobScalingConfig);

        this.shopManager = new ShopManager(this);
        shopManager.load();
        this.shopEditorGUI = new ShopEditorGUI(shopManager);

        this.storyManager = new StoryManager(this, playerProfileManager);
        storyManager.load();

        this.partyManager = new PartyManager();
        Bukkit.getServicesManager().register(
                de.pixelrpg.rpg.api.PartyAPI.class, partyManager, this, ServicePriority.Normal);

        this.questRepository = new QuestRepository(this);
        questRepository.load();

        this.globalEventState = new GlobalEventState(this);
        globalEventState.load();

        double partyShareRange = getConfig().getDouble("quests.party-share-range", 24.0);
        this.questManager = new QuestManager(this, questRepository, playerProfileManager,
                playerProfileManager, globalEventState, partyShareRange);
        questManager.startTimerCheckTask();

        BossAttackPatternRegistry patternRegistry = new BossAttackPatternRegistry();
        patternRegistry.register(new SlamAttackPattern());
        patternRegistry.register(new SummonAddsPattern());
        patternRegistry.register(new ProjectileVolleyPattern());
        patternRegistry.register(new EnrageBuffPattern());

        this.bossRepository = new BossRepository(this);
        bossRepository.load();

        double barRadius = getConfig().getDouble("bosses.bar-radius", 60.0);
        int barUpdateInterval = getConfig().getInt("bosses.bar-update-interval-ticks", 20);
        int phaseCheckInterval = getConfig().getInt("bosses.phase-check-interval-ticks", 10);

        this.bossManager = new BossManager(this, patternRegistry, playerProfileManager, playerProfileManager,
                barRadius, barUpdateInterval, phaseCheckInterval);

        this.dungeonRepository = new DungeonRepository(this);
        dungeonRepository.load();
        this.dungeonSelectionManager = new DungeonSelectionManager();

        String instanceWorldName = getConfig().getString("dungeons.instance-world", "pixelrpg_instances");
        int slotSpacing = getConfig().getInt("dungeons.slot-spacing", 512);
        int cleanupDelayMinutes = getConfig().getInt("dungeons.cleanup-delay-minutes", 20);

        this.dungeonInstanceManager = new DungeonInstanceManager(this, dungeonRepository, playerProfileManager,
                playerProfileManager, partyManager, mobScalingConfig, bossRepository, bossManager,
                instanceWorldName, slotSpacing, cleanupDelayMinutes);

        this.achievementRepository = new AchievementRepository(this);
        achievementRepository.load();

        this.achievementManager = new AchievementManager(achievementRepository, playerProfileManager);
        Bukkit.getServicesManager().register(AchievementAPI.class, achievementManager, this, ServicePriority.Normal);

        this.statisticsService = new StatisticsService(playerProfileManager, achievementManager);
        Bukkit.getServicesManager().register(StatisticsAPI.class, statisticsService, this, ServicePriority.Normal);

        int scoreboardInterval = getConfig().getInt("scoreboard.update-interval-ticks", 20);
        this.scoreboardService = new ScoreboardService(this, playerProfileManager, scoreboardInterval);
        scoreboardService.startTask();

        this.playtimeTracker = new PlaytimeTracker(this, playerProfileManager);
        int autosaveInterval = getConfig().getInt("statistics.autosave-interval-ticks", 6000);
        playtimeTracker.startAutosaveTask(autosaveInterval);

        this.equipmentAuraListener = new EquipmentAuraListener(getConfig().getInt("effects.aura-interval-ticks", 60));
        equipmentAuraListener.start();

        AttributeConfig.configureElytraCost(getConfig().getDouble("elytra.permit-cost", 750.0));

        this.npcManager = new NpcManager(this);
        npcManager.loadAll();

        NpcChunkListener npcChunkListener = new NpcChunkListener(npcManager);
        getServer().getPluginManager().registerEvents(npcChunkListener, this);

        NpcLookTask npcLookTask = new NpcLookTask(this, npcManager,
                getConfig().getDouble("npc.look-radius", 8.0),
                getConfig().getInt("npc.look-interval-ticks", 5));
        npcLookTask.start();

        this.npcBehaviorRegistry = new NpcBehaviorRegistry();
        npcBehaviorRegistry.register(new ReceptionBehavior(playerProfileManager));
        npcBehaviorRegistry.register(new BlacksmithBehavior(blacksmithGUI, playerProfileManager));
        npcBehaviorRegistry.register(new QuestBehavior(questManager, playerProfileManager));
        npcBehaviorRegistry.register(new ShopBehavior(shopManager, playerProfileManager));
        npcBehaviorRegistry.register(new TravelBehavior(npcManager, playerProfileManager));
        npcBehaviorRegistry.register(new StoryBehavior(storyManager, playerProfileManager));

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new GuildJoinLeaveListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new RPGStatsListener(statEngine), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(skillGemCastEngine), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        getServer().getPluginManager().registerEvents(itemFusionGUI, this);
        getServer().getPluginManager().registerEvents(shopEditorGUI, this);
        getServer().getPluginManager().registerEvents(
                new LootDropListener(playerProfileManager, itemEconomyConfig), this);
        getServer().getPluginManager().registerEvents(
                new MobRankScalingListener(playerProfileManager, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(
                new MobNameplateListener(mobNameplateService), this);
        getServer().getPluginManager().registerEvents(
                new CombatDamageListener(playerProfileManager, playerProfileManager, statEngine, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(
                new MobExperienceListener(playerProfileManager, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(
                new RegionWandListener(regionSelectionManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerRegionTracker(regionManager, biomeClusterManager, playerProfileManager,
                        getConfig().getLong("regions.title-fade-in-ms", 400),
                        getConfig().getLong("regions.title-stay-ms", 2400),
                        getConfig().getLong("regions.title-fade-out-ms", 600)), this);
        getServer().getPluginManager().registerEvents(
                new NpcInteractListener(npcManager, npcBehaviorRegistry), this);
        getServer().getPluginManager().registerEvents(
                new QuestMobKillListener(questManager), this);
        getServer().getPluginManager().registerEvents(
                new PartyDisconnectListener(partyManager), this);
        getServer().getPluginManager().registerEvents(
                new DungeonWandListener(this, dungeonSelectionManager), this);
        getServer().getPluginManager().registerEvents(
                new DungeonBossDeathListener(dungeonInstanceManager), this);
        getServer().getPluginManager().registerEvents(
                new BossDeathListener(bossManager), this);
        getServer().getPluginManager().registerEvents(
                new MobKillStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerDeathStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(
                new AchievementTriggerListener(achievementManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(
                new GuildCurrencyPickupListener(playerProfileManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(
                new TitleDisplayJoinListener(titleDisplayService), this);
        getServer().getPluginManager().registerEvents(
                new GuildCompassListener(npcManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(
                new SoulboundDeathListener(), this);
        getServer().getPluginManager().registerEvents(
                new SoulslikeDeathScreenListener(), this);
        getServer().getPluginManager().registerEvents(
                new ElytraPermissionListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(scoreboardService, this);
        getServer().getPluginManager().registerEvents(playtimeTracker, this);

        new QuestPassiveCheckTask(this, questManager).start();

        RootCommand rootCommand = new RootCommand();
        rootCommand.register(new BlacksmithSubCommand(blacksmithGUI));
        rootCommand.register(new FusionSubCommand(itemFusionGUI));
        rootCommand.register(new RegionSubCommand(regionManager, regionSelectionManager));
        rootCommand.register(new NpcSubCommand(npcManager));
        rootCommand.register(new ShopSubCommand(shopManager, shopEditorGUI));
        rootCommand.register(new QuestAdminSubCommand(questManager));
        rootCommand.register(new DungeonSubCommand(this, dungeonRepository, dungeonSelectionManager));
        rootCommand.register(new BossSubCommand(bossRepository, bossManager));
        rootCommand.register(new AchievementAdminSubCommand(achievementManager));

        if (getCommand("rpgadmin") != null) {
            getCommand("rpgadmin").setExecutor(rootCommand);
            getCommand("rpgadmin").setTabCompleter(rootCommand);
        }

        PartySubCommand partyCommand = new PartySubCommand(partyManager, playerProfileManager);
        if (getCommand("rpgparty") != null) {
            getCommand("rpgparty").setExecutor(partyCommand);
            getCommand("rpgparty").setTabCompleter(partyCommand);
        }

        if (getCommand("questlog") != null) {
            getCommand("questlog").setExecutor(new QuestLogCommand(questManager, playerProfileManager));
        }

        getLogger().info("PixelRPG core enabled.");
    }

    @Override
    public void onDisable() {
        if (playtimeTracker != null) {
            playtimeTracker.flushAll();
        }
        if (bossManager != null) {
            bossManager.shutdownAll();
        }
        if (mobNameplateService != null) {
            mobNameplateService.cancelAll();
        }
        if (npcManager != null) {
            npcManager.saveAll();
        }
        if (shopManager != null) {
            shopManager.save();
        }
        if (regionManager != null) {
            regionManager.save();
        }
        if (globalEventState != null) {
            globalEventState.save();
        }
        if (dungeonRepository != null) {
            dungeonRepository.save();
        }
        if (biomeClusterManager != null) {
            biomeClusterManager.saveIfDirty();
        }
        if (playerProfileManager != null) {
            playerProfileManager.shutdown();
        }
        getLogger().info("PixelRPG core disabled.");
    }

    public static PixelRPGPlugin getInstance() {
        return instance;
    }

    public PlayerProfileManager getPlayerProfileManager() {
        return playerProfileManager;
    }

    public StatEngine getStatEngine() {
        return statEngine;
    }

    public ItemEconomyConfig getItemEconomyConfig() {
        return itemEconomyConfig;
    }

    public MobScalingConfig getMobScalingConfig() {
        return mobScalingConfig;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public NpcBehaviorRegistry getNpcBehaviorRegistry() {
        return npcBehaviorRegistry;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public StoryManager getStoryManager() {
        return storyManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public DungeonRepository getDungeonRepository() {
        return dungeonRepository;
    }

    public DungeonInstanceManager getDungeonInstanceManager() {
        return dungeonInstanceManager;
    }

    public BossRepository getBossRepository() {
        return bossRepository;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public ScoreboardService getScoreboardService() {
        return scoreboardService;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public TitleDisplayService getTitleDisplayService() {
        return titleDisplayService;
    }

    public GemRepository getGemRepository() {
        return gemRepository;
    }

    public SkillGemCastEngine getSkillGemCastEngine() {
        return skillGemCastEngine;
    }

    public BiomeClusterManager getBiomeClusterManager() {
        return biomeClusterManager;
    }
}