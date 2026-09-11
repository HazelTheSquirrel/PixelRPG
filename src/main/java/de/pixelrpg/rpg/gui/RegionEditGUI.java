package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.region.PixelRegion;
import de.pixelrpg.rpg.region.RegionFlag;
import de.pixelrpg.rpg.region.RegionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Provides the admin edit menu for global and player-created PixelRPG regions. */
public final class RegionEditGUI extends AbstractGUI {
    private final RegionManager regions;
    private final PixelRegion region;
    private final boolean detail;

    private RegionEditGUI(RegionManager regions, PixelRegion region, boolean detail) {
        super(54, detail
                ? Component.text("Region: " + region.name(), NamedTextColor.DARK_GREEN)
                : Component.text("Region-Edit", NamedTextColor.DARK_GREEN));
        this.regions = regions;
        this.region = region;
        this.detail = detail;
    }

    public static void open(Player player, RegionManager regions) {
        new RegionEditGUI(regions, regions.globalRegion(player.getWorld().getName()), false).open(player);
    }

    private static void openList(Player player, RegionManager regions) {
        new RegionEditGUI(regions, regions.globalRegion(player.getWorld().getName()), false).open(player);
    }

    private static void openDetail(Player player, RegionManager regions, PixelRegion region) {
        new RegionEditGUI(regions, region, true).open(player);
    }

    @Override
    protected void populate() {
        if (detail) populateDetail();
        else populateList();
    }

    private void populateList() {
        List<PixelRegion> entries = new ArrayList<>();
        entries.add(regions.globalRegion(region.worldName()));
        entries.addAll(regions.all());

        for (int slot = 0; slot < Math.min(entries.size(), 53); slot++) {
            PixelRegion selected = entries.get(slot);
            Material material = selected.isGlobal() ? Material.NETHER_STAR : Material.FILLED_MAP;
            ItemStack item = createItem(material,
                    selected.isGlobal() ? "Globale Region: " + selected.worldName() : selected.name(),
                    selected.isGlobal() ? NamedTextColor.GOLD : NamedTextColor.GREEN,
                    List.of(
                            Component.text("Welt: " + selected.worldName(), NamedTextColor.GRAY),
                            Component.text("Flags: " + selected.flags().size(), NamedTextColor.GRAY),
                            Component.text("Klick: Bearbeiten", NamedTextColor.YELLOW)
                    ));
            setItem(slot, item, event -> {
                if (event.getWhoClicked() instanceof Player player) openDetail(player, regions, selected);
            });
        }

        setItem(49, createItem(Material.BARRIER, "Schließen", NamedTextColor.RED, List.of()));
    }

    private void populateDetail() {
        setItem(49, createItem(Material.ARROW, "Zurück", NamedTextColor.YELLOW, List.of()), event -> {
            if (event.getWhoClicked() instanceof Player player) openList(player, regions);
        });

        int slot = 10;
        for (RegionFlag flag : RegionFlag.values()) {
            boolean enabled = region.flag(flag);
            Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(enabled ? "Erlaubt" : "Verboten", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Klick: umschalten", NamedTextColor.YELLOW));
            setItem(slot, createItem(material, flag.name(), enabled ? NamedTextColor.GREEN : NamedTextColor.RED, lore), event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                boolean next = !region.flag(flag);
                if (region.isGlobal()) {
                    regions.setGlobalFlag(region.worldName(), flag, next);
                } else {
                    region.setFlag(flag, next);
                    regions.save();
                }
                openDetail(player, regions, region);
            });
            slot++;
        }
    }

    private ItemStack createItem(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
