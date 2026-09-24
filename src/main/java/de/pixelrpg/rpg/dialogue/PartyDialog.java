package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Native Paper dialog flow for party creation and party management. */
public final class PartyDialog {
    private final PartyManager parties;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    private final InviteDialogService invites;
    private final Consumer<Player> backAction;

    public PartyDialog(PartyManager parties, PlayerProfileManager profiles, DialogueEngine dialogue, InviteDialogService invites) {
        this(parties, profiles, dialogue, invites, Player::closeDialog);
    }

    public PartyDialog(PartyManager parties, PlayerProfileManager profiles, DialogueEngine dialogue, InviteDialogService invites, Consumer<Player> backAction) {
        this.parties = parties;
        this.profiles = profiles;
        this.dialogue = dialogue;
        this.invites = invites;
        this.backAction = backAction;
    }

    public void open(Player player) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openNotice(player, Component.text("Party", NamedTextColor.GOLD),
                    Component.text("Du musst zuerst dein PixelRPG-Profil registrieren.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        parties.getParty(player.getUniqueId()).ifPresentOrElse(
                party -> openOverview(player, party),
                () -> openCreation(player)
        );
    }

    private void openCreation(Player player) {
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = List.of(
                dialogue.actionButton(Component.text("Party gründen", NamedTextColor.GREEN), NamedTextColor.GREEN, target -> {
                    parties.createParty(target.getUniqueId());
                    openOverview(target, parties.getParty(target.getUniqueId()).orElseThrow());
                }),
                dialogue.actionButton(Component.text("Zurück", NamedTextColor.WHITE), NamedTextColor.WHITE, backAction)
        );
        dialogue.openMultiAction(player, Component.text("Party", NamedTextColor.GOLD),
                List.of(DialogBodyFactory.message("Du bist aktuell in keiner Party.")),
                actions, 1);
    }

    private void openOverview(Player player, Party party) {
        boolean leader = party.isLeader(player.getUniqueId());
        List<io.papermc.paper.registry.data.dialog.body.DialogBody> body = new ArrayList<>();
        body.add(DialogBodyFactory.message("Mitglieder: " + party.getMembers().size() + "/" + Party.MAX_MEMBERS));
        body.add(DialogBodyFactory.message("Anführer: " + name(party.getLeader())));

        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        if (leader) {
            actions.add(dialogue.actionButton(Component.text("Spieler einladen", NamedTextColor.GREEN), NamedTextColor.GREEN,
                    target -> invites.openPartyInviteInput(target)));
        }

        for (UUID member : party.getMembers()) {
            if (member.equals(player.getUniqueId()) || !leader) continue;
            actions.add(dialogue.actionButton(Component.text("Verwalten: " + name(member), NamedTextColor.AQUA),
                    NamedTextColor.AQUA, target -> openMemberActions(target, member)));
        }

        actions.add(dialogue.actionButton(
                Component.text(leader ? "Party auflösen" : "Party verlassen", NamedTextColor.RED),
                NamedTextColor.RED,
                target -> {
                    if (leader) parties.disbandParty(party);
                    else parties.leaveParty(target);
                    target.closeDialog();
                }));
        actions.add(dialogue.actionButton(Component.text("Zurück", NamedTextColor.WHITE), NamedTextColor.WHITE, backAction));
        dialogue.openMultiAction(player, Component.text("Party", NamedTextColor.GOLD), body, actions, 2);
    }

    private void openMemberActions(Player player, UUID target) {
        Party party = parties.getParty(player.getUniqueId()).orElse(null);
        if (party == null || !party.isLeader(player.getUniqueId()) || !party.isMember(target)) {
            open(player);
            return;
        }
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = List.of(
                dialogue.actionButton(Component.text("Anführer übertragen", NamedTextColor.GOLD), NamedTextColor.GOLD, next -> {
                    parties.transferLeadership(next, target);
                    open(next);
                }),
                dialogue.actionButton(Component.text("Spieler entfernen", NamedTextColor.RED), NamedTextColor.RED, next -> {
                    parties.kick(next, target);
                    open(next);
                }),
                dialogue.actionButton(Component.text("Zurück", NamedTextColor.WHITE), NamedTextColor.WHITE, this::open)
        );
        dialogue.openMultiAction(player, Component.text("Party-Mitglied", NamedTextColor.GOLD),
                List.of(DialogBodyFactory.message(name(target))), actions, 1);
    }

    private String name(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString().substring(0, 8) : player.getName();
    }

    private static final class DialogBodyFactory {
        private DialogBodyFactory() {}

        private static io.papermc.paper.registry.data.dialog.body.DialogBody message(String text) {
            return io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(Component.text(text, NamedTextColor.WHITE));
        }
    }
}
