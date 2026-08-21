package de.pixelrpg.rpg.profession;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** The four broad professions used by PixelRPG. */
public enum Profession {
    BLACKSMITH("Schmied", "Waffen, Rüstung, Werkzeuge und Metallverarbeitung", NamedTextColor.GRAY),
    PROVISIONER("Versorger", "Nahrung, Fischerei, Landwirtschaft und Tierprodukte", NamedTextColor.GREEN),
    ALCHEMIST("Alchemist", "Tränke, Reagenzien und pflanzliche Wirkstoffe", NamedTextColor.LIGHT_PURPLE),
    SCHOLAR("Gelehrter", "Bücher, Karten, Verzauberungen und Wissensgegenstände", NamedTextColor.AQUA);

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 99;

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
