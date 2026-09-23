package de.pixelrpg.rpg.economy;

import de.pixelrpg.rpg.core.RPGKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class GuildCurrencyItemFactory {
    private final RPGKeys keys;
    private int maxStackSize=64;
    public GuildCurrencyItemFactory(RPGKeys keys){this.keys=keys;}
    public void configureMaxStackSize(int value){maxStackSize=Math.max(1,value);}
    public ItemStack createSingleStack(long amount){int clamped=(int)Math.clamp(amount,1L,maxStackSize);ItemStack item=new ItemStack(Material.SUNFLOWER,clamped);var meta=item.getItemMeta();meta.displayName(Component.text("Goldtaler",NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false));meta.lore(List.of(Component.text("1 Goldtaler = 1 Gold",NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false),Component.text("In der Bank einzahlen oder bei Tod verlieren.",NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC,false)));meta.getPersistentDataContainer().set(keys.guildGold(),org.bukkit.persistence.PersistentDataType.DOUBLE,1.0);item.setItemMeta(meta);return item;}
    public List<ItemStack> createStacks(long total){List<ItemStack> result=new ArrayList<>();long remaining=Math.max(0,total);while(remaining>0){long take=Math.min(maxStackSize,remaining);result.add(createSingleStack(take));remaining-=take;}return result;}
    public boolean isCurrency(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(keys.guildGold(),org.bukkit.persistence.PersistentDataType.DOUBLE);}
    public double readAmount(ItemStack item){return isCurrency(item)?item.getItemMeta().getPersistentDataContainer().getOrDefault(keys.guildGold(),org.bukkit.persistence.PersistentDataType.DOUBLE,1.0):0.0;}
}