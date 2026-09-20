package de.pixelrpg.rpg.companion;

/** Central companion XP and level progression engine. */
public final class CompanionProgression {
    private final CompanionRegistry registry;

    public CompanionProgression(CompanionRegistry registry) {
        this.registry = registry;
    }

    public int maxLevel(CompanionDefinition definition) {
        if (definition.rarity().isUnique()) return 1;
        return Math.min(registry.maxLevel(), definition.progression().maxLevel());
    }

    public long experienceToNextLevel(CompanionDefinition definition, int level) {
        return level >= maxLevel(definition) ? Long.MAX_VALUE : registry.experienceBase() + (long) level * registry.experiencePerLevel();
    }

    public int levelForExperience(CompanionDefinition definition, long experience) {
        long remaining = Math.max(0L, experience);
        int level = 1;
        int maxLevel = maxLevel(definition);
        while (level < maxLevel) {
            long required = experienceToNextLevel(definition, level);
            if (remaining < required) break;
            remaining -= required;
            level++;
        }
        return level;
    }

    public long experienceWithinLevel(CompanionDefinition definition, CompanionInstance instance) {
        long remaining = instance.experience();
        for (int level = 1; level < instance.level(); level++) remaining -= experienceToNextLevel(definition, level);
        return Math.max(0L, remaining);
    }

    public CompanionInstance addExperience(CompanionDefinition definition, CompanionInstance instance, long baseExperience) {
        if (definition.rarity().isUnique() || baseExperience <= 0L || instance.level() >= maxLevel(definition)) return instance;
        long gained = Math.max(1L, Math.round(baseExperience * definition.rarity().experienceMultiplier()));
        long total = instance.experience() + gained;
        return instance.withProgress(levelForExperience(definition, total), total);
    }
}
