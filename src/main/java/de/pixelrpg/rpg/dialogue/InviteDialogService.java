package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.api.events.PlayerCombatExitEvent;
import de.pixelrpg.rpg.combat.CombatStateService;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Shared native-dialog workflow for guild and party invitations. */
public final class InviteDialogService implements Listener {
    private final Plugin plugin;
    private final GuildManager guilds;
    private final PartyManager parties;
    private final PlayerProfileManager profiles;
    private final CombatStateService combat;

    public InviteDialogService(Plugin plugin, GuildManager guilds, PartyManager parties, PlayerProfileManager profiles, CombatStateService combat) {
        this.plugin = plugin;
        this.guilds = guilds;
        this.parties = parties;
        this.profiles = profiles;
        this.combat = combat;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGuildInviteInput(Player player) {
        openPlayerInput(player, InviteType.GUILD);
    }

    public void openPartyInviteInput(Player player) {
        openPlayerInput(player, InviteType.PARTY);
    }

    private void openPlayerInput(Player player, InviteType type) {
        DialogInput input = DialogInput.text("player_name", 320, Component.text("Spielername", NamedTextColor.WHITE), true, "", 16, null);
        ActionButton invite = ActionButton.builder(Component.text("Einladen", NamedTextColor.GREEN))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) handleInput(target, type, response);
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220).build();
        ActionButton back = ActionButton.builder(Component.text("Zurück", NamedTextColor.GRAY))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) target.closeDialog();
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220).build();

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Gib den vollständigen Spielernamen oder den Anfang des Namens ein.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Eine automatische Tab-Vervollständigung bietet das native Dialog-System nicht.", NamedTextColor.GRAY))
        );

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text(type == InviteType.GUILD ? "Gilde – Spieler einladen" : "Party – Spieler einladen", NamedTextColor.GOLD))
                    .body(body).inputs(List.of(input)).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(List.of(invite, back), null, 2));
        }));
    }

    private void handleInput(Player inviter, InviteType type, DialogResponseView response) {
        String raw = response.getText("player_name");
        if (raw == null || raw.isBlank()) {
            inviter.sendMessage(Component.text("Bitte gib einen Spielernamen ein.", NamedTextColor.RED));
            return;
        }
        List<Player> matches = resolveOnlinePlayers(raw.trim(), inviter);
        if (matches.isEmpty()) {
            inviter.sendMessage(Component.text("Kein passender Spieler ist online.", NamedTextColor.RED));
            return;
        }
        if (matches.size() > 1) {
            openSelection(inviter, type, matches);
            return;
        }
        sendInvite(inviter, matches.getFirst(), type);
    }

    private List<Player> resolveOnlinePlayers(String input, Player inviter) {
        String normalized = input.toLowerCase(Locale.ROOT);
        List<Player> exact = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.getUniqueId().equals(inviter.getUniqueId()))
                .filter(player -> player.getName().equalsIgnoreCase(input))
                .toList();
        if (!exact.isEmpty()) return exact;
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.getUniqueId().equals(inviter.getUniqueId()))
                .filter(player -> player.getName().toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void openSelection(Player inviter, InviteType type, List<Player> matches) {
        List<ActionButton> actions = new ArrayList<>();
        for (Player target : matches) {
            actions.add(action(Component.text(target.getName(), NamedTextColor.AQUA), selected -> sendInvite(selected, target, type)));
        }
        actions.add(action(Component.text("Zurück", NamedTextColor.GRAY), Player::closeDialog));
        inviter.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Spieler auswählen", NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text("Mehrere Spieler passen zu deiner Eingabe. Wähle den gewünschten Spieler.", NamedTextColor.WHITE))))
                    .canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(actions, null, 1));
        }));
    }

    private void sendInvite(Player inviter, Player target, InviteType type) {
        if (!profiles.isRegistered(target.getUniqueId())) {
            inviter.sendMessage(Component.text("Der Spieler ist noch nicht registriert.", NamedTextColor.RED));
            return;
        }

        boolean success;
        if (type == InviteType.GUILD) {
            success = guilds.invite(inviter, target) == GuildManager.Result.SUCCESS;
        } else {
            Party party = parties.getParty(inviter.getUniqueId()).orElseGet(() -> parties.createParty(inviter.getUniqueId()));
            if (!party.isLeader(inviter.getUniqueId())) {
                inviter.sendMessage(Component.text("Nur der Gruppenanführer kann einladen.", NamedTextColor.RED));
                return;
            }
            success = parties.addInvite(target.getUniqueId(), inviter.getUniqueId());
        }

        if (!success) {
            inviter.sendMessage(Component.text(type == InviteType.GUILD ? "Die Gildeneinladung konnte nicht gesendet werden." : "Die Partyeinladung konnte nicht gesendet werden.", NamedTextColor.RED));
            return;
        }
        inviter.sendMessage(Component.text("Einladung an " + target.getName() + " gesendet.", NamedTextColor.GREEN));
        queueOrShow(target);
    }

    private void queueOrShow(Player target) {
        if (combat.isInCombat(target.getUniqueId())) {
            target.sendMessage(Component.text("Du hast eine neue Einladung erhalten. Sie wird dir nach dem Kampf angezeigt.", NamedTextColor.YELLOW));
            return;
        }
        showPending(target);
    }

    private void showPending(Player target) {
        if (!target.isOnline() || combat.isInCombat(target.getUniqueId())) return;

        Guild guild = guilds.getInvitationGuild(target.getUniqueId()).orElse(null);
        if (guild != null) {
            showGuildInvitation(target, guild);
            return;
        }

        UUID leaderId = parties.getInviteLeader(target.getUniqueId()).orElse(null);
        if (leaderId != null) {
            Party party = parties.getParty(leaderId).orElse(null);
            if (party != null) showPartyInvitation(target, leaderId);
        }
    }

    private void showGuildInvitation(Player target, Guild guild) {
        ActionButton accept = action(Component.text("Annehmen", NamedTextColor.GREEN), player -> {
            GuildManager.Result result = guilds.acceptInvitation(player);
            player.sendMessage(Component.text(result == GuildManager.Result.SUCCESS ? "Du bist der Gilde „" + guild.name() + "“ beigetreten." : "Die Gildeneinladung ist nicht mehr gültig.", result == GuildManager.Result.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED));
            player.closeDialog();
            showPending(player);
        });
        ActionButton decline = action(Component.text("Ablehnen", NamedTextColor.RED), player -> {
            guilds.declineInvitation(player.getUniqueId());
            player.sendMessage(Component.text("Die Gildeneinladung wurde abgelehnt.", NamedTextColor.YELLOW));
            player.closeDialog();
            showPending(player);
        });
        showInvitationDialog(target, "Gildeneinladung", "Spieler „" + resolveName(guild.leaderId()) + "“ lädt dich in die Gilde „" + guild.name() + "“ ein.", accept, decline);
    }

    private void showPartyInvitation(Player target, UUID leaderId) {
        ActionButton accept = action(Component.text("Annehmen", NamedTextColor.GREEN), player -> {
            boolean success = parties.acceptInvite(player);
            player.sendMessage(Component.text(success ? "Du bist der Party beigetreten." : "Die Partyeinladung ist nicht mehr gültig.", success ? NamedTextColor.GREEN : NamedTextColor.RED));
            player.closeDialog();
            showPending(player);
        });
        ActionButton decline = action(Component.text("Ablehnen", NamedTextColor.RED), player -> {
            parties.removeInvite(player.getUniqueId());
            player.sendMessage(Component.text("Die Partyeinladung wurde abgelehnt.", NamedTextColor.YELLOW));
            player.closeDialog();
            showPending(player);
        });
        showInvitationDialog(target, "Partyeinladung", "Spieler „" + resolveName(leaderId) + "“ lädt dich in seine Party ein.", accept, decline);
    }

    private void showInvitationDialog(Player target, String title, String message, ActionButton accept, ActionButton decline) {
        target.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text(title, NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text(message, NamedTextColor.WHITE))))
                    .canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(List.of(accept, decline), null, 2));
        }));
    }

    private ActionButton action(Component label, java.util.function.Consumer<Player> callback) {
        return ActionButton.builder(label).action(DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player player) callback.accept(player);
        }, ClickCallback.Options.builder().uses(1).build())).width(220).build();
    }

    private String resolveName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    /** Re-opens still-valid invitations when combat ends. */
    @EventHandler
    public void onCombatExit(PlayerCombatExitEvent event) {
        showPending(event.getPlayer());
    }

    /** Shows still-pending invitations when a player joins the server. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> showPending(event.getPlayer()));
    }

    /** Keeps invitation state owned by the guild and party managers across disconnects. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Invitation state is intentionally UUID-based and survives reconnects.
    }

    public enum InviteType {
        GUILD,
        PARTY
    }
}
