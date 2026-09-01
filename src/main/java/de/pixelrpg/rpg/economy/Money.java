package de.pixelrpg.rpg.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact two-decimal currency conversion at the persistence boundary. */
public final class Money {
    private Money() {
    }

    public static long fromMajor(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException("Money must be finite and non-negative");
        }
        return BigDecimal.valueOf(amount)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static double toMajor(long minorUnits) {
        if (minorUnits < 0L) throw new IllegalArgumentException("Money minor units must be non-negative");
        return BigDecimal.valueOf(minorUnits, 2).doubleValue();
    }
}
