package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGItemBuilder {
    private static double blessingChance = 0.12;
    private static double curseChance = 0.10;

    private RPGItemBuilder() { }

    public static void configureChances(double blessing, double curse) {
        blessingChance = blessing;
        curseChance = curse;
    }

    public static Optional<ItemStack> createUnidentified(Material material, ItemRarity rarity, int itemLevel) {
        if (!Level.isValidNormalLevel(itemLevel)) return Optional.empty();
        Optional<ItemCategory> categoryOpt = GearCategoryRegistry.resolve(material);
        if (categoryOpt.isEmpty()) return Optional.empty();
        ItemCategory category = categoryOpt.get();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Unidentified ", NamedTextColor.GRAY)
                .append(rarity.displayName())
                .append(Component.text(" Item", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Bring this to the Blacksmith", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("to reveal its true power.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Item Level: " + itemLevel, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, false);
        pdc.set(RPGKeys.Item.rarity(), PersistentDataType.STRING, rarity.name());
        pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, itemLevel);
        pdc.set(RPGKeys.Item.category(), PersistentDataType.STRING, category.name());
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    public static ItemStack identify(ItemStack unidentifiedItem) {
        ItemStack item = unidentifiedItem.clone();
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        if (Boolean.TRUE.equals(pdc.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN))) return item;

        String rarityRaw = pdc.get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        String categoryRaw = pdc.get(RPGKeys.Item.category(), PersistentDataType.STRING);
        Integer itemLevelRaw = pdc.get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
        ItemRarity rarity = rarityRaw != null ? ItemRarity.valueOf(rarityRaw) : ItemRarity.COMMON;
        ItemCategory category = categoryRaw != null ? ItemCategory.valueOf(categoryRaw) : ItemCategory.TOOL;
        int itemLevel = itemLevelRaw != null ? Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, itemLevelRaw)) : Level.MIN_LEVEL;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = 0.85 + random.nextDouble(0.3);
        double multiplier = rarity.getStatMultiplier();
        double levelFactor = 1.0 + itemLevel * 0.075;
        String instanceId = UUID.randomUUID().toString();
        pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, instanceId);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Rarity: ", NamedTextColor.GRAY).append(rarity.displayName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Item Level: " + itemLevel, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Category: ", NamedTextColor.GRAY).append(category.displayName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));

        switch (category.getProfile()) {
            case WEAPON -> {
                double bonusDamage = round((2.0 + levelFactor * 1.8) * multiplier * roll);
                double critChance = round((1.0 + levelFactor * 0.22) * multiplier * 0.7 * roll);
                pdc.set(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, bonusDamage);
                pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, critChance);
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(RPGKeys.Item.attackDamageModifier(instanceId), bonusDamage, AttributeModifier.Operation.ADD_NUMBER, category.getSlotGroup()));
                lore.add(Component.text("Bonus Damage: +" + bonusDamage, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Crit Chance: +" + critChance + "%", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
            case ARMOR, SHIELD -> {
                double armorValue = round((1.5 + levelFactor * 1.2) * multiplier * roll);
                double healthBonus = category.getProfile() == ItemStatProfile.SHIELD ? 0.0 : round((1.0 + levelFactor * 0.9) * multiplier * roll);
                pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, armorValue);
                pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, healthBonus);
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(RPGKeys.Item.armorModifier(instanceId), armorValue, AttributeModifier.Operation.ADD_NUMBER, category.getSlotGroup()));
                lore.add(Component.text("Armor: +" + armorValue, NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
                if (healthBonus > 0.0) {
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(RPGKeys.Item.healthModifier(instanceId), healthBonus, AttributeModifier.Operation.ADD_NUMBER, category.getSlotGroup()));
                    lore.add(Component.text("Max Health: +" + healthBonus, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                }
            }
            case TOOL -> {
                double toolBonus = round((1.0 + levelFactor * 0.55) * multiplier * roll);
                pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, toolBonus);
                lore.add(Component.text("Efficiency: +" + toolBonus, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
        }

        if (random.nextDouble() < blessingChance) {
            BlessingType blessing = BlessingType.rollRandom();
            pdc.set(RPGKeys.Item.blessingType(), PersistentDataType.STRING, blessing.name());
            lore.add(Component.text("✦ ", NamedTextColor.AQUA).append(blessing.displayName()).decoration(TextDecoration.ITALIC, false));
        }
        if (random.nextDouble() < curseChance) {
            CurseType curse = CurseType.rollRandom();
            pdc.set(RPGKeys.Item.curseType(), PersistentDataType.STRING, curse.name());
            lore.add(Component.text("☠ ", NamedTextColor.DARK_RED).append(curse.displayName()).decoration(TextDecoration.ITALIC, false));
        }

        String materialLabel = item.getType().name().replace('_', ' ').toLowerCase();
        String prettyMaterial = Character.toUpperCase(materialLabel.charAt(0)) + materialLabel.substring(1);
        meta.displayName(rarity.displayName().append(Component.text(" " + prettyMaterial, NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
}
