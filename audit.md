# PixelRPG – Audit / Masterplan NPC, Dialogue, Lore & World
## Verbindliche Arbeitsgrundlage für den Branch `Dialore`

**Status:** Verbindlicher Masterplan  
**Branch:** `Dialore`  
**Referenz:** `main`  
**Zweck:** Vollständiger Umbau der NPC-, Dialog-, Lore-, Story- und Weltbelebungs-Systeme

---

## 1. Ziel

PixelRPG wird von einem funktionalen NPC-System zu einer lebendigen Minecraft-RPG-Welt weiterentwickelt.

Das Ziel ist nicht lediglich mehr Dialogtext. Das Ziel ist ein zusammenhängendes System aus:

- NPCs
- Identitäten
- Persönlichkeiten
- Rollen
- Berufen
- Fraktionen
- Beziehungen
- Wissen
- Gerüchten
- Lore
- Story
- Quests
- Weltzuständen
- NPC-Verhalten
- Tagesabläufen
- Strukturen
- Ereignissen
- Konsequenzen
- Spielerwissen
- visueller Darstellung

Die Welt soll sich so anfühlen, als hätte sie unabhängig vom Spieler eine eigene Geschichte, Bevölkerung und Gegenwart.

Der Umbau ist eines der größten Updates von PixelRPG und bildet die Grundlage für zukünftige Inhalte.

---

## 2. Verbindliche Lore-Grundlage

PixelRPG spielt **in Minecraft selbst**.

PixelRPG ist keine separate Fantasywelt, die lediglich von Minecraft inspiriert wurde.

Die Minecraft-Welt und ihre bekannte Lore bilden die Grundlage der PixelRPG-Chronik.

### Canon-Regeln

1. Was Minecraft eindeutig festlegt, bleibt Canon.
2. Was Minecraft bewusst offenlässt, darf PixelRPG erzählerisch ausarbeiten.
3. Was Minecraft nur andeutet, darf PixelRPG aufgreifen und weiterentwickeln.
4. PixelRPG-Erweiterungen dürfen bekannten Minecraft-Canon nicht widersprechen.
5. Community-Theorien sind nicht automatisch Canon.
6. Eigene PixelRPG-Erklärungen müssen als PixelRPG-Canon erkennbar bleiben, wenn Minecraft sie nicht offiziell festlegt.

Der Begriff **„Alte Baumeister“** ist eine PixelRPG-Sammelbezeichnung für untergegangene Zivilisationen und nicht als offizieller Minecraft-Begriff zu behandeln.

---

## 3. Erste Saga

Die erste große Saga beginnt bei der ältesten bekannten Geschichte der Minecraft-Welt und endet mit dem Tod des Enderdrachen.

Der Enderdrache ist das definitive Ende der ersten Saga.

Es gibt in der ersten Saga:

- keinen geheimen „wahren Boss“ hinter dem Enderdrachen,
- keinen nachträglich erfundenen Superboss,
- keine künstliche Fortsetzung des ersten Endes.

Nach dem Tod des Enderdrachen darf die Welt weiter existieren und zukünftige Zeitalter ermöglichen. Diese Inhalte gehören jedoch nicht mehr zur abgeschlossenen ersten Saga.

Die erste Saga endet mit:

> Das Ende war erreicht.  
> Das Ende vom Ende.

---

# 4. Architekturprinzip

Die zentrale Struktur des Systems lautet:

`NPC → DialogueContext → PlayerState → WorldState → Quest / Story / Profession / Lore`

Der NPC ist nicht mehr nur ein funktionaler Auslöser.

Der NPC ist eine Person innerhalb der Welt.

---

## 5. Bestehende Systeme erhalten

Vor dem Umbau werden bestehende funktionierende Systeme nicht unnötig ersetzt.

Insbesondere werden geprüft und weiterverwendet:

- `NpcManager`
- `RPGNpc`
- `NpcType`
- `NpcFunction`
- `DialogueEngine`
- `DialogueTree`
- `DialogueNode`
- `DialogueOption`
- `DialogueCondition`
- `DialogueTreeService`
- `DialogueProgressStore`
- `DialogueCommand`
- `NpcInteractListener`
- `NpcBehavior`
- `NpcBehaviorRegistry`
- `NpcNameVisibilityService`
- `ExternalSkinService`
- `MannequinSkinResolver`
- `FillerBehavior`

Der vorhandene Dialog-Engine-Ansatz wird ausgebaut, nicht verworfen.

---

# 6. G-Interaktion

Die einzige bewusste Ausnahme vom vollständigen Dialogue-Umbau ist die G-Interaktion.

`G` bleibt:

`G → minecraft:quick_actions → Spielermenü / Übersicht`

Dieses System wird nicht in ein NPC-Dialogsystem umgebaut.

Es dient weiterhin als Spielerübersicht und zentrale Schnellaktionsoberfläche.

---

# 7. NPC-System – Zielmodell

Ein NPC soll langfristig mindestens folgende Informationen besitzen können:

