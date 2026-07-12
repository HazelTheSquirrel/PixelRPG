// src/main/java/de/pixelrpg/rpg/combat/SoulslikeDeathScreenListener.java
package de.pixelrpg.rpg.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;

public final class SoulslikeDeathScreenListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        String causeName = "the void";
        if (player.getKiller() instanceof Player killer) {
            causeName = killer.getName();
        } else {
            LivingEntity lastDamager = null;
            var lastDamageEvent = player.getLastDamageCause();
            if (lastDamageEvent != null && lastDamageEvent.getEntity() instanceof LivingEntity living) {
                lastDamager = living;
            }
            if (lastDamager != null) {
                causeName = lastDamager.getType().name().replace('_', ' ').toLowerCase();
            }
        }

        Title title = Title.title(
                Component.text("YOU DIED", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("Slain by " + causeName, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );
        player.showTitle(title);
    }
}