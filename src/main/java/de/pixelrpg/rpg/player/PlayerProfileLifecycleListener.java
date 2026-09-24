package de.pixelrpg.rpg.player;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
public final class PlayerProfileLifecycleListener implements Listener {
 private final PlayerProfileManager profiles; public PlayerProfileLifecycleListener(PlayerProfileManager p){profiles=p;}
 /** Loads the player's authoritative profile during the join lifecycle. */
 @EventHandler public void onJoin(PlayerJoinEvent e){profiles.load(e.getPlayer().getUniqueId()).exceptionally(f->{e.getPlayer().kick(Component.text("Dein Spielerprofil konnte nicht geladen werden."));return null;});}
 /** Saves and releases the player's profile during the quit lifecycle. */
 @EventHandler public void onQuit(PlayerQuitEvent e){profiles.save(e.getPlayer().getUniqueId()).whenComplete((v,f)->profiles.unload(e.getPlayer().getUniqueId()));}
}