- stabile NPC-ID
- Anzeigename
- Titel
- NPC-Kategorie
- Rolle
- Funktion
- Beruf
- Fraktion
- Herkunft
- Alter/Generation, sofern relevant
- Persönlichkeit
- Charaktereigenschaften
- Wissen
- Geheimnisse
- Beziehungen
- lokale Beziehungen
- Weltbeziehungen
- Story-Relevanz
- Quest-Relevanz
- Lore-Relevanz
- Skin
- Verhalten
- Tagesablauf
- Aufenthaltsorte
- bevorzugte Orte
- Dialoge
- Dialogzustände
- Voraussetzungen
- Konsequenzen
- Fortschritt
- Erinnerungen
- Gerüchte
- Ereignisse

Der NPC muss von seiner Funktion unabhängig als Charakter existieren können.

---

# 8. NPC-Typen und Funktionen trennen

Das aktuelle `NpcType` ist zu stark an Funktionen gekoppelt.

Langfristig müssen folgende Dinge getrennt werden:

### Identität / Kategorie

Beispielsweise:

- Bewohner
- Händler
- Reisender
- Wächter
- Gelehrter
- Suchender
- Handwerker
- Bauer
- Fischer
- Geschichtenerzähler
- Kind
- Ältester
- Story-NPC
- Quest-NPC
- Fraktionsmitglied

### Funktion

Beispielsweise:

- Dialog
- Handel
- Beruf
- Bank
- Reise
- Quest
- Story
- Shop

Ein NPC kann mehrere Funktionen besitzen.

Beispiel:

`Bewohner + Schmied + Quest + Dialog`

---

# 9. Filler-NPCs werden abgeschafft

Filler-NPCs sollen nicht länger als leere Platzhalter existieren.

Sie werden schrittweise zu echten Bewohnern.

Beispiele für Templates:

- `villager.farmer`
- `villager.fisherman`
- `villager.blacksmith`
- `villager.guard`
- `villager.child`
- `villager.elder`
- `villager.traveler`
- `villager.merchant`
- `villager.scholar`
- `villager.cook`
- `villager.tailor`
- `villager.alchemist`
- `villager.mason`
- `villager.woodcutter`

Templates sind Ausgangspunkte, keine vollständigen Charaktere.

Aus Templates werden konkrete Personen.

---

# 10. NPC-Charaktere

Jeder relevante NPC soll eine eigene Identität besitzen.

Ein NPC braucht nicht zwangsläufig eine große persönliche Story.

Aber er soll das Gefühl vermitteln, dass er eine Person und kein Menü ist.

Ein Bewohner kann beispielsweise:

- seinen Beruf erklären,
- über die Umgebung sprechen,
- andere Bewohner kennen,
- Gerüchte kennen,
- Angst vor Monstern haben,
- von Reisen erzählen,
- auf Wetter/Ort/Zeit reagieren,
- den Spieler wiedererkennen,
- auf Storyfortschritt reagieren.

---

# 11. Data-Driven Content

Dialoge und NPC-Inhalte dürfen nicht dauerhaft tausende Zeilen Java-Code erzeugen.

Java soll primär die Engine bereitstellen.

Content soll datengetrieben werden.

Zielstruktur beispielsweise:

`data/dialogues/npc`  
`data/dialogues/story`  
`data/dialogues/professions`  
`data/dialogues/factions`  
`data/dialogues/world`  
`data/dialogues/encounters`

Die konkrete Dateistruktur darf bei der Implementierung angepasst werden, solange das Prinzip erhalten bleibt.

### Grundsatz

**Java = Engine**

**Daten = Content**

---

# 12. DialogueContext

Dialoge benötigen einen zentralen Kontext.

Der `DialogueContext` muss langfristig Zugriff auf relevante Informationen ermöglichen.

Beispiele:

- Spieler
- NPC
- NPC-ID
- Welt
- Position
- Zeit
- Spielerfortschritt
- Queststatus
- Storystatus
- Profession
- Inventar
- bekannte Lore
- bekannte NPCs
- NPC-Wissen
- Weltzustand
- Beziehungen
- Ereignisse
- Gerüchte
- Fraktionen

Dadurch kann derselbe NPC abhängig vom Zustand unterschiedliche Dialoge führen.

---

# 13. Dialogbedingungen

Das Dialogsystem muss Bedingungen unterstützen bzw. erweiterbar dafür sein.

Beispiele:

- Quest gestartet
- Quest abgeschlossen
- Quest fehlgeschlagen
- NPC bereits getroffen
- NPC noch nicht getroffen
- bestimmte Profession
- bestimmtes Item
- bestimmtes Item nicht vorhanden
- Nether betreten
- Ancient City entdeckt
- Stronghold entdeckt
- Endportal gefunden
- End betreten
- Enderdrache lebt
- Enderdrache tot
- Disc 11 gefunden
- Disc 13 gefunden
- Wither beschworen
- Wither besiegt
- Elyan Voss getroffen
- bestimmte Lore bekannt
- bestimmte Fraktion bekannt
- bestimmter Weltzustand

Bedingungen müssen erweiterbar sein.

---

# 14. Dialogaktionen

Dialoge sollen nicht nur Text liefern.

Sie können langfristig Aktionen auslösen.

Beispiele:

