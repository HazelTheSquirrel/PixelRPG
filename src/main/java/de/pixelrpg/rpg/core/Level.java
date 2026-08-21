package de.pixelrpg.rpg.core;

/**
 * Player level progression for PixelRPG.
 * Levels 1-99 are normal progression. Level 100 is reserved for the
 * separate transcendence endgame and is intentionally unreachable here.
 */
public final class Level {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_NORMAL_LEVEL = 99;
    public static final int RESERVED_LEVEL = 100;

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

    /** Returns cumulative XP required to enter the next normal level or transcendence at level 99. */
    public static long getExperienceForNextLevel(int level) {
        validateNormalLevel(level);
        return level == MAX_NORMAL_LEVEL ? getExperienceForTranscendence() : REQUIRED_EXPERIENCE[level];
    }

    /** Returns XP still required for the next level, including the level-99 transcendence grind. */
    public static long getExperienceToNextLevel(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int level = fromExperience(experience);
        return Math.max(0L, getExperienceForNextLevel(level) - experience);
    }

    /** Returns the deliberately absurd XP threshold for the reserved level 100. */
    public static long getExperienceForTranscendence() {
        long level98 = REQUIRED_EXPERIENCE[MAX_NORMAL_LEVEL - 2];
        long level99 = REQUIRED_EXPERIENCE[MAX_NORMAL_LEVEL - 1];
        long increment98To99 = level99 - level98;
        return Math.addExact(level99, Math.multiplyExact(increment98To99, 10_000L));
    }

    public static long getExperienceIntoLevel(long totalExperience) {
        long experience = Math.max(0L, totalExperience);
        int level = fromExperience(experience);
        return experience - REQUIRED_EXPERIENCE[level - 1];
    }

    public static long getExperienceForCurrentLevel(int level) { return getRequiredExperience(level); }
    public static boolean isMaxNormalLevel(int level) { return level == MAX_NORMAL_LEVEL; }
    public static boolean isReservedLevel(int level) { return level == RESERVED_LEVEL; }
    public static boolean isValidNormalLevel(int level) { return level >= MIN_LEVEL && level <= MAX_NORMAL_LEVEL; }

    private static void validateNormalLevel(int level) {
        if (!isValidNormalLevel(level)) throw new IllegalArgumentException("Level must be between 1 and 99: " + level);
    }

    private static long[] createExperienceTable() {
        long[] experience = new long[MAX_NORMAL_LEVEL];
        long[] wotlkXpPerLevel = {
            400L, 900L, 1400L, 2100L, 2800L, 3600L, 4500L, 5400L, 6500L, 7600L,
            8700L, 9800L, 11000L, 12300L, 13600L, 15000L, 16400L, 17800L, 19300L, 20800L,
            22400L, 24000L, 25500L, 27200L, 28900L, 30500L, 32200L, 33900L, 36300L, 38800L,
            41600L, 44600L, 48000L, 51400L, 55000L, 58700L, 62400L, 66200L, 70200L, 74300L,
            78500L, 82800L, 87100L, 91600L, 96300L, 101000L, 105800L, 110700L, 115700L, 120900L,
            126100L, 131500L, 137000L, 142500L, 148200L, 154000L, 159900L, 165800L, 172000L,
            290000L, 317000L, 349000L, 386000L, 428000L, 475000L, 527000L, 585000L, 648000L, 717000L,
            1523800L, 1539600L, 1555700L, 1571800L, 1587900L, 1604200L, 1620700L, 1637400L, 1653900L, 1670800L
        };

        long cumulative = 0L;
        experience[0] = 0L;
        for (int level = 1; level <= 79; level++) {
            cumulative = Math.addExact(cumulative, wotlkXpPerLevel[level - 1]);
            experience[level] = cumulative;
        }

        // Continue the WotLK 80-cap curve through level 99. The first
        // extension increment (80 -> 81) equals the WotLK 79 -> 80 increment.
        // From there the increment grows geometrically so that 98 -> 99 is
        // exactly 100 times the XP required for 80 -> 81.
        double continuationRatio = Math.pow(100.0D, 1.0D / 18.0D);
        double increment = wotlkXpPerLevel[79];
        for (int level = 81; level <= 99; level++) {
            if (level > 81) increment *= continuationRatio;
            cumulative = Math.addExact(cumulative, Math.round(increment));
            experience[level - 1] = cumulative;
        }
        return experience;
    }
}
