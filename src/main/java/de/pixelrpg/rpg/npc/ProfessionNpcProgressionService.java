package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.guild.Guild;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent profession-NPC rank progression, specialization allocation and Grandmaster caps. */
public final class ProfessionNpcProgressionService {
    private final PixelRPGPlugin plugin; private final GuildManager guilds; private final NpcManager npcs; private final File file;
    private final Map<String,Map<Material,Integer>> delivered=new HashMap<>();
    private final MeisterbriefService meisterbrief;
    private final GrandmasterRegistry grandmasters;
    public ProfessionNpcProgressionService(PixelRPGPlugin plugin,GuildManager guilds,NpcManager npcs,MeisterbriefService meisterbrief){this.plugin=plugin;this.guilds=guilds;this.npcs=npcs;this.file=new File(plugin.getDataFolder(),"profession-npc-progress.yml");this.meisterbrief=meisterbrief;this.grandmasters=new GrandmasterRegistry(npcs);load();}
    public int availableSpecializationPoints(UUID guildId){ Guild g=guilds.getGuildById(guildId).orElse(null); if(g==null)return 0; int total=specializationTotal(g.cityLevel()); int used=0; for(RPGNpc npc:npcs.getAll()) if(guildId.equals(npc.kingdomId())) used+=npc.professionNpcRank().specializationCost(); return Math.max(0,total-used); }
    public synchronized Result upgrade(Player player,String npcId){
        RPGNpc npc=npcs.getById(npcId).orElse(null); if(npc==null||npc.profession()==null)return Result.INVALID_NPC;
        Guild guild=guilds.getGuild(player.getUniqueId()).orElse(null); if(guild==null||!guild.canManageTerritory(player.getUniqueId()))return Result.NOT_AUTHORIZED;
        if(!guild.id().equals(npc.kingdomId()))return Result.WRONG_KINGDOM;
        ProfessionNpcRank next=npc.professionNpcRank().next(); if(next==null)return Result.MAX_RANK;
        if(next.ordinal()+1>guild.cityLevel())return Result.CITY_LEVEL;
        if(!hasInfrastructure(npc))return Result.INFRASTRUCTURE;
        if(npc.profession().isMain() && next.specializationCost()>availableSpecializationPoints(guild.id())+npc.professionNpcRank().specializationCost())return Result.SPECIALIZATION;
        if(next.isGrandmaster()&&npc.profession().isMain()&&grandmasters.count(npc.profession())>=plugin.getConfig().getInt("kingdom.grandmaster.main-profession-global-cap",2))return Result.GRANDMASTER_CAP;
        double gold=Math.max(0.0D, rankConfig(next,npc.profession()).getDouble("gold", plugin.getConfig().getDouble("kingdom.profession-npc.ranks."+next.name()+".gold", 0.0D)));
        ConfigurationSection req=rankConfig(next,npc.profession()).getConfigurationSection("materials");
        Map<Material,Integer> progress=delivered.getOrDefault(npc.id(),Map.of());
        if(req!=null)for(String key:req.getKeys(false)){Material m=Material.matchMaterial(key);int amount=req.getInt(key);if(m==null||progress.getOrDefault(m,0)<amount)return Result.MATERIALS;}
        if(npc.profession().isGathering() && next.isGrandmaster() && !meisterbrief.hasSigned(player,npc.id())) {
            if(!meisterbrief.hasAnyBrief(player,npc.id())) {
                player.getInventory().addItem(meisterbrief.issue(npc.id())).values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(),stack));
            }
            return Result.MEISTERBRIEF_ISSUED;
        }
        if(!guilds.chargeTreasury(guild.id(),gold))return Result.GOLD;
        if(npc.profession().isGathering() && next.isGrandmaster() && !meisterbrief.consumeSigned(player,npc.id()))return Result.MEISTERBRIEF_REQUIRED;
        if(next.isGrandmaster()&&npc.profession().isMain()) {
            int cap=plugin.getConfig().getInt("kingdom.grandmaster.main-profession-global-cap",2);
            boolean promoted=grandmasters.promote(npc,cap,()->npcs.setProfessionNpcRank(npc.id(),next));
            if(!promoted)return Result.GRANDMASTER_CAP;
        } else {
            npcs.setProfessionNpcRank(npc.id(),next);
        }
        delivered.remove(npc.id()); save(); return Result.SUCCESS;
    }
    public synchronized boolean contribute(Player player,String npcId,Material material,int amount){
        RPGNpc npc=npcs.getById(npcId).orElse(null); Guild g=guilds.getGuild(player.getUniqueId()).orElse(null); if(npc==null||g==null||!g.id().equals(npc.kingdomId())||material==null||amount<=0)return false;
        ConfigurationSection rank=rankConfig(npc.professionNpcRank().next(),npc.profession()); int required=rank.getInt("materials."+material.name(),0); if(required<=0)return false;
        Map<Material,Integer> map=delivered.computeIfAbsent(npcId,k->new HashMap<>());
        int already=map.getOrDefault(material,0);
        int accepted=Math.min(amount,Math.max(0,required-already));
        if(accepted<=0||!player.getInventory().contains(material,accepted))return false;
        map.put(material,already+accepted);
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material,accepted));
        save(); return true;
    }
    private boolean hasInfrastructure(RPGNpc npc) {
        if(npc.location()==null||npc.location().getWorld()==null)return false;
        int radius=6; var world=npc.location().getWorld(); int bx=npc.location().getBlockX(), by=npc.location().getBlockY(), bz=npc.location().getBlockZ();
        java.util.Set<Material> required=switch(npc.profession()){
            case BLACKSMITH->java.util.Set.of(Material.ANVIL,Material.FURNACE);
            case COOK->java.util.Set.of(Material.SMOKER,Material.CAMPFIRE);
            case TAILOR->java.util.Set.of(Material.LOOM);
            case ALCHEMIST->java.util.Set.of(Material.BREWING_STAND);
            case MASON->java.util.Set.of(Material.STONECUTTER);
            case SCHOLAR->java.util.Set.of(Material.LECTERN,Material.BOOKSHELF);
            case FARMER->java.util.Set.of(Material.FARMLAND);
            case FISHERMAN->java.util.Set.of(Material.WATER,Material.BARREL);
            case WOODCUTTER->java.util.Set.of(Material.CRAFTING_TABLE,Material.OAK_LOG);
            case MOUNTAIN_MINER->java.util.Set.of(Material.CRAFTING_TABLE,Material.STONE);
        };
        java.util.Set<Material> found=java.util.EnumSet.noneOf(Material.class);
        for(int dx=-radius;dx<=radius;dx++)for(int dy=-3;dy<=3;dy++)for(int dz=-radius;dz<=radius;dz++){Material m=world.getBlockAt(bx+dx,by+dy,bz+dz).getType();if(required.contains(m))found.add(m);if(found.containsAll(required))return true;}
        return found.containsAll(required);
    }

    private ConfigurationSection rankConfig(ProfessionNpcRank rank, Profession profession) {
        String specific="kingdom.profession-npc.requirements."+profession.name()+"."+rank.name();
        ConfigurationSection section=plugin.getConfig().getConfigurationSection(specific);
        if(section!=null)return section;
        ConfigurationSection generic=plugin.getConfig().getConfigurationSection("kingdom.profession-npc.ranks."+rank.name());
        if(generic==null)throw new IllegalStateException("Missing profession NPC rank configuration for "+rank);
        return generic;
    }

    public synchronized ProgressView progress(String npcId) {
        RPGNpc npc = npcs.getById(npcId).orElse(null);
        if (npc == null || npc.profession() == null) return null;
        ProfessionNpcRank next = npc.professionNpcRank().next();
        if (next == null) return new ProgressView(npc.professionNpcRank(), null, 0.0D, Map.of(), Map.of(), 0, false);
        ConfigurationSection rank = rankConfig(next, npc.profession());
        Map<Material, Integer> required = new java.util.LinkedHashMap<>();
        ConfigurationSection materials = rank.getConfigurationSection("materials");
        if (materials != null) {
            for (String key : materials.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material != null) required.put(material, Math.max(0, materials.getInt(key)));
            }
        }
        Map<Material, Integer> current = delivered.getOrDefault(npc.id(), Map.of());
        Map<Material, Integer> deliveredCopy = new java.util.LinkedHashMap<>();
        required.forEach((material, amount) -> deliveredCopy.put(material, Math.min(amount, current.getOrDefault(material, 0))));
        int available = npc.kingdomId() == null ? 0 : availableSpecializationPoints(npc.kingdomId());
        int cap = plugin.getConfig().getInt("kingdom.grandmaster.main-profession-global-cap", 2);
        int global = npc.profession().isMain() ? globalGrandmasters(npc.profession()) : 0;
        double gold = Math.max(0.0D, rank.getDouble("gold", 0.0D));
        boolean capReached = next.isGrandmaster() && npc.profession().isMain() && global >= cap;
        return new ProgressView(npc.professionNpcRank(), next, gold, Map.copyOf(required), deliveredCopy, available, capReached);
    }

    public synchronized int repairSpecializationAllocations(UUID guildId) {
        Guild guild = guilds.getGuildById(guildId).orElse(null);
        if (guild == null) return 0;
        int total = specializationTotal(guild.cityLevel());
        java.util.List<RPGNpc> ranked = npcs.getAll().stream()
                .filter(npc -> guildId.equals(npc.kingdomId()) && npc.profession() != null && npc.profession().isMain())
                .sorted(java.util.Comparator.comparingInt((RPGNpc npc) -> npc.professionNpcRank().specializationCost()).reversed()
                        .thenComparing(RPGNpc::id))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        int used = ranked.stream().mapToInt(npc -> npc.professionNpcRank().specializationCost()).sum();
        int repaired = 0;
        for (RPGNpc npc : ranked) {
            if (used <= total) break;
            ProfessionNpcRank rank = npc.professionNpcRank();
            if (rank == ProfessionNpcRank.APPRENTICE) continue;
            int before = rank.specializationCost();
            ProfessionNpcRank downgraded = ProfessionNpcRank.values()[rank.ordinal() - 1];
            npcs.setProfessionNpcRank(npc.id(), downgraded);
            used -= before - downgraded.specializationCost();
            repaired++;
        }
        if (repaired > 0) save();
        return repaired;
    }

    public record ProgressView(ProfessionNpcRank currentRank, ProfessionNpcRank nextRank, double gold,
                               Map<Material, Integer> requiredMaterials, Map<Material, Integer> deliveredMaterials,
                               int availableSpecializationPoints, boolean globalGrandmasterCapReached) { }

    public int globalGrandmasters(Profession profession){return grandmasters.count(profession);}
    private int specializationTotal(int city){return switch(Math.clamp(city,1,10)){case 1->0;case 2->1;case 3->3;case 4->5;default->8;};}
    private void load(){if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);ConfigurationSection r=y.getConfigurationSection("npcs");if(r==null)return;for(String id:r.getKeys(false)){ConfigurationSection s=r.getConfigurationSection(id+".materials");if(s==null)continue;Map<Material,Integer> m=new HashMap<>();for(String k:s.getKeys(false)){Material mat=Material.matchMaterial(k);if(mat!=null)m.put(mat,s.getInt(k));}delivered.put(id,m);}}
    private void save(){YamlConfiguration y=new YamlConfiguration();delivered.forEach((id,m)->m.forEach((mat,n)->y.set("npcs."+id+".materials."+mat.name(),n)));try{y.save(file);}catch(IOException e){plugin.getLogger().warning("Could not save profession NPC progress: "+e.getMessage());}}
    public enum Result{SUCCESS,INVALID_NPC,NOT_AUTHORIZED,WRONG_KINGDOM,MAX_RANK,CITY_LEVEL,INFRASTRUCTURE,SPECIALIZATION,GOLD,MATERIALS,GRANDMASTER_CAP,MEISTERBRIEF_ISSUED,MEISTERBRIEF_REQUIRED}
}
