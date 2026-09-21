package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.DialogueTreeService;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.NpcProfileStore;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Generic resident NPC behavior that now enters the dialogue-driven resident content layer. */
public final class FillerBehavior implements NpcBehavior {
    private final QuestManager questManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final DialogueTreeService dialogueTreeService;
    private final NpcProfileStore profileStore;

    public FillerBehavior(QuestManager questManager, PlayerProfileManager profileManager,
                           DialogueEngine dialogueEngine, DialogueTreeService dialogueTreeService,
                           NpcProfileStore profileStore) {
        this.questManager = questManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.dialogueTreeService = dialogueTreeService;
        this.profileStore = profileStore;
    }

    @Override
    public NpcType type() {
        return NpcType.FILLER;
    }

    @Override
    public void onInteract(DialogueContext context) {
        Player player = context.player();
        RPGNpc npc = context.npc();
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openNotice(player,
                    Component.text("Bewohner", NamedTextColor.WHITE),
                    Component.text("Du bist noch nicht registriert.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.WHITE));
            return;
        }

        questManager.progressTalkToNpc(player, npc.id());
        String treeId = profileStore.getOrCreate(npc).dialogueTreeId();
        dialogueTreeService.open(player,
                de.pixelrpg.rpg.dialogue.DialogueContext.forNpc(player, npc),
                treeId);
    }
}
