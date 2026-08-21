package de.pixelrpg.rpg.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the player's unlocked companions and their active companion state. */
public final class CompanionService {
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();

    public List<Companion> getCompanions(UUID playerId) {
        return List.copyOf(companions.getOrDefault(playerId, List.of()));
    }

    public void unlock(UUID playerId, Companion companion) {
        companions.compute(playerId, (ignored, current) -> {
            List<Companion> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (updated.stream().noneMatch(existing -> existing.id().equals(companion.id()))) {
                updated.add(companion);
            }
            return updated;
        });
    }

    public boolean setActive(UUID playerId, String companionId) {
        List<Companion> current = companions.get(playerId);
        if (current == null || current.stream().noneMatch(companion -> companion.id().equals(companionId))) {
            return false;
        }
        companions.put(playerId, current.stream()
                .map(companion -> companion.withActive(companion.id().equals(companionId)))
                .toList());
        return true;
    }

    public void clearActive(UUID playerId) {
        List<Companion> current = companions.get(playerId);
        if (current == null) return;
        companions.put(playerId, current.stream().map(companion -> companion.withActive(false)).toList());
    }

    public Companion getActive(UUID playerId) {
        return getCompanions(playerId).stream().filter(Companion::active).findFirst().orElse(null);
    }
}
