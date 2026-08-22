package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class ClassSetBonusService {

    public record SetBonus(int piecesEquipped, double damageMultiplier, double healMultiplier, double procChancePercent) {
        public static final SetBonus NONE = new SetBonus(0, 1.0, 1.0, 0.0);
    }

    private ClassSetBonusService() {
    }

    public static SetBonus computeBonus(Player player, PlayerClass playerClass) {
        if (playerClass == PlayerClass.NONE) return SetBonus.NONE;

        PlayerInventory inv = player.getInventory();
        int matching = 0;
        matching += matchesClass(inv.getItemInMainHand(), playerClass) ? 1 : 0;
        matching += matchesClass(inv.getHelmet(), playerClass) ? 1 : 0;
        matching += matchesClass(inv.getChestplate(), playerClass) ? 1 : 0;
        matching += matchesClass(inv.getLeggings(), playerClass) ? 1 : 0;
        matching += matchesClass(inv.getBoots(), playerClass) ? 1 : 0;

        double damageMultiplier = 1.0;
        double healMultiplier = 1.0;
        double procChance = 0.0;

        if (matching >= 2) {
            damageMultiplier = 1.04;
            healMultiplier = 1.04;
        }
        if (matching >= 4) {
            damageMultiplier = 1.10;
            healMultiplier = 1.10;
            procChance = 10.0;
        }

        return new SetBonus(matching, damageMultiplier, healMultiplier, procChance);
    }

    public static boolean rollProc(SetBonus bonus) {
        if (bonus.procChancePercent() <= 0.0) return false;
        return ThreadLocalRandom.current().nextDouble(100.0) < bonus.procChancePercent();
    }

    private static boolean matchesClass(ItemStack item, PlayerClass playerClass) {
        if (item == null || !item.hasItemMeta()) return false;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.classSetClass(), PersistentDataType.STRING);
        return playerClass.name().equals(raw);
    }
}
