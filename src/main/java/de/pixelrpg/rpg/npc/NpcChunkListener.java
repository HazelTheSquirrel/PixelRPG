// src/main/java/de/pixelrpg/rpg/npc/NpcChunkListener.java
package de.pixelrpg.rpg.npc;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

public final class NpcChunkListener implements Listener {

    private final NpcManager npcManager;
    private final Plugin plugin;

    public NpcChunkListener(NpcManager npcManager) {
        this.npcManager = npcManager;
        this.plugin = Bukkit.getPluginManager().getPlugin("PixelRPG");
    }

    // Re-synchronizes NPC entities for a player after the client has finished joining.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> npcManager.resyncPlayer(event.getPlayer()), 2L);
    }

    // Restores NPCs when their chunk becomes loaded again.
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        npcManager.handleChunkLoad(event.getChunk());
    }

    // Removes NPC tracking entries when their chunk unloads.
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        npcManager.handleChunkUnload(event.getChunk());
    }
}
