package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.PartyAPI;
import de.pixelrpg.rpg.api.StatisticsAPI;
import de.pixelrpg.rpg.stats.StatEngine;
import de.pixelrpg.rpg.stats.StatisticsService;
import de.pixelrpg.rpg.stats.RPGStatsListener;
import de.pixelrpg.rpg.combat.CombatDamageListener;
import de.pixelrpg.rpg.combat.MobExperienceListener;
import de.pixelrpg.rpg.combat.SoulboundDeathListener;
import de.pixelrpg.rpg.combat.loot.LootDropListener;
import de.pixelrpg.rpg.combat.scaling.MobLevelScalingListener;
import de.pixelrpg.rpg.combat.scaling.MobNameplateListener;
import de.pixelrpg.rpg.combat.scaling.MobNameplateService;
import de.pixelrpg.rpg.combat.scaling.MobScalingConfig;
import de.pixelrpg.rpg.combat.skill.FireballWeaponListener;
import de.pixelrpg.rpg.combat.skill.SkillInputListener;
import de.pixelrpg.rpg.combat.skill.WeaponAbilityEngine;
import de.pixelrpg.rpg.boss.BossAttackPatternRegistry;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.boss.BiomeBossSpawnTask;
import de.pixelrpg.rpg.boss.BossDamageContributionListener;
import de.pixelrpg.rpg.boss.BossDeathListener;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.companion.CompanionDialogService;
import de.pixelrpg.rpg.companion.CompanionNpcListener;
import de.pixelrpg.rpg.companion.CompanionSystem;
import de.pixelrpg.rpg.content.ContentCatalogService;
import de.pixelrpg.rpg.content.JsonContentCatalogLoader;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.DialogueProgressStore;
import de.pixelrpg.rpg.dialogue.DialogueTreeService;
import de.pixelrpg.rpg.dialogue.NpcDialogueListener;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.core.LifecycleCoordinator;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.economy.GuildCurrencyPickupListener;
import de.pixelrpg.rpg.equipment.EquipmentService;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.npc.NpcChunkListener;
import de.pixelrpg.rpg.npc.NpcProtectionListener;
import de.pixelrpg.rpg.npc.NpcRepository;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.YamlNpcRepository;
import de.pixelrpg.rpg.player.PlayerProfileLifecycleListener;
import de.pixelrpg.rpg.profession.ProfessionDialogService;
import de.pixelrpg.rpg.profession.ProfessionNpcListener;
import de.pixelrpg.rpg.profession.ProfessionSystem;
import de.pixelrpg.rpg.quest.QuestRepository;
import de.pixelrpg.rpg.quest.QuestService;
import de.pixelrpg.rpg.region.RegionEditor;
import de.pixelrpg.rpg.region.RegionFlagDialogService;
import de.pixelrpg.rpg.region.RegionListener;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionPolicyService;
import de.pixelrpg.rpg.region.RegionRepository;
import de.pixelrpg.rpg.region.RegionSpawnService;
import de.pixelrpg.rpg.region.RegionTransitionService;
import de.pixelrpg.rpg.story.StoryNpcInteractionListener;
import de.pixelrpg.rpg.story.StoryRepository;
import de.pixelrpg.rpg.story.StoryService;
import de.pixelrpg.rpg.shop.ShopDialogService;
import de.pixelrpg.rpg.shop.ShopNpcListener;
import de.pixelrpg.rpg.shop.ShopRepository;
import de.pixelrpg.rpg.shop.ShopService;
import de.pixelrpg.rpg.trade.TradeDepotDialogService;
import de.pixelrpg.rpg.trade.TradeDepotRepository;
import de.pixelrpg.rpg.trade.TradeDepotService;
import de.pixelrpg.rpg.trade.TradeGoodsRepository;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PixelRPGPlugin extends JavaPlugin {
    private LifecycleCoordinator lifecycle;
    private PlayerProfileManager playerProfileManager;
    private ExecutorService dialogueIo;
    private ExecutorService contentIo;
    private ExecutorService questIo;
    private ExecutorService storyIo;
    private ExecutorService regionIo;
    private ExecutorService shopIo;
    private ExecutorService tradeDepotIo;
    private ExecutorService tradeGoodsIo;
    private ProfessionSystem professionSystem;
    private CompanionSystem companionSystem;
    private ItemService itemService;
    private StatEngine statEngine;
    private BossManager bossManager;
    private BiomeBossSpawnTask biomeBossSpawnTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("data/content/texts.json", false);
        saveResource("data/quests/definitions.json", false);
        saveResource("data/recipes/crafting-recipes.json", false);
        lifecycle = new LifecycleCoordinator(getLogger());

        new JsonDataManager(this).initialize();
        RPGKeys keys = new RPGKeys(this);

        contentIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-ContentIO");
            thread.setDaemon(true);
            return thread;
        });
        ContentCatalogService contentCatalog = lifecycle.register(new ContentCatalogService(
                this,
                new JsonContentCatalogLoader(this, getDataFolder().toPath().resolve("data/content/texts.json"), contentIo)
        ));
        java.util.concurrent.CompletableFuture<Void> contentLoad = contentCatalog.loadAsync();
        contentLoad.whenComplete((ignored, failure) -> {
            if (failure != null) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to load content catalog.", failure);
            }
        });

        playerProfileManager = lifecycle.register(new PlayerProfileManager(this));
        playerProfileManager.initialize(getConfig());

        professionSystem = lifecycle.register(new ProfessionSystem(this, playerProfileManager));

        ExecutorService questIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-QuestIO");
            thread.setDaemon(true);
            return thread;
        });
        this.questIo = questIo;
        QuestRepository questRepository = new QuestRepository(this, getDataFolder().toPath().resolve("data/quests/definitions.json"), questIo);
        QuestService questService = lifecycle.register(new QuestService(this, playerProfileManager, questRepository));
        questRepository.loadAsync().whenComplete((ignored, failure) -> {
            if (failure != null) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to load quest definitions.", failure);
            }
        });

        itemService = lifecycle.register(new ItemService(this, keys));
        MobScalingConfig mobScalingConfig = new MobScalingConfig();
        ItemEconomyConfig itemEconomyConfig = new ItemEconomyConfig();
        itemEconomyConfig.load(getConfig());
        GuildCurrencyItemFactory currencyFactory = new GuildCurrencyItemFactory(keys);
        currencyFactory.configureMaxStackSize(getConfig().getInt("economy.currency.max-stack-size", 64));
        statEngine = lifecycle.register(new StatEngine(playerProfileManager));
        getServer().getServicesManager().register(StatisticsAPI.class, new StatisticsService(playerProfileManager, statEngine), this, ServicePriority.Normal);
        BossRepository bossRepository = new BossRepository(this);
        bossRepository.load();
        BossAttackPatternRegistry bossPatterns = new BossAttackPatternRegistry();
        bossPatterns.register(new de.pixelrpg.rpg.boss.patterns.EnrageBuffPattern());
        bossPatterns.register(new de.pixelrpg.rpg.boss.patterns.ProjectileVolleyPattern());
        bossPatterns.register(new de.pixelrpg.rpg.boss.patterns.SlamAttackPattern());
        bossPatterns.register(new de.pixelrpg.rpg.boss.patterns.SummonAddsPattern());
        bossManager = new BossManager(this, bossPatterns, new GuildApiAdapter(playerProfileManager),
                new PartyApiAdapter(), new EconomyApiAdapter(playerProfileManager), itemService, mobScalingConfig,
                getConfig().getDouble("bosses.bar-radius", 60.0D),
                getConfig().getInt("bosses.bar-update-interval-ticks", 20),
                getConfig().getInt("bosses.phase-check-interval-ticks", 10));
        biomeBossSpawnTask = new BiomeBossSpawnTask(this, bossRepository, bossManager,
                getConfig().getDouble("bosses.biome-spawn.spawn-radius", 80.0D),
                getConfig().getInt("bosses.biome-spawn.check-interval-seconds", 60),
                getConfig().getInt("bosses.biome-spawn.max-concurrent", 4));
        biomeBossSpawnTask.start();
        getServer().getPluginManager().registerEvents(new RPGStatsListener(statEngine, playerProfileManager), this);

        getServer().getPluginManager().registerEvents(new CombatDamageListener(this, new GuildApiAdapter(playerProfileManager), playerProfileManager, statEngine), this);
        getServer().getPluginManager().registerEvents(new BossDamageContributionListener(bossManager, new GuildApiAdapter(playerProfileManager)), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(bossManager), this);
        getServer().getPluginManager().registerEvents(new MobLevelScalingListener(this, mobScalingConfig, new GuildApiAdapter(playerProfileManager)), this);
        getServer().getPluginManager().registerEvents(new MobNameplateListener(this, new MobNameplateService(this, mobScalingConfig), new GuildApiAdapter(playerProfileManager)), this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(new GuildApiAdapter(playerProfileManager), mobScalingConfig), this);
        getServer().getPluginManager().registerEvents(new LootDropListener(new GuildApiAdapter(playerProfileManager), itemEconomyConfig, itemService, new RPGItemBuilder(keys), currencyFactory), this);
        getServer().getPluginManager().registerEvents(new SoulboundDeathListener(new GuildApiAdapter(playerProfileManager), keys), this);
        WeaponAbilityEngine abilityEngine = new WeaponAbilityEngine(playerProfileManager, statEngine);
        getServer().getPluginManager().registerEvents(new SkillInputListener(abilityEngine), this);
        getServer().getPluginManager().registerEvents(new FireballWeaponListener(playerProfileManager, statEngine), this);
        getServer().getServicesManager().register(ItemAPI.class, itemService, this, ServicePriority.Normal);


        ExecutorService npcIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-NpcIO");
            thread.setDaemon(true);
            return thread;
        });
        NpcRepository npcRepository = new YamlNpcRepository(this, npcIo);
        NpcRuntimeManager npcRuntime = lifecycle.register(new NpcRuntimeManager(this, keys, npcRepository));
        companionSystem = lifecycle.register(new CompanionSystem(this, playerProfileManager, npcRuntime));
        companionSystem.loadAsync().thenRun(() -> getServer().getScheduler().runTask(this, () -> {
            if (!isEnabled()) return;
            companionSystem.register();
            getServer().getPluginManager().registerEvents(new CompanionNpcListener(npcRuntime, new CompanionDialogService(companionSystem.service(), new DialogueEngine())), this);
        })).exceptionally(failure -> { getLogger().log(java.util.logging.Level.SEVERE, "Failed to load companion definitions.", failure); return null; });

        ExecutorService storyIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-StoryIO");
            thread.setDaemon(true);
            return thread;
        });
        this.storyIo = storyIo;
        StoryService storyService = new StoryService(playerProfileManager);
        StoryRepository storyRepository = lifecycle.register(new StoryRepository(
                this,
                getDataFolder().toPath().resolve("data/story/definitions.json"),
                storyIo,
                contentCatalog
        ));
        StoryNpcDialogue storyNpcDialogue = new StoryNpcDialogue(
                storyService,
                new DialogueEngine()
        );
        getServer().getPluginManager().registerEvents(
                new StoryNpcInteractionListener(npcRuntime, storyNpcDialogue), this);
        contentLoad
                .thenCompose(ignored -> storyRepository.loadAsync())
                .thenAccept(storyService::replace)
                .exceptionally(failure -> {
                    getLogger().log(java.util.logging.Level.SEVERE, "Failed to load story definitions.", failure);
                    return null;
                });

        dialogueIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-DialogueIO");
            thread.setDaemon(true);
            return thread;
        });
        DialogueProgressStore dialogueProgressStore = new DialogueProgressStore(this, dialogueIo);
        DialogueTreeService dialogueTreeService = lifecycle.register(
                new DialogueTreeService(new DialogueEngine(), dialogueProgressStore)
        );
        dialogueProgressStore.loadAsync().whenComplete((ignored, failure) -> {
            if (failure != null) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to load dialogue progress; NPC dialogue interaction remains disabled.", failure);
                return;
            }
            getServer().getScheduler().runTask(this, () -> {
                if (isEnabled()) {
                    getServer().getPluginManager().registerEvents(
                            new NpcDialogueListener(npcRuntime, dialogueTreeService), this);
                }
            });
        });

        npcRuntime.loadAsync();
        shopIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-ShopIO");
            thread.setDaemon(true);
            return thread;
        });
        ShopRepository shopRepository = lifecycle.register(new ShopRepository(
                this, getDataFolder().toPath().resolve("shops.yml"), shopIo
        ));
        ShopService shopService = lifecycle.register(new ShopService(this, playerProfileManager, itemService, shopRepository));

        tradeDepotIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-TradeDepotIO");
            thread.setDaemon(true);
            return thread;
        });
        tradeGoodsIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-TradeGoodsIO");
            thread.setDaemon(true);
            return thread;
        });
        TradeDepotRepository tradeDepotRepository = lifecycle.register(new TradeDepotRepository(
                this, getDataFolder().toPath().resolve("trade-depot.yml"), tradeDepotIo
        ));
        TradeGoodsRepository tradeGoodsRepository = lifecycle.register(new TradeGoodsRepository(
                this, getDataFolder().toPath().resolve("trade-goods-storage.yml"), tradeGoodsIo
        ));
        TradeDepotService tradeDepotService = lifecycle.register(new TradeDepotService(
                this, playerProfileManager, itemService, tradeDepotRepository, tradeGoodsRepository
        ));
        java.util.concurrent.CompletableFuture<Void> shopLoad = shopRepository.loadAsync()
                .thenAccept(shopService::replace);
        java.util.concurrent.CompletableFuture<Void> tradeLoad = tradeDepotRepository.loadAsync()
                .thenCombine(tradeGoodsRepository.loadAsync(), (snapshot, goods) -> snapshot)
                .thenAccept(tradeDepotService::replace);
        java.util.concurrent.CompletableFuture.allOf(shopLoad, tradeLoad)
                .thenRun(() -> getServer().getScheduler().runTask(this, () -> {
                    if (!isEnabled()) return;
                    TradeDepotDialogService tradeDialogs = new TradeDepotDialogService(
                            tradeDepotService, playerProfileManager, new DialogueEngine()
                    );
                    ShopDialogService shopDialogs = new ShopDialogService(
                            shopService, playerProfileManager, new DialogueEngine(), tradeDialogs
                    );
                    getServer().getPluginManager().registerEvents(new ShopNpcListener(npcRuntime, shopDialogs), this);
                }))
                .exceptionally(failure -> {
                    getLogger().log(java.util.logging.Level.SEVERE, "Failed to load shop/trade data.", failure);
                    return null;
                });


        // Profession recipe loading completes independently; NPC listener registration is finalized below.
        professionSystem.loadAsync().thenRun(() -> getServer().getScheduler().runTask(this, () -> {
            if (!isEnabled()) return;
            professionSystem.register();
            ProfessionDialogService professionDialogs = new ProfessionDialogService(
                    playerProfileManager,
                    professionSystem.professionService(),
                    professionSystem.craftingService(),
                    new DialogueEngine()
            );
            getServer().getPluginManager().registerEvents(new ProfessionNpcListener(npcRuntime, professionDialogs), this);
        })).exceptionally(failure -> { getLogger().log(java.util.logging.Level.SEVERE, "Failed to load profession recipes.", failure); return null; });

        regionIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-RegionIO");
            thread.setDaemon(true);
            return thread;
        });
        RegionRepository regionRepository = new RegionRepository(getDataFolder(), getLogger(), regionIo);
        RegionManager regionManager = lifecycle.register(new RegionManager(this, regionRepository));
        RegionSpawnService regionSpawnService = lifecycle.register(new RegionSpawnService(this, regionManager));
        RegionEditor regionEditor = lifecycle.register(new RegionEditor(this, regionManager));
        RegionPolicyService regionPolicyService = new RegionPolicyService(regionManager, regionSpawnService);
        RegionTransitionService regionTransitionService = new RegionTransitionService(regionManager);
        getServer().getPluginManager().registerEvents(
                new RegionListener(regionManager, regionPolicyService, regionTransitionService), this);
        regionEditor.start();
        regionManager.loadAsync().thenRun(() -> getServer().getScheduler().runTask(this, regionSpawnService::start))
                .exceptionally(failure -> {
                    getLogger().log(java.util.logging.Level.SEVERE, "Failed to load regions.", failure);
                    return null;
                });

        getServer().getPluginManager().registerEvents(new PlayerProfileLifecycleListener(playerProfileManager), this);
        getServer().getPluginManager().registerEvents(new EquipmentService(this, playerProfileManager), this);
        getServer().getPluginManager().registerEvents(
                new GuildCurrencyPickupListener(playerProfileManager, playerProfileManager, currencyFactory), this);
        getServer().getPluginManager().registerEvents(new NpcChunkListener(this, npcRuntime), this);
        getServer().getPluginManager().registerEvents(new NpcProtectionListener(npcRuntime), this);
    }

    public StatEngine getStatEngine() { return statEngine; }
    public CompanionSystem getCompanionSystem() { return companionSystem; }
    public CompanionSystem getCompanionService() { return companionSystem; }

    private static final class GuildApiAdapter implements GuildAPI {
        private final PlayerProfileManager profiles;
        private GuildApiAdapter(PlayerProfileManager profiles) { this.profiles = profiles; }
        public boolean isRegistered(java.util.UUID id) { return profiles.getProfile(id).map(de.pixelrpg.rpg.player.PlayerProfile::isRegistered).orElse(false); }
        public int getLevel(java.util.UUID id) { return profiles.getProfile(id).map(de.pixelrpg.rpg.player.PlayerProfile::getLevel).orElse(1); }
        public long getExperience(java.util.UUID id) { return profiles.getProfile(id).map(de.pixelrpg.rpg.player.PlayerProfile::getExperience).orElse(0L); }
        public void addExperience(java.util.UUID id, long amount) { profiles.getProfile(id).ifPresent(profile -> { profile.addExperience(amount); profiles.saveProfileAsync(id); }); }
    }
    private static final class EconomyApiAdapter implements EconomyAPI {
        private final PlayerProfileManager profiles;
        private EconomyApiAdapter(PlayerProfileManager profiles) { this.profiles = profiles; }
        public double getBalance(java.util.UUID id) { return profiles.getProfile(id).map(de.pixelrpg.rpg.player.PlayerProfile::getMoney).orElse(0.0D); }
        public void deposit(java.util.UUID id, double amount) { profiles.getProfile(id).ifPresent(profile -> { profile.addMoney(amount); profiles.saveProfileAsync(id); }); }
        public boolean withdraw(java.util.UUID id, double amount) { var profile=profiles.getProfile(id).orElse(null); if(profile==null)return false; boolean result=profile.removeMoney(amount); if(result)profiles.saveProfileAsync(id); return result; }
    }
    private static final class PartyApiAdapter implements PartyAPI {
        public boolean isInParty(java.util.UUID id) { return false; }
        public java.util.Set<java.util.UUID> getPartyMembers(java.util.UUID id) { return java.util.Set.of(id); }
        public java.util.UUID getPartyLeader(java.util.UUID id) { return id; }
        public boolean isLeader(java.util.UUID id) { return true; }
        public boolean isWithinShareRange(java.util.UUID source, java.util.UUID target) { return source.equals(target); }
        public double getShareRange() { return 0.0D; }
    }

    @Override
    public void onDisable() {
        if (lifecycle != null) {
            if (biomeBossSpawnTask != null) { biomeBossSpawnTask.stop(); biomeBossSpawnTask = null; }
            if (bossManager != null) { bossManager.shutdownAll(); bossManager = null; }
            getServer().getServicesManager().unregister(ItemAPI.class);
            lifecycle.close();
            if (dialogueIo != null) {
                dialogueIo.shutdown();
                dialogueIo = null;
            }
            if (contentIo != null) {
                contentIo.shutdown();
                contentIo = null;
            }
            if (questIo != null) {
                questIo.shutdown();
                questIo = null;
            }
            if (storyIo != null) {
                storyIo.shutdown();
                storyIo = null;
            }
            if (regionIo != null) {
                regionIo.shutdown();
                regionIo = null;
            }
            if (shopIo != null) {
                shopIo.shutdown();
                shopIo = null;
            }
            if (tradeDepotIo != null) {
                tradeDepotIo.shutdown();
                tradeDepotIo = null;
            }
            if (tradeGoodsIo != null) {
                tradeGoodsIo.shutdown();
                tradeGoodsIo = null;
            }
            lifecycle = null;
            playerProfileManager = null;
        }
    }
}
