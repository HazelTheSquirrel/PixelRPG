package de.pixelrpg.rpg.npc;

import java.util.List;
import java.util.Objects;

public final class NpcIdentityService {
    private static final List<String> FIRST_NAMES = List.of(
            "Ari", "Bela", "Caro", "Dara", "Edda", "Falk", "Gero", "Heda",
            "Ivo", "Juna", "Kian", "Lena", "Mara", "Nilo", "Oda", "Pia",
            "Quin", "Rika", "Sven", "Tara", "Ulf", "Vera", "Wen", "Yara"
    );
    private static final List<String> FAMILY_NAMES = List.of(
            "Berg", "Feld", "Fluss", "Grimm", "Hain", "Kern", "Licht", "Moor",
            "Nord", "Reif", "Stein", "Tal", "Wald", "Weber", "Winter", "Wolke"
    );

    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;

    public NpcIdentityService(NpcManager npcManager, NpcProfileStore profileStore) {
        this.npcManager = Objects.requireNonNull(npcManager, "npcManager");
        this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
    }

    public void synchronize() {
        for (RPGNpc npc : npcManager.getAll()) {
            assignIdentity(npc);
        }
    }

    public void assignIdentity(RPGNpc npc) {
        if (npc == null) return;
        NpcProfile profile = profileStore.getOrCreate(npc);
        if (isPlaceholder(npc.name())) {
                String name = generatedName(npc.id());
                npcManager.rename(npc.id(), name);
                String title = profile.title().isBlank() ? defaultTitle(profile.category()) : profile.title();
                profile = profile.withIdentity(title, profile.category(), profile.role(), profile.faction(),
                        profile.origin(), profile.personality(), profile.traits(), profile.behavior(),
                        profile.schedule(), profile.dialogueTreeId());
            profileStore.put(profile);
        }
    }

    private boolean isPlaceholder(String name) {
        if (name == null || name.isBlank()) return true;
        return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "npc", "wanderer", "traveler", "wanderer npc" -> true;
            default -> false;
        };
    }

    private String defaultTitle(NpcCategory category) {
        return switch (category) {
            case FARMER -> "Bäuerin";
            case FISHERMAN -> "Fischer";
            case CRAFTSPERSON -> "Handwerker";
            case SCHOLAR -> "Gelehrter";
            case SEEKER -> "Suchender";
            case GUARD -> "Wächter";
            case TRAVELER -> "Reisender";
            case MERCHANT -> "Händler";
            case ELDER -> "Ältester";
            case CHILD -> "Kind";
            case STORY -> "Chronist";
            case QUEST -> "Auftraggeber";
            case FACTION -> "Fraktionsmitglied";
            default -> "Bewohner";
        };
    }

    private String generatedName(String npcId) {
        int hash = Math.abs(npcId.hashCode());
        String first = FIRST_NAMES.get(hash % FIRST_NAMES.size());
        String family = FAMILY_NAMES.get((hash / FIRST_NAMES.size()) % FAMILY_NAMES.size());
        return first + " " + family;
    }
}
