package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.PixelRPGPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Weekly lightweight activity checks; missed maintenance only creates an admin-review flag. */
public final class KingdomMaintenanceService {
    private final PixelRPGPlugin plugin;
    private final GuildManager guilds;
    private final File file;
    private final Map<UUID, MaintenanceState> states = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask checkTask;

    public KingdomMaintenanceService(PixelRPGPlugin plugin, GuildManager guilds) { this.plugin=plugin; this.guilds=guilds; this.file=new File(plugin.getDataFolder(),"kingdom-maintenance.yml"); load(); start(); }

    public void start(){ if(checkTask!=null)return; checkTask=org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin,()->guilds.allGuilds().forEach(g->state(g.id())),20L,20L*3600L); }
    public void shutdown(){ if(checkTask!=null){checkTask.cancel();checkTask=null;} }

    public synchronized MaintenanceState state(UUID guildId) {
        MaintenanceState state=states.computeIfAbsent(guildId,id->new MaintenanceState(0L,0,false));
        long now=System.currentTimeMillis();
        if (state.deadline()==0L) { long deadline=now+7L*86_400_000L; state=new MaintenanceState(deadline,0,false); states.put(guildId,state); save(); }
        else if (now>state.deadline() && !state.reviewRequired()) { state=new MaintenanceState(state.deadline(),state.contributed(),true); states.put(guildId,state); save(); }
        return state;
    }

    public synchronized boolean contribute(UUID guildId, Material material, int amount) {
        if (guildId==null||material==null||amount<=0) return false;
        int required=plugin.getConfig().getInt("kingdom.maintenance.materials."+material.name(),0);
        if(required<=0) return false;
        MaintenanceState state=state(guildId); if(amount>required-state.contributed())return false; int accepted=amount; if(accepted<=0)return false; states.put(guildId,new MaintenanceState(state.deadline(),state.contributed()+accepted,state.reviewRequired())); save(); return true;
    }

    public synchronized boolean complete(UUID guildId) {
        MaintenanceState state=state(guildId); int required=plugin.getConfig().getInt("kingdom.maintenance.amount",16);
        if(state.contributed()<required) return false; states.put(guildId,new MaintenanceState(System.currentTimeMillis()+7L*86_400_000L,0,false)); save(); return true;
    }

    public synchronized boolean needsReview(UUID guildId){ return state(guildId).reviewRequired(); }
    public synchronized Map<UUID,MaintenanceState> reviewRequired(){ return states.entrySet().stream().filter(e->state(e.getKey()).reviewRequired()).collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,Map.Entry::getValue)); }

    private void load(){ if(!file.exists())return; YamlConfiguration y=YamlConfiguration.loadConfiguration(file); ConfigurationSection r=y.getConfigurationSection("guilds"); if(r==null)return; for(String k:r.getKeys(false))try{UUID id=UUID.fromString(k);states.put(id,new MaintenanceState(r.getLong(k+".deadline"),r.getInt(k+".contributed"),r.getBoolean(k+".review")));}catch(IllegalArgumentException ignored){} }
    private void save(){ YamlConfiguration y=new YamlConfiguration(); states.forEach((id,s)->{String p="guilds."+id;y.set(p+".deadline",s.deadline());y.set(p+".contributed",s.contributed());y.set(p+".review",s.reviewRequired());}); try{y.save(file);}catch(IOException e){plugin.getLogger().warning("Could not save kingdom maintenance: "+e.getMessage());} }
    public record MaintenanceState(long deadline,int contributed,boolean reviewRequired){}
}
