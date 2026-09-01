package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Applies the existing four-piece set identities as temporary effects without adding new stat types. */
public final class EquipmentSetEffectService {
    private static final int EFFECT_DURATION_TICKS = 40;
    private static final int REFRESH_THRESHOLD_TICKS = 10;

    private final Map<String, SetEffect> effects = Map.of(
            "donnerwacht", new SetEffect(PotionEffectType.SPEED, 0),
            "schattengeflecht", new SetEffect(PotionEffectType.NIGHT_VISION, 0),
            "stahlwall", new SetEffect(PotionEffectType.RESISTANCE, 0),
            "sonnengewand", new SetEffect(PotionEffectType.FIRE_RESISTANCE, 0),
            "kristallwache", new SetEffect(PotionEffectType.ABSORPTION, 0),
            "hoellenschmiede", new SetEffect(PotionEffectType.STRENGTH, 0)
    );

    public void apply(Player player, int playerLevel) {
        Map<String, Integer> counts = counts(player, playerLevel);
        Set<PotionEffectType> activeTypes = new HashSet<>();

        for (Map.Entry<String, SetEffect> entry : effects.entrySet()) {
            if (counts.getOrDefault(entry.getKey(), 0) < 4) continue;
            SetEffect effect = entry.getValue();
            activeTypes.add(effect.type());
            PotionEffect current = player.getPotionEffect(effect.type());
            if (current == null || current.getDuration() <= REFRESH_THRESHOLD_TICKS || current.getAmplifier() != effect.amplifier()) {
                player.addPotionEffect(new PotionEffect(effect.type(), EFFECT_DURATION_TICKS, effect.amplifier(), false, false, true));
            }
        }

        for (SetEffect effect : effects.values()) {
            if (!activeTypes.contains(effect.type())) player.removePotionEffect(effect.type());
        }
    }

    private Map<String, Integer> counts(Player player, int playerLevel) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : equipped(player)) {
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            Integer required = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER);
            if (required != null && playerLevel < required) continue;
            String setId = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.setId(), PersistentDataType.STRING);
            if (setId != null && !setId.isBlank()) counts.merge(setId.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        return counts;
    }

    private ItemStack[] equipped(Player player) {
        var inventory = player.getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        ItemStack[] result = new ItemStack[armor.length + 2];
        System.arraycopy(armor, 0, result, 0, armor.length);
        result[armor.length] = inventory.getItemInMainHand();
        result[armor.length + 1] = inventory.getItemInOffHand();
        return result;
    }

    private record SetEffect(PotionEffectType type, int amplifier) { }
}
