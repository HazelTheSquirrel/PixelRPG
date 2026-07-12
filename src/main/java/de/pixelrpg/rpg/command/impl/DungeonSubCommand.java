// src/main/java/de/pixelrpg/rpg/command/impl/DungeonSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.dungeon.DungeonDefinition;
import de.pixelrpg.rpg.dungeon.DungeonInstanceMode;
import de.pixelrpg.rpg.dungeon.DungeonMarkerType;
import de.pixelrpg.rpg.dungeon.DungeonRepository;
import de.pixelrpg.rpg.dungeon.DungeonSelectionManager;
import de.pixelrpg.rpg.dungeon.DungeonWandFactory;
import de.pixelrpg.rpg.dungeon.RelativeMarker;
import de.pixelrpg.rpg.dungeon.SchematicData;
import de.pixelrpg.rpg.dungeon.SchematicIO;
import de.pixelrpg.rpg.region.CuboidBounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DungeonSubCommand implements SubCommand {

    private final Plugin plugin;
    private final DungeonRepository repository;
    private final DungeonSelectionManager selectionManager;

    public DungeonSubCommand(Plugin plugin, DungeonRepository repository, DungeonSelectionManager selectionManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.selectionManager = selectionManager;
    }

    @Override
    public String name() {
        return "dungeon";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin dungeon <wand|capture|marker|setrank|setmode|setcooldown|delete|list>",
                    NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        return switch (action) {
            case "wand" -> handleWand(sender);
            case "capture" -> handleCapture(sender, rest);
            case "marker" -> handleMarker(sender, rest);
            case "setrank" -> handleSetRank(sender, rest);
            case "setmode" -> handleSetMode(sender, rest);
            case "setcooldown" -> handleSetCooldown(sender, rest);
            case "delete" -> handleDelete(sender, rest);
            case "list" -> handleList(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown dungeon action.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        player.getInventory().addItem(DungeonWandFactory.create(plugin));
        player.sendMessage(Component.text("Dungeon wand given.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCapture(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin dungeon capture <id> <name...>", NamedTextColor.RED));
            return true;
        }

        DungeonSelectionManager.Selection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            sender.sendMessage(Component.text("Select two positions with the dungeon wand first.", NamedTextColor.RED));
            return true;
        }

        String id = args[0];
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        CuboidBounds bounds = CuboidBounds.fromCorners(selection.getPos1(), selection.getPos2());
        SchematicData schematic = SchematicIO.capture(selection.getPos1().getWorld(), bounds);

        String fileName = id + ".schem";
        try {
            SchematicIO.save(schematic, repository.schematicFile(fileName));
        } catch (IOException e) {
            sender.sendMessage(Component.text("Failed to save schematic: " + e.getMessage(), NamedTextColor.RED));
            return true;
        }

        DungeonDefinition definition = repository.getOrCreate(id, name);
        definition.setDisplayName(name);
        definition.setCaptureWorld(bounds.worldName());
        definition.setOrigin(bounds.minX(), bounds.minY(), bounds.minZ());
        definition.setDimensions(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
        definition.setSchematicFile(fileName);

        repository.save();
        selectionManager.clear(player.getUniqueId());

        sender.sendMessage(Component.text("Dungeon '" + id + "' captured (" + schematic.getVolume() + " blocks).", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMarker(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin dungeon marker <id> <ENTRANCE|MOB_SPAWN|BOSS_SPAWN|LOOT_CHEST> [mobType]",
                    NamedTextColor.RED));
            return true;
        }

        DungeonDefinition definition = repository.get(args[0]);
        if (definition == null) {
            sender.sendMessage(Component.text("Dungeon not found. Capture it first.", NamedTextColor.RED));
            return true;
        }
        if (definition.getCaptureWorld() == null || !definition.getCaptureWorld().equals(player.getWorld().getName())) {
            sender.sendMessage(Component.text("Stand in the world where this dungeon was captured.", NamedTextColor.RED));
            return true;
        }

        DungeonMarkerType type;
        try {
            type = DungeonMarkerType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Unknown marker type.", NamedTextColor.RED));
            return true;
        }

        String mobType = args.length >= 3 ? args[2].toUpperCase() : null;

        int dx = player.getLocation().getBlockX() - definition.getOriginX();
        int dy = player.getLocation().getBlockY() - definition.getOriginY();
        int dz = player.getLocation().getBlockZ() - definition.getOriginZ();

        definition.addMarker(new RelativeMarker(type, dx, dy, dz, mobType));
        repository.save();

        sender.sendMessage(Component.text("Marker added: " + type + " at (" + dx + ", " + dy + ", " + dz + ")", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetRank(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /rpgadmin dungeon setrank <id> <minRank> <maxRank>", NamedTextColor.RED));
            return true;
        }
        DungeonDefinition definition = repository.get(args[0]);
        if (definition == null) {
            sender.sendMessage(Component.text("Dungeon not found.", NamedTextColor.RED));
            return true;
        }
        definition.setMinRank(parseRank(args[1]));
        definition.setMaxRank(parseRank(args[2]));
        repository.save();
        sender.sendMessage(Component.text("Dungeon rank range updated.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin dungeon setmode <id> <SHARED|INSTANCED>", NamedTextColor.RED));
            return true;
        }
        DungeonDefinition definition = repository.get(args[0]);
        if (definition == null) {
            sender.sendMessage(Component.text("Dungeon not found.", NamedTextColor.RED));
            return true;
        }
        try {
            definition.setInstanceMode(DungeonInstanceMode.valueOf(args[1].toUpperCase()));
            repository.save();
            sender.sendMessage(Component.text("Dungeon mode updated.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Unknown mode.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleSetCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin dungeon setcooldown <id> <minutes>", NamedTextColor.RED));
            return true;
        }
        DungeonDefinition definition = repository.get(args[0]);
        if (definition == null) {
            sender.sendMessage(Component.text("Dungeon not found.", NamedTextColor.RED));
            return true;
        }
        try {
            definition.setCooldownMinutes(Integer.parseInt(args[1]));
            repository.save();
            sender.sendMessage(Component.text("Cooldown updated.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Cooldown must be a number.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin dungeon delete <id>", NamedTextColor.RED));
            return true;
        }
        boolean removed = repository.remove(args[0]);
        sender.sendMessage(removed
                ? Component.text("Dungeon deleted.", NamedTextColor.GREEN)
                : Component.text("Dungeon not found.", NamedTextColor.RED));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("Dungeons:", NamedTextColor.GOLD));
        for (DungeonDefinition definition : repository.getAll()) {
            sender.sendMessage(Component.text(" - " + definition.getId() + " (" + definition.getDisplayName() + ")", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wand", "capture", "marker", "setrank", "setmode", "setcooldown", "delete", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("marker") || args[0].equalsIgnoreCase("setrank")
                || args[0].equalsIgnoreCase("setmode") || args[0].equalsIgnoreCase("setcooldown")
                || args[0].equalsIgnoreCase("delete"))) {
            List<String> ids = new ArrayList<>();
            for (DungeonDefinition definition : repository.getAll()) {
                ids.add(definition.getId());
            }
            return ids;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("marker")) {
            return Arrays.stream(DungeonMarkerType.values()).map(Enum::name).toList();
        }
        return List.of();
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Rank.F;
        }
    }
}