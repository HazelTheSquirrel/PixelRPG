package de.pixelrpg.rpg.profession;

import org.bukkit.Material;
import de.pixelrpg.rpg.PixelRPGPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Connects actual gathering activities and manufacturing actions with profession XP. */
public final class ProfessionActivityListener implements Listener {
    private final ProfessionService professionService;
    private final PixelRPGPlugin plugin;
    private final Map<BlockKey, Long> placedBlocks = new ConcurrentHashMap<>();

    public ProfessionActivityListener(PixelRPGPlugin plugin, ProfessionService professionService) { this.plugin=plugin; this.professionService = professionService; }

    // Marks player-placed resources so immediate placement/break loops cannot generate gathering XP.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlocks.put(BlockKey.of(event.getBlock()), System.currentTimeMillis() + 15L * 60_000L);
    }

    // Vergibt Sammler-XP ausschließlich für tatsächlich gewonnene natürliche Ressourcen.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        BlockKey key = BlockKey.of(event.getBlock());
        Long expiry = placedBlocks.remove(key);
        if (expiry != null && expiry > System.currentTimeMillis()) return;
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        Profession profession = switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, IRON_ORE, DEEPSLATE_IRON_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE, NETHER_GOLD_ORE,
                 NETHER_QUARTZ_ORE, ANCIENT_DEBRIS -> Profession.MOUNTAIN_MINER;
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, SWEET_BERRY_BUSH, GLOW_BERRIES, KELP,
                 SEAGRASS, TALL_SEAGRASS, SUGAR_CANE, CACTUS, BAMBOO, VINE, GLOW_LICHEN, MOSS_BLOCK, PUMPKIN, MELON -> Profession.FARMER;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG, PALE_OAK_LOG,
                 CRIMSON_STEM, WARPED_STEM, OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
                 MANGROVE_WOOD, CHERRY_WOOD, PALE_OAK_WOOD, CRIMSON_HYPHAE, WARPED_HYPHAE, STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG,
                 STRIPPED_BIRCH_LOG, STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, STRIPPED_MANGROVE_LOG,
                 STRIPPED_CHERRY_LOG, STRIPPED_PALE_OAK_LOG, STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM -> Profession.WOODCUTTER;
            default -> null;
        };
        if (profession == null) return;
        if (profession == Profession.FARMER && event.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable age && age.getAge() < age.getMaximumAge()) return;
        professionService.addExperience(player, profession, switch (profession) {
            case MOUNTAIN_MINER -> miningXp(material);
            case FARMER -> plugin.getConfig().getLong("profession-xp.gathering.farmer",8L);
            case WOODCUTTER -> plugin.getConfig().getLong("profession-xp.gathering.woodcutter",8L);
            default -> 0L;
        });
    }

    // Vergibt Fischer-XP für tatsächlich erfolgreich gefangene Fische oder Gegenstände.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) professionService.addExperience(event.getPlayer(), Profession.FISHERMAN, plugin.getConfig().getLong("profession-xp.gathering.fisherman",12L));
    }

    // Vergibt Koch-XP nur für echte Kochrezepte; Tierkills sind keine Kochproduktion.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilResult(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL || event.getRawSlot() != 2) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().isEmpty() || !(event.getWhoClicked() instanceof Player player)) return;
        professionService.addExperience(player, Profession.BLACKSMITH, 15L);
    }

    // Vergibt Gelehrten-XP für erfolgreiches Verzaubern als produktive Hauptberufsaktivität.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        professionService.addExperience(event.getEnchanter(), Profession.SCHOLAR, 20L);
    }

    // Entfernt abgelaufene Anti-Exploit-Marker beim Logout ist nicht nötig; die Map ist zeitbegrenzt und wird periodisch bereinigt.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { placedBlocks.entrySet().removeIf(entry -> entry.getValue() <= System.currentTimeMillis()); }

    private long miningXp(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE, ANCIENT_DEBRIS -> plugin.getConfig().getLong("profession-xp.gathering.mountain-miner.rare",35L);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> plugin.getConfig().getLong("profession-xp.gathering.mountain-miner.valuable",20L);
            case IRON_ORE, DEEPSLATE_IRON_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> plugin.getConfig().getLong("profession-xp.gathering.mountain-miner.common",12L);
            default -> plugin.getConfig().getLong("profession-xp.gathering.mountain-miner.default",8L);
        };
    }

    private record BlockKey(String world, int x, int y, int z) { static BlockKey of(Block b) { return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()); } }
}
