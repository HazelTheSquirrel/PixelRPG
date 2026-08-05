package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.combat.gem.ActiveSkillGemDefinition;
import de.pixelrpg.rpg.combat.gem.GemItemFactory;
import de.pixelrpg.rpg.combat.gem.GemRepository;
import de.pixelrpg.rpg.combat.gem.PassiveGemDefinition;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.item.RuneItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootDropListener implements Listener {

    private static final List<Material> DROP_POOL = List.of(
            // Nahkampfwaffen
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.COPPER_AXE, Material.COPPER_SWORD,
            Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.MACE,
            Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            // Fernkampfwaffen
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            // Rüstung: Leder
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            // Rüstung: Kettenhemd
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            // Rüstung: Kupfer
            Material.COPPER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS,
            // Rüstung: Eisen
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            // Rüstung: Gold
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            // Rüstung: Diamant
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            // Rüstung: Netherit
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET,
            // Schild & Werkzeuge
            Material.SHIELD, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_HOE, Material.DIAMOND_HOE,
            Material.SHEARS, Material.FISHING_ROD
    );

    private final GuildAPI guildAPI;
    private final ItemEconomyConfig economyConfig;
    private final GemRepository gemRepository;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig, GemRepository gemRepository) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
        this.gemRepository = gemRepository;
    }

    // Zuständig für sämtliche Loot-Drops beim Töten registrierter Gilden-Mitglieder:
    // unidentifizierte/identifizierte Ausrüstung, Runen und Skill-Gems.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Integer mobRankOrdinal = entity.getPersistentDataContainer()
                .get(RPGKeys.Combat.mobRank(), PersistentDataType.INTEGER);

        de.pixelrpg.rpg.core.Rank itemRank = mobRankOrdinal != null
                ? de.pixelrpg.rpg.core.Rank.fromOrdinalClamped(mobRankOrdinal)
                : de.pixelrpg.rpg.core.Rank.F;

        if (random.nextDouble() < economyConfig.getUnidentifiedDropChance()) {
            Material material = DROP_POOL.get(random.nextInt(DROP_POOL.size()));
            ItemRarity rarity = ItemRarity.rollRandom();

            RPGItemBuilder.createUnidentified(material, rarity, itemRank)
                    .ifPresent(item -> event.getDrops().add(item));
        }

        if (random.nextDouble() < economyConfig.getIdentifiedDropChance()) {
            Material material = DROP_POOL.get(random.nextInt(DROP_POOL.size()));
            ItemRarity rarity = ItemRarity.rollRandomUpTo(ItemRarity.RARE);

            RPGItemBuilder.createUnidentified(material, rarity, itemRank)
                    .map(RPGItemBuilder::identify)
                    .ifPresent(item -> event.getDrops().add(item));
        }

        if (random.nextDouble() < economyConfig.getRuneDropChance()) {
            event.getDrops().add(RuneItemFactory.createRandom());
        }

        if (random.nextDouble() < economyConfig.getGemDropChance()) {
            rollGemDrop(random).ifPresent(item -> event.getDrops().add(item));
        }
    }

    private java.util.Optional<ItemStack> rollGemDrop(ThreadLocalRandom random) {
        boolean dropActive = random.nextDouble() < economyConfig.getGemActiveChance();

        if (dropActive) {
            List<ActiveSkillGemDefinition> activeGems = gemRepository.getAllActive();
            if (!activeGems.isEmpty()) {
                ActiveSkillGemDefinition definition = activeGems.get(random.nextInt(activeGems.size()));
                return java.util.Optional.of(GemItemFactory.createActive(definition));
            }
        }

        List<PassiveGemDefinition> passiveGems = gemRepository.getAllPassive();
        if (passiveGems.isEmpty()) {
            return java.util.Optional.empty();
        }
        PassiveGemDefinition definition = passiveGems.get(random.nextInt(passiveGems.size()));
        return java.util.Optional.of(GemItemFactory.createPassive(definition));
    }
}