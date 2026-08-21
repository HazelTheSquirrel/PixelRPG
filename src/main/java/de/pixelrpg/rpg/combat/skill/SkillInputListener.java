package de.pixelrpg.rpg.combat.skill;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class SkillInputListener implements Listener {
    private final WeaponAbilityEngine abilityEngine;

    public SkillInputListener(WeaponAbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    // Zuständig für die Aktivierung einer Waffenfähigkeit per Shift + Rechtsklick.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponAbility(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().getInventory().getItemInMainHand().hasItemMeta()) return;
        abilityEngine.cast(event.getPlayer());
    }
}
