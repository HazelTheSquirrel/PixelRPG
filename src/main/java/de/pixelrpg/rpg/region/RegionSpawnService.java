package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/** Event-driven explicit region spawning without a global polling loop. */
public final class RegionSpawnService implements Listener, AutoCloseable {
    private static final long INITIAL_DELAY=100L, RESPAWN_DELAY=200L; private static final double RANGE=64.0; private static final int RANGE_CHUNKS=4;
    private final JavaPlugin plugin; private final RegionManager regions; private final NamespacedKey markerKey;
    private final Map<String,Long> nextReady=new HashMap<>(); private final Map<String,BukkitTask> scheduled=new HashMap<>(); private boolean started;
    public RegionSpawnService(JavaPlugin plugin,RegionManager regions){this.plugin=Objects.requireNonNull(plugin);this.regions=Objects.requireNonNull(regions);this.markerKey=new NamespacedKey(plugin,"region_spawn_point");}
    public void start(){if(started)return;started=true;plugin.getServer().getPluginManager().registerEvents(this,plugin);long ready=plugin.getServer().getCurrentTick()+INITIAL_DELAY;for(var p:allPoints())nextReady.putIfAbsent(key(p),ready);for(Player p:plugin.getServer().getOnlinePlayers())evaluateAround(p.getLocation());}
    public boolean isManagedSpawn(Entity entity){return entity!=null&&entity.getPersistentDataContainer().has(markerKey,PersistentDataType.STRING);}
    private void evaluateAround(Location center){if(!started||center==null||center.getWorld()==null)return;for(var ref:regions.spawnPointsNear(center,RANGE_CHUNKS)){Location l=ref.point().location(center.getWorld());if(l!=null&&l.distanceSquared(center)<=RANGE*RANGE)evaluate(ref,key(ref),center,l);}}
    private void evaluate(var ref,String pointKey,Location center,Location location){}
    private void evaluate(RegionManager.SpawnPointRef ref,String pointKey,Location center,Location location){
        long now=plugin.getServer().getCurrentTick(),ready=nextReady.getOrDefault(pointKey,now);if(now<ready){schedule(pointKey,ready-now);return;}
        if(center!=null&&location.distanceSquared(center)>RANGE*RANGE)return;if(center==null&&!hasNearbyPlayer(location))return;if(hasManagedMob(location,pointKey))return;
        EntityType type=SpawnMobType.resolve(ref.point().mobType());if(type==null||type.getEntityClass()==null||!Monster.class.isAssignableFrom(type.getEntityClass()))return;
        World world=location.getWorld();if(world==null)return;Entity entity=world.spawnEntity(location,type);entity.getPersistentDataContainer().set(markerKey,PersistentDataType.STRING,pointKey);
        nextReady.put(pointKey,now+RESPAWN_DELAY);cancel(pointKey);
    }
    private void schedule(String key,long delay){cancel(key);scheduled.put(key,plugin.getServer().getScheduler().runTaskLater(plugin,()->evaluateByKey(key),Math.max(1L,delay)));}
    private void evaluateByKey(String key){RegionManager.SpawnPointRef ref=findPoint(key);if(ref==null)return;World world=plugin.getServer().getWorld(ref.point().worldName());if(world==null)return;Location l=ref.point().location(world);if(l==null||!hasNearbyPlayer(l))return;evaluate(ref,key,null,l);}
    private RegionManager.SpawnPointRef findPoint(String key){int s=key.indexOf(':');if(s<=0)return null;try{UUID id=UUID.fromString(key.substring(0,s));int i=Integer.parseInt(key.substring(s+1));PixelRegion r=regions.get(id).orElse(null);return r==null||i<0||i>=r.spawnPoints().size()?null:new RegionManager.SpawnPointRef(id,i,r.spawnPoints().get(i));}catch(IllegalArgumentException e){return null;}}
    private List<RegionManager.SpawnPointRef> allPoints(){List<RegionManager.SpawnPointRef> out=new ArrayList<>();for(PixelRegion r:regions.all())for(int i=0;i<r.spawnPoints().size();i++)out.add(new RegionManager.SpawnPointRef(r.id(),i,r.spawnPoints().get(i)));return out;}
    private boolean hasNearbyPlayer(Location l){return !l.getNearbyPlayers(RANGE).isEmpty();}
    private boolean hasManagedMob(Location l,String key){for(Entity e:l.getNearbyEntities(1.5,2.5,1.5))if(e instanceof Monster&&key.equals(e.getPersistentDataContainer().get(markerKey,PersistentDataType.STRING)))return true;return false;}
    private void cancel(String key){BukkitTask t=scheduled.remove(key);if(t!=null)t.cancel();}
    private static String key(RegionManager.SpawnPointRef r){return r.regionId()+":"+r.index();}

    // A player joining activates only explicit spawn points in the nearby chunk window.
    @EventHandler public void onJoin(PlayerJoinEvent event){evaluateAround(event.getPlayer().getLocation());}
    // Movement only reevaluates spawn points after crossing a block boundary.
    @EventHandler public void onMove(PlayerMoveEvent event){if(event.hasChangedBlock()){evaluateAround(event.getFrom());evaluateAround(event.getTo());}}
    // World changes activate spawn points in the player's destination area.
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event){evaluateAround(event.getPlayer().getLocation());}
    // Death of a managed mob wakes only its own explicit spawn point after the configured delay.
    @EventHandler public void onDeath(EntityDeathEvent event){String key=event.getEntity().getPersistentDataContainer().get(markerKey,PersistentDataType.STRING);if(key==null)return;long now=plugin.getServer().getCurrentTick();long ready=nextReady.getOrDefault(key,now);schedule(key,Math.max(1L,ready-now));}
    // Disconnects recheck only the affected area instead of scanning the complete world.
    @EventHandler public void onQuit(PlayerQuitEvent event){evaluateAround(event.getPlayer().getLocation());}
    @Override public void close(){if(!started)return;HandlerList.unregisterAll(this);scheduled.values().forEach(BukkitTask::cancel);scheduled.clear();nextReady.clear();started=false;}
}
