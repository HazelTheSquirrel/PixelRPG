package de.pixelrpg.rpg.companion;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;


public final class CompanionMountListener implements Listener, AutoCloseable {
    private final Plugin plugin;
    private final CompanionService companions;
    private final CompanionMountController controller;
    private BukkitTask task;

    public CompanionMountListener(Plugin plugin, CompanionService companions) {
        this.plugin = plugin;
        this.companions = companions;
        this.controller = new CompanionMountController();
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    // Startet das Reiten eines eigenen aktivierten Companion-Mounts per Schleichen + Interaktion.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        UUID activeId = companions.getActiveEntity(player.getUniqueId());
        if (activeId == null || !activeId.equals(event.getRightClicked().getUniqueId())) return;
        Companion active = companions.getActive(player.getUniqueId());
        if (active == null) return;
        CompanionDefinition definition = companions.definition(active.id());
        if (controller.tryMount(player, (LivingEntity) event.getRightClicked(), definition)) {
            controller.prepare((LivingEntity) event.getRightClicked(), definition, player);
            event.setCancelled(true);
        }
    }

    // Beendet die Mount-Steuerung beim Verlassen des Servers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getVehicle() != null) event.getPlayer().leaveVehicle();
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof LivingEntity living)) continue;
            UUID activeId = companions.getActiveEntity(player.getUniqueId());
            if (activeId == null || !activeId.equals(vehicle.getUniqueId())) continue;
            Companion companion = companions.getActive(player.getUniqueId());
            if (companion == null) continue;
            controller.tick(player, living, companions.definition(companion.id()));
        }
    }

    @Override
    public void close() {
        if (task != null) { task.cancel(); task = null; }
    }
}
