package de.pixelrpg.rpg.story;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.entity.Player;

/** Event-driven story structure trigger listener. */
public final class StoryTriggerListener implements Listener {
    private final StoryLocationRegistry registry;

    public StoryTriggerListener(StoryLocationRegistry registry) {
        this.registry = registry;
    }

    // Checks story structures only when a player enters a new block.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        registry.checkPlayer(player);
    }

    // Re-checks the destination after a teleport into a loaded story structure.
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        registry.checkPlayer(event.getPlayer());
    }

    // Checks players already present when a relevant chunk finishes loading.
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Player player : event.getChunk().getPlayersSeeingChunk()) {
            registry.checkChunk(player, event.getChunk());
        }
    }
}
