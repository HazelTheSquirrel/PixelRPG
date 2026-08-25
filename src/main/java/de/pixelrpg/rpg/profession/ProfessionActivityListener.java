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

/** Connects non-crafting gameplay actions with the profession progression system. */
public final class ProfessionActivityListener implements Listener {
    private final ProfessionService professionService;

    public ProfessionActivityListener(ProfessionService professionService) {
        this.professionService = professionService;
    }

    // Vergibt Berufs-XP für relevante Ressourcen, passend zum fachlichen Beruf des Materials.
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
                 VINE, GLOW_LICHEN, MOSS_BLOCK -> Profession.PROVISIONER;
            case RED_MUSHROOM, BROWN_MUSHROOM, CRIMSON_FUNGUS, WARPED_FUNGUS,
                 FLOWERING_AZALEA, AZALEA, DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
                 RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP, OXEYE_DAISY, CORNFLOWER,
                 LILY_OF_THE_VALLEY, WITHER_ROSE, SUNFLOWER, LILAC, ROSE_BUSH, PEONY -> Profession.ALCHEMIST;
            case BOOKSHELF, CHISELED_BOOKSHELF -> Profession.SCHOLAR;
            default -> null;
        };
        if (profession == null) return;

        long experience = switch (profession) {
            case BLACKSMITH -> miningXp(material);
            case PROVISIONER -> 8L;
            case ALCHEMIST -> 8L;
            case SCHOLAR -> 5L;
        };
        professionService.addExperience(player, profession, experience);
    }

    // Vergibt Versorger-XP, wenn ein tatsächlicher Fischfang abgeschlossen wurde.
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        professionService.addExperience(event.getPlayer(), Profession.PROVISIONER, 18L);
    }

    // Vergibt Versorger-XP für Tiere, deren Drops sinnvoll zur Versorgung gehören.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        switch (event.getEntityType()) {
            case COW, MOOSHROOM, SHEEP, PIG, CHICKEN, RABBIT, GOAT, CAMEL, HOGLIN ->
                    professionService.addExperience(killer, Profession.PROVISIONER, 12L);
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
        professionService.addExperience(event.getPlayer(), professionForQuest(event.getQuestId()), 40L);
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
        if (questId == null) return Profession.SCHOLAR;
        String id = questId.toLowerCase();
        if (id.startsWith("blacksmith.")) return Profession.BLACKSMITH;
        if (id.startsWith("provisioner.")) return Profession.PROVISIONER;
        if (id.startsWith("alchemist.")) return Profession.ALCHEMIST;
        return Profession.SCHOLAR;
    }
}
