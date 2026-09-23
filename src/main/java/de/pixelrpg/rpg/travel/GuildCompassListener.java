package de.pixelrpg.rpg.travel;

import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class GuildCompassListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final PlayerProfileManager profiles;

    public GuildCompassListener(NpcRuntimeManager npcs, PlayerProfileManager profiles) {
        this.npcs = npcs;
        this.profiles = profiles;
    }

    // Zeigt bei Benutzung des Gildenkompasses das nächste freigeschaltete Reiseziel an.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !GuildCompassItemFactory.isCompass(event.getItem())) return;
        Player player = event.getPlayer();
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        RPGNpc nearest = null;
        double distanceSquared = Double.MAX_VALUE;
        for (RPGNpc npc : npcs.getAll()) {
            if (npc.type() != NpcType.TRAVEL || !profile.hasUnlockedWaypoint(npc.id())) continue;
            if (!npc.location().getWorld().equals(player.getWorld())) continue;
            double current = npc.location().distanceSquared(player.getLocation());
            if (current < distanceSquared) { distanceSquared = current; nearest = npc; }
        }
        if (nearest == null) {
            player.sendActionBar(Component.text("Noch keine Wegpunkte entdeckt.", NamedTextColor.GRAY));
            return;
        }
        double distance = Math.sqrt(distanceSquared);
        player.sendActionBar(Component.text("Nächstes Ziel: " + nearest.name() + " - " + Math.round(distance) + "m " + direction(player.getLocation(), nearest.location()), NamedTextColor.LIGHT_PURPLE));
    }

    private String direction(Location from, Location to) {
        double angle = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX())) - 90.0;
        if (angle < 0) angle += 360.0;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(angle / 45.0) % 8];
    }
}
