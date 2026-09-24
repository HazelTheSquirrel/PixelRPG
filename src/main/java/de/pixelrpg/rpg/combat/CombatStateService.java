package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.api.events.PlayerCombatEnterEvent;
import de.pixelrpg.rpg.api.events.PlayerCombatExitEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public final class CombatStateService implements Listener, AutoCloseable {
    private static final long TIMEOUT_MILLIS=5000L;
    private final GuildAPI guild;
    private final Map<UUID,Long> combatUntil=new HashMap<>();
    private final Map<UUID,CombatExpiry> byPlayer=new HashMap<>();
    private final TreeSet<CombatExpiry> expiries=new TreeSet<>();
    private final BukkitTask cleanupTask;
    public CombatStateService(Plugin plugin,GuildAPI guild){this.guild=guild;Bukkit.getPluginManager().registerEvents(this,plugin);cleanupTask=Bukkit.getScheduler().runTaskTimer(plugin,this::cleanup,20L,20L);}
    // Zuständig dafür, dass ein Schadensaustausch den beteiligten registrierten Spieler in den Combat State setzt.
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event){Player attacker=resolve(event.getDamager());Player target=event.getEntity() instanceof Player p?p:null;if(attacker!=null)enter(attacker);if(target!=null)enter(target);}
    // Zuständig für die sofortige Bereinigung des Combat State beim Logout.
    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event){exit(event.getPlayer());}
    public boolean isInCombat(UUID uuid){Long expiry=combatUntil.get(uuid);return expiry!=null&&expiry>System.currentTimeMillis();}
    public void enter(Player player){if(!guild.isRegistered(player.getUniqueId()))return;UUID id=player.getUniqueId();boolean active=isInCombat(id);long expiry=System.currentTimeMillis()+TIMEOUT_MILLIS;combatUntil.put(id,expiry);CombatExpiry previous=byPlayer.put(id,new CombatExpiry(id,expiry));if(previous!=null)expiries.remove(previous);expiries.add(byPlayer.get(id));if(!active)Bukkit.getPluginManager().callEvent(new PlayerCombatEnterEvent(player));}
    public void exit(Player player){UUID id=player.getUniqueId();if(combatUntil.remove(id)!=null){CombatExpiry expiry=byPlayer.remove(id);if(expiry!=null)expiries.remove(expiry);Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));}}
    public void shutdown(){cleanupTask.cancel();combatUntil.clear();byPlayer.clear();expiries.clear();}
    @Override public void close(){shutdown();}
    private void cleanup(){long now=System.currentTimeMillis();while(!expiries.isEmpty()){CombatExpiry expiry=expiries.first();if(expiry.expiresAt()>now)return;expiries.pollFirst();if(byPlayer.get(expiry.playerId())!=expiry)continue;byPlayer.remove(expiry.playerId());combatUntil.remove(expiry.playerId());Player player=Bukkit.getPlayer(expiry.playerId());if(player!=null&&player.isOnline())Bukkit.getPluginManager().callEvent(new PlayerCombatExitEvent(player));}}
    private Player resolve(Entity entity){if(entity instanceof Player p)return p;if(entity instanceof Projectile projectile){ProjectileSource source=projectile.getShooter();if(source instanceof Player p)return p;}return null;}
    private record CombatExpiry(UUID playerId,long expiresAt) implements Comparable<CombatExpiry>{public int compareTo(CombatExpiry other){int result=Long.compare(expiresAt,other.expiresAt);return result!=0?result:playerId.compareTo(other.playerId);}}
}
