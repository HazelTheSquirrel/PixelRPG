package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.leaderboard.LeaderboardEntry;
import de.pixelrpg.rpg.leaderboard.LeaderboardType;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class LeaderboardGUI extends AbstractGUI {

    private final Player viewer;
    private final LeaderboardType type;
    private final List<LeaderboardEntry> entries;
    private final LanguageManager lang;

    private LeaderboardGUI(Player viewer, LeaderboardType type, List<LeaderboardEntry> entries) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager()
                .get("leaderboard.gui-title", "type", plainTypeLabel(PixelRPGPlugin.getInstance().getLanguageManager(), type)));
        this.viewer = viewer;
        this.type = type;
        this.entries = entries;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public static void openAsync(Player viewer, PlayerProfileManager profileManager, LeaderboardType type, int limit) {
        Bukkit.getScheduler().runTaskAsynchronously(PixelRPGPlugin.getInstance(), () -> {
            List<LeaderboardEntry> entries;
            try {
                entries = profileManager.getLeaderboard(type, limit);
            } catch (Exception e) {
                entries = List.of();
            }
            List<LeaderboardEntry> finalEntries = entries;
            Bukkit.getScheduler().runTask(PixelRPGPlugin.getInstance(), () ->
                    new LeaderboardGUI(viewer, type, finalEntries).open(viewer));
        });
    }

    @Override
    protected void populate() {
        setItem(1, typeButton(LeaderboardType.EXPERIENCE, "leaderboard.type-rank"),
                event -> openAsync(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager(), LeaderboardType.EXPERIENCE, defaultLimit()));
        setItem(2, typeButton(LeaderboardType.MONEY, "leaderboard.type-gold"),
                event -> openAsync(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager(), LeaderboardType.MONEY, defaultLimit()));
        setItem(3, typeButton(LeaderboardType.MOBS_KILLED, "leaderboard.type-kills"),
                event -> openAsync(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager(), LeaderboardType.MOBS_KILLED, defaultLimit()));
        setItem(4, typeButton(LeaderboardType.BOSSES_DEFEATED, "leaderboard.type-bosses"),
                event -> openAsync(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager(), LeaderboardType.BOSSES_DEFEATED, defaultLimit()));
        setItem(5, typeButton(LeaderboardType.DUNGEONS_CLEARED, "leaderboard.type-dungeons"),
                event -> openAsync(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager(), LeaderboardType.DUNGEONS_CLEARED, defaultLimit()));

        int slot = 9;
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (slot >= 45) {
                break;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid());
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(offlinePlayer);

            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            meta.displayName(Component.text("#" + rank + " " + name, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(formatValue(entry.value())
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);

            setItem(slot, item);
            slot++;
            rank++;
        }

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, PixelRPGPlugin.getInstance().getPlayerProfileManager()).open(viewer));
    }

    private ItemStack typeButton(LeaderboardType buttonType, String labelKey) {
        ItemStack item = new ItemStack(buttonType == type ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(labelKey)
                .color(buttonType == type ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Component formatValue(double value) {
        return type == LeaderboardType.MONEY
                ? lang.get("leaderboard.gold-suffix", "amount", String.format("%.2f", value))
                : Component.text(String.valueOf((long) value));
    }

    private int defaultLimit() {
        return PixelRPGPlugin.getInstance().getConfig().getInt("leaderboard.default-limit", 10);
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static String plainTypeLabel(LanguageManager lang, LeaderboardType type) {
        String key = switch (type) {
            case EXPERIENCE -> "leaderboard.type-rank";
            case MONEY -> "leaderboard.type-gold";
            case MOBS_KILLED -> "leaderboard.type-kills";
            case BOSSES_DEFEATED -> "leaderboard.type-bosses";
            case DUNGEONS_CLEARED -> "leaderboard.type-dungeons";
            case QUESTS_COMPLETED -> "quest.log-title";
        };
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lang.get(key));
    }
}