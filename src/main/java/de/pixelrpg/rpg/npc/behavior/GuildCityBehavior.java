package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.GuildDialog;
import de.pixelrpg.rpg.guild.CityProgressionService;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.guild.GuildTerritoryManager;
import de.pixelrpg.rpg.guild.KingdomCombatMode;
import de.pixelrpg.rpg.guild.KingdomMaintenanceService;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Native guild-city mannequin interaction for city progression and guild administration. */
public final class GuildCityBehavior implements NpcBehavior {
    private final GuildManager guilds;
    private final CityProgressionService cityProgression;
    private final GuildTerritoryManager territories;
    private final KingdomMaintenanceService maintenance;
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;

    public GuildCityBehavior(GuildManager guilds, CityProgressionService cityProgression,
                             GuildTerritoryManager territories, KingdomMaintenanceService maintenance,
                             PlayerProfileManager profiles, DialogueEngine dialogue) {
        this.guilds = guilds;
        this.cityProgression = cityProgression;
        this.territories = territories;
        this.maintenance = maintenance;
        this.profiles = profiles;
        this.dialogue = dialogue;
    }

    @Override
    public NpcType type() {
        return NpcType.GUILD_CITY;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        open(player, npc, Player::closeDialog);
    }

    @Override
    public void onInteract(Player player, RPGNpc npc, Consumer<Player> backAction) {
        open(player, npc, backAction);
    }

    private void open(Player player, RPGNpc npc, Consumer<Player> backAction) {
        if (!profiles.isRegistered(player.getUniqueId())) {
            dialogue.openUnavailable(player, "Gildenstadt", "Du musst zuerst dein PixelRPG-Profil registrieren.");
            return;
        }

        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(npc.kingdomId())) {
            dialogue.openNotice(player, Component.text("Gildenstadt", NamedTextColor.GOLD),
                    Component.text("Diese Gildenstadt gehört nicht zu deiner Gilde.", NamedTextColor.RED),
                    Component.text("Schließen", NamedTextColor.GRAY));
            return;
        }

