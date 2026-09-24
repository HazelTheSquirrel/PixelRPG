package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TravelDialog {
    private final NpcManager npcs; private final PlayerProfileManager profiles; private final DialogueEngine dialogue;
    public TravelDialog(NpcManager npcs,PlayerProfileManager profiles,DialogueEngine dialogue){this.npcs=npcs;this.profiles=profiles;this.dialogue=dialogue;}
    public void open(Player player,String currentNpcId){
        PlayerProfile profile=profiles.getProfile(player.getUniqueId()).orElse(null);
        if(profile==null){dialogue.openUnavailable(player,"Reisen","Dein Spielerprofil konnte nicht geladen werden.");return;}
        List<RPGNpc> destinations=npcs.getAll().stream().filter(npc->npc.type()==NpcType.TRAVEL).filter(npc->currentNpcId==null||!currentNpcId.equals(npc.id())).filter(npc->profile.hasUnlockedWaypoint(npc.id())).toList();
        if(destinations.isEmpty()){dialogue.openNotice(player,Component.text("Reisen",NamedTextColor.GOLD),Component.text("Du hast noch keinen weiteren Reisepunkt freigeschaltet.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.GRAY));return;}
        List<ActionButton> actions=new ArrayList<>();
        for(RPGNpc destination:destinations) actions.add(dialogue.actionButton(Component.text(destination.name()),NamedTextColor.GREEN,target->teleport(target,destination)));
        dialogue.openMultiAction(player,Component.text("Reisen",NamedTextColor.GOLD),List.of(DialogBody.plainMessage(Component.text("Wähle einen freigeschalteten Reisepunkt.",NamedTextColor.WHITE))),actions,2);
    }
    private void teleport(Player player,RPGNpc destination){
        Location arrival=findSafeArrival(destination.location());
        if(arrival==null){player.sendMessage(Component.text("Für diesen Reisepunkt wurde keine sichere Ankunftsposition gefunden.",NamedTextColor.RED));return;}
        player.teleportAsync(arrival).thenAccept(success->{if(!success)return;JavaPlugin plugin=(JavaPlugin)player.getServer().getPluginManager().getPlugin("PixelRPG");if(plugin!=null)Bukkit.getScheduler().runTask(plugin,()->{player.closeDialog();player.playSound(player.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,1.0f,1.0f);});});
    }
    private Location findSafeArrival(Location npcLocation){
        World world=npcLocation.getWorld();if(world==null)return null;List<Location> candidates=new ArrayList<>();
        for(int distance=3;distance<=8;distance++){int samples=Math.max(16,distance*12);for(int index=0;index<samples;index++){double angle=Math.PI*2.0*index/samples;candidates.add(new Location(world,npcLocation.getX()+Math.cos(angle)*distance,npcLocation.getY(),npcLocation.getZ()+Math.sin(angle)*distance));}}
        return candidates.stream().map(candidate->findGround(world,candidate)).filter(java.util.Objects::nonNull).min(Comparator.comparingDouble(candidate->candidate.distanceSquared(npcLocation))).orElse(null);
    }
    private Location findGround(World world,Location horizontal){
        int center=horizontal.getBlockY();
        for(int y=center;y>=Math.max(world.getMinHeight(),center-6);y--){Location candidate=new Location(world,horizontal.getX(),y,horizontal.getZ());if(safe(candidate))return candidate;}
        for(int y=center+1;y<=Math.min(world.getMaxHeight()-2,center+6);y++){Location candidate=new Location(world,horizontal.getX(),y,horizontal.getZ());if(safe(candidate))return candidate;}
        return null;
    }
    private boolean safe(Location location){
        World world=location.getWorld();if(world==null||location.getBlockY()<=world.getMinHeight())return false;
        Block ground=world.getBlockAt(location.getBlockX(),location.getBlockY()-1,location.getBlockZ()),feet=world.getBlockAt(location.getBlockX(),location.getBlockY(),location.getBlockZ()),head=world.getBlockAt(location.getBlockX(),location.getBlockY()+1,location.getBlockZ());
        if(!ground.getType().isSolid()||!feet.isPassable()||!head.isPassable())return false;
        return !switch(ground.getType()){case LAVA,FIRE,SOUL_FIRE,CAMPFIRE,SOUL_CAMPFIRE,MAGMA_BLOCK,CACTUS,SWEET_BERRY_BUSH,POWDER_SNOW,POINTED_DRIPSTONE,WITHER_ROSE->true;default->false;};
    }
}
