package de.pixelrpg.rpg.equipment;

import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;

public final class EquipmentService implements Listener {
    private final Plugin plugin;
    private final PlayerProfileManager profileManager;
    public EquipmentService(Plugin plugin,PlayerProfileManager profileManager){this.plugin=plugin;this.profileManager=profileManager;}
    public Map<EquipmentSlot,ItemStack> snapshot(Player player){Map<EquipmentSlot,ItemStack> result=new EnumMap<>(EquipmentSlot.class);PlayerInventory inventory=player.getInventory();put(result,EquipmentSlot.HELMET,inventory.getHelmet());put(result,EquipmentSlot.CHEST,inventory.getChestplate());put(result,EquipmentSlot.LEGS,inventory.getLeggings());put(result,EquipmentSlot.FEET,inventory.getBoots());put(result,EquipmentSlot.MAINHAND,inventory.getItemInMainHand());put(result,EquipmentSlot.OFFHAND,inventory.getItemInOffHand());return result;}
    public void syncToProfile(Player player){profileManager.getProfile(player.getUniqueId()).ifPresent(profile->profile.setEquipment(snapshot(player)));}
    public void restoreFromProfile(Player player){profileManager.getProfile(player.getUniqueId()).ifPresent(profile->{Map<EquipmentSlot,ItemStack> stored=profile.getEquipment();if(stored.isEmpty())return;PlayerInventory inventory=player.getInventory();inventory.setHelmet(copy(stored.get(EquipmentSlot.HELMET)));inventory.setChestplate(copy(stored.get(EquipmentSlot.CHEST)));inventory.setLeggings(copy(stored.get(EquipmentSlot.LEGS)));inventory.setBoots(copy(stored.get(EquipmentSlot.FEET)));inventory.setItemInMainHand(copy(stored.get(EquipmentSlot.MAINHAND)));inventory.setItemInOffHand(copy(stored.get(EquipmentSlot.OFFHAND)));});}
    private void put(Map<EquipmentSlot,ItemStack> map,EquipmentSlot slot,ItemStack item){if(item!=null&&!item.isEmpty())map.put(slot,item.clone());}
    private ItemStack copy(ItemStack item){return item==null?null:item.clone();}
    // Restores persisted equipment after the player has joined the server.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event){event.getPlayer().getScheduler().runDelayed(plugin,task->restoreFromProfile(event.getPlayer()),null,1L);}
    // Persists the player's final live equipment state when they leave.
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event){syncToProfile(event.getPlayer());}
    // Reapplies persisted equipment after a respawn inventory reset.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event){event.getPlayer().getScheduler().runDelayed(plugin,task->restoreFromProfile(event.getPlayer()),null,1L);}
}