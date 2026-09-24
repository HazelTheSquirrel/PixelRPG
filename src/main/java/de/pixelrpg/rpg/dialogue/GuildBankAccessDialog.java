package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildBankService;
import de.pixelrpg.rpg.guild.GuildManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/** Adds the guild-bank entry to the existing Banker interaction without replacing personal banking. */
public final class GuildBankAccessDialog {
    private final GuildManager guilds;
    private final GuildBankService guildBank;
    private final DialogueEngine dialogue;
    private final BankDialog personalBank;
    private final Consumer<Player> backAction;

    public GuildBankAccessDialog(GuildManager guilds, GuildBankService guildBank, DialogueEngine dialogue, BankDialog personalBank) {
        this.guilds = guilds;
        this.guildBank = guildBank;
        this.dialogue = dialogue;
        this.personalBank = personalBank;
    }

    public void open(Player player) {
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) {
            personalBank.open(player);
            return;
        }
        dialogue.openMultiAction(player, Component.text("Bank", NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text("Persönliches Bankfach und gemeinsame Gildenbank.", NamedTextColor.WHITE))),
                List.of(
                        dialogue.actionButton(Component.text("Persönliche Bank", NamedTextColor.AQUA), NamedTextColor.AQUA, personalBank::open),
                        dialogue.actionButton(Component.text("Gildenbank", NamedTextColor.GOLD), NamedTextColor.GOLD, guildBank::open)
                ), 1);
    }
}
