package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.api.ItemAPI;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ItemService implements ItemAPI, AutoCloseable {
    private final ItemDefinitionRegistry definitions;
    private final UniqueItemService uniqueItems;
    private final FoodService foodService;
    private final RPGItemBuilder builder;
    private final RPGKeys keys;
    public ItemService(Plugin plugin,RPGKeys keys){
        this.keys=keys; this.definitions=new ItemDefinitionRegistry(plugin); this.uniqueItems=new UniqueItemService(plugin);
        this.foodService=new FoodService(plugin,keys); this.builder=new RPGItemBuilder(keys); this.builder.configureScaling(plugin);
    }
    @Override public Optional<ItemStack> createItem(Material material,ItemRarity rarity,int level){if(rarity==ItemRarity.UNIQUE)return Optional.empty();try{return Optional.of(builder.create(material,rarity,Math.clamp(level,1,99)));}catch(IllegalArgumentException ex){return Optional.empty();}}
    @Override public Optional<ItemStack> createItem(String itemId){return createDefined(itemId,0,false);}
    public Optional<ItemStack> createAdminItem(String itemId){return createDefined(itemId,0,true);}
    public Optional<ItemStack> createItem(String itemId,int level){return createDefined(itemId,Math.clamp(level,1,99),false);}
    public List<ItemDefinition> definitions(){return definitions.all().stream().toList();}
    @Override public boolean isRPGItem(ItemStack item){return getItemId(item).isPresent();}
    @Override public boolean isGuildItem(ItemStack item){return item!=null&&item.hasItemMeta()&&Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer().get(keys.guildItem(),PersistentDataType.BOOLEAN));}
    @Override public Optional<String> getItemId(ItemStack item){if(item==null||!item.hasItemMeta())return Optional.empty();String value=item.getItemMeta().getPersistentDataContainer().get(keys.itemId(),PersistentDataType.STRING);return value==null||value.isBlank()?Optional.empty():Optional.of(value);}
    @Override public Optional<ItemRarity> getRarity(ItemStack item){return readEnum(item,keys.rarity(),ItemRarity.class);}
    @Override public Optional<ItemCategory> getCategory(ItemStack item){return readEnum(item,keys.category(),ItemCategory.class);}
    @Override public Optional<ItemDefinition> getDefinition(ItemStack item){return getItemId(item).flatMap(definitions::find);}
    @Override public Optional<Integer> getRequiredLevel(ItemStack item){return read(item,keys.requiredLevel(),PersistentDataType.INTEGER);}
    @Override public Optional<Double> getGearscore(ItemStack item){return read(item,keys.gearscore(),PersistentDataType.DOUBLE);}
    public boolean isEconomySafeItem(ItemStack item){
        if(item==null||item.isEmpty()||!item.hasItemMeta())return false;var pdc=item.getItemMeta().getPersistentDataContainer();
        String id=pdc.get(keys.itemId(),PersistentDataType.STRING), instance=pdc.get(keys.instanceId(),PersistentDataType.STRING);
        ItemRarity rarity=getRarity(item).orElse(null);ItemCategory category=getCategory(item).orElse(null);Integer required=pdc.get(keys.requiredLevel(),PersistentDataType.INTEGER);Double gear=pdc.get(keys.gearscore(),PersistentDataType.DOUBLE);
        if(id==null||instance==null||rarity==null||category==null||required==null||required<1||required>99||gear==null||!Double.isFinite(gear)||gear<0)return false;
        if(Boolean.TRUE.equals(pdc.get(keys.soulbound(),PersistentDataType.BOOLEAN))||Boolean.TRUE.equals(pdc.get(keys.unique(),PersistentDataType.BOOLEAN))||rarity==ItemRarity.UNIQUE)return false;
        return definitions.find(id).map(d->d.category()==category&&d.material()==item.getType()&&!d.adminOnly()&&!d.soulbound()&&!d.unique()).orElse(false);
    }
    private Optional<ItemStack> createDefined(String id,int explicitLevel,boolean admin){
        ItemDefinition d=definitions.find(id).orElse(null);if(d==null||d.adminOnly()&&!admin||d.unique()&&!admin)return Optional.empty();
        boolean claimed=false;if(d.unique()){if(!uniqueItems.claim(d))return Optional.empty();claimed=true;}
        try{
            if(d.category()==ItemCategory.FOOD){
                ItemStack item=ItemStack.of(d.material());ItemMeta meta=item.getItemMeta();var pdc=meta.getPersistentDataContainer();
                pdc.set(keys.identified(),PersistentDataType.BOOLEAN,true);pdc.set(keys.itemId(),PersistentDataType.STRING,d.id());pdc.set(keys.instanceId(),PersistentDataType.STRING,UUID.randomUUID().toString());pdc.set(keys.rarity(),PersistentDataType.STRING,d.rarity().name());pdc.set(keys.itemLevel(),PersistentDataType.INTEGER,d.itemLevel());pdc.set(keys.requiredLevel(),PersistentDataType.INTEGER,d.requiredLevel());pdc.set(keys.category(),PersistentDataType.STRING,d.category().name());pdc.set(keys.guildItem(),PersistentDataType.BOOLEAN,false);pdc.set(keys.resourcepackId(),PersistentDataType.STRING,d.resourcepackId());pdc.set(keys.unique(),PersistentDataType.BOOLEAN,false);pdc.set(keys.gearscore(),PersistentDataType.DOUBLE,0D);meta.displayName(Component.text(d.name(),NamedTextColor.WHITE).decoration(TextDecoration.ITALIC,false));item.setItemMeta(meta);foodService.configure(item,d);return Optional.of(item);
            }
            int level=explicitLevel>0?explicitLevel:d.itemLevel();ItemStack item=builder.create(d.id(),d.name(),d.material(),d.rarity(),level,d.category()==ItemCategory.MELEE_WEAPON||d.category()==ItemCategory.RANGED_WEAPON);
            ItemMeta meta=item.getItemMeta();var pdc=meta.getPersistentDataContainer();pdc.set(keys.itemId(),PersistentDataType.STRING,d.id());pdc.set(keys.requiredLevel(),PersistentDataType.INTEGER,d.requiredLevel());pdc.set(keys.resourcepackId(),PersistentDataType.STRING,d.resourcepackId());pdc.set(keys.unique(),PersistentDataType.BOOLEAN,d.unique());pdc.set(keys.instanceId(),PersistentDataType.STRING,UUID.randomUUID().toString());
            if(!d.equipmentSlot().isBlank())pdc.set(keys.equipmentSlot(),PersistentDataType.STRING,d.equipmentSlot());if(!d.setId().isBlank())pdc.set(keys.setId(),PersistentDataType.STRING,d.setId());double gear=Math.round(level*d.rarity().getStatMultiplier()*d.gearscoreModifier()*10D)/10D;pdc.set(keys.gearscore(),PersistentDataType.DOUBLE,gear);if(d.soulbound())pdc.set(keys.soulbound(),PersistentDataType.BOOLEAN,true);
            if(!d.weaponAbility().isBlank()){pdc.set(keys.weaponAbility(),PersistentDataType.STRING,d.weaponAbility());pdc.set(keys.weaponAbilityCooldownMillis(),PersistentDataType.LONG,d.weaponAbilityCooldownMillis());}
            List<Component> lore=meta.lore()==null?new ArrayList<>():new ArrayList<>(meta.lore());lore.add(Component.text("Ausrüstungswert "+format(gear),NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));if(d.soulbound())lore.add(0,Component.text("⚡ Seelengebunden",NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC,false));if(d.unique())lore.add(0,Component.text("EINZIGARTIG • 1/1",NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false));meta.lore(lore);item.setItemMeta(meta);return Optional.of(item);
        }catch(RuntimeException ex){if(claimed)uniqueItems.release(d);throw ex;}
    }
    private static String format(double value){return Math.abs(value-Math.rint(value))<0.0001?Long.toString(Math.round(value)):String.format(Locale.ROOT,"%.1f",value);}
    private <T> Optional<T> read(ItemStack item,org.bukkit.NamespacedKey key,PersistentDataType<T,T> type){if(item==null||!item.hasItemMeta())return Optional.empty();return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(key,type));}
    private <E extends Enum<E>> Optional<E> readEnum(ItemStack item,org.bukkit.NamespacedKey key,Class<E> type){if(item==null||!item.hasItemMeta())return Optional.empty();String raw=item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);if(raw==null)return Optional.empty();try{return Optional.of(Enum.valueOf(type,raw.toUpperCase(Locale.ROOT)));}catch(IllegalArgumentException ex){return Optional.empty();}}
    @Override public void close(){uniqueItems.close();}
}