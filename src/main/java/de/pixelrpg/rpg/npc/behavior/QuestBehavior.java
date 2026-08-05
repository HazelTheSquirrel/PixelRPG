package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.QuestCategoryGUI;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import org.bukkit.entity.Player;

public final class QuestBehavior implements NpcBehavior {

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public QuestBehavior(QuestManager questManager, PlayerProfileManager profileManager) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    public NpcType type() {
        return NpcType.QUEST;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            lang.send(player, "npc.not-registered");
            return;
        }
        new QuestCategoryGUI(player, questManager, profileManager).open(player);
    }
}