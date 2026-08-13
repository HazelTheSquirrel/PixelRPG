// src/main/java/de/pixelrpg/rpg/item/RuneType.java (VOLLSTÄNDIG, ersetzt alte Datei — nur Enum-Definition, unverändert, zur Vollständigkeit der Übersicht)
package de.pixelrpg.rpg.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public enum RuneType {

    DAMAGE(Component.text("Damage Rune", NamedTextColor.RED), ItemStatProfile.WEAPON, 1.0, 4.0),
    CRIT(Component.text("Crit Rune", NamedTextColor.LIGHT_PURPLE), ItemStatProfile.WEAPON, 0.5, 2.5),
    LIFESTEAL(Component.text("Lifesteal Rune", NamedTextColor.DARK_RED), ItemStatProfile.WEAPON, 1.0, 5.0),
    ARMOR(Component.text("Armor Rune", NamedTextColor.BLUE), ItemStatProfile.ARMOR, 1.0, 3.5),
    VITALITY(Component.text("Vitality Rune", NamedTextColor.GREEN), ItemStatProfile.ARMOR, 1.0, 4.0),
    REGENERATION(Component.text("Regeneration Rune", NamedTextColor.LIGHT_PURPLE), ItemStatProfile.ARMOR, 1.0, 3.0),
    HASTE(Component.text("Haste Rune", NamedTextColor.YELLOW), ItemStatProfile.TOOL, 1.0, 3.0),

    AGILITY_ATTUNEMENT(Component.text("Agility Attunement", NamedTextColor.AQUA), ItemStatProfile.ARMOR, 0.5, 2.0),
    PRECISION_ATTUNEMENT(Component.text("Precision Attunement", NamedTextColor.GOLD), ItemStatProfile.ARMOR, 0.5, 2.0),
    RANGE_ATTUNEMENT(Component.text("Range Attunement", NamedTextColor.YELLOW), ItemStatProfile.ARMOR, 0.5, 2.0),
    TOUGHNESS_ATTUNEMENT(Component.text("Toughness Attunement", NamedTextColor.GRAY), ItemStatProfile.ARMOR, 0.5, 2.0),

    CLASS_SIGIL(Component.text("Class Sigil", NamedTextColor.DARK_PURPLE), ItemStatProfile.ARMOR, 1.0, 3.0);

    private final Component displayName;
    private final ItemStatProfile compatibleProfile;
    private double minValue;
    private double maxValue;

    RuneType(Component displayName, ItemStatProfile compatibleProfile, double minValue, double maxValue) {
        this.displayName = displayName;
        this.compatibleProfile = compatibleProfile;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    private static final Map<RuneType, String> CONFIG_KEYS = new EnumMap<>(RuneType.class);

    static {
        CONFIG_KEYS.put(DAMAGE, "damage");
        CONFIG_KEYS.put(CRIT, "crit");
        CONFIG_KEYS.put(LIFESTEAL, "lifesteal");
        CONFIG_KEYS.put(ARMOR, "armor");
        CONFIG_KEYS.put(VITALITY, "vitality");
        CONFIG_KEYS.put(REGENERATION, "regeneration");
        CONFIG_KEYS.put(HASTE, "haste");
        CONFIG_KEYS.put(AGILITY_ATTUNEMENT, "agility-attunement");
        CONFIG_KEYS.put(PRECISION_ATTUNEMENT, "precision-attunement");
        CONFIG_KEYS.put(RANGE_ATTUNEMENT, "range-attunement");
        CONFIG_KEYS.put(TOUGHNESS_ATTUNEMENT, "toughness-attunement");
        CONFIG_KEYS.put(CLASS_SIGIL, "class-sigil");
    }

    public static void load(FileConfiguration config) {
        for (RuneType type : values()) {
            String key = CONFIG_KEYS.get(type);
            if (key == null || !config.contains("runes." + key)) {
                continue;
            }
            type.minValue = config.getDouble("runes." + key + ".min", type.minValue);
            type.maxValue = config.getDouble("runes." + key + ".max", type.maxValue);
        }
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