package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.api.ItemAPI;
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
    private final NamespacedKey idKey, instanceKey, rarityKey, categoryKey, itemLevelKey, requiredLevelKey,
            gearKey, soulboundKey, uniqueKey, resourcepackKey, equipmentSlotKey, setIdKey, abilityKey, abilityCooldownKey;

    public ItemService(Plugin plugin) {
        defs = new ItemDefinitionRegistry(plugin);
        idKey = new NamespacedKey(plugin, "item_id");
        instanceKey = new NamespacedKey(plugin, "instance_id");
        rarityKey = new NamespacedKey(plugin, "rarity");
        categoryKey = new NamespacedKey(plugin, "category");
        itemLevelKey = new NamespacedKey(plugin, "item_level");
        requiredLevelKey = new NamespacedKey(plugin, "required_level");
        gearKey = new NamespacedKey(plugin, "gearscore");
        soulboundKey = new NamespacedKey(plugin, "soulbound");
        uniqueKey = new NamespacedKey(plugin, "unique");
        resourcepackKey = new NamespacedKey(plugin, "resourcepack_id");
        equipmentSlotKey = new NamespacedKey(plugin, "equipment_slot");
        setIdKey = new NamespacedKey(plugin, "set_id");
        abilityKey = new NamespacedKey(plugin, "weapon_ability");
        abilityCooldownKey = new NamespacedKey(plugin, "weapon_ability_cooldown");
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
        pdc.set(idKey, PersistentDataType.STRING, definition.id());
        pdc.set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        pdc.set(categoryKey, PersistentDataType.STRING, definition.category().name());
        pdc.set(itemLevelKey, PersistentDataType.INTEGER, definition.itemLevel());
        pdc.set(requiredLevelKey, PersistentDataType.INTEGER, definition.requiredLevel());
        pdc.set(gearKey, PersistentDataType.DOUBLE,
                Math.round(definition.itemLevel() * rarityMultiplier(definition.rarity()) * definition.gearscoreModifier() * 10.0D) / 10.0D);
        pdc.set(soulboundKey, PersistentDataType.BOOLEAN, definition.soulbound());
        pdc.set(uniqueKey, PersistentDataType.BOOLEAN, definition.unique());
        pdc.set(resourcepackKey, PersistentDataType.STRING, definition.resourcepackId());
        if (!definition.equipmentSlot().isBlank()) pdc.set(equipmentSlotKey, PersistentDataType.STRING, definition.equipmentSlot());
        if (!definition.setId().isBlank()) pdc.set(setIdKey, PersistentDataType.STRING, definition.setId());
        if (!definition.weaponAbility().isBlank()) {
            pdc.set(abilityKey, PersistentDataType.STRING, definition.weaponAbility());
            pdc.set(abilityCooldownKey, PersistentDataType.LONG, definition.weaponAbilityCooldownMillis());
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
        lore.add(Component.text("Ausrüstungswert " + format(pdc.get(gearKey, PersistentDataType.DOUBLE)), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override public boolean isRPGItem(ItemStack item) { return getItemId(item).isPresent(); }
    @Override public boolean isGuildItem(ItemStack item) { return false; }

    @Override public Optional<String> getItemId(ItemStack item) { return read(item, idKey, PersistentDataType.STRING); }
    @Override public Optional<ItemRarity> getRarity(ItemStack item) {
        return read(item, rarityKey, PersistentDataType.STRING).flatMap(value -> parseEnum(ItemRarity.class, value));
    }
    @Override public Optional<ItemCategory> getCategory(ItemStack item) {
        return read(item, categoryKey, PersistentDataType.STRING).flatMap(value -> parseEnum(ItemCategory.class, value));
    }
    @Override public Optional<ItemDefinition> getDefinition(ItemStack item) { return getItemId(item).flatMap(defs::find); }
    @Override public Optional<Integer> getRequiredLevel(ItemStack item) { return read(item, requiredLevelKey, PersistentDataType.INTEGER); }
    @Override public Optional<Double> getGearscore(ItemStack item) { return read(item, gearKey, PersistentDataType.DOUBLE); }

    public Optional<Integer> getItemLevel(ItemStack item) { return read(item, itemLevelKey, PersistentDataType.INTEGER); }
    public Optional<String> getResourcepackId(ItemStack item) { return read(item, resourcepackKey, PersistentDataType.STRING); }
    public Optional<String> getEquipmentSlot(ItemStack item) { return read(item, equipmentSlotKey, PersistentDataType.STRING); }
    public Optional<String> getSetId(ItemStack item) { return read(item, setIdKey, PersistentDataType.STRING); }
    public Optional<String> getWeaponAbility(ItemStack item) { return read(item, abilityKey, PersistentDataType.STRING); }
    public boolean isSoulbound(ItemStack item) { return read(item, soulboundKey, PersistentDataType.BOOLEAN).orElse(false); }
    public boolean isUnique(ItemStack item) { return read(item, uniqueKey, PersistentDataType.BOOLEAN).orElse(false); }

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
