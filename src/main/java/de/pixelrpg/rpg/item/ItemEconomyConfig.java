package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.Rank;
import org.bukkit.configuration.file.FileConfiguration;

public final class ItemEconomyConfig {

    private double commonIdentifyCost = 20.0;
    private double rareIdentifyCost = 45.0;
    private double epicIdentifyCost = 90.0;
    private double legendaryIdentifyCost = 180.0;
    private double soulboundCost = 200.0;
    private Rank soulboundMinRank = Rank.C;
    private double unidentifiedDropChance = 0.09;
    private double identifiedDropChance = 0.14;
    private double runeDropChance = 0.05;
    private double gemDropChance = 0.05;
    private double gemActiveChance = 0.35;

    public void load(FileConfiguration config) {
        commonIdentifyCost = config.getDouble("items.identification-cost.common", commonIdentifyCost);
        rareIdentifyCost = config.getDouble("items.identification-cost.rare", rareIdentifyCost);
        epicIdentifyCost = config.getDouble("items.identification-cost.epic", epicIdentifyCost);
        legendaryIdentifyCost = config.getDouble("items.identification-cost.legendary", legendaryIdentifyCost);
        soulboundCost = config.getDouble("items.soulbound.cost", soulboundCost);
        soulboundMinRank = parseRank(config.getString("items.soulbound.min-rank", "C"));
        unidentifiedDropChance = config.getDouble("items.loot.unidentified-drop-chance", unidentifiedDropChance);
        identifiedDropChance = config.getDouble("items.loot.identified-drop-chance", identifiedDropChance);
        runeDropChance = config.getDouble("items.loot.rune-drop-chance", runeDropChance);
        gemDropChance = config.getDouble("items.loot.gem-drop-chance", gemDropChance);
        gemActiveChance = config.getDouble("items.loot.gem-active-chance", gemActiveChance);
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Rank.C;
        }
    }

    public double identificationCost(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonIdentifyCost;
            case RARE -> rareIdentifyCost;
            case EPIC -> epicIdentifyCost;
            case LEGENDARY -> legendaryIdentifyCost;
        };
    }

    public double getSoulboundCost() {
        return soulboundCost;
    }

    public Rank getSoulboundMinRank() {
        return soulboundMinRank;
    }

    public double getUnidentifiedDropChance() {
        return unidentifiedDropChance;
    }

    public double getIdentifiedDropChance() {
        return identifiedDropChance;
    }

    public double getRuneDropChance() {
        return runeDropChance;
    }

    public double getGemDropChance() {
        return gemDropChance;
    }

    public double getGemActiveChance() {
        return gemActiveChance;
    }
}