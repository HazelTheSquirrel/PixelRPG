package de.pixelrpg.rpg.item;
import org.bukkit.Material;
public record ItemDefinition(String id,String name,Material material,ItemRarity rarity,ItemCategory category,int itemLevel,int requiredLevel,double gearscoreModifier){}
