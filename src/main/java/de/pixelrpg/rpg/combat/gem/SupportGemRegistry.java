// src/main/java/de/pixelrpg/rpg/combat/gem/SupportGemRegistry.java (ersetzt die alten registerSupport()-Aufrufe aus SkillGemRegistry.java)
package de.pixelrpg.rpg.combat.gem;

import java.util.HashMap;
import java.util.Map;

public final class SupportGemRegistry {

    private static final Map<String, SupportGemModifiers> MODIFIERS = new HashMap<>();

    static {
        MODIFIERS.put("fire_aura", new SupportGemModifiers(1.0, 1.0, true, false, 0.0, 0.0));
        MODIFIERS.put("winters_grasp", new SupportGemModifiers(1.0, 1.0, false, true, 0.0, 0.0));
        MODIFIERS.put("vampiric_touch", new SupportGemModifiers(1.0, 1.0, false, false, 0.0, 12.0));
        MODIFIERS.put("whirlwind_spread", new SupportGemModifiers(0.85, 1.0, false, false, 3.0, 0.0));
        MODIFIERS.put("swift_casting", new SupportGemModifiers(1.0, 0.75, false, false, 0.0, 0.0));
        MODIFIERS.put("empowered", new SupportGemModifiers(1.30, 1.15, false, false, 0.0, 0.0));
    }

    private SupportGemRegistry() {
    }

    public static SupportGemModifiers get(String id) {
        return MODIFIERS.get(id);
    }

    public static boolean isSupportGem(String id) {
        return MODIFIERS.containsKey(id);
    }

    public static java.util.Set<String> getAllIds() {
        return MODIFIERS.keySet();
    }
}