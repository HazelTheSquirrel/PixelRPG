package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.guild.GuildBankService;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Connects BANKER NPCs to the current rebuild account and currency system. */
public final class BankerNpcListener implements Listener {
    private final NpcRuntimeManager npcs;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogs;
    private final GuildBankService guildBank;

    public BankerNpcListener(NpcRuntimeManager npcs, PlayerProfileManager profiles, DialogueEngine dialogs, GuildBankService guildBank) {
        this.npcs = npcs;
        this.profiles = profiles;
        this.dialogs = dialogs;
        this.guildBank = guildBank;
    }

    // Opens the account dialog for a main-hand interaction with a BANKER NPC.
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var npc = npcs.getByEntity(event.getRightClicked().getUniqueId()).orElse(null);
        if (npc == null || npc.type() != NpcType.BANKER) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void open(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogs.openUnavailable(player, "Bank", "Du musst zuerst registriert sein.");
            return;
        }
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        actions.add(dialogs.actionButton(Component.text("Gildenbank öffnen"), NamedTextColor.AQUA, guildBank::open));
        for (long amount : List.of(10L, 100L, 500L)) {
            actions.add(dialogs.actionButton(Component.text(amount + " Gold einzahlen"), NamedTextColor.GREEN,
                    target -> deposit(target, amount)));
            actions.add(dialogs.actionButton(Component.text(amount + " Gold auszahlen"), NamedTextColor.YELLOW,
                    target -> withdraw(target, amount)));
        }
        dialogs.openMultiAction(player, Component.text("Bank", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Kontostand: " + format(profile.getMoney()) + " Gold")),
                        DialogBody.plainMessage(Component.text("Einzahlungen nutzen physische Guild-Währung aus deinem Inventar."))),
                actions, 2);
    }

    private void deposit(Player player, long amount) {
        double removed = 0.0D;
        for (int slot = 0; slot < player.getInventory().getSize() && removed < amount; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!GuildCurrencyItemFactory.isCurrency(item)) continue;
            double unit = GuildCurrencyItemFactory.readAmount(item);
            if (unit <= 0.0D) continue;
            int take = Math.min(item.getAmount(), Math.max(1, (int) Math.ceil((amount - removed) / unit)));
            removed += unit * take;
            if (take >= item.getAmount()) player.getInventory().setItem(slot, null);
            else item.setAmount(item.getAmount() - take);
        }
        if (removed > 0.0D) {
            double deposited = removed;
            profile(player).ifPresent(p -> p.addMoney(deposited));
            profiles.saveProfileAsync(player.getUniqueId());
        }
        open(player);
    }

    private void withdraw(Player player, long amount) {
        PlayerProfile profile = profile(player).orElse(null);
        if (profile == null || !profile.removeMoney(amount)) {
            dialogs.openUnavailable(player, "Bank", "Nicht genügend Gold auf dem Konto.");
            return;
        }
        for (ItemStack stack : GuildCurrencyItemFactory.createStacks(amount)) {
            player.getInventory().addItem(stack);
        }
        profiles.saveProfileAsync(player.getUniqueId());
        open(player);
    }

    private java.util.Optional<PlayerProfile> profile(Player player) {
        return profiles.getProfile(player.getUniqueId());
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
