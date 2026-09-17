package de.pixelrpg.rpg.item;

import java.util.Locale;
import java.util.Objects;

/** Immutable, data-driven definition of one consumable PixelRPG food item. */
public record FoodDefinition(
        String id,
        int nutrition,
        float saturation,
        boolean canAlwaysEat,
        String effectType,
        int effectDurationSeconds,
        int effectAmplifier
) {
    public FoodDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effectType, "effectType");
        id = normalize(id);
        effectType = effectType.trim().toUpperCase(Locale.ROOT);
        if (nutrition < 0 || nutrition > 20) throw new IllegalArgumentException("nutrition must be between 0 and 20");
        if (saturation < 0.0F) throw new IllegalArgumentException("saturation must not be negative");
        if (effectDurationSeconds < 0) throw new IllegalArgumentException("effectDurationSeconds must not be negative");
        if (effectAmplifier < 0) throw new IllegalArgumentException("effectAmplifier must not be negative");
        if (effectType.isBlank() && (effectDurationSeconds != 0 || effectAmplifier != 0)) {
            throw new IllegalArgumentException("Effect duration/amplifier requires an effect type");
        }
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("pixelrpg:") ? normalized : "pixelrpg:" + normalized;
    }
}