- Quest starten
- Quest fortsetzen
- Quest abschließen
- Quest abbrechen
- Beziehung verändern
- NPC kennenlernen
- Wissen hinzufügen
- Lore-Eintrag freischalten
- Gerücht hinzufügen
- Variable setzen
- Weltzustand verändern
- Item geben
- Item entfernen
- Profession freischalten
- Storyfortschritt verändern
- Event auslösen

Alle Aktionen müssen kontrolliert und nachvollziehbar sein.

---

# 15. Dialogfortschritt

NPCs sollen sich an Interaktionen erinnern können.

Beispielsweise:

Erstes Gespräch:

> „Du bist neu hier.“

Später:

> „Ah, du bist wieder da.“

Noch später:

> „Seit deinem letzten Besuch hat sich einiges verändert.“

Dialoge dürfen dadurch zeitlich und erzählerisch wachsen.

---

# 16. Player Knowledge

Das Spiel muss zwischen Weltwissen und Spielerwissen unterscheiden.

Ein Spieler kennt nicht automatisch die komplette Lore.

Es braucht langfristig ein `PlayerKnowledge`-System.

Mögliche Einträge:

- Weltgeschichte
- Nether
- Piglins
- Wither
- Sculk
- Warden
- Ancient City
- Stronghold
- End
- Endermen
- End City
- Ender Dragon
- Elyan Voss
- House of Silence
- Searchers
- Disc 11
- Disc 13
- bestimmte Personen
- bestimmte Ereignisse

---

# 17. NPC Knowledge

Auch NPCs kennen nicht automatisch alles.

Ein NPC kann beispielsweise nur wissen:

- was in seinem Dorf passiert,
- was seine Eltern erzählt haben,
- was er selbst erlebt hat,
- was er von Reisenden gehört hat,
- was seine Fraktion weiß,
- was er glaubt.

Dadurch können widersprüchliche Aussagen entstehen, ohne dass das Spiel selbst seinen Canon verliert.

---

# 18. Gerüchte und Wissen

Es wird zwischen verschiedenen Wissensarten unterschieden.

Zielmodell:

- `CANON`
- `PIXELRPG_CANON`
- `RUMOR`
- `THEORY`
- `UNKNOWN`

Ein NPC darf beispielsweise sagen:

> „Man erzählt sich, dass unter der Erde eine Stadt liegt.“

Das ist ein Gerücht.

Ein anderer NPC kann behaupten:

> „Ich glaube, die Endermen waren einmal Menschen.“

Das ist eine Theorie.

Das Spiel selbst muss diese Aussage nicht automatisch als Wahrheit behandeln.

---

# 19. Weltzustand

Es wird ein zentraler Weltzustand benötigt.

Der Weltzustand soll langfristig Ereignisse abbilden.

Beispiele:

- Netherkrieg bekannt
- Wither erschaffen
- Wither in der Gegenwart
- Ancient City entdeckt
- Warden entdeckt
- Stronghold entdeckt
- Endportal entdeckt
- End betreten
- Enderdrache besiegt

Der Weltzustand verbindet Story, NPCs, Quests und Dialoge.

---

# 20. Lore-System

`lore.md` bleibt die verbindliche Welt- und Canon-Grundlage.

Im Spiel soll daraus langfristig ein strukturiertes Lore-System entstehen.

Lore-Einträge können beispielsweise besitzen:

- ID
- Titel
- Kategorie
- Zeit
- Quelle
- Canon-Status
- Kurzbeschreibung
- vollständigen Text
- Voraussetzungen
- Entdeckungsquelle
- bekannte NPCs
- verbundene Orte
- verbundene Ereignisse

Lore soll durch die Welt entdeckt und durch NPCs vermittelt werden.

---

# 21. story.md

`story.md` ist die vollständige kontinuierliche Erzählung.

Sie beschreibt die Geschichte von der ältesten bekannten Minecraft-Welt bis zum Ende der ersten PixelRPG-Saga.

Sie ist kein Dialogskript.

Sie ist die narrative Gesamtchronik.

Dialoge, Quests und NPCs müssen diese Chronik unterstützen.

---

# 22. Story-NPCs

Besonders wichtige Charaktere müssen vollständig ausgearbeitet werden.

Dazu gehören insbesondere:

- Elyan Voss
- Mitglieder des Hauses der Stille
- Suchende
- wichtige Gelehrte
- wichtige Vertreter der Gilden
- später relevante NPCs der ersten Saga

Story-NPCs brauchen:

- Persönlichkeit
- Vergangenheit
- Motivation
- Wissen
- Grenzen ihres Wissens
- Beziehungen
- eigene Sprache
- wiedererkennbare Dialogstruktur
- Reaktionen auf Storyfortschritt

---

# 23. Elyan Voss

Elyan Voss darf kein einfacher Lore-Erklärer werden.

Er muss als tatsächliche Figur funktionieren.

Seine Funktion:

- Verbindung zu alten Erkenntnissen
- geistiger Ursprung des Hauses der Stille
- Wissenshüter
- Beobachter
- mögliche Quelle wichtiger Hinweise

Sein Wissen muss trotzdem begrenzt sein.

Er kennt nicht automatisch die vollständige Wahrheit.

---

# 24. Haus der Stille

Das Haus der Stille soll eine echte Institution innerhalb der Welt werden.

