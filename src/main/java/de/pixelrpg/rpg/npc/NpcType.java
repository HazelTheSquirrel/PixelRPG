package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.format.NamedTextColor;

public enum NpcType {

    RECEPTION(NamedTextColor.AQUA),
    BLACKSMITH(NamedTextColor.RED),
    PROFESSION_TRAINER(NamedTextColor.BLUE),
    QUEST(NamedTextColor.YELLOW),
    SHOP(NamedTextColor.GREEN),
    TRAVEL(NamedTextColor.LIGHT_PURPLE),
    FILLER(NamedTextColor.WHITE),
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
