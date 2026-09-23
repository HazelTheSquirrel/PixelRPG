package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FoodService implements Listener {
    private final FoodDefinitionRegistry definitions;
    private final RPGKeys keys;
    public FoodService(Plugin plugin,RPGKeys keys){this.definitions=new FoodDefinitionRegistry(plugin);this.keys=keys;plugin.getServer().getPluginManager().registerEvents(this,plugin);}
    public void configure(ItemStack item,ItemDefinition definition){
        FoodDefinition food=definitions.find(definition.id()).orElseThrow(()->new IllegalStateException("Missing food definition for "+definition.id()));
        NamespacedKey model=NamespacedKey.fromString(definition.resourcepackId());
        if(model!=null){var meta=item.getItemMeta();meta.setItemModel(model);item.setItemMeta(meta);}
        item.setData(DataComponentTypes.FOOD,FoodProperties.food().nutrition(food.nutrition()).saturation(food.saturation()).canAlwaysEat(food.canAlwaysEat()));
        item.setData(DataComponentTypes.CONSUMABLE,Consumable.consumable().consumeSeconds(1.6F).animation(animation(definition.id())).sound(Key.key("minecraft:entity.generic.eat")).hasConsumeParticles(true));
    }
    // Applies configured PixelRPG food effects after a successful consumption.
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event){
        ItemStack item=event.getItem();if(item==null||item.isEmpty()||!item.hasItemMeta())return;
        String id=item.getItemMeta().getPersistentDataContainer().get(keys.itemId(),org.bukkit.persistence.PersistentDataType.STRING);
        if(id==null||id.isBlank())return;
        FoodDefinition food=definitions.find(id).orElse(null);if(food==null||food.effectType().isBlank()||food.effectDurationSeconds()<=0)return;
        NamespacedKey effectKey=NamespacedKey.minecraft(food.effectType().toLowerCase(java.util.Locale.ROOT));
        PotionEffectType type=Registry.POTION_EFFECT_TYPE.get(effectKey);if(type==null)return;
        Player player=event.getPlayer();player.addPotionEffect(new PotionEffect(type,food.effectDurationSeconds()*20,food.effectAmplifier(),true,true,true));
    }
    private static ItemUseAnimation animation(String id){String n=id.toLowerCase(java.util.Locale.ROOT);return n.contains("kakao")||n.contains("saft")||n.contains("tee")||n.contains("latte")||n.contains("milch")?ItemUseAnimation.DRINK:ItemUseAnimation.EAT;}
}