package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Central item creation and identification pipeline for every PixelRPG item. */
public final class ItemService implements ItemAPI {
    private final ItemDefinitionRegistry definitions;
    private final UniqueItemService uniqueItems;

    public ItemService(Plugin plugin) {
        this(plugin, new UniqueItemService(plugin));
    }

    public ItemService(Plugin plugin, UniqueItemService uniqueItems) {
        this.definitions = new ItemDefinitionRegistry(plugin);
        this.uniqueItems = uniqueItems;
    }

    @Override public Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        if (rarity == ItemRarity.UNIQUE) return Optional.empty();
        return RPGItemBuilder.createItem(material, rarity, itemLevel);
    }
    @Override public Optional<ItemStack> createItem(String itemId) { return createDefinedItem(itemId, 0, false); }
    public Optional<ItemStack> createAdminItem(String itemId) { return createDefinedItem(itemId, 0, true); }
    public Optional<ItemStack> createItem(String itemId, int itemLevel) { return createDefinedItem(itemId, Math.clamp(itemLevel, 1, 99), false); }
    public Optional<ItemStack> createVanillaReward(Material material, ItemRarity rarity, int itemLevel) {
        return createItem(material, rarity, Math.clamp(itemLevel, 1, 99));
    }

    /**
     * Creates a crafted item using the same semantics as the functional main branch.
     * Unsupported materials remain ordinary vanilla items while retaining the recipe identity.
     */
    public ItemStack createCraftedItem(String itemId, String displayName, Material material, ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (itemLevel < 1 || itemLevel > 99) throw new IllegalArgumentException("itemLevel must be between 1 and 99");
        if (material == null) throw new IllegalArgumentException("material must not be null");
        if (rarity == null) throw new IllegalArgumentException("rarity must not be null");
        if (rarity == ItemRarity.UNIQUE) throw new IllegalArgumentException("UNIQUE items can only be granted by an administrator");

        Optional<ItemStack> rpgItem = RPGItemBuilder.createItem(material, rarity, itemLevel);
        ItemStack item = rpgItem.orElseGet(() -> new ItemStack(material));
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, normalize(itemId));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);

        if (rpgItem.isPresent()) {
            pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, itemLevel);
            pdc.set(RPGKeys.Item.unique(), PersistentDataType.BOOLEAN, false);
            double gearscore = Math.round(itemLevel * rarity.getStatMultiplier() * 10.0D) / 10.0D;
            pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE, gearscore);
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            if (!lore.isEmpty()) lore.add(Component.text(" "));
            lore.add(Component.text("Hergestellter Gegenstand", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Ausrüstungswert " + format(gearscore), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }

        meta.displayName(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public List<ItemDefinition> definitions() { return definitions.all().stream().toList(); }
    @Override public boolean isRPGItem(ItemStack item) { return getItemId(item).isPresent(); }
    @Override public boolean isGuildItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN));
    }
    @Override public Optional<String> getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemId(), PersistentDataType.STRING);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
    @Override public Optional<ItemRarity> getRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        if (raw == null) return Optional.empty();
        try { return Optional.of(ItemRarity.valueOf(raw)); } catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
    @Override public Optional<ItemCategory> getCategory(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.category(), PersistentDataType.STRING);
        if (raw == null) return Optional.empty();
        try { return Optional.of(ItemCategory.valueOf(raw)); } catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
    @Override public Optional<ItemDefinition> getDefinition(ItemStack item) { return getItemId(item).flatMap(definitions::find); }
    @Override public Optional<Integer> getRequiredLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER));
    }
    @Override public Optional<Double> getGearscore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE));
    }

    private Optional<ItemStack> createDefinedItem(String itemId, int explicitItemLevel, boolean admin) {
        ItemDefinition definition = definitions.find(itemId).orElse(null);
        if (definition == null) return Optional.empty();
        if (definition.adminOnly() && !admin) return Optional.empty();
        if (definition.unique() && !admin) return Optional.empty();
        if (definition.unique() && !uniqueItems.claim(definition)) return Optional.empty();
        int itemLevel = explicitItemLevel > 0 ? explicitItemLevel : definition.itemLevel();
        ItemStack item = RPGItemBuilder.createItem(definition.id(), definition.name(), definition.material(), definition.rarity(), itemLevel).orElse(null);
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, definition.id());
        pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, definition.requiredLevel());
        pdc.set(RPGKeys.Item.resourcepackId(), PersistentDataType.STRING, definition.resourcepackId());
        pdc.set(RPGKeys.Item.unique(), PersistentDataType.BOOLEAN, definition.unique());
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        if (!definition.equipmentSlot().isBlank()) pdc.set(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING, definition.equipmentSlot());
        if (!definition.setId().isBlank()) pdc.set(RPGKeys.Item.setId(), PersistentDataType.STRING, definition.setId());
        double gearscore = Math.round(itemLevel * definition.rarity().getStatMultiplier() * definition.gearscoreModifier() * 10.0D) / 10.0D;
        pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE, gearscore);
        if (definition.soulbound()) pdc.set(RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN, true);
        if (!definition.weaponAbility().isBlank()) {
            pdc.set(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING, definition.weaponAbility());
            pdc.set(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, definition.weaponAbilityCooldownMillis());
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(component -> component instanceof TextComponent text && text.content().startsWith("Benötigt Level "));
        lore.add(Math.min(2, lore.size()), Component.text("Benötigt Level " + definition.requiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        if (definition.soulbound()) lore.add(0, Component.text("⚡ Seelengebunden", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        if (definition.unique()) lore.add(0, Component.text("EINZIGARTIG • 1/1", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        if (!definition.weaponAbility().isBlank()) {
            boolean ranged = definition.category() == ItemCategory.RANGED_WEAPON;
            lore.add(Component.text("Fähigkeit: " + definition.weaponAbility(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text((ranged ? "Beim Loslassen" : "Rechtsklick") + " • " + Math.max(0L, definition.weaponAbilityCooldownMillis()) + "ms Abklingzeit", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (definition.category().getProfile() == ItemStatProfile.ARMOR) {
            double critChance = Math.round(itemLevel * definition.rarity().getStatMultiplier() * 0.05D * 10.0D) / 10.0D;
            pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, critChance);
            lore.add(Component.text("+" + format(critChance) + "% Kritische Trefferchance", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Ausrüstungswert " + format(gearscore), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.displayName(Component.text(definition.name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return Optional.of(item);
    }
    private static String normalize(String value) { String normalized = value.trim().toLowerCase(Locale.ROOT); return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized; }
    private static String format(double value) { if (Math.abs(value - Math.rint(value)) < 0.0001D) return Long.toString(Math.round(value)); return String.format(Locale.ROOT, "%.1f", value); }
}
