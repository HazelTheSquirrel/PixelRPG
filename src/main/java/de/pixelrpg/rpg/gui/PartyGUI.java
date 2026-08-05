package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.party.Party;
import de.pixelrpg.rpg.party.PartyManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PartyGUI extends AbstractGUI {

    private final Player viewer;
    private final PartyManager partyManager;
    private final de.pixelrpg.rpg.player.PlayerProfileManager profileManager;
    private final LanguageManager lang;

    public PartyGUI(Player viewer, PartyManager partyManager, de.pixelrpg.rpg.player.PlayerProfileManager profileManager) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("party.gui-title"));
        this.viewer = viewer;
        this.partyManager = partyManager;
        this.profileManager = profileManager;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        Optional<Party> partyOpt = partyManager.getParty(viewer.getUniqueId());

        if (partyOpt.isEmpty()) {
            setItem(22, buildActionItem(Material.LIME_DYE, "party.create-button", NamedTextColor.GREEN), event -> {
                partyManager.createParty(viewer.getUniqueId());
                lang.send(viewer, "party.created");
                open(viewer);
            });
            setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
            return;
        }

        Party party = partyOpt.get();
        int slot = 0;
        for (UUID member : party.getMembers()) {
            if (slot >= 36) {
                break;
            }
            setItem(slot, buildMemberItem(party, member));
            slot++;
        }

        boolean isLeader = party.isLeader(viewer.getUniqueId());

        if (isLeader) {
            setItem(40, buildActionItem(Material.BARRIER, "party.disband-button", NamedTextColor.RED), event -> {
                partyManager.disbandParty(party);
                viewer.closeInventory();
            });
        } else {
            setItem(40, buildActionItem(Material.RED_DYE, "party.leave-button", NamedTextColor.RED), event -> {
                partyManager.leaveParty(viewer);
                viewer.closeInventory();
            });
        }

        setItem(49, backButton(), event -> new ReceptionGUI(viewer, profileManager).open(viewer));
    }

    private ItemStack buildMemberItem(Party party, UUID member) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member);
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);

        boolean isLeader = party.isLeader(member);
        meta.displayName(Component.text(name, isLeader ? NamedTextColor.GOLD : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(lang.get(isLeader ? "party.leader-label" : "party.member-label")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(lang.get(offlinePlayer.isOnline() ? "party.online-label" : "party.offline-label")
                .color(offlinePlayer.isOnline() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionItem(Material material, String nameKey, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(nameKey).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}