package de.pixelrpg.rpg.core;
public final class Level {
    private Level() {}
    public static int fromExperience(long experience) { return Math.max(1, (int) Math.floor(Math.sqrt(Math.max(0, experience) / 100.0D)) + 1); }
}
