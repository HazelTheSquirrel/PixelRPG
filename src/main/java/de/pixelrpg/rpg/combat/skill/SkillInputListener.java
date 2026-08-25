package de.pixelrpg.rpg.combat.skill;

import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class SkillInputListener implements Listener {
    private final WeaponAbilityEngine abilityEngine;

    public SkillInputListener(WeaponAbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    // Zuständig für die Aktivierung einer Nahkampf-Waffenfähigkeit per Rechtsklick; Fernkampfwaffen dürfen normal gespannt werden.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponAbility(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.isEmpty() || !item.hasItemMeta()) return;
        if (abilityEngine.isRangedWeapon(item.getType())) return;
        if (abilityEngine.cast(event.getPlayer())) event.setCancelled(true);
    }

    // Zuständig für das Auslösen der Bogen-/Armbrustfähigkeit genau beim Loslassen der Benutzungstaste.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRangedWeaponRelease(PlayerStopUsingItemEvent event) {
        abilityEngine.castReleased(event.getPlayer(), event.getItem(), event.getTicksHeldFor());
    }

    // Zuständig für die Freigabe temporärer Waffenfähigkeitsdaten beim Verlassen des Servers.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        abilityEngine.clearCooldown(event.getPlayer().getUniqueId());
    }
}
