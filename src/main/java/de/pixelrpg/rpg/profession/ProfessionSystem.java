package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfileManager;

import java.util.Objects;

/** Wires profession progression and the complete crafting registry into PixelRPG. */
public final class ProfessionSystem {
    private final PixelRPGPlugin plugin;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private CraftingService craftingService;

    public ProfessionSystem(PixelRPGPlugin plugin, PlayerProfileManager profileManager) {
        this.plugin = Objects.requireNonNull(plugin);
        this.profileManager = Objects.requireNonNull(profileManager);
        this.professionService = new ProfessionService(profileManager);
        this.craftingRecipeRegistry = new CraftingRecipeRegistry();
        this.craftingRecipeRegistry.load(plugin);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(new ProfessionActivityListener(plugin, professionService), plugin);
    }

    public ProfessionService professionService() { return professionService; }

    public CraftingService craftingService() {
        if (craftingService == null) {
            ItemService itemService = plugin.getItemService();
            if (itemService == null) throw new IllegalStateException("ItemService must be initialized before the crafting service is requested.");
            craftingService = new CraftingService(professionService, profileManager, itemService, craftingRecipeRegistry);
        }
        return craftingService;
    }

    public CraftingRecipeRegistry craftingRecipeRegistry() { return craftingRecipeRegistry; }
}
