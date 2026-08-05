package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.BlessingType;
import de.pixelrpg.rpg.item.CurseType;
import de.pixelrpg.rpg.item.SocketService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentAuraListener {

    private final int intervalTicks;

    public EquipmentAuraListener(int intervalTicks) {
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(PixelRPGPlugin.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
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
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            var pdc = item.getItemMeta().getPersistentDataContainer();

            String blessingRaw = pdc.get(RPGKeys.Item.blessingType(), PersistentDataType.STRING);
            if (blessingRaw != null) {
                try {
                    BlessingType blessing = BlessingType.valueOf(blessingRaw);
                    player.addPotionEffect(new PotionEffect(blessing.getEffectType(), durationTicks, blessing.getAmplifier(), true, false));
                } catch (IllegalArgumentException ignored) {
                }
            }

            String curseRaw = pdc.get(RPGKeys.Item.curseType(), PersistentDataType.STRING);
            if (curseRaw != null) {
                try {
                    CurseType curse = CurseType.valueOf(curseRaw);
                    player.addPotionEffect(new PotionEffect(curse.getEffectType(), durationTicks, curse.getAmplifier(), true, false));
                } catch (IllegalArgumentException ignored) {
                }
            }

            for (String rawSocket : SocketService.readRawSockets(item)) {
                if (rawSocket.startsWith("REGENERATION:")) {
                    double value = parseValue(rawSocket);
                    int amplifier = Math.min(3, (int) Math.round(value / 1.5));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, amplifier, true, false));
                } else if (rawSocket.startsWith("AGILITY_ATTUNEMENT:")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, false));
                } else if (rawSocket.startsWith("TOUGHNESS_ATTUNEMENT:")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, true, false));
                } else if (rawSocket.startsWith("CLASS_SIGIL:")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, durationTicks, 0, true, false));
                }
            }
        }
    }

    private double parseDoubleSafe(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    private double parseValue(String rawSocket) {
        String[] parts = rawSocket.split(":");
        return parts.length > 1 ? parseDoubleSafe(parts[1]) : 1.0;
    }
}