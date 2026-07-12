// src/main/java/de/pixelrpg/rpg/boss/BossDefinition.java
package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.entity.EntityType;

import java.util.List;

public final class BossDefinition {

    private final String id;
    private String displayName;
    private EntityType baseEntityType;
    private Rank rank;
    private double healthMultiplier;
    private double damageMultiplier;
    private List<BossPhase> phases;
    private BossLootConfig lootConfig;

    public BossDefinition(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.baseEntityType = EntityType.ZOMBIE;
        this.rank = Rank.C;
        this.healthMultiplier = 5.0;
        this.damageMultiplier = 2.0;
        this.phases = List.of();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public EntityType getBaseEntityType() {
        return baseEntityType;
    }

    public void setBaseEntityType(EntityType baseEntityType) {
        this.baseEntityType = baseEntityType;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public void setHealthMultiplier(double healthMultiplier) {
        this.healthMultiplier = healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public List<BossPhase> getPhases() {
        return phases;
    }

    public void setPhases(List<BossPhase> phases) {
        this.phases = phases;
    }

    public BossLootConfig getLootConfig() {
        return lootConfig;
    }

    public void setLootConfig(BossLootConfig lootConfig) {
        this.lootConfig = lootConfig;
    }
}