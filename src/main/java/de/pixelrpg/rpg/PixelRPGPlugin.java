package de.pixelrpg.rpg;

import de.pixelrpg.rpg.core.LifecycleCoordinator;
import de.pixelrpg.rpg.player.PlayerProfileLifecycleListener;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Application composition root for the rebuilt PixelRPG runtime.
 *
 * <p>Feature modules are intentionally not registered here until their
 * dependencies and persistence boundaries have been rebuilt and verified.</p>
 */
public final class PixelRPGPlugin extends JavaPlugin {
    private LifecycleCoordinator lifecycle;
    private PlayerProfileManager playerProfileManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lifecycle = new LifecycleCoordinator(getLogger());
        playerProfileManager = lifecycle.register(new PlayerProfileManager(this));
        playerProfileManager.initialize(getConfig());
        getServer().getPluginManager().registerEvents(new PlayerProfileLifecycleListener(playerProfileManager), this);
    }

    @Override
    public void onDisable() {
        if (lifecycle != null) {
            lifecycle.close();
            lifecycle = null;
            playerProfileManager = null;
        }
    }
}
