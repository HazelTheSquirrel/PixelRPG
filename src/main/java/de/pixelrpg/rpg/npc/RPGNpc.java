package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Location;

public record RPGNpc(String id, NpcType type, String name, Location location, String skinSource, Profession profession) {

    public RPGNpc(String id, NpcType type, String name, Location location) {
        this(id, type, name, location, null, null);
    }

    public RPGNpc(String id, NpcType type, String name, Location location, String skinSource) {
        this(id, type, name, location, skinSource, null);
    }

    public boolean hasCustomSkin() {
        return skinSource != null && !skinSource.isBlank();
    }

    public boolean isProfessionTrainer() {
        return type == NpcType.PROFESSION_TRAINER && profession != null;
    }
}
