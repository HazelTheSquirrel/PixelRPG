package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TravelGUI extends AbstractGUI {

    private final Player viewer;
    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;
    private final String currentNpcId;
    private final LanguageManager lang;

    public TravelGUI(Player viewer, NpcManager npcManager, PlayerProfileManager profileManager, String currentNpcId) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("travel.gui-title"));
        this.viewer = viewer;
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.currentNpcId = currentNpcId;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profileManager.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        ItemStack dungeonButton = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta dungeonMeta = dungeonButton.getItemMeta();
        dungeonMeta.displayName(lang.get("travel.dungeons-button").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        dungeonButton.setItemMeta(dungeonMeta);
        setItem(49, dungeonButton, event -> new DungeonBoardGUI(
                viewer,
                PixelRPGPlugin.getInstance().getDungeonRepository(),
                PixelRPGPlugin.getInstance().getDungeonInstanceManager(),
                profileManager,
                currentNpcId
        ).open(viewer));

        int slot = 0;
        for (RPGNpc npc : npcManager.getAll()) {
            if (npc.type() != NpcType.TRAVEL) {
                continue;
            }
            if (currentNpcId != null && currentNpcId.equals(npc.id())) {
                continue;
            }
            if (slot >= 45) {
                break;
            }

            boolean unlocked = profile.hasUnlockedWaypoint(npc.id());

            ItemStack item = new ItemStack(unlocked ? Material.ENDER_PEARL : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(npc.name(), unlocked ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(unlocked
                    ? lang.get("travel.click-to-travel").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    : lang.get("travel.not-yet-discovered").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);

            setItem(slot, item, event -> {
                if (!unlocked) {
                    lang.send(viewer, "travel.not-discovered");
                    return;
                }
                viewer.teleportAsync(npc.location().clone().add(0, 1, 0));
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                viewer.closeInventory();
            });

            slot++;
        }
    }
}