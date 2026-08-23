package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Handles passive companion protection, passive player bonuses and runtime lifecycle. */
public final class CompanionExperienceListener implements Listener {
    private final CompanionService companionService;
    private final JavaPlugin plugin;
    private final NamespacedKey healthKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey critChanceKey;
    private final NamespacedKey critDamageKey;
    private final NamespacedKey lifestealKey;
    private final BukkitTask passiveStatsTask;

    public CompanionExperienceListener(CompanionService companionService) {
        this.companionService = companionService;
        this.plugin = JavaPlugin.getProvidingPlugin(CompanionExperienceListener.class);
        this.healthKey = new NamespacedKey(plugin, "companion_health");
        this.armorKey = new NamespacedKey(plugin, "companion_armor");
        this.critChanceKey = new NamespacedKey(plugin, "companion_crit_chance");
        this.critDamageKey = new NamespacedKey(plugin, "companion_crit_damage");
        this.lifestealKey = new NamespacedKey(plugin, "companion_lifesteal");
        this.passiveStatsTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllPlayers, 1L, 10L);
    }

    // Restores the player's persisted active companion after the player entity is fully joined.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        companionService.restoreActive(event.getPlayer());
        refreshPlayer(event.getPlayer());
    }

    // Clears a companion's active runtime state when its spawned entity dies.
    @EventHandler
    public void onCompanionDeath(EntityDeathEvent event) {
        if (!isCompanion(event.getEntity())) return;
        java.util.UUID ownerUuid = companionService.getOwnerOfEntity(event.getEntity().getUniqueId());
        if (ownerUuid != null) companionService.clearActive(ownerUuid);
    }

    // Makes all non-Unique-Mannequin companions completely invulnerable.
    @EventHandler
    public void onCompanionDamage(EntityDamageEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Prevents hostile vanilla AI from targeting passive companions or their owners.
    @EventHandler
    public void onCompanionTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !isCompanion(entity)) return;
        if (isUniqueMannequin(entity)) return;
        event.setCancelled(true);
        if (entity instanceof Mob mob) mob.setTarget(null);
    }

    // Prevents fire-based vanilla entities from burning while used as passive companions.
    @EventHandler
    public void onCompanionCombust(EntityCombustEvent event) {
        if (!isCompanion(event.getEntity())) return;
        if (isUniqueMannequin((LivingEntity) event.getEntity())) return;
        event.setCancelled(true);
    }

    // Despawns the runtime entity on quit while preserving the persisted active selection for the next join.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        companionService.despawn(event.getPlayer().getUniqueId());
        refreshPlayerWithoutCompanion(event.getPlayer());
    }

    private void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) refreshPlayer(player);
    }

    private void refreshPlayer(Player player) {
        Companion active = companionService.getActive(player.getUniqueId());
        if (active == null) {
            refreshPlayerWithoutCompanion(player);
            return;
        }

        CompanionDefinition definition = companionService.definition(active.id());
        if (definition.rarity().isUnique() && definition.visual().type() == CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN) {
            refreshPlayerWithoutCompanion(player);
            return;
        }

        CompanionStats stats = passiveStats(definition);
        setModifier(player, Attribute.MAX_HEALTH, healthKey, stats.health());
        setModifier(player, Attribute.ARMOR, armorKey, stats.armor());
        player.getPersistentDataContainer().set(critChanceKey, PersistentDataType.DOUBLE, stats.critChance());
        player.getPersistentDataContainer().set(critDamageKey, PersistentDataType.DOUBLE, stats.critDamage());
        player.getPersistentDataContainer().set(lifestealKey, PersistentDataType.DOUBLE, stats.lifesteal());

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) player.setHealth(maxHealth.getValue());
    }

    private CompanionStats passiveStats(CompanionDefinition definition) {
        CompanionStats configured = definition.baseStats();
        CompanionRarity rarity = definition.rarity();
        double tier = switch (rarity) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 2.0D;
            case RARE -> 3.5D;
            case EPIC -> 5.0D;
            case LEGENDARY -> 7.5D;
            case UNIQUE -> 0.0D;
        };
        return new CompanionStats(
                configured.health() > 0.0D ? configured.health() : tier * 2.0D,
                0.0D,
                0.0D,
                configured.armor() > 0.0D ? configured.armor() : tier,
                configured.critChance() > 0.0D ? configured.critChance() : tier * 0.5D,
                configured.critDamage() > 0.0D ? configured.critDamage() : tier * 0.02D,
                configured.lifesteal() > 0.0D ? configured.lifesteal() : tier * 0.25D,
                0.0D,
                0.0D);
    }

    private void setModifier(Player player, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        removeModifier(player, attribute, key);
        if (amount <= 0.0D) return;
        instance.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier modifier = instance.getModifier(key);
        if (modifier != null) instance.removeModifier(modifier);
    }

    private void clearBonusData(Player player) {
        player.getPersistentDataContainer().remove(critChanceKey);
        player.getPersistentDataContainer().remove(critDamageKey);
        player.getPersistentDataContainer().remove(lifestealKey);
    }

    private boolean isCompanion(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(RPGKeys.Companion.id(), PersistentDataType.STRING);
    }

    private boolean isUniqueMannequin(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (id == null) return false;
        CompanionDefinition definition = companionService.definition(id);
        return definition.rarity().isUnique()
                && definition.visual().type() == CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN;
    }

    public void shutdown() {
        passiveStatsTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) refreshPlayerWithoutCompanion(player);
    }

    private void refreshPlayerWithoutCompanion(Player player) {
        removeModifier(player, Attribute.MAX_HEALTH, healthKey);
        removeModifier(player, Attribute.ARMOR, armorKey);
        clearBonusData(player);
    }
}