Mögliche Funktionen:

- Schutz gefährlichen Wissens
- Sammlung alter Aufzeichnungen
- Untersuchung unbekannter Artefakte
- Archivierung
- Beobachtung gefährlicher Phänomene
- Verbindung zu Suchenden

Es darf nicht nur ein Questgeber-Gebäude sein.

---

# 25. Suchende

Die Suchenden werden als eigenständige Gruppe behandelt.

Sie suchen beispielsweise:

- Artefakte
- Wissen
- Ruinen
- Reichtum
- Macht
- Wahrheit

Nicht jeder Suchende muss dieselben Motive besitzen.

Dadurch entstehen unterschiedliche Charaktere und mögliche Konflikte.

---

# 26. Berufe

Die bestehenden Berufe werden vollständig in das Dialogsystem integriert.

Berufs-NPCs sind zuerst Personen und danach Funktionsanbieter.

Ein Spieler spricht beispielsweise mit einem Schmied.

Der Dialog kann anschließend die Schmiede-Funktion öffnen.

Dasselbe gilt für:

- Schmied
- Gelehrter
- Bauer
- Koch
- Schneider
- Alchemist
- Maurer
- Fischer
- Holzfäller

Der funktionale Teil bleibt erhalten.

Die Interaktion davor wird lebendiger.

---

# 27. Gelehrter

Der Gelehrte ist besonders lore-relevant.

Er kennt die Verzauberungskunst und kann über:

- Verzauberungen
- alte Bücher
- Wissen
- Ruinen
- Magie
- historische Erkenntnisse

sprechen.

Die bereits bestehende Funktionalität mit maximalen Verzauberungsstufen bleibt erhalten.

---

# 28. NPC-Verhalten

NPCs sollen langfristig nicht dauerhaft still herumstehen.

Benötigte Verhaltensarten können sein:

- idle
- arbeiten
- spazieren
- reden
- lesen
- essen
- fischen
- farmen
- schmieden
- handeln
- bewachen
- patrouillieren
- reisen
- schlafen
- fliehen
- untersuchen
- zu bestimmten Orten gehen

Das Verhalten muss mit dem Charakter und Beruf verbunden sein.

---

# 29. Tagesabläufe

NPCs sollen langfristig Zeitpläne besitzen können.

Beispiel:

Morgen:
- Haus verlassen

Vormittag:
- Arbeit

Mittag:
- Dorfzentrum

Nachmittag:
- Arbeit

Abend:
- soziale Interaktion

Nacht:
- Haus / Schlafplatz

Nicht jeder NPC braucht denselben Ablauf.

---

# 30. Beziehungen

NPCs sollen Beziehungen zu anderen NPCs und optional zum Spieler besitzen können.

Beispiele:

- Familie
- Freundschaft
- Feindschaft
- Respekt
- Misstrauen
- Mentor/Schüler
- Arbeitgeber/Angestellter
- Fraktionszugehörigkeit

Dadurch können Dialoge auf soziale Beziehungen reagieren.

---

# 31. Fraktionen

Langfristig sollen NPCs Fraktionen angehören können.

Mögliche Fraktionen:

- Dorfgemeinschaft
- Illager
- Gilden
- Haus der Stille
- Suchende
- unabhängige Reisende
- andere zukünftige Gruppen

Fraktionen müssen nicht automatisch moralisch „gut“ oder „böse“ sein.

---

# 32. Strukturbezogene NPCs

Die Welt soll abhängig von ihrer Umgebung bevölkert werden.

Langfristig relevant:

- Dörfer
- Straßen / Wege
- Netherfestungen
- Bastionen
- Strongholds
- Ancient Cities
- End Cities
- Ruinen
- weitere relevante Strukturen

Die Population darf nicht durch einen naiven vollständigen Welt-Scan beim Serverstart erfolgen.

---

# 33. Struktur-NPC-System

Es soll langfristig eine Trennung geben, beispielsweise:

`StructureNpcManager`

oder

`StructureNpcSpawner`

Die konkrete Klassenaufteilung wird erst nach Prüfung der aktuellen Paper-26.2-API festgelegt.

Grundprinzip:

- Strukturen werden bei Bedarf erkannt.
- Chunk-/Struktur-Lebenszyklen werden genutzt.
- NPCs werden nur bei relevanten Bereichen initialisiert.
- Strukturidentität wird persistent gespeichert.
- Doppelte NPCs werden verhindert.
- Bestehender `NpcManager` wird wiederverwendet.

---

# 34. Persistenz

NPCs müssen stabil über Neustarts hinweg bleiben.

Persistiert werden müssen je nach System:

- NPC-ID
- Identität
- Position
- Strukturzugehörigkeit
- Verhalten
- Fortschritt
- Beziehungen
- Wissen
- Storystatus

Der vorhandene Persistenzmechanismus des `NpcManager` soll wiederverwendet bzw. erweitert werden.

---

# 35. Skins und Darstellung

NPCs sollen visuell unterscheidbar werden.

Langfristig:

- individuelle Skins
- Berufskleidung
- Fraktionskleidung
- Story-Skins
- besondere Charakter-Skins
- regionale Unterschiede
- saisonale/eventbezogene Darstellung

