package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dialogue.BankDialog;
import de.pixelrpg.rpg.dialogue.BankInventoryListener;
import de.pixelrpg.rpg.dialogue.BankStorageService;
import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.GuildBankAccessDialog;
import de.pixelrpg.rpg.guild.GuildBankService;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.trade.TradeDepotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class BankerBehavior implements NpcBehavior {
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;
    private final BankStorageService bankStorage;
    private final TradeDepotManager tradeDepot;
    private final GuildBankService guildBankService;
    private final GuildBankAccessDialog guildBankAccess;

    public BankerBehavior(PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
        this.bankStorage = new BankStorageService(PixelRPGPlugin.getInstance());
        PixelRPGPlugin.getInstance().getServer().getPluginManager().registerEvents(new BankInventoryListener(bankStorage), PixelRPGPlugin.getInstance());
        this.tradeDepot = new TradeDepotManager(PixelRPGPlugin.getInstance(), profileManager, bankStorage, dialogueEngine);
        GuildManager guildManager = GuildManager.getInstance(PixelRPGPlugin.getInstance(), profileManager);
        this.guildBankService = new GuildBankService(PixelRPGPlugin.getInstance(), guildManager);
        BankDialog personalBank = new BankDialog(profileManager, dialogueEngine, bankStorage, tradeDepot);
        this.guildBankAccess = new GuildBankAccessDialog(guildManager, guildBankService, dialogueEngine, personalBank);
    }

    @Override public NpcType type() { return NpcType.BANKER; }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        if (!profileManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Du musst registriertes Rathausmitglied sein.", NamedTextColor.RED));
            return;
        }
        guildBankAccess.open(player);
    }

    public void shutdown() {
        guildBankService.shutdown();
        bankStorage.shutdown();
        tradeDepot.shutdown();
    }
}
