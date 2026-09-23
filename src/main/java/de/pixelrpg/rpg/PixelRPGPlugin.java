package de.pixelrpg.rpg;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.config.JsonDataManager;
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
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PixelRPGPlugin extends JavaPlugin {
    private LifecycleCoordinator lifecycle;
    private PlayerProfileManager playerProfileManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lifecycle = new LifecycleCoordinator(getLogger());

        new JsonDataManager(this).initialize();
        RPGKeys keys = new RPGKeys(this);

        playerProfileManager = lifecycle.register(new PlayerProfileManager(this));
        playerProfileManager.initialize(getConfig());

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
        npcRuntime.loadAsync();

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
            lifecycle = null;
            playerProfileManager = null;
        }
    }
}
