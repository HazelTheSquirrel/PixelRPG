// src/main/java/de/pixelrpg/rpg/player/GuildJoinLeaveListener.java (VOLLSTÄNDIG, ersetzt alte Datei — verweigert Login bei fehlgeschlagenem Profil-Ladevorgang statt mit leerem Profil fortzufahren)
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.PixelRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class GuildJoinLeaveListener implements Listener {

    private final PlayerProfileManager profileManager;

    public GuildJoinLeaveListener(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    // Zuständig für das Laden des Spielerprofils vor dem Login. Schlägt das Laden
    // fehl (z. B. DB kurz nicht erreichbar), wird der Login verweigert statt mit
    // einem leeren Profil fortzufahren - verhindert, dass echter Fortschritt
    // beim nächsten Speichern überschrieben wird.
    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerProfileManager.LoadOutcome outcome = profileManager.loadForPreLogin(event.getUniqueId());
        if (outcome == PlayerProfileManager.LoadOutcome.FAILED) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, loadFailedMessage());
        }
    }

    // Zuständig für die Aktivierung des zuvor geladenen Profils, sobald der
    // Spieler den Server tatsächlich betritt.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.activateOnJoin(event.getPlayer());
    }

    // Zuständig für das Auslösen des (queue-gesicherten) Speichervorgangs beim Verlassen.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profileManager.deactivateOnQuit(event.getPlayer().getUniqueId());
    }

    private Component loadFailedMessage() {
        String language = PixelRPGPlugin.getInstance().getLanguageManager().getCurrentLanguage();
        String text = switch (language) {
            case "de" -> "Fehler beim Laden deiner Daten. Bitte versuche es gleich erneut.";
            case "fr" -> "Erreur lors du chargement de vos données. Réessaie dans un instant.";
            case "es" -> "Error al cargar tus datos. Inténtalo de nuevo en un momento.";
            default -> "An error occurred while loading your data. Please try again in a moment.";
        };
        return Component.text(text, NamedTextColor.RED);
    }
}