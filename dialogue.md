# Forensische Analyse des Dialog- und Interaktionssystems

Branch: test
Stand: aktueller test-Branch
Ziel dieser Analyse: Das tatsächlich vorhandene Spielerlebnis dokumentieren. Also: Wer spricht mit dem Spieler, was wird gesagt, welche Antworten/Aktionen gibt es, wie viele Schritte gibt es, welche Bedingungen und Verzweigungen existieren, wann wird eine Quest angeboten und wann verlässt der Dialog das Dialogsystem.

## 1. Gesamtbefund

Das Projekt hat aktuell zwei unterschiedliche Dialogebenen.

1. Ein aktives natives Dialog-/Menüsystem über DialogueEngine und die konkreten Feature-Dialoge.
2. Eine vorbereitete DialogueTree-Infrastruktur mit Nodes, Antworten, Conditions, Actions und persistentem Fortschritt.

Das aktive System ist überwiegend ein interaktives Menü:

    NPC anklicken
      -> NpcInteractListener
      -> NpcManager
      -> NpcBehaviorRegistry
      -> konkretes NpcBehavior
      -> natives Dialogfenster
      -> Button
      -> Funktion / weiteres Dialogfenster / GUI / Buch / Inventar

Es ist derzeit nicht überwiegend das klassische RPG-Gespräch:

    NPC sagt etwas
      -> Spieler antwortet
      -> NPC reagiert
      -> weitere Antwort
      -> Questangebot
      -> Ja/Nein
      -> Questannahme
      -> Wiederholungsdialog

Die zweite Struktur ist technisch vorbereitet, aber im normalen NPC-Runtime-Pfad aktuell nicht mit konkreten produktiven DialogueTrees gefüllt.

## 2. Technische Dialogbasis

Datei: src/main/java/de/pixelrpg/rpg/dialogue/DialogueEngine.java

DialogueEngine ist die zentrale Factory für native Paper/Minecraft-Dialoge.

Vorhandene Formen:

### Notice

Titel + Text + Schließen.

Typische Texte:
- „Du musst zuerst registriertes Rathausmitglied sein.“
- „Du bist noch nicht registriert.“
- „Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.“

### Multi Action

Titel + mehrere Textkörper + mehrere Buttons + optional Zurück + Schließen.

Das ist die häufigste Form und bildet aktuell meistens Menüs.

### Number Input

Titel + Erklärung + Zahlenfeld + Bestätigen/Abbrechen.

Aktuell unter anderem für Bank-Einzahlung und Bank-Auszahlung.

### Text Input

Titel + Textfeld + Bestätigen/Abbrechen.

Beispielsweise für Gildennamen und Begleiternamen.

### Confirmation

Titel + Warnung + Ja + Nein.

Aktuell konkret bei der unwiderruflichen Aufhebung der PixelRPG-Registrierung.

## 3. Der echte DialogueTree

Dateien:
- DialogueTree.java
- DialogueNode.java
- DialogueOption.java
- DialogueCondition.java
- DialogueTreeService.java
- DialogueProgressStore.java

### DialogueTree

Ein Tree besitzt:
- ID
- Start-Node
- mehrere Nodes

### DialogueNode

Eine Gesprächsstufe besitzt:
- ID
- Titel
- Body/Text
- mehrere Optionen
- once

### DialogueOption

Eine Spielerantwort besitzt:
- sichtbares Label
- Condition
- nächste Node
- Action
- completesCurrentNode

Damit kann ein echter Gesprächsbaum gebaut werden.

Beispiel:

    NPC:
    „Was führt dich in die Stadt?“

    [Ich suche Arbeit.]
    [Ich möchte mehr über die Stadt erfahren.]
    [Nur Neugier.]

    Antwort 1 -> Quest-/Arbeits-Node
    Antwort 2 -> Lore-Node
    Antwort 3 -> Gesprächsende

### DialogueCondition

Aktuell technisch nur Player -> boolean.

Damit kann theoretisch geprüft werden:
- registriert
- Level
- Quest aktiv
- Quest abgeschlossen
- Beruf gelernt
- Gegenstand vorhanden
- Storyfortschritt
- Permission
- sonstiger Spielerzustand

