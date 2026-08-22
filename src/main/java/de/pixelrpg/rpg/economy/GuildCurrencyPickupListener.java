package de.pixelrpg.rpg.economy;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.lang.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public final class GuildCurrencyPickupListener implements Listener {

    private final GuildAPI guildAPI;
    private final EconomyAPI economyAPI;
    private final LanguageManager lang;

    public GuildCurrencyPickupListener(GuildAPI guildAPI, EconomyAPI economyAPI) {
        this.guildAPI = guildAPI;
        this.economyAPI = economyAPI;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    // Zuständig für das automatische Einsammeln von physischem Gilden-Gold
    // (Sunflower-Items): storniert das Aufheben ins Inventar und schreibt den
    // Wert direkt dem virtuellen Guthaben des Spielers gut.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!GuildCurrencyItemFactory.isCurrency(event.getItem().getItemStack())) {
            return;
        }
        if (!guildAPI.isRegistered(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        double amount = GuildCurrencyItemFactory.readAmount(event.getItem().getItemStack())
                * event.getItem().getItemStack().getAmount();
        event.getItem().remove();
        economyAPI.deposit(player.getUniqueId(), amount);
        lang.send(player, "bank.currency-deposited", "amount", String.format("%.2f", amount));
    }
}