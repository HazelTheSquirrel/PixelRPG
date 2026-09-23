package de.pixelrpg.rpg.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

public final class NpcChunkListener implements Listener {
    private final Plugin plugin;
    private final NpcRuntimeManager runtime;

    public NpcChunkListener(Plugin plugin, NpcRuntimeManager runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    // Re-synchronizes tracked NPC mannequins after the client has completed login.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> runtime.resyncPlayer(event.getPlayer()), 2L);
    }

    // Spawns persistent NPCs assigned to a chunk when that chunk becomes available.
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        runtime.handleChunkLoad(event.getChunk());
    }

    // Drops runtime entity mappings when the server unloads an NPC chunk.
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        runtime.handleChunkUnload(event.getChunk());
    }
}
