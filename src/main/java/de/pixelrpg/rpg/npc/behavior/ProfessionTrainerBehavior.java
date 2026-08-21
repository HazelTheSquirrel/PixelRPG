package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingRecipeRegistry;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native-dialog profession trainer; crafting is performed directly from the dialog. */
public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingService craftingService;
    private final DialogueEngine dialogueEngine;

    public ProfessionTrainerBehavior(PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     CraftingService craftingService,
                                     DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.craftingService = craftingService;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public NpcType type() { return NpcType.PROFESSION_TRAINER; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, "Handwerkslehrer", "Du musst zuerst Rathausmitglied sein.");
            return;
        }
        if (!npc.isProfessionTrainer()) {
            dialogueEngine.openUnavailable(player, "Handwerkslehrer", "Dieser NPC wurde noch keiner Profession zugewiesen.");
            return;
        }

        Profession profession = npc.profession();
        int level = professionService.getLevel(player.getUniqueId(), profession);
        long experience = professionService.getExperience(player.getUniqueId(), profession);
        long nextLevel = level >= Profession.MAX_LEVEL ? experience : ProfessionService.experienceForLevel(level + 1);
        String progress = level >= Profession.MAX_LEVEL
                ? "Level 99 – Meister"
                : "Level " + level + "/" + Profession.MAX_LEVEL + " • " + experience + "/" + nextLevel + " EP";

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text(progress, level >= Profession.MAX_LEVEL ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));

        List<CraftRecipe> recipes = CraftingRecipeRegistry.getRecipes(profession);
        for (CraftRecipe recipe : recipes) {
            String costs = recipe.costs().entrySet().stream()
                    .map(entry -> entry.getValue() + "x " + pretty(entry.getKey().name()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("keine");
            body.add(DialogBody.plainMessage(Component.text()
                    .append(Component.text(recipe.displayName(), NamedTextColor.WHITE))
                    .append(Component.text(" • ab Level " + recipe.requiredProfessionLevel(), NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("Material: " + costs, NamedTextColor.DARK_GRAY))
                    .build()));
        }

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : recipes) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Herstellen: " + recipe.displayName()),
                    level >= recipe.requiredProfessionLevel() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        var result = craftingService.craft(target, recipe.id());
                        target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (result.success()) target.sendMessage(Component.text("+" + result.experience() + " Berufs-XP", NamedTextColor.AQUA));
                    }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName() + "-Lehrer", NamedTextColor.GOLD), body, actions, 1);
    }

    private String pretty(String raw) {
        String value = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
