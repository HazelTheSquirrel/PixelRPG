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
import java.util.concurrent.ThreadLocalRandom;

public final class RPGItemBuilder {
    private static double growthMultiplier = 10.0D;
    private static double weaponBaseDamage = 3.0D;
    private static double weaponBaseCritChance = 0.5D;
    private static double armorBase = 0.7D;
    private static double healthBase = 1.2D;
    private static double toolBaseEfficiency = 1.0D;
    private static double blessingChance = 0.12;
    private static double curseChance = 0.10;

    private RPGItemBuilder() {
    }

    public static void configureChances(double blessing, double curse) {
        blessingChance = Math.max(0.0, Math.min(1.0, blessing));
        curseChance = Math.max(0.0, Math.min(1.0, curse));
    }

    /** Loads the deterministic item growth curve from the user-editable JSON baseline. */
    public static void configureScaling(Plugin plugin) {
        try {
            var root = new JsonDataManager(plugin).load("item-scaling.json");
            growthMultiplier = positive(root, "growthMultiplier", growthMultiplier);
            var baseStats = root.getAsJsonObject("baseStats");
            weaponBaseDamage = positive(baseStats, "weaponDamage", weaponBaseDamage);
            weaponBaseCritChance = positive(baseStats, "weaponCritChance", weaponBaseCritChance);
            armorBase = positive(baseStats, "armor", armorBase);
            healthBase = positive(baseStats, "health", healthBase);
            toolBaseEfficiency = positive(baseStats, "toolEfficiency", toolBaseEfficiency);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to load item-scaling.json; using safe deterministic item defaults: " + exception.getMessage());
        }
    }

    /** Creates a fully identified PixelRPG item with deterministic core stats for its level and rarity. */
    public static java.util.Optional<ItemStack> createItem(Material material, ItemRarity rarity, int itemLevel) {
        if (material == null || rarity == null || !Level.isValidNormalLevel(itemLevel)) return java.util.Optional.empty();

        var categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return java.util.Optional.empty();

        ItemCategory category = categoryOpt.get();
        ItemStack item = ItemStack.of(material);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, createItemId(material, category));
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(RPGKeys.Item.rarity(), PersistentDataType.STRING, rarity.name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.category(), PersistentDataType.STRING, category.name());
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);

        double multiplier = rarity.getStatMultiplier();
        double levelFactor = levelScaling(itemLevel);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Component> lore = new ArrayList<>();
        lore.add(line(rarity.displayName()));
        lore.add(line(Component.text("Item Level " + itemLevel, NamedTextColor.YELLOW)));
        lore.add(line(Component.text("Requires Level " + itemLevel, NamedTextColor.RED)));
        lore.add(Component.text(" "));

        switch (category.getProfile()) {
            case WEAPON -> addWeaponStats(lore, pdc, multiplier, levelFactor);
            case ARMOR, SHIELD -> addArmorStats(lore, pdc, category, multiplier, levelFactor);
            case TOOL -> addToolStats(lore, pdc, multiplier, levelFactor);
        }

        if (random.nextDouble() < blessingChance) {
            BlessingType blessing = BlessingType.rollRandom();
            pdc.set(RPGKeys.Item.blessingType(), PersistentDataType.STRING, blessing.name());
            lore.add(Component.text("Blessing: ", NamedTextColor.AQUA)
                    .append(blessing.displayName())
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (random.nextDouble() < curseChance) {
            CurseType curse = CurseType.rollRandom();
            pdc.set(RPGKeys.Item.curseType(), PersistentDataType.STRING, curse.name());
            lore.add(Component.text("Curse: ", NamedTextColor.DARK_RED)
                    .append(curse.displayName())
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.displayName(rarity.displayName()
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(Component.text(prettyMaterial(material), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return java.util.Optional.of(item);
    }

    /** Assigns a named ability directly to a weapon item. */
    public static ItemStack withWeaponAbility(ItemStack item, String abilityId, long cooldownMillis) {
        if (abilityId == null || abilityId.isBlank()) throw new IllegalArgumentException("abilityId must not be blank");

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING, abilityId);
        pdc.set(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, Math.max(0L, cooldownMillis));

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty() && !lore.getLast().equals(Component.text(" "))) lore.add(Component.text(" "));
        lore.add(Component.text("Ability", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(abilityId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right Click", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        result.setItemMeta(meta);
        return result;
    }

    private static void addWeaponStats(List<Component> lore, PersistentDataContainer pdc,
                                       double multiplier, double levelFactor) {
        double damage = round(weaponBaseDamage * levelFactor * multiplier);
        double critChance = round(weaponBaseCritChance * levelFactor * multiplier);
        pdc.set(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, damage);
        pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, critChance);
        lore.add(line(Component.text("+" + format(damage) + " Attack Power", NamedTextColor.RED)));
        lore.add(line(Component.text("+" + format(critChance) + "% Critical Strike", NamedTextColor.LIGHT_PURPLE)));
    }

    private static void addArmorStats(List<Component> lore, PersistentDataContainer pdc, ItemCategory category,
                                      double multiplier, double levelFactor) {
        double armor = round(armorBase * levelFactor * multiplier);
        double health = category.getProfile() == ItemStatProfile.SHIELD
                ? 0.0D
                : round(healthBase * levelFactor * multiplier);
        pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, armor);
        pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, health);
        lore.add(line(Component.text("+" + format(armor) + " Armor", NamedTextColor.BLUE)));
        if (health > 0.0) lore.add(line(Component.text("+" + format(health) + " Stamina", NamedTextColor.GREEN)));
    }

    private static void addToolStats(List<Component> lore, PersistentDataContainer pdc,
                                     double multiplier, double levelFactor) {
        double efficiency = round(toolBaseEfficiency * levelFactor * multiplier);
        pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, efficiency);
        lore.add(line(Component.text("+" + format(efficiency) + " Efficiency", NamedTextColor.YELLOW)));
    }

    private static double levelScaling(int itemLevel) {
        if (itemLevel <= Level.MIN_LEVEL) return 1.0D;
        double progress = (itemLevel - 1.0D) / (Level.MAX_NORMAL_LEVEL - 1.0D);
        return Math.pow(growthMultiplier, progress);
    }

    private static double positive(com.google.gson.JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.getAsJsonPrimitive(key).isNumber() && object.get(key).getAsDouble() > 0.0D
                ? object.get(key).getAsDouble() : fallback;
    }

    private static Component line(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static String prettyMaterial(Material material) {
        String raw = material.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(raw.length());
        boolean capitalize = true;
        for (char character : raw.toCharArray()) {
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
            if (character == ' ') capitalize = true;
        }
        return result.toString();
    }

    private static String createItemId(Material material, ItemCategory category) {
        return "pixelrpg:" + category.name().toLowerCase(Locale.ROOT) + "/" + material.name().toLowerCase(Locale.ROOT);
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
