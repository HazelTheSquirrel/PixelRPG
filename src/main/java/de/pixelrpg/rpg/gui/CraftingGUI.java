package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingRecipeRegistry;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class CraftingGUI implements Listener {
    private static final int SIZE = 54;

    private final CraftingService craftingService;
    private final PlayerProfileManager profileManager;

    public CraftingGUI(CraftingService craftingService, PlayerProfileManager profileManager) {
        this.craftingService = craftingService;
        this.profileManager = profileManager;
    }

    public void open(Player player, Profession profession) {
        CraftingHolder holder = new CraftingHolder(profession);
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text("PixelRPG Crafting – " + profession.name(), NamedTextColor.GOLD));
        holder.inventory = inventory;

        List<CraftRecipe> recipes = CraftingRecipeRegistry.getRecipes(profession);
        for (int i = 0; i < recipes.size() && i < 45; i++) {
            CraftRecipe recipe = recipes.get(i);
            inventory.setItem(i, displayRecipe(recipe, player));
        }

        player.openInventory(inventory);
    }

    private ItemStack displayRecipe(CraftRecipe recipe, Player player) {
        ItemStack item = new ItemStack(recipe.resultMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(recipe.displayName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        int professionLevel = profileManager.getProfile(player.getUniqueId())
                .map(profile -> profile.getProfessionLevel(recipe.profession()))
                .orElse(Profession.MIN_LEVEL);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Rarität: ", NamedTextColor.GRAY)
                .append(recipe.rarity().displayName())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Benötigtes Berufslevel: " + recipe.requiredProfessionLevel(),
                professionLevel >= recipe.requiredProfessionLevel() ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Materialien:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        recipe.costs().forEach((material, amount) -> lore.add(
                Component.text("- " + amount + "x " + pretty(material), NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.text(" "));
        lore.add(Component.text("Klicken zum Herstellen", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Verarbeitet das Herstellen eines ausgewählten PixelRPG-Rezepts.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getSlot() < 0 || event.getSlot() >= 45) return;

        List<CraftRecipe> recipes = CraftingRecipeRegistry.getRecipes(holder.profession);
        if (event.getSlot() >= recipes.size()) return;

        CraftingService.CraftResult result = craftingService.craft(player, recipes.get(event.getSlot()).id());
        if (!result.success()) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Hergestellt: " + recipes.get(event.getSlot()).displayName(), NamedTextColor.GREEN));
        open(player, holder.profession);
    }

    private static String pretty(Material material) {
        String raw = material.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static final class CraftingHolder implements InventoryHolder {
        private final Profession profession;
        private Inventory inventory;

        private CraftingHolder(Profession profession) {
            this.profession = profession;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
