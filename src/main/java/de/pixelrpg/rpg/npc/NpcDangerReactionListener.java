package de.pixelrpg.rpg.npc;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public final class NpcDangerReactionListener implements Listener {
    private final NpcManager npcManager;

    public NpcDangerReactionListener(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    /** Prevents hostile targeting of NPC mannequins and moves the NPC to a nearby safe position. */
    @EventHandler
    public void onEntityTargetNpc(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Mannequin mannequin)) return;
        if (npcManager.getByEntity(mannequin.getUniqueId()).isEmpty()) return;

        event.setCancelled(true);
        Location safe = findSafeLocation(mannequin.getLocation());
        if (safe != null) mannequin.teleport(safe);
    }

    private Location findSafeLocation(Location origin) {
        for (int radius = 1; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    int baseY = origin.getBlockY();
                    for (int y = -1; y <= 1; y++) {
                        Location candidate = origin.clone().add(x, y, z);
                        Block feet = candidate.getBlock();
                        Block head = feet.getRelative(0, 1, 0);
                        Block below = feet.getRelative(0, -1, 0);
                        if (feet.isPassable() && head.isPassable() && !below.isPassable()) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }
}
