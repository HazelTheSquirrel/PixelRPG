package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/** Native Paper dialog for companion ownership, activation, progression and naming. */
public final class CompanionDialogService {
    private final CompanionService service;
    private final DialogueEngine dialogs;
    public CompanionDialogService(CompanionService service, DialogueEngine dialogs){this.service=service;this.dialogs=dialogs;}

    public void open(Player player){
        List<Companion> companions=service.getCompanions(player.getUniqueId());
        var actions=new java.util.ArrayList<io.papermc.paper.registry.data.dialog.ActionButton>();
        for(Companion companion:companions){
            if(companion.active()){
                actions.add(dialogs.actionButton(Component.text("Wegschicken: ").append(Component.text(companion.name(),NamedTextColor.LIGHT_PURPLE)),NamedTextColor.RED,target->{service.clearActive(target);open(target);}));
            }else{
                actions.add(dialogs.actionButton(Component.text("Rufen: ").append(Component.text(companion.name(),NamedTextColor.LIGHT_PURPLE)),NamedTextColor.GREEN,target->{service.setActive(target,companion.id());open(target);}));
            }
            CompanionDefinition definition=service.definition(companion.id());
            actions.add(dialogs.actionButton(Component.text(companion.name()+" · Level "+companion.level()),NamedTextColor.YELLOW,target->openDetails(target,companion,definition)));
        }
        actions.add(dialogs.actionButton(Component.text("Schließen"),NamedTextColor.WHITE,target -> target.closeDialog()));
        dialogs.openMultiAction(player,Component.text("PixelRPG – Begleiter",NamedTextColor.GOLD),List.of(),actions,2);
    }

    private void openDetails(Player player,Companion companion,CompanionDefinition definition){
        var actions=new java.util.ArrayList<io.papermc.paper.registry.data.dialog.ActionButton>();
        if(definition.renameable()) actions.add(dialogs.actionButton(Component.text("Umbenennen"),NamedTextColor.YELLOW,target->openRename(target,companion)));
        actions.add(dialogs.actionButton(Component.text("Zurück"),NamedTextColor.WHITE,this::open));
        dialogs.openMultiAction(player,Component.text(companion.name(),NamedTextColor.GOLD),
                List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(
                        Component.text("Rarität: "+companion.rarity()+" · Erfahrung: "+companion.experience(),NamedTextColor.WHITE))),
                actions,2);
    }

    private void openRename(Player player,Companion companion){
        var input=io.papermc.paper.registry.data.dialog.input.DialogInput.text("name",220,Component.text("Neuer Name"),true,companion.name(),service.registry().maxNameLength(),null);
        var confirm=io.papermc.paper.registry.data.dialog.ActionButton.builder(Component.text("Speichern",NamedTextColor.GREEN))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((view,audience)->{
                    if(audience instanceof Player target){String name=view.getText("name");service.rename(target.getUniqueId(),companion.id(),name);open(target);}
                },net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())).width(220).build();
        var cancel=dialogs.actionButton(Component.text("Abbrechen"),NamedTextColor.RED,this::open);
        player.showDialog(io.papermc.paper.dialog.Dialog.create(builder->builder.empty()
                .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(Component.text("Begleiter umbenennen",NamedTextColor.GOLD))
                        .body(List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(Component.text("Vergib einen Namen.",NamedTextColor.WHITE))))
                        .inputs(List.of(input)).canCloseWithEscape(true).build())
                .type(io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(List.of(confirm,cancel),null,2))));
    }
}
