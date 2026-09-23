package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ItemDefinitionRegistry {
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    public ItemDefinitionRegistry(Plugin plugin) {
        loadFile(plugin, "item-definitions.json");
        loadFile(plugin, "boss-reward-items.json");
        loadFile(plugin, "food-definitions.json");
    }
    public Optional<ItemDefinition> find(String id) { return id == null || id.isBlank() ? Optional.empty() : Optional.ofNullable(definitions.get(normalize(id))); }
    public Collection<ItemDefinition> all() { return java.util.List.copyOf(definitions.values()); }
    private void loadFile(Plugin plugin, String fileName) {
        JsonObject root = new JsonDataManager(plugin).load(fileName);
        JsonArray items = root.getAsJsonArray("items");
        if (items == null) throw new IllegalStateException(fileName + " requires an 'items' array");
        for (var element : items) {
            ItemDefinition definition = parse(element.getAsJsonObject());
            if (definitions.put(definition.id(), definition) != null) throw new IllegalStateException("Duplicate PixelRPG item definition: " + definition.id());
        }
    }
    private ItemDefinition parse(JsonObject json) {
        String id = required(json,"id"), name = required(json,"name");
        Material material = Material.matchMaterial(required(json,"material"));
        if (material == null) throw new IllegalStateException("Unknown item material for " + id);
        ItemRarity rarity = enumValue(ItemRarity.class,json,"rarity",id);
        ItemCategory category = enumValue(ItemCategory.class,json,"category",id);
        if (category == ItemCategory.FOOD) {
            if (material != Material.CLOCK) throw new IllegalStateException("Food items must use CLOCK: " + id);
        } else if (!GearCategoryRegistry.resolve(material).filter(category::equals).isPresent()) {
            throw new IllegalStateException("Category " + category + " does not match material " + material + " for " + id);
        }
        int itemLevel = integer(json,"itemLevel",1);
        int requiredLevel = integer(json,"requiredLevel",itemLevel);
        return new ItemDefinition(id,name,material,rarity,category,itemLevel,requiredLevel,
                string(json,"weaponAbility",""),longValue(json,"weaponAbilityCooldownMillis",0L),
                bool(json,"soulbound",false),bool(json,"unique",rarity == ItemRarity.UNIQUE),
                bool(json,"adminOnly",rarity == ItemRarity.UNIQUE),string(json,"resourcepackId",id),
                number(json,"gearscoreModifier",1.0D),string(json,"equipmentSlot",""),string(json,"setId",""));
    }
    private static String required(JsonObject json,String key){String value=string(json,key,"");if(value.isBlank())throw new IllegalStateException("Missing '"+key+"' in item definition");return value;}
    private static String string(JsonObject json,String key,String fallback){return json.has(key)?json.get(key).getAsString():fallback;}
    private static int integer(JsonObject json,String key,int fallback){return json.has(key)?json.get(key).getAsInt():fallback;}
    private static long longValue(JsonObject json,String key,long fallback){return json.has(key)?json.get(key).getAsLong():fallback;}
    private static double number(JsonObject json,String key,double fallback){return json.has(key)?json.get(key).getAsDouble():fallback;}
    private static boolean bool(JsonObject json,String key,boolean fallback){return json.has(key)?json.get(key).getAsBoolean():fallback;}
    private static <E extends Enum<E>> E enumValue(Class<E> type,JsonObject json,String key,String id){try{return Enum.valueOf(type,required(json,key).toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){throw new IllegalStateException("Invalid "+key+" for "+id,ex);}}
    private static String normalize(String id){String n=id.trim().toLowerCase(Locale.ROOT);return n.startsWith("pixelrpg:")?n:"pixelrpg:"+n;}
}