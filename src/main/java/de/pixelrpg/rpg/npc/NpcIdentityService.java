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
            NpcProfile profile = profileStore.getOrCreate(npc);
            if (isPlaceholder(npc.name())) {
                String name = generatedName(npc.id());
                npcManager.rename(npc.id(), name);
                profile = profile.withIdentity(profile.title(), profile.category(), profile.role(), profile.faction(),
                        profile.origin(), profile.personality(), profile.traits(), profile.behavior(),
                        profile.schedule(), profile.dialogueTreeId());
                profileStore.put(profile);
            }
        }
    }

    private boolean isPlaceholder(String name) {
        return name == null || name.isBlank() || name.equalsIgnoreCase("NPC");
    }

    private String generatedName(String npcId) {
        int hash = Math.abs(npcId.hashCode());
        String first = FIRST_NAMES.get(hash % FIRST_NAMES.size());
        String family = FAMILY_NAMES.get((hash / FIRST_NAMES.size()) % FAMILY_NAMES.size());
        return first + " " + family;
    }
}
