package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.gui.RegionEditGUI;
import de.pixelrpg.rpg.region.RegionEditor;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.SpawnMobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** Provides the admin edit menu and temporary region-editor modes. */
public final class EditSubCommand implements SubCommand {
    private final RegionEditor editor;
    private final RegionManager regions;

    public EditSubCommand(RegionEditor editor, RegionManager regions) {
        this.editor = editor;
        this.regions = regions;
    }

    @Override public String name() { return "edit"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "Öffnet den Region-Editor"; }
    @Override public String usage() { return "/pixelrpg edit <region|spawn> [hostile-mob]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können den Editor verwenden.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("region")) {
            RegionEditGUI.open(player, regions);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return editor.beginSpawnMode(player, args[1]);
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("region", "spawn");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return Arrays.stream(EntityType.values())
                    .filter(type -> type.getEntityClass() != null && SpawnMobType.isHostileMob(type.name()))
                    .map(Enum::name)
                    .toList();
        }
        return List.of();
    }
}
