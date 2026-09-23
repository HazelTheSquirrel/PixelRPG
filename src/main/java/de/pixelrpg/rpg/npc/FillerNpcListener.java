package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestDefinition;
import de.pixelrpg.rpg.quest.QuestService;
import de.pixelrpg.rpg.quest.QuestType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/** Connects FILLER NPCs to TALK_TO_NPC quest objectives while keeping filler NPCs content-light. */
public final class FillerNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final PlayerProfileManager profiles;
    private final QuestService quests;
    private final DialogueEngine dialogs;

    public FillerNpcListener(NpcRuntimeManager npcs, PlayerProfileManager profiles, QuestService quests, DialogueEngine dialogs) {
        this.npcs = npcs;
        this.profiles = profiles;
        this.quests = quests;
        this.dialogs = dialogs;
    }

    // Advances TALK_TO_NPC quests when the player reaches a designated filler NPC.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.FILLER) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogs.openUnavailable(player, "Questziel", "Du bist noch nicht registriert.");
            return;
        }
        boolean changed = false;
        for (QuestDefinition quest : quests.repository().getAll()) {
            if (quest.type() != QuestType.TALK_TO_NPC || !quest.targetKey().equals(npc.id())) continue;
            if (profile.hasActiveQuest(quest.id())) changed |= quests.incrementProgress(profile, quest.id(), 1);
        }
        if (changed) profiles.saveProfileAsync(player.getUniqueId());
        dialogs.openNotice(player, Component.text(npc.name(), NamedTextColor.WHITE),
                Component.text(changed ? "Du hast den Ziel-NPC erreicht." : "Dieser NPC hat aktuell keine weitere Questaktion für dich."),
                Component.text("Schließen", NamedTextColor.GRAY));
    }
}
