// src/main/java/de/pixelrpg/rpg/gui/ReceptionGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — 54 Slots, HUD-Buttons zusammengeführt, Storylog entfernt, Beitritt-Sound)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.leaderboard.LeaderboardType;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ReceptionGUI extends AbstractGUI {

    private final Player viewer;
    private final PlayerProfileManager profileManager;

    public ReceptionGUI(Player viewer, PlayerProfileManager profileManager) {
        super(54, Component.text("Guild Reception", NamedTextColor.DARK_AQUA));
        this.viewer = viewer;
        this.profileManager = profileManager;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        boolean registered = profile != null && profile.isRegisteredInGuild();

        setItem(10, buildRegisterButton(registered), event -> {
            if (registered) {
                viewer.sendMessage(Component.text("You are already registered.", NamedTextColor.YELLOW));
            } else {
                profileManager.registerToGuild(viewer);
                open(viewer);
            }
        });

        setItem(13, buildCardItem(profile, registered));

        setItem(16, buildLeaveButton(registered), event -> {
            if (!registered) {
                viewer.sendMessage(Component.text("You are not a guild member yet.", NamedTextColor.RED));
                return;
            }
            new ReceptionConfirmGUI(viewer, profileManager).open(viewer);
        });

        setItem(20, buildClassButton(profile, registered), event -> {
            if (!registered) {
                viewer.sendMessage(Component.text("You must be a registered member.", NamedTextColor.RED));
                return;
            }
            new ClassSelectionGUI(viewer, profileManager).open(viewer);
        });

        setItem(22, buildAttributeButton(), event -> {
            if (!registered) {
                viewer.sendMessage(Component.text("You must be a registered member.", NamedTextColor.RED));
                return;
            }
            new AttributeTraderGUI(viewer, profileManager, PixelRPGPlugin.getInstance().getStatEngine()).open(viewer);
        });

        setItem(24, buildBankButton(), event -> {
            if (!registered) {
                viewer.sendMessage(Component.text("You must be a registered member.", NamedTextColor.RED));
                return;
            }
            new BankGUI(viewer, profileManager).open(viewer);
        });

        setItem(28, buildPartyButton(), event -> {
            if (!registered) {
                viewer.sendMessage(Component.text("You must be a registered member.", NamedTextColor.RED));
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
                viewer.sendMessage(Component.text("You must be a registered member.", NamedTextColor.RED));
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
        meta.displayName(Component.text(registered ? "Membership Active" : "Register",
                        registered ? NamedTextColor.GRAY : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCardItem(PlayerProfile profile, boolean registered) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(viewer.getName() + "'s Guild Card", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (registered && profile != null) {
            Rank rank = profile.getRank();
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                    .append(Component.text("Member", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rank: ", NamedTextColor.GRAY)
                    .append(rank.displayName())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Class: ", NamedTextColor.GRAY)
                    .append(profile.getPlayerClass().displayName())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Money: " + String.format("%.2f", profile.getMoney()) + " Gold", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            if (profile.getSelectedTitle() != null) {
                lore.add(Component.text("Title: " + profile.getSelectedTitle(), NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                    .append(Component.text("Not Registered", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLeaveButton(boolean registered) {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Resign from the Guild", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        if (registered) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Warning: resets all progress!", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClassButton(PlayerProfile profile, boolean registered) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Profession Choice", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (registered && profile != null && profile.getPlayerClass().isNone()) {
            lore.add(Component.text("Unlocks at Rank C.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else if (registered && profile != null) {
            lore.add(Component.text("Class: ", NamedTextColor.GRAY)
                    .append(profile.getPlayerClass().displayName())
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAttributeButton() {
        return simpleButton(Material.EXPERIENCE_BOTTLE, "Attribute Distribution", NamedTextColor.AQUA);
    }

    private ItemStack buildBankButton() {
        return simpleButton(Material.GOLD_INGOT, "Guild Bank", NamedTextColor.GOLD);
    }

    private ItemStack buildPartyButton() {
        return simpleButton(Material.TOTEM_OF_UNDYING, "Party", NamedTextColor.LIGHT_PURPLE);
    }

    private ItemStack buildAchievementsButton() {
        return simpleButton(Material.EMERALD, "Achievements", NamedTextColor.GREEN);
    }

    private ItemStack buildLeaderboardButton() {
        return simpleButton(Material.GOLDEN_HELMET, "Leaderboard", NamedTextColor.GOLD);
    }

    private ItemStack buildTitlesButton() {
        return simpleButton(Material.NAME_TAG, "Titles", NamedTextColor.LIGHT_PURPLE);
    }

    private ItemStack buildHudToggleButton(PlayerProfile profile) {
        boolean mainEnabled = profile != null && profile.isScoreboardEnabled();
        boolean partyEnabled = profile != null && profile.isPartyHudEnabled();
        ItemStack item = new ItemStack(mainEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("HUD Display", mainEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Main HUD: " + (mainEnabled ? "ON" : "OFF"), mainEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Party HUD: " + (partyEnabled ? "ON" : "OFF"), partyEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Left-click: toggle Main HUD", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Right-click: toggle Party HUD", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack simpleButton(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}