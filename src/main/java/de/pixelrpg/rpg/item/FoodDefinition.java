package de.pixelrpg.rpg.item;

import java.util.Locale;
import java.util.Objects;

public record FoodDefinition(String id, int nutrition, float saturation, boolean canAlwaysEat,
                             String effectType, int effectDurationSeconds, int effectAmplifier) {
    public FoodDefinition {
        Objects.requireNonNull(id);
        effectType = effectType == null ? "" : effectType.trim().toUpperCase(Locale.ROOT);
        id = normalize(id);
        if (nutrition < 0 || nutrition > 20) throw new IllegalArgumentException("nutrition must be between 0 and 20");
        if (saturation < 0.0F || !Float.isFinite(saturation)) throw new IllegalArgumentException("invalid saturation");
        if (effectDurationSeconds < 0 || effectAmplifier < 0) throw new IllegalArgumentException("invalid effect values");
    }
    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
