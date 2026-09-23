package de.pixelrpg.rpg.shop;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.trade.TradeDepotDialogService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ShopDialogService {
    private final ShopService shops;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    private final TradeDepotDialogService tradeDepot;

    public ShopDialogService(ShopService shops, PlayerProfileManager profiles, DialogueEngine dialogue, TradeDepotDialogService tradeDepot) {
        this.shops = shops;
        this.profiles = profiles;
        this.dialogue = dialogue;
        this.tradeDepot = tradeDepot;
    }

    public void open(Player player, String npcId) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openUnavailable(player, "Händler", "Du musst registriertes Rathausmitglied sein.");
            return;
        }
        List<ShopEntry> entries = shops.entries(npcId);
        if (entries.isEmpty()) {
            dialogue.openMultiAction(
                    player,
                    Component.text("Händler", NamedTextColor.GOLD),
                    List.of(DialogBody.plainMessage(Component.text("Dieser Händler hat derzeit keine Waren.", NamedTextColor.WHITE))),
                    List.of(dialogue.actionButton(Component.text("Handelsdepot"), NamedTextColor.AQUA, tradeDepot::open)),
                    1
            );
            return;
        }
        openPage(player, npcId, entries, 0);
    }

    private void openPage(Player player, String npcId, List<ShopEntry> entries, int page) {
        int from = page * 6;
        int to = Math.min(entries.size(), from + 6);
        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();
        for (int index = from; index < to; index++) {
            ShopEntry entry = entries.get(index);
            body.add(DialogBody.plainMessage(Component.text(
                    (index + 1) + ". " + displayName(entry.item()) + " • Kauf " + format(entry.buyPrice()) + " • Verkauf " + format(entry.sellPrice()),
                    NamedTextColor.WHITE)));
            actions.add(dialogue.actionButton(Component.text((index + 1) + " kaufen"), NamedTextColor.GREEN,
                    target -> {
                        boolean success = shops.purchase(target, entry);
                        target.sendMessage(Component.text(success ? "Gekauft." : "Kauf nicht möglich: Inventar oder Gold prüfen.",
                                success ? NamedTextColor.GREEN : NamedTextColor.RED));
                        openPage(target, npcId, entries, page);
                    }));
            actions.add(dialogue.actionButton(Component.text((index + 1) + " verkaufen"), NamedTextColor.GOLD,
                    target -> {
                        boolean success = shops.sell(target, entry);
                        target.sendMessage(Component.text(success ? "Verkauft." : "Du besitzt dieses Item nicht.",
                                success ? NamedTextColor.GREEN : NamedTextColor.RED));
                        openPage(target, npcId, entries, page);
                    }));
        }
        if (page > 0) actions.add(dialogue.actionButton(Component.text("Vorherige Seite"), NamedTextColor.WHITE,
                target -> openPage(target, npcId, entries, page - 1)));
        if (to < entries.size()) actions.add(dialogue.actionButton(Component.text("Nächste Seite"), NamedTextColor.WHITE,
                target -> openPage(target, npcId, entries, page + 1)));
        actions.add(dialogue.actionButton(Component.text("Handelsdepot"), NamedTextColor.AQUA, tradeDepot::open));
        dialogue.openMultiAction(player, Component.text("Shop", NamedTextColor.GOLD), body, actions, 2);
    }

    private String displayName(org.bukkit.inventory.ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return item.getType().key().value();
    }

    private String format(double amount) {
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