Die vorhandenen Systeme:

- `ExternalSkinService`
- `MannequinSkinResolver`

werden als Basis berücksichtigt.

---

# 36. Namen und Titel

NPC-Namen müssen Bestandteil der Identität sein.

Zusätzlich können Titel genutzt werden.

Beispiele:

- „Mara, Fischerin“
- „Edrik, Dorfältester“
- „Tomas, Schmied“
- „Elyan Voss, Bewahrer der Archive“

Namen dürfen nicht nur aus zufälligen Platzhaltern bestehen, wenn ein NPC erzählerische Bedeutung besitzt.

---

# 37. Lore durch NPCs

Lore darf nicht ausschließlich in statischen Menüs liegen.

NPCs sollen Lore erzählen können.

Dabei muss berücksichtigt werden:

- was der NPC weiß,
- was der NPC glaubt,
- was der NPC erlebt hat,
- was der Spieler bereits weiß,
- ob die Information geheim ist,
- ob die Information ein Gerücht ist.

Lore wird dadurch Teil der Welt.

---

# 38. Quests

Quests werden stärker mit Dialogen verbunden.

Ziel:

`Dialog → Quest → Ereignis → Weltzustand → neuer Dialog`

Beispiel:

1. NPC erzählt von einem Problem.
2. Spieler nimmt Quest an.
3. Spieler untersucht Ort.
4. Ereignis wird ausgelöst.
5. NPC reagiert auf das Ergebnis.
6. neue Dialogoption wird freigeschaltet.

---

# 39. Storyfortschritt

Storyfortschritt muss zentral verfügbar sein.

NPCs dürfen auf Ereignisse reagieren, ohne jeweils eigene harte Java-Sonderlogik zu benötigen.

Beispiel:

Vor Ancient City:

> „Unter der Erde soll etwas Altes liegen.“

Nach Entdeckung:

> „Du hast diese Stadt wirklich gefunden?“

Nach Warden-Begegnung:

> „Dann hast du gesehen, was dort unten wacht.“

---

# 40. Gegenwartswelt

Die moderne Welt muss unabhängig von der alten Geschichte funktionieren.

Villager und Illager werden als moderne Zivilisation behandelt.

Illager sind nicht automatisch direkte Nachkommen der alten Zivilisation.

Die moderne Welt erhält:

- Dörfer
- Berufe
- Gilden
- Händler
- Reisende
- Wächter
- Gelehrte
- Suchende
- Handwerker
- Familien
- soziale Beziehungen

---

# 41. Weltbevölkerung

Die Welt soll nach und nach bevölkert werden mit:

- Bewohnern
- Reisenden
- Händlern
- Wachen
- Handwerkern
- Suchenden
- Gelehrten
- Abenteurern
- Story-NPCs
- Fraktionsmitgliedern

Die Bevölkerung muss logisch zum Ort passen.

---

# 42. NPC-Reaktionen auf Welt

NPCs sollen auf die Welt reagieren.

Beispiele:

- Nacht
- Monster
- gefährliche Gebiete
- Nether
- ungewöhnliche Ereignisse
- Storyfortschritt
- Tod wichtiger Personen
- neue Entdeckungen
- Drachen-Tod

Dadurch entsteht der Eindruck einer lebendigen Welt.

---

# 43. End und Enderdrache

Der Enderdrache ist der Abschluss der ersten Saga.

Die Endphase umfasst:

- Stronghold
- Endportal
- End
- Endermen
- End Cities
- Shulker
- Enderdrache
- Endkristalle
- letzter Kampf
- Tod des Enderdrachen

Nach dem Tod des Enderdrachen endet die erste Saga definitiv.

---

# 44. Offene Geheimnisse

Nicht jede Frage muss beantwortet werden.

Mögliche offene Fragen:

- Ursprung der Welt
- vollständiger Ursprung der Endermen
- genaue Geschichte der Stronghold-Erbauer
- vollständige Geschichte der End Cities
- Ursprung des Enderdrachen
- unbekannte Portale
- tiefere Natur des Sculk
- unbekannte Bereiche des Endes
- weitere alte Zivilisationen

Offene Fragen sind ein Feature.

Sie ermöglichen zukünftige Geschichten.

---

# 45. Minecraft-Canon bleibt übergeordnet

Wenn Minecraft zukünftig eine bisher offene Frage eindeutig beantwortet, muss PixelRPG seine entsprechende Ausarbeitung überprüfen.

Offizieller Minecraft-Canon hat Vorrang vor PixelRPG-Erweiterungen.

PixelRPG ergänzt Minecraft.

PixelRPG ersetzt Minecraft nicht.

---

# 46. Resourcepack

Nach Stabilisierung der Engine wird auch die visuelle Darstellung erweitert.

Langfristige Ziele:

- NPC-Skins
- Dialogsymbole
- Lore-Symbole
- Fraktionssymbole
- Quest-Symbole
- Story-Elemente
- besondere UI-Elemente
- weitere Charakterdarstellungen

Das Resourcepack wird erst nach der strukturellen Engine-Arbeit massiv erweitert.

---

# 47. Phasenplan

## Phase 1 – Fundament

Priorität: sehr hoch

Umsetzen:

