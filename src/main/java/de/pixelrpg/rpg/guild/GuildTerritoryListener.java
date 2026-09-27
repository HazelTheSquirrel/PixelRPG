package de.pixelrpg.rpg.guild;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** Protects physical guild boundary markers and delegates validated marker edits to the territory service. */
public final class GuildTerritoryListener implements Listener {
    private final GuildTerritoryManager territories;

    public GuildTerritoryListener(GuildTerritoryManager territories) {
        this.territories = Objects.requireNonNull(territories);
    }

    /** Validates special guild-marker placement before Minecraft commits the block. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMarkerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!territories.isMarkerItem(item)) return;

        GuildTerritoryManager.OperationResult result = territories.placeMarker(event.getPlayer(), event.getBlockPlaced(), item);
        if (!result.success()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ComponentFactory.message(result.message(), false));
        } else {
            event.getPlayer().sendMessage(ComponentFactory.message(result.message(), true));
        }
    }

    /** Validates and performs authorized marker removal while returning the marker to the guild member. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMarkerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!territories.isProtectedBlock(block)) return;

        event.setCancelled(true);
        if (!territories.isMarkerBlock(block)) {
            event.getPlayer().sendMessage(ComponentFactory.message("Dieser Block schützt einen Gilden-Grenzmarker und darf nicht entfernt werden.", false));
            return;
        }

        GuildTerritoryManager.OperationResult result = territories.removeMarker(event.getPlayer(), block);
        event.getPlayer().sendMessage(ComponentFactory.message(result.message(), result.success()));
    }

    /** Prevents explosions from destroying markers or their support blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (territories.containsProtectedBlock(event.blockList())) event.setCancelled(true);
    }

    /** Prevents block explosions from destroying markers or their support blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (territories.containsProtectedBlock(event.blockList())) event.setCancelled(true);
    }

    /** Prevents pistons from moving markers or their support blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (territories.containsProtectedBlock(event.getBlocks())) event.setCancelled(true);
    }

    /** Prevents sticky pistons from moving markers or their support blocks during retraction. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (territories.containsProtectedBlock(event.getBlocks())) event.setCancelled(true);
    }

    /** Prevents flowing liquids from breaking a boundary marker. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (territories.isProtectedBlock(event.getToBlock())) event.setCancelled(true);
    }

    /** Keeps an existing marker stable when Minecraft performs a marker physics update. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMarkerPhysics(BlockPhysicsEvent event) {
        if (territories.isMarkerBlock(event.getBlock())) event.setCancelled(true);
    }

    /** Prevents non-player block changes from destroying a boundary marker or its support. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (territories.isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    private static final class ComponentFactory {
        private static net.kyori.adventure.text.Component message(String message, boolean success) {
            return net.kyori.adventure.text.Component.text(
                    message,
                    success ? net.kyori.adventure.text.format.NamedTextColor.GREEN : net.kyori.adventure.text.format.NamedTextColor.RED
            );
        }
    }
}
