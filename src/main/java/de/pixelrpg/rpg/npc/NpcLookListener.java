package de.pixelrpg.rpg.npc;

import io.papermc.paper.entity.LookAnchor;
import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;

public final class NpcLookListener implements Listener {
    private static final double RADIUS_SQUARED = 25.0D;
    private final Plugin plugin;
    private final NpcRuntimeManager npcs;

    public NpcLookListener(Plugin plugin, NpcRuntimeManager npcs) {
        this.plugin = plugin;
        this.npcs = npcs;
    }

    // Richtet einen nahen NPC bei relevanten Bewegungen auf den nächsten Spieler aus.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        updateAround(event.getTo());
        updateAround(event.getFrom());
    }

    private void updateAround(org.bukkit.Location center) {
        if (center == null || center.getWorld() == null) return;
        for (Entity entity : center.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
            if (!(entity instanceof LivingEntity living)) continue;
            String npcId = living.getPersistentDataContainer().get(new RPGKeys(plugin).Npc.npcId(), PersistentDataType.STRING);
            if (npcId == null || npcs.getById(npcId).isEmpty()) continue;
            Player nearest = null;
            double distance = RADIUS_SQUARED;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld() != living.getWorld()) continue;
                double current = player.getLocation().distanceSquared(living.getLocation());
                if (current <= distance) { distance = current; nearest = player; }
            }
            if (nearest != null) living.lookAt(nearest.getEyeLocation(), LookAnchor.EYES);
        }
    }
}
