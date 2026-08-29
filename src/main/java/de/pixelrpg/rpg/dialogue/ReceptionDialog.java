package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.gui.PartyGUI;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Native reception dialog used to register a player and expose guild and party entries. */
public final class ReceptionDialog {
    private final Player player;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final PartyManager partyManager;
    private final GuildManager guildManager;

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this(player, profileManager, dialogueEngine, null, null);
    }

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager) {
        this(player, profileManager, dialogueEngine, partyManager, null);
    }

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager, GuildManager guildManager) {
        this.player = player;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.partyManager = partyManager;
        this.guildManager = guildManager;
    }

    public void open() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegistered();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Willkommen. Hier kannst du dein PixelRPG-Profil registrieren und verwalten.", NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Status: ", NamedTextColor.GRAY).append(
                Component.text(registered ? "Mitglied" : "Nicht registriert", registered ? NamedTextColor.GREEN : NamedTextColor.RED))));

        List<ActionButton> actions = new ArrayList<>();
        if (!registered) {
            actions.add(dialogueEngine.actionButton(Component.text("Registrieren"), NamedTextColor.GREEN, target -> {
                profileManager.registerPlayer(target);
                new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
            }));
        } else {
            // Reception order: Registrierung aufheben, Party, Gilde, Seelenbindung, Scoreboard.
            actions.add(dialogueEngine.actionButton(Component.text("PixelRPG-Registrierung aufheben"), NamedTextColor.RED, this::openLeaveConfirmation));
            if (partyManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Party", NamedTextColor.AQUA), NamedTextColor.AQUA,
                        target -> new PartyGUI(target, partyManager, profileManager).open(target)));
            }
            if (guildManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Gilde", NamedTextColor.GOLD), NamedTextColor.GOLD,
                        target -> new GuildDialog(guildManager, profileManager, dialogueEngine).open(target)));
            }
            actions.add(dialogueEngine.actionButton(Component.text("Seelenbindung"), NamedTextColor.LIGHT_PURPLE, this::openSoulbindSelection));
            actions.add(dialogueEngine.actionButton(
                    Component.text(profile.isScoreboardEnabled() ? "Scoreboard ausschalten" : "Scoreboard einschalten", NamedTextColor.GOLD),
                    NamedTextColor.GOLD, this::toggleScoreboard));
        }
        dialogueEngine.openMultiAction(player, Component.text("RPG-Registrierung", NamedTextColor.GOLD), body, actions, 1);
    }

    private void toggleScoreboard(Player target) {
        var scoreboardService = PixelRPGPlugin.getInstance().getScoreboardService();
        if (scoreboardService == null) {
            new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
            return;
        }
        scoreboardService.setEnabled(target, !scoreboardService.isEnabled(target));
        new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
    }

    private void openSoulbindSelection(Player target) {
        List<ActionButton> actions = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < target.getInventory().getSize(); slot++) {
            ItemStack item = target.getInventory().getItem(slot);
            if (!isSoulbindCandidate(item)) continue;
            final int itemSlot = slot;
            slots.add(itemSlot);
            actions.add(dialogueEngine.actionButton(itemLabel(item, itemSlot), NamedTextColor.LIGHT_PURPLE, player -> openSoulbindConfirmation(player, itemSlot)));
        }
        if (actions.isEmpty()) {
            dialogueEngine.openNotice(target, Component.text("Seelenbindung", NamedTextColor.GOLD), Component.text("Lege zuerst ein identifiziertes Item in den mittleren Slot.", NamedTextColor.WHITE), Component.text("Abbrechen", NamedTextColor.GRAY));
            return;
        }
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle ein identifiziertes PixelRPG-Item aus deinem Inventar. Bereits seelengebundene Items werden nicht angezeigt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Verfügbare Items: " + slots.size(), NamedTextColor.GRAY)));
        dialogueEngine.openMultiAction(target, Component.text("Seelenbindung", NamedTextColor.GOLD), body, actions, 1,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open());
    }

    private void openSoulbindConfirmation(Player target, int slot) {
        ItemStack item = target.getInventory().getItem(slot);
        if (!isSoulbindCandidate(item)) { openSoulbindSelection(target); return; }
        Component itemName = itemLabel(item, slot);
        ActionButton yes = dialogueEngine.actionButton(Component.text("Seelenbinden", NamedTextColor.LIGHT_PURPLE), NamedTextColor.LIGHT_PURPLE, player -> soulbindItem(player, slot));
        ActionButton no = dialogueEngine.actionButton(Component.text("Abbrechen"), NamedTextColor.GRAY, this::openSoulbindSelection);
        dialogueEngine.openConfirmation(target, Component.text("Seelenbindung", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Möchtest du " + plainName(itemName) + " wirklich seelenbinden? Diese Entscheidung kann nicht rückgängig gemacht werden.", NamedTextColor.WHITE))), yes, no);
    }

    private void soulbindItem(Player target, int slot) {
        ItemStack item = target.getInventory().getItem(slot);
        if (!isSoulbindCandidate(item)) { openSoulbindSelection(target); return; }
        SoulboundService.Result result = SoulboundService.apply(item);
        switch (result) {
            case SUCCESS -> target.sendMessage(Component.text("Item ist jetzt seelengebunden!", NamedTextColor.GREEN));
            case ALREADY_SOULBOUND -> target.sendMessage(Component.text("Dieses Item ist bereits seelengebunden.", NamedTextColor.RED));
            case NOT_IDENTIFIED -> target.sendMessage(Component.text("Nur identifizierte Items können seelengebunden werden.", NamedTextColor.RED));
        }
    }

    private boolean isSoulbindCandidate(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return Boolean.TRUE.equals(pdc.get(RPGKeys.Item.identified(), org.bukkit.persistence.PersistentDataType.BOOLEAN)) && !SoulboundService.isSoulbound(item);
    }

    private Component itemLabel(ItemStack item, int slot) {
        Component name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().displayName() : Component.text(item.getType().key().value());
        return name.decoration(TextDecoration.ITALIC, false).append(Component.text(" ×" + item.getAmount() + " [Slot " + (slot + 1) + "]", NamedTextColor.GRAY));
    }

    private String plainName(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void openLeaveConfirmation(Player target) {
        ActionButton yes = dialogueEngine.actionButton(Component.text("Ja, Registrierung unwiderruflich aufheben"), NamedTextColor.RED, player -> {
            profileManager.unregisterPlayer(player);
            player.sendMessage(Component.text("Deine PixelRPG-Registrierung wurde aufgehoben. Dein Fortschritt wurde gelöscht.", NamedTextColor.GREEN));
        });
        ActionButton no = dialogueEngine.actionButton(Component.text("Nein, abbrechen"), NamedTextColor.GREEN,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open());
        dialogueEngine.openConfirmation(target, Component.text("Registrierung aufheben?", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Warnung: Setzt deinen gesamten PixelRPG-Fortschritt zurück.", NamedTextColor.WHITE))), yes, no);
    }
}
