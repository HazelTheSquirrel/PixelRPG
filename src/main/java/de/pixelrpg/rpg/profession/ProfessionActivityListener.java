package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Connects normal Minecraft activities with the ten optional PixelRPG professions. */
public final class ProfessionActivityListener implements Listener {
    private static final int MAX_TREE_LOGS = 64;
    private static final Set<Material> TREE_GROUND = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.SAND,
            Material.RED_SAND,
            Material.MUD,
            Material.MOSS_BLOCK,
            Material.ROOTED_DIRT
    );
    private static final Set<Material> MINEABLE_ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    );

    private static final List<Material> FISHING_TREASURE = List.of(
            Material.BOW,
            Material.FISHING_ROD,
            Material.NAME_TAG,
            Material.NAUTILUS_SHELL,
            Material.SADDLE
    );

    private final ProfessionService professionService;
    private final Set<BlockPosition> automatedTreeFelling = new HashSet<>();
    private final Random random = new Random();

    public ProfessionActivityListener(ProfessionService professionService) {
        this.professionService = professionService;
    }

    // Vergibt Berufs-XP für Bergbau, Feldarbeit, Holzfällen, Steinabbau und pflanzliche Alchemie.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        BlockPosition position = BlockPosition.of(event.getBlock());
        boolean automated = automatedTreeFelling.remove(position);

        Profession profession = switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE, GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS,
                 RAW_IRON_BLOCK, RAW_COPPER_BLOCK, RAW_GOLD_BLOCK,
                 IRON_BLOCK, COPPER_BLOCK, GOLD_BLOCK -> Profession.BLACKSMITH;
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, SWEET_BERRY_BUSH,
                 GLOW_BERRIES, KELP, SEAGRASS, TALL_SEAGRASS, SUGAR_CANE, CACTUS, BAMBOO,
                 VINE, GLOW_LICHEN, MOSS_BLOCK, PUMPKIN, MELON -> Profession.FARMER;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 MANGROVE_LOG, CHERRY_LOG, PALE_OAK_LOG, CRIMSON_STEM, WARPED_STEM,
                 OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
                 MANGROVE_WOOD, CHERRY_WOOD, PALE_OAK_WOOD, CRIMSON_HYPHAE, WARPED_HYPHAE,
                 STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG, STRIPPED_JUNGLE_LOG,
                 STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG,
                 STRIPPED_PALE_OAK_LOG, STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM -> Profession.WOODCUTTER;
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE, GRANITE, DIORITE, ANDESITE,
                 TUFF, CALCITE, SANDSTONE, RED_SANDSTONE, BLACKSTONE, BASALT, NETHERRACK,
                 BRICKS, STONE_BRICKS, DEEPSLATE_BRICKS, DEEPSLATE_TILES, MUD_BRICKS,
                 PRISMARINE, PRISMARINE_BRICKS, DARK_PRISMARINE, QUARTZ_BLOCK -> Profession.MASON;
            case BOOKSHELF, CHISELED_BOOKSHELF -> Profession.SCHOLAR;
            case RED_MUSHROOM, BROWN_MUSHROOM, CRIMSON_FUNGUS, WARPED_FUNGUS,
                 FLOWERING_AZALEA, AZALEA, DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
                 RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP, OXEYE_DAISY, CORNFLOWER,
                 LILY_OF_THE_VALLEY, WITHER_ROSE, SUNFLOWER, LILAC, ROSE_BUSH, PEONY -> Profession.ALCHEMIST;
            default -> null;
        };

        if (profession == null) return;

        long experience = switch (profession) {
            case BLACKSMITH, MOUNTAIN_MINER -> miningXp(material);
            case FARMER, WOODCUTTER, MASON, ALCHEMIST -> 8L;
            case SCHOLAR -> 5L;
            default -> 0L;
        };
        if (experience > 0L) professionService.addExperience(player, profession, experience);
        if (MINEABLE_ORES.contains(material)) {
            professionService.addExperience(player, Profession.MOUNTAIN_MINER, Math.max(4L, miningXp(material)));
        }

        if (!automated
                && profession == Profession.WOODCUTTER
                && professionService.getLevel(player.getUniqueId(), Profession.WOODCUTTER) >= 60
                && Tag.ITEMS_AXES.isTagged(player.getInventory().getItemInMainHand().getType())
                && Tag.OVERWORLD_NATURAL_LOGS.isTagged(material)) {
            fellSafeTree(player, event.getBlock());
        }
    }

    // Verstärkt Holzertrag passiv abhängig vom Holzfäller-Level, ohne Vanilla-Drops zu ersetzen.
    @EventHandler
    public void onWoodDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!Tag.OVERWORLD_NATURAL_LOGS.isTagged(event.getBlockState().getType())) return;
        int level = professionService.getLevel(player.getUniqueId(), Profession.WOODCUTTER);
        if (level < 20) return;

        double chance = level >= 100 ? 0.40D : level >= 80 ? 0.30D : level >= 60 ? 0.25D : 0.20D;
        if (random.nextDouble() >= chance) return;

        Material logType = event.getBlockState().getType();
        Item matchingDrop = event.getItems().stream()
                .filter(item -> item.getItemStack().getType() == logType)
                .findFirst()
                .orElse(null);
        if (matchingDrop == null) return;

        ItemStack stack = matchingDrop.getItemStack();
        if (stack.getAmount() < stack.getMaxStackSize()) stack.setAmount(stack.getAmount() + 1);
    }

    // Verstärkt den passiven Bergbauer-Ertrag auf Vanilla-Erze, ohne Vanilla-Drops zu ersetzen.
    @EventHandler
    public void onMountainMinerDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlockState().getType();
        if (!MINEABLE_ORES.contains(blockType)) return;

        int level = professionService.getLevel(player.getUniqueId(), Profession.MOUNTAIN_MINER);
        if (level < 20) return;

        double chance = level >= 100 ? 0.40D : level >= 80 ? 0.30D : level >= 60 ? 0.25D : 0.20D;
        if (random.nextDouble() >= chance) return;

        Material expectedDrop = oreDropMaterial(blockType);
        if (expectedDrop == null) return;

        Item matchingDrop = event.getItems().stream()
                .filter(item -> item.getItemStack().getType() == expectedDrop)
                .findFirst()
                .orElse(null);
        if (matchingDrop == null) return;

        ItemStack stack = matchingDrop.getItemStack();
        if (stack.getAmount() < stack.getMaxStackSize()) stack.setAmount(stack.getAmount() + 1);
    }

    private static Material oreDropMaterial(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case NETHER_GOLD_ORE -> Material.GOLD_NUGGET;
            case NETHER_QUARTZ_ORE -> Material.QUARTZ;
            case ANCIENT_DEBRIS -> Material.ANCIENT_DEBRIS;
            default -> null;
        };
    }

    // Vergibt Fischer-XP und verbessert Fangmenge sowie Vanilla-Schatzchance rein passiv.
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        int level = professionService.getLevel(player.getUniqueId(), Profession.FISHERMAN);
        professionService.addExperience(player, Profession.FISHERMAN, 18L);

        if (!(event.getCaught() instanceof Item caught)) return;

        ItemStack stack = caught.getItemStack();
        if (level >= 20 && random.nextDouble() < fishingDoubleChance(level) && stack.getAmount() < stack.getMaxStackSize()) {
            stack.setAmount(stack.getAmount() + 1);
        }

        if (level >= 40 && random.nextDouble() < fishingTreasureChance(level)) {
            caught.setItemStack(new ItemStack(FISHING_TREASURE.get(random.nextInt(FISHING_TREASURE.size()))));
        }
    }

    // Vergibt Koch-XP für das Erlegen von Tieren, deren Drops als Nahrung genutzt werden können.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        switch (event.getEntityType()) {
            case COW, MOOSHROOM, PIG, CHICKEN, RABBIT, GOAT, CAMEL, HOGLIN ->
                    professionService.addExperience(killer, Profession.COOK, 12L);
            case SHEEP -> {
                professionService.addExperience(killer, Profession.COOK, 8L);
                professionService.addExperience(killer, Profession.TAILOR, 8L);
            }
            default -> { }
        }
    }

    // Vergibt Gelehrten-XP für erfolgreiches Verzaubern von Gegenständen.
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        professionService.addExperience(event.getEnchanter(), Profession.SCHOLAR, 20L);
    }

    // Vergibt Schmied-XP, wenn ein gültiges Ergebnis aus einem Amboss genommen wird.
    @EventHandler
    public void onAnvilResult(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        if (event.getRawSlot() != 2 || event.getCurrentItem() == null || event.getCurrentItem().isEmpty()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        professionService.addExperience(player, Profession.BLACKSMITH, 15L);
    }

    // Vergibt berufsbezogene XP für abgeschlossene Quests anhand des stabilen Quest-ID-Präfixes.
    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        Profession profession = professionForQuest(event.getQuestId());
        if (profession == null || profession == Profession.WOODCUTTER || profession == Profession.FISHERMAN) return;
        professionService.addExperience(event.getPlayer(), profession, 40L);
    }

    private long miningXp(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE, ANCIENT_DEBRIS -> 35L;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 20L;
            case IRON_ORE, DEEPSLATE_IRON_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> 12L;
            default -> 8L;
        };
    }

    private void fellSafeTree(Player player, org.bukkit.block.Block start) {
        List<org.bukkit.block.Block> logs = collectTreeLogs(start);
        if (logs.size() < 3 || !looksLikeNaturalTree(start, logs)) return;

        for (org.bukkit.block.Block log : logs) {
            BlockPosition position = BlockPosition.of(log);
            if (!position.equals(BlockPosition.of(start))) automatedTreeFelling.add(position);
        }

        for (org.bukkit.block.Block log : logs) {
            if (log.equals(start) || log.getType().isAir()) continue;
            BlockPosition position = BlockPosition.of(log);
            if (!automatedTreeFelling.contains(position)) continue;
            if (player.breakBlock(log)) automatedTreeFelling.remove(position);
        }
    }

    private List<org.bukkit.block.Block> collectTreeLogs(org.bukkit.block.Block start) {
        ArrayDeque<org.bukkit.block.Block> queue = new ArrayDeque<>();
        Set<BlockPosition> visited = new HashSet<>();
        List<org.bukkit.block.Block> logs = new java.util.ArrayList<>();
        queue.add(start);
        visited.add(BlockPosition.of(start));

        while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
            org.bukkit.block.Block current = queue.removeFirst();
            if (!Tag.OVERWORLD_NATURAL_LOGS.isTagged(current.getType())) continue;
            logs.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        org.bukkit.block.Block next = current.getRelative(dx, dy, dz);
                        BlockPosition position = BlockPosition.of(next);
                        if (visited.add(position) && Tag.OVERWORLD_NATURAL_LOGS.isTagged(next.getType())) {
                            queue.addLast(next);
                        }
                    }
                }
            }
        }
        return logs;
    }

    private boolean looksLikeNaturalTree(org.bukkit.block.Block start, List<org.bukkit.block.Block> logs) {
        org.bukkit.block.Block base = logs.stream()
                .min(java.util.Comparator.comparingInt(org.bukkit.block.Block::getY))
                .orElse(start);
        if (!TREE_GROUND.contains(base.getRelative(org.bukkit.block.BlockFace.DOWN).getType())) return false;

        int leaves = 0;
        Set<BlockPosition> counted = new HashSet<>();
        for (org.bukkit.block.Block log : logs) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 4) continue;
                        org.bukkit.block.Block nearby = log.getRelative(dx, dy, dz);
                        BlockPosition position = BlockPosition.of(nearby);
                        if (counted.add(position) && Tag.LEAVES.isTagged(nearby.getType())) leaves++;
                    }
                }
            }
        }
        return leaves >= 3;
    }

    private double fishingDoubleChance(int level) {
        if (level >= 100) return 0.30D;
        if (level >= 80) return 0.20D;
        if (level >= 60) return 0.15D;
        if (level >= 40) return 0.10D;
        return 0.05D;
    }

    private double fishingTreasureChance(int level) {
        if (level >= 100) return 0.25D;
        if (level >= 80) return 0.18D;
        if (level >= 60) return 0.12D;
        return 0.06D;
    }

    private Profession professionForQuest(String questId) {
        if (questId == null || questId.isBlank()) return null;
        String id = questId.toLowerCase(Locale.ROOT);
        if (id.startsWith("blacksmith.") || id.startsWith("blacksmith_")) return Profession.BLACKSMITH;
        if (id.startsWith("provisioner.") || id.startsWith("provisioner_")) return Profession.COOK;
        if (id.startsWith("cook.") || id.startsWith("cook_")) return Profession.COOK;
        if (id.startsWith("farmer.") || id.startsWith("farmer_")) return Profession.FARMER;
        if (id.startsWith("tailor.") || id.startsWith("tailor_")) return Profession.TAILOR;
        if (id.startsWith("alchemist.") || id.startsWith("alchemist_")) return Profession.ALCHEMIST;
        if (id.startsWith("mason.") || id.startsWith("mason_")) return Profession.MASON;
        if (id.startsWith("fisherman.") || id.startsWith("fisherman_")) return Profession.FISHERMAN;
        if (id.startsWith("woodcutter.") || id.startsWith("woodcutter_")) return Profession.WOODCUTTER;
        if (id.startsWith("scholar.") || id.startsWith("scholar_")) return Profession.SCHOLAR;
        return null;
    }

    private record BlockPosition(UUID worldId, int x, int y, int z) {
        static BlockPosition of(org.bukkit.block.Block block) {
            return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
