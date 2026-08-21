package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
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

/** Native player profession menu and direct recipe crafting menu. */
public final class ProfessionDialog {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingService craftingService;
    private final DialogueEngine dialogueEngine;

    public ProfessionDialog(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = new ProfessionService(profileManager);
        this.craftingService = new CraftingService(professionService, profileManager);
        this.dialogueEngine = dialogueEngine;
    }

    public void open(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            dialogueEngine.openUnavailable(player, "PixelRPG – Berufe", "Du bist noch nicht registriert.");
            return;
        }

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle einen Beruf, um deine freigeschalteten Rezepte zu sehen.", NamedTextColor.GRAY))
        );
        List<ActionButton> actions = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            int level = professionService.getLevel(player.getUniqueId(), profession);
            boolean learned = professionService.hasLearned(player.getUniqueId(), profession);
            String label = profession.displayName() + (learned ? " • Level " + level : " • nicht erlernt");
            actions.add(dialogueEngine.actionButton(Component.text(label), learned ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openProfession(target, profession)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), body, actions, 1);
    }

    public void openProfession(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            dialogueEngine.openUnavailable(player, "Beruf", "Du bist noch nicht registriert.");
            return;
        }
        if (!profile.hasLearnedProfession(profession)) {
            dialogueEngine.openUnavailable(player, profession.displayName(), "Du hast diesen Beruf noch nicht erlernt. Sprich mit einem passenden Berufslehrer.");
            return;
        }

        int level = professionService.getLevel(player.getUniqueId(), profession);
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text("Level " + level + "/" + Profession.MAX_LEVEL, NamedTextColor.YELLOW)));

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(profession)) {
            if (!profile.hasUnlockedRecipe(recipe.id())) continue;
            actions.add(dialogueEngine.actionButton(recipeLine(recipe), NamedTextColor.WHITE, target -> {
                var result = craftingService.craft(target, recipe.id());
                target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                if (result.success()) target.sendMessage(Component.text("+" + result.experience() + " Berufs-XP", NamedTextColor.AQUA));
            }));
        }

        if (actions.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Du hast noch keine Rezepte freigeschaltet. Besuche einen "
                    + profession.displayName() + "-Lehrer, um Rezepte zu kaufen.", NamedTextColor.GRAY)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1);
    }

    public static Component recipeLine(CraftRecipe recipe) {
        String costs = recipe.costs().entrySet().stream()
                .map(entry -> entry.getValue() + "x " + prettyMaterial(entry.getKey().name()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("Keine Materialien");
        return Component.text(recipe.displayName() + " - " + costs);
    }

    private static String prettyMaterial(String raw) {
        String value = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
