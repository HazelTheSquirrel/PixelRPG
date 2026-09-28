package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Handles the unique gathering-profession Grandmaster Meisterbrief lifecycle. */
public final class MeisterbriefService {
    private final NamespacedKey npcKey;
    private final NamespacedKey idKey;
    private final NamespacedKey signedKey;
    private final Set<UUID> consumed = new HashSet<>();

    public MeisterbriefService(PixelRPGPlugin plugin) {
        npcKey=new NamespacedKey(plugin,"meisterbrief-npc"); idKey=new NamespacedKey(plugin,"meisterbrief-id"); signedKey=new NamespacedKey(plugin,"meisterbrief-signed");
    }
    public ItemStack issue(String npcId){
        ItemStack item=new ItemStack(Material.PAPER); UUID id=UUID.randomUUID(); item.editMeta(meta->{meta.displayName(Component.text("Meisterbrief",NamedTextColor.GOLD));meta.lore(java.util.List.of(Component.text("Berufs-NPC: "+npcId,NamedTextColor.GRAY),Component.text("Unterschrift erforderlich.",NamedTextColor.YELLOW)));meta.getPersistentDataContainer().set(npcKey,PersistentDataType.STRING,npcId);meta.getPersistentDataContainer().set(idKey,PersistentDataType.STRING,id.toString());meta.getPersistentDataContainer().set(signedKey,PersistentDataType.BYTE,(byte)0);});return item;
    }
    public boolean isBrief(ItemStack item){return item!=null&&!item.isEmpty()&&item.getType()==Material.PAPER&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(npcKey,PersistentDataType.STRING);}
    public boolean signAtSpawn(Player player){
        ItemStack item=player.getInventory().getItemInMainHand(); if(!isBrief(item)||item.getItemMeta().getPersistentDataContainer().getOrDefault(signedKey,PersistentDataType.BYTE,(byte)0)!=0)return false;
        if(player.getWorld().getSpawnLocation().distanceSquared(player.getLocation())>64.0D)return false;
        item.editMeta(meta->{meta.getPersistentDataContainer().set(signedKey,PersistentDataType.BYTE,(byte)1);meta.lore(java.util.List.of(Component.text("Unterschrieben am zentralen Spawn.",NamedTextColor.GREEN)));}); return true;
    }
    public boolean hasSigned(Player player,String npcId){return findSigned(player,npcId)!=null;}
    public boolean consumeSigned(Player player,String npcId){
        ItemStack item=findSigned(player,npcId); if(item==null)return false; String id=item.getItemMeta().getPersistentDataContainer().get(idKey,PersistentDataType.STRING); try{UUID uuid=UUID.fromString(id);if(consumed.contains(uuid))return false;consumed.add(uuid);}catch(Exception e){return false;}
        item.setAmount(item.getAmount()-1); if(item.getAmount()<=0)player.getInventory().setItemInMainHand(null); return true;
    }
    private ItemStack findSigned(Player player,String npcId){for(ItemStack item:player.getInventory().getStorageContents()){if(!isBrief(item))continue;var pdc=item.getItemMeta().getPersistentDataContainer();if(!npcId.equals(pdc.get(npcKey,PersistentDataType.STRING)))continue;if(pdc.getOrDefault(signedKey,PersistentDataType.BYTE,(byte)0)==1)return item;}return null;}
}
