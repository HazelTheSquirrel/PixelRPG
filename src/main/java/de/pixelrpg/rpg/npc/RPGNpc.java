package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;
import java.util.UUID;
import org.bukkit.Location;

public record RPGNpc(String id, NpcType type, String name, Location location, String skinSource, Profession profession, UUID kingdomId, ProfessionNpcRank professionNpcRank) {

    public RPGNpc(String id, NpcType type, String name, Location location) {
        this(id, type, name, location, null, null, null, ProfessionNpcRank.APPRENTICE);
    }

    public RPGNpc(String id, NpcType type, String name, Location location, String skinSource) {
        this(id, type, name, location, skinSource, null, null, ProfessionNpcRank.APPRENTICE);
    }

    public RPGNpc(String id, NpcType type, String name, Location location, String skinSource, Profession profession) { this(id,type,name,location,skinSource,profession,null,ProfessionNpcRank.APPRENTICE); }

    public boolean hasCustomSkin() {
        return skinSource != null && !skinSource.isBlank();
    }
}
