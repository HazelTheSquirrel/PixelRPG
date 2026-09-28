package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.ProfessionDialog;
import de.pixelrpg.rpg.dialogue.QuickActionsDialogService;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.npc.ProfessionNpcBuffService;
import de.pixelrpg.rpg.npc.ProfessionNpcProgressionService;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.profession.ProfessionService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Native profession trainer dialog for learning one profession and browsing all profession recipes. */
public final class ProfessionTrainerBehavior implements NpcBehavior {
    private final NpcType type;
    private final Profession profession;
    private final PlayerProfileManager profileManager;
    private final ProfessionService professionService;
    private final DialogueEngine dialogueEngine;
    private final ProfessionDialog professionDialog;
    private final ProfessionNpcBuffService buffService;
    private final ProfessionNpcProgressionService npcProgression;

    public ProfessionTrainerBehavior(NpcType type, Profession profession,
                                     PlayerProfileManager profileManager,
                                     ProfessionService professionService,
                                     DialogueEngine dialogueEngine,
                                     QuickActionsDialogService quickActions,
                                     ProfessionNpcBuffService buffService) {
        this.type = type;
        this.profession = profession;
        this.profileManager = profileManager;
        this.professionService = professionService;
        this.dialogueEngine = dialogueEngine;
        this.professionDialog = new ProfessionDialog(profileManager, dialogueEngine, quickActions, quickActions.questManager());
        this.buffService = buffService;
        this.npcProgression = PixelRPGPlugin.getInstance().getProfessionNpcProgressionService();
    }

    @Override
    public NpcType type() { return type; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        onInteract(player, npc, Player::closeDialog);
    }

