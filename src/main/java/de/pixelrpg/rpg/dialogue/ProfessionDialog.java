package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.CraftRecipe;
import de.pixelrpg.rpg.profession.CraftingService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestText;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Native player profession menu and detailed recipe dialogs. */
public final class ProfessionDialog {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingService craftingService;
    private final DialogueEngine dialogueEngine;
    private final QuickActionsDialogService quickActions;
    private final QuestManager questManager;

    public ProfessionDialog(PlayerProfileManager profileManager, DialogueEngine dialogueEngine, QuickActionsDialogService quickActions) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.quickActions = quickActions;
        this.questManager = PixelRPGPlugin.getInstance().getQuestManager();
        var professionSystem = PixelRPGPlugin.getInstance().getProfessionSystem();
        this.professionService = professionSystem.professionService();
        this.craftingService = professionSystem.craftingService();
    }

    public void open(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogueEngine.openUnavailable(player, "PixelRPG – Berufe", "Du bist noch nicht registriert.");
            return;
        }

        List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text(
                "Wähle einen Beruf, um deine freigeschalteten Rezepte und Herstellungsdetails zu sehen.", NamedTextColor.WHITE)));
        List<ActionButton> actions = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            int level = professionService.getLevel(player.getUniqueId(), profession);
            boolean learned = professionService.hasLearned(player.getUniqueId(), profession);
            String label = profession.displayName() + (learned ? " • Level " + level : " • nicht erlernt");
            actions.add(dialogueEngine.actionButton(Component.text(label), learned ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openProfession(target, profession)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, quickActions::openQuickActions));
        dialogueEngine.openMultiAction(player, Component.text("PixelRPG – Berufe", NamedTextColor.GOLD), body, actions, 1);
    }

    public void openProfession(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogueEngine.openUnavailable(player, "Beruf", "Du bist noch nicht registriert.");
            return;
        }
        if (!profile.hasLearnedProfession(profession)) {
            dialogueEngine.openUnavailable(player, profession.displayName(),
                    "Du hast diesen Beruf noch nicht erlernt. Sprich mit einem passenden Berufslehrer.");
            return;
        }

        int level = professionService.getLevel(player.getUniqueId(), profession);
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Level " + level + "/" + Profession.MAX_LEVEL, NamedTextColor.AQUA)));

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : craftingService.recipes(profession)) {
            if (!craftingService.isUnlocked(player, recipe)) continue;
            actions.add(dialogueEngine.actionButton(Component.text(recipe.displayName()), NamedTextColor.GREEN,
                    target -> openRecipeDetails(target, recipe, false)));
        }
        if (actions.isEmpty()) body = List.of(
                body.getFirst(),
                body.getLast(),
                DialogBody.plainMessage(Component.text("Du hast noch keine Rezepte freigeschaltet.", NamedTextColor.WHITE)));

        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1,
                target -> open(target));
    }

    /** Opens a complete recipe description before crafting or buying the recipe. */
    public void openRecipeDetails(Player player, CraftRecipe recipe, boolean allowPurchase) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) {
            dialogueEngine.openUnavailable(player, "Rezept", "Du bist noch nicht registriert.");
            return;
        }

        Profession profession = recipe.profession();
        int professionLevel = professionService.getLevel(player.getUniqueId(), profession);
        boolean learned = profile.hasLearnedProfession(profession);
        boolean unlocked = craftingService.isUnlocked(player, recipe);
        boolean levelAvailable = professionLevel >= recipe.requiredProfessionLevel();

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Ergebnis: " + prettyMaterial(recipe.resultMaterial().name()), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Rezeptart: " + (recipe.vanillaRecipe() ? "Vanilla" : "PixelRPG"), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Benötigt: " + profession.displayName() + " Level " + recipe.requiredProfessionLevel(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Materialien:", NamedTextColor.WHITE)));
        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) {
            body.add(DialogBody.plainMessage(Component.text("• " + cost.getValue() + "x " + prettyMaterial(cost.getKey().name()), NamedTextColor.WHITE)));
        }

        List<ActionButton> actions = new ArrayList<>();
        if (unlocked && learned) {
            actions.add(dialogueEngine.actionButton(Component.text("Herstellen"), levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        var result = craftingService.craft(target, recipe.id());
                        target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (result.success()) openRecipeDetails(target, recipe, false);
                    }));
        } else if (allowPurchase && learned && !recipe.vanillaRecipe()) {
            String unlockText = recipe.requiredQuestId().isBlank()
                    ? "Rezept freischalten" + (recipe.unlockPrice() > 0L ? " • " + recipe.unlockPrice() + " Gold" : "")
                    : "Rezept über Quest freischalten";
            actions.add(dialogueEngine.actionButton(Component.text(unlockText), levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        var result = craftingService.unlockRecipe(target, recipe);
                        target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (result.success()) openRecipeDetails(target, recipe, true);
                    }));
        }

        if (recipe.vanillaRecipe() && !unlocked) body.add(DialogBody.plainMessage(Component.text("Dieses Vanilla-Rezept muss zuerst im Minecraft-Rezeptbuch entdeckt werden.", NamedTextColor.YELLOW)));
        if (!recipe.requiredQuestId().isBlank() && !unlocked) {
            Quest quest = questManager == null ? null : questManager.getRepository().getQuest(recipe.requiredQuestId());
            Component questLabel = quest == null
                    ? Component.text(recipe.requiredQuestId(), NamedTextColor.AQUA)
                    : QuestText.title(quest).color(NamedTextColor.AQUA);
            body.add(DialogBody.plainMessage(Component.text("Quest: ", NamedTextColor.WHITE).append(questLabel)));
            if (quest != null) body.add(DialogBody.plainMessage(QuestText.objective(quest).color(NamedTextColor.GRAY)));
        }
        if (recipe.unlockPrice() > 0L && !unlocked) body.add(DialogBody.plainMessage(Component.text("Preis: " + recipe.unlockPrice() + " Gold", NamedTextColor.GOLD)));
        if (!learned) body.add(DialogBody.plainMessage(Component.text("Du musst diesen Beruf zuerst erlernen.", NamedTextColor.WHITE)));

        Consumer<Player> back = target -> {
            if (allowPurchase) {
                openTrainerRecipes(target, profession);
            } else {
                openProfession(target, profession);
            }
        };
        dialogueEngine.openMultiAction(player, Component.text(recipe.displayName(), NamedTextColor.GOLD), body, actions, 1, back);
    }

    /** Opens the trainer recipe list; every recipe leads to its own detail dialog. */
    public void openTrainerRecipes(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;

        int level = professionService.getLevel(player.getUniqueId(), profession);
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Beruf Level " + level + "/" + Profession.MAX_LEVEL, NamedTextColor.AQUA)),
                DialogBody.plainMessage(Component.text("Wähle ein Rezept für Zutaten, Levelanforderung und Freischaltung.", NamedTextColor.WHITE)));

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : craftingService.recipes(profession)) {
            boolean unlocked = craftingService.isUnlocked(player, recipe);
            String state = unlocked ? " • freigeschaltet" : recipe.vanillaRecipe() ? " • Rezeptbuch" : " • freischalten";
            actions.add(dialogueEngine.actionButton(Component.text(recipe.displayName() + state),
                    unlocked ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                    target -> openRecipeDetails(target, recipe, true)));
        }
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName() + "-Lehrer", NamedTextColor.GOLD), body, actions, 1);
    }

    public static Component recipeLine(CraftRecipe recipe) {
        return Component.text(recipe.displayName() + " • Level " + recipe.requiredProfessionLevel());
    }

    private static String prettyMaterial(String raw) {
        String value = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
