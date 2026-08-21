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

    // Vergibt Mining- oder Woodcutting-XP für erfolgreich abgebaute Ressourcen.
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
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS -> Profession.MINING;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 MANGROVE_LOG, CHERRY_LOG, PALE_OAK_LOG, CRIMSON_STEM, WARPED_STEM -> Profession.WOODCUTTING;
            default -> null;
        };
        if (profession == null) return;
        professionService.addExperience(player, profession, profession == Profession.MINING ? 10L : 8L);
    }

    // Vergibt Fishing-XP, wenn tatsächlich ein Fang abgeschlossen wurde.
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        professionService.addExperience(event.getPlayer(), Profession.FISHING, 12L);
    }

    // Vergibt Skinning-XP für von einem registrierten Spieler erlegte geeignete Tiere.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!isSkinnable(event.getEntityType())) return;
        professionService.addExperience(killer, Profession.SKINNING, 10L);
    }

    private boolean isSkinnable(EntityType type) {
        return switch (type) {
            case COW, MOOSHROOM, SHEEP, PIG, CHICKEN, RABBIT, HORSE, DONKEY, MULE,
                 LLAMA, TRADER_LLAMA, GOAT, CAMEL, HOGLIN -> true;
            default -> false;
        };
    }
}
