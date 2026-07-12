// src/main/java/de/pixelrpg/rpg/gui/AttributeTraderGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — Elytra-Permit-Slot ergänzt)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.player.AttributeConfig;
import de.pixelrpg.rpg.player.PlayerAttribute;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AttributeTraderGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;

    public AttributeTraderGUI(Player viewer, PlayerProfileManager profileManager, StatEngine statEngine) {
        super(54, Component.text("Attribute Distribution", NamedTextColor.DARK_AQUA));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        setAttributeItem(19, Material.APPLE, PlayerAttribute.VITALITY, profile,
                "+" + AttributeConfig.VITALITY_HP_PER_POINT + " Max Health per point");
        setAttributeItem(20, Material.SUGAR, PlayerAttribute.AGILITY, profile,
                "+" + AttributeConfig.AGILITY_SPEED_PER_POINT + " Speed, +" + AttributeConfig.AGILITY_CRIT_PER_POINT + "% Crit per point");
        setAttributeItem(21, Material.GOLDEN_SWORD, PlayerAttribute.PRECISION, profile,
                "+" + AttributeConfig.PRECISION_DAMAGE_PER_POINT + " Damage per point");
        setAttributeItem(23, Material.BOW, PlayerAttribute.RANGE, profile,
                "+" + AttributeConfig.RANGE_BLOCK_PER_POINT + " Block / +" + AttributeConfig.RANGE_ENTITY_PER_POINT + " Entity reach per point");
        setAttributeItem(24, Material.SHIELD, PlayerAttribute.TOUGHNESS, profile,
                "+" + AttributeConfig.TOUGHNESS_ARMOR_PER_POINT + " Armor per point");
        setAttributeItem(29, Material.ENDER_EYE, PlayerAttribute.SOULVIEW, profile,
                "Grants permanent Night Vision.");
        setAttributeItem(31, Material.ELYTRA, PlayerAttribute.ELYTRA_PERMIT, profile,
                "Grants permission to use an Elytra.");

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private void setAttributeItem(int slot, Material material, PlayerAttribute attribute, PlayerProfile profile, String description) {
        int current = profile.getAttributePoints(attribute);
        int max = attribute.getMaxPoints();
        boolean maxed = current >= max;
        Rank required = AttributeConfig.rankRequirementForPoint(attribute, current);
        boolean rankMet = profile.getRank().isAtLeast(required);
        double cost = AttributeConfig.costFor(attribute, current, profile.getPlayerClass());
        boolean discounted = AttributeConfig.isPrimaryFor(profile.getPlayerClass(), attribute);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(attribute.displayName()
                .append(Component.text(" [" + current + "/" + max + "]", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (discounted) {
            lore.add(Component.text("Class Affinity: -25% Cost", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(" "));

        if (maxed) {
            lore.add(Component.text("Maximum reached.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (!rankMet) {
            lore.add(Component.text("Requires Rank " + required.name() + ".", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Cost: " + cost + " Gold", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to increase by 1.", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);

        setItem(slot, item, event -> {
            PlayerProfileManager.AttributePurchaseResult result = profileManager.purchaseAttribute(viewer, attribute);
            switch (result) {
                case SUCCESS -> {
                    statEngine.recalculate(viewer);
                    viewer.playSound(viewer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
                    open(viewer);
                }
                case MAX_REACHED -> viewer.sendMessage(Component.text("This attribute is already maxed out.", NamedTextColor.RED));
                case RANK_TOO_LOW -> viewer.sendMessage(Component.text("Your rank is too low for this upgrade.", NamedTextColor.RED));
                case INSUFFICIENT_FUNDS -> viewer.sendMessage(Component.text("You do not have enough gold.", NamedTextColor.RED));
                case NOT_REGISTERED -> viewer.sendMessage(Component.text("You must be a guild member.", NamedTextColor.RED));
            }
        });
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}