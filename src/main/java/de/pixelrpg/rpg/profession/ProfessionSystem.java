package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;

import java.util.Objects;

/** Wires profession progression, gathering and the complete crafting registry into PixelRPG. */
public final class ProfessionSystem {
    private final PixelRPGPlugin plugin;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private final CraftingService craftingService;

    public ProfessionSystem(PixelRPGPlugin plugin, PlayerProfileManager profileManager) {
        this(plugin, profileManager, new ItemService());
    }

    public ProfessionSystem(PixelRPGPlugin plugin, PlayerProfileManager profileManager, ItemService itemService) {
        this.plugin = Objects.requireNonNull(plugin);
        this.profileManager = Objects.requireNonNull(profileManager);
        this.professionService = new ProfessionService(profileManager);
        this.craftingRecipeRegistry = new CraftingRecipeRegistry();
        this.craftingRecipeRegistry.load(plugin);
        this.craftingService = new CraftingService(professionService, profileManager, itemService, craftingRecipeRegistry);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(new ProfessionGatheringListener(professionService), plugin);
    }

    public ProfessionService professionService() { return professionService; }
    public CraftingService craftingService() { return craftingService; }
    public CraftingRecipeRegistry craftingRecipeRegistry() { return craftingRecipeRegistry; }
}
