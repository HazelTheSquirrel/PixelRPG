package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.Level;
import org.bukkit.entity.EntityType;

import java.util.List;

public final class BossDefinition {
    private final String id;
    private String displayName;
    private EntityType baseEntityType;
    private int level;
    private double healthMultiplier;
    private double damageMultiplier;
    private double scaleMultiplier;
    private List<BossPhase> phases;
    private BossLootConfig lootConfig;

    public BossDefinition(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.baseEntityType = EntityType.ZOMBIE;
        this.level = 30;
        this.healthMultiplier = 5.0;
        this.damageMultiplier = 2.0;
        this.scaleMultiplier = 1.35;
        this.phases = List.of();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public EntityType getBaseEntityType() { return baseEntityType; }
    public void setBaseEntityType(EntityType baseEntityType) { this.baseEntityType = baseEntityType; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, level)); }
    public double getHealthMultiplier() { return healthMultiplier; }
    public void setHealthMultiplier(double healthMultiplier) { this.healthMultiplier = Math.max(0.1D, healthMultiplier); }
    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = Math.max(0.1D, damageMultiplier); }
    public double getScaleMultiplier() { return scaleMultiplier; }
    public void setScaleMultiplier(double scaleMultiplier) { this.scaleMultiplier = Math.max(0.1D, scaleMultiplier); }
    public List<BossPhase> getPhases() { return phases; }
    public void setPhases(List<BossPhase> phases) { this.phases = phases; }
    public BossLootConfig getLootConfig() { return lootConfig; }
    public void setLootConfig(BossLootConfig lootConfig) { this.lootConfig = lootConfig; }
}
