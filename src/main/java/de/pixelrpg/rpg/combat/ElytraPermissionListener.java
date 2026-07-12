// src/main/java/de/pixelrpg/rpg/combat/ElytraPermissionListener.java
package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.player.PlayerAttribute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public final class ElytraPermissionListener implements Listener {

    private final GuildAPI guildAPI;

    public ElytraPermissionListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!event.isGliding()) {
            return;
        }
        if (!guildAPI.isRegistered(player.getUniqueId())) {
            return;
        }

        int permit = guildAPI.getAttributePoints(player.getUniqueId(), PlayerAttribute.ELYTRA_PERMIT);
        if (permit <= 0) {
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    "Elytra usage requires the Elytra Permit, purchasable at Rank S.", NamedTextColor.RED));
        }
    }
}