package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Event-driven companion runtime; each active owner schedules only its next required runtime pass. */
public final class CompanionRuntimeListener implements Listener {
    private static final long WAKE_TICKS = 2L;
    private final Plugin plugin;
    private final CompanionService service;
    private final RPGKeys keys;
    private final Set<UUID> scheduled = new HashSet<>();
    private final Map<UUID, Long> lastAttack = new HashMap<>();

    CompanionRuntimeListener(Plugin plugin, CompanionService service) { this.plugin=plugin; this.service=service; this.keys=new RPGKeys(plugin); }

    public void wake(UUID ownerId) {
        if (ownerId == null || !scheduled.add(ownerId)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            scheduled.remove(ownerId);
            tick(ownerId);
        }, 1L);
    }

    // Restores a persisted active companion after the player has joined the world.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) { service.restoreActive(event.getPlayer()); }

    // Re-evaluates follow state only for the player whose block position changed.
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo()==null) return;
        if (event.getFrom().getWorld()!=event.getTo().getWorld()
                || event.getFrom().getBlockX()!=event.getTo().getBlockX()
                || event.getFrom().getBlockY()!=event.getTo().getBlockY()
                || event.getFrom().getBlockZ()!=event.getTo().getBlockZ()) wake(event.getPlayer().getUniqueId());
    }

    // Repositions an active companion after a world change.
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { wake(event.getPlayer().getUniqueId()); }

    // Despawns the runtime entity when its owner leaves the server.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) { scheduled.remove(event.getPlayer().getUniqueId()); lastAttack.remove(event.getPlayer().getUniqueId()); service.despawn(event.getPlayer().getUniqueId()); }

    // Clears persistent companion activation when its runtime entity dies.
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        UUID owner=service.getOwnerOfEntity(event.getEntity().getUniqueId());
        if(owner!=null) service.clearActive(owner);
    }

    // Lets the configured tamed wolf react to its owner's direct combat target.
    @EventHandler
    public void onOwnerCombat(EntityDamageByEntityEvent event) {
        Player owner = null;
        LivingEntity target = null;
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            owner = player; target = living;
        } else if (event.getEntity() instanceof Player player && event.getDamager() instanceof LivingEntity living) {
            owner = player; target = living;
        }
        if (owner == null || target == null || target instanceof Player) return;
        UUID entityId=service.getActiveEntity(owner.getUniqueId());
        Entity entity=entityId==null?null:plugin.getServer().getEntity(entityId);
        if (!(entity instanceof Wolf wolf) || !"uncommon-wolf".equalsIgnoreCase(id(wolf))) return;
        if (wolf.getWorld()!=owner.getWorld() || target.getWorld()!=owner.getWorld()) return;
        wolf.setTarget(target);
        wake(owner.getUniqueId());
    }

    public void shutdown() { scheduled.clear(); lastAttack.clear(); }

    private void tick(UUID ownerId) {
        Player owner=plugin.getServer().getPlayer(ownerId);
        UUID entityId=service.getActiveEntity(ownerId);
        if(owner==null||entityId==null) return;
        Entity entity=plugin.getServer().getEntity(entityId);
        if(!(entity instanceof LivingEntity living)||!living.isValid()){service.despawn(ownerId);return;}
        Companion companion=service.getActive(ownerId);
        if(companion==null){service.despawn(ownerId);return;}
        CompanionDefinition definition=service.definition(companion.id());
        applyStats(definition, companion, living);
        if(definition.combat().enabled()) combat(owner,living,definition);
        follow(owner,living,definition.follow());
        if(shouldWake(owner,living,definition)) wake(ownerId);
    }

    private void combat(Player owner, LivingEntity companion, CompanionDefinition definition) {
        if(companion instanceof Wolf wolf && wolf.getTarget()!=null && validTarget(owner,wolf.getTarget(),definition)) {
            attackIfReady(owner,wolf,wolf.getTarget(),definition);
            return;
        }
        if(!definition.combat().enabled()) return;
        LivingEntity target=companion.getWorld().getNearbyEntities(companion.getLocation(),definition.combat().aggroRange(),definition.combat().aggroRange(),definition.combat().aggroRange()).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(candidate->validTarget(owner,candidate,definition)).min(java.util.Comparator.comparingDouble(candidate->candidate.getLocation().distanceSquared(companion.getLocation()))).orElse(null);
        if(target!=null) attackIfReady(owner,companion,target,definition);
    }

    private void attackIfReady(Player owner, LivingEntity entity, LivingEntity target, CompanionDefinition definition) {
        long now=entity.getWorld().getGameTime();
        long last=lastAttack.getOrDefault(owner.getUniqueId(),Long.MIN_VALUE);
        if(now-last<definition.combat().intervalTicks()) return;
        if(entity.getLocation().distanceSquared(target.getLocation())>definition.combat().attackRange()*definition.combat().attackRange()) return;
        AttributeInstance attack=entity.getAttribute(Attribute.ATTACK_DAMAGE);
        double damage=attack==null?1.0D:Math.max(0.0D,attack.getValue());
        target.damage(damage, entity);
        lastAttack.put(owner.getUniqueId(),now);
    }

    private boolean validTarget(Player owner, LivingEntity target, CompanionDefinition definition) {
        if(target==owner||target.isDead()||!target.isValid()||target.getWorld()!=owner.getWorld()) return false;
        if(target instanceof Player) return definition.combat().playerTargets() && !target.getUniqueId().equals(owner.getUniqueId());
        return definition.combat().hostileTargets() || definition.combat().friendlyTargets();
    }

    private void follow(Player owner, LivingEntity companion, CompanionDefinition.Follow follow) {
        if(!follow.enabled() || companion.getPassengers().contains(owner)) return;
        if(companion.getWorld()!=owner.getWorld()){companion.teleport(owner.getLocation().clone().add(1,0,1));return;}
        double distance=companion.getLocation().distanceSquared(owner.getLocation());
        if(distance>=follow.teleportDistance()*follow.teleportDistance()){companion.teleport(owner.getLocation().clone().add(1,0,1));companion.setVelocity(new Vector());return;}
        if(distance<=follow.startDistance()*follow.startDistance()){slow(companion);return;}
        Location target=owner.getLocation().clone().add(owner.getLocation().getDirection().setY(0).normalize().multiply(-Math.max(1.0,follow.stopDistance())));
        Vector delta=target.toVector().subtract(companion.getLocation().toVector());
        if(delta.lengthSquared()<0.04){slow(companion);return;}
        delta.normalize().multiply(Math.max(.05,follow.speed()));
        if(companion.isOnGround()&&target.getY()>companion.getY()+.5)delta.setY(.42);
        companion.setVelocity(delta);
    }

    private boolean shouldWake(Player owner, LivingEntity companion, CompanionDefinition definition) {
        double start=Math.max(definition.follow().startDistance(),definition.follow().stopDistance());
        return definition.follow().enabled() && companion.getLocation().distanceSquared(owner.getLocation())>start*start;
    }

    private static void slow(LivingEntity entity){Vector v=entity.getVelocity();entity.setVelocity(new Vector(v.getX()*.35,v.getY(),v.getZ()*.35));}
    private String id(Entity entity){return entity.getPersistentDataContainer().get(keys.companionId(),PersistentDataType.STRING);}

    private static void applyStats(CompanionDefinition definition, Companion companion, LivingEntity entity) {
        int level=Math.max(1,companion.level()); CompanionDefinition.Progression p=definition.progression(); double rarity=definition.rarity().statMultiplier();
        double healthMultiplier=Math.min(p.healthCap(),rarity*(1+(level-1)*p.healthPerLevel()));
        double damageMultiplier=Math.min(p.damageCap(),rarity*(1+(level-1)*p.damagePerLevel()));
        double speedMultiplier=Math.min(p.speedCap(),rarity*(1+(level-1)*p.speedPerLevel()));
        set(entity,Attribute.MAX_HEALTH,Math.max(1,base(entity,Attribute.MAX_HEALTH,20)*healthMultiplier));
        set(entity,Attribute.ATTACK_DAMAGE,Math.max(0,base(entity,Attribute.ATTACK_DAMAGE,1)*damageMultiplier));
        set(entity,Attribute.MOVEMENT_SPEED,Math.max(.01,base(entity,Attribute.MOVEMENT_SPEED,.3)*speedMultiplier));
        AttributeInstance health=entity.getAttribute(Attribute.MAX_HEALTH);if(health!=null)entity.setHealth(Math.min(entity.getHealth(),health.getValue()));
    }
    private static double base(LivingEntity e,Attribute a,double fallback){AttributeInstance i=e.getAttribute(a);return i==null?fallback:i.getBaseValue();}
    private static void set(LivingEntity e,Attribute a,double value){AttributeInstance i=e.getAttribute(a);if(i!=null&&Double.isFinite(value))i.setBaseValue(value);}
}
