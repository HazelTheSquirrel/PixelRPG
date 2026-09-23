package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.item.SoulboundService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SoulboundDeathListener implements Listener {
    private final GuildAPI guildAPI;

    public SoulboundDeathListener(GuildAPI guildAPI) {
        this.guildAPI = guildAPI;
    }

    // Zuständig dafür, dass Soulbound-Items beim Tod sofort im Inventar des Spielers verbleiben.
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!guildAPI.isRegistered(player.getUniqueId())) return;

        List<ItemStack> drops = event.getDrops();
        List<ItemStack> keep = event.getItemsToKeep();
        for (ItemStack item : new ArrayList<>(drops)) {
            if (!SoulboundService.isSoulbound(item)) continue;
            drops.remove(item);
            keep.add(item.clone());
        }
    }
}
