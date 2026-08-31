package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.balance.BalanceModel;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGItemBuilder {
    private static final long WEAPON_ABILITY_COOLDOWN_MILLIS = 6_000L;
    private static double toolBaseEfficiency = 1.0D;

    private RPGItemBuilder() {}

    public static void configureScaling(Plugin plugin) {
        try {
            var root = new JsonDataManager(plugin).load("item-scaling.json");
            var baseStats = root.has("baseStats") && root.get("baseStats").isJsonObject() ? root.getAsJsonObject("baseStats") : null;
            toolBaseEfficiency = positive(baseStats, "toolEfficiency", toolBaseEfficiency);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load item-scaling.json; using safe defaults: " + exception.getMessage());
        }
    }

    public static java.util.Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        if (material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();
        var categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return java.util.Optional.empty();
        ItemCategory category = categoryOpt.get();
        return createItem(generatedItemId(material, category, rarity, itemLevel),
                generatedDisplayName(material, category, rarity, itemLevel), material, rarity, itemLevel, true);
    }

    public static java.util.Optional<ItemStack> createItem(String itemId, String displayName, Material material,
                                                            ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank() || displayName == null || displayName.isBlank()
                || material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();
        if (GearCategoryRegistry.resolve(material).isEmpty()) return java.util.Optional.empty();
        return createItem(itemId, displayName, material, rarity, itemLevel, true);
    }

    private static java.util.Optional<ItemStack> createItem(String itemId, String displayName, Material material,
                                                             ItemRarity rarity, int itemLevel, boolean guildItem) {
        var categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return java.util.Optional.empty();
        ItemCategory category = categoryOpt.get();
        ItemStack item = ItemStack.of(material);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, normalizeItemId(itemId));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.rarity(), PersistentDataType.STRING, rarity.name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.category(), PersistentDataType.STRING, category.name());
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, guildItem);

        List<Component> lore = new ArrayList<>();
        lore.add(line(rarity.displayName()));
        lore.add(line(Component.text("Gegenstandslevel " + itemLevel, NamedTextColor.YELLOW)));
        lore.add(line(Component.text("Benötigt Level " + itemLevel, NamedTextColor.RED)));
        lore.add(Component.text(" "));
        switch (category.getProfile()) {
            case WEAPON -> addRandomEquipmentStats(lore, pdc, BalanceModel.pool(true, false, false), rarity, itemLevel, category);
            case ARMOR -> addRandomEquipmentStats(lore, pdc, BalanceModel.pool(false, true, false), rarity, itemLevel, category);
            case SHIELD -> addRandomEquipmentStats(lore, pdc, BalanceModel.pool(false, false, true), rarity, itemLevel, category);
            case TOOL -> addToolStats(lore, pdc, itemLevel);
        }
        meta.displayName(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        if (category.getProfile() == ItemStatProfile.WEAPON) item = applyMaterialWeaponAbility(item).orElse(item);
        return java.util.Optional.of(item);
    }

    public static ItemStack withWeaponAbility(ItemStack item, String abilityId, long cooldownMillis) {
        if (abilityId == null || abilityId.isBlank()) throw new IllegalArgumentException("abilityId must not be blank");
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING, abilityId);
        pdc.set(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, Math.max(0L, cooldownMillis));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty() && !lore.getLast().equals(Component.text(" "))) lore.add(Component.text(" "));
        lore.add(Component.text("Fähigkeit", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(abilityId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rechtsklick", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore); result.setItemMeta(meta); return result;
    }

    private static java.util.Optional<ItemStack> applyMaterialWeaponAbility(ItemStack item) {
        String id = weaponAbilityId(item.getType());
        return id == null ? java.util.Optional.of(item) : java.util.Optional.of(withWeaponAbility(item, id, WEAPON_ABILITY_COOLDOWN_MILLIS));
    }

    private static String weaponAbilityId(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> "Holzklingenfeger"; case STONE_SWORD -> "Steinbrecher"; case COPPER_SWORD -> "Kupfersturm";
            case IRON_SWORD -> "Eiserne Bastion"; case GOLDEN_SWORD -> "Goldene Klingenflut"; case DIAMOND_SWORD -> "Diamantspalter";
            case NETHERITE_SWORD -> "Netherit-Eruption"; case COPPER_AXE -> "Kupferstatische Axt"; case IRON_AXE -> "Eisen-Erdbruch";
            case GOLDEN_AXE -> "Goldener Wirbel"; case DIAMOND_AXE -> "Diamant-Henker"; case NETHERITE_AXE -> "Höllenspalter";
            case BOW -> "Kupferwind-Schuss"; case CROSSBOW -> "Eiserne Salve"; default -> null;
        };
    }

    private static void addRandomEquipmentStats(List<Component> lore, PersistentDataContainer pdc,
                                                Set<BalanceModel.EquipmentStat> pool, ItemRarity rarity,
                                                int itemLevel, ItemCategory category) {
        if (pool.isEmpty()) return;
        int min = Math.min(BalanceModel.minStatLines(rarity), pool.size());
        int max = Math.min(BalanceModel.maxStatLines(rarity), pool.size());
        int count = min + (max > min ? ThreadLocalRandom.current().nextInt(max - min + 1) : 0);
        List<BalanceModel.EquipmentStat> candidates = new ArrayList<>(pool);
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        Set<BalanceModel.EquipmentStat> selected = EnumSet.copyOf(candidates.subList(0, count));

        double budget = BalanceModel.itemBudget(itemLevel, rarity, BalanceModel.slotWeight(slotProfile(category)));
        Map<BalanceModel.EquipmentStat, Double> weights = new EnumMap<>(BalanceModel.EquipmentStat.class);
        double totalWeight = 0.0D;
        for (BalanceModel.EquipmentStat stat : selected) {
            double weight = ThreadLocalRandom.current().nextDouble(0.5D, 1.5D);
            weights.put(stat, weight); totalWeight += weight;
        }
        Map<BalanceModel.EquipmentStat, Double> shares = new EnumMap<>(BalanceModel.EquipmentStat.class);
        for (BalanceModel.EquipmentStat stat : selected) shares.put(stat, budget * weights.get(stat) / totalWeight);

        for (int pass = 0; pass < selected.size(); pass++) {
            double excess = 0.0D, freeWeight = 0.0D;
            for (BalanceModel.EquipmentStat stat : selected) {
                double share = shares.get(stat);
                if (share > 1.0D) { excess += share - 1.0D; shares.put(stat, 1.0D); }
                else freeWeight += Math.max(0.001D, weights.get(stat));
            }
            if (excess <= 1.0E-9D || freeWeight <= 0.0D) break;
            for (BalanceModel.EquipmentStat stat : selected) if (shares.get(stat) < 0.999999D) {
                double add = excess * Math.max(0.001D, weights.get(stat)) / freeWeight;
                shares.put(stat, Math.min(1.0D, shares.get(stat) + add));
            }
        }

        for (BalanceModel.EquipmentStat stat : selected) {
            double quality = ThreadLocalRandom.current().nextDouble(0.85D, Math.nextUp(1.0D));
            double value = round(BalanceModel.value(stat, shares.get(stat) * quality));
            writeStat(pdc, stat, value); lore.add(line(lore(stat, value)));
        }
    }

    private static String slotProfile(ItemCategory category) {
        return switch (category) {
            case MELEE_WEAPON, RANGED_WEAPON -> "WEAPON"; case HELMET -> "HELMET"; case CHESTPLATE -> "CHEST";
            case LEGGINGS -> "LEGS"; case BOOTS -> "BOOTS"; case SHIELD -> "SHIELD"; case TOOL -> "TOOL";
        };
    }

    private static void writeStat(PersistentDataContainer pdc, BalanceModel.EquipmentStat stat, double value) {
        switch (stat) {
            case HP -> pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, value);
            case ARMOR -> pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, value);
            case MOVEMENT_SPEED -> pdc.set(RPGKeys.Item.movementSpeed(), PersistentDataType.DOUBLE, value / 100.0D);
            case REACH -> pdc.set(RPGKeys.Item.reachBonus(), PersistentDataType.DOUBLE, value);
            case ATTACK_POWER -> pdc.set(RPGKeys.Item.attackPower(), PersistentDataType.DOUBLE, value);
            case CRIT -> pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, value);
            case CRIT_DAMAGE -> pdc.set(RPGKeys.Item.critDamage(), PersistentDataType.DOUBLE, value / 100.0D);
            case LIFESTEAL -> pdc.set(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, value);
        }
    }

    private static Component lore(BalanceModel.EquipmentStat stat, double value) {
        return switch (stat) {
            case HP -> Component.text("+" + format(value) + " RPG HP", NamedTextColor.GREEN);
            case ARMOR -> Component.text("+" + format(value) + " Rüstung", NamedTextColor.BLUE);
            case MOVEMENT_SPEED -> Component.text("+" + format(value) + "% Bewegungsgeschwindigkeit", NamedTextColor.WHITE);
            case REACH -> Component.text("+" + format(value) + " Reichweite", NamedTextColor.AQUA);
            case ATTACK_POWER -> Component.text("+" + format(value) + " Angriffskraft", NamedTextColor.GOLD);
            case CRIT -> Component.text("+" + format(value) + "% Kritische Trefferchance", NamedTextColor.LIGHT_PURPLE);
            case CRIT_DAMAGE -> Component.text("+" + format(value) + "% Kritischer Schaden", NamedTextColor.LIGHT_PURPLE);
            case LIFESTEAL -> Component.text("+" + format(value) + "% Lebensraub", NamedTextColor.DARK_RED);
        };
    }

    private static void addToolStats(List<Component> lore, PersistentDataContainer pdc, int itemLevel) {
        double efficiency = round(toolBaseEfficiency * Math.max(1.0D, 1.0D + BalanceModel.levelPower(itemLevel) * 9.0D));
        pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, efficiency);
        lore.add(line(Component.text("+" + format(efficiency) + " Effizienz", NamedTextColor.YELLOW)));
    }

    private static double positive(com.google.gson.JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.getAsJsonPrimitive(key).isNumber() && object.get(key).getAsDouble() > 0.0D ? object.get(key).getAsDouble() : fallback;
    }

    private static Component line(Component component) { return component.decoration(TextDecoration.ITALIC, false); }
    private static String generatedItemId(Material material, ItemCategory category, ItemRarity rarity, int level) {
        return "pixelrpg:item/" + category.name().toLowerCase(Locale.ROOT) + "/" + material.name().toLowerCase(Locale.ROOT)
                + "/" + rarity.name().toLowerCase(Locale.ROOT) + "/lvl_" + level;
    }
    private static String generatedDisplayName(Material material, ItemCategory category, ItemRarity rarity, int level) {
        String base = prettyMaterial(material);
        String prefix = switch (rarity) {
            case COMMON -> "Abgenutzte"; case UNCOMMON -> "Veredelte"; case RARE -> "Meisterhafte";
            case EPIC -> "Epische"; case LEGENDARY -> "Legendäre"; case UNIQUE -> "Einzigartige";
        };
        String noun = switch (category.getProfile()) {
            case WEAPON -> switch (material) {
                case IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD, WOODEN_SWORD, STONE_SWORD, COPPER_SWORD -> "Klinge";
                case IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE, WOODEN_AXE, STONE_AXE, COPPER_AXE -> "Streitaxt";
                default -> base;
            };
            case ARMOR -> base; case SHIELD -> "Schild"; case TOOL -> base;
        };
        return prefix + " " + noun + " " + level;
    }
    private static String prettyMaterial(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' '); StringBuilder result = new StringBuilder();
        for (String part : raw.split(" ")) { if (part.isEmpty()) continue; if (!result.isEmpty()) result.append(' '); result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)); }
        return result.toString();
    }
    private static String normalizeItemId(String value) { String normalized = value.trim().toLowerCase(Locale.ROOT); return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized; }
    private static double round(double value) { return Math.round(value * 10.0D) / 10.0D; }
    private static String format(double value) { return Math.abs(value - Math.rint(value)) < 0.0001D ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value); }
}
