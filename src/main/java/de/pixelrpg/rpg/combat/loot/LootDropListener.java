package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootDropListener implements Listener {
    private final GuildAPI guildAPI;
    private final ItemEconomyConfig economyConfig;
    private final ItemService itemService;
    private final List<ItemDefinition> lootDefinitions;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
        this.itemService = de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getItemService();
        this.lootDefinitions = itemService.definitions().stream()
                .filter(definition -> !definition.adminOnly())
                .filter(definition -> !definition.unique())
                .filter(definition -> !definition.id().startsWith("pixelrpg:boss/"))
                .toList();
        if (lootDefinitions.isEmpty()) throw new IllegalStateException("Item registry contains no lootable item definitions");
    }

    // Zuständig für registriertes PixelRPG-Loot und Gildengold bei Monster-Toden.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;
        if (!guildAPI.isRegistered(killer.getUniqueId())) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int playerLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, guildAPI.getLevel(killer.getUniqueId())));

        if (random.nextDouble() < economyConfig.getItemDropChance()) {
            int lootLevel = rollLootLevel(playerLevel, random);
            createLoot(lootLevel, playerLevel, random).ifPresent(event.getDrops()::add);
        }

        if (random.nextDouble() < economyConfig.getCurrencyDropChance()) {
            long min = economyConfig.getCurrencyDropMinAmount();
            long max = Math.max(min, economyConfig.getCurrencyDropMaxAmount());
            long amount = min == max ? min : random.nextLong(min, max + 1);
            event.getDrops().addAll(GuildCurrencyItemFactory.createStacks(amount));
        }
    }

    private java.util.Optional<org.bukkit.inventory.ItemStack> createLoot(int lootLevel, int playerLevel, ThreadLocalRandom random) {
        List<ItemDefinition> eligible = lootDefinitions.stream()
                .filter(definition -> definition.itemLevel() <= lootLevel)
                .filter(definition -> definition.requiredLevel() <= playerLevel)
                .toList();
        if (eligible.isEmpty()) return java.util.Optional.empty();
        ItemDefinition definition = eligible.get(random.nextInt(eligible.size()));
        return itemService.createItem(definition.id());
    }

    private int rollLootLevel(int playerLevel, ThreadLocalRandom random) {
        long totalWeight = 0L;
        for (int level = Level.MIN_LEVEL; level <= playerLevel; level++) totalWeight += levelWeight(level);

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
