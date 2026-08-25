// src/main/java/de/pixelrpg/rpg/boss/BossAttackPatternRegistry.java
package de.pixelrpg.rpg.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BossAttackPatternRegistry {

    private final Map<String, BossAttackPattern> patternsById = new HashMap<>();

    public void register(BossAttackPattern pattern) {
        patternsById.put(pattern.id(), pattern);
    }

    public Optional<BossAttackPattern> get(String id) {
        return Optional.ofNullable(patternsById.get(id));
    }
}