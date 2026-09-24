package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.party.PartyManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.gui.PartyGUI;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Native reception dialog used to register a player and expose guild and party entries. */
public final class ReceptionDialog {
    private final Player player;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final PartyManager partyManager;
    private final GuildManager guildManager;

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this(player, profileManager, dialogueEngine, null, null);
    }

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager) {
        this(player, profileManager, dialogueEngine, partyManager, null);
    }

    public ReceptionDialog(Player player, PlayerProfileManager profileManager, DialogueEngine dialogueEngine, PartyManager partyManager, GuildManager guildManager) {
        this.player = player;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.partyManager = partyManager;
        this.guildManager = guildManager;
    }

    public void open() {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegistered();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Willkommen. Hier kannst du dein PixelRPG-Profil registrieren und verwalten.", NamedTextColor.WHITE)));
        body.add(DialogBody.plainMessage(Component.text("Status: ", NamedTextColor.GRAY).append(
                Component.text(registered ? "Mitglied" : "Nicht registriert", registered ? NamedTextColor.GREEN : NamedTextColor.RED))));

        List<ActionButton> actions = new ArrayList<>();
        if (!registered) {
            actions.add(dialogueEngine.actionButton(Component.text("Registrieren"), NamedTextColor.GREEN, target -> {
                profileManager.registerPlayer(target);
                new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
            }));
        } else {
            actions.add(dialogueEngine.actionButton(Component.text("PixelRPG-Registrierung aufheben"), NamedTextColor.RED, this::openLeaveConfirmation));
            if (partyManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Party", NamedTextColor.AQUA), NamedTextColor.AQUA,
                        target -> new PartyGUI(target, partyManager, profileManager, PixelRPGPlugin.getInstance().getInviteDialogService()).open(target)));
            }
            if (guildManager != null) {
                actions.add(dialogueEngine.actionButton(Component.text("Gilde", NamedTextColor.GOLD), NamedTextColor.GOLD,
                        target -> new GuildDialog(guildManager, profileManager, dialogueEngine).open(target)));
            }
            PixelRPGPlugin pixelRPG = PixelRPGPlugin.getInstance();
            if (pixelRPG != null && pixelRPG.getStoryManager() != null && pixelRPG.getQuestManager() != null) {
                pixelRPG.getStoryManager().getNextChapterFor(player.getUniqueId()).ifPresent(chapter ->
                        actions.add(dialogueEngine.actionButton(
                                Component.text("Story: " + chapter.title(), NamedTextColor.LIGHT_PURPLE),
                                NamedTextColor.LIGHT_PURPLE,
                                target -> openStoryChapter(target, chapter))));
            }
            actions.add(dialogueEngine.actionButton(
                    Component.text(profile.isScoreboardEnabled() ? "Scoreboard ausschalten" : "Scoreboard einschalten", NamedTextColor.GOLD),
                    NamedTextColor.GOLD, this::toggleScoreboard));
        }
        dialogueEngine.openMultiAction(player, Component.text("RPG-Registrierung", NamedTextColor.GOLD), body, actions, 1);
    }

    private void openStoryChapter(Player target, de.pixelrpg.rpg.story.StoryChapter chapter) {
        PixelRPGPlugin pixelRPG = PixelRPGPlugin.getInstance();
        if (pixelRPG == null || pixelRPG.getStoryManager() == null || pixelRPG.getQuestManager() == null) return;

        String questId = chapter.startQuestId();
        if (questId.isBlank()) {
            new StoryNpcDialogue(profileManager, pixelRPG.getStoryManager(), dialogueEngine).openChapter(target, chapter);
            return;
        }

        var quest = pixelRPG.getQuestManager().getRepository().getQuest(questId);
        if (quest == null) return;
        PlayerProfile profile = profileManager.getProfile(target.getUniqueId()).orElse(null);
        if (profile == null) return;

        boolean active = profile.hasActiveQuest(quest.id());
        boolean completed = profile.hasCompletedQuest(quest.id());
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(chapter.title(), NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text(quest.description(), NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text("Freigeschaltet ab Level " + chapter.requiredLevel(), NamedTextColor.AQUA))
        );
        List<ActionButton> actions = new ArrayList<>();
        if (!active && !completed) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest annehmen", NamedTextColor.GREEN), NamedTextColor.GREEN,
                    next -> {
                        PixelRPGPlugin current = PixelRPGPlugin.getInstance();
                        if (current != null && current.getQuestManager() != null) current.getQuestManager().acceptQuest(next, quest);
                        openStoryChapter(next, chapter);
                    }));
        } else if (active) {
            actions.add(dialogueEngine.actionButton(Component.text("Quest bereits aktiv", NamedTextColor.YELLOW), NamedTextColor.YELLOW,
                    next -> openStoryChapter(next, chapter)));
        }
        actions.add(dialogueEngine.actionButton(Component.text("Geschichte erfahren", NamedTextColor.LIGHT_PURPLE),
                next -> new StoryNpcDialogue(profileManager, pixelRPG.getStoryManager(), dialogueEngine).openChapter(next, chapter)));
        actions.add(dialogueEngine.actionButton(Component.text("Schließen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogueEngine.openMultiAction(target, Component.text("Story & Lore", NamedTextColor.GOLD), body, actions, 1);
    }

    private void toggleScoreboard(Player target) {
        var scoreboardService = PixelRPGPlugin.getInstance().getScoreboardService();
        if (scoreboardService == null) {
            new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
            return;
        }
        scoreboardService.setEnabled(target, !scoreboardService.isEnabled(target));
        new ReceptionDialog(target, profileManager, dialogueEngine, partyManager, guildManager).open();
    }

    private void openLeaveConfirmation(Player target) {
        ActionButton yes = dialogueEngine.actionButton(Component.text("Ja, Registrierung unwiderruflich aufheben"), NamedTextColor.RED, player -> {
            profileManager.unregisterPlayer(player);
            player.sendMessage(Component.text("Deine PixelRPG-Registrierung wurde aufgehoben. Dein Fortschritt wurde gelöscht.", NamedTextColor.GREEN));
        });
        ActionButton no = dialogueEngine.actionButton(Component.text("Nein, abbrechen"), NamedTextColor.GREEN,
                player -> new ReceptionDialog(player, profileManager, dialogueEngine, partyManager, guildManager).open());
        dialogueEngine.openConfirmation(target, Component.text("Registrierung aufheben?", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Warnung: Setzt deinen gesamten PixelRPG-Fortschritt zurück.", NamedTextColor.WHITE))), yes, no);
    }
}
