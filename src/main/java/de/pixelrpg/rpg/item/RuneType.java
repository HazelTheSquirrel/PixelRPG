// src/main/java/de/pixelrpg/rpg/item/RuneType.java (VOLLSTÄNDIG, ersetzt alte Datei — 12 statt 7 Runen)
package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum RuneType {

    DAMAGE(Component.text("Damage Rune", NamedTextColor.RED), ItemStatProfile.WEAPON, 1.0, 4.0),
    CRIT(Component.text("Crit Rune", NamedTextColor.LIGHT_PURPLE), ItemStatProfile.WEAPON, 0.5, 2.5),
    LIFESTEAL(Component.text("Lifesteal Rune", NamedTextColor.DARK_RED), ItemStatProfile.WEAPON, 1.0, 5.0),
    ARMOR(Component.text("Armor Rune", NamedTextColor.BLUE), ItemStatProfile.ARMOR, 1.0, 3.5),
    VITALITY(Component.text("Vitality Rune", NamedTextColor.GREEN), ItemStatProfile.ARMOR, 1.0, 4.0),
    REGENERATION(Component.text("Regeneration Rune", NamedTextColor.LIGHT_PURPLE), ItemStatProfile.ARMOR, 1.0, 3.0),
    HASTE(Component.text("Haste Rune", NamedTextColor.YELLOW), ItemStatProfile.TOOL, 1.0, 3.0),

    // Attributgebundene Runen: verstärken direkt eines der fünf Spielerattribute.
    AGILITY_ATTUNEMENT(Component.text("Agility Attunement", NamedTextColor.AQUA), ItemStatProfile.ARMOR, 0.5, 2.0),
    PRECISION_ATTUNEMENT(Component.text("Precision Attunement", NamedTextColor.GOLD), ItemStatProfile.ARMOR, 0.5, 2.0),
    RANGE_ATTUNEMENT(Component.text("Range Attunement", NamedTextColor.YELLOW), ItemStatProfile.ARMOR, 0.5, 2.0),
    TOUGHNESS_ATTUNEMENT(Component.text("Toughness Attunement", NamedTextColor.GRAY), ItemStatProfile.ARMOR, 0.5, 2.0),

    // Klassengebundene Runen: geben eine kleine, aber spürbare Zweitboni-Kombination.
    CLASS_SIGIL(Component.text("Class Sigil", NamedTextColor.DARK_PURPLE), ItemStatProfile.ARMOR, 1.0, 3.0);

    private final Component displayName;
    private final ItemStatProfile compatibleProfile;
    private final double minValue;
    private final double maxValue;

    RuneType(Component displayName, ItemStatProfile compatibleProfile, double minValue, double maxValue) {
        this.displayName = displayName;
        this.compatibleProfile = compatibleProfile;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public Component displayName() {
        return displayName;
    }

    public boolean isCompatible(ItemCategory category) {
        return category.getProfile() == compatibleProfile
                || (compatibleProfile == ItemStatProfile.ARMOR && category.getProfile() == ItemStatProfile.SHIELD);
    }

    public double rollValue() {
        return Math.round((minValue + Math.random() * (maxValue - minValue)) * 10.0) / 10.0;
    }
}