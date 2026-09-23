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
import java.util.Locale;

public final class GuildCurrencyPickupListener implements Listener {
    private final GuildAPI guildAPI; private final EconomyAPI economyAPI; private final GuildCurrencyItemFactory currencyFactory;
    public GuildCurrencyPickupListener(GuildAPI guildAPI,EconomyAPI economyAPI,GuildCurrencyItemFactory currencyFactory){this.guildAPI=guildAPI;this.economyAPI=economyAPI;this.currencyFactory=currencyFactory;}
    // Converts physical guild currency pickups into the player's persisted balance.
    @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true)
    public void onPickup(EntityPickupItemEvent event){
        if(!(event.getEntity() instanceof Player player))return;
        ItemStackHolder holder=new ItemStackHolder(event.getItem().getItemStack());
        if(!currencyFactory.isCurrency(holder.item()))return;
        if(!guildAPI.isRegistered(player.getUniqueId()))return;
        event.setCancelled(true);double amount=currencyFactory.readAmount(holder.item())*holder.item().getAmount();event.getItem().remove();economyAPI.deposit(player.getUniqueId(),amount);
        player.sendMessage(Component.text("+"+String.format(Locale.ROOT,"%.2f",amount)+" Gold eingezahlt.",NamedTextColor.GREEN));
    }
    private record ItemStackHolder(org.bukkit.inventory.ItemStack item){}
}