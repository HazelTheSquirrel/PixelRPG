package de.pixelrpg.rpg.core;

/**
 * Player level progression for PixelRPG.
 * The normal player progression is strictly limited to levels 1-60.
 */
public final class Level {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_NORMAL_LEVEL = 60;
    /** Compatibility marker for callers that distinguish the unreachable post-cap level. */
    public static final int RESERVED_LEVEL = MAX_NORMAL_LEVEL + 1;

    private static final long[] REQUIRED_EXPERIENCE = createExperienceTable();

    private Level() { }

    public static int fromExperience(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int low = 0;
        int high = REQUIRED_EXPERIENCE.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (experience >= REQUIRED_EXPERIENCE[middle]) low = middle + 1;
            else high = middle - 1;
        }
        return Math.max(MIN_LEVEL, Math.min(MAX_NORMAL_LEVEL, low));
    }

    /** Returns cumulative XP at the beginning of the requested normal level. */
    public static long getRequiredExperience(int level) {
        validateNormalLevel(level);
        return REQUIRED_EXPERIENCE[level - 1];
    }

    /** Returns cumulative XP required to enter the next normal level, or the current threshold at level 60. */
    public static long getExperienceForNextLevel(int level) {
        validateNormalLevel(level);
        return level == MAX_NORMAL_LEVEL ? REQUIRED_EXPERIENCE[level - 1] : REQUIRED_EXPERIENCE[level];
    }

    /** Returns XP still required for the next level, or zero once level 60 is reached. */
    public static long getExperienceToNextLevel(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int level = fromExperience(experience);
        if (level == MAX_NORMAL_LEVEL) return 0L;
        return Math.max(0L, getExperienceForNextLevel(level) - experience);
    }

    /** Compatibility threshold for legacy callers; level 60 is the hard cap and has no further progression. */
    public static long getExperienceForTranscendence() {
        return REQUIRED_EXPERIENCE[MAX_NORMAL_LEVEL - 1];
    }

    public static boolean isReservedLevel(int level) { return level == RESERVED_LEVEL; }

    public static long getExperienceIntoLevel(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int level = fromExperience(experience);
        return experience - REQUIRED_EXPERIENCE[level - 1];
    }

    public static long getExperienceForCurrentLevel(int level) { return getRequiredExperience(level); }
    public static boolean isMaxNormalLevel(int level) { return level == MAX_NORMAL_LEVEL; }
    public static boolean isValidNormalLevel(int level) { return level >= MIN_LEVEL && level <= MAX_NORMAL_LEVEL; }

    private static void validateNormalLevel(int level) {
        if (!isValidNormalLevel(level)) throw new IllegalArgumentException("Level must be between 1 and 60: " + level);
    }

    private static long[] createExperienceTable() {
        long[] experience = new long[MAX_NORMAL_LEVEL];
        long[] xpToNextLevel = {
            400L, 900L, 1400L, 2100L, 2800L, 3600L, 4500L, 5400L, 6500L, 7600L,
            8700L, 9800L, 11000L, 12300L, 13600L, 15000L, 16400L, 17800L, 19300L, 20800L,
            22400L, 24000L, 25500L, 27200L, 28900L, 30500L, 32200L, 33900L, 36300L, 38800L,
            41600L, 44600L, 48000L, 51400L, 55000L, 58700L, 62400L, 66200L, 70200L, 74300L,
            78500L, 82800L, 87100L, 91600L, 96300L, 101000L, 105800L, 110700L, 115700L, 120900L,
            126100L, 131500L, 137000L, 142500L, 148200L, 154000L, 159900L, 165800L, 172000L
        };

        long cumulative = 0L;
        experience[0] = 0L;
        for (int level = 1; level < MAX_NORMAL_LEVEL; level++) {
            cumulative = Math.addExact(cumulative, xpToNextLevel[level - 1]);
            experience[level] = cumulative;
        }
        return experience;
    }
}