- NPC-Datenmodell erweitern
- NPC-Typ und Funktion trennen
- DialogueContext
- datengetriebene Dialogdefinitionen
- Dialogbedingungen
- Dialogaktionen
- WorldState-Grundlage
- PlayerKnowledge-Grundlage
- NpcKnowledge-Grundlage
- persistente Dialogfortschritte

Ziel:

Die Engine muss bereit sein, große Content-Mengen aufzunehmen.

---

## Phase 2 – NPCs

Umsetzen:

- Filler-NPCs ersetzen
- NPC-Templates
- Namen
- Titel
- Persönlichkeiten
- Berufe
- Skins
- Verhalten
- Tagesabläufe
- einfache Beziehungen

Ziel:

Die Welt besteht aus echten Personen statt Platzhaltern.

---

## Phase 3 – Welt

Umsetzen:

- WorldState
- PlayerKnowledge
- NpcKnowledge
- Gerüchte
- Fraktionen
- Beziehungen
- strukturabhängige Population
- StructureNpc-System

Ziel:

NPCs und Weltzustand werden miteinander verbunden.

---

## Phase 4 – Lore

Umsetzen:

- Lore-Registry
- Lore-Einträge
- Canon-Status
- Entdeckungen
- Gerüchte
- Theorien
- NPC-Wissen
- Spielerwissen

Ziel:

Die Lore wird spielbar.

---

## Phase 5 – Story

Umsetzen:

- Elyan Voss
- Haus der Stille
- Suchende
- Ancient City
- Nether
- Wither
- Stronghold
- End
- Enderdrache

Ziel:

Die erste Saga wird als spielbare Geschichte erlebbar.

---

## Phase 6 – Berufe

Alle Berufs-NPCs werden vollständig in das Dialogsystem integriert.

Ziel:

Berufe fühlen sich wie Menschen und nicht wie Menüs an.

Die bestehende Funktionalität der Berufe bleibt erhalten.

---

## Phase 7 – Weltbevölkerung

Umsetzen:

- Dörfer
- Reisende
- Händler
- Wächter
- Gilden
- Suchende
- Story-NPCs
- Struktur-NPCs
- regionale Population

Ziel:

Die Welt fühlt sich dauerhaft bewohnt an.

---

## Phase 8 – Präsentation

Umsetzen:

- Dialogdesign
- Namen
- Titel
- Skins
- UI
- Lore-Darstellung
- Story-Darstellung
- Resourcepack
- visuelle Identität der Fraktionen

Ziel:

Die technische Welt wird zu einer erkennbaren RPG-Welt.

---

# 48. Prioritätsregel

Nicht sofort tausende Dialogzeilen schreiben.

Zuerst muss die Architektur stabil sein.

Reihenfolge:

1. Engine
2. Datenmodell
3. Zustände
4. Persistenz
5. NPC-Grundmodell
6. Dialogsystem
7. Bedingungen/Aktionen
8. Wissen/Lore
9. Weltpopulation
10. Charaktere
11. große Story
12. Content-Masse
13. Präsentation

---

# 49. Keine unnötigen Komplett-Rewrites

Bestehender funktionierender Code wird nicht grundlos ersetzt.

Vor jeder Änderung:

1. aktuellen Code prüfen
2. Architektur prüfen
3. `settings.gradle` prüfen
4. `build.gradle` prüfen
5. `main` als Referenz prüfen
6. aktuelle Paper-26.2-API prüfen
7. betroffene Systeme prüfen
8. minimale notwendige Änderung bestimmen
9. bestehende Funktionalität schützen
10. Build/Verifikation ausführen

---

# 50. Technische Plattform

Verbindlich:

- Java 25
- Paper 26.2
- Paperweight Userdev 2.0.0-beta.21
- Mojang-Mappings
- `paper-plugin.yml`
- Adventure Components
- native Paper-Dialog-System
- keine Legacy-Bukkit-APIs
- keine CraftBukkit-Klassen
- kein Legacy-NMS
- keine 1.21.x-Workarounds

---

# 51. Build-Regeln

Bestehende Dependencies bleiben unverändert.

Insbesondere:

- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

Shadow-Relocations bleiben:

- `com.google.gson` → `de.pixelrpg.rpg.libs.gson`
- `com.zaxxer.hikari` → `de.pixelrpg.rpg.libs.hikari`
- `com.mysql` → `de.pixelrpg.rpg.libs.mysql`

Bestehende Build- und Verifikationsprüfungen dürfen nicht entfernt oder abgeschwächt werden.

---

# 52. Branch-Regel

Für diesen Umbau gilt:

**Ausschließlich `Dialore`.**

`main` bleibt Referenz.

`test` bleibt der normale Entwicklungsbranch für andere Arbeiten.

Es wird nicht direkt auf `main` gearbeitet.

---

# 53. Qualitätsregeln

Keine erfundenen Paper-26.2-APIs.

Wenn eine API unklar ist:

1. aktuelle Paper-26.2-API prüfen
2. vorhandenen Projektcode prüfen
3. erst danach implementieren

Keine alten Minecraft-/Paper-Versionen als Referenz verwenden.

Keine unnötigen Workarounds.

Keine unnötigen Refactorings.

