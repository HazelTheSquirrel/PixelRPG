// src/main/java/de/pixelrpg/rpg/player/TitleDisplayService.java
package de.pixelrpg.rpg.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public final class TitleDisplayService {

    private final PlayerProfileManager profileManager;

    public TitleDisplayService(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public void apply(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            player.playerListName(Component.text(player.getName()));
            return;
        }

        String title = profile.getSelectedTitle();
        Component listName;
        if (title != null) {
            listName = Component.text("[" + title + "] ", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(player.getName(), NamedTextColor.WHITE));
        } else {
            listName = Component.text(player.getName());
        }

        player.playerListName(listName);
    }
}