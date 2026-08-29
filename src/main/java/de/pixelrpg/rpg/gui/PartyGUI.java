package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PartyGUI extends AbstractGUI {
    private final Player viewer;
    private final PartyManager partyManager;
    private final PlayerProfileManager profileManager;

    public PartyGUI(Player viewer, PartyManager partyManager, PlayerProfileManager profileManager) {
        super(54, Component.text("Party", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.partyManager = partyManager;
        this.profileManager = profileManager;
    }

    @Override protected void populate() {
        Optional<Party> partyOpt = partyManager.getParty(viewer.getUniqueId());
        if (partyOpt.isEmpty()) {
            setItem(22, buildActionItem(Material.LIME_DYE, "Party erstellen", NamedTextColor.GREEN), event -> {
                partyManager.createParty(viewer.getUniqueId());
                viewer.closeInventory();
            });
            return;
        }
        Party party = partyOpt.get();
        int slot = 0;
        for (UUID member : party.getMembers()) {
            if (slot >= 5) break;
            ItemStack item = buildMemberItem(party, member);
            UUID target = member;
            if (party.isLeader(viewer.getUniqueId()) && !target.equals(viewer.getUniqueId())) setItem(slot, item, event -> openMemberActions(target));
            else setItem(slot, item);
            slot++;
        }
        setItem(40, buildActionItem(Material.RED_DYE, party.isLeader(viewer.getUniqueId()) ? "Party auflösen" : "Party verlassen", NamedTextColor.RED), event -> {
            if (party.isLeader(viewer.getUniqueId())) partyManager.disbandParty(party);
            else partyManager.leaveParty(viewer);
            viewer.closeInventory();
        });
    }

    private void openMemberActions(UUID target) {
        DialogueEngine engine = new DialogueEngine();
        List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text("Mitglied verwalten", NamedTextColor.WHITE)), DialogBody.plainMessage(Component.text(name(target), NamedTextColor.GRAY)));
        ActionButton transfer = engine.actionButton(Component.text("Anführer übertragen", NamedTextColor.GOLD), NamedTextColor.GOLD, player -> {
            partyManager.transferLeadership(player, target);
            new PartyGUI(player, partyManager, profileManager).open(player);
        });
        ActionButton kick = engine.actionButton(Component.text("Spieler entfernen", NamedTextColor.RED), NamedTextColor.RED, player -> {
            partyManager.kick(player, target);
            new PartyGUI(player, partyManager, profileManager).open(player);
        });
        engine.openMultiAction(viewer, Component.text("Party-Mitglied"), body, List.of(transfer, kick), 1,
                player -> new PartyGUI(player, partyManager, profileManager).open(player));
    }

    private String name(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private ItemStack buildMemberItem(Party party, UUID member) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member);
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unbekannt";
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        boolean isLeader = party.isLeader(member);
        meta.displayName(Component.text(name, isLeader ? NamedTextColor.GOLD : NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(isLeader ? "Anführer" : "Mitglied", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(offlinePlayer.isOnline() ? "Online" : "Offline", offlinePlayer.isOnline() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}