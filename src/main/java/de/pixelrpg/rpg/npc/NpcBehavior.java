package de.pixelrpg.rpg.npc;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface NpcBehavior {

    NpcType type();

    void onInteract(Player player, RPGNpc npc);

    /**
     * Opens the functional dialog as a child of another NPC dialog.
     * Existing behaviors remain compatible by defaulting to their normal interaction.
     */
    default void onInteract(Player player, RPGNpc npc, Consumer<Player> backAction) {
        onInteract(player, npc);
    }
}
