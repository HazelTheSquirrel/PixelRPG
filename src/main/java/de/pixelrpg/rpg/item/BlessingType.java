// src/main/java/de/pixelrpg/rpg/item/BlessingType.java
package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.potion.PotionEffectType;

public enum BlessingType {

    SWIFTNESS("Swiftness", NamedTextColor.AQUA, PotionEffectType.SPEED, 0),
    VITALITY_AURA("Vitality", NamedTextColor.GREEN, PotionEffectType.REGENERATION, 0),
    FORTUNE("Fortune", NamedTextColor.GOLD, PotionEffectType.LUCK, 0);

    private final String shortLabel;
    private final NamedTextColor color;
    private final PotionEffectType effectType;
    private final int amplifier;

    BlessingType(String shortLabel, NamedTextColor color, PotionEffectType effectType, int amplifier) {
        this.shortLabel = shortLabel;
        this.color = color;
        this.effectType = effectType;
        this.amplifier = amplifier;
    }

    public Component displayName() {
        return Component.text(shortLabel, color);
    }

    public PotionEffectType getEffectType() {
        return effectType;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public static BlessingType rollRandom() {
        return values()[(int) (Math.random() * values().length)];
    }
}