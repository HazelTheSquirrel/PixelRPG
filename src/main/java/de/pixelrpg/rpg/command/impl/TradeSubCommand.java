package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.trade.PlayerTradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Provides the player-facing direct item-for-gold trade commands. */
public final class TradeSubCommand implements SubCommand {
    private static final List<String> ACTIONS = List.of("offer", "accept", "decline", "cancel");
    private final PlayerTradeManager trades;
    public TradeSubCommand(PlayerTradeManager trades) { this.trades = trades; }
    @Override public String name() { return "trade"; }
    @Override public String permission() { return "rpg.member"; }
    @Override public String description() { return "Direkten Spielerhandel verwalten"; }
    @Override public String usage() { return "/pixelrpg trade <offer|accept|decline|cancel>"; }
    @Override public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Component.text("Nur Spieler können handeln.", NamedTextColor.RED)); return true; }
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "offer" -> offer(player, args);
            case "accept" -> { boolean ok=trades.accept(player); if(!ok) player.sendMessage(Component.text("Kein handelbares Angebot konnte abgeschlossen werden.", NamedTextColor.RED)); yield true; }
            case "decline" -> { boolean ok=trades.decline(player); if(!ok) player.sendMessage(Component.text("Du hast kein offenes Handelsangebot.", NamedTextColor.YELLOW)); yield true; }
            case "cancel" -> { boolean ok=trades.cancel(player); if(!ok) player.sendMessage(Component.text("Du hast kein eigenes Handelsangebot.", NamedTextColor.YELLOW)); yield true; }
            default -> false;
        };
    }
    @Override public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return ACTIONS;
        if (args.length == 2 && args[0].equalsIgnoreCase("offer")) {
            String prefix=args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name->name.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        return List.of();
    }
    private boolean offer(Player seller, String[] args) {
        if (args.length != 3) { seller.sendMessage(Component.text("Verwendung: /pixelrpg trade offer <Spieler> <Preis>", NamedTextColor.YELLOW)); return true; }
        Player buyer=Bukkit.getPlayerExact(args[1]);
        if (buyer==null) { seller.sendMessage(Component.text("Dieser Spieler ist nicht online.", NamedTextColor.RED)); return true; }
        final double price;
        try { price=new BigDecimal(args[2].replace(',','.')).doubleValue(); } catch (NumberFormatException exception) { seller.sendMessage(Component.text("Der Preis ist ungültig.", NamedTextColor.RED)); return true; }
        if (!trades.offer(seller,buyer,price)) seller.sendMessage(Component.text("Handelsangebot konnte nicht erstellt werden. Prüfe Item, Preis oder offene Angebote.", NamedTextColor.RED));
        return true;
    }
}
