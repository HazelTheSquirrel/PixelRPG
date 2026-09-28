package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.profession.Profession;

/** Central synchronized registry for global main-profession Grandmaster slots. */
public final class GrandmasterRegistry {
    private final NpcManager npcs;

    public GrandmasterRegistry(NpcManager npcs) {
        this.npcs = npcs;
    }

    public synchronized int count(Profession profession) {
        if (profession == null || !profession.isMain()) return 0;
        return (int) npcs.getAll().stream()
                .filter(npc -> npc.profession() == profession)
                .filter(npc -> npc.professionNpcRank().isGrandmaster())
                .count();
    }

    /**
     * Atomically checks and performs a main-profession Grandmaster promotion.
     * Gathering professions are intentionally outside the global cap.
     */
    public synchronized boolean promote(RPGNpc npc, int globalCap, Runnable promotion) {
        if (npc == null || npc.profession() == null || promotion == null) return false;
        if (!npc.profession().isMain()) {
            promotion.run();
            return true;
        }
        if (!npc.professionNpcRank().next().isGrandmaster()) {
            promotion.run();
            return true;
        }
        if (count(npc.profession()) >= Math.max(0, globalCap)) return false;
        promotion.run();
        return true;
    }
}
