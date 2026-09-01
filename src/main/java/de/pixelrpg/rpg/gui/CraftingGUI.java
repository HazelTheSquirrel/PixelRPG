package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestText;
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
    private static final int RECIPE_SLOTS = 45;
    private static final int LAST_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private final CraftingService craftingService;
    private final PlayerProfileManager profileManager;

    public CraftingGUI(CraftingService craftingService, PlayerProfileManager profileManager) {
        this.craftingService = craftingService;
        this.profileManager = profileManager;
    }

    public void open(Player player, Profession profession) {
        open(player, profession, 0);
    }

    private void open(Player player, Profession profession, int page) {
        CraftingHolder holder = new CraftingHolder(profession, Math.max(0, page));
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text("PixelRPG Crafting – " + profession.name(), NamedTextColor.GOLD));
        holder.inventory = inventory;
        render(player, holder);
        player.openInventory(inventory);
    }

    private void render(Player player, CraftingHolder holder) {
        Inventory inventory = holder.inventory;
        inventory.clear();
        List<CraftRecipe> recipes = craftingService.recipes(holder.profession);
        int pageCount = Math.max(1, (recipes.size() + RECIPE_SLOTS - 1) / RECIPE_SLOTS);
        holder.page = Math.min(holder.page, pageCount - 1);

        int from = holder.page * RECIPE_SLOTS;
        int to = Math.min(from + RECIPE_SLOTS, recipes.size());
        for (int i = from; i < to; i++) {
            inventory.setItem(i - from, displayRecipe(recipes.get(i), player));
        }

        if (holder.page > 0) inventory.setItem(LAST_PAGE_SLOT, navigationItem("← Vorherige Seite", Material.ARROW));
        inventory.setItem(PAGE_INFO_SLOT, navigationItem("Seite " + (holder.page + 1) + " / " + pageCount, Material.PAPER));
        if (holder.page + 1 < pageCount) inventory.setItem(NEXT_PAGE_SLOT, navigationItem("Nächste Seite →", Material.ARROW));
    }

    private ItemStack navigationItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack displayRecipe(CraftRecipe recipe, Player player) {
        ItemStack item = new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(recipe.displayName(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        int professionLevel = profile == null ? Profession.MIN_LEVEL : profile.getProfessionLevel(recipe.profession());
        boolean unlocked = recipe.unlockedByDefault() || (profile != null && profile.hasUnlockedRecipe(recipe.id()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(recipe.vanillaRecipe() ? "Vanilla-Rezept" : "PixelRPG-Rezept", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Benötigtes Berufslevel: " + recipe.requiredProfessionLevel(),
                professionLevel >= recipe.requiredProfessionLevel() ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Materialien:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        recipe.costs().forEach((material, amount) -> {
            int available = count(player, material);
            lore.add(Component.text("- " + amount + "x " + QuestText.itemNamePlain(material.name()) + " (" + available + ")",
                    available >= amount ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        });
        recipe.itemCosts().forEach((itemId, amount) -> {
            int available = countItemId(player, itemId);
            lore.add(Component.text("- " + amount + "x " + itemId + " (" + available + ")",
                    available >= amount ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        });

        lore.add(Component.text(" "));
        if (!unlocked) {
            if (!recipe.requiredQuestId().isBlank()) lore.add(Component.text("Quest: " + recipe.requiredQuestId(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            if (recipe.unlockPrice() > 0L) lore.add(Component.text("Freischalten: " + recipe.unlockPrice() + " Gold", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Klicken zum Freischalten", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Klicken zum Herstellen", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        if (event.getSlot() == LAST_PAGE_SLOT) {
            if (holder.page > 0) {
                holder.page--;
                render(player, holder);
            }
            return;
        }
        if (event.getSlot() == NEXT_PAGE_SLOT) {
            List<CraftRecipe> recipes = craftingService.recipes(holder.profession);
            int pageCount = Math.max(1, (recipes.size() + RECIPE_SLOTS - 1) / RECIPE_SLOTS);
            if (holder.page + 1 < pageCount) {
                holder.page++;
                render(player, holder);
            }
            return;
        }
        if (event.getSlot() < 0 || event.getSlot() >= RECIPE_SLOTS) return;

        List<CraftRecipe> recipes = craftingService.recipes(holder.profession);
        int recipeIndex = holder.page * RECIPE_SLOTS + event.getSlot();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;
        CraftRecipe recipe = recipes.get(recipeIndex);
        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean unlocked = recipe.unlockedByDefault() || profile.hasUnlockedRecipe(recipe.id());
        if (!unlocked) {
            var unlock = craftingService.unlockRecipe(player, recipe);
            if (!unlock.success()) {
                player.sendMessage(Component.text(unlock.message(), NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Rezept freigeschaltet.", NamedTextColor.GREEN));
            render(player, holder);
            return;
        }

        var result = craftingService.craft(player, recipe.id());
        if (!result.success()) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Hergestellt: " + recipe.displayName(), NamedTextColor.GREEN));
        render(player, holder);
    }

    private int count(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) if (item != null && item.getType() == material) count += item.getAmount();
        return count;
    }

    private int countItemId(Player player, String requiredId) {
        String normalized = normalizeItemId(requiredId);
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            String itemId = item.getItemMeta().getPersistentDataContainer().get(de.pixelrpg.rpg.core.RPGKeys.Item.itemId(), org.bukkit.persistence.PersistentDataType.STRING);
            if (itemId != null && normalizeItemId(itemId).equals(normalized)) count += item.getAmount();
        }
        return count;
    }

    private String normalizeItemId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }

    private static final class CraftingHolder implements InventoryHolder {
        private final Profession profession;
        private int page;
        private Inventory inventory;
        private CraftingHolder(Profession profession, int page) {
            this.profession = profession;
            this.page = page;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}