Es gibt aber noch keine umfangreiche Bibliothek fertiger benannter Gesprächsbedingungen.

### DialogueProgressStore

Speichert:
- seen
- completed

Datei:
dialogue-progress.yml

Ein Node mit once=true kann nach dem ersten Anzeigen blockiert werden.

## 4. Wichtigster Befund zur Runtime

Der normale NPC-Pfad ruft nicht automatisch DialogueTreeService auf.

Der reale Pfad ist:

    PlayerInteractEntityEvent
      -> NpcInteractListener
      -> NpcManager
      -> NpcBehaviorRegistry
      -> NpcBehavior
      -> Feature-Dialog

Die konkrete Funktion entscheidet anschließend selbst, welchen Dialog sie öffnet.

Damit ist DialogueTree aktuell eine vorhandene, aber nicht als allgemeines NPC-Gespräch eingesetzte Schicht.

## 5. NPC-Typen

Aktuell existieren 16 NPC-Typen:

1. RECEPTION
2. PROFESSION_BLACKSMITH
3. PROFESSION_SCHOLAR
4. PROFESSION_FARMER
5. PROFESSION_COOK
6. PROFESSION_TAILOR
7. PROFESSION_ALCHEMIST
8. PROFESSION_MASON
9. PROFESSION_FISHERMAN
10. PROFESSION_WOODCUTTER
11. QUEST
12. SHOP
13. TRAVEL
14. FILLER
15. STORY
16. BANKER

Davon sind 9 Berufslehrer.

## 6. RECEPTION

Datei: ReceptionDialog.java

Titel:
RPG-Registrierung

Text:

„Willkommen. Hier kannst du dein PixelRPG-Profil registrieren und verwalten.“

Status:
- „Mitglied“
- „Nicht registriert“

### Nicht registriert

Button:
- Registrieren

Nach Registrierung wird das Dialogfenster erneut geöffnet.

### Registriert

Buttons:
- PixelRPG-Registrierung aufheben
- Party
- Gilde
- Scoreboard einschalten/ausschalten

### Registrierung löschen

Titel:
„Registrierung aufheben?“

Text:
„Warnung: Setzt deinen gesamten PixelRPG-Fortschritt zurück.“

Antworten:
- „Ja, Registrierung unwiderruflich aufheben“
- „Nein, abbrechen“

Das ist eine echte Ja/Nein-Entscheidung.

Struktur:

    RPG-Registrierung
      -> Registrieren
      -> Party
      -> Gilde
      -> Scoreboard
      -> Registrierung aufheben
           -> Ja / Nein

## 7. QUEST-NPC

Datei: QuestBehavior.java

Voraussetzung:
Der Spieler muss registriertes Rathausmitglied sein.

Fehlerdialog:
Titel „Questgeber“
Text „Du musst zuerst registriertes Rathausmitglied sein.“
Button Schließen.

### Stufe 1: Levelbereich

Titel:
Questgeber

Text:
„Wähle zuerst deinen gewünschten Levelbereich. Danach siehst du die Quests dieses Bereichs.“

Zusätzlich:
„Dein aktuelles Level: X“

Buttons:
- dynamische Levelbereiche

Die Farbe zeigt:
- Grün = aktueller Bereich
- Gelb = freigeschaltet
- Rot = gesperrt

### Stufe 2: Questliste

Titel:
Level X–Y

Text:
„Questbereich Level X–Y“

und:
„Quests werden bis zu X Level vor ihrem empfohlenen Level freigeschaltet.“

Jede Quest wird als Button dargestellt:
- Status
- Titel
- empfohlenes Level

Status:
- Verfügbar
- [Aktiv] Angenommen
- Nicht verfügbar
- Abgeschlossen

Zusätzlich:
- Zurück

### Stufe 3: Questdetails

Zeigt:
- Questbeschreibung
- Ziel
- bei Collect-Quests das benötigte Item
- empfohlenes Level
- Freischaltlevel
- Status
- Belohnung

Belohnung:
X Gold, Y EP

### Quest verfügbar

Button:
- Quest annehmen

Nach Annahme:
Quest wird aktiviert und die Questliste wird erneut geöffnet.

### Quest aktiv

Buttons:
- Quest abgeben
- Quest abbrechen
- Zurück

