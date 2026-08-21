package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.command.impl.CraftingCommand;
import de.pixelrpg.rpg.player.PlayerProfileManager;

import java.util.Objects;

/** Wires the profession progression, gathering and crafting services into PixelRPG. */
public final class ProfessionSystem {
    private final PixelRPGPlugin plugin;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingService craftingService;

    public ProfessionSystem(PixelRPGPlugin plugin, PlayerProfileManager profileManager) {
        this.plugin = Objects.requireNonNull(plugin);
        this.profileManager = Objects.requireNonNull(profileManager);
        this.professionService = new ProfessionService(profileManager);
        this.craftingService = new CraftingService(professionService, profileManager);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(
                new ProfessionGatheringListener(professionService), plugin);

        CraftingCommand command = new CraftingCommand(craftingService);
        if (plugin.getCommand("rpgcraft") != null) {
            plugin.getCommand("rpgcraft").setExecutor(command);
            plugin.getCommand("rpgcraft").setTabCompleter(command);
        }
    }

    public ProfessionService professionService() {
        return professionService;
    }

    public CraftingService craftingService() {
        return craftingService;
    }
}
