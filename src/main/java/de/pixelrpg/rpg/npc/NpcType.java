// src/main/java/de/pixelrpg/rpg/npc/NpcType.java (VOLLSTÄNDIG, ersetzt alte Datei — BANKER ergänzt)
package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.format.NamedTextColor;

public enum NpcType {

    RECEPTION(NamedTextColor.AQUA),
    BLACKSMITH(NamedTextColor.RED),
    QUEST(NamedTextColor.YELLOW),
    SHOP(NamedTextColor.GREEN),
    TRAVEL(NamedTextColor.LIGHT_PURPLE),
    STORY(NamedTextColor.GOLD),
    BANKER(NamedTextColor.DARK_GREEN);

    private final NamedTextColor color;

    NpcType(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor getColor() {
        return color;
    }
}