### Quest abgeschlossen / nicht verfügbar

Keine Annahme.

Kompletter Ablauf:

    Questgeber
      -> Levelbereich
      -> Questliste
      -> Questdetails
      -> Quest annehmen
         oder Quest abgeben
         oder Quest abbrechen

Das ist derzeit ein dreistufiges Quest-Menü, kein mehrzeiliges NPC-Gespräch.

## 8. FILLER-NPC

Datei: FillerBehavior.java

Dieser NPC ist ein Questziel und kein klassischer Questgeber.

Nicht registriert:
„Du bist noch nicht registriert.“

Registriert:
„Du hast den Ziel-NPC erreicht.“

Button:
Schließen

Zusätzlich wird beim Kontakt Questfortschritt für das NPC-Ziel ausgelöst.

Ablauf:

    Filler
      -> Ziel erreicht
      -> Schließen

Keine Antwort, keine Verzweigung, kein Questangebot.

## 9. BERUFSLEHRER

Betroffen:
- Schmied
- Gelehrter
- Bauer
- Koch
- Schneider
- Alchemist
- Steinmetz
- Fischer
- Holzfäller

Dateien:
- ProfessionTrainerBehavior.java
- ProfessionDialog.java

### Beruf noch nicht gelernt

Titel:
Berufsname

Text:
- Berufs-Beschreibung
- „Du hast den Beruf X noch nicht erlernt.“

Buttons:
- X erlernen
- Schließen

Nach „X erlernen“:
Der Beruf wird gelernt und die Rezeptübersicht des Lehrers geöffnet.

### Beruf bereits gelernt

Titel:
Berufsname

Text:
- Beschreibung
- „Level X/MAX“

Buttons, wenn Inhalte vorhanden:
- Rezepte (X)
- Berufsquests (X)
- Zurück

## 10. Beruf -> Rezepte

Stufe:
Rezeptübersicht

Dann:
Rezeptkategorien

Text:
„Wähle eine Rezeptkategorie für X.“

und:
„Dein Beruflevel: X/MAX“

Danach:
- Kategorie A
- Kategorie B
- Kategorie C
- Zurück

Danach:
Rezeptliste.

Jedes Rezept zeigt:
- Name
- freigeschaltet / gesperrt

Danach:
Rezeptdetails.

## 11. Rezeptdetails

Zeigt:
- Ergebnis
- Kategorie
- benötigtes Berufslevel
- Materialien
- zusätzliche PixelRPG-Itemkosten
- Questvoraussetzung, wenn vorhanden
- Freischaltpreis, wenn vorhanden
- Hinweis, wenn der Beruf noch nicht gelernt wurde

Je nach Zustand:
- Herstellen
- Rezept freischalten
- Rezept über Quest freischalten
- Zurück

Damit ist der Rezeptpfad:

    Beruf
      -> Rezepte
      -> Kategorie
      -> Rezeptliste
      -> Rezeptdetails
      -> Herstellen / Freischalten

## 12. Berufsquests

Pfad:

    Beruf
      -> Berufsquests
      -> Questdetails
      -> Quest annehmen / abgeben / abbrechen

Die Questliste zeigt:
- abgeschlossen
- aktiv
- verfügbar
- gesperrt

Questdetails zeigen:
- Beschreibung
- Ziel/Fortschritt
- benötigtes Berufslevel
- benötigtes Charakterlevel
- Belohnung

Mögliche Aktionen:
- Quest annehmen
- Quest abgeben
- Quest abbrechen
- Zurück

## 13. SHOP-NPC

Datei: ShopBehavior.java

Voraussetzung:
registrierter Spieler.

Titel:
Händler

Text:
„Öffne den PixelRPG-Shop dieses Händlers.“

Button:
Shop öffnen

Danach wird ShopGUI geöffnet.

Ablauf:

    Händler
      -> Shop öffnen
      -> ShopGUI

Kein weiterer NPC-Dialog.

## 14. TRAVEL-NPC

Dateien:
- TravelBehavior.java
- TravelDialog.java

### Erster Besuch

Titel:
Reisepunkt freigeschaltet

Text:
„NPC-Name wurde als Reisepunkt freigeschaltet.“

Button:
Schließen

Der Waypoint wird dabei freigeschaltet.

