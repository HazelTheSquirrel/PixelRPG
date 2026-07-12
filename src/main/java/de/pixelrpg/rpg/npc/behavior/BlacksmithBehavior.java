// src/main/java/de/pixelrpg/rpg/npc/behavior/BlacksmithBehavior.java
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class BlacksmithBehavior implements NpcBehavior {

    private final BlacksmithGUI blacksmithGUI;
    private final PlayerProfileManager profileManager;

    public BlacksmithBehavior(BlacksmithGUI blacksmithGUI, PlayerProfileManager profileManager) {
        this.blacksmithGUI = blacksmithGUI;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.BLACKSMITH;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return;
        }
        blacksmithGUI.open(player);
    }
}