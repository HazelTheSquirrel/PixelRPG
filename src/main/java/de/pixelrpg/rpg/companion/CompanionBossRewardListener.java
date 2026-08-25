package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.api.events.BossDefeatedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Unlocks data-configured companion rewards for eligible players after a boss defeat. */
public final class CompanionBossRewardListener implements Listener {
    private final CompanionService companionService;

    public CompanionBossRewardListener(CompanionService companionService) {
        this.companionService = companionService;
    }

    // Handles companion unlocks configured as rewards for defeated bosses.
    @EventHandler
    public void onBossDefeated(BossDefeatedEvent event) {
        for (var playerId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            if (!companionService.unlockFromBoss(playerId, event.getBossId())) continue;
            player.sendMessage(Component.text("Neuer Begleiter freigeschaltet!", NamedTextColor.GREEN));
        }
    }
}
