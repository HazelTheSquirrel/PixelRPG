package de.pixelrpg.rpg.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private static final BigDecimal MAX_MINOR_UNITS = BigDecimal.valueOf(Long.MAX_VALUE);
    private Money() { }

    public static long fromMajor(double amount) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Money must be finite");
        if (amount <= 0.0D) return 0L;
        BigDecimal minorUnits = BigDecimal.valueOf(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
        return minorUnits.compareTo(MAX_MINOR_UNITS) >= 0 ? Long.MAX_VALUE : minorUnits.longValueExact();
    }

    public static double toMajor(long minorUnits) {
        if (minorUnits < 0L) throw new IllegalArgumentException("Money minor units must be non-negative");
        return BigDecimal.valueOf(minorUnits, 2).doubleValue();
    }
}
