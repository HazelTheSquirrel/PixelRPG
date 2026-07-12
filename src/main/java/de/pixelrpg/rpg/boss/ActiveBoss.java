// src/main/java/de/pixelrpg/rpg/boss/ActiveBoss.java
package de.pixelrpg.rpg.boss;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ActiveBoss {

    private final UUID entityUuid;
    private final BossDefinition definition;
    private final BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();
    private int currentPhaseIndex = -1;
    private int ticksSinceLastAttack = 0;
    private BukkitTask task;

    public ActiveBoss(UUID entityUuid, BossDefinition definition, BossBar bossBar) {
        this.entityUuid = entityUuid;
        this.definition = definition;
        this.bossBar = bossBar;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public BossDefinition getDefinition() {
        return definition;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void setCurrentPhaseIndex(int currentPhaseIndex) {
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public int getTicksSinceLastAttack() {
        return ticksSinceLastAttack;
    }

    public void resetAttackTimer() {
        this.ticksSinceLastAttack = 0;
    }

    public void incrementAttackTimer(int amount) {
        this.ticksSinceLastAttack += amount;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }
}