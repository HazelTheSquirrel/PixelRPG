// src/main/java/de/pixelrpg/rpg/region/RegionManager.java
package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.combat.scaling.RegionDangerProvider;
import de.pixelrpg.rpg.core.Rank;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RegionManager implements RegionDangerProvider {

    private static final Region WILDERNESS_FALLBACK =
            new Region("wilderness", "Wilderness", RegionCategory.WILDERNESS);

    private final Plugin plugin;
    private final RegionRepository repository;
    private final Map<String, Region> regionsById = new ConcurrentHashMap<>();

    public RegionManager(Plugin plugin, RegionRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void load() {
        try {
            repository.init();
            regionsById.clear();
            for (Region region : repository.loadAll()) {
                regionsById.put(region.getId(), region);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load regions.yml", e);
        }
    }

    public void save() {
        try {
            repository.saveAll(regionsById.values());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save regions.yml", e);
        }
    }

    public Optional<Region> getRegion(String id) {
        return Optional.ofNullable(regionsById.get(id));
    }

    public List<Region> getAllRegions() {
        return new ArrayList<>(regionsById.values());
    }

    public Region createRegion(String id, String displayName, RegionCategory category) {
        Region region = new Region(id, displayName, category);
        regionsById.put(id, region);
        save();
        return region;
    }

    public boolean deleteRegion(String id) {
        boolean removed = regionsById.remove(id) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public List<Region> getRegionsAt(Location location) {
        List<Region> matches = new ArrayList<>();
        for (Region region : regionsById.values()) {
            if (region.contains(location)) {
                matches.add(region);
            }
        }
        return matches;
    }

    public Region resolveTopRegion(Location location) {
        List<Region> matches = getRegionsAt(location);
        if (matches.isEmpty()) {
            return WILDERNESS_FALLBACK;
        }

        return matches.stream()
                .max(Comparator.<Region>comparingInt(Region::getPriority)
                        .thenComparing(r -> -r.getTotalVolume()))
                .orElse(WILDERNESS_FALLBACK);
    }

    @Override
    public Rank getMinRank(Location location) {
        return resolveTopRegion(location).getMinRank();
    }

    @Override
    public Rank getMaxRank(Location location) {
        return resolveTopRegion(location).getMaxRank();
    }
}