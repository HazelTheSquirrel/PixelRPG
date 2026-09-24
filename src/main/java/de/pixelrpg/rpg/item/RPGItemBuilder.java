package de.pixelrpg.rpg.item;

import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGItemBuilder {
    private static double growthMultiplier = 2.0D;
    private static double weaponBaseDamage = 3.0D;
    private static double weaponBaseCritChance = 0.5D;
    private static double weaponBaseCritDamage = 0.05D;
    private static double weaponBaseReach = 0.10D;
    private static double weaponBaseLifesteal = 0.25D;
    private static double armorBase = 0.7D;
    private static double healthBase = 1.2D;
    private static double movementSpeedBase = 0.001D;
    private static double toolBaseEfficiency = 1.0D;

    private RPGItemBuilder() {}

    public static void configureScaling(Plugin plugin) {
        try {
            JsonObject root = new JsonDataManager(plugin).load("item-scaling.json");
            growthMultiplier = positive(root, "growthMultiplier", growthMultiplier);
            JsonObject base = root.has("baseStats") ? root.getAsJsonObject("baseStats") : null;
            weaponBaseDamage = positive(base, "weaponDamage", weaponBaseDamage);
            weaponBaseCritChance = positive(base, "weaponCritChance", weaponBaseCritChance);
            weaponBaseCritDamage = positive(base, "weaponCritDamage", weaponBaseCritDamage);
            weaponBaseReach = positive(base, "weaponReach", weaponBaseReach);
            weaponBaseLifesteal = positive(base, "weaponLifesteal", weaponBaseLifesteal);
            armorBase = positive(base, "armor", armorBase);
            healthBase = positive(base, "health", healthBase);
            movementSpeedBase = positive(base, "movementSpeed", movementSpeedBase);
            toolBaseEfficiency = positive(base, "toolEfficiency", toolBaseEfficiency);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load item-scaling.json; using deterministic defaults: " + exception.getMessage());
        }
    }

    public static java.util.Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        if (material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();
        ItemCategory category = GearCategoryRegistry.resolve(material).orElse(null);
        if (category == null) return java.util.Optional.empty();
        return createItem(generatedItemId(material, category, rarity, itemLevel),
                generatedDisplayName(material, category, rarity, itemLevel), material, rarity, itemLevel);
    }

    public static java.util.Optional<ItemStack> createItem(String itemId, String displayName,
                                                            Material material, ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank() || displayName == null || displayName.isBlank()
                || material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) {
            return java.util.Optional.empty();
        }
        ItemCategory category = GearCategoryRegistry.resolve(material).orElse(null);
        if (category == null) return java.util.Optional.empty();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, normalize(itemId));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.rarity(), PersistentDataType.STRING, rarity.name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.category(), PersistentDataType.STRING, category.name());
        pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE,
                Math.round(itemLevel * rarity.getStatMultiplier() * 10.0D) / 10.0D);
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);

        List<Component> lore = new ArrayList<>();
        lore.add(line(rarity.displayName()));
        lore.add(line(Component.text("Gegenstandslevel " + itemLevel, NamedTextColor.YELLOW)));
        lore.add(line(Component.text("Benötigt Level " + itemLevel, NamedTextColor.RED)));
        lore.add(Component.text(" "));

        switch (category.getProfile()) {
            case WEAPON, ARMOR, SHIELD -> addEquipmentStats(lore, pdc, rarity, levelScaling(itemLevel));
            case TOOL -> addToolStats(lore, pdc, rarity, levelScaling(itemLevel));
            case FOOD -> { }
        }

        meta.displayName(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return java.util.Optional.of(item);
    }

    public static ItemStack withWeaponAbility(ItemStack item, String abilityId, long cooldownMillis) {
        if (item == null || abilityId == null || abilityId.isBlank()) throw new IllegalArgumentException("Invalid weapon ability");
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING, abilityId);
        pdc.set(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, Math.max(0L, cooldownMillis));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.text("Fähigkeit", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(abilityId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rechtsklick", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        result.setItemMeta(meta);
        return result;
    }

    private static void addEquipmentStats(List<Component> lore, PersistentDataContainer pdc,
                                           ItemRarity rarity, double levelFactor) {
        List<EquipmentStat> pool = new ArrayList<>(List.of(EquipmentStat.values()));
        Collections.shuffle(pool, ThreadLocalRandom.current());
        int count = switch (rarity) {
            case COMMON -> 2;
            case UNCOMMON -> 3;
            case RARE -> 4;
            case EPIC -> 5;
            case LEGENDARY, UNIQUE -> 6;
        };
        Set<EquipmentStat> selected = EnumSet.copyOf(pool.subList(0, Math.min(count, pool.size())));
        double multiplier = rarity.getStatMultiplier();
        for (EquipmentStat stat : selected) {
            double value = round(stat.baseValue() * levelFactor * multiplier
                    * ThreadLocalRandom.current().nextDouble(0.50D, Math.nextUp(1.50D)));
            stat.write(pdc, value);
            lore.add(line(stat.lore(value)));
        }
    }

    private static void addToolStats(List<Component> lore, PersistentDataContainer pdc,
                                     ItemRarity rarity, double levelFactor) {
        double value = round(toolBaseEfficiency * levelFactor * rarity.getStatMultiplier());
        pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, value);
        lore.add(line(Component.text("+" + format(value) + " Effizienz", NamedTextColor.YELLOW)));
    }

    private enum EquipmentStat {
        HP(healthBase, RPGKeys.Item.healthBonus(), " LP", NamedTextColor.GREEN),
        ARMOR(armorBase, RPGKeys.Item.armorValue(), " Rüstung", NamedTextColor.BLUE),
        MOVEMENT_SPEED(movementSpeedBase, RPGKeys.Item.movementSpeed(), "% Bewegungsgeschwindigkeit", NamedTextColor.WHITE),
        REACH(weaponBaseReach, RPGKeys.Item.reachBonus(), " Reichweite", NamedTextColor.AQUA),
        ATTACK_POWER(weaponBaseDamage, RPGKeys.Item.attackPower(), " Angriffskraft", NamedTextColor.GOLD),
        CRIT(weaponBaseCritChance, RPGKeys.Item.critChance(), "% Kritische Trefferchance", NamedTextColor.LIGHT_PURPLE),
        CRIT_DAMAGE(weaponBaseCritDamage, RPGKeys.Item.critDamage(), "% Kritischer Schaden", NamedTextColor.LIGHT_PURPLE),
        LIFESTEAL(weaponBaseLifesteal, RPGKeys.Item.lifestealPercent(), "% Lebensraub", NamedTextColor.DARK_RED);

        private final double base;
        private final org.bukkit.NamespacedKey key;
        private final String suffix;
        private final NamedTextColor color;

        EquipmentStat(double base, org.bukkit.NamespacedKey key, String suffix, NamedTextColor color) {
            this.base = base;
            this.key = key;
            this.suffix = suffix;
            this.color = color;
        }
        double baseValue() { return base; }
        void write(PersistentDataContainer pdc, double value) { pdc.set(key, PersistentDataType.DOUBLE, value); }
        Component lore(double value) {
            double display = this == MOVEMENT_SPEED || this == CRIT_DAMAGE ? value * 100.0D : value;
            return Component.text("+" + format(display) + suffix, color);
        }
    }

    private static double levelScaling(int itemLevel) {
        if (itemLevel <= Level.MIN_LEVEL) return 1.0D;
        double progress = (itemLevel - 1.0D) / (Level.MAX_NORMAL_LEVEL - 1.0D);
        return 1.0D + Math.max(0.0D, growthMultiplier - 1.0D) * Math.clamp(progress, 0.0D, 1.0D);
    }

    private static double positive(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isNumber()) return fallback;
        double value = object.get(key).getAsDouble();
        return Double.isFinite(value) && value > 0.0D ? value : fallback;
    }

    private static Component line(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static String generatedItemId(Material material, ItemCategory category, ItemRarity rarity, int level) {
        return "pixelrpg:item/" + category.name().toLowerCase(Locale.ROOT) + "/"
                + material.name().toLowerCase(Locale.ROOT) + "/"
                + rarity.name().toLowerCase(Locale.ROOT) + "/lvl_" + level;
    }

    private static String generatedDisplayName(Material material, ItemCategory category, ItemRarity rarity, int level) {
        String base = prettyMaterial(material);
        String prefix = switch (rarity) {
            case COMMON -> "Abgenutzte";
            case UNCOMMON -> "Veredelte";
            case RARE -> "Meisterhafte";
            case EPIC -> "Epische";
            case LEGENDARY -> "Legendäre";
            case UNIQUE -> "Einzigartige";
        };
        String noun = switch (category.getProfile()) {
            case WEAPON -> switch (material) {
                case IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD, WOODEN_SWORD, STONE_SWORD, COPPER_SWORD -> "Klinge";
                case IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE, WOODEN_AXE, STONE_AXE, COPPER_AXE -> "Streitaxt";
                default -> base;
            };
            case ARMOR -> base;
            case SHIELD -> "Schild";
            case TOOL, FOOD -> base;
        };
        return prefix + " " + noun + " " + level;
    }

    private static String prettyMaterial(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? Long.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }
}
