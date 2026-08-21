package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.BlessingType;
import de.pixelrpg.rpg.item.CurseType;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentAuraListener {

    private final PlayerProfileManager profileManager;
    private final int intervalTicks;

    public EquipmentAuraListener(PlayerProfileManager profileManager, int intervalTicks) {
        this.profileManager = profileManager;
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(PixelRPGPlugin.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!profileManager.isRegistered(player.getUniqueId())) continue;
                scanAndApply(player);
            }
        }, intervalTicks, intervalTicks);
    }

    private void scanAndApply(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> equipped = new ArrayList<>();
        equipped.add(inv.getItemInMainHand());
        if (inv.getHelmet() != null) equipped.add(inv.getHelmet());
        if (inv.getChestplate() != null) equipped.add(inv.getChestplate());
        if (inv.getLeggings() != null) equipped.add(inv.getLeggings());
        if (inv.getBoots() != null) equipped.add(inv.getBoots());

        int durationTicks = intervalTicks + 20;

        for (ItemStack item : equipped) {
            if (item == null || !item.hasItemMeta()) continue;

            var pdc = item.getItemMeta().getPersistentDataContainer();

            String blessingRaw = pdc.get(RPGKeys.Item.blessingType(), PersistentDataType.STRING);
            if (blessingRaw != null) {
                try {
                    BlessingType blessing = BlessingType.valueOf(blessingRaw);
                    player.addPotionEffect(new PotionEffect(
                            blessing.getEffectType(), durationTicks, blessing.getAmplifier(), true, false));
                } catch (IllegalArgumentException ignored) {
                }
            }

            String curseRaw = pdc.get(RPGKeys.Item.curseType(), PersistentDataType.STRING);
            if (curseRaw != null) {
                try {
                    CurseType curse = CurseType.valueOf(curseRaw);
                    player.addPotionEffect(new PotionEffect(
                            curse.getEffectType(), durationTicks, curse.getAmplifier(), true, false));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