### Späterer Besuch

Titel:
Reisen

Text:
„Wähle einen freigeschalteten Reisepunkt.“

und:
„Nicht entdeckte Orte werden automatisch freigeschaltet, sobald du sie besuchst.“

Buttons:
- alle anderen freigeschalteten Reise-NPCs

Klick auf Ziel:
- sichere Ankunftsposition suchen
- teleportieren
- Dialog schließen

Keine Ja/Nein-Entscheidung.

## 15. BANKER

Dateien:
- BankerBehavior.java
- GuildBankAccessDialog.java
- BankDialog.java

Nicht registriert:
Titel Bank
Text:
„Du musst zuerst registriertes Rathausmitglied sein.“

### Ohne Gilde

Titel:
Bank

Text:
„Kontostand: X Gold“

und:
„Das Bankfach und das separate Handelsfach werden unabhängig voneinander gespeichert.“

Buttons:
- Einzahlen
- Auszahlen
- Bankfach öffnen
- Handelsfach
- Handelsdepot

### Mit Gilde

Titel:
Bank

Text:
„Persönliches Bankfach und gemeinsame Gildenbank.“

Buttons:
- Persönliche Bank
- Gildenbank

Das ist eine echte erste Verzweigung.

### Einzahlen

Titel:
Geld einzahlen

Text:
„Wähle den Betrag, den du einzahlen möchtest.“

und:
„Verfügbar: X Gold“

Input:
Betrag

Buttons:
- Einzahlen
- Abbrechen

### Auszahlen

Titel:
Geld auszahlen

Text:
„Wähle den Betrag, den du auszahlen möchtest.“

und:
„Kontostand: X Gold“

Input:
Betrag

Buttons:
- Auszahlen
- Abbrechen

Danach können Bankfach und Handelsfach in Inventar-GUIs wechseln.

## 16. STORY-NPC

Dateien:
- StoryBehavior.java
- StoryNpcDialogue.java
- StoryManager.java
- StoryBookFactory.java

### Neues Kapitel vorhanden

Titel:
Geschichte

Text:
Kapitel-Titel

Button:
Kapitel lesen

Danach:
- Written Book wird geöffnet
- StoryChapter wird abgeschlossen
- StoryChapterIndex wird aktualisiert
- mögliche EP werden vergeben

Das eigentliche Kapitel besteht aus mehreren dialogueLines aus story.yml.

### Kein neues Kapitel

Titel:
Geschichte

Text:
„Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.“

Button:
Schließen

Wichtig:
Hier existiert kein echter verzweigter NPC-Dialog.

### Default-Story

Wenn keine Storydatei existiert, wird aktuell erzeugt:

Titel:
The Summoning

Text:
„You awaken in a world that is not your own.“
„The Guild recognizes potential in you.“
„Register at the Reception to begin your journey.“

Belohnung:
100 XP

Diese Texte erscheinen im Buch, nicht als einzelne NPC-Antworten.

## 17. STORY-FORTSCHRITT

StoryManager verwendet PlayerProfile.storyChapterIndex.

Der nächste Abschnitt ist immer:

    aktueller Index + 1

Die Story ist damit aktuell linear:

    Kapitel 0
      -> Kapitel 1
      -> Kapitel 2
      -> Kapitel 3
      -> ...

Keine Story-Branches.

## 18. G-CHARAKTERKARTE

Das G-System ist ein separates natives Dialogsystem und kein NPC-Gespräch.

Einstieg:

    G
      -> Character Card

Bereiche:
- Charakterprofil
- aktive Quests
- Begleiter
- Berufe
- Gilde
- Schließen

### Charakterprofil

Zeigt:
- Name
- Level
- Erfahrung
- Lebenspunkte
- Rüstung
- Bewegungsgeschwindigkeit
- Reichweite
- kritische Trefferchance
- kritischer Schaden
- Lebensraub
- Angriffskraft

Aktionen:
- Gegenstand binden
- Zurück
- Schließen

### Aktive Quests

Wenn leer:
„Du hast aktuell keine aktiven Quests.“

Sonst:
„Aktive Quests: X/Y“

Questdetails zeigen:
- Titel
- Beschreibung
- Ziel/Fortschritt
- Typ
- Berufsanforderung
- Belohnungen
- Zeit verbleibend bei Ablauf