        openOverview(player, npc, backAction);
    }

    private void openOverview(Player player, RPGNpc npc, Consumer<Player> backAction) {
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(npc.kingdomId())) return;

        CityProgressionService.CityView view = cityProgression.view(guild.id());
        if (view == null) return;

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Gildenstadt: " + guild.name() + " • Stadtlevel " + view.level() + " – " + view.levelName(),
                NamedTextColor.GOLD)));
        body.add(DialogBody.plainMessage(Component.text(
                guild.cityLevel() >= Guild.MAX_CITY_LEVEL
                        ? "Die Gildenstadt hat die maximale Stufe erreicht."
                        : "Nächstes Ziel: " + view.nextLevelName(),
                NamedTextColor.AQUA)));

        if (guild.cityLevel() < Guild.MAX_CITY_LEVEL) {
            body.add(DialogBody.plainMessage(Component.text(
                    "Gildenkasse: " + formatGold(guild.treasury()) + " • benötigtes Gold: " + formatGold(view.requiredGoldMinorUnits() / 100.0D)
                            + " • fehlend: " + formatGold(view.remainingGoldMinorUnits() / 100.0D),
                    NamedTextColor.GOLD)));
            for (var entry : view.requiredMaterials().entrySet()) {
                body.add(DialogBody.plainMessage(Component.text(
                        entry.getKey().name() + ": " + view.deliveredMaterials().getOrDefault(entry.getKey(), 0) + "/" + entry.getValue(),
                        view.deliveredMaterials().getOrDefault(entry.getKey(), 0) >= entry.getValue()
                                ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            }
            for (var entry : view.requiredObjectives().entrySet()) {
                body.add(DialogBody.plainMessage(Component.text(
                        entry.getKey() + ": " + view.objectiveProgress().getOrDefault(entry.getKey(), 0) + "/" + entry.getValue(),
                        view.objectiveProgress().getOrDefault(entry.getKey(), 0) >= entry.getValue()
                                ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            }
            long remaining = Math.max(0L, view.cooldownUntil() - System.currentTimeMillis());
            body.add(DialogBody.plainMessage(Component.text(
                    "Cooldown: " + formatDuration(remaining),
                    remaining == 0L ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));
        }

        body.add(DialogBody.plainMessage(Component.text(
                "Gebietsmodus: " + guild.combatMode().name(),
                guild.combatMode() == KingdomCombatMode.PVP ? NamedTextColor.RED : NamedTextColor.GREEN)));

        List<ActionButton> actions = new ArrayList<>();
        if (guild.cityLevel() < Guild.MAX_CITY_LEVEL) {
            actions.add(action("Stadtressourcen beitragen", NamedTextColor.GREEN, target -> openCityContribution(target, npc, backAction)));
            if (guild.canManageTerritory(player.getUniqueId())) {
                actions.add(action("Stadt aufwerten", NamedTextColor.GOLD, target -> {
                    CityProgressionService.Result result = cityProgression.upgrade(target);
                    target.sendMessage(Component.text(cityUpgradeMessage(result),
                            result == CityProgressionService.Result.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED));
                    open(target, npc, backAction);
                }));
            }
        }

        if (guild.canManageTerritory(player.getUniqueId())) {
            actions.add(action("Grenzmarker kaufen", NamedTextColor.GOLD, target -> {
                GuildTerritoryManager.OperationResult result = territories.purchaseMarker(target);
                target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                open(target, npc, backAction);
            }));
            actions.add(action("Gebietsmodus: PvE", NamedTextColor.GREEN, target -> requestCombatMode(target, npc, KingdomCombatMode.PVE, backAction)));
            actions.add(action("Gebietsmodus: PvP", NamedTextColor.RED, target -> requestCombatMode(target, npc, KingdomCombatMode.PVP, backAction)));
        }

        actions.add(action("Wöchentliche Wartung", NamedTextColor.AQUA, target -> openMaintenance(target, npc, backAction)));
        actions.add(action("Gilde verwalten", NamedTextColor.GOLD, target -> {
            GuildDialog guildDialog = new GuildDialog(guilds, profiles, dialogue, null, null,
                    next -> open(next, npc, backAction));
            guildDialog.open(target);
        }));
        actions.add(action("Schließen", NamedTextColor.GRAY, Player::closeDialog));

        dialogue.openMultiAction(player, Component.text("Gildenstadt – " + guild.name(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void openCityContribution(Player player, RPGNpc npc, Consumer<Player> backAction) {
        dialogue.openTextInputAction(player, Component.text("Stadtressourcen beitragen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(
                        "Eingabe: MATERIAL MENGE • z. B. STONE 128", NamedTextColor.WHITE))),
                io.papermc.paper.registry.data.dialog.input.DialogInput.text(
                        "contribution", 360, Component.text("Material und Menge", NamedTextColor.WHITE), true, "", 64, null),
                Component.text("Einlagern", NamedTextColor.GREEN), NamedTextColor.GREEN,
                (target, response) -> {
                    String raw = response.getText("contribution");
                    String[] parts = raw == null ? new String[0] : raw.trim().split("\s+");
                    Material material = parts.length == 2
                            ? Material.matchMaterial(parts[0].toUpperCase(Locale.ROOT)) : null;
                    int amount = 0;
                    if (parts.length == 2) {
                        try {
                            amount = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            amount = 0;
                        }
                    }
                    if (material == null || amount <= 0 || !cityProgression.contribute(target, material, amount)) {
                        target.sendMessage(Component.text(
                                "Material, Menge oder Stadtvoraussetzung sind ungültig.",
                                NamedTextColor.RED));
                    } else {
                        target.sendMessage(Component.text(
                                "Material wurde für den nächsten Stadtaufstieg eingelagert.",
                                NamedTextColor.GREEN));
                    }
                    open(target, npc, backAction);
                });
    }

    private void openMaintenance(Player player, RPGNpc npc, Consumer<Player> backAction) {
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(npc.kingdomId())) return;

        KingdomMaintenanceService.MaintenanceState state = maintenance.state(guild.id());
        int required = PixelRPGPlugin.getInstance().getConfig().getInt("kingdom.maintenance.amount", 16);
        Material configuredMaterial = firstMaintenanceMaterial();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Beitrag: " + state.contributed() + "/" + required,
                        state.contributed() >= required ? NamedTextColor.GREEN : NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text(
                        "Material: " + (configuredMaterial == null ? "nicht konfiguriert" : configuredMaterial.name()),
                        NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text(
                        "Status: " + (state.reviewRequired() ? "Adminprüfung erforderlich" : "aktiv"),
                        state.reviewRequired() ? NamedTextColor.RED : NamedTextColor.GREEN))
        );

        List<ActionButton> actions = new ArrayList<>();
        if (configuredMaterial != null && state.contributed() < required) {
            actions.add(action("Wartungsmaterial beitragen", NamedTextColor.GREEN,
                    target -> openMaintenanceContribution(target, npc, backAction, configuredMaterial, required)));
        }
        if (state.contributed() >= required) {
            actions.add(action("Wartung abschließen", NamedTextColor.GOLD, target -> {
                if (!maintenance.complete(guild.id())) {
                    target.sendMessage(Component.text("Die Wartung konnte nicht abgeschlossen werden.", NamedTextColor.RED));
                } else {
                    target.sendMessage(Component.text("Die wöchentliche Wartung wurde abgeschlossen.", NamedTextColor.GREEN));
                }
                open(target, npc, backAction);
            }));
        }
        actions.add(action("Zurück", target -> open(target, npc, backAction)));
        dialogue.openMultiAction(player, Component.text("Wöchentliche Wartung", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openMaintenanceContribution(Player player, RPGNpc npc, Consumer<Player> backAction,
                                              Material material, int required) {
        dialogue.openTextInputAction(player, Component.text("Wartungsmaterial", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(
                        "Benötigt: " + material.name() + " • maximal " + (required - maintenance.state(
                                guilds.getGuild(player.getUniqueId()).map(Guild::id).orElse(null)).contributed()) + " Stück.",
                        NamedTextColor.WHITE))),
                io.papermc.paper.registry.data.dialog.input.DialogInput.text(
                        "amount", 300, Component.text("Menge", NamedTextColor.WHITE), true, "", 8, null),
                Component.text("Einlagern", NamedTextColor.GREEN), NamedTextColor.GREEN,
                (target, response) -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(response.getText("amount").trim());
                    } catch (Exception ignored) {
                        amount = 0;
                    }
                    Guild guild = guilds.getGuild(target.getUniqueId()).orElse(null);
                    if (guild == null || amount <= 0 || amount > required
                            || !target.getInventory().contains(material, amount)
                            || !maintenance.contribute(guild.id(), material, amount)) {
                        target.sendMessage(Component.text("Wartungsbeitrag konnte nicht eingelagert werden.", NamedTextColor.RED));
                    } else {
                        target.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material, amount));
                        target.sendMessage(Component.text("Wartungsbeitrag eingelagert.", NamedTextColor.GREEN));
                    }
                    open(target, npc, backAction);
                });
    }

    private void requestCombatMode(Player player, RPGNpc npc, KingdomCombatMode mode, Consumer<Player> backAction) {
        GuildManager.Result result = guilds.requestCombatMode(player, mode);
        if (result != GuildManager.Result.SUCCESS) {
            player.sendMessage(Component.text(combatModeMessage(result), NamedTextColor.RED));
        }
        open(player, npc, backAction);
    }

    private Material firstMaintenanceMaterial() {
        var section = PixelRPGPlugin.getInstance().getConfig().getConfigurationSection("kingdom.maintenance.materials");
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) return material;
        }
        return null;
    }

    private String cityUpgradeMessage(CityProgressionService.Result result) {
        return switch (result) {
            case SUCCESS -> "Die Gildenstadt wurde aufgewertet.";
            case NOT_AUTHORIZED -> "Nur Gildenmeister oder Stellvertreter dürfen den Stadtaufstieg auslösen.";
            case MAX_LEVEL -> "Das maximale Stadtlevel ist bereits erreicht.";
            case INSUFFICIENT_GOLD -> "Die Gildenkasse enthält nicht genügend Goldtaler.";
            default -> "Cooldown, Gold, Materialien oder Objectives verhindern den Stadtaufstieg.";
        };
    }

    private String combatModeMessage(GuildManager.Result result) {
        return switch (result) {
            case COMBAT_MODE_COOLDOWN -> "Der Gebietsmodus befindet sich noch im Cooldown.";
            case COMBAT_MODE_PENDING -> "Ein Gebietsmoduswechsel ist bereits geplant.";
            case COMBAT_MODE_ALREADY_ACTIVE -> "Dieser Gebietsmodus ist bereits aktiv.";
            case NOT_LEADER -> "Nur Gildenmeister oder Stellvertreter dürfen den Gebietsmodus ändern.";
            default -> "Der Gebietsmodus konnte nicht geändert werden.";
        };
    }

    private ActionButton action(String label, NamedTextColor color, Consumer<Player> callback) {
        return dialogue.actionButton(Component.text(label, color), color, callback);
    }

    private static String formatGold(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private static String formatDuration(long millis) {
        long hours = millis / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
