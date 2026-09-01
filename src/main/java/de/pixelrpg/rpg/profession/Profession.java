package de.pixelrpg.rpg.profession;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** The nine optional professions available to every PixelRPG player. */
public enum Profession {
    BLACKSMITH("Schmied", "Waffen, Rüstung, Werkzeuge und Metallverarbeitung", NamedTextColor.GRAY),
    SCHOLAR("Gelehrter", "Bücher, Karten, Verzauberungen und Wissensgegenstände", NamedTextColor.AQUA),
    FARMER("Landwirt", "Felder, Pflanzen und landwirtschaftliche Rohstoffe", NamedTextColor.GREEN),
    COOK("Koch", "Nahrung, Mahlzeiten und verarbeitete Lebensmittel", NamedTextColor.GOLD),
    TAILOR("Schneider", "Wolle, Leder, Stoffe und textile Verarbeitung", NamedTextColor.LIGHT_PURPLE),
    ALCHEMIST("Alchemist", "Tränke, Reagenzien und pflanzliche Wirkstoffe", NamedTextColor.DARK_PURPLE),
    MASON("Steinmetz", "Stein, Ziegel und die Verarbeitung von Baublöcken", NamedTextColor.DARK_GRAY),
    FISHERMAN("Fischer", "Angeln und Fisch als Rohstoff für die Küche", NamedTextColor.BLUE),
    WOODCUTTER("Holzfäller", "Holz, Stämme und die Verarbeitung von Holz", NamedTextColor.DARK_GREEN);

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;

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
