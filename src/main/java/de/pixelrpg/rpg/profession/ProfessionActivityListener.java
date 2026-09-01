package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;

/** Connects normal Minecraft activities with the nine optional PixelRPG professions. */
public final class ProfessionActivityListener implements Listener {
    private final ProfessionService professionService;

    public ProfessionActivityListener(ProfessionService professionService) {
        this.professionService = professionService;
    }

    // Vergibt Berufs-XP für Bergbau, passend zur Metall- und Ausrüstungsproduktion des Schmieds.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
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
            case BLACKSMITH -> miningXp(material);
            case FARMER, WOODCUTTER, MASON -> 8L;
            case ALCHEMIST -> 8L;
            case SCHOLAR -> 5L;
            default -> 0L;
        };
        professionService.addExperience(player, profession, experience);
    }

    // Vergibt Fischer-XP für einen tatsächlich abgeschlossenen Fischfang.
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        professionService.addExperience(event.getPlayer(), Profession.FISHERMAN, 18L);
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
        if (profession == null) return;
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

    private Profession professionForQuest(String questId) {
        if (questId == null || questId.isBlank()) return null;
        String id = questId.toLowerCase(java.util.Locale.ROOT);
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
}
