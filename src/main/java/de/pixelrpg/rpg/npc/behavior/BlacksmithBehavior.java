package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.gui.BlacksmithGUI;
import de.pixelrpg.rpg.gui.CraftingGUI;
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
import net.kyori.adventure.text.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native-dialog blacksmith NPC using the unified PixelRPG Blacksmith profession. */
public final class BlacksmithBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final CraftingService craftingService;
    private final DialogueEngine dialogueEngine;

    public BlacksmithBehavior(PlayerProfileManager profileManager,
                              ProfessionService professionService,
                              CraftingService craftingService,
                              DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.craftingService = craftingService;
        this.dialogueEngine = dialogueEngine;
    }

    /** Compatibility constructor for the existing plugin bootstrap; the legacy GUIs are intentionally not used. */
    public BlacksmithBehavior(BlacksmithGUI ignoredBlacksmithGUI, CraftingGUI ignoredCraftingGUI,
                              PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this(profileManager,
                PixelRPGPlugin.getInstance().getProfessionSystem().professionService(),
                PixelRPGPlugin.getInstance().getProfessionSystem().craftingService(),
                dialogueEngine);
    }

    @Override
    public NpcType type() { return NpcType.BLACKSMITH; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, "Schmied", "Du musst zuerst Rathausmitglied sein.");
            return;
        }

        int level = professionService.getLevel(player.getUniqueId(), Profession.BLACKSMITH);
        long experience = professionService.getExperience(player.getUniqueId(), Profession.BLACKSMITH);
        long next = level >= Profession.MAX_LEVEL ? experience : ProfessionService.experienceForLevel(level + 1);

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Waffen, Rüstung und Werkzeuge aus Meisterhand.", NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text(
                level >= Profession.MAX_LEVEL
                        ? "Schmied Level 99 – Meister"
                        : "Schmied Level " + level + "/99 • " + experience + "/" + next + " EP",
                level >= Profession.MAX_LEVEL ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));

        List<ActionButton> actions = new ArrayList<>();
        for (CraftRecipe recipe : CraftingRecipeRegistry.getRecipes(Profession.BLACKSMITH)) {
            body.add(DialogBody.plainMessage(Component.text()
                    .append(Component.text(recipe.displayName(), NamedTextColor.WHITE))
                    .append(Component.text(" • Level " + recipe.requiredProfessionLevel(), NamedTextColor.GRAY))
                    .build()));
            actions.add(dialogueEngine.actionButton(
                    Component.text("Herstellen: " + recipe.displayName()),
                    level >= recipe.requiredProfessionLevel() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY,
                    target -> {
                        var result = craftingService.craft(target, recipe.id());
                        target.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                    }));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogueEngine.openMultiAction(player, Component.text("Schmied", NamedTextColor.GOLD), body, actions, 1);
    }
}
