package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import de.pixelrpg.rpg.guild.GuildManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns region enter/leave state and presentation decisions outside the Paper event adapter. */
public final class RegionTransitionService {
    private final RegionManager regions;
    private final Map<UUID, UUID> currentRegions = new HashMap<>();
    private final GuildManager guilds;

    public RegionTransitionService(RegionManager regions, GuildManager guilds) {
        this.regions = Objects.requireNonNull(regions);
        this.guilds = Objects.requireNonNull(guilds);
    }

    public void update(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) return;
        UUID playerId = player.getUniqueId();
        UUID oldId = currentRegions.get(playerId);
        UUID newId = regions.find(location).map(PixelRegion::id).orElse(null);
        if (Objects.equals(oldId, newId)) return;
        PixelRegion oldRegion = oldId == null ? null : regions.get(oldId).orElse(null);
        PixelRegion newRegion = newId == null ? null : regions.get(newId).orElse(null);
        showTransition(player, oldRegion, newRegion, guilds);
        if (newId == null) currentRegions.remove(playerId);
        else currentRegions.put(playerId, newId);
    }

    public void clear(UUID playerId) {
        if (playerId != null) currentRegions.remove(playerId);
    }

    public void clearAll() {
        currentRegions.clear();
    }

    private static void showTransition(Player player, PixelRegion oldRegion, PixelRegion newRegion, GuildManager guilds) {
        String leave = oldRegion == null ? "" : oldRegion.leaveMessage();
        String enter = newRegion == null ? "" : newRegion.enterMessage();
        if (newRegion != null && newRegion.type() == RegionType.GUILD_TERRITORY) {
            try {
                java.util.UUID guildId = java.util.UUID.fromString(newRegion.properties().getOrDefault("guild-id", ""));
                var guild = guilds.getGuildById(guildId).orElse(null);
                if (guild != null) enter = "Königreich „" + guild.name() + "“ — " + guild.combatMode().name();
            } catch (IllegalArgumentException ignored) { }
        }
        if (leave.isBlank() && enter.isBlank()) return;

        Component title = Component.text(enter.isBlank() ? leave : enter);
        Component subtitle = enter.isBlank() || leave.isBlank() ? Component.empty() : Component.text(leave);
        player.showTitle(Title.title(title, subtitle));
    }
}
