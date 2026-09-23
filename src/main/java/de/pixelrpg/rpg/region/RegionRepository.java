package de.pixelrpg.rpg.region;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/** Asynchronous YAML persistence for region definitions and world-global policies. */
public final class RegionRepository implements AutoCloseable {
    private static final int FORMAT_VERSION = 8;
    private final File file; private final Logger logger; private final Executor ioExecutor;
    private volatile boolean migrationNeeded;
    public RegionRepository(File dataFolder, Logger logger, Executor ioExecutor) {
        Objects.requireNonNull(dataFolder); this.logger=Objects.requireNonNull(logger); this.ioExecutor=Objects.requireNonNull(ioExecutor);
        if(!dataFolder.exists()&&!dataFolder.mkdirs())throw new IllegalStateException("Could not create plugin data directory");
        this.file=new File(dataFolder,"regions.yml");
    }
    public CompletableFuture<Snapshot> loadAsync(){return CompletableFuture.supplyAsync(this::readSnapshot,ioExecutor);}
    public CompletableFuture<Void> saveAsync(Snapshot snapshot){return CompletableFuture.runAsync(()->writeSnapshot(snapshot),ioExecutor);}
    public boolean consumeMigrationNeeded(){if(!migrationNeeded)return false;migrationNeeded=false;return true;}

