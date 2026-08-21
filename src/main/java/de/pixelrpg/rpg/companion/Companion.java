package de.pixelrpg.rpg.companion;

import java.util.Objects;

/** Immutable player companion definition used by the companion subsystem. */
public record Companion(String id, String name, int level, boolean active) {
    public Companion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
    }

    public Companion withActive(boolean value) {
        return new Companion(id, name, level, value);
    }
}