Keine funktionierenden Systeme ohne Auftrag entfernen.

---

# 54. Definition of Done

Der Umbau gilt nicht als abgeschlossen, wenn lediglich mehr Dialogtexte vorhanden sind.

Ein Bereich gilt erst als fertig, wenn:

- NPCs echte Identitäten besitzen,
- Dialoge zustandsabhängig funktionieren,
- NPC-Wissen und Spielerwissen getrennt sind,
- Weltzustände berücksichtigt werden,
- relevante Ereignisse Dialoge verändern können,
- NPCs persistent sind,
- Funktionen erhalten bleiben,
- Story und Lore miteinander verbunden sind,
- NPC-Verhalten zum Charakter passt,
- Strukturen sinnvoll bevölkert werden,
- keine unnötigen Legacy-Mechaniken verwendet werden,
- Build und bestehende Verifikationen weiterhin erfolgreich sind.

---

# 55. Zentrale Architektur

Die langfristige Zielarchitektur lautet:

```
Minecraft World
      │
      ▼
   WorldState
      │
      ├───────────────┐
      ▼               ▼
   Structure       Events
      │               │
      ▼               ▼
 NPC Population → NPC State
      │               │
      ▼               ▼
 NPC Identity      Knowledge
      │               │
      └───────┬───────┘
              ▼
       DialogueContext
              │
              ▼
        DialogueEngine
              │
       ┌──────┼──────┐
       ▼      ▼      ▼
     Story   Quest  Profession
       │      │      │
       └──────┼──────┘
              ▼
          PlayerState
              │
              ▼
       Player Knowledge
```

---

# 56. Endziel

PixelRPG soll sich nicht wie ein Server mit NPC-Menüs anfühlen.

PixelRPG soll sich wie eine lebendige Minecraft-RPG-Welt anfühlen.

Der Spieler soll den Eindruck bekommen:

- Die Welt war vor ihm da.
- Die Menschen haben eigene Leben.
- Jeder Ort hat Geschichte.
- Jeder Beruf gehört einer Person.
- Wissen ist verteilt.
- Nicht jeder kennt die Wahrheit.
- Gerüchte können falsch sein.
- Ereignisse verändern die Welt.
- NPCs erinnern sich.
- Orte erzählen Geschichte.
- Die Lore ist überall.
- Die Story entsteht aus der Welt.
- Der Spieler entdeckt die Geschichte, statt sie nur vorgelesen zu bekommen.

Die technische Architektur muss diese Vision ermöglichen.

---

# 57. Verbindliche Leitlinie

Ab jetzt werden NPC-, Dialogue-, Lore-, Story- und Weltänderungen gegen dieses Dokument geprüft.

Wenn eine geplante Implementierung diesem Audit widerspricht, muss die Abweichung bewusst begründet und entschieden werden.

Dieses Dokument ist die Arbeitsgrundlage für den vollständigen NPC-/Dialogue-/Lore-/World-Umbau von PixelRPG.


# 51. Umsetzungsstatus – Dialore

## 51.1 Bereits umgesetzt

- [x] DialogueContext als zentrale Dialogbasis eingeführt.
- [x] persistentes PlayerKnowledge-System eingeführt.
- [x] persistenter WorldState eingeführt.
- [x] NPC-Wissen und NPC-Beziehungen als eigene Stores ergänzt.
- [x] datengetriebener JSON-Dialogloader eingeführt.
- [x] Kontextbedingungen und Dialogaktionen erweitert.
- [x] Filler-NPCs an datengetriebene Bewohnerdialoge angebunden.
- [x] Berufs-NPCs an Dialogbäume vor der Funktionsöffnung angebunden.
- [x] Dialoginhalte für Bewohner-, Rollen- und Berufsgruppen ergänzt.
- [x] strukturbezogene NPC-Verwaltung und Persistenz ergänzt.
- [x] LoreEntry/LoreRegistry als technische Lore-Schicht ergänzt.
- [x] WorldState-Reaktionen auf Dimensionen, Advancements, Wither und Enderdrachen ergänzt.
- [x] NPC-Tagesablauf-Service als technische Basis ergänzt.
- [x] bestehende Build-/API-Grenzprüfungen beibehalten.

## 51.2 Aktueller Ausbau

- [ ] vollständige Migration aller bestehenden Hardcode-Dialoge
- [ ] vollständige NPC-Identitäts-/Template-Pipeline
- [ ] vollständige Beziehungen zwischen NPCs, Spieler und Fraktionen
- [ ] vollständige Quest-/Story-Verknüpfung mit Dialogaktionen
- [ ] vollständige Lore-Freischaltung aus Dialogen und Entdeckungen
- [ ] vollständige strukturabhängige Population mit belastbarer Duplikatvermeidung
- [ ] echte ortsbezogene Tagesabläufe statt technischer Bewegungs-Platzhalter
- [ ] vollständige Story-NPC-Ausarbeitung für Elyan Voss, Haus der Stille und Suchende
- [ ] Reaktionssystem auf Monster, Gefahr, wichtige Weltereignisse und Saga-Fortschritt
- [ ] vollständige Skin-/Titel-/Identitätsintegration
- [ ] Resourcepack-Ausbau
- [ ] End-to-End-CI-Verifikation

