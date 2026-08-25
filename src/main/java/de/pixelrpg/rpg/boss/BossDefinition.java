package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.Level;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.List;

public final class BossDefinition {
    private final String id;
    private String displayName;
    private EntityType baseEntityType;
    private BossKind kind;
    private Biome biome;
    private List<Biome> biomes = List.of();
    private int level;
    private double healthMultiplier;
    private double damageMultiplier;
    private double scaleMultiplier;
    private int attackIntervalTicks;
    private List<String> attackPatternIds;
    private List<BossPhase> phases;
    private BossLootConfig lootConfig;

    public BossDefinition(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.baseEntityType = EntityType.ZOMBIE;
        this.kind = BossKind.BIOME;
        this.level = 30;
        this.healthMultiplier = 5.0;
        this.damageMultiplier = 2.0;
        this.scaleMultiplier = 1.35;
        this.attackIntervalTicks = 100;
        this.attackPatternIds = List.of();
        this.phases = List.of();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public EntityType getBaseEntityType() { return baseEntityType; }
    public void setBaseEntityType(EntityType baseEntityType) { this.baseEntityType = baseEntityType; }
    public BossKind getKind() { return kind; }
    public void setKind(BossKind kind) { this.kind = kind == null ? BossKind.BIOME : kind; }
    public Biome getBiome() { return biome; }
    public void setBiome(Biome biome) { this.biome = biome; }
    public List<Biome> getBiomes() { return biomes; }
    public void setBiomes(List<Biome> biomes) {
        this.biomes = biomes == null ? List.of() : biomes.stream().filter(java.util.Objects::nonNull).distinct().toList();
        this.biome = this.biomes.isEmpty() ? null : this.biomes.getFirst();
    }
    public boolean matchesBiome(Biome biome) { return biome != null && this.biomes.contains(biome); }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, level)); }
    public double getHealthMultiplier() { return healthMultiplier; }
    public void setHealthMultiplier(double healthMultiplier) { this.healthMultiplier = Math.max(0.1D, healthMultiplier); }
    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = Math.max(0.1D, damageMultiplier); }
    public double getScaleMultiplier() { return scaleMultiplier; }
    public void setScaleMultiplier(double scaleMultiplier) { this.scaleMultiplier = Math.max(0.1D, scaleMultiplier); }
    public int getAttackIntervalTicks() { return attackIntervalTicks; }
    public void setAttackIntervalTicks(int attackIntervalTicks) { this.attackIntervalTicks = Math.max(1, attackIntervalTicks); }
    public List<String> getAttackPatternIds() { return attackPatternIds; }
    public void setAttackPatternIds(List<String> attackPatternIds) { this.attackPatternIds = attackPatternIds == null ? List.of() : List.copyOf(attackPatternIds); }
    public List<BossPhase> getPhases() { return phases; }
    public void setPhases(List<BossPhase> phases) { this.phases = phases == null ? List.of() : List.copyOf(phases); }
    public BossLootConfig getLootConfig() { return lootConfig; }
    public void setLootConfig(BossLootConfig lootConfig) { this.lootConfig = lootConfig; }
}
