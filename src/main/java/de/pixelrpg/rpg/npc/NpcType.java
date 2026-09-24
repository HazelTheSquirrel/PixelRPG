package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.EnumSet;
import java.util.Set;

public enum NpcType {
    RECEPTION(NamedTextColor.AQUA, NpcFunction.RECEPTION),
    PROFESSION_BLACKSMITH(NamedTextColor.GRAY, NpcFunction.PROFESSION),
    PROFESSION_SCHOLAR(NamedTextColor.AQUA, NpcFunction.PROFESSION),
    PROFESSION_FARMER(NamedTextColor.GREEN, NpcFunction.PROFESSION),
    PROFESSION_COOK(NamedTextColor.GOLD, NpcFunction.PROFESSION),
    PROFESSION_TAILOR(NamedTextColor.LIGHT_PURPLE, NpcFunction.PROFESSION),
    PROFESSION_ALCHEMIST(NamedTextColor.DARK_PURPLE, NpcFunction.PROFESSION),
    PROFESSION_MASON(NamedTextColor.DARK_GRAY, NpcFunction.PROFESSION),
    PROFESSION_FISHERMAN(NamedTextColor.BLUE, NpcFunction.PROFESSION),
    PROFESSION_WOODCUTTER(NamedTextColor.DARK_GREEN, NpcFunction.PROFESSION),
    QUEST(NamedTextColor.YELLOW, NpcFunction.QUEST),
    SHOP(NamedTextColor.GREEN, NpcFunction.SHOP),
    TRAVEL(NamedTextColor.LIGHT_PURPLE, NpcFunction.TRAVEL),
    FILLER(NamedTextColor.WHITE),
    STORY(NamedTextColor.GOLD, NpcFunction.STORY),
    BANKER(NamedTextColor.DARK_GREEN, NpcFunction.BANK);

    private final NamedTextColor color;
    private final Set<NpcFunction> functions;

    NpcType(NamedTextColor color, NpcFunction... functions) {
        this.color = color;
        this.functions = functions.length == 0 ? Set.of() : Set.copyOf(EnumSet.of(functions[0], functions));
    }

    public NamedTextColor color() { return color; }
    public NamedTextColor getColor() { return color; }
    public Set<NpcFunction> functions() { return functions; }
    public boolean supports(NpcFunction function) { return functions.contains(function); }
}
