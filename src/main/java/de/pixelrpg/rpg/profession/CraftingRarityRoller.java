package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.item.ItemRarity;
import java.util.concurrent.ThreadLocalRandom;
/** Rolls the quality of a profession-crafted item; Legendary is hard-capped at 0.1%. */
public final class CraftingRarityRoller{
 private static final double COMMON=65,UNCOMMON=23,RARE=9,EPIC=2.9,LEGENDARY=0.1;
 private CraftingRarityRoller(){}
 public static ItemRarity roll(ItemRarity maximum){if(maximum==null||maximum==ItemRarity.COMMON)return ItemRarity.COMMON;double total=0; if(maximum.ordinal()>=1)total+=COMMON;if(maximum.ordinal()>=2)total+=UNCOMMON;if(maximum.ordinal()>=3)total+=RARE;if(maximum.ordinal()>=4)total+=EPIC;if(maximum.ordinal()>=5)total+=LEGENDARY;double r=ThreadLocalRandom.current().nextDouble(total);if((r-=COMMON)<0)return ItemRarity.COMMON;if(maximum.ordinal()>=2&&(r-=UNCOMMON)<0)return ItemRarity.UNCOMMON;if(maximum.ordinal()>=3&&(r-=RARE)<0)return ItemRarity.RARE;if(maximum.ordinal()>=4&&(r-=EPIC)<0)return ItemRarity.EPIC;if(maximum.ordinal()>=5&&(r-=LEGENDARY)<0)return ItemRarity.LEGENDARY;return ItemRarity.COMMON;}
}
