package de.pixelrpg.rpg.npc;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public final class NpcDangerReactionListener implements Listener {
    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;

    public NpcDangerReactionListener(NpcManager npcManager, NpcProfileStore profileStore) {
        this.npcManager = npcManager;
        this.profileStore = profileStore;
    }

    /** Prevents hostile targeting and applies a profile-aware NPC danger response. */
    @EventHandler
    public void onEntityTargetNpc(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Mannequin mannequin)) return;
        var npcOptional = npcManager.getByEntity(mannequin.getUniqueId());
        if (npcOptional.isEmpty()) return;

        event.setCancelled(true);
        RPGNpc npc = npcOptional.get();
        NpcProfile profile = profileStore.getOrCreate(npc);

        if (profile.category() == NpcCategory.GUARD || profile.behavior().equalsIgnoreCase("guard")) {
            return;
        }

        int radius = switch (profile.category()) {
            case TRAVELER, SEEKER, STORY -> 8;
            case CHILD -> 6;
            default -> 4;
        };

        Location safe = findSafeLocation(mannequin.getLocation(), radius);
        if (safe != null) mannequin.teleport(safe);
    }

    private Location findSafeLocation(Location origin, int maxRadius) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
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
