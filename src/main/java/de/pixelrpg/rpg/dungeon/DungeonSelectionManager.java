// src/main/java/de/pixelrpg/rpg/dungeon/DungeonSelectionManager.java
package de.pixelrpg.rpg.dungeon;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonSelectionManager {

    public static final class Selection {
        private Location pos1;
        private Location pos2;

        public Location getPos1() {
            return pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null
                    && pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld());
        }
    }

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public Selection getOrCreate(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new Selection());
    }

    public void setPos1(UUID uuid, Location location) {
        getOrCreate(uuid).pos1 = location;
    }

    public void setPos2(UUID uuid, Location location) {
        getOrCreate(uuid).pos2 = location;
    }

    public Selection get(UUID uuid) {
        return selections.get(uuid);
    }

    public void clear(UUID uuid) {
        selections.remove(uuid);
    }
}