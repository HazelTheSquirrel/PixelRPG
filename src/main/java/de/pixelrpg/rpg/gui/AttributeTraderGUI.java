package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;

    public AttributeTraderGUI(Player viewer, PlayerProfileManager profileManager, StatEngine statEngine) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("attribute.gui-title"));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.statEngine = statEngine;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
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
            lore.add(lang.get("attribute.class-affinity").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(" "));

        if (maxed) {
            lore.add(lang.get("attribute.maximum-reached").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (!rankMet) {
            lore.add(lang.get("attribute.requires-rank", "rank", required.name())
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(lang.get("attribute.cost-label", "cost", String.valueOf(cost))
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(lang.get("attribute.click-to-increase").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
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
                case MAX_REACHED -> lang.send(viewer, "attribute.max-reached");
                case RANK_TOO_LOW -> lang.send(viewer, "attribute.rank-too-low");
                case INSUFFICIENT_FUNDS -> lang.send(viewer, "attribute.insufficient-funds");
                case NOT_REGISTERED -> lang.send(viewer, "attribute.not-registered");
            }
        });
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}