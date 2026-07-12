// src/main/java/de/pixelrpg/rpg/npc/behavior/TravelBehavior.java (VOLLSTÄNDIG, ersetzt alte Datei — übergibt eigene ID zum Ausschließen)
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.TravelGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class TravelBehavior implements NpcBehavior {

    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;

    public TravelBehavior(NpcManager npcManager, PlayerProfileManager profileManager) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.TRAVEL;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return;
        }

        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean firstTime = profile != null && !profile.hasUnlockedWaypoint(npc.id());
        if (firstTime) {
            profileManager.unlockWaypoint(player.getUniqueId(), npc.id());
            player.sendMessage(Component.text("Waypoint unlocked: ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(npc.name(), NamedTextColor.WHITE)));
            return;
        }

        new TravelGUI(player, npcManager, profileManager, npc.id()).open(player);
    }
}