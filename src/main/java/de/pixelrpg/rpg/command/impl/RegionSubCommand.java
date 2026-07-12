// src/main/java/de/pixelrpg/rpg/command/impl/RegionSubCommand.java
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.region.CuboidBounds;
import de.pixelrpg.rpg.region.Region;
import de.pixelrpg.rpg.region.RegionCategory;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionSelectionManager;
import de.pixelrpg.rpg.region.RegionWandFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RegionSubCommand implements SubCommand {

    private final RegionManager regionManager;
    private final RegionSelectionManager selectionManager;

    public RegionSubCommand(RegionManager regionManager, RegionSelectionManager selectionManager) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
    }

    @Override
    public String name() {
        return "region";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(
                    "Usage: /rpgadmin region <wand|create|addbox|setname|setrank|setpriority|delete|list|info>",
                    NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        return switch (action) {
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, rest);
            case "addbox" -> handleAddBox(sender, rest);
            case "setname" -> handleSetName(sender, rest);
            case "setrank" -> handleSetRank(sender, rest);
            case "setpriority" -> handleSetPriority(sender, rest);
            case "delete" -> handleDelete(sender, rest);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, rest);
            default -> {
                sender.sendMessage(Component.text("Unknown region action.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        player.getInventory().addItem(RegionWandFactory.create());
        player.sendMessage(Component.text("Region wand given.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region create <id> <category>", NamedTextColor.RED));
            return true;
        }

        String id = args[0];
        if (regionManager.getRegion(id).isPresent()) {
            sender.sendMessage(Component.text("A region with this id already exists.", NamedTextColor.RED));
            return true;
        }

        RegionCategory category = parseCategory(args[1]);
        RegionSelectionManager.Selection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            sender.sendMessage(Component.text("Select two positions with the region wand first.", NamedTextColor.RED));
            return true;
        }

        Region region = regionManager.createRegion(id, id, category);
        region.addBox(CuboidBounds.fromCorners(selection.getPos1(), selection.getPos2()));
        regionManager.save();
        selectionManager.clear(player.getUniqueId());

        sender.sendMessage(Component.text("Region '" + id + "' created.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAddBox(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region addbox <id>", NamedTextColor.RED));
            return true;
        }

        Optional<Region> regionOpt = regionManager.getRegion(args[0]);
        if (regionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Region not found.", NamedTextColor.RED));
            return true;
        }

        RegionSelectionManager.Selection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            sender.sendMessage(Component.text("Select two positions with the region wand first.", NamedTextColor.RED));
            return true;
        }

        regionOpt.get().addBox(CuboidBounds.fromCorners(selection.getPos1(), selection.getPos2()));
        regionManager.save();
        selectionManager.clear(player.getUniqueId());

        sender.sendMessage(Component.text("Box added to region '" + args[0] + "'.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetName(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region setname <id> <name...>", NamedTextColor.RED));
            return true;
        }
        Optional<Region> regionOpt = regionManager.getRegion(args[0]);
        if (regionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Region not found.", NamedTextColor.RED));
            return true;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        regionOpt.get().setDisplayName(name);
        regionManager.save();
        sender.sendMessage(Component.text("Region name updated.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetRank(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region setrank <id> <minRank> <maxRank>", NamedTextColor.RED));
            return true;
        }
        Optional<Region> regionOpt = regionManager.getRegion(args[0]);
        if (regionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Region not found.", NamedTextColor.RED));
            return true;
        }

        Rank min = parseRank(args[1]);
        Rank max = parseRank(args[2]);
        Region region = regionOpt.get();
        region.setMinRank(min);
        region.setMaxRank(max);
        regionManager.save();
        sender.sendMessage(Component.text("Region rank range updated.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetPriority(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region setpriority <id> <value>", NamedTextColor.RED));
            return true;
        }
        Optional<Region> regionOpt = regionManager.getRegion(args[0]);
        if (regionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Region not found.", NamedTextColor.RED));
            return true;
        }

        try {
            int priority = Integer.parseInt(args[1]);
            regionOpt.get().setPriority(priority);
            regionManager.save();
            sender.sendMessage(Component.text("Region priority updated.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Priority must be a number.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region delete <id>", NamedTextColor.RED));
            return true;
        }
        boolean removed = regionManager.deleteRegion(args[0]);
        sender.sendMessage(removed
                ? Component.text("Region deleted.", NamedTextColor.GREEN)
                : Component.text("Region not found.", NamedTextColor.RED));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<Region> regions = regionManager.getAllRegions();
        if (regions.isEmpty()) {
            sender.sendMessage(Component.text("No regions defined.", NamedTextColor.YELLOW));
            return true;
        }
        sender.sendMessage(Component.text("Regions:", NamedTextColor.GOLD));
        for (Region region : regions) {
            sender.sendMessage(Component.text(" - " + region.getId() + " (" + region.getCategory() + ")", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin region info <id>", NamedTextColor.RED));
            return true;
        }
        Optional<Region> regionOpt = regionManager.getRegion(args[0]);
        if (regionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Region not found.", NamedTextColor.RED));
            return true;
        }
        Region region = regionOpt.get();
        sender.sendMessage(Component.text("Region: " + region.getId(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Name: " + region.getDisplayName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Category: " + region.getCategory(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Rank range: " + region.getMinRank() + " - " + region.getMaxRank(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Priority: " + region.getPriority(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Boxes: " + region.getBoxes().size(), NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wand", "create", "addbox", "setname", "setrank", "setpriority", "delete", "list", "info");
        }
        if (args.length == 2) {
            List<String> ids = new ArrayList<>();
            for (Region region : regionManager.getAllRegions()) {
                ids.add(region.getId());
            }
            return ids;
        }
        return List.of();
    }

    private RegionCategory parseCategory(String raw) {
        try {
            return RegionCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RegionCategory.WILDERNESS;
        }
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Rank.F;
        }
    }
}