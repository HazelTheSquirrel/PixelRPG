package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.dialogue.DialogueProgressStore;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provides a consistent first-visit conversation layer for administrator-placed NPCs.
 * Functional NPC behavior remains the source of truth for the actual service action.
 */
public final class NpcDialogueService implements AutoCloseable {
    private final PlayerProfileManager profiles;
    private final DialogueEngine dialogue;
    private final DialogueProgressStore progress;
    private final NpcBehaviorRegistry behaviors;

    public NpcDialogueService(org.bukkit.plugin.Plugin plugin,
                              PlayerProfileManager profiles,
                              DialogueEngine dialogue,
                              NpcBehaviorRegistry behaviors) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.dialogue = Objects.requireNonNull(dialogue, "dialogue");
        this.progress = new DialogueProgressStore(Objects.requireNonNull(plugin, "plugin"));
        this.progress.load();
        this.behaviors = Objects.requireNonNull(behaviors, "behaviors");
    }

    public boolean open(Player player, RPGNpc npc) {
        if (npc.type() == NpcType.FILLER) return false;

        String key = "npc-intro:" + npc.id();
        if (progress.hasSeen(player.getUniqueId(), key)) return false;

        progress.markSeen(player.getUniqueId(), key);

        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        int level = profile == null ? 0 : profile.getLevel();

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(npcOpening(npc.type()), npc.type().getColor())));
        body.add(DialogBody.plainMessage(Component.text(levelLine(level, npc.type()), NamedTextColor.WHITE)));

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogue.actionButton(Component.text("Erzähl mir mehr"), NamedTextColor.AQUA,
                target -> continueToBehavior(target, npc)));

        switch (npc.type()) {
            case SHOP -> actions.add(dialogue.actionButton(Component.text("Kaufen"), NamedTextColor.GREEN,
                    target -> continueToBehavior(target, npc)));
            case TRAVEL -> actions.add(dialogue.actionButton(Component.text("Reisen"), NamedTextColor.LIGHT_PURPLE,
                    target -> continueToBehavior(target, npc)));
            case QUEST -> actions.add(dialogue.actionButton(Component.text("Quests ansehen"), NamedTextColor.YELLOW,
                    target -> continueToBehavior(target, npc)));
            case BANKER -> actions.add(dialogue.actionButton(Component.text("Bank öffnen"), NamedTextColor.DARK_GREEN,
                    target -> continueToBehavior(target, npc)));
            case RECEPTION -> actions.add(dialogue.actionButton(Component.text("Verwaltung öffnen"), NamedTextColor.AQUA,
                    target -> continueToBehavior(target, npc)));
            case STORY -> actions.add(dialogue.actionButton(Component.text("Geschichte hören"), NamedTextColor.GOLD,
                    target -> continueToBehavior(target, npc)));
            case PROFESSION_BLACKSMITH, PROFESSION_SCHOLAR, PROFESSION_FARMER,
                 PROFESSION_COOK, PROFESSION_TAILOR, PROFESSION_ALCHEMIST,
                 PROFESSION_MASON, PROFESSION_FISHERMAN, PROFESSION_WOODCUTTER ->
                    actions.add(dialogue.actionButton(Component.text("Über den Beruf sprechen"), NamedTextColor.GREEN,
                            target -> continueToBehavior(target, npc)));
            case FILLER -> { }
        }

        actions.add(dialogue.actionButton(Component.text("Gehen"), NamedTextColor.GRAY, Player::closeDialog));
        dialogue.openMultiAction(player, Component.text(npc.name(), npc.type().getColor()), body, actions, 2);
        return true;
    }

    private void continueToBehavior(Player player, RPGNpc npc) {
        behaviors.get(npc.type()).ifPresentOrElse(
                behavior -> behavior.onInteract(player, npc),
                () -> dialogue.openUnavailable(player, npc.name(), "Für diesen NPC ist noch keine Funktion hinterlegt."));
    }

    private String npcOpening(NpcType type) {
        return switch (type) {
            case RECEPTION -> "Die Nacht war unruhig. Die Rezeption ist trotzdem besetzt.";
            case SHOP -> "Die Nacht war schrecklich. Die Waren sind noch da, aber einige Regale sind leer.";
            case TRAVEL -> "Die Nacht war schrecklich. Reisende berichten von seltsamen Geräuschen auf den Wegen.";
            case QUEST -> "Die Nacht war schrecklich. Seit Sonnenaufgang kommen neue Hilferufe aus der Umgebung.";
            case BANKER -> "Die Nacht war schrecklich. Zum Glück sind die Tresore unversehrt geblieben.";
            case STORY -> "Die Nacht war schrecklich. Alte Zeichen sind wieder aufgetaucht.";
            case PROFESSION_BLACKSMITH -> "Die Nacht war schrecklich. Der Ofen ist ausgegangen, bevor die letzte Klinge fertig war.";
            case PROFESSION_SCHOLAR -> "Die Nacht war schrecklich. Im Archiv fehlten heute Morgen mehrere Seiten.";
            case PROFESSION_FARMER -> "Die Nacht war schrecklich. Einige Felder wurden verwüstet.";
            case PROFESSION_COOK -> "Die Nacht war schrecklich. Die Vorräte für die Küche sind knapp geworden.";
            case PROFESSION_TAILOR -> "Die Nacht war schrecklich. Mehrere Stoffballen wurden vom Wind fortgerissen.";
            case PROFESSION_ALCHEMIST -> "Die Nacht war schrecklich. Einige Phiolen haben im Labor reagiert.";
            case PROFESSION_MASON -> "Die Nacht war schrecklich. An den Mauern sind neue Risse aufgetaucht.";
            case PROFESSION_FISHERMAN -> "Die Nacht war schrecklich. Die Fische sind ungewöhnlich nah ans Ufer gekommen.";
            case PROFESSION_WOODCUTTER -> "Die Nacht war schrecklich. Im Wald sind Spuren zu sehen, die dort nicht hingehören.";
            case FILLER -> "";
        };
    }

    private String levelLine(int level, NpcType type) {
        if (level <= 0) return "Du bist hier noch als unbekannter Reisender unterwegs.";
        if (level < 10) return "Du wirkst noch wie ein Anfänger. Viele Dinge hier werden dir erst später vertraut sein.";
        if (level < 30) return "Du hast bereits Erfahrung gesammelt. " + levelHint(type);
        if (level < 60) return "Dein Ruf eilt dir voraus. " + levelHint(type);
        return "Du gehörst inzwischen zu den erfahrenen Abenteurern. " + levelHint(type);
    }

    private String levelHint(NpcType type) {
        return switch (type) {
            case SHOP -> "Ich kann dir bessere Waren zeigen, wenn du weiterkommst.";
            case QUEST -> "Es gibt Aufträge, die nur erfahrene Abenteurer übernehmen können.";
            case TRAVEL -> "Neue Wege werden dir mit der Zeit offenstehen.";
            case BANKER -> "Größere Vorhaben beginnen mit einem sicheren Vorrat.";
            default -> "Vielleicht können wir gemeinsam etwas daraus machen.";
        };
    }

    @Override
    public void close() {
        progress.shutdown();
    }
}
