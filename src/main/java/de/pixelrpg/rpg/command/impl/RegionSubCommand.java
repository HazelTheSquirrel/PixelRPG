package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.region.PixelRegion;
import de.pixelrpg.rpg.region.RegionEditor;
import de.pixelrpg.rpg.region.RegionFlag;
import de.pixelrpg.rpg.region.RegionFlagDialogService;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Provides the player/admin /pixelrpg region command tree for polygon region management. */
public final class RegionSubCommand implements SubCommand {
    private static final String GLOBAL_SELECTOR = "__global__";

    private final RegionManager regions;
    private final RegionEditor editor;
    private final RegionFlagDialogService flagDialog;

    public RegionSubCommand(RegionManager regions, RegionEditor editor) {
        this.regions = regions;
        this.editor = editor;
        this.flagDialog = new RegionFlagDialogService(regions);
    }

    /** Keeps the existing plugin bootstrap constructor compatible while removing guild ownership from regions. */
    public RegionSubCommand(RegionManager regions, RegionEditor editor, Object ignoredLegacyDependency) {
        this(regions, editor);
    }

    @Override public String name() { return "region"; }
    @Override public String permission() { return "rpg.member"; }
    @Override public String description() { return "Verwaltet und inspiziert PixelRPG-Regionen"; }
    @Override public String usage() { return "/pixelrpg region <info|flags|member|owner|create|finish|confirm|cancel|delete|edit|list> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> adminOnly(sender, args, this::create);
            case "finish" -> adminPlayerOnly(sender, editor::finish);
            case "confirm" -> adminPlayerOnly(sender, editor::confirm);
            case "cancel" -> adminPlayerOnly(sender, editor::cancel);
            case "delete" -> adminOnly(sender, args, this::delete);
            case "info" -> info(sender, args);
            case "flags", "flagmenu" -> flags(sender, args);
            case "member", "members" -> members(sender, args);
            case "owner" -> owner(sender, args);
            case "list" -> adminOnly(sender, args, (ignoredSender, ignoredArgs) -> list(ignoredSender));
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
        PixelRegion region = resolve(args[1], sender);
        if (region == null || region.isGlobal()) {
            sender.sendMessage(Component.text(
                    region != null && region.isGlobal() ? "Die globale Region kann nicht gelöscht werden." : "Region nicht gefunden.",
                    NamedTextColor.RED
            ));
            return true;
        }
        boolean deleted = regions.delete(region.id());
        sender.sendMessage(Component.text(
                deleted ? "Region „" + region.name() + "“ gelöscht." : "Region konnte nicht gelöscht werden.",
                deleted ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        PixelRegion region;
        if (args.length < 2) {
            if (!(sender instanceof Player player)) return false;
            region = regions.find(player.getLocation()).orElse(null);
        } else {
            region = resolve(args[1], sender);
        }
        if (region == null) {
            sender.sendMessage(Component.text("Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Region „" + region.name() + "“", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  ID: " + region.id(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Welt: " + region.worldName(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Typ: " + (region.isGlobal() ? "Globale Standardregion" : region.type()), NamedTextColor.GRAY));
        if (region.isGlobal()) {
            sender.sendMessage(Component.text("  Gültigkeit: gesamte Welt", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text(
                    "  Punkte: " + region.geometry().points().size()
                            + " | Fläche: " + String.format(Locale.ROOT, "%.2f", region.geometry().area()),
                    NamedTextColor.GRAY
            ));
            sender.sendMessage(Component.text(
                    "  Höhe: " + region.minY() + ".." + region.maxY() + " | Priorität: " + region.priority(),
                    NamedTextColor.GRAY
            ));
        }
        sender.sendMessage(Component.text("  Besitzer: " + playerName(region.ownerId()), region.ownerId() == null ? NamedTextColor.GRAY : NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Mitglieder: " + memberNames(region), NamedTextColor.GRAY));
        if (!region.description().isBlank()) sender.sendMessage(Component.text("  Beschreibung: " + region.description(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Flags:", NamedTextColor.YELLOW));
        for (RegionFlag flag : RegionFlag.values()) {
            boolean enabled = effectiveFlag(region, flag);
            String suffix = region.isGlobal() || region.hasFlag(flag) ? "" : " (global geerbt)";
            sender.sendMessage(Component.text(
                    "    " + flag.displayName() + ": " + (enabled ? "AN" : "AUS") + suffix,
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
        }
        return true;
    }

    private boolean flags(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Das Flag-Menü kann nur von Spielern geöffnet werden.", NamedTextColor.RED));
            return true;
        }
        PixelRegion region = args.length >= 2 ? resolve(args[1], sender) : regions.find(player.getLocation()).orElse(null);
        if (region == null) {
            sender.sendMessage(Component.text("Keine Region an deiner Position gefunden.", NamedTextColor.RED));
            return true;
        }
        if (!canManageFlags(player, region)) {
            sender.sendMessage(Component.text("Du bist nicht Besitzer dieser Region.", NamedTextColor.RED));
            return true;
        }
        flagDialog.open(player, region);
        return true;
    }

    private boolean members(CommandSender sender, String[] args) {
        if (args.length != 4) return false;
        String action = args[1].toLowerCase(Locale.ROOT);
        PixelRegion region = resolve(args[2], sender);
        if (region == null || region.isGlobal()) {
            sender.sendMessage(Component.text("Normale Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }
        if (!canManageMembers(sender, region)) {
            sender.sendMessage(Component.text("Nur der Besitzer oder ein Administrator darf Mitglieder verwalten.", NamedTextColor.RED));
            return true;
        }
        UUID playerId = resolvePlayerId(args[3]);
        if (playerId == null) {
            sender.sendMessage(Component.text("Spieler nicht gefunden. Verwende einen online/cached Spieler oder eine UUID.", NamedTextColor.RED));
            return true;
        }
        boolean changed = switch (action) {
            case "add" -> regions.addMember(region.id(), playerId);
            case "remove", "rem" -> regions.removeMember(region.id(), playerId);
            default -> false;
        };
        if (!changed && !action.equals("add") && !action.equals("remove") && !action.equals("rem")) return false;
        sender.sendMessage(Component.text(
                changed
                        ? (action.equals("add") ? "Spieler wurde als Mitglied hinzugefügt." : "Spieler wurde als Mitglied entfernt.")
                        : "Die Mitgliederliste wurde nicht verändert.",
                changed ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ));
        return true;
    }

    private boolean owner(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            sender.sendMessage(Component.text("Nur Administratoren dürfen Besitzer festlegen.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 3) return false;
        PixelRegion region = resolve(args[1], sender);
        if (region == null || region.isGlobal()) {
            sender.sendMessage(Component.text("Normale Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }
        if (args[2].equalsIgnoreCase("none") || args[2].equalsIgnoreCase("remove")) {
            regions.setOwner(region.id(), null);
            sender.sendMessage(Component.text("Besitzer der Region „" + region.name() + "“ entfernt.", NamedTextColor.GREEN));
            return true;
        }
        UUID ownerId = resolvePlayerId(args[2]);
        if (ownerId == null) {
            sender.sendMessage(Component.text("Spieler nicht gefunden. Verwende einen online/cached Spieler oder eine UUID.", NamedTextColor.RED));
            return true;
        }
        regions.setOwner(region.id(), ownerId);
        sender.sendMessage(Component.text("„" + region.name() + "“ gehört jetzt " + playerName(ownerId) + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean list(CommandSender sender) {
        List<PixelRegion> regionsList = regions.all();
        if (regionsList.isEmpty()) {
            sender.sendMessage(Component.text("Keine PixelRPG-Regionen vorhanden.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("PixelRPG-Regionen (" + regionsList.size() + ")", NamedTextColor.GOLD));
        regionsList.forEach(region -> sender.sendMessage(Component.text(
                "  " + region.name() + " | " + region.type() + " | "
                        + region.geometry().points().size() + " Punkte | Priorität " + region.priority()
                        + " | Besitzer " + playerName(region.ownerId()),
                NamedTextColor.GRAY
        )));
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) {
        if (args.length < 4) return false;
        PixelRegion region = resolve(args[1], sender);
        if (region == null) {
            sender.sendMessage(Component.text("Region nicht gefunden.", NamedTextColor.RED));
            return true;
        }

        String field = args[2].toLowerCase(Locale.ROOT);
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        boolean admin = isAdmin(sender);
        boolean owner = sender instanceof Player player && region.isOwner(player.getUniqueId());

        if (region.isGlobal()) {
            if (!admin) {
                sender.sendMessage(Component.text("Die globale Region kann nur von Administratoren geändert werden.", NamedTextColor.RED));
                return true;
            }
            switch (field) {
                case "name" -> region.setName(value);
                case "type" -> region.setType(RegionType.parse(value));
                case "description" -> region.setDescription(value);
                case "priority" -> region.setPriority(parseInt(value, region.priority()));
                case "enter" -> region.setEnterMessage(value);
                case "leave" -> region.setLeaveMessage(value);
                case "flag" -> { return editFlag(sender, region, value); }
                case "property" -> {
                    String[] parts = value.split("\\s+", 2);
                    if (parts.length != 2) return false;
                    region.setProperty(parts[0], parts[1]);
                }
                default -> { return false; }
            }
            regions.saveGlobalRegion(region);
            sender.sendMessage(Component.text("Globale Region in Welt „" + region.worldName() + "“ geändert.", NamedTextColor.GREEN));
            return true;
        }

        if (!admin && !owner) {
            sender.sendMessage(Component.text("Nur der Besitzer oder ein Administrator darf diese Region bearbeiten.", NamedTextColor.RED));
            return true;
        }
        if (!admin && !field.equals("enter") && !field.equals("leave") && !field.equals("flag")) {
            sender.sendMessage(Component.text("Als Besitzer kannst du nur Flags sowie Enter-/Leave-Titel bearbeiten.", NamedTextColor.RED));
            return true;
        }

        switch (field) {
            case "name" -> region.setName(value);
            case "type" -> region.setType(RegionType.parse(value));
            case "description" -> region.setDescription(value);
            case "priority" -> region.setPriority(parseInt(value, region.priority()));
            case "enter" -> region.setEnterMessage(value);
            case "leave" -> region.setLeaveMessage(value);
            case "flag" -> { return editFlag(sender, region, value); }
            case "property" -> {
                if (!admin) return false;
                String[] parts = value.split("\\s+", 2);
                if (parts.length != 2) return false;
                region.setProperty(parts[0], parts[1]);
            }
            default -> { return false; }
        }

        regions.save();
        sender.sendMessage(Component.text("Region „" + region.name() + "“ geändert. Die Polygon-Geometrie blieb unverändert.", NamedTextColor.GREEN));
        return true;
    }

    private boolean editFlag(CommandSender sender, PixelRegion region, String value) {
        String[] parts = value.split("\\s+", 2);
        if (parts.length != 2) return false;
        try {
            RegionFlag flag = RegionFlag.valueOf(parts[0].toUpperCase(Locale.ROOT));
            boolean enabled = parseBoolean(parts[1]);
            if (region.isGlobal()) regions.setGlobalFlag(region.worldName(), flag, enabled);
            else {
                region.setFlag(flag, enabled);
                regions.save();
            }
            sender.sendMessage(Component.text(
                    "Flag " + flag.displayName() + ": " + (enabled ? "AN" : "AUS") + ".",
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            return true;
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Unbekanntes Flag oder Wert. Verwende true = AN, false = AUS.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("create", "finish", "confirm", "cancel", "delete", "info", "flags", "member", "owner", "edit", "list");
        if (args.length == 2 && (args[0].equalsIgnoreCase("member") || args[0].equalsIgnoreCase("members"))) return List.of("add", "remove");
        if (args.length == 2 && isRegionSelectorCommand(args[0])) return regionSelectors(sender);
        if (args.length == 3 && (args[0].equalsIgnoreCase("member") || args[0].equalsIgnoreCase("members"))) return regionSelectors(sender);
        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            return List.of("name", "type", "description", "priority", "enter", "leave", "flag", "property");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("owner")) return List.of("none");
        if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("flag")) {
            return Arrays.stream(RegionFlag.values()).map(Enum::name).toList();
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("flag")) {
            return List.of("true", "false");
        }
        return List.of();
    }

    private List<String> regionSelectors(CommandSender sender) {
        List<String> selectors = regions.all().stream()
                .flatMap(region -> java.util.stream.Stream.of(region.name(), region.id().toString()))
                .distinct()
                .toList();
        if (sender instanceof Player) return java.util.stream.Stream.concat(selectors.stream(), java.util.stream.Stream.of(GLOBAL_SELECTOR)).toList();
        return selectors;
    }

    private boolean isRegionSelectorCommand(String command) {
        return command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("info") || command.equalsIgnoreCase("flags")
                || command.equalsIgnoreCase("flagmenu") || command.equalsIgnoreCase("edit") || command.equalsIgnoreCase("owner");
    }

    private PixelRegion resolve(String text, CommandSender sender) {
        if (text.equalsIgnoreCase(GLOBAL_SELECTOR) || text.equalsIgnoreCase("global")) {
            if (sender instanceof Player player) return regions.globalRegion(player.getWorld().getName());
            return null;
        }
        UUID id = parseUuid(text);
        return id == null
                ? regions.all().stream().filter(region -> region.name().equalsIgnoreCase(text)).findFirst().orElse(null)
                : regions.get(id).orElse(null);
    }

    private boolean canManageFlags(CommandSender sender, PixelRegion region) {
        if (isAdmin(sender)) return true;
        return sender instanceof Player player && !region.isGlobal() && region.isOwner(player.getUniqueId());
    }

    private boolean canManageMembers(CommandSender sender, PixelRegion region) {
        if (isAdmin(sender)) return true;
        return sender instanceof Player player && region.isOwner(player.getUniqueId());
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("rpg.admin") || (sender instanceof org.bukkit.command.ConsoleCommandSender) || (sender instanceof org.bukkit.entity.Player player && player.isOp());
    }

    private static boolean adminOnly(CommandSender sender, String[] args, java.util.function.BiFunction<CommandSender, String[], Boolean> action) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage(Component.text("Keine Administrator-Berechtigung für diese Region-Aktion.", NamedTextColor.RED));
            return true;
        }
        return action.apply(sender, args);
    }

    private static boolean adminPlayerOnly(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage(Component.text("Keine Administrator-Berechtigung für diese Region-Aktion.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diese Aktion ausführen.", NamedTextColor.RED));
            return true;
        }
        action.accept(player);
        return true;
    }

    private UUID resolvePlayerId(String text) {
        UUID uuid = parseUuid(text);
        if (uuid != null) return uuid;
        Player online = Bukkit.getPlayerExact(text);
        if (online != null) return online.getUniqueId();
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(text);
        if (cached != null) return cached.getUniqueId();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(text)) return player.getUniqueId();
        }
        return null;
    }

    private static String playerName(UUID uuid) {
        if (uuid == null) return "keiner";
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private static String memberNames(PixelRegion region) {
        if (region.members().isEmpty()) return "keine";
        return region.members().stream().map(RegionSubCommand::playerName).sorted(String.CASE_INSENSITIVE_ORDER).reduce((left, right) -> left + ", " + right).orElse("keine");
    }

    private boolean effectiveFlag(PixelRegion region, RegionFlag flag) {
        if (region.isGlobal() || region.hasFlag(flag)) return region.flag(flag);
        return regions.globalRegion(region.worldName()).flag(flag);
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
}
