package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class GemItemFactory {

    private GemItemFactory() {
    }

    public static ItemStack createActive(ActiveSkillGemDefinition def) {
        ItemStack item = new ItemStack(def.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(def.displayName() + " Gem", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(def.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));
        lore.add(Component.text("Active Skill Gem", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Requires Class: " + def.ownerClass().name(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Drag onto a weapon with a free gem socket.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.gemType(), PersistentDataType.STRING, def.id());
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPassive(PassiveGemDefinition def) {
        ItemStack item = new ItemStack(def.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(def.displayName(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(def.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));
        lore.add(Component.text("Passive Gem", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Drag onto a weapon with a free gem socket.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.gemType(), PersistentDataType.STRING, def.id());
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSupport(String supportId, String displayName, String description, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Support Gem", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.gemType(), PersistentDataType.STRING, supportId);
        pdc.set(RPGKeys.Item.guildItem(), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public static String readGemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.gemType(), PersistentDataType.STRING);
    }
}