// src/main/java/de/pixelrpg/rpg/combat/gem/GemSocketService.java (VOLLSTÄNDIG, ersetzt alte Datei — Sockelanzahl kommt jetzt aus dem beim Identifizieren gesetzten PDC-Wert, nicht mehr aus itemLevel)
package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemStatProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class GemSocketService {

    public enum Result {
        SUCCESS,
        NO_FREE_SLOTS,
        ALREADY_SOCKETED,
        NOT_A_WEAPON
    }

    private GemSocketService() {
    }

    public static List<String> readSockets(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (!item.hasItemMeta()) {
            return result;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer used = pdc.getOrDefault(RPGKeys.Item.gemUsedSockets(), PersistentDataType.INTEGER, 0);
        for (int i = 0; i < used; i++) {
            String raw = pdc.get(RPGKeys.Item.gemSocket(i), PersistentDataType.STRING);
            if (raw != null) {
                result.add(raw);
            }
        }
        return result;
    }

    public static Result socket(ItemStack item, String gemId) {
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        String categoryRaw = pdc.get(RPGKeys.Item.category(), PersistentDataType.STRING);
        ItemCategory category = categoryRaw != null ? ItemCategory.valueOf(categoryRaw) : null;
        if (category == null || category.getProfile() != ItemStatProfile.WEAPON) {
            return Result.NOT_A_WEAPON;
        }

        Integer maxSockets = pdc.getOrDefault(RPGKeys.Item.gemMaxSockets(), PersistentDataType.INTEGER, 0);
        Integer used = pdc.getOrDefault(RPGKeys.Item.gemUsedSockets(), PersistentDataType.INTEGER, 0);
        if (used >= maxSockets) {
            return Result.NO_FREE_SLOTS;
        }

        for (int i = 0; i < used; i++) {
            if (gemId.equals(pdc.get(RPGKeys.Item.gemSocket(i), PersistentDataType.STRING))) {
                return Result.ALREADY_SOCKETED;
            }
        }

        pdc.set(RPGKeys.Item.gemSocket(used), PersistentDataType.STRING, gemId);
        pdc.set(RPGKeys.Item.gemUsedSockets(), PersistentDataType.INTEGER, used + 1);

        rewriteGemLore(meta, gemId);
        item.setItemMeta(meta);
        return Result.SUCCESS;
    }

    private static void rewriteGemLore(ItemMeta meta, String gemId) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            return;
        }
        List<Component> newLore = new ArrayList<>();
        boolean replaced = false;
        for (Component line : lore) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (!replaced && plain.contains("Empty Gem Socket")) {
                newLore.add(Component.text("◈ Gem: " + gemId, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                replaced = true;
            } else {
                newLore.add(line);
            }
        }
        meta.lore(newLore);
    }
}