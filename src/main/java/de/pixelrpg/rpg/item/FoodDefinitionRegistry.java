package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.plugin.Plugin;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FoodDefinitionRegistry {
    private final Map<String,FoodDefinition> definitions=new LinkedHashMap<>();
    public FoodDefinitionRegistry(Plugin plugin){
        JsonObject root=new JsonDataManager(plugin).load("food-definitions.json");
        JsonArray items=root.getAsJsonArray("items");
        if(items==null)throw new IllegalStateException("food-definitions.json requires an 'items' array");
        for(var element:items){FoodDefinition definition=parse(element.getAsJsonObject());if(definitions.put(definition.id(),definition)!=null)throw new IllegalStateException("Duplicate food definition: "+definition.id());}
    }
    public Optional<FoodDefinition> find(String id){return id==null||id.isBlank()?Optional.empty():Optional.ofNullable(definitions.get(normalize(id)));}
    private FoodDefinition parse(JsonObject json){String id=required(json,"id");return new FoodDefinition(id,intValue(json,"nutrition",1),(float)number(json,"saturation",0.5D),bool(json,"canAlwaysEat",false),string(json,"effectType",""),intValue(json,"effectDurationSeconds",0),intValue(json,"effectAmplifier",0));}
    private static String required(JsonObject json,String key){String v=string(json,key,"");if(v.isBlank())throw new IllegalStateException("Missing '"+key+"' in food definition");return v;}
    private static String string(JsonObject json,String key,String fallback){return json.has(key)?json.get(key).getAsString():fallback;}
    private static int intValue(JsonObject json,String key,int fallback){return json.has(key)?json.get(key).getAsInt():fallback;}
    private static double number(JsonObject json,String key,double fallback){return json.has(key)?json.get(key).getAsDouble():fallback;}
    private static boolean bool(JsonObject json,String key,boolean fallback){return json.has(key)?json.get(key).getAsBoolean():fallback;}
    private static String normalize(String id){String n=id.trim().toLowerCase(Locale.ROOT);return n.startsWith("pixelrpg:")?n:"pixelrpg:"+n;}
}