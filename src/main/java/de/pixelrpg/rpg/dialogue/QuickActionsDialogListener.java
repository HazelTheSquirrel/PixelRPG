package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.companion.CompanionService;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

public final class QuickActionsDialogListener implements Listener {
    private static final Key PROFILE_ACTION = Key.key("pixelrpg:character_card/profile");
    private static final Key COMPANIONS_ACTION = Key.key("pixelrpg:character_card/companions");
    private static final Key PROFESSIONS_ACTION = Key.key("pixelrpg:character_card/professions");
    private static final Key CLOSE_ACTION = Key.key("pixelrpg:character_card/close");

    private final PixelRPGPlugin plugin;
    private final QuickActionsDialogService service;
    private final CompanionService companionService;
    private final CompanionDialog companionDialog;
    private final ProfessionDialog professionDialog;
    private final CharacterCardScoreboardService characterCardScoreboard;

    public QuickActionsDialogListener(QuickActionsDialogService service) {
        this.plugin = PixelRPGPlugin.getInstance();
        this.service = service;
        DialogueEngine dialogueEngine = new DialogueEngine();
        this.companionService = new CompanionService(plugin);
        this.companionDialog = new CompanionDialog(companionService, dialogueEngine);
        this.professionDialog = new ProfessionDialog(service.profileManager(), dialogueEngine);
        this.characterCardScoreboard = new CharacterCardScoreboardService(plugin, service.profileManager(), service.statEngine());
        this.characterCardScoreboard.start();
    }

    /** Opens the dynamic player-specific character profile from the G quick action. */
    @EventHandler
    public void onProfileAction(PlayerCustomClickEvent event) {
        handlePlayerAction(event, PROFILE_ACTION,
                player -> service.openCharacterProfile(player, companionDialog, professionDialog));
    }

    /** Opens the companion section from the direct G character card. */
    @EventHandler
    public void onCompanionAction(PlayerCustomClickEvent event) {
        handlePlayerAction(event, COMPANIONS_ACTION, companionDialog::open);
    }

    /** Opens the profession section from the direct G character card. */
    @EventHandler
    public void onProfessionAction(PlayerCustomClickEvent event) {
        handlePlayerAction(event, PROFESSIONS_ACTION, professionDialog::open);
    }

    /** Closes the native character card when the player selects the close action. */
    @EventHandler
    public void onCloseAction(PlayerCustomClickEvent event) {
        handlePlayerAction(event, CLOSE_ACTION, Player::closeDialog);
    }

    /** Stops the character-card and companion entity bridges when PixelRPG is disabled. */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        characterCardScoreboard.stop();
        companionService.shutdown();
    }

    private void handlePlayerAction(PlayerCustomClickEvent event, Key identifier, java.util.function.Consumer<Player> action) {
        if (!identifier.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        if (!service.isAvailable(player)) return;
        action.accept(player);
    }
}
