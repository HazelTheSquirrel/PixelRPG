package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.util.*;

public final class ItemService implements ItemAPI {
    private final ItemDefinitionRegistry defs;
    private final ItemDefinitionRegistry defs;

    public ItemService(Plugin plugin) {
        defs = new ItemDefinitionRegistry(plugin);
    }

    @Override public Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        return defs.findByMaterial(material, rarity, Math.clamp(itemLevel, 1, 99)).map(this::create);
    }

    @Override public Optional<ItemStack> createItem(String itemId) {
        return defs.find(itemId).filter(definition -> !definition.adminOnly()).map(this::create);
    }

    private ItemStack create(ItemDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, definition.id());
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.rarity(), PersistentDataType.STRING, definition.rarity().name());
        pdc.set(RPGKeys.Item.category(), PersistentDataType.STRING, definition.category().name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, definition.itemLevel());
        pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, definition.requiredLevel());
        pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE,
                Math.round(definition.itemLevel() * rarityMultiplier(definition.rarity()) * definition.gearscoreModifier() * 10.0D) / 10.0D);
        pdc.set(RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN, definition.soulbound());
        pdc.set(RPGKeys.Item.unique(), PersistentDataType.BOOLEAN, definition.unique());
        pdc.set(RPGKeys.Item.resourcepackId(), PersistentDataType.STRING, definition.resourcepackId());
        if (!definition.equipmentSlot().isBlank()) pdc.set(RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING, definition.equipmentSlot());
        if (!definition.setId().isBlank()) pdc.set(RPGKeys.Item.setId(), PersistentDataType.STRING, definition.setId());
        if (!definition.weaponAbility().isBlank()) {
            pdc.set(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING, definition.weaponAbility());
            pdc.set(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, definition.weaponAbilityCooldownMillis());
        }

        meta.displayName(Component.text(definition.name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Benötigt Level " + definition.requiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        if (definition.soulbound()) lore.add(Component.text("⚡ Seelengebunden", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        if (definition.unique()) lore.add(Component.text("EINZIGARTIG • 1/1", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        if (!definition.weaponAbility().isBlank()) {
            lore.add(Component.text("Fähigkeit: " + definition.weaponAbility(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(Math.max(0L, definition.weaponAbilityCooldownMillis()) + "ms Abklingzeit", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Ausrüstungswert " + format(pdc.get(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE)), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override public boolean isRPGItem(ItemStack item) { return getItemId(item).isPresent(); }
    @Override public boolean isGuildItem(ItemStack item) { return false; }

    @Override public Optional<String> getItemId(ItemStack item) { return read(item, RPGKeys.Item.itemId(), PersistentDataType.STRING); }
    @Override public Optional<ItemRarity> getRarity(ItemStack item) {
        return read(item, RPGKeys.Item.rarity(), PersistentDataType.STRING).flatMap(value -> parseEnum(ItemRarity.class, value));
    }
    @Override public Optional<ItemCategory> getCategory(ItemStack item) {
        return read(item, RPGKeys.Item.category(), PersistentDataType.STRING).flatMap(value -> parseEnum(ItemCategory.class, value));
    }
    @Override public Optional<ItemDefinition> getDefinition(ItemStack item) { return getItemId(item).flatMap(defs::find); }
    @Override public Optional<Integer> getRequiredLevel(ItemStack item) { return read(item, RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER); }
    @Override public Optional<Double> getGearscore(ItemStack item) { return read(item, RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE); }

    public Optional<Integer> getItemLevel(ItemStack item) { return read(item, RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER); }
    public Optional<String> getResourcepackId(ItemStack item) { return read(item, RPGKeys.Item.resourcepackId(), PersistentDataType.STRING); }
    public Optional<String> getEquipmentSlot(ItemStack item) { return read(item, RPGKeys.Item.equipmentSlot(), PersistentDataType.STRING); }
    public Optional<String> getSetId(ItemStack item) { return read(item, RPGKeys.Item.setId(), PersistentDataType.STRING); }
    public Optional<String> getWeaponAbility(ItemStack item) { return read(item, RPGKeys.Item.weaponAbility(), PersistentDataType.STRING); }
    public boolean isSoulbound(ItemStack item) { return read(item, RPGKeys.Item.soulbound(), PersistentDataType.BOOLEAN).orElse(false); }
    public boolean isUnique(ItemStack item) { return read(item, RPGKeys.Item.unique(), PersistentDataType.BOOLEAN).orElse(false); }

    private static double rarityMultiplier(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 1.15D;
            case RARE -> 1.35D;
            case EPIC -> 1.60D;
            case LEGENDARY -> 2.0D;
            case UNIQUE -> 2.5D;
        };
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    private <T, Z> Optional<Z> read(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(key, type));
    }

    private static <E extends Enum<E>> Optional<E> parseEnum(Class<E> type, String raw) {
        try { return Optional.of(Enum.valueOf(type, raw.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
}
