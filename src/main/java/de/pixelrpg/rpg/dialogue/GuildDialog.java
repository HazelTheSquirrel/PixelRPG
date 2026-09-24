package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.player.PlayerProfile;
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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native guild dialog for creation and basic guild management. */
public final class GuildDialog {
    private final GuildManager guilds;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    private final QuickActionsDialogService quickActions;
    private final InviteDialogService invites;

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue) {
        this(guilds, profiles, dialogue, null, null);
    }

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue, QuickActionsDialogService quickActions) {
        this(guilds, profiles, dialogue, quickActions, null);
    }

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue, QuickActionsDialogService quickActions, InviteDialogService invites) {
        this.guilds = guilds;
        this.profiles = profiles;
        this.dialogue = dialogue;
        this.quickActions = quickActions;
        this.invites = invites;
    }

    public void open(Player player) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openNotice(player, Component.text("Gilde", NamedTextColor.GOLD),
                    Component.text("Du musst zuerst dein PixelRPG-Profil an der Rezeption registrieren.", NamedTextColor.WHITE),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) openCreation(player); else openOverview(player, guild);
    }

    private void openCreation(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElseThrow();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Eine Gilde ist eure gemeinsame Gemeinschaft und kann später eine eigene, manuell verwaltete Stadt als Basis besitzen.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Voraussetzung: Level " + Guild.MIN_CREATION_LEVEL + " und " + Guild.CREATION_COST_GOLD + " Gold im Wallet.", NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text("Dein aktuelles Level: " + profile.getLevel() + " • Gold: " + (long) profile.getMoney(), NamedTextColor.GRAY))
        );
        DialogInput name = DialogInput.text("guild_name", 320, Component.text("Gildenname", NamedTextColor.WHITE), true, "", 24, null);
        ActionButton create = ActionButton.builder(Component.text("Gilde gründen – 2.500 Gold", NamedTextColor.GREEN))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) createFromResponse(target, response);
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220).build();
        ActionButton cancel = ActionButton.builder(Component.text("Abbrechen", NamedTextColor.RED))
                .action(DialogAction.customClick((response, audience) -> {
                    if (!(audience instanceof Player target)) return;
                    if (quickActions != null) quickActions.openQuickActions(target);
                    else new ReceptionDialog(target, profiles, dialogue, null, guilds).open();
                }, ClickCallback.Options.builder().uses(1).build()))
                .width(220).build();
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Gilde gründen", NamedTextColor.GOLD)).body(body).inputs(List.of(name)).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
            builder.type(DialogType.multiAction(List.of(create, cancel), null, 2));
        }));
    }

    private void createFromResponse(Player player, DialogResponseView response) {
        String name = response.getText("guild_name");
        if (name == null) return;
        GuildManager.Result result = guilds.createGuild(player, name);
        switch (result) {
            case SUCCESS -> { player.sendMessage(Component.text("Gilde „" + name.trim() + "“ wurde gegründet. Du bist jetzt Gildenmeister.", NamedTextColor.GREEN)); open(player); }
            case LEVEL_TOO_LOW -> player.sendMessage(Component.text("Du musst mindestens Level 20 sein, um eine Gilde zu gründen.", NamedTextColor.RED));
            case INSUFFICIENT_GOLD -> player.sendMessage(Component.text("Du benötigst 2.500 Gold im Wallet.", NamedTextColor.RED));
            case NAME_TAKEN -> player.sendMessage(Component.text("Dieser Gildenname ist bereits vergeben.", NamedTextColor.RED));
            case INVALID_NAME -> player.sendMessage(Component.text("Der Gildenname muss 3–24 Zeichen lang sein.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits Mitglied einer Gilde.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Gilde konnte nicht erstellt werden.", NamedTextColor.RED));
        }
    }

    private void openOverview(Player player, Guild guild) {
        boolean leader = guild.isLeader(player.getUniqueId());
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Gilde: " + guild.name(), NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text("Rolle: " + (leader ? "Gildenmeister" : "Mitglied"), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Die Gildenstadt wird ausschließlich manuell mit WorldEdit/WorldGuard verwaltet.", NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text("Zum Einladen: /gildeneinladen <Spieler>", NamedTextColor.YELLOW))
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(action(Component.text("Mitglieder anzeigen", NamedTextColor.AQUA), p -> showMembers(p, guild)));
        if (leader && invites != null) actions.add(action(Component.text("Spieler einladen", NamedTextColor.GREEN), invites::openGuildInviteInput));
        actions.add(action(Component.text("Schließen", NamedTextColor.GRAY), Player::closeDialog));
        dialogue.openMultiAction(player, Component.text("Gilde – " + guild.name(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void showMembers(Player player, Guild guild) {
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Gildenmeister: " + resolveName(guild.leaderId()), NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Weitere Mitglieder werden über /gildeneinladen und /gildeannehmen verwaltet.", NamedTextColor.WHITE))
        );
        dialogue.openMultiAction(player, Component.text("Gildenmitglieder", NamedTextColor.GOLD), body,
                List.of(action(Component.text("Zurück", NamedTextColor.WHITE), this::open)), 1);
    }

    private String resolveName(java.util.UUID uuid) {
        Player online = org.bukkit.Bukkit.getPlayer(uuid);
        return online != null ? online.getName() : uuid.toString().substring(0, 8);
    }

    private ActionButton action(Component label, java.util.function.Consumer<Player> callback) {
        return dialogue.actionButton(label, NamedTextColor.WHITE, callback);
    }
}
