package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;
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
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
        this.partyManager = partyManager;
        this.guildManager = guildManager;
    }

    public void open() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegistered();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Willkommen. Hier kannst du dein PixelRPG-Profil registrieren und verwalten.", NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(lang.get("reception.status").append(
                registered ? lang.get("reception.status-member").color(NamedTextColor.GREEN) : lang.get("reception.status-not-registered").color(NamedTextColor.RED))));

        List<ActionButton> actions = new ArrayList<>();
        if (!registered) {
            actions.add(dialogueEngine.actionButton(lang.get("reception.register-button"), NamedTextColor.GREEN, target -> {
                profileManager.registerPlayer(target);
                new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
            }));
        } else {
            actions.add(dialogueEngine.actionButton(
                    Component.text(profile.isScoreboardEnabled() ? "Scoreboard ausschalten" : "Scoreboard einschalten", NamedTextColor.GOLD),
                    NamedTextColor.GOLD, this::toggleScoreboard));
            if (partyManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Party", NamedTextColor.AQUA), NamedTextColor.AQUA,
                        target -> new PartyGUI(target, partyManager, profileManager).open(target)));
            }
            if (guildManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Gilde", NamedTextColor.GOLD), NamedTextColor.GOLD,
                        target -> new GuildDialog(guildManager, profileManager, dialogueEngine).open(target)));
            }
            actions.add(dialogueEngine.actionButton(lang.get("reception.resign-button"), NamedTextColor.RED, this::openLeaveConfirmation));
            actions.add(dialogueEngine.actionButton(lang.get("blacksmith.soulbind-button"), NamedTextColor.LIGHT_PURPLE, this::openSoulbindSelection));
        }
        dialogueEngine.openMultiAction(player, lang.get("reception.guild-reception-title"), body, actions, 1);
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
            dialogueEngine.openNotice(target, lang.get("blacksmith.soulbind-button"), lang.get("blacksmith.place-identified"), lang.get("reception.no-cancel"));
            return;
        }
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle ein identifiziertes PixelRPG-Item aus deinem Inventar. Bereits seelengebundene Items werden nicht angezeigt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Verfügbare Items: " + slots.size(), NamedTextColor.GRAY)));
        dialogueEngine.openMultiAction(target, lang.get("blacksmith.soulbind-button"), body, actions, 1,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open());
    }

    private void openSoulbindConfirmation(Player target, int slot) {
        ItemStack item = target.getInventory().getItem(slot);
        if (!isSoulbindCandidate(item)) { openSoulbindSelection(target); return; }
        Component itemName = itemLabel(item, slot);
        ActionButton yes = dialogueEngine.actionButton(Component.text("Seelenbinden", NamedTextColor.LIGHT_PURPLE), NamedTextColor.LIGHT_PURPLE, player -> soulbindItem(player, slot));
        ActionButton no = dialogueEngine.actionButton(lang.get("reception.no-cancel"), NamedTextColor.GRAY, this::openSoulbindSelection);
        dialogueEngine.openConfirmation(target, lang.get("blacksmith.soulbind-button"),
                List.of(DialogBody.plainMessage(Component.text("Möchtest du " + plainName(itemName) + " wirklich seelenbinden? Diese Entscheidung kann nicht rückgängig gemacht werden.", NamedTextColor.WHITE))), yes, no);
    }

    private void soulbindItem(Player target, int slot) {
        ItemStack item = target.getInventory().getItem(slot);
        if (!isSoulbindCandidate(item)) { openSoulbindSelection(target); return; }
        SoulboundService.Result result = SoulboundService.apply(item);
        switch (result) {
            case SUCCESS -> lang.send(target, "blacksmith.now-soulbound");
            case ALREADY_SOULBOUND -> lang.send(target, "blacksmith.already-soulbound");
            case NOT_IDENTIFIED -> lang.send(target, "blacksmith.only-identified-soulbind");
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
        ActionButton yes = dialogueEngine.actionButton(lang.get("reception.yes-resign"), NamedTextColor.RED, player -> {
            profileManager.unregisterPlayer(player);
            lang.send(player, "reception.left-guild");
        });
        ActionButton no = dialogueEngine.actionButton(lang.get("reception.no-cancel"), NamedTextColor.GREEN,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open());
        dialogueEngine.openConfirmation(target, lang.get("reception.resign-title"),
                List.of(DialogBody.plainMessage(lang.get("reception.resign-warning").color(NamedTextColor.WHITE))), yes, no);
    }
}
