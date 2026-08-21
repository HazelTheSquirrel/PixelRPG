package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.Level;
import org.bukkit.configuration.file.FileConfiguration;

public final class ItemEconomyConfig {
    private double commonIdentifyCost = 20.0;
    private double uncommonIdentifyCost = 30.0;
    private double rareIdentifyCost = 45.0;
    private double epicIdentifyCost = 90.0;
    private double legendaryIdentifyCost = 180.0;
    private double uniqueIdentifyCost = 360.0;
    private double soulboundCost = 200.0;
    private int soulboundMinLevel = 30;
    private double unidentifiedDropChance = 0.09;
    private double identifiedDropChance = 0.14;
    private double currencyDropChance = 0.20;
    private long currencyDropMinAmount = 1;
    private long currencyDropMaxAmount = 8;

    public void load(FileConfiguration config) {
        commonIdentifyCost = config.getDouble("items.identification-cost.common", commonIdentifyCost);
        uncommonIdentifyCost = config.getDouble("items.identification-cost.uncommon", uncommonIdentifyCost);
        rareIdentifyCost = config.getDouble("items.identification-cost.rare", rareIdentifyCost);
        epicIdentifyCost = config.getDouble("items.identification-cost.epic", epicIdentifyCost);
        legendaryIdentifyCost = config.getDouble("items.identification-cost.legendary", legendaryIdentifyCost);
        uniqueIdentifyCost = config.getDouble("items.identification-cost.unique", uniqueIdentifyCost);
        soulboundCost = config.getDouble("items.soulbound.cost", soulboundCost);
        soulboundMinLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, config.getInt("items.soulbound.min-level", soulboundMinLevel)));
        unidentifiedDropChance = config.getDouble("items.loot.unidentified-drop-chance", unidentifiedDropChance);
        identifiedDropChance = config.getDouble("items.loot.identified-drop-chance", identifiedDropChance);
        currencyDropChance = config.getDouble("items.loot.currency-drop-chance", currencyDropChance);
        currencyDropMinAmount = config.getLong("items.loot.currency-drop-min-amount", currencyDropMinAmount);
        currencyDropMaxAmount = config.getLong("items.loot.currency-drop-max-amount", currencyDropMaxAmount);
    }

    public double identificationCost(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonIdentifyCost;
            case UNCOMMON -> uncommonIdentifyCost;
            case RARE -> rareIdentifyCost;
            case EPIC -> epicIdentifyCost;
            case LEGENDARY -> legendaryIdentifyCost;
            case UNIQUE -> uniqueIdentifyCost;
        };
    }

    public double getSoulboundCost() { return soulboundCost; }
    public int getSoulboundMinLevel() { return soulboundMinLevel; }
    public double getUnidentifiedDropChance() { return unidentifiedDropChance; }
    public double getIdentifiedDropChance() { return identifiedDropChance; }
    public double getCurrencyDropChance() { return currencyDropChance; }
    public long getCurrencyDropMinAmount() { return currencyDropMinAmount; }
    public long getCurrencyDropMaxAmount() { return currencyDropMaxAmount; }
}
