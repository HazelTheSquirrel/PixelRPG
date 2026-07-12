// src/main/java/de/pixelrpg/rpg/combat/skill/SkillInputListener.java (VOLLSTÄNDIG, ersetzt alte Datei — eine Taste, ein Skill)
package de.pixelrpg.rpg.combat.skill;

import de.pixelrpg.rpg.combat.gem.SkillGemCastEngine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SkillInputListener implements Listener {

    private final SkillGemCastEngine castEngine;

    public SkillInputListener(SkillGemCastEngine castEngine) {
        this.castEngine = castEngine;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        castEngine.cast(event.getPlayer());
    }
}