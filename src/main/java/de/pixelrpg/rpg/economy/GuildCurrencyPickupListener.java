package de.pixelrpg.rpg.economy;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public final class GuildCurrencyPickupListener implements Listener {
    private final GuildAPI guildAPI;
    private final EconomyAPI economyAPI;

    public GuildCurrencyPickupListener(GuildAPI guildAPI, EconomyAPI economyAPI) {
        this.guildAPI = guildAPI;
        this.economyAPI = economyAPI;
    }

    // Zuständig für das automatische Einsammeln von physischem Gilden-Gold.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!GuildCurrencyItemFactory.isCurrency(event.getItem().getItemStack())) return;
        if (!guildAPI.isRegistered(player.getUniqueId())) return;
        event.setCancelled(true);
        double amount = GuildCurrencyItemFactory.readAmount(event.getItem().getItemStack()) * event.getItem().getItemStack().getAmount();
        event.getItem().remove();
        economyAPI.deposit(player.getUniqueId(), amount);
        player.sendMessage(Component.text("+" + String.format("%.2f", amount) + " Gold eingezahlt.", NamedTextColor.GREEN));
    }
}