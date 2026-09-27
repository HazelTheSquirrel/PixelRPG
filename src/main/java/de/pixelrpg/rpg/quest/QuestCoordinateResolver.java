package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.player.PlayerProfile;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;

import java.util.Locale;

/**
 * Resolves a quest structure once through Paper's vanilla locate API.
 *
 * The resolved X/Z coordinates are persisted while the quest is active.
 * There is no navigation UI, navigation bar, or live target tracking.
 */
public final class QuestCoordinateResolver {
    private static final int DEFAULT_STRUCTURE_RADIUS_CHUNKS = 64;

    private QuestCoordinateResolver() {
    }

    public static PlayerProfileTarget resolve(Player player, Quest quest) {
        if (player == null || quest == null || !quest.hasLocateTarget()) return null;

        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (world == null) return null;

        NamespacedKey key = NamespacedKey.fromString(quest.targetStructureKey().trim().toLowerCase(Locale.ROOT));
        if (key == null) throw new IllegalArgumentException("Invalid structure key: " + quest.targetStructureKey());

        Structure structure = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.STRUCTURE)
                .get(key);
        if (structure == null) throw new IllegalArgumentException("Unknown structure: " + quest.targetStructureKey());

        int radius = Math.max(1, quest.locateRadius() > 0
                ? quest.locateRadius()
                : DEFAULT_STRUCTURE_RADIUS_CHUNKS);
        var result = world.locateNearestStructure(origin, structure, radius, false);
        if (result == null) return null;

        Location located = result.getLocation();
        int targetX = located.getBlockX();
        int targetZ = located.getBlockZ();

        int surfaceY = world.getHighestBlockYAt(
                targetX,
                targetZ,
                HeightMap.MOTION_BLOCKING_NO_LEAVES
        ) + 1;
        Location location = new Location(world, targetX + 0.5D, surfaceY, targetZ + 0.5D);

        NamespacedKey resolvedKey = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.STRUCTURE)
                .getKey(result.getStructure());

        return new PlayerProfileTarget(
                location,
                resolvedKey == null ? key.toString() : resolvedKey.toString()
        );
    }

    public record PlayerProfileTarget(Location location, String targetKey) {
    }
}
