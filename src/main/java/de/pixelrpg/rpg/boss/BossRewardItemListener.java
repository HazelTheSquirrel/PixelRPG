package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRewardItemListener implements Listener {
    private final GuildAPI guildAPI;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BossRewardItemListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
    }

    // Zuständig für die Aktivierung der PixelRPG-Bossbelohnungen per Rechtsklick; Vanilla-Spieler werden strikt ausgeschlossen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRewardUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!guildAPI.isRegistered(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String itemId = pdc.get(RPGKeys.Item.itemId(), PersistentDataType.STRING);
        String ability = pdc.get(RPGKeys.Item.weaponAbility(), PersistentDataType.STRING);
        if (itemId == null || !itemId.startsWith("pixelrpg:boss/") || ability == null || ability.isBlank()) return;

        long cooldown = pdc.getOrDefault(RPGKeys.Item.weaponAbilityCooldownMillis(), PersistentDataType.LONG, 0L);
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) return;
        cooldowns.put(player.getUniqueId(), now + Math.max(0L, cooldown));

        if (!activate(player, ability)) return;
        event.setCancelled(true);
    }

    private boolean activate(Player player, String ability) {
        switch (ability) {
            case "PLUNDERER_SEAL" -> effect(player, PotionEffectType.GLOWING, 10, 0);
            case "BEE_QUEEN_MARK" -> {
                effect(player, PotionEffectType.SPEED, 10, 0);
                effect(player, PotionEffectType.REGENERATION, 5, 0);
            }
            case "WITCH_CAULDRON" -> effect(player, PotionEffectType.POISON, 8, 0);
            case "KNAARZ_HEART" -> {
                effect(player, PotionEffectType.ABSORPTION, 15, 1);
                effect(player, PotionEffectType.RESISTANCE, 8, 0);
            }
            case "JUNGLE_AMULET" -> {
                effect(player, PotionEffectType.SPEED, 15, 0);
                effect(player, PotionEffectType.JUMP_BOOST, 15, 1);
            }
            case "SWAMP_POTION" -> {
                effect(player, PotionEffectType.WATER_BREATHING, 20, 0);
                effect(player, PotionEffectType.NIGHT_VISION, 20, 0);
            }
            case "HUSK_SEAL" -> {
                effect(player, PotionEffectType.FIRE_RESISTANCE, 20, 0);
                effect(player, PotionEffectType.STRENGTH, 10, 0);
            }
            case "RAVAGER_TROPHY" -> {
                effect(player, PotionEffectType.RESISTANCE, 12, 0);
                effect(player, PotionEffectType.STRENGTH, 8, 0);
            }
            case "GOLDEN_FOSSIL" -> {
                effect(player, PotionEffectType.HASTE, 20, 0);
                effect(player, PotionEffectType.LUCK, 20, 0);
            }
            case "FROSTWOLF_FANG" -> {
                effect(player, PotionEffectType.SPEED, 15, 1);
                effect(player, PotionEffectType.RESISTANCE, 10, 0);
            }
            case "FROST_ARROW_QUIVER" -> player.getInventory().addItem(new ItemStack(Material.SPECTRAL_ARROW, 8));
            case "MOUNTAIN_HORN" -> {
                effect(player, PotionEffectType.RESISTANCE, 12, 0);
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 18, 0.5, 0.7, 0.5, 0.01);
            }
            case "WILD_HORN" -> {
                effect(player, PotionEffectType.JUMP_BOOST, 15, 1);
                effect(player, PotionEffectType.SPEED, 15, 0);
            }
            case "BLOSSOM_HONEY" -> effect(player, PotionEffectType.REGENERATION, 10, 1);
            case "CAPTAINS_NAUTILUS" -> effect(player, PotionEffectType.WATER_BREATHING, 30, 0);
            case "RIVER_PEBBLE" -> effect(player, PotionEffectType.DOLPHINS_GRACE, 20, 0);
            case "EYE_OF_DEPTH" -> effect(player, PotionEffectType.CONDUIT_POWER, 20, 0);
            case "MYCELIUM_CORE" -> {
                effect(player, PotionEffectType.REGENERATION, 12, 1);
                effect(player, PotionEffectType.SATURATION, 1, 0);
            }
            case "SPIDER_EYE_HUNTER" -> {
                effect(player, PotionEffectType.NIGHT_VISION, 30, 0);
                effect(player, PotionEffectType.SPEED, 15, 0);
            }
            case "ECHO_HEART" -> {
                effect(player, PotionEffectType.RESISTANCE, 10, 1);
                effect(player, PotionEffectType.ABSORPTION, 10, 0);
            }
            case "NETHER_CORE" -> {
                effect(player, PotionEffectType.FIRE_RESISTANCE, 30, 0);
                effect(player, PotionEffectType.STRENGTH, 10, 0);
            }
            case "CRIMSON_HEART" -> {
                effect(player, PotionEffectType.FIRE_RESISTANCE, 20, 0);
                effect(player, PotionEffectType.REGENERATION, 10, 0);
            }
            case "BOUND_ENDER_PEARL" -> {
                player.teleport(player.getWorld().getSpawnLocation());
                return true;
            }
            case "SOUL_FRAGMENT" -> {
                effect(player, PotionEffectType.FIRE_RESISTANCE, 20, 0);
                effect(player, PotionEffectType.SLOW_FALLING, 15, 0);
            }
            case "MAGMA_HEART" -> {
                effect(player, PotionEffectType.FIRE_RESISTANCE, 30, 0);
                effect(player, PotionEffectType.STRENGTH, 12, 0);
            }
            case "SHULKER_CORE" -> effect(player, PotionEffectType.LEVITATION, 3, 0);
            case "RIFT_CORE" -> {
                effect(player, PotionEffectType.RESISTANCE, 15, 0);
                effect(player, PotionEffectType.STRENGTH, 12, 0);
            }
            case "STORM_HEART" -> {
                effect(player, PotionEffectType.SPEED, 20, 1);
                effect(player, PotionEffectType.SLOW_FALLING, 20, 0);
            }
            case "ABYSS_CORE" -> {
                effect(player, PotionEffectType.CONDUIT_POWER, 30, 0);
                effect(player, PotionEffectType.WATER_BREATHING, 30, 0);
            }
            case "SOUL_CROWN" -> {
                effect(player, PotionEffectType.STRENGTH, 15, 1);
                effect(player, PotionEffectType.FIRE_RESISTANCE, 20, 0);
            }
            case "END_RIFT" -> {
                effect(player, PotionEffectType.SPEED, 15, 1);
                effect(player, PotionEffectType.NIGHT_VISION, 30, 0);
            }
            case "WORLD_HEART" -> {
                effect(player, PotionEffectType.RESISTANCE, 20, 1);
                effect(player, PotionEffectType.ABSORPTION, 20, 1);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.6, 0.9, 0.6, 0.05);
            }
            default -> { return false; }
        }
        return true;
    }

    private void effect(Player player, PotionEffectType type, int seconds, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier));
    }
}
