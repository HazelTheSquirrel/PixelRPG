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

/**
 * Provides the narrative layer for administrator-placed functional NPCs.
 * Story and filler NPCs deliberately use their own systems.
 */
public final class NpcDialogueService implements AutoCloseable {
    private static final int VARIANTS_PER_CYCLE = 5;

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

    /**
     * Opens the NPC's narrative hub. The functional action remains available beside the lore.
     * The lore page rotates through five unique entries per level band and repeats only after all
     * five entries of the current cycle have been seen.
     */
    public boolean open(Player player, RPGNpc npc) {
        if (npc.type() == NpcType.FILLER || npc.type() == NpcType.STORY) return false;

        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        int level = profile == null ? 0 : profile.getLevel();
        String introKey = "npc-intro:" + npc.id();
        boolean firstVisit = !progress.hasSeen(player.getUniqueId(), introKey);
        progress.markSeen(player.getUniqueId(), introKey);

        openIntro(player, npc, level, firstVisit);
        return true;
    }

    private void openIntro(Player player, RPGNpc npc, int level, boolean firstVisit) {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(npcOpening(npc.type(), firstVisit), npc.type().getColor())));
        body.add(DialogBody.plainMessage(Component.text(levelLine(level, npc.type()), NamedTextColor.WHITE)));

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogue.actionButton(Component.text("Erzähl mir mehr"), NamedTextColor.AQUA,
                target -> openMore(target, npc)));
        addServiceAction(actions, npc);
        actions.add(dialogue.actionButton(Component.text("Gehen"), NamedTextColor.GRAY, Player::closeDialog));

        dialogue.openMultiAction(player, Component.text(npc.name(), npc.type().getColor()), body, actions, 2);
    }

    private void openMore(Player player, RPGNpc npc) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        int level = profile == null ? 0 : profile.getLevel();
        int band = levelBand(level);
        int variant = nextVariant(player, npc, band);

        String[] lines = moreLines(npc.type(), variant);
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(lines[0], npc.type().getColor())),
                DialogBody.plainMessage(Component.text(lines[1], NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(levelPerspective(level, npc.type(), variant), NamedTextColor.LIGHT_PURPLE))
        );

        List<ActionButton> actions = new ArrayList<>();
        actions.add(dialogue.actionButton(Component.text("Noch etwas erzählen"), NamedTextColor.AQUA,
                target -> openMore(target, npc)));
        actions.add(dialogue.actionButton(Component.text("Zurück"), NamedTextColor.WHITE,
                target -> openIntro(target, npc, level, false)));

        dialogue.openMultiAction(player, Component.text(npc.name() + " – mehr erfahren", npc.type().getColor()),
                body, actions, 1);
    }

    private int nextVariant(Player player, RPGNpc npc, int band) {
        for (int cycle = 0; cycle < 10_000; cycle++) {
            for (int variant = 0; variant < VARIANTS_PER_CYCLE; variant++) {
                String key = moreKey(npc, band, cycle, variant);
                if (!progress.hasSeen(player.getUniqueId(), key)) {
                    progress.markSeen(player.getUniqueId(), key);
                    return variant;
                }
            }
        }
        return 0;
    }

    private String moreKey(RPGNpc npc, int band, int cycle, int variant) {
        return "npc-more:" + npc.id() + ":" + band + ":" + cycle + ":" + variant;
    }

    private void addServiceAction(List<ActionButton> actions, RPGNpc npc) {
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
            case PROFESSION_BLACKSMITH, PROFESSION_SCHOLAR, PROFESSION_FARMER,
                 PROFESSION_COOK, PROFESSION_TAILOR, PROFESSION_ALCHEMIST,
                 PROFESSION_MASON, PROFESSION_FISHERMAN, PROFESSION_WOODCUTTER ->
                    actions.add(dialogue.actionButton(Component.text("Über den Beruf sprechen"), NamedTextColor.GREEN,
                            target -> continueToBehavior(target, npc)));
            case STORY, FILLER -> {
                // Story and filler NPCs deliberately do not use the generic admin-NPC service layer.
            }
        }
    }

    private int levelOf(Player player) {
        return profiles.getProfile(player.getUniqueId()).map(PlayerProfile::getLevel).orElse(0);
    }

    private void continueToBehavior(Player player, RPGNpc npc) {
        behaviors.get(npc.type()).ifPresentOrElse(
                behavior -> behavior.onInteract(player, npc, target -> openIntro(target, npc, levelOf(target), false)),
                () -> dialogue.openUnavailable(player, npc.name(), "Für diesen NPC ist noch keine Funktion hinterlegt."));
    }

    private String npcOpening(NpcType type, boolean firstVisit) {
        if (firstVisit) {
            return switch (type) {
                case RECEPTION -> "Die Nacht war unruhig. Die Rezeption ist trotzdem besetzt. Wenn du neu bist, zeige ich dir, wo du anfangen kannst.";
                case SHOP -> "Die Nacht war schrecklich. Die Waren sind noch da, aber einige Regale sind leer. Ich kann dir erzählen, was heute Nacht passiert ist.";
                case TRAVEL -> "Die Nacht war schrecklich. Reisende berichten von seltsamen Geräuschen auf den Wegen. Bevor du aufbrichst, solltest du wissen, was sich verändert hat.";
                case QUEST -> "Die Nacht war schrecklich. Seit Sonnenaufgang kommen neue Hilferufe aus der Umgebung. Manche davon sind dringender, als sie zunächst klingen.";
                case BANKER -> "Die Nacht war schrecklich. Zum Glück sind die Tresore unversehrt geblieben. Bei solchen Nächten merkt man, warum Vertrauen und Ordnung wichtig sind.";
                case PROFESSION_BLACKSMITH -> "Die Nacht war schrecklich. Der Ofen ist ausgegangen, bevor die letzte Klinge fertig war. Wenn du lernen willst, wie man hier arbeitet, hör gut zu.";
                case PROFESSION_SCHOLAR -> "Die Nacht war schrecklich. Im Archiv fehlten heute Morgen mehrere Seiten. Wissen verschwindet manchmal leiser als Gold.";
                case PROFESSION_FARMER -> "Die Nacht war schrecklich. Einige Felder wurden verwüstet. Landwirtschaft bedeutet hier mehr, als nur etwas zu pflanzen und zu warten.";
                case PROFESSION_COOK -> "Die Nacht war schrecklich. Die Vorräte für die Küche sind knapp geworden. Ein guter Koch muss wissen, was man retten und was man ersetzen kann.";
                case PROFESSION_TAILOR -> "Die Nacht war schrecklich. Mehrere Stoffballen wurden vom Wind fortgerissen. Stoff ist geduldig – aber nur, wenn man ihn richtig behandelt.";
                case PROFESSION_ALCHEMIST -> "Die Nacht war schrecklich. Einige Phiolen haben im Labor reagiert. In der Alchemie ist Neugier nützlich, Nachlässigkeit dagegen teuer.";
                case PROFESSION_MASON -> "Die Nacht war schrecklich. An den Mauern sind neue Risse aufgetaucht. Ein guter Maurer repariert nicht nur den Schaden, sondern versteht seine Ursache.";
                case PROFESSION_FISHERMAN -> "Die Nacht war schrecklich. Die Fische sind ungewöhnlich nah ans Ufer gekommen. Das Wasser erzählt oft früher von Veränderungen als die Menschen.";
                case PROFESSION_WOODCUTTER -> "Die Nacht war schrecklich. Im Wald sind Spuren zu sehen, die dort nicht hingehören. Wer Holz schlägt, muss den Wald lesen können.";
                case STORY, FILLER -> "";
            };
        }
        return switch (type) {
            case RECEPTION -> "Wieder da? Gut. An der Rezeption sammeln sich jeden Tag neue Geschichten, Beschwerden und kleine Probleme.";
            case SHOP -> "Du bist wieder da. Einige Waren sind ersetzt worden, andere fehlen noch. Der Handel verändert sich schneller als man denkt.";
            case TRAVEL -> "Die Wege sind heute ruhiger. Trotzdem würde ich nicht jede Spur am Straßenrand für harmlos halten.";
            case QUEST -> "Es gibt neue Meldungen. Nicht jede davon ist ein Auftrag – manche sind Warnungen, die erst später wichtig werden.";
            case BANKER -> "Die Kasse stimmt, die Tresore sind sicher. Was außerhalb der Bank passiert, ist allerdings eine andere Frage.";
            case PROFESSION_BLACKSMITH -> "Der Ofen brennt wieder. Eine gute Klinge beginnt nicht am Amboss, sondern bei der Vorbereitung.";
            case PROFESSION_SCHOLAR -> "Das Archiv ist wieder geordnet. Trotzdem bleiben einige Fragen absichtlich offen.";
            case PROFESSION_FARMER -> "Die Felder erholen sich. Heute sieht man wieder, warum Geduld ein Teil dieses Handwerks ist.";
            case PROFESSION_COOK -> "Die Küche läuft wieder. Ein Rezept ist selten nur eine Liste von Zutaten.";
            case PROFESSION_TAILOR -> "Die Stoffe sind wieder aufgerollt. Gute Arbeit erkennt man oft erst, wenn sie viele Abenteuer übersteht.";
            case PROFESSION_ALCHEMIST -> "Die Phiolen stehen wieder sicher. Die interessantesten Ergebnisse entstehen selten beim ersten Versuch.";
            case PROFESSION_MASON -> "Die Mauer hält. Jetzt geht es darum, zu verstehen, warum sie überhaupt nachgegeben hat.";
            case PROFESSION_FISHERMAN -> "Das Wasser ist ruhiger. Die Strömung hat trotzdem Dinge mitgebracht, die ich noch nicht einordnen kann.";
            case PROFESSION_WOODCUTTER -> "Der Wald ist heute still. Zu still, wenn du mich fragst.";
            case STORY, FILLER -> "";
        };
    }

    private String[] moreLines(NpcType type, int variant) {
        return switch (type) {
            case RECEPTION -> switch (variant) {
                case 0 -> new String[]{"Hier beginnen viele Wege.", "Wer sich registriert, bekommt Zugang zu den grundlegenden Verwaltungsfunktionen. Von dort aus verzweigt sich dein Abenteuer in Quests, Berufe, Party und Gilde."};
                case 1 -> new String[]{"Eine Stadt lebt von ihren Leuten.", "An der Rezeption landen Nachrichten, bevor sie zu Gerüchten werden. Wenn du etwas suchst, lohnt es sich, zuerst nach dem richtigen Ansprechpartner zu fragen."};
                case 2 -> new String[]{"Nicht jede Hilfe ist ein großer Auftrag.", "Manchmal ist ein Hinweis wertvoller als eine Belohnung. Kleine Informationen können dir später Wege öffnen, die du heute noch nicht siehst."};
                case 3 -> new String[]{"Ordnung klingt langweilig – bis sie fehlt.", "Registrierungen, Gruppen und Gilden wirken getrennt, greifen aber ineinander. Ein organisierter Abenteurer verliert weniger Zeit mit Dingen, die andere längst geklärt haben."};
                default -> new String[]{"Du musst nicht alles sofort verstehen.", "PixelRPG wächst mit deinem Fortschritt. Wenn du später wiederkommst, wirst du manche Orte und Funktionen mit anderen Augen sehen."};
            };
            case SHOP -> switch (variant) {
                case 0 -> new String[]{"Ware hat eine Geschichte.", "Manche Kisten kommen aus Dörfern, andere von Reisenden oder aus Gegenden, in die Händler nur selten gehen. Deshalb verändert sich mein Sortiment mit dem, was die Welt hergibt."};
                case 1 -> new String[]{"Die leeren Regale sind kein Zufall.", "Nach einer schlechten Nacht fehlen manchmal ganz gewöhnliche Dinge: Werkzeuge, Vorräte oder Material. Wer aufmerksam einkauft, erkennt oft früher als andere, wo etwas knapp wird."};
                case 2 -> new String[]{"Ein guter Händler kennt seine Kunden.", "Anfänger brauchen andere Dinge als erfahrene Abenteurer. Wenn du stärker wirst, achte nicht nur auf den Preis – achte darauf, ob ein Gegenstand zu deinem nächsten Ziel passt."};
                case 3 -> new String[]{"Manche Waren tauchen nur selten auf.", "Seltene Lieferungen hängen von Wegen, Regionen und den Leuten ab, die sie erreichen können. Wenn etwas heute fehlt, heißt das nicht, dass es für immer verschwunden ist."};
                default -> new String[]{"Handel ist auch Information.", "Wer weiß, was gebraucht wird, versteht oft, was gerade in der Welt passiert. Ein leerer Vorratskorb kann manchmal mehr erzählen als ein langer Bericht."};
            };
            case TRAVEL -> switch (variant) {
                case 0 -> new String[]{"Wege sind niemals nur Linien.", "Ein Reisepunkt bedeutet Sicherheit, Orientierung und eine bekannte Ankunft. Je mehr Orte du entdeckst, desto weniger abhängig bist du von Zufällen auf langen Wegen."};
                case 1 -> new String[]{"Manche Straßen verändern ihren Ruf.", "Ein Weg kann heute sicher wirken und morgen gemieden werden. Geräusche, Spuren und Berichte der Reisenden sind oft die ersten Hinweise auf eine Veränderung."};
                case 2 -> new String[]{"Ich merke mir nicht nur Orte.", "Ich merke mir, wo Reisende zurückkehren und wo sie plötzlich ausbleiben. Ein Ziel, das niemand mehr anfährt, verdient meistens eine Erklärung."};
                case 3 -> new String[]{"Entdecken und Reisen sind zwei verschiedene Dinge.", "Ein Ort kann bekannt sein, ohne dass du ihn wirklich verstanden hast. Besuche neue Gebiete, aber nimm dir Zeit, ihre Bewohner und Gefahren kennenzulernen."};
                default -> new String[]{"Der schnellste Weg ist nicht immer der beste.", "Wenn du eine neue Region erreichst, kann ein sicherer Umweg wertvoller sein als ein paar Minuten Zeitersparnis. Überleben ist schließlich auch Fortschritt."};
            };
            case QUEST -> switch (variant) {
                case 0 -> new String[]{"Nicht jeder Hilferuf wird zum großen Abenteuer.", "Manche Aufgaben beginnen mit etwas Alltäglichem: einer Lieferung, einem verschwundenen Gegenstand oder einer Spur. Erst deine Entscheidungen machen daraus eine Geschichte."};
                case 1 -> new String[]{"Aufträge haben Konsequenzen.", "Wenn du einen Auftrag annimmst, verschiebt sich dein Alltag. Erledigte Aufgaben bringen nicht nur Gold und Erfahrung, sondern können neue Möglichkeiten sichtbar machen."};
                case 2 -> new String[]{"Erfahrene Abenteurer hören genauer hin.", "Eine scheinbar einfache Bitte kann an einen größeren Konflikt grenzen. Lies die Beschreibung und das Ziel einer Quest, bevor du dich blind auf den Weg machst."};
                case 3 -> new String[]{"Manche Geschichten brauchen Zeit.", "Nicht jede Quest lässt sich in einem einzigen Besuch abschließen. Fortschritt, Rückkehr und ein guter Zeitpunkt gehören manchmal genauso zum Auftrag wie das eigentliche Ziel."};
                default -> new String[]{"Ich sortiere nicht nur nach Belohnung.", "Ein Auftrag kann dir eine Gegend zeigen, einen Beruf ergänzen oder dich auf spätere Herausforderungen vorbereiten. Manchmal ist der unscheinbare Auftrag der nützlichere."};
            };
            case BANKER -> switch (variant) {
                case 0 -> new String[]{"Gold ist nur dann nützlich, wenn es bleibt.", "Die Bank schützt deinen Vorrat vor den kleinen Entscheidungen, die sich während eines Abenteuers summieren. Sicherheit schafft dir Raum für größere Pläne."};
                case 1 -> new String[]{"Vertrauen ist hier eine Rechnung.", "Jede Einlage muss nachvollziehbar sein. Deshalb behandeln wir Ordnung nicht als Bürokratie, sondern als Schutz vor Fehlern und Missverständnissen."};
                case 2 -> new String[]{"Große Ziele beginnen mit kleinen Reserven.", "Ausrüstung, Reisen, Handwerk und andere Vorhaben kosten Ressourcen. Wer alles sofort ausgibt, merkt oft zu spät, wie wertvoll ein sicherer Vorrat gewesen wäre."};
                case 3 -> new String[]{"Eine ruhige Bank ist ein gutes Zeichen.", "Wenn hier nichts passiert, bedeutet das meistens, dass andere Leute draußen beschäftigt sind. Ich bevorzuge diese Art von Nachrichten."};
                default -> new String[]{"Die Tresore erzählen wenig.", "Das ist Absicht. Was Menschen hier verwahren, geht niemanden etwas an. Du kannst dich auf dein Abenteuer konzentrieren, ohne jeden Vorrat mit dir herumzutragen."};
            };
            case PROFESSION_BLACKSMITH -> switch (variant) {
                case 0 -> new String[]{"Metall verzeiht keine Ungeduld.", "Temperatur, Material und Timing entscheiden darüber, ob aus einem Stück Metall ein Werkzeug oder eine Schwachstelle wird. Gute Schmiede arbeiten kontrolliert, nicht hektisch."};
                case 1 -> new String[]{"Der Amboss ist nur der Anfang.", "Eine Klinge braucht Vorbereitung, Formgebung und Nacharbeit. Wer nur auf den letzten Schlag achtet, übersieht die vielen kleinen Entscheidungen davor."};
                case 2 -> new String[]{"Nicht jedes Metall ist gleich.", "Härte, Gewicht und Verwendungszweck bestimmen, welches Material sinnvoll ist. Ein schwereres Stück ist nicht automatisch die bessere Wahl."};
                case 3 -> new String[]{"Reparieren ist eine eigene Kunst.", "Ein beschädigter Gegenstand zeigt dir oft, wo seine Schwäche lag. Wer daraus lernt, fertigt beim nächsten Mal nicht nur eine Reparatur, sondern eine Verbesserung."};
                default -> new String[]{"Eine gute Schmiede hört zu.", "Metall klingt anders, wenn etwas stimmt oder wenn ein Riss entsteht. Erfahrung bedeutet, solche Unterschiede zu erkennen, bevor sie zum Problem werden."};
            };
            case PROFESSION_SCHOLAR -> switch (variant) {
                case 0 -> new String[]{"Wissen beginnt mit einer guten Frage.", "Ein Archiv ist kein Lager voller Antworten. Es ist ein Ort, an dem Beobachtungen, Quellen und Vermutungen voneinander getrennt werden müssen."};
                case 1 -> new String[]{"Eine fehlende Seite kann wichtig sein.", "Manchmal fehlt nur ein Detail, aber genau dieses Detail entscheidet, ob eine Theorie trägt. Deshalb bewahren wir auch scheinbar unspektakuläre Aufzeichnungen auf."};
                case 2 -> new String[]{"Quellen und Vermutungen sind nicht dasselbe.", "Was wir in der Welt beobachten können, gehört in den Bericht. Was wir daraus ableiten, muss als Theorie erkennbar bleiben."};
                case 3 -> new String[]{"Forschen heißt auch, Irrtümer auszuhalten.", "Eine gute Theorie darf scheitern. Entscheidend ist, dass du anschließend weißt, welcher Teil der Beobachtung sicher war und welcher nur eine Annahme."};
                default -> new String[]{"Die besten Entdeckungen beginnen unscheinbar.", "Ein Name, ein Symbol oder eine alte Markierung kann später mit anderen Funden zusammenpassen. Deshalb lohnt es sich, Details nicht vorschnell wegzuwerfen."};
            };
            case PROFESSION_FARMER -> switch (variant) {
                case 0 -> new String[]{"Der Boden merkt sich alles.", "Zu viel Ertrag ohne Pflege erschöpft Ressourcen. Gute Landwirtschaft bedeutet, Vorräte zu planen und nicht jede Saison gleich zu behandeln."};
                case 1 -> new String[]{"Wetter ist Teil des Handwerks.", "Regen, Trockenheit und Temperatur beeinflussen, was gelingt. Wer nur auf das Ergebnis schaut, übersieht die Bedingungen, die es möglich gemacht haben."};
                case 2 -> new String[]{"Nicht alles Wertvolle wächst schnell.", "Manche Pflanzen brauchen Geduld, andere liefern früh einen kleinen Ertrag. Ein guter Hof kombiniert beides, statt alles auf eine einzige Ernte zu setzen."};
                case 3 -> new String[]{"Eine Ernte ist auch eine Entscheidung.", "Was du aufbewahrst, was du verkaufst und was du weiterverarbeitest, bestimmt, was dir später zur Verfügung steht. Planung macht aus Nahrung einen Vorrat."};
                default -> new String[]{"Die Felder sind ein Frühwarnsystem.", "Veränderte Tiere, beschädigte Pflanzen oder fremde Spuren können Hinweise auf Probleme in der Umgebung geben. Wer täglich hinsieht, bemerkt Veränderungen früh."};
            };
            case PROFESSION_COOK -> switch (variant) {
                case 0 -> new String[]{"Kochen beginnt vor dem Feuer.", "Zutaten, Menge und Reihenfolge entscheiden über das Ergebnis. Gute Küche ist weniger Zauberei als saubere Vorbereitung."};
                case 1 -> new String[]{"Reste sind keine Schande.", "Viele Zutaten lassen sich sinnvoll weiterverarbeiten. Wer Ressourcen versteht, wirft weniger weg und kann aus einfachen Vorräten überraschend viel machen."};
                case 2 -> new String[]{"Ein Rezept ist eine Richtung.", "Erfahrene Köche wissen, wann eine Zutat ersetzt werden kann und wann sie das Gericht verändert. Lernen bedeutet, den Unterschied zu verstehen."};
                case 3 -> new String[]{"Essen ist auch Vorbereitung.", "Eine gute Mahlzeit kann dich für einen langen Weg oder einen schwierigen Kampf vorbereiten. Nicht jede Belohnung muss dauerhaft sein, um wertvoll zu sein."};
                default -> new String[]{"Die Küche hört viel.", "Reisende reden beim Essen. Deshalb erfährt man hier manchmal früh, welche Wege unsicher sind, welche Waren fehlen oder wo jemand Hilfe braucht."};
            };
            case PROFESSION_TAILOR -> switch (variant) {
                case 0 -> new String[]{"Stoff ist ein Material mit Gedächtnis.", "Fasern reagieren auf Spannung, Feuchtigkeit und Verarbeitung. Wer sie falsch behandelt, bekommt ein Kleidungsstück, das schon vor dem ersten Abenteuer verschleißt."};
                case 1 -> new String[]{"Schnitt ist wichtiger als Schmuck.", "Eine gute Form entscheidet zuerst über Beweglichkeit und Haltbarkeit. Verzierungen kommen danach – zumindest bei Arbeit, die wirklich etwas aushalten muss."};
                case 2 -> new String[]{"Farben erzählen von Regionen.", "Bestimmte Farbstoffe und Stoffe sind nicht überall gleich leicht zu bekommen. Deshalb kann Kleidung manchmal verraten, wo jemand unterwegs war."};
                case 3 -> new String[]{"Reparaturen zeigen gute Arbeit.", "Wenn eine Naht sauber nachgibt und sich wieder schließen lässt, war sie richtig gesetzt. Schlechte Verarbeitung hält nur so lange, bis das erste echte Abenteuer kommt."};
                default -> new String[]{"Schutz und Stil schließen sich nicht aus.", "Ein gutes Stück Kleidung muss sich bewegen lassen, schützen und trotzdem zum Träger passen. Das Handwerk liegt darin, diese Anforderungen zusammenzubringen."};
            };
            case PROFESSION_ALCHEMIST -> switch (variant) {
                case 0 -> new String[]{"Alchemie beginnt mit Vorsicht.", "Eine Reaktion kann nützlich sein, weil sie stark ist – oder gefährlich, weil sie unkontrolliert ist. Deshalb ist jede Mischung zuerst ein Versuch mit klaren Grenzen."};
                case 1 -> new String[]{"Nicht jede Reaktion ist ein Erfolg.", "Ein misslungenes Ergebnis kann trotzdem zeigen, was eine Zutat nicht verträgt. Wer sauber dokumentiert, macht aus einem Fehler später Wissen."};
                case 2 -> new String[]{"Dosis verändert Bedeutung.", "Eine kleine Menge kann unterstützen, eine größere Menge kann eine völlig andere Wirkung haben. Präzision ist in der Alchemie kein Luxus."};
                case 3 -> new String[]{"Phiolen sind kleine Archive.", "Farbe, Trübung, Geruch und Reaktion verraten oft, ob eine Mischung stabil geblieben ist. Gute Alchemisten beobachten, bevor sie urteilen."};
                default -> new String[]{"Neugier braucht Disziplin.", "Die spannendste Mischung ist wertlos, wenn niemand weiß, was darin steckt. Deshalb sind Beschriftung und saubere Arbeitsweise genauso wichtig wie die Entdeckung selbst."};
            };
            case PROFESSION_MASON -> switch (variant) {
                case 0 -> new String[]{"Eine Mauer beginnt mit dem Untergrund.", "Wenn das Fundament falsch sitzt, hilft auch perfektes Mauerwerk nur vorübergehend. Gute Bauarbeit beginnt deshalb dort, wo man sie später kaum noch sieht."};
                case 1 -> new String[]{"Risse sind Nachrichten.", "Breite, Richtung und Lage eines Risses verraten, welche Belastung gewirkt haben könnte. Reparieren ohne Ursache zu verstehen bedeutet, das Problem nur zu verschieben."};
                case 2 -> new String[]{"Materialwahl ist Standortwahl.", "Stein, Ziegel und andere Materialien reagieren unterschiedlich auf Belastung und Umgebung. Ein guter Baumeister baut nicht überall mit demselben Rezept."};
                case 3 -> new String[]{"Alte Mauern sind Lehrbücher.", "Selbst beschädigte Strukturen zeigen, wie frühere Baumeister Lasten verteilt und Wege geschützt haben. Wer genau hinsieht, kann aus Fehlern und Erfolgen lernen."};
                default -> new String[]{"Stabilität ist unsichtbare Arbeit.", "Wenn eine Mauer ihren Zweck erfüllt, bemerkt niemand ihre Konstruktion. Genau das ist das Ziel: Das Bauwerk soll funktionieren, ohne ständig Aufmerksamkeit zu verlangen."};
            };
            case PROFESSION_FISHERMAN -> switch (variant) {
                case 0 -> new String[]{"Das Wasser spricht durch Bewegung.", "Strömung, Oberfläche und Verhalten der Fische verraten, was unterhalb der Sichtweite passiert. Geduld bedeutet beim Angeln nicht Stillstand, sondern Beobachtung."};
                case 1 -> new String[]{"Nicht jeder Fang ist Beute.", "Manchmal bringt das Wasser Dinge an die Oberfläche, die mehr Fragen als Gold wert sind. Ein guter Fischer schaut deshalb zuerst, was er eigentlich gefunden hat."};
                case 2 -> new String[]{"Tiefe verändert alles.", "Ein ruhiges Ufer und ein tiefer Abschnitt können völlig unterschiedliche Bedingungen haben. Wer seine Umgebung versteht, muss wissen, wo sich das Leben sammelt."};
                case 3 -> new String[]{"Das Wetter schreibt auf die Wasseroberfläche.", "Wind, Regen und Temperatur verändern, wie sich Fische bewegen. Erfahrung heißt, diese Zeichen zu lesen, bevor die Angel überhaupt im Wasser liegt."};
                default -> new String[]{"Fischen ist auch Geduld mit Grenzen.", "Wenn nichts beißt, bringt hektisches Wechseln selten etwas. Manchmal ist die beste Entscheidung, den Platz zu wechseln und später wiederzukommen."};
            };
            case PROFESSION_WOODCUTTER -> switch (variant) {
                case 0 -> new String[]{"Ein Wald ist kein Vorratslager.", "Wenn du nur nimmst, was du siehst, zerstörst du langfristig die Grundlage deines eigenen Handwerks. Nachhaltiges Arbeiten beginnt mit der Frage, was morgen noch wachsen soll."};
                case 1 -> new String[]{"Holz zeigt dir den Standort.", "Jahresringe, Feuchtigkeit und Wuchsrichtung erzählen, welchen Bedingungen ein Baum ausgesetzt war. Das hilft bei der Wahl des richtigen Materials."};
                case 2 -> new String[]{"Nicht jeder Baum gehört gefällt.", "Manche stehen an Stellen, an denen sie Schutz, Orientierung oder Lebensraum bieten. Gute Holzfäller erkennen, wann ein Baum wertvoller ist, wenn er stehen bleibt."};
                case 3 -> new String[]{"Der Wald verändert sich langsam.", "Neue Spuren, beschädigte Rinde oder ungewöhnliche Stille können Hinweise sein. Wer täglich dort arbeitet, bemerkt Veränderungen, die anderen entgehen."};
                default -> new String[]{"Gutes Holz braucht Zeit.", "Schnelles Wachstum und hohe Qualität sind nicht immer dasselbe. Geduld bei Auswahl und Lagerung entscheidet später darüber, wie zuverlässig das Material arbeitet."};
            };
            case STORY, FILLER -> new String[]{"Dieser Dialog gehört nicht zum allgemeinen NPC-System.", "Story- und FILLER-NPCs werden ausschließlich von ihren eigenen Systemen gesteuert."};
        };
    }

    private String levelLine(int level, NpcType type) {
        if (level <= 0) return "Du bist hier noch als unbekannter Reisender unterwegs.";
        if (level < 10) return "Du bist noch am Anfang. " + levelHint(type, 0);
        if (level < 30) return "Du hast bereits einige Erfahrungen gesammelt. " + levelHint(type, 1);
        if (level < 60) return "Dein Ruf eilt dir voraus. " + levelHint(type, 2);
        return "Du gehörst inzwischen zu den erfahrenen Abenteurern. " + levelHint(type, 3);
    }

    private int levelBand(int level) {
        if (level < 10) return 0;
        if (level < 30) return 1;
        if (level < 60) return 2;
        return 3;
    }

    private String levelHint(NpcType type, int band) {
        return switch (type) {
            case SHOP -> switch (band) {
                case 0 -> "Lerne erst, was du wirklich brauchst.";
                case 1 -> "Du kannst bereits gezielter einkaufen.";
                case 2 -> "Du erkennst den Unterschied zwischen Bedarf und Luxus.";
                default -> "Erfahrung zeigt sich oft darin, was man nicht kauft.";
            };
            case QUEST -> switch (band) {
                case 0 -> "Beginne mit Aufgaben, deren Folgen du überblicken kannst.";
                case 1 -> "Anspruchsvollere Aufträge öffnen dir neue Wege.";
                case 2 -> "Einige Aufgaben verlangen inzwischen echte Vorbereitung.";
                default -> "Erfahrene Abenteurer übernehmen oft die Folgen ihrer Entscheidungen.";
            };
            case TRAVEL -> switch (band) {
                case 0 -> "Entdecke zuerst die Wege in deiner Nähe.";
                case 1 -> "Weitere Reisepunkte werden für dich zunehmend wichtig.";
                case 2 -> "Du kannst längere Expeditionen besser planen.";
                default -> "Für erfahrene Reisende wird nicht die Entfernung, sondern das Ziel entscheidend.";
            };
            case BANKER -> switch (band) {
                case 0 -> "Ein kleiner Vorrat ist besser als gar keiner.";
                case 1 -> "Jetzt beginnen größere Vorhaben.";
                case 2 -> "Du kannst deine Ressourcen langfristiger planen.";
                default -> "Erfahrene Abenteurer denken nicht nur an den nächsten Kampf.";
            };
            default -> switch (band) {
                case 0 -> "Lerne zuerst die Grundlagen und beobachte genau.";
                case 1 -> "Mit Erfahrung erkennst du Zusammenhänge schneller.";
                case 2 -> "Du kannst inzwischen anspruchsvollere Aufgaben übernehmen.";
                default -> "Deine Erfahrung erlaubt dir, auch die kleinen Details ernst zu nehmen.";
            };
        };
    }

    private String levelPerspective(int level, NpcType type, int variant) {
        if (level < 10) return "Für dich zählt gerade vor allem, die Grundlagen zu lernen. Komm später wieder – manche Zusammenhänge werden dann klarer.";
        if (level < 30) return "Du bist weit genug, um erste Zusammenhänge zu erkennen. Wenn du weitergehst, wirst du merken, dass dieselben Probleme aus verschiedenen Blickwinkeln aussehen können.";
        if (level < 60) return "Du hast inzwischen genug erlebt, um nicht jede Erklärung sofort zu glauben. Genau das ist nützlich: Vergleiche Beobachtungen und entscheide selbst, was daraus folgt.";
        return "Du hast genug Erfahrung gesammelt, um Details ernst zu nehmen. Vielleicht kennst du bereits eine andere Erklärung – erzähl mir ruhig, was du daraus schließt.";
    }

    @Override
    public void close() {
        progress.shutdown();
    }
}