Aktionen:
- Quest abbrechen
- Zurück

### Begleiter

Je nach Zustand:
- Rufen: Name
- Wegschicken: Name
- Ausrüstung: Name
- Umbenennen: Name

Umbenennen:
Titel:
Begleiter umbenennen

Text:
„Vergib einen Namen für deinen Begleiter.“

Input:
Neuer Name

Buttons:
- Umbenennen
- Abbrechen

### Berufe

Text:
„Wähle einen Beruf, um Rezepte, Berufsquests und Herstellungsdetails zu sehen.“

Danach:
- jeder Beruf
- Level oder nicht erlernt

### Gilde

Ohne Gilde:

Titel:
Gilde gründen

Text:
„Eine Gilde ist eure gemeinsame Gemeinschaft und kann später eine eigene, manuell verwaltete Stadt als Basis besitzen.“

Voraussetzung:
Level 20 und 2.500 Gold.

Input:
Gildenname

Buttons:
- Gilde gründen – 2.500 Gold
- Abbrechen

Bei vorhandener Gilde:
- Gildenname
- Rolle
- Mitgliederzahl
- Verwaltungshinweis
- Mitglieder anzeigen
- Schließen

## 19. Wie viele Stufen hat das System?

Es gibt keine globale feste Zahl.

| System | typische Tiefe | Verzweigung |
|---|---:|---|
| Reception | 2–3 | Registrierung, Party, Gilde, Scoreboard, Löschen |
| Questgeber | 3 | Levelbereich -> Quest -> Details |
| Filler | 1 | keine |
| Berufslehrer | 1 bis 5+ | Beruf -> Rezepte/Quests -> Kategorie -> Rezept -> Aktion |
| Händler | 1 | Dialog -> ShopGUI |
| Reisepunkt | 1–2 | Erstfreischaltung oder Reiseauswahl |
| Banker | 2–4 | Bank -> persönlich/Gilde -> Ein-/Auszahlung/Fächer |
| Story | 1 -> Buch | linear |
| G-Charakterkarte | mehrere | Profil/Quests/Begleiter/Berufe/Gilde |
| DialogueTree | theoretisch beliebig | echte Node-Verzweigungen |

Die Tiefe bei Berufen ist dynamisch und hängt von Kategorien, Rezepten und Quests ab.

## 20. Echte Ja/Nein-Dialoge

Aktuell ist die klare Ja/Nein-Entscheidung:

    Registrierung aufheben?
      -> Ja, Registrierung unwiderruflich aufheben
      -> Nein, abbrechen

Andere Aktionen sind meistens direkte Buttons und keine Bestätigungsfragen.

Beispiele:
- Quest annehmen
- Quest abbrechen
- Beruf erlernen
- Rezept freischalten
- Shop öffnen
- Reisen
- Bankfach öffnen

Diese besitzen aktuell überwiegend keine zusätzliche „Bist du sicher?“-Stufe.

## 21. Quest-Angebote

Es gibt zwei zentrale Quest-Angebotsstellen.

### Normaler Questgeber

    Questgeber
      -> Levelbereich
      -> Quest
      -> Questdetails
      -> Quest annehmen

### Berufsquests

    Beruf
      -> Berufsquests
      -> Questdetails
      -> Quest annehmen

Die eigentlichen Questtexte liegen nicht komplett in den Dialogklassen. Sie kommen über Quest, QuestRepository und QuestText aus den Questdaten unter src/main/resources/data/quests/.

## 22. Wo liegen die Dialoginhalte?

Dialogstruktur und viele statische Texte:

src/main/java/de/pixelrpg/rpg/dialogue/

Wichtige Dateien:
- DialogueEngine.java
- ReceptionDialog.java
- ProfessionDialog.java
- TravelDialog.java
- BankDialog.java
- GuildBankAccessDialog.java
- GuildDialog.java
- CompanionDialog.java
- QuickActionsDialogService.java
- QuickActionsDialogListener.java
- StoryNpcDialogue.java

NPC-Einstieg:

src/main/java/de/pixelrpg/rpg/npc/behavior/

