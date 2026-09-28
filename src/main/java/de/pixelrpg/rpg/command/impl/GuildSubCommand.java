package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.guild.CityProgressionService;
import de.pixelrpg.rpg.guild.KingdomCombatMode;
import org.bukkit.Material;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Provides the complete player-facing guild command tree below /pixelrpg guild. */
public final class GuildSubCommand implements SubCommand {
    private static final List<String> SUBCOMMANDS = List.of("create", "invite", "accept", "leave", "info", "city", "maintenance", "disband");

    private final GuildManager guildManager;
    private final CityProgressionService cityProgression;

    public GuildSubCommand(GuildManager guildManager, CityProgressionService cityProgression) {
        this.guildManager = guildManager;
        this.cityProgression = cityProgression;
    }

    @Override
    public String name() {
        return "guild";
    }

    @Override
    public String permission() {
        return null;
    }

    @Override
    public String description() {
        return "Gilden verwalten";
    }

    @Override
    public String usage() {
        return "/pixelrpg guild <create|invite|accept|leave|info|city|maintenance|disband>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(player, args);
            case "invite" -> invite(player, args);
            case "accept" -> accept(player);
            case "leave" -> leave(player);
            case "info" -> info(player);
            case "city" -> city(player, args);
            case "disband" -> disband(player);
            case "maintenance" -> maintenance(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("city")) {
            return List.of("info", "claim", "markers", "contribute", "upgrade", "pvp").stream()
                    .filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("city") && args[1].equalsIgnoreCase("claim")) {
            return List.of("deine-gilde");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("city") && args[1].equalsIgnoreCase("markers")) {
            return List.of("info", "buy");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("city") && args[1].equalsIgnoreCase("pvp")) {
            return List.of("pve", "pvp").stream()
                    .filter(value -> value.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private boolean create(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /pixelrpg guild create <Name>", NamedTextColor.YELLOW));
            return true;
        }

        GuildManager.Result result = guildManager.createGuild(player, args[1]);
        switch (result) {
            case SUCCESS -> player.sendMessage(Component.text("Gilde erfolgreich erstellt.", NamedTextColor.GREEN));
            case NOT_REGISTERED -> player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
            case LEVEL_TOO_LOW -> player.sendMessage(Component.text("Dein Level ist für die Gildengründung zu niedrig.", NamedTextColor.RED));
            case INVALID_NAME -> player.sendMessage(Component.text("Der Gildenname ist ungültig.", NamedTextColor.RED));
            case NAME_TAKEN -> player.sendMessage(Component.text("Dieser Gildenname ist bereits vergeben.", NamedTextColor.RED));
            case INSUFFICIENT_GOLD -> player.sendMessage(Component.text("Du hast nicht genug Gold für die Gildengründung.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Gilde konnte nicht erstellt werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /pixelrpg guild invite <Spieler>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Dieser Spieler ist nicht online.", NamedTextColor.RED));
            return true;
        }

        switch (guildManager.invite(player, target)) {
            case SUCCESS -> { }
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister darf Spieler einladen.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde hat bereits die maximale Mitgliederzahl erreicht.", NamedTextColor.RED));
            case TARGET_ALREADY_IN_GUILD -> player.sendMessage(Component.text("Dieser Spieler ist bereits in einer Gilde.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht gesendet werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean accept(Player player) {
        switch (guildManager.acceptInvitation(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du bist der Gilde beigetreten.", NamedTextColor.GREEN));
            case NO_INVITATION -> player.sendMessage(Component.text("Du hast keine ausstehende Gildeneinladung.", NamedTextColor.RED));
            case GUILD_NOT_FOUND -> player.sendMessage(Component.text("Die Gilde existiert nicht mehr.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Du bist bereits in einer Gilde.", NamedTextColor.RED));
            case GUILD_FULL -> player.sendMessage(Component.text("Die Gilde ist bereits voll.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Einladung konnte nicht angenommen werden.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean leave(Player player) {
        switch (guildManager.leave(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Du hast die Gilde verlassen.", NamedTextColor.YELLOW));
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case LEADER_CANNOT_LEAVE -> player.sendMessage(Component.text("Als Gildenmeister kannst du die Gilde nicht einfach verlassen. Löse sie stattdessen auf.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Du konntest die Gilde nicht verlassen.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean info(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) {
            player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Gilde: " + guild.name(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Mitglieder: " + guild.memberCount() + "/" + Guild.MAX_MEMBERS, NamedTextColor.GRAY));
        player.sendMessage(Component.text("Gildenmeister: " + Bukkit.getOfflinePlayer(guild.leaderId()).getName(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Gildenstadt: " + (guild.cityRegionId() == null ? "keine" : guild.cityRegionId()), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Stadtlevel: " + guild.cityLevel() + " – " + Guild.cityName(guild.cityLevel()), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Gebietsmodus: " + guild.combatMode().name(), NamedTextColor.GRAY));
        return true;
    }

    private boolean city(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /pixelrpg guild city <info|claim|markers|contribute|upgrade|pvp>", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "claim" -> {
                var territoryManager = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getGuildTerritoryManager();
                String selector = args.length >= 3 ? args[2] : null;
                if (territoryManager == null) {
                    player.sendMessage(Component.text("Das Gildengebietssystem ist derzeit nicht verfügbar.", NamedTextColor.RED));
                    return true;
                }
                var result = territoryManager.synchronizeGuildCity(player, selector);
                player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            case "markers" -> {
                var territoryManager = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getGuildTerritoryManager();
                Guild guild = guildManager.getGuild(player.getUniqueId()).orElse(null);
                if (territoryManager == null || guild == null) {
                    player.sendMessage(Component.text("Du bist in keiner Gilde oder das Gildengebietssystem ist nicht verfügbar.", NamedTextColor.RED));
                    return true;
                }
                if (args.length >= 3 && args[2].equalsIgnoreCase("buy")) {
                    var result = territoryManager.purchaseMarker(player);
                    player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                    return true;
                }
                int count = territoryManager.markerCount(guild.id());
                player.sendMessage(Component.text("Grenzmarker: " + count, NamedTextColor.GOLD));
                player.sendMessage(Component.text("Nächster Grenzmarker: " + String.format(Locale.ROOT, "%.2f", territoryManager.nextMarkerPrice(guild.id())) + " Goldtaler", NamedTextColor.GRAY));
                player.sendMessage(Component.text("Verwendung: /pixelrpg guild city markers buy", NamedTextColor.YELLOW));
            }
            case "info" -> {
                var view = cityProgression.view(guildManager.getGuild(player.getUniqueId()).map(Guild::id).orElse(null));
                if (view == null) {
                    player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
                    return true;
                }
                player.sendMessage(Component.text("Stadtlevel " + view.level() + " – " + view.levelName(), NamedTextColor.GOLD));
                if (view.level() >= Guild.MAX_CITY_LEVEL) {
                    player.sendMessage(Component.text("Maximales Stadtlevel erreicht.", NamedTextColor.GREEN));
                    return true;
                }
                player.sendMessage(Component.text("Nächstes Level: " + view.nextLevelName(), NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Benötigtes Gold in der Stadtkasse: " + view.requiredGoldMinorUnits() / 100.0D
                        + " (fehlend " + view.remainingGoldMinorUnits() / 100.0D + ")", NamedTextColor.GRAY));
                view.requiredMaterials().forEach((material, amount) ->
                        player.sendMessage(Component.text(material + ": " + view.deliveredMaterials().getOrDefault(material, 0) + "/" + amount, NamedTextColor.GRAY)));
                view.requiredObjectives().forEach((key, amount) ->
                        player.sendMessage(Component.text(key + ": " + view.objectiveProgress().getOrDefault(key, 0) + "/" + amount, NamedTextColor.GRAY)));
                long remaining = Math.max(0L, view.cooldownUntil() - System.currentTimeMillis());
                player.sendMessage(Component.text("Cooldown: " + (remaining / 3_600_000L) + "h", NamedTextColor.GRAY));
            }
            case "contribute" -> {
                if (args.length != 4) {
                    player.sendMessage(Component.text("Verwendung: /pixelrpg guild city contribute <Material> <Menge>", NamedTextColor.YELLOW));
                    return true;
                }
                Material material = Material.matchMaterial(args[2]);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException exception) {
                    amount = 0;
                }
                if (material == null || amount <= 0 || !cityProgression.contribute(player, material, amount)) {
                    player.sendMessage(Component.text("Diese Ressource wird für den nächsten Stadtaufstieg nicht benötigt oder fehlt im Inventar.", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Ressourcen für den nächsten Stadtaufstieg eingelagert.", NamedTextColor.GREEN));
                }
            }
            case "upgrade" -> {
                switch (cityProgression.upgrade(player)) {
                    case SUCCESS -> { }
                    case NOT_AUTHORIZED -> player.sendMessage(Component.text("Nur Gildenmeister oder Stellvertreter dürfen den Stadtaufstieg auslösen.", NamedTextColor.RED));
                    case MAX_LEVEL -> player.sendMessage(Component.text("Maximales Stadtlevel erreicht.", NamedTextColor.RED));
                    default -> player.sendMessage(Component.text("Cooldown, Gold oder Ressourcen verhindern den Aufstieg.", NamedTextColor.RED));
                }
            }
            case "pvp" -> {
                if (args.length != 3) {
                    player.sendMessage(Component.text("Verwendung: /pixelrpg guild city pvp <pve|pvp>", NamedTextColor.YELLOW));
                    return true;
                }
                KingdomCombatMode mode;
                try {
                    mode = KingdomCombatMode.valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    player.sendMessage(Component.text("Modus muss pve oder pvp sein.", NamedTextColor.RED));
                    return true;
                }
                switch (guildManager.requestCombatMode(player, mode)) {
                    case SUCCESS -> { }
                    case NOT_LEADER -> player.sendMessage(Component.text("Nur Gildenmeister oder Stellvertreter dürfen den Gebietsmodus ändern.", NamedTextColor.RED));
                    case COMBAT_MODE_COOLDOWN -> player.sendMessage(Component.text("Der Gebietsmodus befindet sich noch im Cooldown.", NamedTextColor.RED));
                    case COMBAT_MODE_PENDING -> player.sendMessage(Component.text("Ein Gebietsmoduswechsel ist bereits geplant.", NamedTextColor.RED));
                    case COMBAT_MODE_ALREADY_ACTIVE -> player.sendMessage(Component.text("Dieser Modus ist bereits aktiv.", NamedTextColor.RED));
                    default -> player.sendMessage(Component.text("Der Gebietsmodus konnte nicht geändert werden.", NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Verwendung: /pixelrpg guild city <info|contribute|upgrade|pvp>", NamedTextColor.YELLOW));
        }
        return true;
    }


    private boolean maintenance(Player player, String[] args) {
        if(args.length!=3){player.sendMessage(Component.text("Verwendung: /pixelrpg guild maintenance <Material> <Menge>",NamedTextColor.YELLOW));return true;}
        Guild guild=guildManager.getGuild(player.getUniqueId()).orElse(null); if(guild==null){player.sendMessage(Component.text("Du bist in keiner Gilde.",NamedTextColor.RED));return true;}
        Material material=Material.matchMaterial(args[1]); int amount; try{amount=Integer.parseInt(args[2]);}catch(NumberFormatException e){amount=0;}
        var service=de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getKingdomMaintenanceService();
        if(material==null||amount<=0||!player.getInventory().contains(material,amount)||!service.contribute(guild.id(),material,amount)){player.sendMessage(Component.text("Diese Wartungsressource wird nicht benötigt oder fehlt.",NamedTextColor.RED));return true;}
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material,amount));
        service.complete(guild.id()); player.sendMessage(Component.text("Wartungsbeitrag registriert.",NamedTextColor.GREEN)); return true;
    }

    private boolean disband(Player player) {
        switch (guildManager.disband(player)) {
            case SUCCESS -> player.sendMessage(Component.text("Die Gilde wurde aufgelöst.", NamedTextColor.YELLOW));
            case NOT_IN_GUILD -> player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            case NOT_LEADER -> player.sendMessage(Component.text("Nur der Gildenmeister kann die Gilde auflösen.", NamedTextColor.RED));
            default -> player.sendMessage(Component.text("Die Gilde konnte nicht aufgelöst werden.", NamedTextColor.RED));
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("PixelRPG Gildenbefehle", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/pixelrpg guild create <Name>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild invite <Spieler>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild accept", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild leave", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city claim [Gildenname]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city markers [buy]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city contribute <Material> <Menge>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city upgrade", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild city pvp <pve|pvp>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild maintenance <Material> <Menge>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/pixelrpg guild disband", NamedTextColor.YELLOW));
    }
}
