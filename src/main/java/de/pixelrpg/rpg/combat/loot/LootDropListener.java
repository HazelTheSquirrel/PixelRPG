package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the remaining hostile-mob economy drop.
 * PixelRPG custom progression items are intentionally excluded from mob loot and
 * are obtained through quests and professions instead.
 */
public final class LootDropListener implements Listener {
    private final GuildAPI guildAPI;
    private final ItemEconomyConfig economyConfig;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
    }

    // Vergibt ausschließlich das bestehende Gildengold aus normalen Hostile-Mob-Kills; Custom-Items werden nicht mehr gedroppt.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        if (entity.getPersistentDataContainer().has(RPGKeys.Boss.bossId(), PersistentDataType.STRING)) return;

        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) return;
        if (ThreadLocalRandom.current().nextDouble() >= economyConfig.getCurrencyDropChance()) return;

        long min = economyConfig.getCurrencyDropMinAmount();
        long max = Math.max(min, economyConfig.getCurrencyDropMaxAmount());
        long amount = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        event.getDrops().addAll(GuildCurrencyItemFactory.createStacks(amount));
    }
}