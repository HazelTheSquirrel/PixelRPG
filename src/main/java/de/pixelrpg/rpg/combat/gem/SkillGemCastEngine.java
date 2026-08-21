package de.pixelrpg.rpg.combat.gem;

import de.pixelrpg.rpg.combat.skill.WeaponAbilityEngine;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.stats.StatEngine;
import org.bukkit.entity.Player;

/**
 * Temporary bootstrap compatibility wrapper. Socketed skill gems are removed;
 * weapon-bound abilities are handled by {@link WeaponAbilityEngine}.
 */
@Deprecated(forRemoval = true)
public final class SkillGemCastEngine {
    private final WeaponAbilityEngine weaponAbilityEngine;

    public SkillGemCastEngine(PlayerProfileManager profileManager, StatEngine statEngine, Object ignoredLegacyGemRepository) {
        this.weaponAbilityEngine = new WeaponAbilityEngine(profileManager, statEngine);
    }

    public void cast(Player player) {
        weaponAbilityEngine.cast(player);
    }
}
