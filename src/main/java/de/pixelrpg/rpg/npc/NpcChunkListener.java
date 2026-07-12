// src/main/java/de/pixelrpg/rpg/npc/NpcChunkListener.java
package de.pixelrpg.rpg.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class NpcChunkListener implements Listener {

    private final NpcManager npcManager;

    public NpcChunkListener(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        npcManager.handleChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        npcManager.handleChunkUnload(event.getChunk());
    }
}