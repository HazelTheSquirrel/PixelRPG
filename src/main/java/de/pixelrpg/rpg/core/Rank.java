// src/main/java/de/pixelrpg/rpg/core/Rank.java
package de.pixelrpg.rpg.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum Rank {

    F(0L, NamedTextColor.GRAY),
    E(500L, NamedTextColor.WHITE),
    D(1_500L, NamedTextColor.GREEN),
    C(3_500L, NamedTextColor.AQUA),
    B(7_000L, NamedTextColor.BLUE),
    A(13_000L, NamedTextColor.LIGHT_PURPLE),
    S(24_000L, NamedTextColor.GOLD);

    private final long requiredExp;
    private final NamedTextColor color;

    Rank(long requiredExp, NamedTextColor color) {
        this.requiredExp = requiredExp;
        this.color = color;
    }

    public long getRequiredExp() {
        return requiredExp;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Component displayName() {
        return Component.text(name(), color);
    }

    public Rank next() {
        Rank[] values = values();
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal < values.length ? values[nextOrdinal] : this;
    }

    public boolean isMax() {
        return this == S;
    }

    public boolean isAtLeast(Rank other) {
        return this.ordinal() >= other.ordinal();
    }

    public static Rank fromExperience(long totalExp) {
        Rank result = F;
        for (Rank rank : values()) {
            if (totalExp >= rank.requiredExp) {
                result = rank;
            }
        }
        return result;
    }

    public static Rank fromOrdinalClamped(int ordinal) {
        Rank[] values = values();
        int clamped = Math.max(0, Math.min(ordinal, values.length - 1));
        return values[clamped];
    }
}