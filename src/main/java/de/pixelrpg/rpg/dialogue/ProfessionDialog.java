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
import java.util.Map;

/** Native player profession menu and detailed recipe dialogs. */
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
                DialogBody.plainMessage(Component.text(
                        "Wähle einen Beruf, um deine freigeschalteten Rezepte und Herstellungsdetails zu sehen.",
                        NamedTextColor.WHITE))
        );
        List<ActionButton> actions = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            int level = professionService.getLevel(player.getUniqueId(), profession);
            boolean learned = professionService.hasLearned(player.getUniqueId(), profession);
            String label = profession.displayName() + (learned ? " • Level " + level : " • nicht erlernt");
            actions.add(dialogueEngine.actionButton(
                    Component.text(label),
                    learned ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openProfession(target, profession)));
        }
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), body, actions, 1);
    }

    public void openProfession(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            dialogueEngine.openUnavailable(player, "Beruf", "Du bist noch nicht registriert.");
            return;
        }
        if (!profile.hasLearnedProfession(profession)) {
            dialogueEngine.openUnavailable(player, profession.displayName(),
                    "Du hast diesen Beruf noch nicht erlernt. Sprich mit einem passenden Berufslehrer.");
            return;
        }

        int level = professionService.getLevel(player.getUniqueId(), profession);
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text(
                "Level " + level + "/" + Profession.MAX_LEVEL,
                NamedTextColor.WHITE)));

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(profession)) {
            if (!profile.hasUnlockedRecipe(recipe.id())) continue;
            actions.add(dialogueEngine.actionButton(
                    Component.text(recipe.displayName()),
                    NamedTextColor.GREEN,
                    target -> openRecipeDetails(target, recipe, false)));
        }

        if (actions.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text(
                    "Du hast noch keine Rezepte freigeschaltet. Besuche einen "
                            + profession.displayName() + "-Lehrer, um Rezepte zu kaufen.",
                    NamedTextColor.WHITE)));
        }
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1,
                target -> open(target));
    }

    /** Opens a complete recipe description before crafting or buying the recipe. */
    public void openRecipeDetails(Player player, CraftRecipe recipe, boolean allowPurchase) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            dialogueEngine.openUnavailable(player, "Rezept", "Du bist noch nicht registriert.");
            return;
        }

        Profession profession = recipe.profession();
        int professionLevel = professionService.getLevel(player.getUniqueId(), profession);
        boolean learned = profile.hasLearnedProfession(profession);
        boolean unlocked = profile.hasUnlockedRecipe(recipe.id());
        boolean levelAvailable = professionLevel >= recipe.requiredProfessionLevel();

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Ergebnis: " + prettyMaterial(recipe.resultMaterial().name()),
                NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(
                Component.text("Seltenheit: ", NamedTextColor.WHITE).append(recipe.rarity().displayName())));
        body.add(DialogBody.plainMessage(Component.text(
                "Benötigt: " + profession.displayName() + " Level " + recipe.requiredProfessionLevel(),
                NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text(
                "Herstellungszeit: " + recipe.craftSeconds() + " Sekunden",
                NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Materialien:", NamedTextColor.WHITE)));
        for (Map.Entry<org.bukkit.Material, Integer> cost : recipe.costs().entrySet()) {
            body.add(DialogBody.plainMessage(Component.text(
                    "• " + cost.getValue() + "x " + prettyMaterial(cost.getKey().name()),
                    NamedTextColor.WHITE)));
        }

        List<ActionButton> actions = new ArrayList<>();
        if (unlocked && learned) {
            actions.add(dialogueEngine.actionButton(
                    Component.text("Herstellen"),
                    levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        if (professionService.getLevel(target.getUniqueId(), profession) < recipe.requiredProfessionLevel()) {
                            openRecipeDetails(target, recipe, false);
                            return;
                        }
                        var result = craftingService.craft(target, recipe.id());
                        target.sendMessage(Component.text(
                                result.message(),
                                result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (result.success()) {
                            target.sendMessage(Component.text(
                                    "+" + result.experience() + " Berufs-XP",
                                    NamedTextColor.AQUA));
                        }
                    }));
        } else if (allowPurchase && learned) {
            long price = professionService.recipePrice(recipe);
            actions.add(dialogueEngine.actionButton(
                    Component.text("Rezept kaufen • " + price + " Gold"),
                    levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        if (professionService.getLevel(target.getUniqueId(), profession) < recipe.requiredProfessionLevel()) {
                            openRecipeDetails(target, recipe, true);
                            return;
                        }
                        var result = professionService.buyRecipe(target, recipe);
                        target.sendMessage(Component.text(
                                result.message(),
                                result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (result.success()) openRecipeDetails(target, recipe, true);
                    }));
        } else if (!learned) {
            body.add(DialogBody.plainMessage(Component.text(
                    "Du musst diesen Beruf zuerst beim passenden Lehrer erlernen.",
                    NamedTextColor.WHITE)));
        }

        java.util.function.Consumer<Player> back = target -> {
            if (allowPurchase) openTrainerRecipes(target, profession);
            else openProfession(target, profession);
        };
        dialogueEngine.openMultiAction(player, Component.text(recipe.displayName(), NamedTextColor.GOLD), body, actions, 1, back);
    }

    /** Opens the trainer recipe list; every recipe leads to its own detail dialog. */
    public void openTrainerRecipes(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) return;

        int level = professionService.getLevel(player.getUniqueId(), profession);
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Beruf Level " + level + "/" + Profession.MAX_LEVEL,
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Wähle ein Rezept für Zutaten, Herstellungszeit, Levelanforderung und Preis.",
                        NamedTextColor.WHITE))
        );

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(profession)) {
            boolean unlocked = profile.hasUnlockedRecipe(recipe.id());
            actions.add(dialogueEngine.actionButton(
                    Component.text(recipe.displayName() + (unlocked ? " • freigeschaltet" : " • kaufen")),
                    unlocked ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                    target -> openRecipeDetails(target, recipe, true)));
        }
        dialogueEngine.openMultiAction(
                player,
                Component.text(profession.displayName() + "-Lehrer", NamedTextColor.GOLD),
                body,
                actions,
                1,
                target -> open(target));
    }

    public static Component recipeLine(CraftRecipe recipe) {
        return Component.text(recipe.displayName() + " • Level " + recipe.requiredProfessionLevel());
    }

    private static String prettyMaterial(String raw) {
        String value = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
