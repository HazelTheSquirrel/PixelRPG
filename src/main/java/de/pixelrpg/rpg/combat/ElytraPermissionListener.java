package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerAttribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public final class ElytraPermissionListener implements Listener {

    private final GuildAPI guildAPI;
    private final LanguageManager lang;

    public ElytraPermissionListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    // Zuständig für die Durchsetzung der Elytra-Erlaubnis: verhindert Gleitflug
    // für registrierte Spieler ohne freigeschaltetes ELYTRA_PERMIT-Attribut.
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
            lang.sendActionBar(player, "elytra.requires-permit");
        }
    }
}