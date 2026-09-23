package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.config.JsonDataManager;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGItemBuilder {
    private final RPGKeys keys;
    private double growthMultiplier=2.0, weaponDamage=3.0, weaponCrit=0.5, weaponCritDamage=0.05, weaponReach=0.10, lifesteal=0.25, armor=0.7, health=1.2, movementSpeed=0.001, toolEfficiency=1.0;
    public RPGItemBuilder(RPGKeys keys){this.keys=keys;}
    public void configureScaling(Plugin plugin){
        var root=new JsonDataManager(plugin).load("item-scaling.json");
        growthMultiplier=positive(root,"growthMultiplier",growthMultiplier);
        var base=root.getAsJsonObject("baseStats");
        weaponDamage=positive(base,"weaponDamage",weaponDamage); weaponCrit=positive(base,"weaponCritChance",weaponCrit); weaponCritDamage=positive(base,"weaponCritDamage",weaponCritDamage);
        weaponReach=positive(base,"weaponReach",weaponReach); lifesteal=positive(base,"weaponLifesteal",lifesteal); armor=positive(base,"armor",armor); health=positive(base,"health",health); movementSpeed=positive(base,"movementSpeed",movementSpeed); toolEfficiency=positive(base,"toolEfficiency",toolEfficiency);
    }
    public ItemStack create(Material material,ItemRarity rarity,int level){
        if(material==null||rarity==null||!Level.isValidNormalLevel(level))throw new IllegalArgumentException("Invalid item parameters");
        ItemCategory category=GearCategoryRegistry.resolve(material).orElseThrow(()->new IllegalArgumentException("Unsupported material: "+material));
        return create("pixelrpg:generated/"+category.name().toLowerCase(Locale.ROOT)+"/"+material.name().toLowerCase(Locale.ROOT)+"/"+rarity.name().toLowerCase(Locale.ROOT)+"/lvl_"+level,generatedName(material,rarity,level),material,rarity,level,true);
    }
    public ItemStack create(String id,String name,Material material,ItemRarity rarity,int level,boolean guildItem){
        ItemCategory category=GearCategoryRegistry.resolve(material).orElseThrow(()->new IllegalArgumentException("Unsupported material: "+material));
        ItemStack item=ItemStack.of(material); ItemMeta meta=item.getItemMeta(); PersistentDataContainer pdc=meta.getPersistentDataContainer();
        pdc.set(keys.identified(),PersistentDataType.BOOLEAN,true); pdc.set(keys.itemId(),PersistentDataType.STRING,normalize(id)); pdc.set(keys.instanceId(),PersistentDataType.STRING,UUID.randomUUID().toString());
        pdc.set(keys.rarity(),PersistentDataType.STRING,rarity.name()); pdc.set(keys.itemLevel(),PersistentDataType.INTEGER,level); pdc.set(keys.requiredLevel(),PersistentDataType.INTEGER,level); pdc.set(keys.category(),PersistentDataType.STRING,category.name()); pdc.set(keys.guildItem(),PersistentDataType.BOOLEAN,guildItem);
        double factor=rarity.getStatMultiplier()*levelScaling(level); List<Component> lore=new ArrayList<>();
        lore.add(rarity.displayName().decoration(TextDecoration.ITALIC,false)); lore.add(Component.text("Gegenstandslevel "+level,NamedTextColor.YELLOW)); lore.add(Component.text("Benötigt Level "+level,NamedTextColor.RED));
        addStats(lore,pdc,category,factor); double gearscore=Math.round(level*rarity.getStatMultiplier()*10.0)/10.0; pdc.set(keys.gearscore(),PersistentDataType.DOUBLE,gearscore); lore.add(Component.text("Ausrüstungswert "+format(gearscore),NamedTextColor.YELLOW));
        meta.displayName(Component.text(name,NamedTextColor.WHITE).decoration(TextDecoration.ITALIC,false)); meta.lore(lore); item.setItemMeta(meta); return item;
    }
    private void addStats(List<Component> lore,PersistentDataContainer pdc,ItemCategory category,double factor){
        switch(category.getProfile()){
            case WEAPON -> { double attack=roll(weaponDamage*factor); pdc.set(keys.attackPower(),PersistentDataType.DOUBLE,attack); lore.add(Component.text("+"+format(attack)+" Angriffskraft",NamedTextColor.GOLD)); double crit=roll(weaponCrit*factor); pdc.set(keys.critChance(),PersistentDataType.DOUBLE,crit); lore.add(Component.text("+"+format(crit)+"% Kritische Trefferchance",NamedTextColor.LIGHT_PURPLE)); double cd=roll(weaponCritDamage*factor); pdc.set(keys.critDamage(),PersistentDataType.DOUBLE,cd); lore.add(Component.text("+"+format(cd*100)+"% Kritischer Schaden",NamedTextColor.LIGHT_PURPLE)); double reach=roll(weaponReach*factor); pdc.set(keys.reachBonus(),PersistentDataType.DOUBLE,reach); lore.add(Component.text("+"+format(reach)+" Reichweite",NamedTextColor.AQUA)); double ls=roll(lifesteal*factor); pdc.set(keys.lifestealPercent(),PersistentDataType.DOUBLE,ls); lore.add(Component.text("+"+format(ls)+"% Lebensraub",NamedTextColor.DARK_RED)); }
            case ARMOR -> { double hp=roll(health*factor); pdc.set(keys.healthBonus(),PersistentDataType.DOUBLE,hp); lore.add(Component.text("+"+format(hp)+" LP",NamedTextColor.GREEN)); double ar=roll(armor*factor); pdc.set(keys.armorValue(),PersistentDataType.DOUBLE,ar); lore.add(Component.text("+"+format(ar)+" Rüstung",NamedTextColor.BLUE)); double ms=roll(movementSpeed*factor); pdc.set(keys.movementSpeed(),PersistentDataType.DOUBLE,ms); lore.add(Component.text("+"+format(ms*100)+"% Bewegungsgeschwindigkeit",NamedTextColor.WHITE)); }
            case TOOL -> { double value=roll(toolEfficiency*factor); pdc.set(keys.toolBonus(),PersistentDataType.DOUBLE,value); lore.add(Component.text("+"+format(value)+" Effizienz",NamedTextColor.YELLOW)); }
            case SHIELD,FOOD -> {}
        }
    }
    private double levelScaling(int level){double progress=(level-1D)/98D;return 1D+Math.max(0D,growthMultiplier-1D)*Math.clamp(progress,0D,1D);}
    private static double positive(com.google.gson.JsonObject o,String k,double f){return o!=null&&o.has(k)&&o.get(k).isJsonPrimitive()&&o.get(k).getAsJsonPrimitive().isNumber()&&o.get(k).getAsDouble()>0?o.get(k).getAsDouble():f;}
    private static double roll(double value){return Math.round(value*ThreadLocalRandom.current().nextDouble(0.5,1.5000000001)*100D)/100D;}
    private static String format(double value){return String.format(Locale.ROOT,"%.2f",value);}
    private static String generatedName(Material material,ItemRarity rarity,int level){return rarity.displayName().toString()+" "+material.name().toLowerCase(Locale.ROOT).replace('_',' ')+" "+level;}
    private static String normalize(String id){String n=id.trim().toLowerCase(Locale.ROOT);return n.startsWith("pixelrpg:")?n:"pixelrpg:"+n;}
}