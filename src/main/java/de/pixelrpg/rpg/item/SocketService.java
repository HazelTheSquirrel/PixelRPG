// src/main/java/de/pixelrpg/rpg/item/SocketService.java (VOLLSTÄNDIG, ersetzt alte Datei — instanzgebundene Modifikator-Keys, Self-Healing für Alt-Items ohne instanceId)
package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SocketService {

    public enum Result {
        SUCCESS,
        NOT_IDENTIFIED,
        NO_FREE_SLOTS,
        INCOMPATIBLE_RUNE
    }

    private SocketService() {
    }

    public static Result applyRune(ItemStack gearItem, RuneType runeType, double runeValue) {
        ItemMeta meta = gearItem.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        Boolean identified = pdc.get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN);
        if (!Boolean.TRUE.equals(identified)) {
            return Result.NOT_IDENTIFIED;
        }

        String categoryRaw = pdc.get(RPGKeys.Item.category(), PersistentDataType.STRING);
        ItemCategory category = categoryRaw != null ? ItemCategory.valueOf(categoryRaw) : ItemCategory.TOOL;
        if (!runeType.isCompatible(category)) {
            return Result.INCOMPATIBLE_RUNE;
        }

        Integer maxSockets = pdc.getOrDefault(RPGKeys.Item.maxSockets(), PersistentDataType.INTEGER, 0);
        Integer usedSockets = pdc.getOrDefault(RPGKeys.Item.usedSockets(), PersistentDataType.INTEGER, 0);
        if (usedSockets >= maxSockets) {
            return Result.NO_FREE_SLOTS;
        }

        // Self-Healing: ältere Items (vor diesem Fix identifiziert) besitzen noch keine
        // instanceId — hier wird sie nachträglich vergeben, damit Modifikatoren korrekt bleiben.
        String instanceId = pdc.get(RPGKeys.Item.instanceId(), PersistentDataType.STRING);
        if (instanceId == null) {
            instanceId = UUID.randomUUID().toString();
            pdc.set(RPGKeys.Item.instanceId(), PersistentDataType.STRING, instanceId);
        }

        pdc.set(RPGKeys.Item.socket(usedSockets), PersistentDataType.STRING, runeType.name() + ":" + runeValue);
        pdc.set(RPGKeys.Item.usedSockets(), PersistentDataType.INTEGER, usedSockets + 1);

        switch (runeType) {
            case DAMAGE -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, updated);
                bumpAttribute(meta, Attribute.ATTACK_DAMAGE, RPGKeys.Item.attackDamageModifier(instanceId), runeValue, category);
            }
            case CRIT -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, updated);
            }
            case LIFESTEAL -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, updated);
            }
            case ARMOR -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, updated);
                bumpAttribute(meta, Attribute.ARMOR, RPGKeys.Item.armorModifier(instanceId), runeValue, category);
            }
            case VITALITY -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, updated);
                bumpAttribute(meta, Attribute.MAX_HEALTH, RPGKeys.Item.healthModifier(instanceId), runeValue, category);
            }
            case REGENERATION -> {
                // Passive Aura, wird dynamisch von EquipmentAuraListener aus dem Socket-String gelesen.
            }
            case HASTE -> {
                double updated = pdc.getOrDefault(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, 0.0) + runeValue;
                pdc.set(RPGKeys.Item.toolBonus(), PersistentDataType.DOUBLE, updated);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + runeType);
        }

        rewriteSocketLore(meta, runeType, runeValue);
        gearItem.setItemMeta(meta);
        return Result.SUCCESS;
    }

    private static void bumpAttribute(ItemMeta meta, Attribute attribute, org.bukkit.NamespacedKey key,
                                       double addValue, ItemCategory category) {
        AttributeModifier existing = meta.getAttributeModifiers() != null
                ? meta.getAttributeModifiers().values().stream()
                        .filter(mod -> mod.getKey().equals(key))
                        .findFirst().orElse(null)
                : null;

        double base = existing != null ? existing.getAmount() : 0.0;
        if (existing != null) {
            meta.removeAttributeModifier(attribute, existing);
        }
        meta.addAttributeModifier(attribute, new AttributeModifier(
                key, base + addValue, AttributeModifier.Operation.ADD_NUMBER, category.getSlotGroup()));
    }

    private static void rewriteSocketLore(ItemMeta meta, RuneType runeType, double value) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            return;
        }
        List<Component> newLore = new ArrayList<>();
        boolean replaced = false;
        for (Component line : lore) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (!replaced && plain.contains("Empty Socket")) {
                newLore.add(Component.text("✦ " + runeType.name() + " +" + value, NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                replaced = true;
            } else {
                newLore.add(line);
            }
        }
        meta.lore(newLore);
    }

    public static List<String> readRawSockets(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (!item.hasItemMeta()) {
            return result;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer used = pdc.getOrDefault(RPGKeys.Item.usedSockets(), PersistentDataType.INTEGER, 0);
        for (int i = 0; i < used; i++) {
            String raw = pdc.get(RPGKeys.Item.socket(i), PersistentDataType.STRING);
            if (raw != null) {
                result.add(raw);
            }
        }
        return result;
    }
}