    @Override
    public void onInteract(Player player, RPGNpc npc, Consumer<Player> backAction) {

        buffService.grant(player, npc);
        if (!profileManager.isRegistered(player.getUniqueId())) {
            dialogueEngine.openUnavailable(player, profession.displayName(), "Du musst zuerst Rathausmitglied sein.");
            return;
        }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        if (profile.hasLearnedProfession(profession)) {
            if (profession.isMain() && profession != profile.getMainProfession()) {
                List<DialogBody> body = new ArrayList<>();
                body.add(DialogBody.plainMessage(Component.text("Dein aktueller Hauptberuf: " + (profile.getMainProfession() == null ? "keiner" : profile.getMainProfession().displayName()), NamedTextColor.GRAY)));
                body.add(DialogBody.plainMessage(Component.text("Ein Wechsel setzt diesen Hauptberuf auf Level 1 zurück.", NamedTextColor.YELLOW)));
                List<ActionButton> actions = new ArrayList<>();
                actions.add(dialogueEngine.actionButton(Component.text("Hauptberuf wechseln"), NamedTextColor.GREEN, target -> {
                    if (professionService.learn(target, profession)) professionDialog.openProfession(target, profession, backAction);
                }));
                actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction));
                dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1);
                return;
            }
            List<ActionButton> actions = new ArrayList<>();
            actions.add(dialogueEngine.actionButton(Component.text("Berufsinhalte"), NamedTextColor.GREEN,
                    target -> professionDialog.openProfession(target, profession, backAction)));
            addNpcProgressAction(actions, player, npc);
            actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction));
            dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD),
                    List.of(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY))),
                    actions, 1);
            return;
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(profession.description(), NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(Component.text("Du hast den Beruf " + profession.displayName() + " noch nicht erlernt.", NamedTextColor.RED)));
        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(Component.text(profession.displayName() + " erlernen"), NamedTextColor.GREEN,
                target -> {
                    if (professionService.learn(target, profession)) {
                        professionDialog.openTrainerRecipes(target, profession, backAction);
                    }
                }));
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction));
        addNpcProgressAction(actions, player, npc);
        dialogueEngine.openMultiAction(player, Component.text(profession.displayName(), NamedTextColor.GOLD), body, actions, 1);
    }

    private void addNpcProgressAction(List<ActionButton> actions, Player player, RPGNpc npc) {
        if (npcProgression == null || npc.profession() == null || npc.kingdomId() == null) return;
        Guild guild = PixelRPGPlugin.getInstance().getGuildManager().getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(npc.kingdomId())) return;
        actions.add(dialogueEngine.actionButton(Component.text("NPC-Aufwertung"), NamedTextColor.AQUA,
                target -> openNpcProgress(target, npc)));
    }

    private void openNpcProgress(Player player, RPGNpc npc) {
        RPGNpc currentNpc = PixelRPGPlugin.getInstance().getNpcManager().getById(npc.id()).orElse(npc);
        var view = npcProgression.progress(currentNpc.id());
        if (view == null) return;

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Rang: " + view.currentRank().displayName() + " → " +
                        (view.nextRank() == null ? "maximal" : view.nextRank().displayName()),
                NamedTextColor.AQUA)));
        if (view.nextRank() != null) {
            body.add(DialogBody.plainMessage(Component.text(
                    "Gold: " + view.gold() + " • freie Spezialisierungspunkte: " + view.availableSpecializationPoints(),
                    NamedTextColor.GOLD)));
            view.requiredMaterials().forEach((material, required) ->
                    body.add(DialogBody.plainMessage(Component.text(
                            material.name() + ": " + view.deliveredMaterials().getOrDefault(material, 0) + "/" + required,
                            view.deliveredMaterials().getOrDefault(material, 0) >= required ? NamedTextColor.GREEN : NamedTextColor.GRAY))));
            if (view.globalGrandmasterCapReached()) {
                body.add(DialogBody.plainMessage(Component.text(
                        "Der serverweite Großmeister-Slot dieser Hauptberufsart ist bereits belegt.",
                        NamedTextColor.RED)));
            }
        }

        List<ActionButton> actions = new ArrayList<>();
        if (view.nextRank() != null) {
            actions.add(dialogueEngine.actionButton(Component.text("Material beitragen"), NamedTextColor.GREEN,
                    target -> openNpcContribution(target, currentNpc)));
            if (guildCanManage(player, currentNpc)) {
                actions.add(dialogueEngine.actionButton(Component.text("Rang aufwerten"), NamedTextColor.GOLD,
                        target -> {
                            var result = npcProgression.upgrade(target, currentNpc.id());
                            target.sendMessage(Component.text(npcUpgradeMessage(result),
                                    result == ProfessionNpcProgressionService.Result.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED));
                            openNpcProgress(target, PixelRPGPlugin.getInstance().getNpcManager().getById(currentNpc.id()).orElse(currentNpc));
                        }));
            }
        }
        actions.add(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, target -> onInteract(target, currentNpc)));
        dialogueEngine.openMultiAction(player, Component.text("NPC-Aufwertung", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openNpcContribution(Player player, RPGNpc npc) {
        DialogInput materialInput = DialogInput.text("material", 260, Component.text("Material", NamedTextColor.WHITE), true, "", 32, null);
        DialogInput amountInput = DialogInput.text("amount", 160, Component.text("Menge", NamedTextColor.WHITE), true, "1", 8, null);
        dialogueEngine.openTextInputAction(player, Component.text("Material beitragen", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Gib Materialnamen und Menge für den nächsten Rang ein.", NamedTextColor.WHITE))),
                materialInput, Component.text("Weiter"), NamedTextColor.GREEN, (target, response) -> {
                    String materialName = response.getText("material");
                    String amountText = response.getText("amount");
                    Material material = materialName == null ? null : Material.matchMaterial(materialName.trim().toUpperCase(java.util.Locale.ROOT));
                    int amount;
                    try { amount = Integer.parseInt(amountText == null ? "0" : amountText.trim()); }
                    catch (NumberFormatException ignored) { amount = 0; }
                    if (material == null || amount <= 0 || !npcProgression.contribute(target, npc.id(), material, amount)) {
                        target.sendMessage(Component.text("Material, Menge oder Berechtigung sind ungültig.", NamedTextColor.RED));
                    } else {
                        target.sendMessage(Component.text("Material wurde für den nächsten NPC-Rang eingelagert.", NamedTextColor.GREEN));
                    }
                    openNpcProgress(target, PixelRPGPlugin.getInstance().getNpcManager().getById(npc.id()).orElse(npc));
                });
    }

    private boolean guildCanManage(Player player, RPGNpc npc) {
        Guild guild = PixelRPGPlugin.getInstance().getGuildManager().getGuild(player.getUniqueId()).orElse(null);
        return guild != null && guild.id().equals(npc.kingdomId()) && guild.canManageTerritory(player.getUniqueId());
    }

    private String npcUpgradeMessage(ProfessionNpcProgressionService.Result result) {
        return switch (result) {
            case SUCCESS -> "Der Berufs-NPC wurde aufgewertet.";
            case CITY_LEVEL -> "Das Stadtlevel erlaubt den nächsten NPC-Rang noch nicht.";
            case INFRASTRUCTURE -> "Die benötigte Berufs-Infrastruktur fehlt am NPC.";
            case SPECIALIZATION -> "Es stehen nicht genügend Spezialisierungspunkte zur Verfügung.";
            case MATERIALS -> "Die geforderten Materialien fehlen noch.";
            case GOLD -> "Die Gildenkasse enthält nicht genügend Goldtaler.";
            case GRANDMASTER_CAP -> "Der globale Großmeister-Slot ist bereits belegt.";
            case MEISTERBRIEF_ISSUED -> "Ein Meisterbrief liegt bereits in deinem Inventar oder wurde jetzt ausgestellt.";
            case MEISTERBRIEF_REQUIRED -> "Der unterschriebene Meisterbrief wird benötigt.";
            default -> "Die NPC-Aufwertung konnte nicht durchgeführt werden.";
        };
    }
}
