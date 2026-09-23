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

import java.util.Optional;

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
        Optional<Entity> active = companions.getActiveEntity(player.getUniqueId()).map(entity -> (Entity) entity);
        if (active.isEmpty() || !active.get().getUniqueId().equals(event.getRightClicked().getUniqueId())) return;
        String id = companions.state(player.getUniqueId(), companions.getActive(player.getUniqueId()).id()).id();
        companions.definition(id).ifPresent(definition -> {
            if (controller.tryMount(player, (LivingEntity) event.getRightClicked(), definition)) {
                controller.prepare((LivingEntity) event.getRightClicked(), definition, player);
                event.setCancelled(true);
            }
        });
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
            companions.getOwnerOfEntity(vehicle.getUniqueId());
            companions.getActiveEntity(player.getUniqueId()).ifPresent(active -> {
                if (!active.getUniqueId().equals(vehicle.getUniqueId())) return;
                Companion companion = companions.getActive(player.getUniqueId());
                if (companion == null) return;
                companions.definition(companion.id()).ifPresent(definition -> controller.tick(player, living, definition));
            });
        }
    }

    @Override
    public void close() {
        if (task != null) { task.cancel(); task = null; }
    }
}
