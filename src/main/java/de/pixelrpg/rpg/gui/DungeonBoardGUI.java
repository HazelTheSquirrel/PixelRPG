package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.dungeon.DungeonDefinition;
import de.pixelrpg.rpg.dungeon.DungeonInstanceManager;
import de.pixelrpg.rpg.dungeon.DungeonRepository;
import de.pixelrpg.rpg.lang.LanguageManager;
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
    private final LanguageManager lang;

    public DungeonBoardGUI(Player viewer, DungeonRepository repository, DungeonInstanceManager instanceManager,
                            PlayerProfileManager profileManager, String originNpcId) {
        super(54, PixelRPGPlugin.getInstance().getLanguageManager().get("dungeon.gui-title"));
        this.viewer = viewer;
        this.repository = repository;
        this.instanceManager = instanceManager;
        this.profileManager = profileManager;
        this.originNpcId = originNpcId;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
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
                    lang.get("dungeon.rank-range", "min", definition.getMinRank().name(), "max", definition.getMaxRank().name())
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    lang.get("dungeon.cooldown-minutes", "minutes", String.valueOf(definition.getCooldownMinutes()))
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    lang.get("dungeon.click-to-enter").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);

            setItem(slot, item, event -> {
                DungeonInstanceManager.EnterResult result = instanceManager.enterDungeon(viewer, definition.getId());
                switch (result) {
                    case SUCCESS -> viewer.closeInventory();
                    case RANK_TOO_LOW -> lang.send(viewer, "dungeon.rank-too-low");
                    case ON_COOLDOWN -> lang.send(viewer, "dungeon.on-cooldown");
                    case NO_SCHEMATIC -> lang.send(viewer, "dungeon.no-structure");
                    case DUNGEON_NOT_FOUND -> lang.send(viewer, "dungeon.not-found");
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
        meta.displayName(lang.get("common.back").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}