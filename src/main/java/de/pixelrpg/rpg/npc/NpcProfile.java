package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;

import java.util.List;
import java.util.Set;

public record NpcProfile(
        String npcId,
        String title,
        NpcCategory category,
        String role,
        Set<NpcFunction> functions,
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
        boolean loreRelevant,
        String skinSource
) {
    public NpcProfile(String npcId, String title, NpcCategory category, String role, Profession profession,
                       NpcFaction faction, String origin, String personality, Set<String> traits,
                       Set<String> knowledge, Set<String> secrets, Set<String> relationships,
                       String behavior, String schedule, String dialogueTreeId,
                       boolean storyRelevant, boolean questRelevant, boolean loreRelevant) {
        this(npcId, title, category, role, functionsForCategory(category, profession),
                profession, faction, origin, personality, traits, knowledge, secrets, relationships,
                behavior, schedule, dialogueTreeId, storyRelevant, questRelevant, loreRelevant, null);
    }

    public NpcProfile {
        title = title == null ? "" : title;
        role = role == null ? "" : role;
        functions = Set.copyOf(functions == null || functions.isEmpty()
                ? functionsForCategory(category, profession)
                : functions);
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
        skinSource = skinSource == null || skinSource.isBlank() ? null : skinSource.trim();
    }

    public NpcProfile withIdentity(String title, NpcCategory category, String role, NpcFaction faction,
                                   String origin, String personality, Set<String> traits, String behavior,
                                   String schedule, String dialogueTreeId) {
        return new NpcProfile(
                npcId, title, category, role, functions, profession, faction, origin, personality, traits,
                knowledge, secrets, relationships, behavior, schedule, dialogueTreeId,
                storyRelevant, questRelevant, loreRelevant, skinSource);
    }

    public NpcProfile withKnowledge(Set<String> knowledge) {
        return new NpcProfile(npcId, title, category, role, functions, profession, faction, origin, personality,
                traits, knowledge, secrets, relationships, behavior, schedule, dialogueTreeId,
                storyRelevant, questRelevant, loreRelevant, skinSource);
    }

    public boolean supports(NpcFunction function) {
        return function != null && functions.contains(function);
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
                npc.id(), "", category, category.name().toLowerCase(), functionsForCategory(category, npc.profession()),
                npc.profession(), NpcFaction.NONE, "", "unbeschrieben", Set.of(), Set.of(), Set.of(), Set.of(),
                "resident", "resident", tree, category == NpcCategory.STORY,
                category == NpcCategory.QUEST, category == NpcCategory.SCHOLAR || category == NpcCategory.STORY, npc.skinSource()
        );
    }

    private static Set<NpcFunction> functionsForCategory(NpcCategory category, Profession profession) {
        if (category == null) return Set.of(NpcFunction.RECEPTION);
        return switch (category) {
            case QUEST -> Set.of(NpcFunction.DIALOG, NpcFunction.QUEST);
            case STORY -> Set.of(NpcFunction.DIALOG, NpcFunction.STORY);
            case TRAVELER -> Set.of(NpcFunction.DIALOG, NpcFunction.TRAVEL);
            case MERCHANT -> Set.of(NpcFunction.DIALOG, NpcFunction.SHOP);
            case SCHOLAR, FARMER, FISHERMAN, CRAFTSPERSON -> Set.of(NpcFunction.DIALOG, NpcFunction.PROFESSION);
            case RESIDENT, GUARD, ELDER, CHILD, SEEKER, FACTION -> Set.of(NpcFunction.DIALOG);
        };
    }
}
