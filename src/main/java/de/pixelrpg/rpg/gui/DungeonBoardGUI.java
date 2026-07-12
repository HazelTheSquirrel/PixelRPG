// src/main/java/de/pixelrpg/rpg/gui/DungeonBoardGUI.java (VOLLSTÄNDIG, ersetzt alte Datei — currentNpcId wird durchgereicht, Zurück-Bug behoben)
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dungeon.DungeonDefinition;
import de.pixelrpg.rpg.dungeon.DungeonInstanceManager;
import de.pixelrpg.rpg.dungeon.DungeonRepository;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class DungeonBoardGUI extends AbstractGUI {

    private final Player viewer;
    private final DungeonRepository repository;
    private final DungeonInstanceManager instanceManager;
    private final PlayerProfileManager profileManager;
    private final String originNpcId;

    public DungeonBoardGUI(Player viewer, DungeonRepository repository, DungeonInstanceManager instanceManager,
                            PlayerProfileManager profileManager, String originNpcId) {
        super(54, Component.text("Dungeons", NamedTextColor.DARK_RED));
        this.viewer = viewer;
        this.repository = repository;
        this.instanceManager = instanceManager;
        this.profileManager = profileManager;
        this.originNpcId = originNpcId;
    }

    @Override
    protected void populate() {
        List<DungeonDefinition> dungeons = repository.getAll();

        int slot = 0;
        for (DungeonDefinition definition : dungeons) {
            if (slot >= 45) {
                break;
            }

            ItemStack item = new ItemStack(Material.NETHERITE_HOE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(definition.getDisplayName(), NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Rank: " + definition.getMinRank().name() + " - " + definition.getMaxRank().name(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Cooldown: " + definition.getCooldownMinutes() + " minutes", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Click to enter.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);

            setItem(slot, item, event -> {
                DungeonInstanceManager.EnterResult result = instanceManager.enterDungeon(viewer, definition.getId());
                switch (result) {
                    case SUCCESS -> viewer.closeInventory();
                    case RANK_TOO_LOW -> viewer.sendMessage(Component.text("Your rank is too low for this dungeon.", NamedTextColor.RED));
                    case ON_COOLDOWN -> viewer.sendMessage(Component.text("This dungeon is on cooldown for you.", NamedTextColor.RED));
                    case NO_SCHEMATIC -> viewer.sendMessage(Component.text("This dungeon has no built structure yet.", NamedTextColor.RED));
                    case DUNGEON_NOT_FOUND -> viewer.sendMessage(Component.text("Dungeon not found.", NamedTextColor.RED));
                }
            });

            slot++;
        }

        setItem(49, backButton(), event -> new TravelGUI(
                viewer, PixelRPGPlugin.getInstance().getNpcManager(), profileManager, originNpcId
        ).open(viewer));
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}