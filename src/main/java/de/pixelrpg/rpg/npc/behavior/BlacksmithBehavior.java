package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

public final class BlacksmithBehavior implements NpcBehavior {

    private final BlacksmithGUI blacksmithGUI;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public BlacksmithBehavior(BlacksmithGUI blacksmithGUI, PlayerProfileManager profileManager) {
        this.blacksmithGUI = blacksmithGUI;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.BLACKSMITH;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }
        blacksmithGUI.open(player);
    }
}