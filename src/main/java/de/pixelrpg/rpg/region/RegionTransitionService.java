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
        PixelRegion oldRegion = oldId == null ? null : regions.get(oldId).orElse(null);
        PixelRegion newRegion = newId == null ? null : regions.get(newId).orElse(null);
        showTransition(player, oldRegion, newRegion);
        if (newId == null) currentRegions.remove(playerId);
        else currentRegions.put(playerId, newId);
    }

    public void clear(UUID playerId) {
        if (playerId != null) currentRegions.remove(playerId);
    }

    public void clearAll() {
        currentRegions.clear();
    }

    private static void showTransition(Player player, PixelRegion oldRegion, PixelRegion newRegion) {
        String leave = oldRegion == null ? "" : oldRegion.leaveMessage();
        String enter = newRegion == null ? "" : newRegion.enterMessage();
        if (leave.isBlank() && enter.isBlank()) return;

        Component title = Component.text(enter.isBlank() ? leave : enter);
        Component subtitle = enter.isBlank() || leave.isBlank() ? Component.empty() : Component.text(leave);
        player.showTitle(Title.title(title, subtitle));
    }
}
