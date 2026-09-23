package de.pixelrpg.rpg.trade;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class TradeDepotDialogService {
    private final TradeDepotService depot;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;

    public TradeDepotDialogService(TradeDepotService depot, PlayerProfileManager profiles, DialogueEngine dialogue) {
        this.depot = depot;
        this.profiles = profiles;
        this.dialogue = dialogue;
    }

    public void open(Player player) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openUnavailable(player, "Handelsdepot", "Du musst registriertes Rathausmitglied sein.");
            return;
        }
        depot.claimPayout(player);
        openListings(player);
    }

    public void openListings(Player player) {
        List<TradeDepotListing> listings = depot.listings();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Öffentliche Angebote: " + listings.size(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("7 Tage Laufzeit · 5 % Verkaufsgebühr · gekaufte Ware landet im Handelsfach.", NamedTextColor.GRAY))
        );
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new java.util.ArrayList<>();
        int shown = Math.min(12, listings.size());
        for (int i = 0; i < shown; i++) {
            TradeDepotListing listing = listings.get(i);
            final UUID id = listing.id();
            boolean own = listing.sellerId().equals(player.getUniqueId());
            actions.add(dialogue.actionButton(Component.text((i + 1) + (own ? " zurücknehmen" : " kaufen")),
                    own ? NamedTextColor.YELLOW : NamedTextColor.GREEN,
                    target -> {
                        boolean success = own ? depot.cancel(target, id) : depot.purchase(target, id);
                        target.sendMessage(Component.text(success ? "Handelsaktion abgeschlossen." : "Handelsaktion nicht möglich.", success ? NamedTextColor.GREEN : NamedTextColor.RED));
                        openListings(target);
                    }));
            body.add(DialogBody.plainMessage(Component.text((i + 1) + ". " + displayName(listing.item()) + " · " + format(listing.price()) + " Gold", NamedTextColor.WHITE)));
        }
        actions.add(dialogue.actionButton(Component.text("Item einstellen"), NamedTextColor.GOLD, this::openCreate));
        actions.add(dialogue.actionButton(Component.text("Handelsfach abholen"), NamedTextColor.AQUA, this::claimGoods));
        dialogue.openMultiAction(player, Component.text("Handelsdepot", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openCreate(Player player) {
        DialogInput slot = DialogInput.text("slot", 240, Component.text("Inventar-Slot 0-35", NamedTextColor.WHITE), true, "", 2, null);
        DialogInput price = DialogInput.text("price", 240, Component.text("Verkaufspreis in Gold", NamedTextColor.WHITE), true, "", 16, null);
        dialogue.openTextInputAction(player, Component.text("Handelsware einstellen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Gib den Slot des kompletten Stapels an. Das Item wird für 7 Tage eingestellt.", NamedTextColor.GRAY))),
                slot, Component.text("Weiter", NamedTextColor.GREEN), NamedTextColor.GREEN,
                (target, response) -> {
                    String slotValue = response.getText("slot");
                    String priceValue = response.getText("price");
                    try {
                        int slotNumber = Integer.parseInt(slotValue == null ? "" : slotValue.trim());
                        double priceValueParsed = Double.parseDouble((priceValue == null ? "" : priceValue.trim()).replace(',', '.'));
                        if (depot.createListing(target, slotNumber, priceValueParsed)) target.sendMessage(Component.text("Handelsware eingestellt.", NamedTextColor.GREEN));
                        else target.sendMessage(Component.text("Handelsware konnte nicht eingestellt werden.", NamedTextColor.RED));
                    } catch (NumberFormatException exception) {
                        target.sendMessage(Component.text("Slot oder Preis ist ungültig.", NamedTextColor.RED));
                    }
                    openListings(target);
                });
    }

    private void claimGoods(Player player) {
        int moved = depot.claimTradeGoods(player);
        player.sendMessage(Component.text(moved + " Handelsstapel ins Inventar übertragen.", NamedTextColor.GREEN));
        openListings(player);
    }

    private String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return item.getType().key().value();
    }

    private String format(double amount) {
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
