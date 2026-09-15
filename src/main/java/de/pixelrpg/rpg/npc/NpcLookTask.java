package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Event-driven NPC look and per-player name visibility controller. */
public final class NpcLookTask implements Listener {
    private static final double INTERACTION_RADIUS = 5.0D;
    private static final double INTERACTION_RADIUS_SQUARED = INTERACTION_RADIUS * INTERACTION_RADIUS;
    private static final double NAME_HEIGHT_OFFSET = 2.15D;

    private final Plugin plugin;
    private final NpcManager npcManager;
    private final Map<String, UUID> nameDisplayByNpcId = new ConcurrentHashMap<>();
    private boolean started;

    public NpcLookTask(Plugin plugin, NpcManager npcManager, double ignoredLookRadius, double ignoredNameplateRadius, int ignoredIntervalTicks) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    public void start() {
        if (started) return;
        started = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refreshAllFor(player);
        }
    }

    public void stop() {
        if (!started) return;
        HandlerList.unregisterAll(this);
        for (UUID uuid : nameDisplayByNpcId.values()) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null) entity.remove();
        }
        nameDisplayByNpcId.clear();
        started = false;
    }

    // A player joining must receive only the NPC names that are within the interaction radius.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshAllFor(event.getPlayer());
    }

    // NPC look direction and per-player name visibility update when the player crosses a block boundary.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        updateAround(event.getFrom());
        updateAround(event.getTo());
    }

    // A disconnect can change which player is the nearest active look target.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        updateAround(event.getPlayer().getLocation());
    }

    private void updateAround(Location center) {
        if (center == null || center.getWorld() == null) return;

        for (Entity entity : center.getNearbyEntities(INTERACTION_RADIUS, INTERACTION_RADIUS, INTERACTION_RADIUS)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getPersistentDataContainer().has(RPGKeys.Npc.npcId(), PersistentDataType.STRING)) continue;
            updateNpc(living);
        }
    }

    private void refreshAllFor(Player player) {
        for (RPGNpc npc : npcManager.getAll()) {
            Entity mannequin = getMannequin(npc);
            if (mannequin == null) continue;
            updateNpc(npc, mannequin);
        }
    }

    private void updateNpc(LivingEntity living) {
        String npcId = living.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
        if (npcId == null) return;

        RPGNpc npc = npcManager.getById(npcId).orElse(null);
        if (npc == null) return;
        updateNpc(npc, living);
    }

    private void updateNpc(RPGNpc npc, Entity mannequin) {
        if (!mannequin.isValid()) return;

        TextDisplay nameDisplay = getOrCreateNameDisplay(npc, mannequin.getLocation());
        nameDisplay.text(Component.text(npc.name(), npc.type().getColor()));
        nameDisplay.teleport(mannequin.getLocation().add(0.0D, NAME_HEIGHT_OFFSET, 0.0D));

        Location npcLocation = mannequin.getLocation();
        Player nearestPlayer = null;
        double nearestDistanceSquared = INTERACTION_RADIUS_SQUARED;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() != npcLocation.getWorld()) continue;

            double distanceSquared = player.getLocation().distanceSquared(npcLocation);
            boolean inRange = distanceSquared <= INTERACTION_RADIUS_SQUARED;

            if (inRange) {
                player.showEntity(plugin, nameDisplay);
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearestPlayer = player;
                }
            } else {
                player.hideEntity(plugin, nameDisplay);
            }
        }

        if (nearestPlayer != null && mannequin instanceof LivingEntity living) {
            living.lookAt(nearestPlayer.getEyeLocation(), LookAnchor.EYES);
        }
    }

    private TextDisplay getOrCreateNameDisplay(RPGNpc npc, Location mannequinLocation) {
        UUID trackedUuid = nameDisplayByNpcId.get(npc.id());
        if (trackedUuid != null) {
            Entity tracked = plugin.getServer().getEntity(trackedUuid);
            if (tracked instanceof TextDisplay display && display.isValid()) return display;
            nameDisplayByNpcId.remove(npc.id(), trackedUuid);
        }

        TextDisplay display = mannequinLocation.getWorld().spawn(
                mannequinLocation.clone().add(0.0D, NAME_HEIGHT_OFFSET, 0.0D),
                TextDisplay.class,
                entity -> {
                    entity.text(Component.text(npc.name(), npc.type().getColor()));
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setDefaultBackground(false);
                    entity.setShadowed(true);
                    entity.setSeeThrough(false);
                    entity.setPersistent(false);
                    entity.setViewRange(5.0F);
                    entity.setDisplayWidth(2.0F);
                    entity.setDisplayHeight(0.5F);
                    entity.getPersistentDataContainer().set(RPGKeys.Npc.npcType(), PersistentDataType.STRING, "NAME_DISPLAY");
                    entity.getPersistentDataContainer().set(RPGKeys.Npc.npcId(), PersistentDataType.STRING, npc.id());
                }
        );

        nameDisplayByNpcId.put(npc.id(), display.getUniqueId());
        return display;
    }

    private Entity getMannequin(RPGNpc npc) {
        for (UUID uuid : npcManager.getSpawnedEntityUuids()) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity == null || !entity.isValid()) continue;
            if (!(entity instanceof LivingEntity)) continue;

            String npcId = entity.getPersistentDataContainer().get(RPGKeys.Npc.npcId(), PersistentDataType.STRING);
            if (npc.id().equals(npcId)) return entity;
        }
        return null;
    }
}
