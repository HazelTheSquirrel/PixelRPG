package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.leaderboard.LeaderboardType;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ReceptionGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public ReceptionGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("reception.guild-reception-title"));
        this.viewer = viewer;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegisteredInGuild();

        setItem(10, buildRegisterButton(registered), event -> {
            if (registered) {
                lang.send(viewer, "reception.already-registered");
            } else {
                profileManager.registerToGuild(viewer);
                open(viewer);
            }
        });

        setItem(13, buildCardItem(profile, registered));

        setItem(16, buildLeaveButton(registered), event -> {
            if (!registered) {
                lang.send(viewer, "reception.not-member");
                return;
            }
            new ReceptionConfirmGUI(viewer, profileManager).open(viewer);
        });

        setItem(20, buildClassButton(profile, registered), event -> {
            if (!registered) {
                lang.send(viewer, "common.not-registered");
                return;
            }
            new ClassSelectionGUI(viewer, profileManager).open(viewer);
        });

        setItem(22, buildAttributeButton(), event -> {
            if (!registered) {
                lang.send(viewer, "common.not-registered");
                return;
            }
            new AttributeTraderGUI(viewer, profileManager, PixelRPGPlugin.getInstance().getStatEngine()).open(viewer);
        });

        setItem(24, buildBankButton(), event -> {
            if (!registered) {
                lang.send(viewer, "common.not-registered");
                return;
            }
            new BankGUI(viewer, profileManager).open(viewer);
        });

        setItem(28, buildPartyButton(), event -> {
            if (!registered) {
                lang.send(viewer, "common.not-registered");
                return;
            }
            new PartyGUI(viewer, PixelRPGPlugin.getInstance().getPartyManager(), profileManager).open(viewer);
        });

        setItem(30, buildAchievementsButton(), event ->
                new AchievementsGUI(viewer, PixelRPGPlugin.getInstance().getAchievementManager().getRepository(), profileManager).open(viewer));

        setItem(32, buildLeaderboardButton(), event ->
                LeaderboardGUI.openAsync(viewer, profileManager, LeaderboardType.EXPERIENCE,
                        PixelRPGPlugin.getInstance().getConfig().getInt("leaderboard.default-limit", 10)));

        setItem(34, buildTitlesButton(), event -> {
            if (!registered) {
                lang.send(viewer, "common.not-registered");
                return;
            }
            new TitleSelectionGUI(viewer, profileManager).open(viewer);
        });

        setItem(40, buildHudToggleButton(profile), event -> {
            if (!registered) {
                return;
            }
            if (event.getClick().isRightClick()) {
                if (profile != null) {
                    profile.setPartyHudEnabled(!profile.isPartyHudEnabled());
                    profileManager.saveProfileAsync(viewer.getUniqueId());
                }
            } else {
                PixelRPGPlugin.getInstance().getScoreboardService().toggle(viewer);
            }
            open(viewer);
        });
    }

    private ItemStack buildRegisterButton(boolean registered) {
        ItemStack item = new ItemStack(registered ? Material.GRAY_DYE : Material.GREEN_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(registered ? "reception.membership-active" : "reception.register-button")
                .color(registered ? NamedTextColor.GRAY : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCardItem(PlayerProfile profile, boolean registered) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("reception.card-title", "player", viewer.getName())
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (registered && profile != null) {
            Rank rank = profile.getRank();
            lore.add(lang.get("reception.status")
                    .append(lang.get("reception.status-member").color(NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(lang.get("reception.rank-label").append(rank.displayName())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(lang.get("reception.class-label").append(profile.getPlayerClass().displayName())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(lang.get("reception.money-label", "amount", String.format("%.2f", profile.getMoney()))
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            if (profile.getSelectedTitle() != null) {
                lore.add(lang.get("reception.title-label", "title", profile.getSelectedTitle())
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(lang.get("reception.status")
                    .append(lang.get("reception.status-not-registered").color(NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLeaveButton(boolean registered) {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("reception.resign-button")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        if (registered) {
            List<Component> lore = new ArrayList<>();
            lore.add(lang.get("reception.resign-warning")
                    .color(NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClassButton(PlayerProfile profile, boolean registered) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("reception.profession-choice")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (registered && profile != null && profile.getPlayerClass().isNone()) {
            lore.add(lang.get("reception.profession-unlock").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else if (registered && profile != null) {
            lore.add(lang.get("reception.class-label").append(profile.getPlayerClass().displayName())
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAttributeButton() {
        return simpleButton(Material.EXPERIENCE_BOTTLE, "reception.attribute-distribution", NamedTextColor.AQUA);
    }

    private ItemStack buildBankButton() {
        return simpleButton(Material.GOLD_INGOT, "reception.guild-bank", NamedTextColor.GOLD);
    }

    private ItemStack buildPartyButton() {
        return simpleButton(Material.TOTEM_OF_UNDYING, "reception.party-button", NamedTextColor.LIGHT_PURPLE);
    }

    private ItemStack buildAchievementsButton() {
        return simpleButton(Material.EMERALD, "reception.achievements-button", NamedTextColor.GREEN);
    }

    private ItemStack buildLeaderboardButton() {
        return simpleButton(Material.GOLDEN_HELMET, "reception.leaderboard-button", NamedTextColor.GOLD);
    }

    private ItemStack buildTitlesButton() {
        return simpleButton(Material.NAME_TAG, "reception.titles-button", NamedTextColor.LIGHT_PURPLE);
    }

    private ItemStack buildHudToggleButton(PlayerProfile profile) {
        boolean mainEnabled = profile != null && profile.isScoreboardEnabled();
        boolean partyEnabled = profile != null && profile.isPartyHudEnabled();
        ItemStack item = new ItemStack(mainEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("reception.hud-display")
                .color(mainEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lang.get("reception.main-hud", "state", plainOnOff(mainEnabled))
                        .color(mainEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                lang.get("reception.party-hud", "state", plainOnOff(partyEnabled))
                        .color(partyEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                lang.get("reception.hud-left-click").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                lang.get("reception.hud-right-click").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private String plainOnOff(boolean state) {
        return PlainTextComponentSerializer.plainText().serialize(lang.get(state ? "hud.on" : "hud.off"));
    }

    private ItemStack simpleButton(Material material, String key, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(key).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}