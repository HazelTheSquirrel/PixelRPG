package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.ReceptionGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

public final class ReceptionBehavior implements NpcBehavior {

    private final PlayerProfileManager profileManager;

    public ReceptionBehavior(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.RECEPTION;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        new ReceptionGUI(player, profileManager).open(player);
    }
}