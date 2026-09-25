package de.pixelrpg.rpg.profession;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** The nine player professions defined by the PixelRPG RP progression model. */
public enum Profession {
    BLACKSMITH("Schmied", "Metall, Werkzeuge, Waffen und Rüstung", NamedTextColor.GRAY),
    SCHOLAR("Gelehrter", "Bücher, Wissen, Zaubertisch und Verzauberungen", NamedTextColor.AQUA),
    FARMER("Landwirt", "Pflanzen, Feldarbeit und landwirtschaftliche Erzeugnisse", NamedTextColor.GREEN),
    COOK("Koch", "Gekochte Mahlzeiten und hochwertige Nahrung", NamedTextColor.GOLD),
    TAILOR("Schneider", "Wolle, Leder, Betten und tragbare Ausrüstung", NamedTextColor.LIGHT_PURPLE),
    ALCHEMIST("Alchemist", "Vanilla-Tränke und alchemistische Verarbeitung", NamedTextColor.DARK_PURPLE),
    MASON("Steinmetz", "Stein, Ziegel und hochwertige Baublöcke", NamedTextColor.DARK_GRAY),
    FISHERMAN("Fischer", "Passiv: Angeln, Fangmenge und Vanilla-Schätze", NamedTextColor.BLUE),
    WOODCUTTER("Holzfäller", "Passiv: Holzfällen, Holzertrag und sicheres Baumfällen", NamedTextColor.DARK_GREEN);

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 60;

    /**
     * Compatibility alias for the historical tenth profession. Mountain mining is
     * now part of the blacksmith resource chain and is no longer a separate
     * profession in the RP progression model.
     */
    @Deprecated(forRemoval = true)
    public static final Profession MOUNTAIN_MINER = BLACKSMITH;

    private final String displayName;
    private final String description;
    private final NamedTextColor color;

    Profession(String displayName, String description, NamedTextColor color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public Component displayComponent() { return Component.text(displayName, color); }
}
