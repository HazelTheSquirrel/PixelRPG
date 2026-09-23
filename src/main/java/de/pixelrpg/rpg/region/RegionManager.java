package de.pixelrpg.rpg.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/** Owns the in-memory region snapshot, spatial indexes and serialized persistence writes. */
public final class RegionManager implements AutoCloseable {
    private final JavaPlugin plugin; private final RegionRepository repository;
    private final Map<UUID,PixelRegion> regions=new HashMap<>(); private final Map<String,PixelRegion> globals=new HashMap<>();
    private final Map<ChunkKey,List<UUID>> index=new HashMap<>(); private final Map<ChunkKey,List<SpawnPointRef>> spawnIndex=new HashMap<>();
    private CompletableFuture<Void> persistence=CompletableFuture.completedFuture(null); private boolean closing;

    public RegionManager(JavaPlugin plugin,RegionRepository repository){this.plugin=Objects.requireNonNull(plugin);this.repository=Objects.requireNonNull(repository);}
    public CompletableFuture<Void> loadAsync(){
        return repository.loadAsync().thenAccept(snapshot->plugin.getServer().getScheduler().runTask(plugin,()->apply(snapshot)));
    }
    private synchronized void apply(RegionRepository.Snapshot snapshot){
        if(closing)return;regions.clear();globals.clear();index.clear();spawnIndex.clear();
        snapshot.regions().forEach(this::register);globals.putAll(snapshot.globalRegions());plugin.getServer().getWorlds().forEach(w->globalRegion(w.getName()));
        if(repository.consumeMigrationNeeded())save();
    }
    public synchronized Optional<PixelRegion> get(UUID id){if(id==null)return Optional.empty();PixelRegion r=regions.get(id);if(r!=null)return Optional.of(r);return globals.values().stream().filter(g->g.id().equals(id)).findFirst();}
    public synchronized List<PixelRegion> all(){return regions.values().stream().sorted(Comparator.comparing(r -> r.name(),String.CASE_INSENSITIVE_ORDER)).toList();}
    public synchronized RegionGeometry.ValidationResult create(UUID id,String world,List<RegionPoint> points,int minY,int maxY,String name,RegionType type,List<RegionSpawnPoint> spawns){
        if(closing||regions.containsKey(id))return RegionGeometry.ValidationResult.invalid("Eine Region mit dieser ID existiert bereits.");
        if(world==null||world.isBlank()||minY>maxY)return RegionGeometry.ValidationResult.invalid("Ungültige Regionseckdaten.");
        var v=RegionGeometry.validate(points);if(!v.valid())return v;
        PixelRegion r=new PixelRegion(id,world,v.geometry(),minY,maxY,name,type,"",null,Set.of(),"","",0,Map.of(),Map.of(),spawns);
        register(r);save();return v;
    }
    public synchronized boolean delete(UUID id){PixelRegion r=regions.remove(id);if(r==null)return false;removeIndex(r);removeSpawnIndex(r);save();return true;}
    public synchronized Optional<PixelRegion> find(Location location){if(location==null||location.getWorld()==null)return Optional.empty();return find(location.getWorld(),location.getX(),location.getBlockY(),location.getZ());}
    public synchronized Optional<PixelRegion> find(World world,double x,int y,double z){
        if(world==null)return Optional.empty();List<UUID> candidates=index.getOrDefault(new ChunkKey(world.getName(),chunk(x),chunk(z)),List.of());
        Optional<PixelRegion> match=candidates.stream().map(regions::get).filter(Objects::nonNull).filter(r->r.contains(x,y,z))
                .max(Comparator.comparingInt(r -> r.priority()).thenComparing(r -> r.id()));
        return match.isPresent()?match:Optional.of(globalRegion(world.getName()));
    }
    public synchronized PixelRegion globalRegion(String world){return globals.computeIfAbsent(world,w->PixelRegion.global(w,defaultFlags()));}
    public synchronized boolean hasFlag(Location location,RegionFlag flag){if(location==null||location.getWorld()==null||flag==null)return true;Optional<PixelRegion> local=findLocal(location);return local.map(r->r.hasFlag(flag)?r.flag(flag):globalRegion(r.worldName()).flag(flag)).orElse(globalRegion(location.getWorld().getName()).flag(flag));}
    public synchronized boolean isExplicitSpawnPoint(Location location,String mobType){
        if(location==null||location.getWorld()==null||mobType==null)return false;String n=SpawnMobType.normalize(mobType);
        return spawnIndex.getOrDefault(new ChunkKey(location.getWorld().getName(),chunk(location.getX()),chunk(location.getZ())),List.of()).stream()
                .map(ref -> ref.point()).filter(p->p.mobType().equals(n)).anyMatch(p->Math.abs(p.x()-location.getX())<.01&&Math.abs(p.y()-location.getY())<.01&&Math.abs(p.z()-location.getZ())<.01);
    }
    public synchronized List<SpawnPointRef> spawnPointsNear(Location location,int radiusChunks){
        if(location==null||location.getWorld()==null||radiusChunks<0)return List.of();int cx=chunk(location.getX()),cz=chunk(location.getZ());List<SpawnPointRef> out=new ArrayList<>();
        for(int x=cx-radiusChunks;x<=cx+radiusChunks;x++)for(int z=cz-radiusChunks;z<=cz+radiusChunks;z++)out.addAll(spawnIndex.getOrDefault(new ChunkKey(location.getWorld().getName(),x,z),List.of()));return List.copyOf(out);
    }
    public synchronized boolean setOwner(UUID id,UUID owner){PixelRegion r=regions.get(id);if(r==null)return false;if(owner==null)r.clearOwner();else r.setOwner(owner);save();return true;}
    public synchronized boolean addMember(UUID id,UUID member){PixelRegion r=regions.get(id);if(r==null||!r.addMember(member))return false;save();return true;}
    public synchronized boolean removeMember(UUID id,UUID member){PixelRegion r=regions.get(id);if(r==null||!r.removeMember(member))return false;save();return true;}
    public synchronized void setGlobalFlag(String world,RegionFlag flag,boolean value){PixelRegion r=globalRegion(world);r.setFlag(flag,value);saveGlobals();}
    public synchronized void save(){if(closing)return;enqueue(snapshot());}
    public synchronized void saveGlobals(){if(closing)return;enqueue(new RegionRepository.Snapshot(List.copyOf(regions.values()),Map.copyOf(globals)));}
    private void enqueue(RegionRepository.Snapshot snapshot){persistence=persistence.handle((v,e)->null).thenCompose(v->repository.saveAsync(snapshot)).exceptionally(e->{plugin.getLogger().log(Level.SEVERE,"Region persistence failed",e);return null;});}
    private RegionRepository.Snapshot snapshot(){List<PixelRegion> normal=regions.values().stream().map(RegionManager::copy).toList();Map<String,PixelRegion> global=globals.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> entry.getKey(),e->copy(e.getValue())));return new RegionRepository.Snapshot(normal,global);}
    private Optional<PixelRegion> findLocal(Location l){List<UUID> ids=index.getOrDefault(new ChunkKey(l.getWorld().getName(),chunk(l.getX()),chunk(l.getZ())),List.of());return ids.stream().map(regions::get).filter(Objects::nonNull).filter(r->r.contains(l.getX(),l.getBlockY(),l.getZ())).max(Comparator.comparingInt(PixelRegion::priority).thenComparing(PixelRegion::id));}
    private void register(PixelRegion r){regions.put(r.id(),r);addIndex(r);addSpawnIndex(r);}
    private void addIndex(PixelRegion r){for(int x=chunk(r.geometry().minX());x<=chunk(r.geometry().maxX());x++)for(int z=chunk(r.geometry().minZ());z<=chunk(r.geometry().maxZ());z++){ChunkKey k=new ChunkKey(r.worldName(),x,z);List<UUID> u=new ArrayList<>(index.getOrDefault(k,List.of()));u.add(r.id());index.put(k,List.copyOf(u));}}
    private void addSpawnIndex(PixelRegion r){for(int i=0;i<r.spawnPoints().size();i++){RegionSpawnPoint p=r.spawnPoints().get(i);ChunkKey k=new ChunkKey(p.worldName(),chunk(p.x()),chunk(p.z()));List<SpawnPointRef> u=new ArrayList<>(spawnIndex.getOrDefault(k,List.of()));u.add(new SpawnPointRef(r.id(),i,p));spawnIndex.put(k,List.copyOf(u));}}
    private void removeIndex(PixelRegion r){for(int x=chunk(r.geometry().minX());x<=chunk(r.geometry().maxX());x++)for(int z=chunk(r.geometry().minZ());z<=chunk(r.geometry().maxZ());z++){ChunkKey k=new ChunkKey(r.worldName(),x,z);List<UUID> u=index.getOrDefault(k,List.of()).stream().filter(id->!id.equals(r.id())).toList();if(u.isEmpty())index.remove(k);else index.put(k,u);}}
    private void removeSpawnIndex(PixelRegion r){for(RegionSpawnPoint p:r.spawnPoints()){ChunkKey k=new ChunkKey(p.worldName(),chunk(p.x()),chunk(p.z()));List<SpawnPointRef> u=spawnIndex.getOrDefault(k,List.of()).stream().filter(s->!s.regionId().equals(r.id())).toList();if(u.isEmpty())spawnIndex.remove(k);else spawnIndex.put(k,u);}}
    private static PixelRegion copy(PixelRegion r){return new PixelRegion(r.id(),r.worldName(),r.geometry(),r.minY(),r.maxY(),r.name(),r.type(),r.description(),r.ownerId(),r.members(),r.enterMessage(),r.leaveMessage(),r.priority(),r.flags(),r.properties(),r.spawnPoints());}
    private static int chunk(double v){return Math.floorDiv((int)Math.floor(v),16);}
    private static Map<RegionFlag,Boolean> defaultFlags(){EnumMap<RegionFlag,Boolean> f=new EnumMap<>(RegionFlag.class);for(RegionFlag x:RegionFlag.values())f.put(x,true);for(RegionFlag x:List.of(RegionFlag.FIRE_SPREAD,RegionFlag.LAVA_FLOW,RegionFlag.EXPLOSION,RegionFlag.TNT,RegionFlag.CREEPER_EXPLOSION,RegionFlag.GHAST_FIREBALL,RegionFlag.ENDERMAN_GRIEF,RegionFlag.DENY_SPAWN))f.put(x,false);return f;}
    @Override public synchronized void close(){if(closing)return;closing=true;try{persistence.get(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(ExecutionException|TimeoutException e){plugin.getLogger().log(Level.SEVERE,"Timed out waiting for region persistence.",e);}regions.clear();globals.clear();index.clear();spawnIndex.clear();}
    private record ChunkKey(String world,int x,int z){}
    public record SpawnPointRef(UUID regionId,int index,RegionSpawnPoint point){}
}
