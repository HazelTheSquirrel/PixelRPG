package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.companion.Companion;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.guild.GuildManager;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

public final class QuickActionsDialogListener implements Listener {
    private static final Key PROFILE_ACTION = Key.key("pixelrpg:character_card/profile");
    private static final Key ACTIVE_QUESTS_ACTION = Key.key("pixelrpg:character_card/active_quests");
    private static final Key COMPANIONS_ACTION = Key.key("pixelrpg:character_card/companions");
    private static final Key PROFESSIONS_ACTION = Key.key("pixelrpg:character_card/professions");
    private static final Key GUILD_ACTION = Key.key("pixelrpg:character_card/guild");
    private static final Key CLOSE_ACTION = Key.key("pixelrpg:character_card/close");
    private final PixelRPGPlugin plugin;
    private final QuickActionsDialogService service;
    private final CompanionService companionService;
    private final CompanionDialog companionDialog;
    private final ProfessionDialog professionDialog;
    private final CharacterCardScoreboardService characterCardScoreboard;
    private final GuildManager guildManager;
    private final InviteDialogService inviteDialogService;

    public QuickActionsDialogListener(QuickActionsDialogService service, CompanionService companionService, InviteDialogService inviteDialogService) {
        this.plugin = PixelRPGPlugin.getInstance();
        this.service = service;
        this.companionService = companionService;
        this.guildManager = GuildManager.getInstance(plugin, service.profileManager());
        this.inviteDialogService = inviteDialogService;
        DialogueEngine dialogueEngine = new DialogueEngine();
        this.companionDialog = new CompanionDialog(companionService, dialogueEngine, service);
        this.professionDialog = new ProfessionDialog(service.profileManager(), dialogueEngine, service, service.questManager());
        this.characterCardScoreboard = new CharacterCardScoreboardService(plugin, service.profileManager(), service.statEngine());
        this.characterCardScoreboard.start();
    }

    public CompanionService companionService() { return companionService; }

    /** Handles the profile action from the native G character card. */
    @EventHandler
    public void onProfileAction(PlayerCustomClickEvent event) { handlePlayerAction(event, PROFILE_ACTION, player -> service.openCharacterProfile(player, companionDialog, professionDialog)); }

    /** Handles the active-quests action from the native G character card. */
    @EventHandler
    public void onActiveQuestsAction(PlayerCustomClickEvent event) { handlePlayerAction(event, ACTIVE_QUESTS_ACTION, player -> service.openActiveQuests(player, companionDialog, professionDialog)); }

    /** Handles the companion action from the native G character card. */
    @EventHandler
    public void onCompanionAction(PlayerCustomClickEvent event) { handlePlayerAction(event, COMPANIONS_ACTION, companionDialog::open); }

    /** Handles the profession action from the native G character card. */
    @EventHandler
    public void onProfessionAction(PlayerCustomClickEvent event) { handlePlayerAction(event, PROFESSIONS_ACTION, professionDialog::open); }

    /** Handles guild creation or guild management from the native G character card. */
    @EventHandler
    public void onGuildAction(PlayerCustomClickEvent event) {
        handlePlayerAction(event, GUILD_ACTION, player -> new GuildDialog(guildManager, service.profileManager(), new DialogueEngine(), service, inviteDialogService).open(player));
    }

    /** Handles the validated companion equipment action from its native dialog. */
    @EventHandler
    public void onCompanionEquipAction(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        if (!CompanionDialog.isEquipAction(event.getIdentifier())) return;
        Player player = connection.getPlayer();
        if (!service.isAvailable(player)) return;
        String companionId = CompanionDialog.companionIdFromEquipAction(event.getIdentifier());
        if (companionId == null) return;
        Companion companion = companionService.getCompanions(player.getUniqueId()).stream().filter(candidate -> candidate.id().equals(companionId)).findFirst().orElse(null);
        if (companion == null || companion.entityType() != EntityType.MANNEQUIN) return;
        companionService.openEquipment(player, companion);
    }

    /** Handles closing the native character card. */
    @EventHandler
    public void onCloseAction(PlayerCustomClickEvent event) { handlePlayerAction(event, CLOSE_ACTION, Player::closeDialog); }

    /** Stops character-card and companion services when PixelRPG is disabled. */
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