package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Optional;

public final class StoryBehavior implements NpcBehavior {
    private final StoryManager story; private final PlayerProfileManager profiles; private final DialogueEngine dialogue;
    public StoryBehavior(StoryManager story,PlayerProfileManager profiles,DialogueEngine dialogue){this.story=story;this.profiles=profiles;this.dialogue=dialogue;}
    @Override public NpcType type(){return NpcType.STORY;}
    @Override public void onInteract(Player player,RPGNpc npc){
        if(!profiles.isRegistered(player.getUniqueId())){dialogue.openNotice(player,Component.text("Geschichte",NamedTextColor.GOLD),Component.text("Du musst zuerst registriertes Rathausmitglied sein.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.GRAY));return;}
        Optional<StoryChapter> next=story.getNextChapterFor(player.getUniqueId());
        if(next.isEmpty()){dialogue.openNotice(player,Component.text("Geschichte",NamedTextColor.GOLD),Component.text("Für dich gibt es momentan kein neues Kapitel. Kehre später zurück.",NamedTextColor.WHITE),Component.text("Schließen",NamedTextColor.GRAY));return;}
        StoryChapter chapter=next.get();
        dialogue.openMultiAction(player,Component.text("Geschichte",NamedTextColor.GOLD),List.of(DialogBody.plainMessage(Component.text(chapter.title(),NamedTextColor.YELLOW))),List.of(dialogue.actionButton(Component.text("Kapitel lesen"),NamedTextColor.GREEN,target->{target.openBook(StoryBookFactory.build(chapter));story.completeChapter(target,chapter);})),1);
    }
}
