package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.TravelDialog;
import de.pixelrpg.rpg.dialogue.DialogueContext;
import de.pixelrpg.rpg.dialogue.DialogueTreeService;
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
    private final DialogueEngine dialogueEngine;
    private final DialogueTreeService dialogueTreeService;

    public TravelBehavior(NpcManager npcManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine, DialogueTreeService dialogueTreeService) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.dialogueTreeService = dialogueTreeService;
    }

    @Override public NpcType type() { return NpcType.TRAVEL; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openNotice(
                    player,
                    Component.text("Schnellreise", NamedTextColor.LIGHT_PURPLE),
                    Component.text("Du musst zuerst registriertes Rathausmitglied sein.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        dialogueTreeService.open(player, DialogueContext.forNpc(player, npc), "npc.function.travel", () -> openTravelFunction(player, npc));
    }

    private void openTravelFunction(Player player, RPGNpc npc) {
        var profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean firstTime = profile != null && !profile.hasUnlockedWaypoint(npc.id());
        if (firstTime) {
            profileManager.unlockWaypoint(player.getUniqueId(), npc.id());
            dialogueEngine.openNotice(
                    player,
                    Component.text("Reisepunkt freigeschaltet", NamedTextColor.GREEN),
                    Component.text(npc.name() + " wurde als Reisepunkt freigeschaltet."),
                    Component.text("Schließen", NamedTextColor.GREEN)
            );
            return;
        }
        new TravelDialog(npcManager, profileManager, dialogueEngine).open(player, npc.id());
    }
}
