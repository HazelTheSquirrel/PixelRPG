package de.pixelrpg.rpg.profession;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public final class ProfessionGatheringListener implements Listener {
    private final ProfessionService professionService;

    public ProfessionGatheringListener(ProfessionService professionService) {
        this.professionService = professionService;
    }

    // Vergibt Versorger- oder Schmied-XP für das Abbauen relevanter Vanilla-Ressourcen.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (event.isCancelled()) return;
        Material material = event.getBlock().getType();
        Profession profession = switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE, GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS,
                 RAW_IRON_BLOCK, RAW_COPPER_BLOCK, RAW_GOLD_BLOCK, IRON_BLOCK, COPPER_BLOCK, GOLD_BLOCK -> Profession.BLACKSMITH;
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, SWEET_BERRY_BUSH,
                 GLOW_BERRIES, KELP, SEAGRASS, TALL_SEAGRASS, SUGAR_CANE, CACTUS, BAMBOO,
                 VINE, GLOW_LICHEN, MOSS_BLOCK -> Profession.PROVISIONER;
            default -> null;
        };
        if (profession == null) return;
        long amount = profession == Profession.BLACKSMITH ? miningXp(material) : 8L;
        professionService.addExperience(player, profession, amount);
    }

    // Vergibt Versorger-XP, wenn ein tatsächlicher Fischfang abgeschlossen wurde.
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        professionService.addExperience(event.getPlayer(), Profession.PROVISIONER, 18L);
    }

    // Vergibt Versorger-XP für verwertbare Tierprodukte und Nahrungstiere.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !isProvisionerTarget(event.getEntityType())) return;
        professionService.addExperience(killer, Profession.PROVISIONER, 12L);
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

    private boolean isProvisionerTarget(EntityType type) {
        return switch (type) {
            case COW, MOOSHROOM, SHEEP, PIG, CHICKEN, RABBIT, GOAT, CAMEL, HOGLIN -> true;
            default -> false;
        };
    }
}
