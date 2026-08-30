package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.region.PixelRegion;
import de.pixelrpg.rpg.region.RegionEditor;
import de.pixelrpg.rpg.region.RegionFlag;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Provides the admin-only /pixelrpg region command tree for polygon region management. */
public final class RegionSubCommand implements SubCommand {
    private final RegionManager regions;
    private final RegionEditor editor;
    private final GuildManager guilds;

    public RegionSubCommand(RegionManager regions, RegionEditor editor, GuildManager guilds) {
        this.regions = regions;
        this.editor = editor;
        this.guilds = guilds;
    }

    @Override public String name() { return "region"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "Verwaltet PixelRPG-Polygonregionen"; }
    @Override public String usage() { return "/pixelrpg region <create|finish|confirm|cancel|delete|info|edit|list> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "finish" -> playerOnly(sender, editor::finish);
            case "confirm" -> playerOnly(sender, editor::confirm);
            case "cancel" -> playerOnly(sender, editor::cancel);
            case "delete" -> delete(sender, args);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            case "edit" -> edit(sender, args);
            default -> false;
        };
    }

    private boolean create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können eine Polygon-Session starten.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 2) return false;
        if (regions.all().stream().anyMatch(region -> region.name().equalsIgnoreCase(args[1]))) {
            sender.sendMessage(Component.text("Eine Region mit diesem Namen existiert bereits.", NamedTextColor.RED));
            return true;
        }
        editor.begin(player, args[1]);
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        PixelRegion region = resolve(args[1]);
        if (region == null) {
            sender.sendMessage(Component.text("Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }
        boolean deleted = regions.delete(region.id());
        sender.sendMessage(Component.text(deleted ? "Region gelöscht." : "Region konnte nicht gelöscht werden.", deleted ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        PixelRegion region = resolve(args[1]);
        if (region == null) {
            sender.sendMessage(Component.text("Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Region " + region.name(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ID: " + region.id(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Welt: " + region.worldName() + " | Typ: " + region.type() + " | Punkte: " + region.geometry().points().size(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Y: " + region.minY() + ".." + region.maxY() + " | Fläche: " + String.format(Locale.ROOT, "%.2f", region.geometry().area()) + " | Priorität: " + region.priority(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Gilde: " + (region.ownerGuildName() == null ? "keine" : region.ownerGuildName()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Flags: " + region.flags(), NamedTextColor.GRAY));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (regions.all().isEmpty()) {
            sender.sendMessage(Component.text("Keine PixelRPG-Regionen vorhanden.", NamedTextColor.YELLOW));
            return true;
        }
        regions.all().forEach(region -> sender.sendMessage(Component.text(region.id() + " | " + region.name() + " | " + region.type() + " | " + region.geometry().points().size() + " Punkte", NamedTextColor.GRAY)));
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) {
        if (args.length < 4) return false;
        PixelRegion region = resolve(args[1]);
        if (region == null) {
            sender.sendMessage(Component.text("Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }
        String field = args[2].toLowerCase(Locale.ROOT);
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        switch (field) {
            case "name" -> region.setName(value);
            case "type" -> region.setType(RegionType.parse(value));
            case "description" -> region.setDescription(value);
            case "priority" -> region.setPriority(parseInt(value, region.priority()));
            case "enter" -> region.setEnterMessage(value);
            case "leave" -> region.setLeaveMessage(value);
            case "flag" -> {
                String[] parts = value.split("\\s+", 2);
                if (parts.length != 2) return false;
                try {
                    region.setFlag(RegionFlag.valueOf(parts[0].toUpperCase(Locale.ROOT)), parseBoolean(parts[1]));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(Component.text("Unbekanntes Flag oder Wert. Verwende true oder false.", NamedTextColor.RED));
                    return true;
                }
            }
            case "guild" -> {
                Guild guild = guilds.getGuildByName(value).orElse(null);
                if (guild == null) {
                    sender.sendMessage(Component.text("Gilde nicht gefunden.", NamedTextColor.RED));
                    return true;
                }
                region.setOwner(guild.id(), guild.name());
            }
            case "unguild" -> region.clearOwner();
            case "property" -> {
                String[] parts = value.split("\\s+", 2);
                if (parts.length != 2) return false;
                region.setProperty(parts[0], parts[1]);
            }
            default -> { return false; }
        }
        regions.save();
        sender.sendMessage(Component.text("Region geändert. Die Polygon-Geometrie wurde nicht verändert.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("create", "finish", "confirm", "cancel", "delete", "info", "edit", "list");
        if (args.length == 2 && isRegionSelectorCommand(args[0])) return regions.all().stream().flatMap(region -> java.util.stream.Stream.of(region.name(), region.id().toString())).distinct().toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) return List.of("name", "type", "description", "priority", "enter", "leave", "flag", "guild", "unguild", "property");
        if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("flag")) return Arrays.stream(RegionFlag.values()).map(Enum::name).toList();
        if (args.length == 5 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("flag")) return List.of("true", "false");
        return List.of();
    }

    private boolean isRegionSelectorCommand(String command) {
        return command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("info") || command.equalsIgnoreCase("edit");
    }

    private PixelRegion resolve(String text) {
        UUID id = parseUuid(text);
        return id == null ? regions.all().stream().filter(region -> region.name().equalsIgnoreCase(text)).findFirst().orElse(null) : regions.get(id).orElse(null);
    }

    private static UUID parseUuid(String value) {
        try { return UUID.fromString(value); } catch (IllegalArgumentException | NullPointerException ignored) { return null; }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Expected true or false");
    }

    private static boolean playerOnly(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diese Aktion ausführen.", NamedTextColor.RED));
            return true;
        }
        action.accept(player);
        return true;
    }
}
