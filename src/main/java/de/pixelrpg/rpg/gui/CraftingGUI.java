package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingCategory;
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
import java.util.Comparator;
import java.util.List;

public final class CraftingGUI implements Listener {
    private static final int SIZE = 54;
    private static final int CATEGORY_SLOTS = 9;
    private static final int FIRST_RECIPE_SLOT = 9;
    private static final int LAST_RECIPE_SLOT = 44;
    private static final int RECIPE_SLOTS = LAST_RECIPE_SLOT - FIRST_RECIPE_SLOT + 1;
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
        List<CraftingCategory> categories = categories(profession);
        if (categories.isEmpty()) return;
        open(player, profession, categories.getFirst(), 0);
    }

    private void open(Player player, Profession profession, CraftingCategory category, int page) {
        CraftingHolder holder = new CraftingHolder(profession, category, Math.max(0, page));
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text("PixelRPG Crafting – " + profession.name() + " – " + category.displayName(), NamedTextColor.GOLD));
        holder.inventory = inventory;
        render(player, holder);
        player.openInventory(inventory);
    }

    private void render(Player player, CraftingHolder holder) {
        Inventory inventory = holder.inventory;
        inventory.clear();

        List<CraftingCategory> categories = categories(holder.profession);
        if (!categories.contains(holder.category)) holder.category = categories.getFirst();
        renderCategories(inventory, categories, holder.category, holder.profession);

        List<CraftRecipe> recipes = recipes(holder.profession, holder.category);
        int pageCount = Math.max(1, (recipes.size() + RECIPE_SLOTS - 1) / RECIPE_SLOTS);
        holder.page = Math.min(holder.page, pageCount - 1);

        int from = holder.page * RECIPE_SLOTS;
        int to = Math.min(from + RECIPE_SLOTS, recipes.size());
        for (int i = from; i < to; i++) {
            inventory.setItem(FIRST_RECIPE_SLOT + (i - from), displayRecipe(recipes.get(i), player));
        }

        if (holder.page > 0) inventory.setItem(LAST_PAGE_SLOT, navigationItem("← Vorherige Seite", Material.ARROW));
        inventory.setItem(PAGE_INFO_SLOT, navigationItem("Seite " + (holder.page + 1) + " / " + pageCount, Material.PAPER));
        if (holder.page + 1 < pageCount) inventory.setItem(NEXT_PAGE_SLOT, navigationItem("Nächste Seite →", Material.ARROW));
    }

    private void renderCategories(Inventory inventory, List<CraftingCategory> categories, CraftingCategory selected, Profession profession) {
        for (int i = 0; i < categories.size() && i < CATEGORY_SLOTS; i++) {
            CraftingCategory category = categories.get(i);
            ItemStack item = new ItemStack(category.icon());
            ItemMeta meta = item.getItemMeta();
            NamedTextColor color = category == selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            meta.displayName(Component.text(category.displayName(), color).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(category == selected ? "Aktive Kategorie" : "Kategorie öffnen", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Rezepte: " + countRecipes(profession, category), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
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
        lore.add(Component.text(recipe.category().displayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
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

    // Listener für Kategorien, Seitenwechsel, Freischalten und Herstellen.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        List<CraftingCategory> categories = categories(holder.profession);
        if (event.getSlot() >= 0 && event.getSlot() < CATEGORY_SLOTS && event.getSlot() < categories.size()) {
            CraftingCategory category = categories.get(event.getSlot());
            if (category != holder.category) {
                holder.category = category;
                holder.page = 0;
                render(player, holder);
            }
            return;
        }

        if (event.getSlot() == LAST_PAGE_SLOT) {
            if (holder.page > 0) {
                holder.page--;
                render(player, holder);
            }
            return;
        }
        if (event.getSlot() == NEXT_PAGE_SLOT) {
            List<CraftRecipe> recipes = recipes(holder.profession, holder.category);
            int pageCount = Math.max(1, (recipes.size() + RECIPE_SLOTS - 1) / RECIPE_SLOTS);
            if (holder.page + 1 < pageCount) {
                holder.page++;
                render(player, holder);
            }
            return;
        }
        if (event.getSlot() < FIRST_RECIPE_SLOT || event.getSlot() > LAST_RECIPE_SLOT) return;

        List<CraftRecipe> recipes = recipes(holder.profession, holder.category);
        int recipeIndex = holder.page * RECIPE_SLOTS + (event.getSlot() - FIRST_RECIPE_SLOT);
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

    private List<CraftingCategory> categories(Profession profession) {
        return craftingService.recipes(profession).stream()
                .map(CraftRecipe::category)
                .distinct()
                .sorted(Comparator.comparingInt(CraftingCategory::ordinal))
                .toList();
    }

    private List<CraftRecipe> recipes(Profession profession, CraftingCategory category) {
        return craftingService.recipes(profession).stream()
                .filter(recipe -> recipe.category() == category)
                .toList();
    }

    private int countRecipes(CraftingCategory category) {
        return craftingService.recipes(currentProfession()).stream()
                .filter(recipe -> recipe.category() == category)
                .count() > Integer.MAX_VALUE ? 0 : (int) craftingService.recipes(currentProfession()).stream()
                .filter(recipe -> recipe.category() == category)
                .count();
    }

    private Profession currentProfession() {
        throw new UnsupportedOperationException();
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
        private CraftingCategory category;
        private int page;
        private Inventory inventory;
        private CraftingHolder(Profession profession, CraftingCategory category, int page) {
            this.profession = profession;
            this.category = category;
            this.page = page;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}
