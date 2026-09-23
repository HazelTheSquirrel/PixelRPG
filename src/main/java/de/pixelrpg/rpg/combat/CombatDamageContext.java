package de.pixelrpg.rpg.combat;

/** Synchronous combat context used to prevent weapon skills from adding their weapon power twice. */
public final class CombatDamageContext {
    private static final ThreadLocal<Boolean> WEAPON_SKILL = ThreadLocal.withInitial(() -> false);

    private CombatDamageContext() {
    }

    public static boolean isWeaponSkill() {
        return WEAPON_SKILL.get();
    }

    public static void runWeaponSkill(Runnable action) {
        boolean previous = WEAPON_SKILL.get();
        WEAPON_SKILL.set(true);
        try {
            action.run();
        } finally {
            WEAPON_SKILL.set(previous);
        }
    }
}
