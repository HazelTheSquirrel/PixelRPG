// src/main/java/de/pixelrpg/rpg/npc/behavior/QuestBehavior.java (VOLLSTÄNDIG, ersetzt alte Datei — öffnet Rang-Kategorie-Menü)
package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.gui.QuestCategoryGUI;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class QuestBehavior implements NpcBehavior {

    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;

    public QuestBehavior(QuestManager questManager, PlayerProfileManager profileManager) {
        this.questManager = questManager;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.QUEST;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a registered guild member.", NamedTextColor.RED));
            return;
        }
        new QuestCategoryGUI(player, questManager, profileManager).open(player);
    }
}