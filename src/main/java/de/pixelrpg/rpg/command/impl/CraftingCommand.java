package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingRecipeRegistry;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CraftingCommand implements SubCommand {
    private final CraftingService craftingService;

    public CraftingCommand(CraftingService craftingService) {
        this.craftingService = craftingService;
    }

    @Override
    public String name() {
        return "craft";
    }

    @Override
    public String permission() {
        return "rpg.member";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Crafting recipes:", NamedTextColor.GOLD));
            for (Profession profession : Profession.values()) {
                for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(profession)) {
                    sender.sendMessage(Component.text(recipe.id() + " - " + recipe.label()
                            + " (" + profession.name() + ")", NamedTextColor.YELLOW));
                }
            }
            return true;
        }

        var result = craftingService.craft(player, args[0]);
        sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (result.success()) {
            sender.sendMessage(Component.text("+" + result.experience() + " profession XP", NamedTextColor.AQUA));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        List<String> ids = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(profession)) {
                if (recipe.id().startsWith(args[0].toLowerCase())) ids.add(recipe.id());
            }
        }
        return ids;
    }
}
