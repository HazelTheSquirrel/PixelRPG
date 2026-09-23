package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

/** Owns player region transition state and presentation. */
public final class RegionTransitionService {
    private final RegionManager regions; private final Map<UUID,UUID> current=new HashMap<>();
    public RegionTransitionService(RegionManager regions){this.regions=Objects.requireNonNull(regions);}
    public void update(Player player,Location location){if(player==null||location==null||location.getWorld()==null)return;UUID id=player.getUniqueId();UUID next=regions.find(location).map(r -> r.id()).orElse(null);UUID old=current.get(id);if(Objects.equals(old,next))return;PixelRegion oldR=old==null?null:regions.get(old).orElse(null),newR=next==null?null:regions.get(next).orElse(null);String leave=oldR==null?"":oldR.leaveMessage(),enter=newR==null?"":newR.enterMessage();if(!leave.isBlank()||!enter.isBlank())player.showTitle(Title.title(Component.text(enter.isBlank()?leave:enter),Component.text(enter.isBlank()||leave.isBlank()?"":leave)));if(next==null)current.remove(id);else current.put(id,next);}
    public void clear(UUID id){if(id!=null)current.remove(id);}
    public void clearAll(){current.clear();}
}
