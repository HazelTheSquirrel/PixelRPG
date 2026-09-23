package de.pixelrpg.rpg.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class NpcProtectionListener implements Listener {
    private final NpcRuntimeManager runtime;

    public NpcProtectionListener(NpcRuntimeManager runtime) { this.runtime = runtime; }

    // Prevents persistent PixelRPG NPC mannequins from receiving normal entity damage.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (runtime.getByEntity(event.getEntity().getUniqueId()).isPresent()) event.setCancelled(true);
    }
}
