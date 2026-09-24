package de.pixelrpg.rpg.item;
import com.google.gson.JsonParser; import org.bukkit.Material; import org.bukkit.plugin.Plugin; import java.io.InputStreamReader; import java.nio.charset.StandardCharsets; import java.util.*;
public final class ItemDefinitionRegistry {
 private final Map<String,ItemDefinition> definitions=new HashMap<>();
 public ItemDefinitionRegistry(Plugin plugin){try(var in=plugin.getResource("data/item-definitions.json")){if(in==null)return;var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();for(var e:root.getAsJsonArray("items")){var j=e.getAsJsonObject();definitions.put(j.get("id").getAsString(),new ItemDefinition(j.get("id").getAsString(),j.get("name").getAsString(),Material.valueOf(j.get("material").getAsString()),ItemRarity.valueOf(j.get("rarity").getAsString()),ItemCategory.valueOf(j.get("category").getAsString()),j.get("itemLevel").getAsInt(),j.get("requiredLevel").getAsInt(),j.has("gearscoreModifier")?j.get("gearscoreModifier").getAsDouble():1));}}catch(Exception e){throw new IllegalStateException("Failed to load item definitions.",e);}}
 public Optional<ItemDefinition> find(String id){return Optional.ofNullable(definitions.get(id));}
 public Optional<ItemDefinition> findByMaterial(Material m,ItemRarity r,int level){return definitions.values().stream().filter(d->d.material()==m&&d.rarity()==r&&d.itemLevel()==level).findFirst();}
}
