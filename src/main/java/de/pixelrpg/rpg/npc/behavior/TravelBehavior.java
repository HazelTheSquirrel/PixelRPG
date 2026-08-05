package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.TravelGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

public final class TravelBehavior implements NpcBehavior {

    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public TravelBehavior(NpcManager npcManager, PlayerProfileManager profileManager) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.TRAVEL;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }

        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean firstTime = profile != null && !profile.hasUnlockedWaypoint(npc.id());
        if (firstTime) {
            profileManager.unlockWaypoint(player.getUniqueId(), npc.id());
            lang.send(player, "travel.unlocked", "name", npc.name());
            return;
        }

        new TravelGUI(player, npcManager, profileManager, npc.id()).open(player);
    }
}