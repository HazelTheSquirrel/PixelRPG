package de.pixelrpg.rpg.shop;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ShopDialogService {
    private final ShopService shops;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;

    public ShopDialogService(ShopService shops, PlayerProfileManager profiles, DialogueEngine dialogue) {
        this.shops = shops;
        this.profiles = profiles;
        this.dialogue = dialogue;
    }

    public void open(Player player, String npcId) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openUnavailable(player, "Händler", "Du musst registriertes Rathausmitglied sein.");
            return;
        }
        List<ShopEntry> entries = shops.entries(npcId);
        if (entries.isEmpty()) {
            dialogue.openUnavailable(player, "Händler", "Dieser Händler hat derzeit keine Waren.");
            return;
        }
        openPage(player, npcId, entries, 0);
    }

    private void openPage(Player player, String npcId, List<ShopEntry> entries, int page) {
        int from = page * 8;
        int to = Math.min(entries.size(), from + 8);
        List<DialogBody> body = new ArrayList<>();
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        for (int index = from; index < to; index++) {
            ShopEntry entry = entries.get(index);
            body.add(DialogBody.plainMessage(Component.text(
                    (index + 1) + ". " + displayName(entry.item()) + " • Kauf " + format(entry.buyPrice()) + " • Verkauf " + format(entry.sellPrice()),
                    NamedTextColor.WHITE)));
            final int entryIndex = index;
            actions.add(dialogue.actionButton(Component.text((index + 1) + " kaufen"), NamedTextColor.GREEN,
                    target -> {
                        if (shops.purchase(target, entry)) {
                            target.sendMessage(Component.text("Gekauft.", NamedTextColor.GREEN));
                        } else {
                            target.sendMessage(Component.text("Kauf nicht möglich: Inventar oder Gold prüfen.", NamedTextColor.RED));
                        }
                        openPage(target, npcId, entries, page);
                    }));
            actions.add(dialogue.actionButton(Component.text((index + 1) + " verkaufen"), NamedTextColor.GOLD,
                    target -> {
                        if (shops.sell(target, entry)) {
                            target.sendMessage(Component.text("Verkauft.", NamedTextColor.GREEN));
                        } else {
                            target.sendMessage(Component.text("Du besitzt dieses Item nicht.", NamedTextColor.RED));
                        }
                        openPage(target, npcId, entries, page);
                    }));
        }
        if (page > 0) actions.add(dialogue.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> openPage(target, npcId, entries, page - 1)));
        if (to < entries.size()) actions.add(dialogue.actionButton(Component.text("Nächste Seite"), NamedTextColor.WHITE, target -> openPage(target, npcId, entries, page + 1)));
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
