package de.pixelrpg.rpg.boss;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ActiveBoss {
    private final UUID entityUuid;
    private final BossDefinition definition;
    private final BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();
    private final Map<UUID, Double> damageContribution = new HashMap<>();
    private int currentPhaseIndex = -1;
    private int ticksSinceLastAttack = 0;
    private int ticksSinceLastBarUpdate = Integer.MAX_VALUE;
    private Location lastValidBiomeLocation;
    private BukkitTask task;

    public ActiveBoss(UUID entityUuid, BossDefinition definition, BossBar bossBar) {
        this.entityUuid = entityUuid;
        this.definition = definition;
        this.bossBar = bossBar;
    }

    public UUID getEntityUuid() { return entityUuid; }
    public BossDefinition getDefinition() { return definition; }
    public BossBar getBossBar() { return bossBar; }
    public Set<UUID> getViewers() { return viewers; }
    public void recordDamage(UUID playerUuid, double damage) { if (damage > 0.0) damageContribution.merge(playerUuid, damage, Double::sum); }
    public Map<UUID, Double> getDamageContribution() { return Map.copyOf(damageContribution); }
    public int getCurrentPhaseIndex() { return currentPhaseIndex; }
    public void setCurrentPhaseIndex(int currentPhaseIndex) { this.currentPhaseIndex = currentPhaseIndex; }
    public int getTicksSinceLastAttack() { return ticksSinceLastAttack; }
    public void resetAttackTimer() { this.ticksSinceLastAttack = 0; }
    public void incrementAttackTimer(int amount) { this.ticksSinceLastAttack += amount; }
    public int getTicksSinceLastBarUpdate() { return ticksSinceLastBarUpdate; }
    public void incrementBarUpdateTimer(int amount) { this.ticksSinceLastBarUpdate += amount; }
    public void resetBarUpdateTimer() { this.ticksSinceLastBarUpdate = 0; }
    public Location getLastValidBiomeLocation() { return lastValidBiomeLocation; }
    public void setLastValidBiomeLocation(Location location) { this.lastValidBiomeLocation = location == null ? null : location.clone(); }
    public BukkitTask getTask() { return task; }
    public void setTask(BukkitTask task) { this.task = task; }
}
