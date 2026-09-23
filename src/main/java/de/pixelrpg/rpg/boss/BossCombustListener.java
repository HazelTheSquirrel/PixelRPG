package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.persistence.PersistentDataType;

public final class BossCombustListener implements Listener {

    // Zuständig dafür, dass registrierte Biom- und Worldbosse nicht durch Tageslicht verbrennen.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSunlightCombust(EntityCombustEvent event) {
        if (event.getClass() != EntityCombustEvent.class) return;
        if (!event.getEntity().getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;
        event.setCancelled(true);
    }
}
