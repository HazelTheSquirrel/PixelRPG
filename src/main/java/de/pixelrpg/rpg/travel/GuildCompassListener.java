package de.pixelrpg.rpg.travel;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class GuildCompassListener implements Listener {

    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public GuildCompassListener(NpcManager npcManager, PlayerProfileManager profileManager) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    // Zuständig für die Anzeige des nächstgelegenen freigeschalteten Reise-NPCs
    // in der Actionbar, wenn der Spieler mit dem Gildenkompass interagiert.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!GuildCompassItemFactory.isCompass(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        RPGNpc nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (RPGNpc npc : npcManager.getAll()) {
            if (npc.type() != NpcType.TRAVEL || !profile.hasUnlockedWaypoint(npc.id())) {
                continue;
            }
            if (!npc.location().getWorld().equals(player.getWorld())) {
                continue;
            }
            double distanceSquared = npc.location().distanceSquared(player.getLocation());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = npc;
            }
        }

        if (nearest == null) {
            lang.sendActionBar(player, "compass.none");
            return;
        }

        String direction = computeDirection(player.getLocation(), nearest.location());
        double distance = Math.sqrt(nearestDistanceSquared);
        player.sendActionBar(lang.get("compass.waypoint",
                "name", nearest.name(),
                "distance", String.valueOf(Math.round(distance)),
                "direction", direction));
    }

    private String computeDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        if (angle < 0) angle += 360.0;

        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(angle / 45.0) % 8;
        return directions[index];
    }
}