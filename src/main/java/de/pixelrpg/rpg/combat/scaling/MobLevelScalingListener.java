package de.pixelrpg.rpg.combat.scaling;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
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
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobLevelScalingListener implements Listener {
    private static final long COMBAT_TIMEOUT_MILLIS = 5_000L;

    private final Plugin plugin;
    private final GuildAPI guildAPI;
    private final MobScalingConfig scalingConfig;
    private final Map<UUID, Map<UUID, Long>> activeParticipants = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public MobLevelScalingListener(Plugin plugin, GuildAPI guildAPI, MobScalingConfig scalingConfig) {
        this.plugin = plugin;
        this.guildAPI = guildAPI;
        this.scalingConfig = scalingConfig;
    }

    public void start() {
        if (cleanupTask != null) return;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::restoreExpiredScaling, 20L, 20L);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        activeParticipants.clear();
    }

    // Zuständig dafür, dass die Skalierung beim Zielwechsel auf einen registrierten Spieler aktiviert wird.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        if (event.getTarget() instanceof Player player && guildAPI.isRegistered(player.getUniqueId())) {
            markParticipant(monster, player);
        }
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

    private void markParticipant(Monster monster, Player player) {
        activeParticipants.computeIfAbsent(monster.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis());
        applyScaling(monster);
    }

    private void applyScaling(Monster monster) {
        rememberOriginalAttributes(monster);

        Map<UUID, Long> participants = activeParticipants.getOrDefault(monster.getUniqueId(), Map.of());
        int playerLevel = participants.keySet().stream()
                .mapToInt(guildAPI::getLevel)
                .max()
                .orElse(1);
        playerLevel = Math.clamp(playerLevel, 1, 99);
        final int scalingPlayerLevel = playerLevel;

        double gearMultiplier = participants.keySet().stream()
                .map(this::findPlayer)
                .filter(player -> player != null)
                .mapToDouble(player -> gearLevelMultiplier(player, scalingPlayerLevel))
                .max()
                .orElse(1.0D);

        MobScalingConfig.LevelBaseStats baseStats = scalingConfig.getBaseStats(scalingPlayerLevel);
        double maxHealth = baseStats.hp() * scalingConfig.getPlayerParityMultiplier() * gearMultiplier;
        double attackDamage = baseStats.damage() * scalingConfig.getPlayerParityMultiplier() * gearMultiplier;

        AttributeInstance hp = monster.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            double oldMaxHealth = Math.max(1.0D, hp.getValue());
            double healthRatio = Math.clamp(monster.getHealth() / oldMaxHealth, 0.0D, 1.0D);
            hp.setBaseValue(maxHealth);
            monster.setHealth(Math.clamp(maxHealth * healthRatio, 0.0D, maxHealth));
        }

        AttributeInstance attack = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attackDamage);
        monster.getPersistentDataContainer().set(RPGKeys.Combat.mobLevel(), PersistentDataType.INTEGER, scalingPlayerLevel);
    }

    private double gearLevelMultiplier(Player player, int playerLevel) {
        int counted = 0;
        double totalLevel = 0.0D;
        for (ItemStack item : equippedItems(player)) {
            if (item == null || !item.hasItemMeta()) continue;
            Integer itemLevel = item.getItemMeta().getPersistentDataContainer().get(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER);
            if (itemLevel == null) continue;
            totalLevel += Math.clamp(itemLevel, 1, 99);
            counted++;
        }
        if (counted == 0) return 1.0D;
        double averageItemLevel = totalLevel / counted;
        double ratio = averageItemLevel / Math.max(1.0D, playerLevel);
        return Math.clamp(ratio, scalingConfig.getMinimumGearMultiplier(), scalingConfig.getMaximumGearMultiplier());
    }

    private ItemStack[] equippedItems(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] equipped = new ItemStack[armor.length + 2];
        System.arraycopy(armor, 0, equipped, 0, armor.length);
        equipped[armor.length] = player.getInventory().getItemInMainHand();
        equipped[armor.length + 1] = player.getInventory().getItemInOffHand();
        return equipped;
    }

    private Player findPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline() ? player : null;
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

    private void restoreExpiredScaling() {
        long now = System.currentTimeMillis();
        for (UUID mobUuid : activeParticipants.keySet()) {
            Map<UUID, Long> participants = activeParticipants.get(mobUuid);
            if (participants == null) continue;
            participants.entrySet().removeIf(entry -> now - entry.getValue() >= COMBAT_TIMEOUT_MILLIS || !guildAPI.isRegistered(entry.getKey()));

            var entity = Bukkit.getEntity(mobUuid);
            if (!(entity instanceof Monster monster)) {
                activeParticipants.remove(mobUuid);
                continue;
            }
            if (participants.isEmpty()) {
                restoreVanillaScaling(monster);
                activeParticipants.remove(mobUuid);
                continue;
            }
            applyScaling(monster);
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
}
