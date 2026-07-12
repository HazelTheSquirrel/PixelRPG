// src/main/java/de/pixelrpg/rpg/command/impl/NpcSubCommand.java (VOLLSTÄNDIG, ersetzt alte Datei — create mit Skin-Argument, neue skin-Subaktion, Mannequin statt Villager)
package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class NpcSubCommand implements SubCommand {

    private final NpcManager npcManager;

    public NpcSubCommand(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public String name() {
        return "npc";
    }

    @Override
    public String permission() {
        return "rpg.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgadmin npc <create|remove|rename|skin|list>", NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        return switch (action) {
            case "create" -> handleCreate(sender, rest);
            case "remove" -> handleRemove(sender);
            case "rename" -> handleRename(sender, rest);
            case "skin" -> handleSkin(sender, rest);
            case "list" -> handleList(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown npc action.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /rpgadmin npc create <type> <name> [playerName|skinUrl]", NamedTextColor.RED));
            return true;
        }

        NpcType type;
        try {
            type = NpcType.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown npc type. Valid: " + Arrays.toString(NpcType.values()), NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        String skinSource = args.length >= 3 ? args[2] : null;

        RPGNpc npc = npcManager.create(type, name, player.getLocation(), skinSource);

        player.sendMessage(Component.text("Created " + type + " NPC (internal id: " + npc.id() + ")"
                + (npc.hasCustomSkin() ? " with custom skin." : "."), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        Entity hit = rayTraceNpc(player);
        if (hit == null) {
            player.sendMessage(Component.text("Look at an NPC to remove it.", NamedTextColor.RED));
            return true;
        }

        Optional<RPGNpc> npcOpt = npcManager.getByEntity(hit.getUniqueId());
        if (npcOpt.isEmpty()) {
            player.sendMessage(Component.text("That is not a guild NPC.", NamedTextColor.RED));
            return true;
        }

        npcManager.removeById(npcOpt.get().id());
        player.sendMessage(Component.text("NPC removed.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRename(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin npc rename <name...>", NamedTextColor.RED));
            return true;
        }

        Entity hit = rayTraceNpc(player);
        if (hit == null) {
            player.sendMessage(Component.text("Look at an NPC to rename it.", NamedTextColor.RED));
            return true;
        }

        Optional<RPGNpc> npcOpt = npcManager.getByEntity(hit.getUniqueId());
        if (npcOpt.isEmpty()) {
            player.sendMessage(Component.text("That is not a guild NPC.", NamedTextColor.RED));
            return true;
        }

        String newName = String.join(" ", args);
        npcManager.rename(npcOpt.get().id(), newName);

        player.sendMessage(Component.text("NPC renamed.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) {
            sender.sendMessage(Component.text("Usage: /rpgadmin npc skin <playerName|skinUrl>", NamedTextColor.RED));
            return true;
        }

        Entity hit = rayTraceNpc(player);
        if (hit == null) {
            player.sendMessage(Component.text("Look at an NPC to change its skin.", NamedTextColor.RED));
            return true;
        }

        Optional<RPGNpc> npcOpt = npcManager.getByEntity(hit.getUniqueId());
        if (npcOpt.isEmpty()) {
            player.sendMessage(Component.text("That is not a guild NPC.", NamedTextColor.RED));
            return true;
        }

        npcManager.updateSkin(npcOpt.get().id(), args[0]);
        player.sendMessage(Component.text("NPC skin updated.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("NPCs:", NamedTextColor.GOLD));
        for (RPGNpc npc : npcManager.getAll()) {
            sender.sendMessage(Component.text(" - (" + npc.id() + ") " + npc.type() + " " + npc.name(), NamedTextColor.YELLOW));
        }
        return true;
    }

    private Entity rayTraceNpc(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                6.0,
                entity -> entity instanceof Mannequin
        );
        return result != null ? result.getHitEntity() : null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "remove", "rename", "skin", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            List<String> typeNames = new ArrayList<>();
            for (NpcType type : NpcType.values()) {
                typeNames.add(type.name());
            }
            return typeNames;
        }
        return List.of();
    }
}