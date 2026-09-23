package de.pixelrpg.rpg.region;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.*;

/** Native Paper dialog for categorized region-flag inspection and editing. */
public final class RegionFlagDialogService {
    private final RegionManager regions;
    public RegionFlagDialogService(RegionManager regions){this.regions=Objects.requireNonNull(regions);}
    public void open(Player player,PixelRegion region){if(!canEdit(player,region)){player.sendMessage(Component.text("Du darfst die Flags dieser Region nicht bearbeiten.",NamedTextColor.RED));return;}showCategories(player,region);}
    private void showCategories(Player p,PixelRegion r){
        List<ActionButton> actions=new ArrayList<>();
        for(RegionFlagCategory c:RegionFlagCategory.values()){List<RegionFlag> flags=RegionFlag.forCategory(c);if(flags.isEmpty())continue;long enabled=flags.stream().filter(f->effective(r,f)).count();
            actions.add(ActionButton.builder(Component.text(c.displayName(),NamedTextColor.WHITE)).tooltip(Component.text(enabled+"/"+flags.size()+" aktiviert",NamedTextColor.GRAY))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response,audience)->{if(audience instanceof Player target)showCategory(target,r,c);},net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(260).build());}
        p.showDialog(Dialog.create(factory->{DialogRegistryEntry.Builder b=factory.empty();b.base(DialogBase.builder(Component.text("PixelRPG – Region Flags",NamedTextColor.GOLD)).body(List.of(DialogBody.plainMessage(Component.text("Region: "+r.name(),NamedTextColor.GRAY)))).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());b.type(DialogType.multiAction(actions,null,2));}));
    }
    private void showCategory(Player p,PixelRegion r,RegionFlagCategory c){
        PixelRegion current=r.isGlobal()?regions.globalRegion(r.worldName()):regions.get(r.id()).orElse(null);if(current==null||!canEdit(p,current))return;List<ActionButton> actions=new ArrayList<>();
        for(RegionFlag f:RegionFlag.forCategory(c)){boolean explicit=current.isGlobal()||current.hasFlag(f),enabled=effective(current,f);
            actions.add(ActionButton.builder(Component.text((enabled?"✓ ":"✕ ")+f.displayName(),enabled?NamedTextColor.GREEN:NamedTextColor.RED))
                .tooltip(Component.text(explicit?(enabled?"Klicken: deaktivieren":"Klicken: aktivieren"):"Geerbt – klicken setzt eigenes Flag",NamedTextColor.GRAY))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response,audience)->{if(!(audience instanceof Player target))return;PixelRegion refreshed=current.isGlobal()?regions.globalRegion(current.worldName()):regions.get(current.id()).orElse(null);if(refreshed==null||!canEdit(target,refreshed))return;boolean value=effective(refreshed,f);if(refreshed.isGlobal())regions.setGlobalFlag(refreshed.worldName(),f,!value);else{refreshed.setFlag(f,!value);regions.save();}showCategory(target,refreshed,c);},net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(260).build());}
        actions.add(ActionButton.builder(Component.text("← Kategorien",NamedTextColor.YELLOW)).action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response,audience)->{if(audience instanceof Player target)showCategories(target,current);},net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(260).build());
        p.showDialog(Dialog.create(factory->{DialogRegistryEntry.Builder b=factory.empty();b.base(DialogBase.builder(Component.text("Region – "+c.displayName(),NamedTextColor.GOLD)).body(List.of(DialogBody.plainMessage(Component.text("Einzelne Berechtigungen konfigurieren.",NamedTextColor.GRAY)))).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build());b.type(DialogType.multiAction(actions,null,2));}));
    }
    private boolean effective(PixelRegion r,RegionFlag f){return r.isGlobal()||r.hasFlag(f)?r.flag(f):regions.globalRegion(r.worldName()).flag(f);}
    private static boolean canEdit(Player p,PixelRegion r){return p.hasPermission("rpg.admin")||(!r.isGlobal()&&r.isOwner(p.getUniqueId()));}
}
