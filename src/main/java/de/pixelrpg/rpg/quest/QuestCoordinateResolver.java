package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.player.PlayerProfile;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;

import java.util.Locale;

/**
 * Resolves a quest's world target exactly once from the vanilla/Paper locate APIs.
 *
 * The resolved coordinates are persisted in PlayerProfile and are never recalculated
 * while the quest is active. No entity or locator bar is involved.
 */
public final class QuestCoordinateResolver {
    private static final int DEFAULT_STRUCTURE_RADIUS_CHUNKS = 64;
    private static final int DEFAULT_BIOME_RADIUS_BLOCKS = 10_000;

    private QuestCoordinateResolver() {
    }

    public static PlayerProfileTarget resolve(Player player, Quest quest) {
        if (player == null || quest == null || !quest.hasNavigationTarget()) return null;

        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (world == null) return null;

        if (quest.targetStructureKey() != null && !quest.targetStructureKey().isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(quest.targetStructureKey().trim().toLowerCase(Locale.ROOT));
            if (key == null) throw new IllegalArgumentException("Invalid structure key: " + quest.targetStructureKey());

            Structure structure = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.STRUCTURE)
                    .get(key);
            if (structure == null) throw new IllegalArgumentException("Unknown structure: " + quest.targetStructureKey());

            int radius = Math.max(1, quest.navigationRadius() > 0
                    ? quest.navigationRadius()
                    : DEFAULT_STRUCTURE_RADIUS_CHUNKS);
            var result = world.locateNearestStructure(origin, structure, radius, false);
            if (result == null) return null;

            Location location = result.getLocation();
            NamespacedKey resolvedKey = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.STRUCTURE)
                    .getKey(result.getStructure());
            return new PlayerProfileTarget(location, resolvedKey == null ? key.toString() : resolvedKey.toString());
        }

        if (!quest.targetBiomeKeys().isEmpty()) {
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
            var biomes = quest.targetBiomeKeys().stream()
                    .map(String::trim)
                    .filter(key -> !key.isBlank())
                    .map(key -> NamespacedKey.fromString(key.toLowerCase(Locale.ROOT)))
                    .filter(java.util.Objects::nonNull)
                    .map(registry::get)
                    .filter(java.util.Objects::nonNull)
                    .toArray(Biome[]::new);
            if (biomes.length == 0) return null;

            int radius = Math.max(1, quest.navigationRadius() > 0
                    ? quest.navigationRadius()
                    : DEFAULT_BIOME_RADIUS_BLOCKS);
            var result = world.locateNearestBiome(origin, radius, biomes);
            if (result == null) return null;

            NamespacedKey resolvedKey = registry.getKey(result.getBiome());
            return new PlayerProfileTarget(result.getLocation(), resolvedKey == null ? null : resolvedKey.toString());
        }

        Location fixed = quest.reachLocation();
        if (fixed == null || fixed.getWorld() == null) return null;
        return new PlayerProfileTarget(fixed, null);
    }

    public record PlayerProfileTarget(Location location, String targetKey) {
    }
}