package de.pixelrpg.rpg.npc.behavior;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.StoryNpcDialogue;
import de.pixelrpg.rpg.npc.NpcBehavior;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class StoryBehavior implements NpcBehavior {
    private final StoryManager storyManager;
    private final StoryNpcDialogue storyDialogue;
    private final DialogueEngine dialogueEngine;
    private final PlayerProfileManager profileManager;

    public StoryBehavior(StoryManager storyManager, StoryNpcDialogue storyDialogue,
                         DialogueEngine dialogueEngine, PlayerProfileManager profileManager) {
        this.storyManager = storyManager;
        this.storyDialogue = storyDialogue;
        this.dialogueEngine = dialogueEngine;
        this.profileManager = profileManager;
    }

    @Override
    public NpcType type() {
        return NpcType.STORY;
    }

    @Override
    public void onInteract(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) return;
        storyManager.getChapter(profile.getStoryChapterIndex() + 1).ifPresentOrElse(chapter -> {
            if (profile.getLevel() < chapter.requiredLevel()) {
                dialogueEngine.openUnavailable(player, chapter.title(),
                        "Diese Spur wird erst ab Level " + chapter.requiredLevel() + " zugänglich.");
                return;
            }
            if (chapter.hasStructureTrigger() && !npc.id().startsWith("story_" + chapter.npcId().toLowerCase() + "_")) return;
            storyDialogue.openChapter(player, chapter, npc);
        }, () -> dialogueEngine.openNotice(player,
                Component.text("Das Archiv schweigt", NamedTextColor.GOLD),
                Component.text("Für deine aktuelle Reise ist keine weitere Hauptspur registriert.", NamedTextColor.WHITE),
                Component.text("Schließen", NamedTextColor.GRAY)));
    }
}
