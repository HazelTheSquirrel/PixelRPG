package de.pixelrpg.rpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.item.ItemRarity;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import java.util.*;
/** Immutable-after-load registry for vanilla profession crafting recipes. */
public final class CraftingRecipeRegistry {
 private static final String PATH="recipes/crafting-recipes.json";
 private volatile Map<String,CraftRecipe> recipes=Map.of();
 public void load(Plugin plugin){
  JsonObject root=new JsonDataManager(plugin).load(PATH); JsonArray definitions=root.getAsJsonArray("recipes");
  if(definitions==null)throw new IllegalStateException("crafting-recipes.json requires a 'recipes' array");
  Map<String,CraftRecipe> loaded=new LinkedHashMap<>();
  for(var element:definitions){
   if(!element.isJsonObject())throw new IllegalStateException("Each crafting recipe definition must be a JSON object");
   JsonObject json=element.getAsJsonObject(); String id=canonical(required(json,"id"));
   if(loaded.containsKey(id))throw new IllegalStateException("Duplicate crafting recipe: "+id);
   Profession profession=enumValue(Profession.class,json,"profession",id);
   CraftingCategory category=CraftingCategory.from(required(json,"category"),id);
   if(profession==Profession.WOODCUTTER||profession==Profession.FISHERMAN)throw new IllegalStateException("Passive profession cannot define crafting recipes: "+id);
   Material result=resolveMaterial(required(json,"result"));
   if(result==null||result.isAir())throw new IllegalStateException("Unknown crafting result for "+id);
   ItemRarity rarity=enumValue(ItemRarity.class,json,"rarity",id);
   if(rarity==ItemRarity.UNIQUE)throw new IllegalStateException("UNIQUE is not valid for craftable recipe "+id);
   Map<Material,Integer> costs=parseCosts(json,id);
   if(json.has("itemCosts")&&!json.getAsJsonObject("itemCosts").isEmpty())throw new IllegalStateException("Custom itemCosts are not allowed for vanilla profession recipes: "+id);
   int level=integerField(json,"requiredProfessionLevel",id,1), amount=integerField(json,"resultAmount",id,1); long price=longField(json,"unlockPrice",id,0);
   String quest=json.has("requiredQuestId")?json.get("requiredQuestId").getAsString():"";
   boolean defaultUnlocked=json.has("unlockedByDefault")&&json.get("unlockedByDefault").getAsBoolean();
   String label=json.has("label")?json.get("label").getAsString():pretty(result);
   String resultItemId=json.has("resultItemId")?json.get("resultItemId").getAsString():"";
   if(!resultItemId.isBlank())throw new IllegalStateException("Custom resultItemId is not allowed for vanilla profession recipes: "+id);
   String potion=json.has("potionType")?json.get("potionType").getAsString():"";
   String enchant=json.has("enchantment")?json.get("enchantment").getAsString():"";
   int enchantLevel=integerField(json,"enchantmentLevel",id,0);
   if(level<Profession.MIN_LEVEL||level>Profession.MAX_LEVEL||amount<=0||price<0)throw new IllegalStateException("Invalid recipe values for "+id);
   validateSpecialFields(id,result,potion,enchant,enchantLevel);
   loaded.put(id,new CraftRecipe(profession,category,id,label,result,amount,rarity,costs,Map.of(),level,price,quest,defaultUnlocked,false,"",potion,enchant,enchantLevel));
  }
  recipes=Map.copyOf(loaded);
 }
 public List<CraftRecipe> getRecipes(Profession profession){return recipes.values().stream().filter(r->r.profession()==profession).toList();}
 public Optional<CraftRecipe> find(String id){return id==null||id.isBlank()?Optional.empty():Optional.ofNullable(recipes.get(canonical(id)));}
 private static Material resolveMaterial(String raw){String v=raw.trim().toUpperCase(Locale.ROOT); v=switch(v){case"DIAMOND_INGOT"->"DIAMOND";case"CHAIN"->"IRON_CHAIN";case"GOLD_AXE"->"GOLDEN_AXE";case"GOLD_BOOTS"->"GOLDEN_BOOTS";case"GOLD_CHESTPLATE"->"GOLDEN_CHESTPLATE";case"GOLD_HELMET"->"GOLDEN_HELMET";case"GOLD_HOE"->"GOLDEN_HOE";case"GOLD_LEGGINGS"->"GOLDEN_LEGGINGS";case"GOLD_PICKAXE"->"GOLDEN_PICKAXE";case"GOLD_SHOVEL"->"GOLDEN_SHOVEL";case"GOLD_SPEAR"->"GOLDEN_SPEAR";case"GOLD_SWORD"->"GOLDEN_SWORD";default->v;};return Material.matchMaterial(v);}
 private static Map<Material,Integer> parseCosts(JsonObject json,String id){JsonObject costs=json.has("costs")?json.getAsJsonObject("costs"):null;if(costs==null||costs.isEmpty())return Map.of();Map<Material,Integer> out=new EnumMap<>(Material.class);for(var e:costs.entrySet()){if(!e.getValue().isJsonPrimitive()||!e.getValue().getAsJsonPrimitive().isNumber())throw new IllegalStateException("Cost amount must be a JSON number for "+id);Material m=resolveMaterial(e.getKey());int a=e.getValue().getAsInt();if(m==null||m.isAir()||a<=0)throw new IllegalStateException("Invalid cost for "+id+": "+e.getKey());out.merge(m,a,(current, added) -> current + added);}return out;}
 private static void validateSpecialFields(String id,Material result,String potion,String enchant,int level){if(!potion.isBlank()){if(result!=Material.POTION&&result!=Material.SPLASH_POTION&&result!=Material.LINGERING_POTION)throw new IllegalStateException("potionType requires potion result for "+id);try{PotionType.valueOf(potion.trim().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid potionType for "+id,e);}}if(!enchant.isBlank()){if(result!=Material.ENCHANTED_BOOK||level<=0)throw new IllegalStateException("Invalid enchanted-book recipe "+id);var registry=RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);var value=registry.get(NamespacedKey.minecraft(enchant.trim().toLowerCase(Locale.ROOT)));if(value==null||level>value.getMaxLevel())throw new IllegalStateException("Invalid enchantment for "+id+": "+enchant+" "+level);}else if(level!=0)throw new IllegalStateException("enchantmentLevel requires an enchantment for "+id);}
 private static int integerField(JsonObject j,String k,String id,int d){if(!j.has(k))return d;if(!j.get(k).isJsonPrimitive()||!j.getAsJsonPrimitive(k).isNumber())throw new IllegalStateException(k+" must be a JSON number for "+id);return j.get(k).getAsInt();}
 private static long longField(JsonObject j,String k,String id,long d){if(!j.has(k))return d;if(!j.get(k).isJsonPrimitive()||!j.getAsJsonPrimitive(k).isNumber())throw new IllegalStateException(k+" must be a JSON number for "+id);return j.get(k).getAsLong();}
 private static String canonical(String raw){String v=raw.trim().toLowerCase(Locale.ROOT);if(v.startsWith("pixelrpg:"))v=v.substring(9);return v.replace('/',':');}
 private static String required(JsonObject j,String k){if(!j.has(k)||j.get(k).getAsString().isBlank())throw new IllegalStateException("Missing '"+k+"' in crafting recipe");return j.get(k).getAsString();}
 private static<E extends Enum<E>>E enumValue(Class<E> c,JsonObject j,String k,String id){try{return Enum.valueOf(c,required(j,k).toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid "+k+" for "+id,e);}}
 private static String pretty(Material m){String v=m.name().toLowerCase(Locale.ROOT).replace('_',' ');return Character.toUpperCase(v.charAt(0))+v.substring(1);}
}
