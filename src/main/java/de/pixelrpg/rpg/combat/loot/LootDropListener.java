package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LootDropListener implements Listener {
    private static final List<Material> DROP_POOL = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.COPPER_AXE, Material.COPPER_SWORD,
            Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.MACE,
            Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.COPPER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET, Material.SHIELD, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_HOE, Material.DIAMOND_HOE,
            Material.SHEARS, Material.FISHING_ROD
    );

    private final GuildAPI guildAPI;
    private final ItemEconomyConfig economyConfig;
    private final ItemService itemService;
    private final Map<Material, List<ItemDefinition>> definedItemsByMaterial;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
        this.definedItemsByMaterial = itemService.definitions().stream()
                .filter(definition -> !definition.adminOnly())
                .filter(definition -> !definition.unique())
                .collect(Collectors.groupingBy(ItemDefinition::material, Collectors.toList()));
    }

    // Zuständig für PixelRPG-Loot und Gildengold; Vanilla-Spieler bleiben vollständig beim Vanilla-Loot.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;

        // Oberste Trennlinie: Nur registrierte PixelRPG-Spieler dürfen das RPG-Lootsystem erreichen.
        if (!guildAPI.isRegistered(killer.getUniqueId())) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int playerLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, guildAPI.getLevel(killer.getUniqueId())));

        if (random.nextDouble() < economyConfig.getItemDropChance()) {
            int lootLevel = rollLootLevel(playerLevel, random);
            Material material = DROP_POOL.get(random.nextInt(DROP_POOL.size()));
            createLoot(material, lootLevel, playerLevel, random).ifPresent(event.getDrops()::add);
        }

        if (random.nextDouble() < economyConfig.getCurrencyDropChance()) {
            long min = economyConfig.getCurrencyDropMinAmount();
            long max = Math.max(min, economyConfig.getCurrencyDropMaxAmount());
            long amount = min == max ? min : random.nextLong(min, max + 1);
            event.getDrops().addAll(GuildCurrencyItemFactory.createStacks(amount));
        }
    }

    private java.util.Optional<org.bukkit.inventory.ItemStack> createLoot(
            Material material,
            int lootLevel,
            int playerLevel,
            ThreadLocalRandom random
    ) {
        ItemDefinition definition = definedItemsByMaterial.getOrDefault(material, List.of()).stream()
                .filter(candidate -> candidate.itemLevel() <= lootLevel)
                .filter(candidate -> candidate.requiredLevel() <= playerLevel)
                .max(java.util.Comparator.comparingInt(ItemDefinition::itemLevel))
                .orElse(null);

        if (definition != null) {
            return itemService.createItem(definition.id());
        }

        int itemLevel = Math.max(Level.MIN_LEVEL, Math.min(lootLevel, playerLevel));
        ItemRarity rarity = ItemRarity.rollRandom();
        return RPGItemBuilder.createItem(material, rarity, itemLevel);
    }

    private int rollLootLevel(int playerLevel, ThreadLocalRandom random) {
        long totalWeight = 0L;
        for (int level = Level.MIN_LEVEL; level <= playerLevel; level++) {
            totalWeight += levelWeight(level);
        }

        long roll = random.nextLong(totalWeight);
        for (int level = Level.MIN_LEVEL; level <= playerLevel; level++) {
            roll -= levelWeight(level);
            if (roll < 0L) return level;
        }

        return playerLevel;
    }

    private long levelWeight(int itemLevel) {
        long level = Math.max(1L, itemLevel);
        return level * level;
    }
}