## 51.3 CI-Befund

Der GitHub-Actions-Lauf 35547071402 auf Dialore wurde durch concurrency.cancel-in-progress abgebrochen, bevor die eigentliche Build- und Verifikationsphase abgeschlossen werden konnte. Das ist kein bestätigter Java-Compilefehler.

Daher gilt:
- Kein Build-Erfolg wird behauptet.
- Keine Verifikationsstufe wird als bestanden markiert, solange kein vollständiger Lauf vorliegt.
- Nach weiteren Änderungen muss ein vollständiger CI-Lauf abgewartet und bei echten Compile-/Testfehlern anhand des konkreten Logs korrigiert werden.


# 58. Umsetzungsstatus – laufender Vollausbau

## 58.1 Seit Status 51 zusätzlich umgesetzt

- [x] Datengetriebener Dialogloader unterstützt NPC-Wissen, NPC-Treffen, Beziehungen, Dimensionen und Items als Bedingungen.
- [x] Datengetriebener Dialogloader unterstützt NPC-Treffen, Beziehungswerte, NPC-Wissen, Lore-Freischaltungen und Weltzustände als Aktionen.
- [x] Datengetriebene Dialoge können Queststatus prüfen sowie Quests annehmen, abschließen und abbrechen.
- [x] Quest-, Story-, Lore-, NPC- und WorldState-Systeme sind im zentralen DialogueContext-Laufweg verbunden.
- [x] Story-NPCs durchlaufen vor der bestehenden Kapitel-/Storyfunktion einen datengetriebenen Dialog.
- [x] NPC-Profile besitzen eine persistente, datengetriebene Definitionsquelle.
- [x] Beispielprofile für Elyan Voss, Haus der Stille und Suchende ergänzt.
- [x] Data-driven Dialoge für Elyan Voss, Haus der Stille und Suchende ergänzt.
- [x] Strukturentdeckungen können passende Lore-Einträge für den Spieler freischalten.
- [x] Advancement-Ereignisse spiegeln relevante Dimension-, Ancient-City-, Stronghold- und End-Entdeckungen in PlayerKnowledge und WorldState.
- [x] LoreRegistry prüft Voraussetzungen vor einer Freischaltung.
- [x] Paper-26.2-RegistryAccess-Nutzung für Strukturregistrierung wurde auf die aktuelle RegistryAccess/RegistryKey-Struktur gebracht.
- [x] CI-Lauf 35548793733 auf dem aktuellen Stand f41d4e58e1b58b9c5b81ffa01aec4549068a09c6 war erfolgreich.

## 58.2 Noch nicht als fertig markieren

Die folgenden Bereiche bleiben bewusst offen, bis die Implementierung und End-to-End-Verifikation tatsächlich abgeschlossen sind:

- [ ] vollständige Migration aller funktionalen Hardcode-Dialoge
- [ ] vollständige NPC-NPC-Beziehungen und Fraktionsbeziehungen
- [x] vollständige automatische Charakter-/Identitätsgenerierung aus Templates
- [ ] vollständige Quest-/Story-Konsequenzpipeline ohne Sonderlogik
- [ ] vollständige Lore-Präsentation im Spiel
- [ ] belastbare Strukturpopulation inklusive Wiederanlauf-/Konfliktfällen
- [x] echte ortsbezogene Tagesabläufe mit konfigurierten Zielorten
- [ ] vollständiges Reaktionssystem auf Monster, Gefahr und Weltveränderungen
- [ ] vollständige Skin-/Titel-/Darstellungsintegration
- [ ] vollständige Weltbevölkerung für alle vorgesehenen Struktur- und Regionaltypen
- [ ] Resourcepack-Ausbau
- [ ] abschließende End-to-End-CI-Verifikation nach dem aktuellen Änderungsstand

## 58.3 Verbindliche Qualitätsregel

Kein Punkt wird als abgeschlossen markiert, nur weil die technische Basis vorhanden ist. Ein Punkt wird erst auf [x] gesetzt, wenn seine konkrete Funktion im bestehenden PixelRPG-System implementiert, in die vorhandene Architektur integriert und durch den Build-/Verifikationslauf bestätigt wurde.

Der Audit-Stand bleibt damit bewusst unter 100 %, bis alle offenen Punkte tatsächlich abgeschlossen sind.

# 59. Verifikationshinweis

Der Build des damaligen Stands wurde anschließend korrigiert und mit CI-Lauf 35548793733 erfolgreich verifiziert. Für nachfolgende Änderungen bleibt eine neue End-to-End-Verifikation erforderlich.


# 60. Verifikationsstand nach Build-Fix

- [x] Paper-26.2-Import für `EntityConstructEvent` korrigiert.
- [x] CI-Lauf 35548793733 erfolgreich abgeschlossen.
- [x] NPC-Präsentationsservice integriert; Titel werden aus NPC-Profilen auf die bestehende Mannequin-Darstellung angewendet.
- [x] Struktur-NPCs verwenden dieselbe Präsentationspipeline.
- [ ] Resourcepack und vollständige visuelle Fraktions-/Storydarstellung bleiben bis zur tatsächlichen Asset-Integration offen.
