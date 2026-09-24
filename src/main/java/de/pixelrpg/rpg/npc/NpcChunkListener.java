package de.pixelrpg.rpg.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/** Keeps persisted NPC mannequins synchronized with chunk lifecycle. */
public final class NpcChunkListener implements Listener {
    private final NpcManager npcs;
    public NpcChunkListener(NpcManager npcs) { this.npcs = npcs; }

    // Zuständig für das Spawnen der NPCs, sobald ihr Persistenz-Chunk geladen wurde.
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        npcs.handleChunkLoad(event.getChunk());
    }

    // Zuständig für das Entfernen der Runtime-Referenzen beim Entladen eines NPC-Chunks.
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        npcs.handleChunkUnload(event.getChunk());
    }
}
