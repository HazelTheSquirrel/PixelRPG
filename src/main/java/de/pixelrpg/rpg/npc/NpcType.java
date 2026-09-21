package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.format.NamedTextColor;

public enum NpcType {

    RECEPTION(NamedTextColor.AQUA),
    PROFESSION_BLACKSMITH(NamedTextColor.GRAY),
    PROFESSION_SCHOLAR(NamedTextColor.AQUA),
    PROFESSION_FARMER(NamedTextColor.GREEN),
    PROFESSION_COOK(NamedTextColor.GOLD),
    PROFESSION_TAILOR(NamedTextColor.LIGHT_PURPLE),
    PROFESSION_ALCHEMIST(NamedTextColor.DARK_PURPLE),
    PROFESSION_MASON(NamedTextColor.DARK_GRAY),
    PROFESSION_FISHERMAN(NamedTextColor.BLUE),
    PROFESSION_WOODCUTTER(NamedTextColor.DARK_GREEN),
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
