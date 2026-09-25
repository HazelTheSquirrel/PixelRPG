package de.pixelrpg.rpg.item;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RPGItemBuilder {
    private static final long WEAPON_ABILITY_COOLDOWN_MILLIS = 6_000L;
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

    private RPGItemBuilder() {
    }

    public static void configureScaling(Plugin plugin) {
        try {
            var root = new JsonDataManager(plugin).load("item-scaling.json");
            growthMultiplier = positive(root, "growthMultiplier", growthMultiplier);
            var baseStats = root.getAsJsonObject("baseStats");
            weaponBaseDamage = positive(baseStats, "weaponDamage", weaponBaseDamage);
            weaponBaseCritChance = positive(baseStats, "weaponCritChance", weaponBaseCritChance);
            weaponBaseCritDamage = positive(baseStats, "weaponCritDamage", weaponBaseCritDamage);
            weaponBaseReach = positive(baseStats, "weaponReach", weaponBaseReach);
            weaponBaseLifesteal = positive(baseStats, "weaponLifesteal", weaponBaseLifesteal);
            armorBase = positive(baseStats, "armor", armorBase);
            healthBase = positive(baseStats, "health", healthBase);
            movementSpeedBase = positive(baseStats, "movementSpeed", movementSpeedBase);
            toolBaseEfficiency = positive(baseStats, "toolEfficiency", toolBaseEfficiency);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load item-scaling.json; using safe deterministic item defaults: " + exception.getMessage());
        }
    }

    public static java.util.Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        if (material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();
        var categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return java.util.Optional.empty();
        ItemCategory category = categoryOpt.get();
        String itemId = generatedItemId(material, category, rarity, itemLevel);
        String displayName = generatedDisplayName(material, category, rarity, itemLevel);
        return createItem(itemId, displayName, material, rarity, itemLevel, true);
    }

    public static java.util.Optional<ItemStack> createItem(String itemId, String displayName,
                                                            Material material, ItemRarity rarity, int itemLevel) {
        if (itemId == null || itemId.isBlank() || displayName == null || displayName.isBlank()) return java.util.Optional.empty();
        if (material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();
        var categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return java.util.Optional.empty();
        return createItem(itemId, displayName, material, rarity, itemLevel, true);
    }

    private static java.util.Optional<ItemStack> createItem(String itemId, String displayName,
                                                             Material material, ItemRarity rarity,
                                                             int itemLevel, boolean guildItem) {
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

        double multiplier = rarity.getStatMultiplier();
        double levelFactor = levelScaling(itemLevel);

        List<Component> lore = new ArrayList<>();
        lore.add(line(rarity.displayName()));
        lore.add(line(Component.text("Gegenstandslevel " + itemLevel, NamedTextColor.YELLOW)));
        lore.add(line(Component.text("Benötigt Level " + itemLevel, NamedTextColor.RED)));
        lore.add(Component.text(" "));

        switch (category.getProfile()) {
            case WEAPON, ARMOR, SHIELD -> addDeterministicEquipmentStats(lore, pdc, category, rarity, multiplier, levelFactor);
            case TOOL -> addToolStats(lore, pdc, multiplier, levelFactor);
            case FOOD -> {
            }
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
        meta.lore(lore);
        result.setItemMeta(meta);
        return result;
    }

    private static java.util.Optional<ItemStack> applyMaterialWeaponAbility(ItemStack item) {
        String abilityId = weaponAbilityId(item.getType());
        if (abilityId == null) return java.util.Optional.of(item);
        return java.util.Optional.of(withWeaponAbility(item, abilityId, WEAPON_ABILITY_COOLDOWN_MILLIS));
    }

    private static String weaponAbilityId(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> "Holzklingenfeger";
            case STONE_SWORD -> "Steinbrecher";
            case COPPER_SWORD -> "Kupfersturm";
            case IRON_SWORD -> "Eiserne Bastion";
            case GOLDEN_SWORD -> "Goldene Klingenflut";
            case DIAMOND_SWORD -> "Diamantspalter";
            case NETHERITE_SWORD -> "Netherit-Eruption";
            case COPPER_AXE -> "Kupferstatische Axt";
            case IRON_AXE -> "Eisen-Erdbruch";
            case GOLDEN_AXE -> "Goldener Wirbel";
            case DIAMOND_AXE -> "Diamant-Henker";
            case NETHERITE_AXE -> "Höllenspalter";
            case BOW -> "Kupferwind-Schuss";
            case CROSSBOW -> "Eiserne Salve";
            default -> null;
        };
    }

    private static void addDeterministicEquipmentStats(List<Component> lore, PersistentDataContainer pdc,
                                                        ItemCategory category, ItemRarity rarity,
                                                        double multiplier, double levelFactor) {
        List<EquipmentStat> stats = switch (category.getProfile()) {
            case WEAPON -> List.of(
                    EquipmentStat.ATTACK_POWER,
                    EquipmentStat.CRIT,
                    EquipmentStat.CRIT_DAMAGE,
                    EquipmentStat.REACH,
                    EquipmentStat.LIFESTEAL,
                    EquipmentStat.HP
            );
            case ARMOR -> List.of(
                    EquipmentStat.ARMOR,
                    EquipmentStat.HP,
                    EquipmentStat.MOVEMENT_SPEED,
                    EquipmentStat.CRIT,
                    EquipmentStat.CRIT_DAMAGE,
                    EquipmentStat.LIFESTEAL
            );
            case SHIELD -> List.of(
                    EquipmentStat.ARMOR,
                    EquipmentStat.HP,
                    EquipmentStat.REACH,
                    EquipmentStat.CRIT,
                    EquipmentStat.CRIT_DAMAGE,
                    EquipmentStat.LIFESTEAL
            );
            default -> List.of();
        };

        int count = Math.min(statCount(rarity), stats.size());
        for (EquipmentStat stat : stats.subList(0, count)) {
            double value = deterministicValue(stat.baseValue() * levelFactor * multiplier);
            stat.write(pdc, value);
            lore.add(line(stat.lore(value)));
        }
    }

    private enum EquipmentStat {
        HP {
            @Override double baseValue() { return healthBase; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + " LP", NamedTextColor.GREEN); }
        },
        ARMOR {
            @Override double baseValue() { return armorBase; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + " Rüstung", NamedTextColor.BLUE); }
        },
        MOVEMENT_SPEED {
            @Override double baseValue() { return movementSpeedBase; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.movementSpeed(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value * 100.0D) + "% Bewegungsgeschwindigkeit", NamedTextColor.WHITE); }
        },
        REACH {
            @Override double baseValue() { return weaponBaseReach; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.reachBonus(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + " Reichweite", NamedTextColor.AQUA); }
        },
        ATTACK_POWER {
            @Override double baseValue() { return weaponBaseDamage; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.attackPower(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + " Angriffskraft", NamedTextColor.GOLD); }
        },
        CRIT {
            @Override double baseValue() { return weaponBaseCritChance; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + "% Kritische Trefferchance", NamedTextColor.LIGHT_PURPLE); }
        },
        CRIT_DAMAGE {
            @Override double baseValue() { return weaponBaseCritDamage; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.critDamage(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value * 100.0D) + "% Kritischer Schaden", NamedTextColor.LIGHT_PURPLE); }
        },
        LIFESTEAL {
            @Override double baseValue() { return weaponBaseLifesteal; }
            @Override void write(PersistentDataContainer pdc, double value) { pdc.set(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, value); }
            @Override Component lore(double value) { return Component.text("+" + format(value) + "% Lebensraub", NamedTextColor.DARK_RED); }
        };

        abstract double baseValue();
        abstract void write(PersistentDataContainer pdc, double value);
        abstract Component lore(double value);
    }

    private static void addToolStats(List<Component> lore, PersistentDataContainer pdc,
                                     double multiplier, double levelFactor) {
        double efficiency = deterministicValue(toolBaseEfficiency * levelFactor * multiplier);
        pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, efficiency);
        lore.add(line(Component.text("+" + format(efficiency) + " Effizienz", NamedTextColor.YELLOW)));
    }

    private static double levelScaling(int itemLevel) {
        if (itemLevel <= Level.MIN_LEVEL) return 1.0D;
        double progress = (itemLevel - 1.0D) / (Level.MAX_NORMAL_LEVEL - 1.0D);
        return 1.0D + Math.max(0.0D, growthMultiplier - 1.0D) * Math.clamp(progress, 0.0D, 1.0D);
    }

    private static double positive(com.google.gson.JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.getAsJsonPrimitive(key).isNumber() && object.get(key).getAsDouble() > 0.0D
                ? object.get(key).getAsDouble() : fallback;
    }

    private static Component line(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static String generatedItemId(Material material, ItemCategory category, ItemRarity rarity, int itemLevel) {
        return "pixelrpg:item/" + category.name().toLowerCase(Locale.ROOT) + "/" + material.name().toLowerCase(Locale.ROOT)
                + "/" + rarity.name().toLowerCase(Locale.ROOT) + "/lvl_" + itemLevel;
    }

    private static String generatedDisplayName(Material material, ItemCategory category, ItemRarity rarity, int itemLevel) {
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
            case TOOL -> base;
            case FOOD -> base;
        };
        return prefix + " " + noun + " " + itemLevel;
    }

    private static String prettyMaterial(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static String normalizeItemId(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
