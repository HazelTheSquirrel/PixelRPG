package de.pixelrpg.rpg.item;

import org.bukkit.configuration.file.FileConfiguration;

public final class ItemEconomyConfig {
    private double soulboundCost = 200.0;
    private int soulboundMinLevel = 30;
    private double itemDropChance = 0.23;
    private double currencyDropChance = 0.20;
    private long currencyDropMinAmount = 1;
    private long currencyDropMaxAmount = 8;

    public void load(FileConfiguration config) {
        soulboundCost = config.getDouble("items.soulbound.cost", soulboundCost);
        soulboundMinLevel = Math.max(1, Math.min(99,
                config.getInt("items.soulbound.min-level", soulboundMinLevel)));
        itemDropChance = Math.max(0.0, Math.min(1.0,
                config.getDouble("items.loot.item-drop-chance", itemDropChance)));
        currencyDropChance = Math.max(0.0, Math.min(1.0,
                config.getDouble("items.loot.currency-drop-chance", currencyDropChance)));
        currencyDropMinAmount = Math.max(0, config.getLong("items.loot.currency-drop-min-amount", currencyDropMinAmount));
        currencyDropMaxAmount = Math.max(currencyDropMinAmount,
                config.getLong("items.loot.currency-drop-max-amount", currencyDropMaxAmount));
    }

    public double getSoulboundCost() { return soulboundCost; }
    public int getSoulboundMinLevel() { return soulboundMinLevel; }
    public double getItemDropChance() { return itemDropChance; }
    public double getCurrencyDropChance() { return currencyDropChance; }
    public long getCurrencyDropMinAmount() { return currencyDropMinAmount; }
    public long getCurrencyDropMaxAmount() { return currencyDropMaxAmount; }
}
