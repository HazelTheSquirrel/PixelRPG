package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.boss.BossAttackPatternRegistry;
import de.pixelrpg.rpg.boss.BossDamageContributionListener;
import de.pixelrpg.rpg.boss.BossDeathListener;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.boss.WorldBossSpawnTask;
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
import de.pixelrpg.rpg.combat.loot.LootDropListener;
import de.pixelrpg.rpg.combat.scaling.MobLevelScalingListener;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.combat.skill.SkillInputListener;
import de.pixelrpg.rpg.combat.skill.WeaponAbilityEngine;
import de.pixelrpg.rpg.command.RootCommand;
import de.pixelrpg.rpg.command.impl.BlacksmithSubCommand;
import de.pixelrpg.rpg.command.impl.BossSubCommand;
import de.pixelrpg.rpg.command.impl.NpcSubCommand;
import de.pixelrpg.rpg.command.impl.PartySubCommand;
import de.pixelrpg.rpg.command.impl.QuestAdminSubCommand;
import de.pixelrpg.rpg.command.impl.QuestLogCommand;
import de.pixelrpg.rpg.command.impl.ShopSubCommand;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.dialogue.DialogueCommand;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogListener;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.economy.GuildCurrencyPickupListener;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.gui.GUIListener;
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
import de.pixelrpg.rpg.player.ClassBalance;
import de.pixelrpg.rpg.player.GuildJoinLeaveListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.quest.GlobalEventState;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestMobKillListener;
import de.pixelrpg.rpg.quest.QuestPassiveCheckTask;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.scoreboard.PlaytimeTracker;
import de.pixelrpg.rpg.scoreboard.ScoreboardService;
import de.pixelrpg.rpg.shop.ShopManager;
import de.pixelrpg.rpg.stats.ManaRegenerationTask;
import de.pixelrpg.rpg.stats.MobKillStatisticListener;
import de.pixelrpg.rpg.stats.PlayerDeathStatisticListener;
import de.pixelrpg.rpg.stats.QuestBossStatisticListener;
import de.pixelrpg.rpg.stats.RPGStatsListener;
import de.pixelrpg.rpg.stats.StatEngine;
import de.pixelrpg.rpg.stats.StatisticsService;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryManager;
import de.pixelrpg.rpg.travel.GuildCompassListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelRPGPlugin extends JavaPlugin {
    private static PixelRPGPlugin instance;
    private PlayerProfileManager playerProfileManager;
    private StatEngine statEngine;
    private ManaRegenerationTask manaRegenerationTask;
    private ItemEconomyConfig itemEconomyConfig;
    private ItemService itemService;
    private BlacksmithGUI blacksmithGUI;
    private MobScalingConfig mobScalingConfig;
    private MobNameplateService mobNameplateService;
    private NpcManager npcManager;
    private NpcBehaviorRegistry npcBehaviorRegistry;
    private ShopManager shopManager;
    private ShopEditorGUI shopEditorGUI;
    private StoryManager storyManager;
    private PartyManager partyManager;
    private QuestRepository questRepository;
    private QuestManager questManager;
    private GlobalEventState globalEventState;
    private BossRepository bossRepository;
    private BossManager bossManager;
    private StatisticsService statisticsService;
    private ScoreboardService scoreboardService;
    private PlaytimeTracker playtimeTracker;
    private LanguageManager languageManager;
    private EquipmentAuraListener equipmentAuraListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RPGKeys.init(this);
        languageManager = new LanguageManager(this);
        languageManager.load(getConfig().getString("language.default", "en"));
        AttributeConfig.load(getConfig());
        ClassBalance.load(getConfig());
        StoryBookFactory.load(getConfig());
        GuildCurrencyItemFactory.configureMaxStackSize(getConfig().getInt("economy.currency.max-stack-size", 64));
        playerProfileManager = new PlayerProfileManager(this);
        playerProfileManager.initialize(getConfig());
        statEngine = new StatEngine(playerProfileManager);
        new ProfessionSystem(this, playerProfileManager).register();
        WeaponAbilityEngine weaponAbilityEngine = new WeaponAbilityEngine(playerProfileManager, statEngine);
        itemEconomyConfig = new ItemEconomyConfig();
        itemEconomyConfig.load(getConfig());
        RPGItemBuilder.configureChances(getConfig().getDouble("items.loot.blessing-chance", 0.12), getConfig().getDouble("items.loot.curse-chance", 0.10));
        itemService = new ItemService();
        Bukkit.getServicesManager().register(de.pixelrpg.rpg.api.ItemAPI.class, itemService, this, ServicePriority.Normal);
        blacksmithGUI = new BlacksmithGUI(playerProfileManager, itemEconomyConfig);
        mobScalingConfig = new MobScalingConfig();
        mobScalingConfig.load(getConfig());
        mobNameplateService = new MobNameplateService(this, mobScalingConfig);
        shopManager = new ShopManager(this);
        shopManager.load();
        shopEditorGUI = new ShopEditorGUI(shopManager);
        storyManager = new StoryManager(this, playerProfileManager);
        storyManager.load();
        partyManager = new PartyManager();
        Bukkit.getServicesManager().register(de.pixelrpg.rpg.api.PartyAPI.class, partyManager, this, ServicePriority.Normal);
        questRepository = new QuestRepository(this);
        questRepository.load();
        globalEventState = new GlobalEventState(this);
        globalEventState.load();
        double partyShareRange = getConfig().getDouble("quests.party-share-range", 24.0);
        questManager = new QuestManager(this, questRepository, playerProfileManager, playerProfileManager, globalEventState, partyShareRange);
        questManager.startTimerCheckTask();
        BossAttackPatternRegistry patternRegistry = new BossAttackPatternRegistry();
        patternRegistry.register(new SlamAttackPattern());
        patternRegistry.register(new SummonAddsPattern());
        patternRegistry.register(new ProjectileVolleyPattern());
        patternRegistry.register(new EnrageBuffPattern());
        bossRepository = new BossRepository(this);
        bossRepository.load();
        double barRadius = getConfig().getDouble("bosses.bar-radius", 60.0);
        int barUpdateInterval = getConfig().getInt("bosses.bar-update-interval-ticks", 20);
        int phaseCheckInterval = getConfig().getInt("bosses.phase-check-interval-ticks", 10);
        bossManager = new BossManager(this, patternRegistry, playerProfileManager, playerProfileManager, itemEconomyConfig, barRadius, barUpdateInterval, phaseCheckInterval);
        new WorldBossSpawnTask(this, bossRepository, bossManager, playerProfileManager, getConfig().getBoolean("bosses.auto-spawn.enabled", true), getConfig().getInt("bosses.auto-spawn.interval-minutes", 45), getConfig().getDouble("bosses.auto-spawn.spawn-radius", 80.0), getConfig().getInt("bosses.auto-spawn.max-concurrent", 2)).start();
        statisticsService = new StatisticsService(playerProfileManager);
        Bukkit.getServicesManager().register(StatisticsAPI.class, statisticsService, this, ServicePriority.Normal);
        scoreboardService = new ScoreboardService(this, playerProfileManager, getConfig().getInt("scoreboard.update-interval-ticks", 20));
        scoreboardService.startTask();
        playtimeTracker = new PlaytimeTracker(this, playerProfileManager);
        playtimeTracker.startAutosaveTask(getConfig().getInt("statistics.autosave-interval-ticks", 6000));
        equipmentAuraListener = new EquipmentAuraListener(playerProfileManager, getConfig().getInt("effects.aura-interval-ticks", 60));
        equipmentAuraListener.start();
        manaRegenerationTask = new ManaRegenerationTask(this, statEngine, playerProfileManager);
        manaRegenerationTask.start();
        AttributeConfig.configureElytraCost(getConfig().getDouble("elytra.permit-cost", 750.0));
        npcManager = new NpcManager(this);
        npcManager.loadAll();
        getServer().getPluginManager().registerEvents(new NpcChunkListener(npcManager), this);
        new NpcLookTask(this, npcManager, getConfig().getDouble("npc.look-radius", 8.0), getConfig().getInt("npc.look-interval-ticks", 5)).start();
        DialogueEngine dialogueEngine = new DialogueEngine();
        StoryNpcDialogue storyNpcDialogue = new StoryNpcDialogue(playerProfileManager, dialogueEngine);
        QuickActionsDialogService quickActions = new QuickActionsDialogService(playerProfileManager, statEngine);
        npcBehaviorRegistry = new NpcBehaviorRegistry();
        npcBehaviorRegistry.register(new ReceptionBehavior(playerProfileManager, dialogueEngine));
        npcBehaviorRegistry.register(new BlacksmithBehavior(blacksmithGUI, playerProfileManager));
        npcBehaviorRegistry.register(new QuestBehavior(questManager, playerProfileManager));
        npcBehaviorRegistry.register(new ShopBehavior(shopManager, playerProfileManager));
        npcBehaviorRegistry.register(new TravelBehavior(npcManager, playerProfileManager));
        npcBehaviorRegistry.register(new StoryBehavior(storyManager, storyNpcDialogue, playerProfileManager));
        npcBehaviorRegistry.register(new de.pixelrpg.rpg.npc.behavior.BankerBehavior(playerProfileManager));
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new GuildJoinLeaveListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new RPGStatsListener(statEngine, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(weaponAbilityEngine), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        getServer().getPluginManager().registerEvents(shopEditorGUI, this);
        getServer().getPluginManager().registerEvents(new LootDropListener(playerProfileManager, itemEconomyConfig), this);
        getServer().getPluginManager().registerEvents(new MobLevelScalingListener(playerProfileManager, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new MobNameplateListener(mobNameplateService, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new CombatDamageListener(playerProfileManager, playerProfileManager, statEngine, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new BossDamageContributionListener(bossManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(playerProfileManager, mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new NpcInteractListener(npcManager, npcBehaviorRegistry), this);
        getServer().getPluginManager().registerEvents(new QuestMobKillListener(questManager), this);
        getServer().getPluginManager().registerEvents(new PartyDisconnectListener(partyManager), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(bossManager), this);
        getServer().getPluginManager().registerEvents(new MobKillStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathStatisticListener(playerProfileManager, statisticsService), this);
        getServer().getPluginManager().registerEvents(new QuestBossStatisticListener(statisticsService), this);
        getServer().getPluginManager().registerEvents(new GuildCurrencyPickupListener(playerProfileManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new GuildCompassListener(npcManager, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new SoulboundDeathListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new ElytraPermissionListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(scoreboardService, this);
        getServer().getPluginManager().registerEvents(playtimeTracker, this);
        getServer().getPluginManager().registerEvents(new QuickActionsDialogListener(quickActions), this);
        new QuestPassiveCheckTask(this, questManager).start();
        RootCommand rootCommand = new RootCommand();
        rootCommand.register(new BlacksmithSubCommand(blacksmithGUI));
        rootCommand.register(new NpcSubCommand(npcManager));
        rootCommand.register(new ShopSubCommand(shopManager, shopEditorGUI, npcManager));
        rootCommand.register(new QuestAdminSubCommand(questManager));
        rootCommand.register(new BossSubCommand(bossRepository, bossManager));
        if (getCommand("rpgadmin") != null) { getCommand("rpgadmin").setExecutor(rootCommand); getCommand("rpgadmin").setTabCompleter(rootCommand); }
        PartySubCommand partyCommand = new PartySubCommand(partyManager, playerProfileManager);
        if (getCommand("rpgparty") != null) { getCommand("rpgparty").setExecutor(partyCommand); getCommand("rpgparty").setTabCompleter(partyCommand); }
        if (getCommand("questlog") != null) getCommand("questlog").setExecutor(new QuestLogCommand(questManager, playerProfileManager));
        if (getCommand("dialogue") != null) {
            DialogueCommand dialogueCommand = new DialogueCommand(playerProfileManager, dialogueEngine);
            getCommand("dialogue").setExecutor(dialogueCommand);
            getCommand("dialogue").setTabCompleter(dialogueCommand);
        }
        getLogger().info("PixelRPG core enabled.");
    }

    @Override
    public void onDisable() {
        if (manaRegenerationTask != null) manaRegenerationTask.stop();
        if (equipmentAuraListener != null) equipmentAuraListener.stop();
        if (questManager != null) questManager.shutdown();
        if (scoreboardService != null) scoreboardService.shutdown();
        if (playtimeTracker != null) playtimeTracker.shutdown();
        if (bossManager != null) bossManager.shutdown();
        if (shopManager != null) shopManager.shutdown();
        if (playerProfileManager != null) playerProfileManager.shutdown();
        Bukkit.getServicesManager().unregisterAll(this);
        instance = null;
    }

    public static PixelRPGPlugin getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public PlayerProfileManager getPlayerProfileManager() { return playerProfileManager; }
    public StatEngine getStatEngine() { return statEngine; }
    public ItemService getItemService() { return itemService; }
    public NpcManager getNpcManager() { return npcManager; }
    public ShopManager getShopManager() { return shopManager; }
    public StoryManager getStoryManager() { return storyManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public QuestManager getQuestManager() { return questManager; }
    public BossManager getBossManager() { return bossManager; }
    public ScoreboardService getScoreboardService() { return scoreboardService; }
}
