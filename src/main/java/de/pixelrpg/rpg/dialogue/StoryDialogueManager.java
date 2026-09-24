package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryBookFactory;
import de.pixelrpg.rpg.story.StoryManager;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Native branching story dialogue for the PixelRPG lore campaign. */
public final class StoryDialogueManager {
    private final StoryManager storyManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public StoryDialogueManager(StoryManager storyManager,
                                PlayerProfileManager profileManager,
                                DialogueEngine dialogueEngine) {
        this.storyManager = Objects.requireNonNull(storyManager, "storyManager");
        this.profileManager = Objects.requireNonNull(profileManager, "profileManager");
        this.dialogueEngine = Objects.requireNonNull(dialogueEngine, "dialogueEngine");
    }

    public void openChapter(Player player, StoryChapter chapter) {
        if (!isRegistered(player) || chapter == null) return;
        if (!storyManager.isNextChapter(player.getUniqueId(), chapter)) {
            dialogueEngine.openUnavailable(player, "Geschichte", "Dieses Kapitel ist für dich noch nicht freigeschaltet.");
            return;
        }
        openNode(player, chapter, 0);
    }

    public void openEpilogue(Player player) {
        if (!isRegistered(player)) return;

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Der Archivar legt die Feder beiseite. "Die Welt gibt ihre Antworten nicht freiwillig. "
                                + "Manche Spuren führen zu einer Stadt unter der Erde, andere in den Nether und wieder andere "
                                + "durch ein Tor an einen Ort, dessen Geschichte niemand vollständig kennt."",
                        NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Du hast alle derzeit bekannten Kapitel gelesen. Die offenen Fragen bleiben absichtlich offen: "
                                + "Was die alten Erbauer wirklich wollten, warum sie verschwanden und welche Verbindung "
                                + "zwischen Sculk, den Tiefen und dem Ende besteht, muss die Welt selbst beantworten.",
                        NamedTextColor.GRAY))
        );

        dialogueEngine.openMultiAction(
                player,
                Component.text("Die Archive", NamedTextColor.GOLD),
                body,
                List.of(
                        dialogueEngine.actionButton(
                                Component.text("Kapitel erneut ansehen", NamedTextColor.YELLOW),
                                target -> reopenLastChapter(target)),
                        dialogueEngine.actionButton(
                                Component.text("Schließen", NamedTextColor.GRAY),
                                Player::closeDialog)
                ),
                1
        );
    }

    private void reopenLastChapter(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;
        StoryChapter chapter = storyManager.getChapter(profile.getStoryChapterIndex()).orElse(null);
        if (chapter == null) {
            openEpilogue(player);
            return;
        }
        openNode(player, chapter, 0);
    }

    private void openNode(Player player, StoryChapter chapter, int node) {
        if (!isRegistered(player) || chapter == null) return;

        switch (chapter.id()) {
            case "echoes_beneath" -> echoesBeneath(player, chapter, node);
            case "city_without_sky" -> cityWithoutSky(player, chapter, node);
            case "black_flame" -> blackFlame(player, chapter, node);
            case "door_beyond_stars" -> doorBeyondStars(player, chapter, node);
            case "after_the_end" -> afterTheEnd(player, chapter, node);
            default -> genericChapter(player, chapter);
        }
    }

    private void echoesBeneath(Player player, StoryChapter chapter, int node) {
        switch (node) {
            case 0 -> show(player, chapter, "Der erste Riss",
                    "Der Archivar spricht leiser als zuvor. "Unter unseren Füßen liegen Ruinen, die älter sind "
                            + "als jede Chronik unserer Gilde. Wir wissen nicht, wer sie errichtete. Wir wissen nur, "
                            + "dass manche ihrer Städte leer zurückblieben."",
                    option(player, chapter, "Erzähl mir von den Erbauern.", NamedTextColor.YELLOW, 1),
                    option(player, chapter, "Warum sollte mich das interessieren?", NamedTextColor.AQUA, 2));
            case 1 -> show(player, chapter, "Die Erbauer",
                    "Ihre Bauwerke zeigen gewaltige Kenntnisse von Redstone, Portalen und ungewöhnlichen Materialien. "
                            + "Ob sie eine einzige Kultur waren, wissen wir nicht. "Wer behauptet, ihre gesamte Geschichte "
                            + "zu kennen, verkauft dir eine Vermutung als Tatsache."",
                    option(player, chapter, "Und was ist mit ihren Städten?", NamedTextColor.YELLOW, 3),
                    option(player, chapter, "Ich will lieber selbst nachsehen.", NamedTextColor.GREEN, 4));
            case 2 -> show(player, chapter, "Eine Warnung",
                    ""Weil dieselben Ruinen überall Spuren hinterlassen: in verlassenen Minen, Festungen, Tempeln "
                            + "und an Orten, an denen die Welt selbst seltsam geworden ist. Wenn du ihre Geschichte suchst, "
                            + "suchst du vielleicht auch den Grund ihres Verschwindens."",
                    option(player, chapter, "Erzähl weiter.", NamedTextColor.YELLOW, 1),
                    option(player, chapter, "Ich brauche erst Beweise.", NamedTextColor.GRAY, 4));
            case 3 -> show(player, chapter, "Stadt ohne Himmel",
                    "Die Spur führt in die Tiefen. Dort liegen Ancient Cities, gewaltige Anlagen ohne Bewohner. "
                            + "In ihrem Zentrum wartet Sculk. Und wenn Sculk erwacht, antwortet etwas, das selbst erfahrene "
                            + "Abenteurer meiden.",
                    option(player, chapter, "Was antwortet dort?", NamedTextColor.RED, 5),
                    option(player, chapter, "Ich habe genug gehört.", NamedTextColor.GRAY, 4));
            case 4 -> finishPrompt(player, chapter,
                    "Der Archivar nickt. "Dann beginne mit dem, was du sehen kannst. Keine Legende ersetzt eigene Spuren."");
            case 5 -> show(player, chapter, "Der Warden",
                    ""Der Warden ist kein Schatzwächter, den jemand vor einer Truhe abgestellt hat. Er ist eine "
                            + "Bedrohung, die aus der Tiefe selbst zu kommen scheint. Sculk reagiert auf Geräusche. "
                            + "Die Ancient Cities bewahren diese Mechanik wie eine Narbe."",
                    option(player, chapter, "Ich werde vorsichtig sein.", NamedTextColor.GREEN, 4),
                    option(player, chapter, "Dann muss ich wissen, was Sculk ist.", NamedTextColor.LIGHT_PURPLE, 6));
            case 6 -> finishPrompt(player, chapter,
                    "Eine letzte Notiz: "Was Sculk ursprünglich war, wissen wir nicht sicher. Aber seine Ausbreitung "
                            + "ist eine der stärksten Spuren dafür, dass die alten Ruinen mehr verbargen als Reichtümer."");
            default -> finishPrompt(player, chapter, "Die Seite endet mit einem einzigen Wort: "Tiefe."");
        }
    }

    private void cityWithoutSky(Player player, StoryChapter chapter, int node) {
        switch (node) {
            case 0 -> show(player, chapter, "Das Echo",
                    "Du hast die Geschichten der Tiefen gehört. Der Archivar zeigt auf eine Skizze einer Ancient City. "
                            + ""Die Städte sind real. Der Warden ist real. Alles darüber hinaus müssen wir sauber trennen: "
                            + "Beobachtung hier, Theorie dort."",
                    option(player, chapter, "Was wissen wir sicher?", NamedTextColor.GREEN, 1),
                    conditionalEchoShard(player, chapter));
            case 1 -> show(player, chapter, "Was sicher ist",
                    "Sculk kann Erfahrung aufnehmen und auf Aktivität reagieren. Der Warden besitzt keine gewöhnlichen "
                            + "Augen, sondern findet Beute über Vibrationen und Geruch. Die Städte selbst zeigen, dass hier "
                            + "jemand lange genug lebte, um gewaltige Anlagen zu errichten.",
                    option(player, chapter, "Und was ist nur Theorie?", NamedTextColor.YELLOW, 2),
                    option(player, chapter, "Ich habe genug Beweise.", NamedTextColor.GRAY, 3));
            case 2 -> finishPrompt(player, chapter,
                    "Theorien über eine direkte Verbindung zwischen den Ancient Cities und anderen Dimensionen sind verbreitet. "
                            + "Der Archivar streicht sie durch: "Möglich ist vieles. Bewiesen ist es nicht."");
            case 3 -> finishPrompt(player, chapter,
                    "Du kennst nun den Unterschied zwischen einer Spur und einer Behauptung. Die nächste Spur führt dorthin, "
                            + "wo eine andere Zivilisation ihre eigene Geschichte bewahrt hat: in den Nether.");
            case 4 -> finishPrompt(player, chapter,
                    "Der Echo-Splitter pulsiert in deiner Tasche. Der Archivar sagt nur: "Manche Orte erinnern sich."");
            default -> finishPrompt(player, chapter, "Die Aufzeichnungen über die Tiefen werden versiegelt.");
        }
    }

    private void blackFlame(Player player, StoryChapter chapter, int node) {
        switch (node) {
            case 0 -> show(player, chapter, "Schwarze Flamme",
                    "Im Nether stehen die Spuren der Piglins im Mittelpunkt. Bastionsreste, Gold und Ruinen erzählen von "
                            + "einer Zivilisation, die mit ihrer Umgebung anders umging als die Bewohner der Oberwelt.",
                    option(player, chapter, "Was wissen wir über die Piglins?", NamedTextColor.GOLD, 1),
                    option(player, chapter, "Und der Wither?", NamedTextColor.RED, 2));
            case 1 -> show(player, chapter, "Die Piglin-Zivilisation",
                    "Piglins schätzen Gold und verteidigen ihre Bastions. Ihre Kultur ist nicht bloß eine Ansammlung "
                            + "feindlicher Mobs: Die Ruinen zeigen Lager, Schatzkammern und eine organisierte Gesellschaft. "
                            + "Was vor dem Verfall ihrer Bastions geschah, bleibt teilweise im Nebel.",
                    option(player, chapter, "Was geschah mit den alten Erbauern?", NamedTextColor.YELLOW, 3),
                    option(player, chapter, "Erzähl mir vom Wither.", NamedTextColor.RED, 2));
            case 2 -> show(player, chapter, "Die Erschaffung des Withers",
                    "Die ältesten Aufzeichnungen nennen eine konkrete Tat: Der Wither wird von Spielern beziehungsweise "
                            + "Menschen erschaffen, indem Seelensand oder Seelenerde mit drei Wither-Skelettschädeln "
                            + "verbunden wird. Seine Entstehung ist keine Naturkatastrophe ohne Ursprung.",
                    option(player, chapter, "Warum wurde er erschaffen?", NamedTextColor.YELLOW, 4),
                    option(player, chapter, "Ich will diese Macht nicht verstehen.", NamedTextColor.GRAY, 5));
            case 3 -> show(player, chapter, "Die offene Frage",
                    "Die Ruinen vieler alter Kulturen werden oft miteinander verbunden. Doch die Welt liefert keine vollständige "
                            + "Chronik. "Vielleicht waren manche Wege getrennt. Vielleicht trafen sie sich. Wir haben zu wenig Beweise."",
                    option(player, chapter, "Dann suche ich die nächste Spur.", NamedTextColor.GREEN, 5),
                    option(player, chapter, "Was hat das mit dem Ende zu tun?", NamedTextColor.LIGHT_PURPLE, 6));
            case 4 -> show(player, chapter, "Die Versuchung",
                    "Der Wither zeigt eine gefährliche Wahrheit: Wissen kann eine Waffe werden. "Nicht jede Entdeckung muss "
                            + "wiederholt werden", sagt der Archivar. "Manche muss nur verstanden werden."",
                    option(player, chapter, "Was kommt als Nächstes?", NamedTextColor.GREEN, 5),
                    option(player, chapter, "Ich suche einen anderen Weg.", NamedTextColor.AQUA, 5));
            case 5 -> finishPrompt(player, chapter,
                    "Im Archiv liegt eine Skizze eines Portals. Darunter steht: "Festungen. Enderaugen. Ein Tor ohne Rückkehr."");
            case 6 -> finishPrompt(player, chapter,
                    "Der Archivar antwortet: "Das Ende ist kein Beweis für die Herkunft der alten Völker. Aber es bewahrt "
                            + "Spuren, die wir sonst nirgends finden."");
            default -> finishPrompt(player, chapter, "Die Nether-Karte wird geschlossen.");
        }
    }

    private void doorBeyondStars(Player player, StoryChapter chapter, int node) {
        switch (node) {
            case 0 -> show(player, chapter, "Das Tor",
                    "Strongholds führen zu Endportalen. Enderaugen öffnen den Weg. Dahinter liegt eine Dimension, deren "
                            + "Zentrum von einer gewaltigen Insel und einem Enderdrachen beherrscht wird.",
                    option(player, chapter, "Was wissen wir über den Drachen?", NamedTextColor.LIGHT_PURPLE, 1),
                    option(player, chapter, "Warum heißt es das Ende?", NamedTextColor.YELLOW, 2));
            case 1 -> show(player, chapter, "Der Enderdrache",
                    "Der Enderdrache bewacht das zentrale Endgebiet und regeneriert sich über Endkristalle. "
                            + "Seine Niederlage verändert die Dimension und öffnet den Zugang zu den äußeren Endinseln.",
                    option(player, chapter, "Und die äußeren Inseln?", NamedTextColor.GREEN, 3),
                    option(player, chapter, "Das reicht mir.", NamedTextColor.GRAY, 4));
            case 2 -> show(player, chapter, "Ein Name ist keine Antwort",
                    ""Ende" beschreibt einen Ort und eine Erfahrung, nicht automatisch den Ursprung aller Dinge. "
                            + "Unsere Archive nennen die Dimension so, weil dort unsere sicherste Reise endet. Mehr dürfen wir "
                            + "nicht hineinlesen.",
                    option(player, chapter, "Dann zeig mir die Spuren.", NamedTextColor.GREEN, 3),
                    option(player, chapter, "Ich kehre um.", NamedTextColor.GRAY, 4));
            case 3 -> show(player, chapter, "Die äußeren Inseln",
                    "Dort stehen End Cities. In ihnen findet sich Beute, die auf eine frühere Präsenz hinweist, darunter "
                            + "Elytren in Endschiffen. Ob die früheren Reisenden die alten Erbauer waren, ist eine verbreitete "
                            + "Theorie, aber keine vollständig erklärte Chronik.",
                    option(player, chapter, "Dann bleibt eine letzte Frage.", NamedTextColor.YELLOW, 5),
                    option(player, chapter, "Ich habe genug gesehen.", NamedTextColor.GRAY, 4));
            case 4 -> finishPrompt(player, chapter,
                    "Du schließt das Buch. Der Archivar sagt: "Manchmal ist der verantwortungsvollste Schritt, eine offene "
                            + "Frage offen zu lassen."");
            case 5 -> finishPrompt(player, chapter,
                    "Eine letzte Karte zeigt nur einen Kreis: das Endportal. Darunter steht: "Wer die Reise beendet, "
                            + "findet nicht automatisch ihre Bedeutung."");
            default -> finishPrompt(player, chapter, "Das Tor bleibt in der Zeichnung geschlossen.");
        }
    }

    private void afterTheEnd(Player player, StoryChapter chapter, int node) {
        switch (node) {
            case 0 -> show(player, chapter, "Nach dem Ende",
                    "Der Archivar legt vier Berichte nebeneinander: die leeren Ancient Cities, die Piglin-Bastions, "
                            + "die Geschichte des Withers und die Spuren des Endes.",
                    option(player, chapter, "Ich glaube, alles hängt zusammen.", NamedTextColor.LIGHT_PURPLE, 1),
                    option(player, chapter, "Vielleicht muss nicht alles zusammenhängen.", NamedTextColor.GREEN, 2));
            case 1 -> show(player, chapter, "Die Theorie",
                    ""Es gibt Muster", sagt er. "Aber ein Muster ist noch kein Beweis. Sculk, Portale, Ruinen und das Ende "
                            + "liegen in derselben Welt. Vielleicht erzählen sie eine gemeinsame Geschichte. Vielleicht erzählen "
                            + "sie mehrere."",
                    option(player, chapter, "Ich werde die Welt selbst untersuchen.", NamedTextColor.GREEN, 3),
                    option(player, chapter, "Ich will die Archive bewahren.", NamedTextColor.YELLOW, 4));
            case 2 -> show(player, chapter, "Die offene Welt",
                    ""Das ist die Haltung eines guten Historikers." Der Archivar lächelt. "Wir bewahren Fakten, markieren "
                            + "Theorien und lassen der nächsten Generation neue Spuren finden."",
                    option(player, chapter, "Dann führe ich die Spur weiter.", NamedTextColor.GREEN, 3),
                    option(player, chapter, "Ich helfe den Archiven.", NamedTextColor.YELLOW, 4));
            case 3 -> finishPrompt(player, chapter,
                    "Du erhältst keine Krone und keine endgültige Antwort. Nur eine Aufgabe: Beobachte, prüfe und hinterlasse "
                            + "bessere Aufzeichnungen als jene, die du vorgefunden hast.");
            case 4 -> finishPrompt(player, chapter,
                    "Der Archivar übergibt dir das Archivbuch. "Geschichte gehört niemandem. Sie gehört denen, die sie bewahren "
                            + "und ehrlich von ihren Lücken erzählen."");
            default -> finishPrompt(player, chapter, "Das letzte Kapitel endet mit einer leeren Seite.");
        }
    }

    private void genericChapter(Player player, StoryChapter chapter) {
        finishPrompt(player, chapter, String.join(" ", chapter.dialogueLines()));
    }

    private void finishPrompt(Player player, StoryChapter chapter, String text) {
        List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text(text, NamedTextColor.WHITE)));
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = new ArrayList<>();
        actions.add(dialogueEngine.actionButton(
                Component.text("Kapitel im Archiv lesen", NamedTextColor.YELLOW),
                target -> target.openBook(StoryBookFactory.build(chapter))));
        actions.add(dialogueEngine.actionButton(
                Component.text("Kapitel abschließen", NamedTextColor.GREEN),
                target -> {
                    if (!storyManager.completeChapter(target, chapter)) {
                        dialogueEngine.openUnavailable(target, "Geschichte", "Dein Story-Fortschritt hat sich inzwischen geändert.");
                        return;
                    }
                    target.sendMessage(Component.text(
                            "Kapitel abgeschlossen: " + chapter.title(),
                            NamedTextColor.GREEN));
                    target.closeDialog();
                }));
        dialogueEngine.openMultiAction(player, Component.text(chapter.title(), NamedTextColor.GOLD), body, actions, 1);
    }

    private io.papermc.paper.registry.data.dialog.ActionButton conditionalEchoShard(Player player, StoryChapter chapter) {
        if (!hasItem(player, Material.ECHO_SHARD)) {
            return dialogueEngine.actionButton(
                    Component.text("Ich habe keine weiteren Beweise.", NamedTextColor.GRAY),
                    target -> openNode(target, chapter, 3));
        }
        return dialogueEngine.actionButton(
                Component.text("Ich besitze einen Echo-Splitter.", NamedTextColor.LIGHT_PURPLE),
                target -> {
                    if (!hasItem(target, Material.ECHO_SHARD)) {
                        dialogueEngine.openUnavailable(target, "Die Tiefen", "Der Echo-Splitter befindet sich nicht mehr in deinem Inventar.");
                        return;
                    }
                    openNode(target, chapter, 4);
                });
    }

    private io.papermc.paper.registry.data.dialog.ActionButton option(Player player, StoryChapter chapter,
                                                                        String label, NamedTextColor color, int nextNode) {
        return dialogueEngine.actionButton(Component.text(label, color), target -> openNode(target, chapter, nextNode));
    }

    private void show(Player player, StoryChapter chapter, String title, String body, io.papermc.paper.registry.data.dialog.ActionButton... options) {
        List<io.papermc.paper.registry.data.dialog.ActionButton> actions = List.of(options);
        dialogueEngine.openMultiAction(
                player,
                Component.text(title, NamedTextColor.GOLD),
                List.of(DialogBody.plainMessage(Component.text(body, NamedTextColor.WHITE))),
                actions,
                actions.size() > 2 ? 2 : 1);
    }

    private boolean isRegistered(Player player) {
        return player != null && player.isOnline() && profileManager.isRegistered(player.getUniqueId());
    }

    private boolean hasItem(Player player, Material material) {
        if (player == null || material == null) return false;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material && item.getAmount() > 0) return true;
        }
        return false;
    }
}