    private Snapshot readSnapshot(){
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file); int version=yaml.getInt("format-version",1);
        List<PixelRegion> regions=new ArrayList<>(); ConfigurationSection root=yaml.getConfigurationSection("regions");
        if(root!=null)for(String idText:root.getKeys(false))try{
            String b="regions."+idText; UUID id=UUID.fromString(idText); String world=root.getString(idText+".world");
            List<RegionPoint> points=new ArrayList<>(); for(Map<?,?> e:root.getMapList(idText+".points"))points.add(new RegionPoint(number(e.get("x")),number(e.get("z"))));
            var validation=RegionGeometry.validate(points); if(world==null||!validation.valid()){logger.warning("Skipping invalid region "+idText);continue;}
            EnumMap<RegionFlag,Boolean> flags=readFlags(yaml,b+".flags"); Map<String,String> props=readProperties(yaml.getConfigurationSection(b+".properties"));
            List<RegionSpawnPoint> spawns=new ArrayList<>(); for(Map<?,?> e:yaml.getMapList(b+".spawn-points"))try{
                if(e.get("mob")!=null)spawns.add(new RegionSpawnPoint(String.valueOf(e.get("mob")),world,number(e.get("x")),number(e.get("y")),number(e.get("z"))));
            }catch(IllegalArgumentException ignored){}
            UUID owner=parseUuid(root.getString(idText+".owner")); Set<UUID> members=new HashSet<>(); for(String m:yaml.getStringList(b+".members")){UUID u=parseUuid(m);if(u!=null)members.add(u);} if(owner!=null)members.remove(owner);
            regions.add(new PixelRegion(id,world,validation.geometry(),root.getInt(idText+".min-y"),root.getInt(idText+".max-y"),root.getString(idText+".name",idText),
                RegionType.parse(root.getString(idText+".type","OTHER")),root.getString(idText+".description",""),owner,members,root.getString(idText+".enter-message",""),
                root.getString(idText+".leave-message",""),root.getInt(idText+".priority",0),flags,props,spawns));
        }catch(Exception ex){logger.warning("Skipping malformed region "+idText+": "+ex.getMessage());}
        if(version<FORMAT_VERSION)migrationNeeded=true;
        return new Snapshot(List.copyOf(regions),readGlobals(yaml));
    }
    private Map<String,PixelRegion> readGlobals(YamlConfiguration yaml){
        ConfigurationSection worlds=yaml.getConfigurationSection("global-regions");if(worlds==null)return Map.of();Map<String,PixelRegion> out=new HashMap<>();
        for(String world:worlds.getKeys(false)){ConfigurationSection s=yaml.getConfigurationSection("global-regions."+world);EnumMap<RegionFlag,Boolean> f=defaultGlobalFlags();
            if(s!=null){ConfigurationSection fs=s.getConfigurationSection("flags");if(fs!=null)for(String k:fs.getKeys(false))try{f.put(RegionFlag.valueOf(k),fs.getBoolean(k));}catch(IllegalArgumentException ignored){}}
            out.put(world,PixelRegion.global(world,f,s==null?"Wildnis":s.getString("name","Wildnis"),s==null?RegionType.OTHER:RegionType.parse(s.getString("type","OTHER")),
                s==null?"Globale Standardregion":s.getString("description","Globale Standardregion"),s==null?"":s.getString("enter-message",""),s==null?"":s.getString("leave-message",""),
                s==null?0:s.getInt("priority",0),s==null?Map.of():readProperties(s.getConfigurationSection("properties"))));
        } return Map.copyOf(out);
    }
    private void writeSnapshot(Snapshot snapshot){
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);yaml.set("format-version",FORMAT_VERSION);yaml.set("regions",null);
        for(PixelRegion r:snapshot.regions()){String b="regions."+r.id();yaml.set(b+".world",r.worldName());yaml.set(b+".name",r.name());yaml.set(b+".type",r.type().name());yaml.set(b+".description",r.description());
            yaml.set(b+".min-y",r.minY());yaml.set(b+".max-y",r.maxY());yaml.set(b+".priority",r.priority());if(r.ownerId()!=null)yaml.set(b+".owner",r.ownerId().toString());
            yaml.set(b+".members",r.members() .stream().map(uuid -> uuid.toString()).sorted().toList());yaml.set(b+".points",r.geometry().points().stream().map(p->Map.of("x",p.x(),"z",p.z())).toList());
            yaml.set(b+".flags",null);for(var e:r.flags().entrySet())yaml.set(b+".flags."+e.getKey().name(),e.getValue());yaml.set(b+".properties",null);for(var e:r.properties().entrySet())yaml.set(b+".properties."+e.getKey(),e.getValue());
            yaml.set(b+".spawn-points",r.spawnPoints().stream().map(p->Map.<String,Object>of("mob",p.mobType(),"x",p.x(),"y",p.y(),"z",p.z())).toList());yaml.set(b+".enter-message",r.enterMessage());yaml.set(b+".leave-message",r.leaveMessage());}
        yaml.set("global-regions",null);for(var e:snapshot.globalRegions().entrySet()){PixelRegion g=e.getValue();String b="global-regions."+e.getKey();yaml.set(b+".name",g.name());yaml.set(b+".type",g.type().name());
            yaml.set(b+".description",g.description());yaml.set(b+".priority",g.priority());yaml.set(b+".enter-message",g.enterMessage());yaml.set(b+".leave-message",g.leaveMessage());yaml.set(b+".flags",null);
            for(var f:g.flags().entrySet())yaml.set(b+".flags."+f.getKey().name(),f.getValue());yaml.set(b+".properties",null);for(var p:g.properties().entrySet())yaml.set(b+".properties."+p.getKey(),p.getValue());}
        writeAtomically(yaml);
    }
    private EnumMap<RegionFlag,Boolean> readFlags(YamlConfiguration y,String path){EnumMap<RegionFlag,Boolean> f=new EnumMap<>(RegionFlag.class);ConfigurationSection s=y.getConfigurationSection(path);if(s!=null)for(String k:s.getKeys(false))try{f.put(RegionFlag.valueOf(k),s.getBoolean(k));}catch(IllegalArgumentException ignored){}return f;}
    private static Map<String,String> readProperties(ConfigurationSection s){if(s==null)return Map.of();Map<String,String> r=new HashMap<>();for(String k:s.getKeys(false))r.put(k,s.getString(k,""));return Map.copyOf(r);}
    private static double number(Object v){if(v instanceof Number n)return n.doubleValue();return Double.parseDouble(String.valueOf(v));}
    private static UUID parseUuid(String v){try{return v==null?null:UUID.fromString(v);}catch(IllegalArgumentException e){return null;}}
    private void writeAtomically(YamlConfiguration y){Path p=file.toPath(),tmp=p.resolveSibling(file.getName()+".tmp");try{y.save(tmp.toFile());try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){try{Files.deleteIfExists(tmp);}catch(IOException ignored){}throw new IllegalStateException("Could not save regions.yml",e);}}
    private static EnumMap<RegionFlag,Boolean> defaultGlobalFlags(){EnumMap<RegionFlag,Boolean> f=new EnumMap<>(RegionFlag.class);
        f.put(RegionFlag.PVP,true);f.put(RegionFlag.MOB_SPAWNING,true);f.put(RegionFlag.DENY_SPAWN,false);f.put(RegionFlag.BLOCK_BREAK,true);f.put(RegionFlag.BLOCK_PLACE,true);f.put(RegionFlag.ENTITY_INTERACTION,true);
        f.put(RegionFlag.DAMAGE_ANIMALS,true);f.put(RegionFlag.ITEM_DROP,true);f.put(RegionFlag.ITEM_PICKUP,true);f.put(RegionFlag.FALL_DAMAGE,true);f.put(RegionFlag.FIRE_SPREAD,false);f.put(RegionFlag.LAVA_FLOW,false);
        f.put(RegionFlag.WATER_FLOW,true);f.put(RegionFlag.EXPLOSION,false);f.put(RegionFlag.TNT,false);f.put(RegionFlag.CREEPER_EXPLOSION,false);f.put(RegionFlag.GHAST_FIREBALL,false);f.put(RegionFlag.ENDERMAN_GRIEF,false);
        f.put(RegionFlag.LIGHTNING,true);f.put(RegionFlag.CROP_GROWTH,true);f.put(RegionFlag.LEAF_DECAY,true);f.put(RegionFlag.BLOCK_TRAMPLING,true);f.put(RegionFlag.ENTRY,true);f.put(RegionFlag.EXIT,true);
        f.put(RegionFlag.RESPAWN_ANCHORS,true);f.put(RegionFlag.SLEEP,true);f.put(RegionFlag.ENDERPEARL,true);f.put(RegionFlag.CHORUS_FRUIT_TELEPORT,true);f.put(RegionFlag.NATURAL_HEALTH_REGEN,true);f.put(RegionFlag.NATURAL_HUNGER_DRAIN,true);
        for(RegionFlag x:RegionFlag.values())f.putIfAbsent(x,true);return f;}
    @Override public void close(){}
    public record Snapshot(List<PixelRegion> regions,Map<String,PixelRegion> globalRegions){}
}
