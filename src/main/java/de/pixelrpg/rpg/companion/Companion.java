package de.pixelrpg.rpg.companion;

import java.util.Objects;

/** Immutable runtime view of a player-owned companion. */
public record Companion(String id,String name,int level,long experience,CompanionRarity rarity,org.bukkit.entity.EntityType entityType,boolean active){
    public Companion{Objects.requireNonNull(id);Objects.requireNonNull(name);Objects.requireNonNull(rarity);Objects.requireNonNull(entityType);if(level<1)throw new IllegalArgumentException("level must be at least 1");if(experience<0)throw new IllegalArgumentException("experience must not be negative");}
}
