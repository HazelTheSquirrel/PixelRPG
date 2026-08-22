package de.pixelrpg.rpg.companion;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the player's unlocked companions and their active companion entity state. */
public final class CompanionService {
    private static final String TEST_WOLF_ID = "test-wolf";
    private static final String TEST_WOLF_NAME = "PixelRPG Wolf";

    private final Plugin plugin;
    private final Map<UUID, List<Companion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();

    public CompanionService(Plugin plugin) {
        this.plugin = plugin;
    }

    public List<Companion> getCompanions(UUID playerId) {
        return List.copyOf(companions.getOrDefault(playerId, List.of()));
    }

    /** Adds the temporary wolf companion used for the current companion UI test. */
    public void ensureTestWolf(UUID playerId) {
        companions.compute(playerId, (ignored, current) -> {
            List<Companion> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (updated.stream().noneMatch(existing -> existing.id().equals(TEST_WOLF_ID))) {
                updated.add(new Companion(TEST_WOLF_ID, TEST_WOLF_NAME, 1, false));
            }
            return updated;
        });
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

    public boolean setActive(Player player, String companionId) {
        UUID playerId = player.getUniqueId();
        List<Companion> current = companions.get(playerId);
        if (current == null || current.stream().noneMatch(companion -> companion.id().equals(companionId))) {
            return false;
        }

        clearActiveEntity(player);
        companions.put(playerId, current.stream()
                .map(companion -> companion.withActive(companion.id().equals(companionId)))
                .toList());

        if (companionId.equals(TEST_WOLF_ID)) {
            Location spawnLocation = player.getLocation().clone().add(1.0, 0.0, 1.0);
            Wolf wolf = player.getWorld().spawn(spawnLocation, Wolf.class, spawned -> {
                spawned.setTamed(true);
                spawned.setOwner(player);
                spawned.setAdult();
                spawned.setCustomNameVisible(true);
                spawned.setCustomName(companionName(playerId, companionId));
            });
            activeEntities.put(playerId, wolf.getUniqueId());
        }
        return true;
    }

    /** Compatibility overload for callers that only have the player UUID. */
    public boolean setActive(UUID playerId, String companionId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return false;
        return setActive(player, companionId);
    }

    public void clearActive(Player player) {
        clearActiveEntity(player);
        UUID playerId = player.getUniqueId();
        List<Companion> current = companions.get(playerId);
        if (current == null) return;
        companions.put(playerId, current.stream().map(companion -> companion.withActive(false)).toList());
    }

    public void clearActive(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) clearActive(player);
        else {
            UUID entityId = activeEntities.remove(playerId);
            if (entityId != null) {
                plugin.getServer().getEntity(entityId).remove();
            }
            List<Companion> current = companions.get(playerId);
            if (current != null) companions.put(playerId, current.stream().map(companion -> companion.withActive(false)).toList());
        }
    }

    public Companion getActive(UUID playerId) {
        return getCompanions(playerId).stream().filter(Companion::active).findFirst().orElse(null);
    }

    public boolean rename(UUID playerId, String companionId, String newName) {
        String cleaned = newName == null ? "" : newName.strip();
        if (cleaned.isEmpty() || cleaned.length() > 24) return false;
        List<Companion> current = companions.get(playerId);
        if (current == null) return false;
        boolean exists = current.stream().anyMatch(companion -> companion.id().equals(companionId));
        if (!exists) return false;
        companions.put(playerId, current.stream()
                .map(companion -> companion.id().equals(companionId)
                        ? new Companion(companion.id(), cleaned, companion.level(), companion.active())
                        : companion)
                .toList());

        UUID entityId = activeEntities.get(playerId);
        if (entityId != null) {
            var entity = plugin.getServer().getEntity(entityId);
            if (entity instanceof Wolf wolf) wolf.setCustomName(cleaned);
        }
        return true;
    }

    public void shutdown() {
        for (UUID entityId : activeEntities.values()) {
            var entity = plugin.getServer().getEntity(entityId);
            if (entity != null) entity.remove();
        }
        activeEntities.clear();
    }

    private void clearActiveEntity(Player player) {
        UUID entityId = activeEntities.remove(player.getUniqueId());
        if (entityId == null) return;
        var entity = plugin.getServer().getEntity(entityId);
        if (entity != null) entity.remove();
    }

    private String companionName(UUID playerId, String companionId) {
        return getCompanions(playerId).stream()
                .filter(companion -> companion.id().equals(companionId))
                .map(Companion::name)
                .findFirst()
                .orElse(TEST_WOLF_NAME);
    }
}
