package de.pixelrpg.rpg.region;

import java.nio.charset.StandardCharsets;
import java.util.*;
 
/** Region domain object. Runtime mutation is confined to the region service/editor boundary. */
public final class PixelRegion {
    private final UUID id;
    private final String worldName;
    private final RegionGeometry geometry;
    private final boolean global;
    private final int minY, maxY;
    private String name, description, enterMessage, leaveMessage;
    private RegionType type;
    private UUID ownerId;
    private final Set<UUID> members;
    private final EnumMap<RegionFlag, Boolean> flags;
    private final Map<String,String> properties;
    private final List<RegionSpawnPoint> spawnPoints;

    public PixelRegion(UUID id, String worldName, RegionGeometry geometry, int minY, int maxY, String name,
                       RegionType type, String description, UUID ownerId, Set<UUID> members,
                       String enterMessage, String leaveMessage, int priority,
                       Map<RegionFlag,Boolean> flags, Map<String,String> properties, List<RegionSpawnPoint> spawnPoints) {
        if (id == null || worldName == null || worldName.isBlank()) throw new IllegalArgumentException("id/worldName required");
        if (!globalGeometryAllowed(geometry, minY, maxY)) throw new IllegalArgumentException("invalid region geometry");
        this.id=id; this.worldName=worldName; this.geometry=geometry; this.global=geometry==null;
        this.minY=minY; this.maxY=maxY; this.name=name==null||name.isBlank()?id.toString():name;
        this.type=type==null?RegionType.OTHER:type; this.description=description==null?"":description;
        this.ownerId=global?null:ownerId; this.members=new HashSet<>(members==null?Set.of():members);
        if(this.ownerId!=null)this.members.remove(this.ownerId);
        this.enterMessage=enterMessage==null?"":enterMessage; this.leaveMessage=leaveMessage==null?"":leaveMessage;
        this.priority=priority; this.flags=new EnumMap<>(RegionFlag.class); if(flags!=null)this.flags.putAll(flags);
        this.properties=new HashMap<>(properties==null?Map.of():properties);
        this.spawnPoints=new ArrayList<>(spawnPoints==null?List.of():spawnPoints);
    }
    private int priority;
    private static boolean globalGeometryAllowed(RegionGeometry g,int min,int max){ return min<=max && g!=null || g==null && min<=max; }
    public static PixelRegion global(String world, Map<RegionFlag,Boolean> flags){return global(world,flags,"Wildnis",RegionType.OTHER,"Globale Standardregion","","",0,Map.of());}
    public static PixelRegion global(String world, Map<RegionFlag,Boolean> flags,String name,RegionType type,String desc,String enter,String leave,int priority,Map<String,String> props){
        return new PixelRegion(UUID.nameUUIDFromBytes(("pixelrpg:global:"+world).getBytes(StandardCharsets.UTF_8)),world,null,Integer.MIN_VALUE,Integer.MAX_VALUE,name,type,desc,null,Set.of(),enter,leave,priority,flags,props,List.of());
    }
    public UUID id(){return id;} public String worldName(){return worldName;} public RegionGeometry geometry(){return geometry;} public boolean isGlobal(){return global;}
    public int minY(){return minY;} public int maxY(){return maxY;} public String name(){return name;} public RegionType type(){return type;}
    public String description(){return description;} public UUID ownerId(){return ownerId;} public Set<UUID> members(){return Set.copyOf(members);}
    public String enterMessage(){return enterMessage;} public String leaveMessage(){return leaveMessage;} public int priority(){return priority;}
    public Map<RegionFlag,Boolean> flags(){return Map.copyOf(flags);} public Map<String,String> properties(){return Map.copyOf(properties);} public List<RegionSpawnPoint> spawnPoints(){return List.copyOf(spawnPoints);}
    public boolean contains(double x,int y,double z){return global || (y>=minY&&y<=maxY&&geometry.contains(x,z));}
    public boolean hasFlag(RegionFlag flag){return flags.containsKey(flag);}
    public boolean flag(RegionFlag flag){return flags.getOrDefault(flag,defaultFlag(flag));}
    public boolean isOwner(UUID player){return !global&&player!=null&&player.equals(ownerId);}
    public boolean isMember(UUID player){return player!=null&&members.contains(player);}
    public void setFlag(RegionFlag flag,boolean enabled){flags.put(flag,enabled);}
    public void setName(String value){name=value==null?"":value;} public void setType(RegionType value){type=value==null?RegionType.OTHER:value;}
    public void setDescription(String value){description=value==null?"":value;} public void setOwner(UUID id){ownerId=global?null:id;if(ownerId!=null)members.remove(ownerId);}
    public void clearOwner(){ownerId=null;} public boolean addMember(UUID id){return id!=null&&!isOwner(id)&&members.add(id);} public boolean removeMember(UUID id){return id!=null&&members.remove(id);}
    public void setEnterMessage(String v){enterMessage=v==null?"":v;} public void setLeaveMessage(String v){leaveMessage=v==null?"":v;} public void setPriority(int v){priority=v;}
    public void setProperty(String k,String v){if(k!=null&&!k.isBlank())properties.put(k,v==null?"":v);} public void removeProperty(String k){properties.remove(k);}
    public void addSpawnPoint(RegionSpawnPoint p){spawnPoints.add(Objects.requireNonNull(p));}
    private static boolean defaultFlag(RegionFlag flag){return switch(flag){case FIRE_SPREAD,LAVA_FLOW,EXPLOSION,TNT,CREEPER_EXPLOSION,GHAST_FIREBALL,ENDERMAN_GRIEF,DENY_SPAWN->false;default->true;};}
}
