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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Native player profession menu and detailed recipe/quest dialogs. */
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
                "Wähle einen Beruf, um Rezepte, Berufsquests und Herstellungsdetails zu sehen.", NamedTextColor.WHITE)));
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
        List<DialogBody> body = new ArrayList<>(List.of(
                DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Level " + level + "/" + Profession.MAX_LEVEL, NamedTextColor.AQUA))));
        List<ActionButton> actions = new ArrayList<>();
        List<Quest> professionQuests = professionQuests(profession);
        if (!professionQuests.isEmpty()) {
            actions.add(dialogueEngine.actionButton(Component.text("Berufsquests (" + professionQuests.size() + ")"), NamedTextColor.YELLOW,
                    target -> openProfessionQuests(target, profession)));
        }
        List<CraftRecipe> recipes = craftingService.recipes(profession).stream()
                .filter(recipe -> craftingService.isUnlocked(player, recipe))
                .toList();
        if (!recipes.isEmpty()) {
            actions.add(dialogueEngine.actionButton(Component.text("Rezepte (" + recipes.size() + ")"), NamedTextColor.GREEN,
                    target -> openProfessionRecipes(target, profession)));
        }
        if (actions.isEmpty()) body.add(DialogBody.plainMessage(Component.text("Du hast noch keine Inhalte freigeschaltet.", NamedTextColor.WHITE)));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1,
                target -> open(target));
    }

    private List<Quest> professionQuests(Profession profession) {
        if (questManager == null) return List.of();
        return questManager.getRepository().getAllQuests().stream()
                .filter(Quest::isProfessionQuest)
                .filter(quest -> quest.profession() == profession)
                .sorted(Comparator.comparingInt(Quest::requiredProfessionLevel).thenComparing(Quest::title))
                .toList();
    }

    private void openProfessionQuests(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        int professionLevel = professionService.getLevel(player.getUniqueId(), profession);
        List<Quest> quests = professionQuests(profession);
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Berufsquests für " + profession.displayName(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Dein Beruflevel: " + professionLevel + "/" + Profession.MAX_LEVEL, NamedTextColor.AQUA)));
        List<ActionButton> actions = new ArrayList<>();
        for (Quest quest : quests) {
            boolean active = profile.hasActiveQuest(quest.id());
            boolean completed = profile.hasCompletedQuest(quest.id());
            boolean levelAvailable = professionLevel >= quest.requiredProfessionLevel() && profile.getLevel() >= quest.requiredLevel();
            String state = completed ? " • abgeschlossen" : active ? " • aktiv" : levelAvailable ? " • verfügbar" : " • gesperrt";
            actions.add(dialogueEngine.actionButton(QuestText.title(quest).append(Component.text(state)),
                    completed ? NamedTextColor.GRAY : active ? NamedTextColor.YELLOW : levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> openProfessionQuestDetails(target, profession, quest)));
        }
        if (actions.isEmpty()) body.add(DialogBody.plainMessage(Component.text("Aktuell sind keine Berufsquests vorhanden.", NamedTextColor.WHITE)));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName() + " – Berufsquests", NamedTextColor.GOLD), body, actions, 1,
                target -> openProfession(target, profession));
    }

    private void openProfessionRecipes(Player player, Profession profession) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        int professionLevel = professionService.getLevel(player.getUniqueId(), profession);
        List<CraftRecipe> recipes = craftingService.recipes(profession).stream()
                .filter(recipe -> craftingService.isUnlocked(player, recipe))
                .toList();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Rezepte für " + profession.displayName(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Dein Beruflevel: " + professionLevel + "/" + Profession.MAX_LEVEL, NamedTextColor.AQUA)));
        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : recipes) {
            actions.add(dialogueEngine.actionButton(Component.text(recipe.displayName()), NamedTextColor.GREEN,
                    target -> openRecipeDetails(target, recipe, false)));
        }
        if (actions.isEmpty()) body.add(DialogBody.plainMessage(Component.text("Aktuell sind keine Rezepte freigeschaltet.", NamedTextColor.WHITE)));
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName() + " – Rezepte", NamedTextColor.GOLD), body, actions, 1,
                target -> openProfession(target, profession));
    }

    private void openProfessionQuestDetails(Player player, Profession profession, Quest quest) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        int professionLevel = professionService.getLevel(player.getUniqueId(), profession);
        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        boolean levelAvailable = professionLevel >= quest.requiredProfessionLevel() && profile.getLevel() >= quest.requiredLevel();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(QuestText.description(quest).color(NamedTextColor.WHITE)));
        QuestProgressView progressView = new QuestProgressView(quest, active ? profile.getActiveQuests().get(quest.id()) : null);
        body.add(DialogBody.plainMessage(progressView.component()));
        body.add(DialogBody.plainMessage(Component.text("Beruf: " + profession.displayName() + " Level " + quest.requiredProfessionLevel()
                + " • Charakterlevel " + quest.requiredLevel(), levelAvailable ? NamedTextColor.AQUA : NamedTextColor.RED)));
        body.add(DialogBody.plainMessage(Component.text("Belohnung: " + quest.rewardExp() + " EP • " + quest.rewardMoney() + " Gold", NamedTextColor.GOLD)));
        List<ActionButton> actions = new ArrayList<>();
        if (!active && !completed && levelAvailable && questManager.canAccept(profile, quest)) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen"), NamedTextColor.GREEN, target -> {
                questManager.acceptQuest(target, quest);
                openProfessionQuestDetails(target, profession, quest);
            }));
        }
        if (active) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest abgeben"), NamedTextColor.YELLOW, target -> {
                questManager.completeQuest(target, quest.id());
                openProfessionQuestDetails(target, profession, quest);
            }));
            actions.add(dialogueEngine.actionButton(Component.text("Quest abbrechen"), NamedTextColor.RED, target -> {
                questManager.abandonQuest(target, quest.id());
                openProfessionQuests(target, profession);
            }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                target -> openProfessionQuests(target, profession)));
        dialogueEngine.openMultiAction(player, QuestText.title(quest).color(NamedTextColor.GOLD), body, actions, 1);
    }

    private static final class QuestProgressView {
        private final Component component;
        private QuestProgressView(Quest quest, de.pixelrpg.rpg.quest.QuestProgress progress) {
            this.component = progress == null
                    ? QuestText.objective(quest).color(NamedTextColor.AQUA)
                    : QuestText.objectiveWithProgress(quest, progress);
        }
        private Component component() { return component; }
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
        body.add(DialogBody.plainMessage(Component.text("Ergebnis: " + QuestText.itemNamePlain(recipe.resultMaterial().name()), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Rezeptart: " + (recipe.vanillaRecipe() ? "Vanilla" : "PixelRPG"), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Benötigt: " + profession.displayName() + " Level " + recipe.requiredProfessionLevel(), NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Materialien:", NamedTextColor.WHITE)));
        for (Map.Entry<Material, Integer> cost : recipe.costs().entrySet()) body.add(DialogBody.plainMessage(Component.text("• " + cost.getValue() + "x " + QuestText.itemNamePlain(cost.getKey().name()), NamedTextColor.WHITE)));
        List<ActionButton> actions = new ArrayList<>();
        if (unlocked && learned) {
            actions.add(dialogueEngine.actionButton(Component.text("Herstellen"), levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY, target -> {
                var result = craftingService.craft(target, recipe.id());
                target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                if (result.success()) openRecipeDetails(target, recipe, false);
            }));
        } else if (allowPurchase && learned && !recipe.vanillaRecipe()) {
            String unlockText = recipe.requiredQuestId().isBlank() ? "Rezept freischalten" + (recipe.unlockPrice() > 0L ? " • " + recipe.unlockPrice() + " Gold" : "") : "Rezept über Quest freischalten";
            actions.add(dialogueEngine.actionButton(Component.text(unlockText), levelAvailable ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY, target -> {
                var result = craftingService.unlockRecipe(target, recipe);
                target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                if (result.success()) openRecipeDetails(target, recipe, true);
            }));
        }
        if (recipe.vanillaRecipe() && !unlocked) body.add(DialogBody.plainMessage(Component.text("Dieses Vanilla-Rezept muss zuerst im Minecraft-Rezeptbuch entdeckt werden.", NamedTextColor.YELLOW)));
        if (!recipe.requiredQuestId().isBlank() && !unlocked) {
            Quest quest = questManager == null ? null : questManager.getRepository().getQuest(recipe.requiredQuestId());
            Component questLabel = quest == null ? Component.text("Unbekannte Quest", NamedTextColor.RED) : QuestText.title(quest).color(NamedTextColor.AQUA);
            body.add(DialogBody.plainMessage(Component.text("Quest: ", NamedTextColor.WHITE).append(questLabel)));
            if (quest != null) body.add(DialogBody.plainMessage(QuestText.objective(quest).color(NamedTextColor.GRAY)));
        }
        if (recipe.unlockPrice() > 0L && !unlocked) body.add(DialogBody.plainMessage(Component.text("Preis: " + recipe.unlockPrice() + " Gold", NamedTextColor.GOLD)));
        if (!learned) body.add(DialogBody.plainMessage(Component.text("Du musst diesen Beruf zuerst erlernen.", NamedTextColor.WHITE)));
        Consumer<Player> back = target -> {
            if (allowPurchase) openTrainerRecipes(target, profession);
            else openProfessionRecipes(target, profession);
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
            actions.add(dialogueEngine.actionButton(Component.text(recipe.displayName() + state), unlocked ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                    target -> openRecipeDetails(target, recipe, true)));
        }
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName() + "-Lehrer", NamedTextColor.GOLD), body, actions, 1);
    }

    public static Component recipeLine(CraftRecipe recipe) { return Component.text(recipe.displayName() + " • Level " + recipe.requiredProfessionLevel()); }
}
