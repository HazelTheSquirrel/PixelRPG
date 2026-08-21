package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.combat.gem.SkillGemCastEngine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class SkillInputListener implements Listener {
    private final SkillGemCastEngine castEngine;
    public SkillInputListener(SkillGemCastEngine castEngine) { this.castEngine = castEngine; }

    // Zuständig für die Aktivierung einer Waffenfähigkeit per Shift + Rechtsklick.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponAbility(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().getInventory().getItemInMainHand().hasItemMeta()) return;
        castEngine.cast(event.getPlayer());
    }
}
