package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Location;

public record RPGNpc(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
    public RPGNpc {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("NPC id must not be blank");
        if (type == null) throw new IllegalArgumentException("NPC type must not be null");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("NPC name must not be blank");
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("NPC location must have a world");
        location = location.clone();
    }
    public boolean hasCustomSkin() { return skinSource != null && !skinSource.isBlank(); }
}
