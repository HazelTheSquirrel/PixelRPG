// src/main/java/de/pixelrpg/rpg/item/CurseType.java
package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.potion.PotionEffectType;

public enum CurseType {

    WEAKNESS("Weakness", NamedTextColor.DARK_RED, PotionEffectType.WEAKNESS, 0),
    CLUMSINESS("Clumsiness", NamedTextColor.DARK_GRAY, PotionEffectType.SLOWNESS, 0),
    MISFORTUNE("Misfortune", NamedTextColor.DARK_PURPLE, PotionEffectType.UNLUCK, 0);

    private final String shortLabel;
    private final NamedTextColor color;
    private final PotionEffectType effectType;
    private final int amplifier;

    CurseType(String shortLabel, NamedTextColor color, PotionEffectType effectType, int amplifier) {
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

    public static CurseType rollRandom() {
        return values()[(int) (Math.random() * values().length)];
    }
}