- ReceptionBehavior.java
- QuestBehavior.java
- FillerBehavior.java
- ProfessionTrainerBehavior.java
- ShopBehavior.java
- TravelBehavior.java
- BankerBehavior.java
- StoryBehavior.java

Story:
- StoryManager.java
- StoryChapter.java
- StoryBookFactory.java
- story.yml

Quest:
- QuestRepository
- QuestText
- src/main/resources/data/quests/*.json

Rezepte:
- Profession-/Crafting-System
- dynamische Darstellung in ProfessionDialog

## 23. Was wird statisch gesagt und was dynamisch?

Statische Beispiele:

„Willkommen. Hier kannst du dein PixelRPG-Profil registrieren und verwalten.“

„Du musst zuerst registriertes Rathausmitglied sein.“

„Öffne den PixelRPG-Shop dieses Händlers.“

„Wähle einen freigeschalteten Reisepunkt.“

„Persönliches Bankfach und gemeinsame Gildenbank.“

Dynamische Inhalte:
- NPC-Name
- Questname
- Questbeschreibung
- Questziel
- Belohnung
- Level
- Beruf
- Beruflevel
- Rezept
- Materialien
- Gildenname
- Mitglieder
- Reisepunkte
- Begleiter
- Storykapitel

## 24. Erstbesuch vs. späterer Besuch

Es gibt keinen globalen First-Talk-/Repeat-Talk-Mechanismus.

Jeder Feature-NPC entscheidet selbst.

### Reise-NPC
Erstbesuch:
Reisepunkt freischalten.

Später:
Reiseauswahl.

### Berufslehrer
Erstbesuch:
Beruf erlernen.

Später:
Berufsmenü.

### Story-NPC
Solange Kapitel vorhanden:
nächstes Kapitel.

Danach:
„Für dich gibt es momentan kein neues Kapitel. Kehre später zu diesem NPC zurück.“

### Questgeber
Bei jedem Besuch:
Questübersicht.

### Filler
Bei jedem Besuch:
„Du hast den Ziel-NPC erreicht.“

## 25. Was fehlt für ein klassisches RPG-NPC-Gespräch?

Wenn das gewünschte Spielerlebnis lautet:

    NPC:
    „Halt. Ich kenne dich nicht.“

    [Ich bin neu hier.]
    [Das geht dich nichts an.]
    [Ich suche Arbeit.]

    -> Antwort verändert den nächsten NPC-Text
    -> ggf. Questangebot
    -> Ja/Nein
    -> Questannahme
    -> Wiederholungsdialog bei aktivem Queststatus
    -> Abschlussdialog nach Questende

dann fehlen als Content-Schicht vor allem:

- NPC-spezifische Gesprächsbäume
- Erstgespräch
- Wiederholungsdialog
- aktive-Quest-Dialoge
- Abschlussdialoge
- echte Spielerantworten
- Bedingungen pro Antwort
- Questangebote als Gesprächs-Node
- Ja/Nein-Questannahme
- Actions für Quest, Belohnung, Story, Reputation usw.

Die technische DialogueTree-Basis kann diese Struktur bereits grundsätzlich modellieren.

## 26. Was bereits vorhanden ist

Vorhanden:
- native Paper Dialoge
- Titel
- Text
- Buttons
- Conditions
- Actions
- nextNode
- once
- Seen/Completed-Persistenz
- Questintegration
- Storyintegration
- Spielerprofilintegration
- Feature-Dialoge für Bank, Berufe, Reisen, Shops, Gilde, Begleiter

Das Rendering muss daher nicht neu erfunden werden.

## 27. Was ein zukünftiger echter NPC-Tree enthalten sollte

Beispiel:

    NPC: Wache

    Node: greeting
    „Halt. Ich kenne dich nicht.“

    Antworten:
    1. „Ich bin neu in der Stadt.“
       -> lore
    2. „Das geht dich nichts an.“
       -> hostile
    3. „Ich suche Arbeit.“
       -> job

    Node: job
    „Arbeit? Vielleicht kann ich dir helfen.“

    Antworten:
    1. „Was hast du?“
       -> quest_offer
    2. „Nein, danke.“
       -> end

    Node: quest_offer
    „Die Straße nach Norden wird von Kreaturen blockiert.“

    Antworten:
    1. „Ja, ich helfe.“
       -> accept
    2. „Nein, danke.“
       -> declined

    Node: accept
    Action:
    Quest annehmen

    Text:
    „Gut. Geh zum Nordtor und untersuche die Straße.“

Das ist genau die Form von NPC-Gespräch, die der aktuelle DialogueTree technisch vorbereiten kann.

## 28. Gesamtarchitektur

Aktuell:

    PLAYER
      |
      +-- G-Taste
      |     -> Character Card
      |        -> Profil
      |        -> Quests
      |        -> Begleiter
      |        -> Berufe
      |        -> Gilde
      |
      +-- NPC-Klick
            -> PlayerInteractEntityEvent
            -> NpcManager
            -> NpcBehaviorRegistry
            -> NpcBehavior
                 +-> ReceptionDialog
                 +-> QuestBehavior/Dialog
                 +-> ProfessionTrainerBehavior/ProfessionDialog
                 +-> ShopBehavior/ShopGUI
                 +-> TravelBehavior/TravelDialog
                 +-> BankerBehavior/BankDialog
                 +-> StoryBehavior/StoryNpcDialogue/StoryBook
                 +-> FillerBehavior

Parallel vorhanden:

    DialogueTree
      -> DialogueNode
      -> DialogueOption
      -> DialogueCondition
      -> DialogueAction
      -> DialogueTreeService
      -> DialogueProgressStore

Dieser zweite Pfad ist aktuell nicht die allgemeine NPC-Runtime.

## 29. Endgültiger forensischer Befund

Das aktuelle PixelRPG-Dialogsystem ist bereits ein umfangreiches natives Interaktionssystem, aber inhaltlich überwiegend ein Menü-/Service-System.

Die reale Spielerlogik lautet momentan meistens:

    NPC
      -> Information
      -> Auswahl
      -> Funktion

und nur selten:

    NPC
      -> Gespräch
      -> Spielerantwort
      -> nächste Gesprächsstufe
      -> Verzweigung
      -> Questangebot
      -> Entscheidung
      -> Konsequenz

Die vorhandene DialogueTree-Architektur ist der richtige technische Ansatz für den zweiten Typ, aber die konkreten NPC-Gesprächsinhalte dafür fehlen aktuell.

Für ein echtes RPG-Erlebnis sollte daher nicht die native DialogEngine ersetzt werden. Die fehlende Schicht ist die inhaltliche NPC-Konversation:

    NPC
      -> First Talk
      -> NPC-Text
      -> Spielerantworten
      -> Conditions
      -> Branch
      -> Quest Offer
      -> Ja/Nein
      -> Action
      -> Quest Active
      -> Repeat Talk
      -> Quest Complete Talk

Das ist der tatsächliche Abstand zwischen dem aktuellen System und einem klassischen RPG-NPC-Dialogsystem.

## 30. Relevante Dateien

Dialog:
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueEngine.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueTree.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueNode.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueOption.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueCondition.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueTreeService.java
- src/main/java/de/pixelrpg/rpg/dialogue/DialogueProgressStore.java
- src/main/java/de/pixelrpg/rpg/dialogue/ReceptionDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/ProfessionDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/TravelDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/BankDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/GuildBankAccessDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/GuildDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/CompanionDialog.java
- src/main/java/de/pixelrpg/rpg/dialogue/QuickActionsDialogService.java
- src/main/java/de/pixelrpg/rpg/dialogue/QuickActionsDialogListener.java
- src/main/java/de/pixelrpg/rpg/dialogue/StoryNpcDialogue.java

NPC:
- src/main/java/de/pixelrpg/rpg/npc/NpcInteractListener.java
- src/main/java/de/pixelrpg/rpg/npc/NpcBehaviorRegistry.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/ReceptionBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/QuestBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/FillerBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/ProfessionTrainerBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/ShopBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/TravelBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/BankerBehavior.java
- src/main/java/de/pixelrpg/rpg/npc/behavior/StoryBehavior.java

Story:
- src/main/java/de/pixelrpg/rpg/story/StoryManager.java
- src/main/java/de/pixelrpg/rpg/story/StoryChapter.java
- src/main/java/de/pixelrpg/rpg/story/StoryBookFactory.java

