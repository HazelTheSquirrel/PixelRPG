package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.region.RegionPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Persistent ordered boundary markers belonging to one guild territory. */
public record GuildTerritory(UUID guildId, UUID regionId, List<Marker> markers) {
    public static final int INITIAL_MARKERS = 4;
    public static final double MAX_MARKER_DISTANCE = 32.0D;

    public GuildTerritory {
        Objects.requireNonNull(guildId, "guildId");
        Objects.requireNonNull(regionId, "regionId");
        markers = List.copyOf(Objects.requireNonNull(markers, "markers"));
    }

    public List<RegionPoint> points() {
        return markers.stream().map(marker -> new RegionPoint(marker.x(), marker.z())).toList();
    }

    public record Marker(UUID id, String worldName, int x, int y, int z) {
        public Marker {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(worldName, "worldName");
            if (worldName.isBlank()) throw new IllegalArgumentException("worldName must not be blank");
        }
    }
}
