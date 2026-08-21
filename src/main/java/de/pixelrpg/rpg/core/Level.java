package de.pixelrpg.rpg.core;

import java.util.Arrays;

/**
 * Player level progression for PixelRPG.
 *
 * Levels 1-99 are the normal progression. Level 100 is reserved for a
 * separate endgame/transcendence system and is intentionally not part of
 * the normal level calculation.
 */
public final class Level {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_NORMAL_LEVEL = 99;
    public static final int RESERVED_LEVEL = 100;

    /*
     * WotLK-inspired progression anchor points. The curve is deliberately
     * stored as cumulative experience so the progression can be tuned without
     * changing player data storage. Values beyond level 80 continue the same
     * progression model and are intentionally much more demanding.
     */
    private static final long[] REQUIRED_EXPERIENCE = createExperienceTable();

    private Level() {
    }

    public static int fromExperience(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int level = MIN_LEVEL;

        for (int index = 0; index < REQUIRED_EXPERIENCE.length; index++) {
            if (experience < REQUIRED_EXPERIENCE[index]) {
                break;
            }
            level = index + 1;
        }

        return Math.min(level, MAX_NORMAL_LEVEL);
    }

    public static long getRequiredExperience(int level) {
        validateNormalLevel(level);
        return REQUIRED_EXPERIENCE[level - 1];
    }

    public static long getExperienceToNextLevel(long totalExperience) {
        int level = fromExperience(totalExperience);
        if (level >= MAX_NORMAL_LEVEL) {
            return 0L;
        }
        return Math.max(0L, REQUIRED_EXPERIENCE[level] - Math.max(0L, totalExperience));
    }

    public static long getExperienceIntoLevel(long totalExperience) {
        int level = fromExperience(totalExperience);
        return Math.max(0L, Math.max(0L, totalExperience) - REQUIRED_EXPERIENCE[level - 1]);
    }

    public static long getExperienceForCurrentLevel(int level) {
        return getRequiredExperience(level);
    }

    public static boolean isMaxNormalLevel(int level) {
        return level == MAX_NORMAL_LEVEL;
    }

    public static boolean isReservedLevel(int level) {
        return level == RESERVED_LEVEL;
    }

    public static boolean isValidNormalLevel(int level) {
        return level >= MIN_LEVEL && level <= MAX_NORMAL_LEVEL;
    }

    public static long getTotalExperienceForReservedLevel() {
        return Long.MAX_VALUE;
    }

    private static void validateNormalLevel(int level) {
        if (!isValidNormalLevel(level)) {
            throw new IllegalArgumentException("Level must be between " + MIN_LEVEL + " and " + MAX_NORMAL_LEVEL + ": " + level);
        }
    }

    private static long[] createExperienceTable() {
        long[] experience = new long[MAX_NORMAL_LEVEL];
        experience[0] = 0L;

        // WotLK-style accelerating progression, normalized for PixelRPG.
        for (int level = 2; level <= MAX_NORMAL_LEVEL; level++) {
            long previous = experience[level - 2];
            long increment = Math.round(250.0 * Math.pow(level - 1, 1.72));
            experience[level - 1] = Math.addExact(previous, Math.max(1L, increment));
        }

        return Arrays.copyOf(experience, experience.length);
    }
}
