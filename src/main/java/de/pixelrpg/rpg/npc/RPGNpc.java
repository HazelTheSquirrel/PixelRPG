// src/main/java/de/pixelrpg/rpg/npc/RPGNpc.java (VOLLSTÄNDIG, ersetzt alte Datei — skinSource ergänzt)
package de.pixelrpg.rpg.npc;

import org.bukkit.Location;

public record RPGNpc(String id, NpcType type, String name, Location location, String skinSource) {

    public RPGNpc(String id, NpcType type, String name, Location location) {
        this(id, type, name, location, null);
    }

    public boolean hasCustomSkin() {
        return skinSource != null && !skinSource.isBlank();
    }
}