package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.PlayerLevelUpEvent;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.core.WakeScheduler;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Event-driven monster scaling. Active combat owns its own expiry wake-up; idle mobs are untouched. */
public final class MobLevelScalingListener implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;
    private static final long COMBAT_TIMEOUT_TICKS = 100L;

    private final Plugin plugin;
    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, Map<UUID, Long>> activeParticipants = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> mobsByParticipant = new ConcurrentHashMap<>();
    private final Map<UUID, Double> gearMultiplierCache = new ConcurrentHashMap<>();
    private final Map<UUID, ScalingSnapshot> scalingCache = new ConcurrentHashMap<>();
    private final WakeScheduler<ParticipantKey> expiryScheduler;

    public MobLevelScalingListener(Plugin plugin, GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.plugin = plugin;
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
        this.expiryScheduler = new WakeScheduler<>(plugin);
    }

    public void start() {
    }

    public void shutdown() {
        expiryScheduler.clear();
        activeParticipants.clear();
        mobsByParticipant.clear();
        gearMultiplierCache.clear();
        scalingCache.clear();
    }

    // Zuständig dafür, dass die Skalierung beim Zielwechsel auf einen registrierten Spieler aktiviert wird.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        if (event.getTarget() instanceof Player player && guildAPI.isRegistered(player.getUniqueId())) markParticipant(monster, player);
    }

    // Zuständig dafür, dass jeder registrierte RPG-Angreifer als aktiver Teilnehmer der Monster-Skalierung erfasst wird.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRpgDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) attacker = player;
        }
        if (attacker == null || !guildAPI.isRegistered(attacker.getUniqueId())) return;
        markParticipant(monster, attacker);
    }

    // Zuständig dafür, dass ein Level-Up nur die tatsächlich betroffenen aktiven Mobs neu skaliert.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLevelUp(PlayerLevelUpEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        gearMultiplierCache.remove(playerUuid);
        Set<UUID> affectedMobs = mobsByParticipant.get(playerUuid);
        if (affectedMobs == null) return;
        Set<UUID> staleMobs = null;
        for (UUID mobUuid : affectedMobs) {
            Map<UUID, Long> participants = activeParticipants.get(mobUuid);
            if (participants == null || !participants.containsKey(playerUuid)) continue;
            var entity = Bukkit.getEntity(mobUuid);
            if (!(entity instanceof Monster monster) || !monster.isValid() || monster.isDead()) {
                if (staleMobs == null) staleMobs = ConcurrentHashMap.newKeySet();
                staleMobs.add(mobUuid);
                continue;
            }
            participants.put(playerUuid, System.currentTimeMillis());
            scheduleExpiry(mobUuid, playerUuid);
            scalingCache.remove(mobUuid);
            applyScaling(monster);
        }
        if (staleMobs != null) {
            for (UUID mobUuid : staleMobs) {
                Map<UUID, Long> participants = activeParticipants.get(mobUuid);
                if (participants != null && participants.containsKey(playerUuid)) removeMob(mobUuid, participants);
            }
        }
    }

    // Zuständig dafür, dass Equipmentänderungen nur die Gear-Cachewerte und betroffene Mobs invalidieren.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        invalidateGearAndScaling(event.getPlayer().getUniqueId());
    }

    // Zuständig für die Invalidierung der Gear-Skalierung beim Wechsel des aktiven Hotbar-Slots.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        invalidateGearAndScaling(event.getPlayer().getUniqueId());
    }

    // Zuständig für die Freigabe des spielerbezogenen Scaling-Caches beim Disconnect.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gearMultiplierCache.remove(event.getPlayer().getUniqueId());
        mobsByParticipant.remove(event.getPlayer().getUniqueId());
    }

    // Zuständig für die sofortige Freigabe aller Skalierungsdaten beim Tod eines verwalteten Monsters.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        Map<UUID, Long> participants = activeParticipants.remove(monster.getUniqueId());
        scalingCache.remove(monster.getUniqueId());
        if (participants == null) return;
        removeReverseIndex(monster.getUniqueId(), participants.keySet());
        participants.keySet().forEach(playerUuid -> cancelParticipant(monster.getUniqueId(), playerUuid));
    }

    // Zuständig dafür, dass entladene Mobs keine Spieler- oder Scheduler-Referenzen im Scaling-System behalten.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (var entity : event.getEntities()) {
            if (!(entity instanceof Monster monster)) continue;
            Map<UUID, Long> participants = activeParticipants.remove(monster.getUniqueId());
            scalingCache.remove(monster.getUniqueId());
            if (participants == null) continue;
            removeReverseIndex(monster.getUniqueId(), participants.keySet());
            participants.keySet().forEach(playerUuid -> cancelParticipant(monster.getUniqueId(), playerUuid));
        }
    }

    private void invalidateGearAndScaling(UUID playerUuid) {
        gearMultiplierCache.remove(playerUuid);
        Set<UUID> affectedMobs = mobsByParticipant.get(playerUuid);
        if (affectedMobs == null) return;
        Set<UUID> staleMobs = null;
        for (UUID mobUuid : affectedMobs) {
            Map<UUID, Long> participants = activeParticipants.get(mobUuid);
            if (participants == null || !participants.containsKey(playerUuid)) continue;
            var entity = Bukkit.getEntity(mobUuid);
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                scalingCache.remove(mobUuid);
                applyScaling(monster);
            } else {
                if (staleMobs == null) staleMobs = ConcurrentHashMap.newKeySet();
                staleMobs.add(mobUuid);
            }
        }
        if (staleMobs != null) {
            for (UUID mobUuid : staleMobs) {
                Map<UUID, Long> participants = activeParticipants.get(mobUuid);
                if (participants != null && participants.containsKey(playerUuid)) removeMob(mobUuid, participants);
            }
        }
    }

    private void markParticipant(Monster monster, Player player) {
        UUID mobUuid = monster.getUniqueId();
        UUID playerUuid = player.getUniqueId();
        activeParticipants.computeIfAbsent(mobUuid, ignored -> new ConcurrentHashMap<>()).put(playerUuid, System.currentTimeMillis());
        mobsByParticipant.computeIfAbsent(playerUuid, ignored -> ConcurrentHashMap.newKeySet()).add(mobUuid);
        scheduleExpiry(mobUuid, playerUuid);
        applyScaling(monster);
    }

    private void scheduleExpiry(UUID mobUuid, UUID playerUuid) {
        ParticipantKey key = new ParticipantKey(mobUuid, playerUuid);
        expiryScheduler.cancel(key);
        expiryScheduler.wakeLater(key, COMBAT_TIMEOUT_TICKS, () -> expireParticipant(key));
    }

    private void expireParticipant(ParticipantKey key) {
        Map<UUID, Long> participants = activeParticipants.get(key.mobUuid());
        if (participants == null) return;
        Long lastActivity = participants.get(key.playerUuid());
        if (lastActivity == null) return;
        if (System.currentTimeMillis() - lastActivity < COMBAT_TIMEOUT_MILLIS) {
            scheduleExpiry(key.mobUuid(), key.playerUuid());
            return;
        }
        participants.remove(key.playerUuid());
        Set<UUID> reverse = mobsByParticipant.get(key.playerUuid());
        if (reverse != null) {
            reverse.remove(key.mobUuid());
            if (reverse.isEmpty()) mobsByParticipant.remove(key.playerUuid(), reverse);
        }
        scalingCache.remove(key.mobUuid());
        if (participants.isEmpty()) {
            activeParticipants.remove(key.mobUuid(), participants);
            var entity = Bukkit.getEntity(key.mobUuid());
            if (entity instanceof Monster monster && monster.isValid()) restoreVanillaScaling(monster);
        } else {
            var entity = Bukkit.getEntity(key.mobUuid());
            if (entity instanceof Monster monster && monster.isValid()) applyScaling(monster);
        }
    }

    private void cancelParticipant(UUID mobUuid, UUID playerUuid) {
        expiryScheduler.cancel(new ParticipantKey(mobUuid, playerUuid));
    }

    private void applyScaling(Monster monster) {
        UUID mobUuid = monster.getUniqueId();
        rememberOriginalAttributes(monster);
        Map<UUID, Long> participants = activeParticipants.getOrDefault(mobUuid, Map.of());
        int playerLevel = 1;
        double gearMultiplier = 1.0D;
        for (UUID playerUuid : participants.keySet()) {
            playerLevel = Math.max(playerLevel, guildAPI.getLevel(playerUuid));
            gearMultiplier = Math.max(gearMultiplier, gearLevelMultiplierCached(playerUuid));
        }
        playerLevel = Math.clamp(playerLevel, Level.MIN_LEVEL, Level.MAX_NORMAL_LEVEL);
        ScalingSnapshot desired = new ScalingSnapshot(playerLevel, gearMultiplier);
        ScalingSnapshot previous = scalingCache.put(mobUuid, desired);

        MobScalingConfig.LevelBaseStats baseStats = scalingConfig.getBaseStats(playerLevel);
        double parityMultiplier = scalingConfig.getPlayerParityMultiplier();
        double expectedMaxHealth = baseStats.hp() * parityMultiplier * gearMultiplier;
        double expectedAttackDamage = baseStats.damage() * parityMultiplier * gearMultiplier;
        var pdc = monster.getPersistentDataContainer();
        int storedMobLevel = pdc.getOrDefault(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, -1);
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        boolean attributesAlreadyMatch = storedMobLevel == playerLevel
                && (hp == null || Double.compare(hp.getBaseValue(), expectedMaxHealth) == 0)
                && (attack == null || Double.compare(attack.getBaseValue(), expectedAttackDamage) == 0);
        if (previous != null && previous.equals(desired) && attributesAlreadyMatch) return;
        if (attributesAlreadyMatch) return;

        if (hp != null) {
            double oldMaxHealth = Math.max(1.0D, hp.getValue());
            double healthRatio = Math.clamp(monster.getHealth() / oldMaxHealth, 0.0D, 1.0D);
            hp.setBaseValue(expectedMaxHealth);
            monster.setHealth(Math.clamp(expectedMaxHealth * healthRatio, 0.0D, expectedMaxHealth));
        }
        if (attack != null) attack.setBaseValue(expectedAttackDamage);
        pdc.set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, playerLevel);
    }

    private double gearLevelMultiplierCached(UUID playerUuid) {
        return gearMultiplierCache.computeIfAbsent(playerUuid, uuid -> {
            Player player = findPlayer(uuid);
            if (player == null) return 1.0D;
            int playerLevel = Math.clamp(guildAPI.getLevel(uuid), Level.MIN_LEVEL, Level.MAX_NORMAL_LEVEL);
            return gearLevelMultiplier(player, playerLevel);
        });
    }

    private double gearLevelMultiplier(Player player, int playerLevel) {
        int counted = 0;
        double totalLevel = 0.0D;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            Integer itemLevel = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
            if (itemLevel == null) continue;
            totalLevel += Math.clamp(itemLevel, 1, 99);
            counted++;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta()) {
            Integer itemLevel = mainHand.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
            if (itemLevel != null) {
                totalLevel += Math.clamp(itemLevel, 1, 99);
                counted++;
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.hasItemMeta()) {
            Integer itemLevel = offHand.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
            if (itemLevel != null) {
                totalLevel += Math.clamp(itemLevel, 1, 99);
                counted++;
            }
        }
        if (counted == 0) return 1.0D;
        double averageItemLevel = totalLevel / counted;
        double ratio = averageItemLevel / Math.max(1.0D, playerLevel);
        return Math.clamp(ratio, scalingConfig.getMinimumGearMultiplier(), scalingConfig.getMaximumGearMultiplier());
    }

    private Player findPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline() ? player : null;
    }

    private void removeMob(UUID mobUuid, Map<UUID, Long> participants) {
        activeParticipants.remove(mobUuid, participants);
        scalingCache.remove(mobUuid);
        removeReverseIndex(mobUuid, participants.keySet());
        participants.keySet().forEach(playerUuid -> cancelParticipant(mobUuid, playerUuid));
    }

    private void removeReverseIndex(UUID mobUuid, Set<UUID> participants) {
        for (UUID playerUuid : participants) {
            Set<UUID> reverse = mobsByParticipant.get(playerUuid);
            if (reverse == null) continue;
            reverse.remove(mobUuid);
            if (reverse.isEmpty()) mobsByParticipant.remove(playerUuid, reverse);
        }
    }

    private void rememberOriginalAttributes(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && !pdc.has(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE)) {
            pdc.set(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE, hp.getBaseValue());
        }
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null && !pdc.has(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE)) {
            pdc.set(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE, attack.getBaseValue());
        }
    }

    private void restoreVanillaScaling(Monster monster) {
        var pdc = monster.getPersistentDataContainer();
        if (!pdc.has(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER)) return;
        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        Double originalMaxHealth = pdc.get(RPGKeys.Combat.originalMaxHealth(), PersistentDataType.DOUBLE);
        if (hp != null && originalMaxHealth != null) {
            double oldMaxHealth = Math.max(1.0D, hp.getValue());
            double healthRatio = Math.clamp(monster.getHealth() / oldMaxHealth, 0.0D, 1.0D);
            hp.setBaseValue(originalMaxHealth);
            monster.setHealth(Math.clamp(originalMaxHealth * healthRatio, 0.0D, originalMaxHealth));
        }
        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        Double originalAttackDamage = pdc.get(RPGKeys.Combat.originalAttackDamage(), PersistentDataType.DOUBLE);
        if (attack != null && originalAttackDamage != null) attack.setBaseValue(originalAttackDamage);
        pdc.remove(RPGKeys.Combat.mobLevel());
        pdc.remove(RPGKeys.Combat.originalMaxHealth());
        pdc.remove(RPGKeys.Combat.originalAttackDamage());
    }

    private record ParticipantKey(UUID mobUuid, UUID playerUuid) {
    }

    private record ScalingSnapshot(int playerLevel, double gearMultiplier) {
    }
}
