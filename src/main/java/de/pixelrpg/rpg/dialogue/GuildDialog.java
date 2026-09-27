package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.guild.GuildTerritoryManager;
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
import java.util.function.Consumer;

/** Native guild dialog for creation and basic guild management. */
public final class GuildDialog {
    private final GuildManager guilds;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    private final QuickActionsDialogService quickActions;
    private final InviteDialogService invites;
    private final Consumer<Player> backAction;

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue) {
        this(guilds, profiles, dialogue, null, null, Player::closeDialog);
    }

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue, QuickActionsDialogService quickActions) {
        this(guilds, profiles, dialogue, quickActions, null, quickActions == null ? Player::closeDialog : quickActions::openQuickActions);
    }

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue, QuickActionsDialogService quickActions, InviteDialogService invites) {
        this(guilds, profiles, dialogue, quickActions, invites, quickActions == null ? Player::closeDialog : quickActions::openQuickActions);
    }

    public GuildDialog(GuildManager guilds, PlayerProfileManager profiles, DialogueEngine dialogue, QuickActionsDialogService quickActions, InviteDialogService invites, Consumer<Player> backAction) {
        this.guilds = guilds;
        this.profiles = profiles;
        this.dialogue = dialogue;
        this.quickActions = quickActions;
        this.invites = invites;
        this.backAction = backAction;
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
                    backAction.accept(target);
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
                DialogBody.plainMessage(Component.text("Rolle: " + (leader ? "Gildenmeister" : guild.isDeputy(player.getUniqueId()) ? "Stellvertreter" : "Mitglied"), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Gildenkasse: " + String.format(java.util.Locale.ROOT, "%.2f", guild.treasury()) + " Goldtaler", NamedTextColor.GOLD)),
                territoryBody(guild),
                DialogBody.plainMessage(Component.text("Das Gildengebiet wird physisch mit Kupfer-Grenzmarkern aufgebaut. Jede Grenzkante darf höchstens 32 Blöcke lang sein.", NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text("Zum Einladen: /gildeneinladen <Spieler>", NamedTextColor.YELLOW))
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(action(Component.text("Mitglieder anzeigen", NamedTextColor.AQUA), p -> showMembers(p, guild)));
        actions.add(action(Component.text("Gildengold", NamedTextColor.GOLD), p -> openGuildGold(p, guild)));
        if (leader || guild.isDeputy(player.getUniqueId())) actions.add(action(Component.text("Grenzmarker kaufen", NamedTextColor.GOLD), this::purchaseMarker));
        if (leader && invites != null) actions.add(action(Component.text("Spieler einladen", NamedTextColor.GREEN), invites::openGuildInviteInput));
        if (leader) actions.add(action(Component.text(guild.deputyId() == null ? "Stellvertreter ernennen" : "Stellvertreter verwalten", NamedTextColor.AQUA), this::openDeputyManagement));
        actions.add(action(Component.text("Zurück", NamedTextColor.WHITE), backAction));
        dialogue.openMultiAction(player, Component.text("Gilde – " + guild.name(), NamedTextColor.GOLD), body, actions, 1);
    }

    private DialogBody territoryBody(Guild guild) {
        GuildTerritoryManager territories = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getGuildTerritoryManager();
        if (territories == null) {
            return DialogBody.plainMessage(Component.text("Gildengebiet: derzeit nicht verfügbar.", NamedTextColor.RED));
        }
        int markers = territories.markerCount(guild.id());
        double price = territories.nextMarkerPrice(guild.id());
        String markerText = markers < de.pixelrpg.rpg.guild.GuildTerritory.INITIAL_MARKERS
                ? markers + "/" + de.pixelrpg.rpg.guild.GuildTerritory.INITIAL_MARKERS + " Grenzmarker für die erste Grenze"
                : markers + " Grenzmarker";
        return DialogBody.plainMessage(Component.text("Gildengebiet: " + markerText + " • nächster Marker: "
                + String.format(java.util.Locale.ROOT, "%.2f", price) + " Goldtaler", NamedTextColor.AQUA));
    }

    private void purchaseMarker(Player player) {
        GuildTerritoryManager territories = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getGuildTerritoryManager();
        if (territories == null) {
            player.sendMessage(Component.text("Das Gildengebiet-System ist derzeit nicht verfügbar.", NamedTextColor.RED));
            return;
        }
        GuildTerritoryManager.OperationResult result = territories.purchaseMarker(player);
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        open(player);
    }

    private void openDeputyManagement(Player player) {
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.isLeader(player.getUniqueId())) {
            open(player);
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        if (guild.deputyId() == null) {
            body.add(DialogBody.plainMessage(Component.text("Der Stellvertreter erhält dieselben Rechte wie der Gildenmeister für die Verwaltung des Gildengebiets.", NamedTextColor.WHITE)));
            DialogInput input = DialogInput.text("deputy_name", 260, Component.text("Mitgliedsname", NamedTextColor.WHITE), true, "", 16, null);
            ActionButton appoint = ActionButton.builder(Component.text("Stellvertreter ernennen", NamedTextColor.GREEN))
                    .action(DialogAction.customClick((response, audience) -> {
                        if (!(audience instanceof Player target)) return;
                        String name = response.getText("deputy_name");
                        if (name == null || name.isBlank()) {
                            target.sendMessage(Component.text("Bitte gib den Namen eines Gildenmitglieds ein.", NamedTextColor.RED));
                            open(target);
                            return;
                        }
                        java.util.UUID targetId = null;
                        for (java.util.UUID memberId : guilds.getMembers(guild.id())) {
                            Player onlineMember = org.bukkit.Bukkit.getPlayer(memberId);
                            String memberName = onlineMember != null ? onlineMember.getName() : org.bukkit.Bukkit.getOfflinePlayer(memberId).getName();
                            if (memberName != null && memberName.equalsIgnoreCase(name.trim())) {
                                targetId = memberId;
                                break;
                            }
                        }
                        if (targetId == null) {
                            target.sendMessage(Component.text("Das Gildenmitglied wurde nicht gefunden.", NamedTextColor.RED));
                            open(target);
                            return;
                        }
                        GuildManager.Result result = guilds.appointDeputy(target, targetId);
                        target.sendMessage(Component.text(result == GuildManager.Result.SUCCESS
                                ? "Stellvertreter wurde ernannt."
                                : "Der Stellvertreter konnte nicht ernannt werden.", result == GuildManager.Result.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED));
                        open(target);
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .width(220).build();
            ActionButton cancel = action(Component.text("Zurück", NamedTextColor.WHITE), this::open);
            player.showDialog(Dialog.create(factory -> {
                DialogRegistryEntry.Builder builder = factory.empty();
                builder.base(DialogBase.builder(Component.text("Stellvertreter ernennen", NamedTextColor.AQUA))
                        .body(body).inputs(List.of(input)).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());
                builder.type(DialogType.multiAction(List.of(appoint, cancel), null, 1));
            }));
            return;
        }

        body.add(DialogBody.plainMessage(Component.text("Aktueller Stellvertreter: " + resolveName(guild.deputyId()), NamedTextColor.AQUA)));
        ActionButton remove = action(Component.text("Stellvertreter absetzen", NamedTextColor.RED), target -> {
            GuildManager.Result result = guilds.removeDeputy(target);
            target.sendMessage(Component.text(result == GuildManager.Result.SUCCESS ? "Stellvertreter wurde abgesetzt." : "Der Stellvertreter konnte nicht abgesetzt werden.",
                    result == GuildManager.Result.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED));
            open(target);
        });
        ActionButton back = action(Component.text("Zurück", NamedTextColor.WHITE), this::open);
        dialogue.openMultiAction(player, Component.text("Stellvertreter", NamedTextColor.AQUA), body, List.of(remove, back), 1);
    }

    private void openGuildGold(Player player, Guild guild) {
        Guild currentGuild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (currentGuild == null || !currentGuild.id().equals(guild.id())) {
            open(player);
            return;
        }

        boolean leader = currentGuild.isLeader(player.getUniqueId());
        List<ActionButton> actions = new ArrayList<>();
        actions.add(action(Component.text("Gold einzahlen", NamedTextColor.GOLD), this::openTreasuryDeposit));
        if (leader) actions.add(action(Component.text("Gold auszahlen", NamedTextColor.YELLOW), this::openTreasuryWithdraw));
        actions.add(action(Component.text("Zurück", NamedTextColor.WHITE), this::open));

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Gildenkasse: " + String.format(java.util.Locale.ROOT, "%.2f", currentGuild.treasury()) + " Goldtaler",
                        NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text(
                        leader
                                ? "Du kannst Gold einzahlen oder als Gildenmeister aus der Gildenkasse auszahlen."
                                : "Du kannst Gold in die gemeinsame Gildenkasse einzahlen.",
                        NamedTextColor.WHITE))
        );

        dialogue.openMultiAction(
                player,
                Component.text("Gildengold", NamedTextColor.GOLD),
                body,
                actions,
                1
        );
    }

    private void openTreasuryDeposit(Player player) {
        DialogInput input = DialogInput.text("amount", 260, Component.text("Goldbetrag", NamedTextColor.WHITE), true, "", 16, null);
        dialogue.openTextInputAction(player, Component.text("Gold einzahlen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Das Gold wird aus deinem Wallet in die gemeinsame Gildenkasse übertragen.", NamedTextColor.WHITE))),
                input, Component.text("Einzahlen"), NamedTextColor.GREEN, (target, response) -> {
                    try {
                        String raw = response.getText("amount");
                        if (raw == null || raw.isBlank()) throw new NumberFormatException();
                        double value = new java.math.BigDecimal(raw.trim().replace(',', '.')).doubleValue();
                        if (!guilds.depositToTreasury(target, value)) {
                            target.sendMessage(Component.text("Die Einzahlung konnte nicht durchgeführt werden.", NamedTextColor.RED));
                        } else {
                            target.sendMessage(Component.text("Gold wurde in die Gildenkasse eingezahlt.", NamedTextColor.GREEN));
                        }
                    } catch (Exception ignored) {
                        target.sendMessage(Component.text("Ungültiger Goldbetrag.", NamedTextColor.RED));
                    }
                    open(target);
                });
    }

    private void openTreasuryWithdraw(Player player) {
        DialogInput input = DialogInput.text("amount", 260, Component.text("Goldbetrag", NamedTextColor.WHITE), true, "", 16, null);
        dialogue.openTextInputAction(player, Component.text("Gold auszahlen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Nur der Gildenmeister kann Gold aus der Gildenkasse in sein Wallet auszahlen.", NamedTextColor.WHITE))),
                input, Component.text("Auszahlen"), NamedTextColor.GREEN, (target, response) -> {
                    try {
                        String raw = response.getText("amount");
                        if (raw == null || raw.isBlank()) throw new NumberFormatException();
                        double value = new java.math.BigDecimal(raw.trim().replace(',', '.')).doubleValue();
                        if (!guilds.withdrawFromTreasury(target, value)) {
                            target.sendMessage(Component.text("Die Auszahlung konnte nicht durchgeführt werden.", NamedTextColor.RED));
                        } else {
                            target.sendMessage(Component.text("Gold wurde aus der Gildenkasse ausgezahlt.", NamedTextColor.GREEN));
                        }
                    } catch (Exception ignored) {
                        target.sendMessage(Component.text("Ungültiger Goldbetrag.", NamedTextColor.RED));
                    }
                    open(target);
                });
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
