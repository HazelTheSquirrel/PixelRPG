package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;

import java.util.List;
import java.util.Set;

public record NpcProfile(
        String npcId,
        String title,
        NpcCategory category,
        String role,
        Profession profession,
        NpcFaction faction,
        String origin,
        String personality,
        Set<String> traits,
        Set<String> knowledge,
        Set<String> secrets,
        Set<String> relationships,
        String behavior,
        String schedule,
        String dialogueTreeId,
        boolean storyRelevant,
        boolean questRelevant,
        boolean loreRelevant
) {
    public NpcProfile {
        title = title == null ? "" : title;
        role = role == null ? "" : role;
        origin = origin == null ? "" : origin;
        personality = personality == null ? "" : personality;
        behavior = behavior == null ? "resident" : behavior;
        schedule = schedule == null ? "resident" : schedule;
        dialogueTreeId = dialogueTreeId == null ? "npc.resident.basic" : dialogueTreeId;
        category = category == null ? NpcCategory.RESIDENT : category;
        faction = faction == null ? NpcFaction.NONE : faction;
        traits = Set.copyOf(traits == null ? Set.of() : traits);
        knowledge = Set.copyOf(knowledge == null ? Set.of() : knowledge);
        secrets = Set.copyOf(secrets == null ? Set.of() : secrets);
        relationships = Set.copyOf(relationships == null ? Set.of() : relationships);
    }

    public static NpcProfile resident(RPGNpc npc) {
        NpcCategory category = switch (npc.type()) {
            case PROFESSION_FARMER -> NpcCategory.FARMER;
            case PROFESSION_FISHERMAN -> NpcCategory.FISHERMAN;
            case PROFESSION_SCHOLAR -> NpcCategory.SCHOLAR;
            case PROFESSION_BLACKSMITH, PROFESSION_COOK, PROFESSION_TAILOR,
                 PROFESSION_ALCHEMIST, PROFESSION_MASON, PROFESSION_WOODCUTTER -> NpcCategory.CRAFTSPERSON;
            case STORY -> NpcCategory.STORY;
            case QUEST -> NpcCategory.QUEST;
            case TRAVEL -> NpcCategory.TRAVELER;
            default -> NpcCategory.RESIDENT;
        };
        String tree = switch (category) {
            case FARMER -> "npc.resident.farmer";
            case FISHERMAN -> "npc.resident.fisherman";
            case SCHOLAR -> "npc.resident.scholar";
            case CRAFTSPERSON -> "npc.resident.craftsperson";
            case TRAVELER -> "npc.resident.traveler";
            case STORY -> "npc.story.basic";
            default -> "npc.resident.basic";
        };
        return new NpcProfile(
                npc.id(), "", category, category.name().toLowerCase(), npc.profession(),
                NpcFaction.NONE, "", "unbeschrieben", Set.of(), Set.of(), Set.of(), Set.of(),
                "resident", "resident", tree, category == NpcCategory.STORY,
                category == NpcCategory.QUEST, category == NpcCategory.SCHOLAR || category == NpcCategory.STORY
        );
    }
}
