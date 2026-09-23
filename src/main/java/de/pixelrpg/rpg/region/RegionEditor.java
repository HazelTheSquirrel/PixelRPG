package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Owns temporary admin polygon/spawn-point editing sessions and their visualization. */
public final class RegionEditor implements AutoCloseable {
    private final JavaPlugin plugin; private final RegionManager regions; private final NamespacedKey toolKey,spawnToolKey;
    private final Map<UUID,Session> sessions=new ConcurrentHashMap<>(); private BukkitTask visualization;
    public RegionEditor(JavaPlugin plugin,RegionManager regions){this.plugin=Objects.requireNonNull(plugin);this.regions=Objects.requireNonNull(regions);toolKey=new NamespacedKey(plugin,"region_editor_tool");spawnToolKey=new NamespacedKey(plugin,"region_spawn_tool");}
    public void start(){if(visualization==null)visualization=plugin.getServer().getScheduler().runTaskTimer(plugin,this::visualize,1L,2L);}
    public void begin(Player p,String name){sessions.put(p.getUniqueId(),new Session(UUID.randomUUID(),name));removeTools(p);p.getInventory().addItem(tool());p.sendMessage(Component.text("Region-CREATE gestartet: "+name,NamedTextColor.GREEN));p.sendMessage(Component.text("Rechtsklick auf Blöcke setzt Polygonpunkte.",NamedTextColor.GRAY));}
    public boolean isEditing(UUID id){return sessions.containsKey(id);}
    public boolean isTool(ItemStack i){return has(i,Material.STICK,toolKey,PersistentDataType.BYTE);}
    public boolean isSpawnTool(ItemStack i){return has(i,Material.ARROW,spawnToolKey,PersistentDataType.STRING);}
    public boolean addPoint(Player p,Location l){Session s=sessions.get(p.getUniqueId());if(s==null||l.getWorld()==null)return false;if(s.world==null)s.world=l.getWorld().getName();if(!s.world.equals(l.getWorld().getName())){p.sendMessage(Component.text("Alle Punkte müssen in derselben Welt liegen.",NamedTextColor.RED));return true;}RegionPoint point=new RegionPoint(l.getBlockX()+.5,l.getBlockZ()+.5);if(!s.points.add(point)){p.sendMessage(Component.text("Dieser Punkt wurde bereits gesetzt.",NamedTextColor.RED));return true;}s.finished=false;p.sendMessage(Component.text("P"+s.points.size()+" gesetzt.",NamedTextColor.AQUA));return true;}
    public boolean beginSpawnMode(Player p,String type){Session s=sessions.get(p.getUniqueId());if(s==null)return false;if(!SpawnMobType.isLivingEntity(type)){p.sendMessage(Component.text("Unbekannter Entity-Typ.",NamedTextColor.RED));return false;}s.mob=SpawnMobType.normalize(type);removeSpawnTools(p);p.getInventory().addItem(spawnTool(s.mob));return true;}
    public boolean addSpawnPoint(Player p,Location l){Session s=sessions.get(p.getUniqueId());if(s==null||s.mob==null||l.getWorld()==null)return false;if(s.world==null)s.world=l.getWorld().getName();if(!s.world.equals(l.getWorld().getName()))return true;RegionSpawnPoint point=new RegionSpawnPoint(s.mob,s.world,l.getBlockX()+.5,l.getBlockY()+1,l.getBlockZ()+.5);if(!s.spawns.contains(point))s.spawns.add(point);return true;}
    public boolean finish(Player p){Session s=sessions.get(p.getUniqueId());if(s==null)return false;var v=RegionGeometry.validate(s.points);if(!v.valid()){p.sendMessage(Component.text("Polygon ungültig: "+v.error(),NamedTextColor.RED));return true;}s.finished=true;p.sendMessage(Component.text("Polygon gültig: "+s.points.size()+" Punkte.",NamedTextColor.GREEN));return true;}
    public boolean confirm(Player p){Session s=sessions.get(p.getUniqueId());if(s==null)return false;if(!s.finished||s.world==null){p.sendMessage(Component.text("Bitte zuerst finish ausführen und Punkte setzen.",NamedTextColor.YELLOW));return true;}World w=plugin.getServer().getWorld(s.world);if(w==null){p.sendMessage(Component.text("Die Welt ist nicht geladen.",NamedTextColor.RED));return true;}var result=regions.create(s.id,s.world,s.points,w.getMinHeight(),w.getMaxHeight()-1,s.name,RegionType.OTHER,s.spawns);if(!result.valid()){p.sendMessage(Component.text("Region konnte nicht erstellt werden: "+result.error(),NamedTextColor.RED));return true;}sessions.remove(p.getUniqueId());removeTools(p);p.sendMessage(Component.text("Region erstellt: "+s.name,NamedTextColor.GREEN));return true;}
    public void cancel(Player p){if(sessions.remove(p.getUniqueId())!=null){removeTools(p);p.sendMessage(Component.text("Region-CREATE abgebrochen.",NamedTextColor.YELLOW));}}
    private void visualize(){for(var e:sessions.entrySet()){Player p=plugin.getServer().getPlayer(e.getKey());if(p==null||!p.isOnline())continue;Session s=e.getValue();World w=s.world==null?p.getWorld():plugin.getServer().getWorld(s.world);if(w==null)continue;double y=p.getLocation().getY()+1;for(int i=0;i<s.points.size();i++){RegionPoint a=s.points.get(i);p.spawnParticle(Particle.END_ROD,new Location(w,a.x(),y,a.z()),2,.05,.05,.05,0);if(i>0)line(p,w,s.points.get(i-1),a,y);}if(s.points.size()>2)line(p,w,s.points.getLast(),s.points.getFirst(),y);for(RegionSpawnPoint sp:s.spawns){Location l=sp.location(w);if(l!=null)p.spawnParticle(Particle.FLAME,l,4,.12,.2,.12,0);}}}
    private void line(Player p,World w,RegionPoint a,RegionPoint b,double y){double dx=b.x()-a.x(),dz=b.z()-a.z(),len=Math.sqrt(dx*dx+dz*dz);int n=Math.max(1,(int)Math.ceil(len*1.5));for(int i=0;i<=n;i++){double t=i/(double)n;p.spawnParticle(Particle.END_ROD,new Location(w,a.x()+dx*t,y,a.z()+dz*t),1,0,0,0,0);}}
    private ItemStack tool(){ItemStack i=new ItemStack(Material.STICK);ItemMeta m=i.getItemMeta();m.displayName(Component.text("PixelRPG Region-Werkzeug",NamedTextColor.GOLD));m.getPersistentDataContainer().set(toolKey,PersistentDataType.BYTE,(byte)1);i.setItemMeta(m);return i;}
    private ItemStack spawnTool(String mob){ItemStack i=new ItemStack(Material.ARROW);ItemMeta m=i.getItemMeta();m.displayName(Component.text("Spawnpunkt: "+mob,NamedTextColor.GOLD));m.getPersistentDataContainer().set(spawnToolKey,PersistentDataType.STRING,mob);i.setItemMeta(m);return i;}
    private void removeTools(Player p){for(int s=0;s<p.getInventory().getSize();s++){ItemStack i=p.getInventory().getItem(s);if(isTool(i)||isSpawnTool(i))p.getInventory().setItem(s,null);}}
    private void removeSpawnTools(Player p){for(int s=0;s<p.getInventory().getSize();s++)if(isSpawnTool(p.getInventory().getItem(s)))p.getInventory().setItem(s,null);}
    private static <T> boolean has(ItemStack i,Material m,NamespacedKey k,org.bukkit.persistence.PersistentDataType<T,?> type){if(i==null||i.getType()!=m)return false;ItemMeta meta=i.getItemMeta();return meta!=null&&meta.getPersistentDataContainer().has(k,type);}
    @Override public void close(){if(visualization!=null)visualization.cancel();visualization=null;sessions.clear();}
    private static final class Session{final UUID id;final String name;final List<RegionPoint> points=new ArrayList<>();final List<RegionSpawnPoint> spawns=new ArrayList<>();String world,mob;boolean finished;Session(UUID id,String name){this.id=id;this.name=name;}}
}
