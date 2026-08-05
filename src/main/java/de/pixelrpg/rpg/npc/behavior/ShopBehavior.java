package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.ShopGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.shop.ShopManager;
import org.bukkit.entity.Player;

public final class ShopBehavior implements NpcBehavior {

    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public ShopBehavior(ShopManager shopManager, PlayerProfileManager profileManager) {
        this.shopManager = shopManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.SHOP;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }
        new ShopGUI(player, npc.id(), shopManager, profileManager).open(player);
    }
}