package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.PixelRPGPlugin;
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
    private final List<ItemDefinition> lootPool;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
        this.lootPool = itemService.definitions().stream()
                .filter(definition -> !definition.adminOnly())
                .filter(definition -> !definition.unique())
                .toList();
    }

    // Zuständig für direkte PixelRPG-Ausrüstungsdrops und Gildengold beim Töten eines Monsters.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int playerLevel = Math.max(1, guildAPI.getLevel(killer.getUniqueId()));

        if (!lootPool.isEmpty() && random.nextDouble() < economyConfig.getItemDropChance()) {
            List<ItemDefinition> eligibleItems = lootPool.stream()
                    .filter(definition -> definition.itemLevel() <= playerLevel)
                    .filter(definition -> definition.requiredLevel() <= playerLevel)
                    .toList();

            if (!eligibleItems.isEmpty()) {
                ItemDefinition definition = eligibleItems.get(random.nextInt(eligibleItems.size()));
                itemService.createItem(definition.id()).ifPresent(event.getDrops()::add);
            }
        }

        if (random.nextDouble() < economyConfig.getCurrencyDropChance()) {
            long min = economyConfig.getCurrencyDropMinAmount();
            long max = Math.max(min, economyConfig.getCurrencyDropMaxAmount());
            long amount = min == max ? min : random.nextLong(min, max + 1);
            event.getDrops().addAll(GuildCurrencyItemFactory.createStacks(amount));
        }
    }
}
