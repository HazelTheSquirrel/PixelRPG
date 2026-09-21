package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.lore.LoreRegistry;
import de.pixelrpg.rpg.npc.NpcProfileStore;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.QuestManager;

import java.util.Objects;

public record DialogueDomainState(
        PlayerKnowledgeStore playerKnowledge,
        NpcKnowledgeStore npcKnowledge,
        WorldState worldState,
        NpcRelationshipStore playerNpcRelationships,
        NpcNetworkRelationshipStore npcRelationships,
        PlayerFactionRelationshipStore playerFactionRelationships,
        FactionRelationshipStore factionRelationships,
        LoreRegistry loreRegistry,
        QuestManager questManager,
        PlayerProfileManager playerProfiles,
        NpcProfileStore npcProfiles
) {
    public DialogueDomainState {
        Objects.requireNonNull(playerKnowledge, "playerKnowledge");
        Objects.requireNonNull(npcKnowledge, "npcKnowledge");
        Objects.requireNonNull(worldState, "worldState");
        Objects.requireNonNull(playerNpcRelationships, "playerNpcRelationships");
        Objects.requireNonNull(npcRelationships, "npcRelationships");
        Objects.requireNonNull(playerFactionRelationships, "playerFactionRelationships");
        Objects.requireNonNull(factionRelationships, "factionRelationships");
        Objects.requireNonNull(loreRegistry, "loreRegistry");
        Objects.requireNonNull(questManager, "questManager");
        Objects.requireNonNull(playerProfiles, "playerProfiles");
        Objects.requireNonNull(npcProfiles, "npcProfiles");
    }
}
