// src/main/java/de/pixelrpg/rpg/npc/NpcBehavior.java
package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueContext;
import org.bukkit.entity.Player;

public interface NpcBehavior {

    NpcType type();

    default void onInteract(Player player, RPGNpc npc) {
        onInteract(DialogueContext.forNpc(player, npc));
    }

    default void onInteract(DialogueContext context) {
        onInteract(context.player(), context.npc());
    }
}