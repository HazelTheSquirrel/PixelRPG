// src/main/java/de/pixelrpg/rpg/region/PlayerRegionTracker.java (VOLLSTÄNDIG, ersetzt alte Datei — Neuberechnung nur bei Chunk-Wechsel statt jedem Block)
package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.region.biome.BiomeClusterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegionTracker implements Listener {

    private final RegionManager regionManager;
    private final BiomeClusterManager biomeClusterManager;
    private final GuildAPI guildAPI;
    private final Title.Times titleTimes;
    private final Map<UUID, String> lastIdentityByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChunkKeyByPlayer = new ConcurrentHashMap<>();

    public PlayerRegionTracker(RegionManager regionManager, BiomeClusterManager biomeClusterManager, GuildAPI guildAPI,
                                long fadeInMs, long stayMs, long fadeOutMs) {
        this.regionManager = regionManager;
        this.biomeClusterManager = biomeClusterManager;
        this.guildAPI = guildAPI;
        this.titleTimes = Title.Times.times(
                Duration.ofMillis(fadeInMs), Duration.ofMillis(stayMs), Duration.ofMillis(fadeOutMs));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        int fromChunkX = event.getFrom().getBlockX() >> 4;
        int fromChunkZ = event.getFrom().getBlockZ() >> 4;
        int toChunkX = event.getTo().getBlockX() >> 4;
        int toChunkZ = event.getTo().getBlockZ() >> 4;

        if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) {
            return;
        }
        evaluate(event.getPlayer().getUniqueId(), event.getTo(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        evaluate(event.getPlayer().getUniqueId(), event.getPlayer().getLocation(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        lastIdentityByPlayer.remove(event.getPlayer().getUniqueId());
        lastChunkKeyByPlayer.remove(event.getPlayer().getUniqueId());
        evaluate(event.getPlayer().getUniqueId(), event.getPlayer().getLocation(), event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastIdentityByPlayer.remove(event.getPlayer().getUniqueId());
        lastChunkKeyByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void evaluate(UUID uuid, org.bukkit.Location location, Player player) {
        if (!guildAPI.isRegistered(uuid)) {
            return;
        }

        List<Region> manualRegions = regionManager.getRegionsAt(location);
        String identityKey;
        Component title;
        Component subtitle;

        if (!manualRegions.isEmpty()) {
            Region top = manualRegions.stream()
                    .max(java.util.Comparator.<Region>comparingInt(Region::getPriority)
                            .thenComparing(r -> -r.getTotalVolume()))
                    .orElseThrow();
            identityKey = "manual:" + top.getId();
            title = top.titleComponent();
            subtitle = top.subtitleComponent();
        } else {
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;
            var clusterInfo = biomeClusterManager.resolveChunk(location.getWorld(), chunkX, chunkZ, location.getBlockY());
            if (clusterInfo.isEmpty()) {
                return;
            }
            identityKey = "biome:" + clusterInfo.get().clusterId();
            title = Component.text(clusterInfo.get().displayName(), NamedTextColor.GREEN);
            subtitle = Component.text("Wilderness", NamedTextColor.GRAY);
        }

        String previousKey = lastIdentityByPlayer.put(uuid, identityKey);
        if (previousKey != null && previousKey.equals(identityKey)) {
            return;
        }

        player.showTitle(Title.title(title, subtitle, titleTimes));
    }
}