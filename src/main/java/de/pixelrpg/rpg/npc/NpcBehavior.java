// src/main/java/de/pixelrpg/rpg/npc/NpcBehavior.java
package de.pixelrpg.rpg.npc;

import org.bukkit.entity.Player;

public interface NpcBehavior {

    NpcType type();

    void onInteract(Player player, RPGNpc npc);
}