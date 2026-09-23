package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Location;

public record RPGNpc(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {
    public RPGNpc {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (name == null || name.isBlank()) name = "NPC";
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("location must have a world");
        location = location.clone();
    }
    public RPGNpc(String id, NpcType type, String name, Location location) { this(id,type,name,location,null,null); }
    public RPGNpc(String id, NpcType type, String name, Location location, String skinSource) { this(id,type,name,location,skinSource,null); }
    public boolean hasCustomSkin() { return skinSource != null && !skinSource.isBlank(); }
}
