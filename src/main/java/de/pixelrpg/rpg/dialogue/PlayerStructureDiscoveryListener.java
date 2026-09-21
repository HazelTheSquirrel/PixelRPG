package de.pixelrpg.rpg.dialogue;

import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PlayerStructureDiscoveryListener implements Listener {
    private final WorldState worldState;
    private final PlayerKnowledgeStore knowledgeStore;

    public PlayerStructureDiscoveryListener(WorldState worldState, PlayerKnowledgeStore knowledgeStore) {
        this.worldState = Objects.requireNonNull(worldState, "worldState");
        this.knowledgeStore = Objects.requireNonNull(knowledgeStore, "knowledgeStore");
    }

    /** Discovers generated structures only when a player enters a new chunk. */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !event.hasChangedBlock()) return;
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        World world = event.getPlayer().getWorld();
        for (GeneratedStructure structure : world.getStructures(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ())) {
            NamespacedKey key = RegistryAccess.registryAccess()
                    .getRegistry(io.papermc.paper.registry.RegistryKey.STRUCTURE)
                    .getKey(structure.getStructure());
            if (key == null || !key.getNamespace().equals(NamespacedKey.MINECRAFT)) continue;
            String id = key.getKey();
            String knowledge = switch (id) {
                case "ancient_city" -> "ancient_city";
                case "stronghold" -> "stronghold";
                case "end_city" -> "end_city";
                case "fortress" -> "nether.war";
                case "bastion_remnant" -> "nether.war";
                default -> null;
            };
            if (knowledge == null) continue;
            knowledgeStore.learn(event.getPlayer().getUniqueId(), knowledge);
            worldState.set("structure." + id + ".discovered");
        }
    }
}
