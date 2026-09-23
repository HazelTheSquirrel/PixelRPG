package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import java.util.Map;

public record CraftRecipe(Profession profession,CraftingCategory category,String id,String label,Material resultMaterial,int resultAmount,ItemRarity rarity,Map<Material,Integer> costs,Map<String,Integer> itemCosts,int requiredProfessionLevel,long unlockPrice,String requiredQuestId,boolean unlockedByDefault,boolean vanillaRecipe,String resultItemId,String potionType,String enchantment,int enchantmentLevel){
 public CraftRecipe{
  if(profession==null||category==null)throw new IllegalArgumentException("Recipe profession/category must not be null");
  if(id==null||id.isBlank()||label==null||label.isBlank())throw new IllegalArgumentException("Recipe id/label must not be blank");
  if(resultMaterial==null||resultMaterial.isAir()||resultAmount<=0)throw new IllegalArgumentException("Recipe result must be valid");
  if(rarity==null)throw new IllegalArgumentException("Recipe rarity must not be null");
  if(costs==null||costs.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null||e.getValue()<=0))throw new IllegalArgumentException("Invalid material costs");
  if(itemCosts==null||itemCosts.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue()<=0))throw new IllegalArgumentException("Invalid item costs");
  if(costs.isEmpty()&&itemCosts.isEmpty())throw new IllegalArgumentException("Recipe must have at least one cost");
  if(requiredProfessionLevel<Profession.MIN_LEVEL||requiredProfessionLevel>Profession.MAX_LEVEL)throw new IllegalArgumentException("Invalid profession level");
  if(unlockPrice<0L)throw new IllegalArgumentException("Unlock price must not be negative");
  requiredQuestId=requiredQuestId==null?"":requiredQuestId; resultItemId=resultItemId==null?"":resultItemId;
  potionType=potionType==null?"":potionType; enchantment=enchantment==null?"":enchantment;
  if(enchantmentLevel<0)throw new IllegalArgumentException("Enchantment level must not be negative");
  costs=Map.copyOf(costs); itemCosts=Map.copyOf(itemCosts);
 }
 public String displayName(){return label;}
}
