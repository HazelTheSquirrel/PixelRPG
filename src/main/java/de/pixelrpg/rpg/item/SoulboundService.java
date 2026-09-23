package de.pixelrpg.rpg.item;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public final class SoulboundService {
    public enum Result { SUCCESS, ALREADY_SOULBOUND, NOT_IDENTIFIED }
    private final RPGKeys keys;
    public SoulboundService(RPGKeys keys){this.keys=keys;}
    public boolean isSoulbound(ItemStack item){return item!=null&&item.hasItemMeta()&&Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer().get(keys.soulbound(),PersistentDataType.BOOLEAN));}
    public Result apply(ItemStack item){
        if(item==null||item.isEmpty()||!item.hasItemMeta())return Result.NOT_IDENTIFIED;
        var pdc=item.getItemMeta().getPersistentDataContainer();
        if(!Boolean.TRUE.equals(pdc.get(keys.identified(),PersistentDataType.BOOLEAN)))return Result.NOT_IDENTIFIED;
        if(Boolean.TRUE.equals(pdc.get(keys.soulbound(),PersistentDataType.BOOLEAN)))return Result.ALREADY_SOULBOUND;
        pdc.set(keys.soulbound(),PersistentDataType.BOOLEAN,true);
        var meta=item.getItemMeta(); List<Component> lore=meta.lore(); List<Component> updated=new ArrayList<>(); updated.add(Component.text("⚡ Soulbound",NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC,false)); if(lore!=null)updated.addAll(lore); meta.lore(updated); item.setItemMeta(meta); return Result.SUCCESS;
    }
}