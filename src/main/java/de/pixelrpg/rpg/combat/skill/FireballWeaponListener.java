package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles the PixelRPG Feuerball weapon without allowing block destruction or friendly fire. */
public final class FireballWeaponListener implements Listener {
    private static final long COOLDOWN_MILLIS = 3_000L;
    private static final double EXPLOSION_RADIUS = 3.5D;

    private final PlayerProfileManager profileManager;
    private final StatEngine statEngine;
    private final Map<UUID, Long> cooldownExpiry = new ConcurrentHashMap<>();

    public FireballWeaponListener(PlayerProfileManager profileManager, StatEngine statEngine) {
        this.profileManager = profileManager;
        this.statEngine = statEngine;
    }

    // Zuständig für das Verschießen des PixelRPG-Feuerballs per Rechtsklick.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFireballWeapon(item)) return;

        if (!canUse(player)) {
            event.setCancelled(true);
            return;
        }

        Fireball fireball = player.getWorld().spawn(player.getEyeLocation(), Fireball.class);
        fireball.setShooter(player);
        fireball.setDirection(player.getEyeLocation().getDirection());
        fireball.setYield(0.0F);
        fireball.setIsIncendiary(false);
        fireball.setGravity(false);

        fireball.getPersistentDataContainer().set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER,
                item.getItemMeta().getPersistentDataContainer().getOrDefault(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, 1));

        cooldownExpiry.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_MILLIS);
        player.sendActionBar(Component.text("Feuerball  •  3s Cooldown", NamedTextColor.GOLD));
        event.setCancelled(true);
    }

    // Zuständig für die schadensverursachende Explosion des Feuerballs ohne Blockschaden.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!(fireball.getShooter() instanceof Player shooter)) return;

        event.blockList().clear();
        event.setYield(0.0F);

        double attackPower = Math.max(0.5D, statEngine.getCachedStats(shooter.getUniqueId()).attackPower());
        for (LivingEntity target : fireball.getWorld().getNearbyEntities(
                fireball.getLocation(), EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS,
                entity -> entity instanceof Monster && !entity.equals(shooter))) {
            if (target.isDead()) continue;
            target.damage(attackPower, shooter);
        }
    }

    // Zuständig für das Freigeben des Feuerball-Cooldowns beim Verlassen des Servers.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cooldownExpiry.remove(event.getPlayer().getUniqueId());
    }

    private boolean canUse(Player player) {
        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return false;

        long now = System.currentTimeMillis();
        long expiry = cooldownExpiry.getOrDefault(player.getUniqueId(), 0L);
        if (now >= expiry) return true;

        long seconds = (expiry - now + 999L) / 1000L;
        player.sendActionBar(Component.text("Ability-Cooldown: " + seconds + "s", NamedTextColor.GRAY));
        return false;
    }

    private static boolean isFireballWeapon(ItemStack item) {
        if (item == null || item.isEmpty() || item.getType() != Material.FIRE_CHARGE || !item.hasItemMeta()) return false;
        return Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN));
    }
}
