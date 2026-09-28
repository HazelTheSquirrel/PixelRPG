package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class BossDeathListener implements Listener {

    private final BossManager bossManager;

    public BossDeathListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    // Zuständig für das Auslösen von Bossbar-Cleanup und Belohnungsverteilung
    // (Welt-Bosse), sobald ein registrierter Boss (World-Boss-Marker oder bossId) stirbt.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        boolean isWorldBoss = Boolean.TRUE.equals(entity.getPersistentDataContainer()
                .get(RPGKeys.Boss.worldBossMarker(), PersistentDataType.BOOLEAN));

        boolean hasBossId = entity.getPersistentDataContainer()
                .has(RPGKeys.Boss.bossId(), PersistentDataType.STRING);

        if (!isWorldBoss && !hasBossId) {
            return;
        }

        bossManager.onBossDeath(entity);
    }

    // Zuständig für die Weiterleitung eines besiegten Weltbosses an aktive Teilnehmer-Quests.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDefeated(de.pixelrpg.rpg.api.events.BossDefeatedEvent event) {
        var questManager = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getQuestManager();
        if (questManager == null) return;
        event.getParticipants().forEach(playerId -> questManager.progressWorldBossQuest(playerId, event.getBossId()));
    }
}