package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.config.JsonDataManager;
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
    private ProfessionSystem professionSystem;

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

        ItemService itemService = lifecycle.register(new ItemService(this, keys));
        getServer().getServicesManager().register(ItemAPI.class, itemService, this, ServicePriority.Normal);

        GuildCurrencyItemFactory currencyFactory = new GuildCurrencyItemFactory(keys);
        currencyFactory.configureMaxStackSize(getConfig().getInt("economy.currency.max-stack-size", 64));

        ExecutorService npcIo = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-NpcIO");
            thread.setDaemon(true);
            return thread;
        });
        NpcRepository npcRepository = new YamlNpcRepository(this, npcIo);
        NpcRuntimeManager npcRuntime = lifecycle.register(new NpcRuntimeManager(this, keys, npcRepository));

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

    @Override
    public void onDisable() {
        if (lifecycle != null) {
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
            lifecycle = null;
            playerProfileManager = null;
        }
    }
}
