package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns region enter/leave state and presentation decisions outside the Paper event adapter. */
public final class RegionTransitionService {
    private final RegionManager regions;
    private final Map<UUID, UUID> currentRegions = new HashMap<>();

    public RegionTransitionService(RegionManager regions) {
        this.regions = Objects.requireNonNull(regions);
    }

    public void update(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) return;
        UUID playerId = player.getUniqueId();
        UUID oldId = currentRegions.get(playerId);
        UUID newId = regions.find(location).map(PixelRegion::id).orElse(null);
        if (Objects.equals(oldId, newId)) return;
        if (oldId != null) regions.get(oldId).ifPresent(region -> showRegionTitle(player, region.name(), region.leaveMessage(), false));
        if (newId != null) regions.get(newId).ifPresent(region -> showRegionTitle(player, region.name(), region.enterMessage(), true));
        if (newId == null) currentRegions.remove(playerId);
        else currentRegions.put(playerId, newId);
    }

    public void clear(UUID playerId) {
        if (playerId != null) currentRegions.remove(playerId);
    }

    public void clearAll() {
        currentRegions.clear();
    }

    private static void showRegionTitle(Player player, String regionName, String message, boolean entering) {
        String title = message == null || message.isBlank() ? (entering ? regionName : "Verlassen") : message;
        String subtitle = message == null || message.isBlank() ? (entering ? "" : regionName) : regionName;
        player.showTitle(Title.title(Component.text(title), Component.text(subtitle)));
    }
}
