# PixelRPG – Quest Roadmap

> Verbindliche Ergänzung zur Master-Roadmap für das Quest-System.
>
> Diese Roadmap ersetzt keine bestehenden technischen Regeln. Sie konkretisiert ausschließlich die zukünftige Quest- und Open-World-Struktur.

---

## 1. Grundprinzip

PixelRPG ist eine offene Welt.

Quests dürfen nicht voraussetzen, dass eine frei gewählte Koordinate für den Spieler verständlich oder interessant ist.

Nicht vorgesehen als regulärer Questtyp:

- „Gehe zu X:123 Y:64 Z:456“
- reine Koordinaten-Suche ohne erkennbaren Ort
- künstliche Gebietsgrenzen über Quest-Level
- Reputation-System
- Titel-System

Quests sollen den Spieler stattdessen über erkennbare NPCs, Orte, Dörfer, Gegner, Berufe, Gegenstände und konkrete Handlungen durch die Welt führen.

---

## 2. Quest-Annahme

Ein Spieler kann maximal **5 aktive Quests gleichzeitig** besitzen.

```text
Maximal aktive Quests = 5
```

Das Quest-System muss verhindern, dass eine sechste Quest angenommen wird.

---

## 3. Quest-Freischaltung nach Level

Die vorhandenen Quest-NPCs bleiben die primären Questgeber.

Quests werden nicht ausschließlich ab dem exakten Quest-Level sichtbar.

Eine Quest wird bereits **5 Level unter ihrem eigentlichen Quest-Level** freigeschaltet.

Beispiel:

```text
Quest-Level 20
        ↓
freigeschaltet ab Level 15
        ↓
empfohlen für Level 20
```

Damit kann der Spieler frühzeitig sehen, was als Nächstes kommt, ohne dass jede Quest bereits für deutlich niedrigere Spieler vollständig trivial wird.

Die konkrete Balance der XP-, Gegner- und Belohnungswerte wird beim späteren Volltest angepasst.

---

## 4. Quest-Struktur Level 1–99

Die Spielerprogression läuft über:

```text
Level 1 → 99
```

Level 100 bleibt der reservierte **Grind-Bereich**.

Für die Levelprogression wird langfristig eine große Questmenge benötigt. Ziel ist eine sinnvolle Questabdeckung über die gesamte Progression, statt nur wenige große Questblöcke zu bauen.

Nicht zwingend jedes Level benötigt exakt dieselbe Anzahl an Quests. Entscheidend ist, dass die Levelbereiche sinnvoll miteinander verbunden sind und der Spieler regelmäßig neue Inhalte erhält.

---

## 5. Aktuelle Quest-NPCs

Die bestehenden Quest-NPCs bleiben erhalten.

Sie schalten ihre jeweiligen Questbereiche abhängig vom Spielerlevel frei.

Grundstruktur:

```text
Quest-NPC
   ↓
Spielerlevel prüfen
   ↓
verfügbare Questbereiche
   ↓
Quests anzeigen
   ↓
Quest auswählen
```

Die Questübersicht soll den relevanten Levelbereich sichtbar machen, bevor die konkrete Quest ausgewählt wird.

---

## 6. Neuer Filler-NPC-Typ: Reise-/Orts-NPC

Es wird **ein zusätzlicher generischer NPC-Typ** vorgesehen, der später für „Gehe zu …“-Quests verwendet werden kann.

Dieser NPC ist kein neuer Quest-NPC mit eigenem komplexen System, sondern ein flexibler Weltanker.

Beispiele:

```text
„Gehe zum Dorf Sonnenhain und sprich mit Mira.“

„Reise zum Dorf Eisenfels und finde den dortigen Händler.“

„Finde den Verwalter des Dorfes und frage ihn nach dem Weg.“
```

Der eigentliche Ort wird vom Admin durch Platzierung und Konfiguration festgelegt.

Damit können auch von Spielern gebaute Dörfer sinnvoll in Questketten eingebunden werden.

---

## 7. Dorf-zu-Dorf-Questketten

Dörfer werden als mögliche Quest-Hubs behandelt.

Ein Spieler kann durch eine Quest zu einem anderen Dorf geschickt werden und dort eine neue Questlinie erhalten.

Beispiel:

```text
Dorf A
  ↓
Quest: Reise nach Dorf B
  ↓
NPC in Dorf B
  ↓
neue Questlinie
  ↓
Dorf C
  ↓
weitere Questlinie
```

Das ermöglicht Open-World-Questketten, ohne feste Gebietsgrenzen einzubauen.

---

## 8. Koordinatenquests

Koordinatenquests werden nicht grundsätzlich ausgeschlossen, aber nur für **klar definierte, vom Admin gesetzte Orte/NPCs** verwendet.

Keine Quest soll den Spieler mit einer nackten Koordinate in eine unbekannte Welt schicken.

Der Spieler soll immer einen Kontext bekommen:

- Name des Ortes
- Name des NPCs
- Beschreibung des Ziels
- ggf. Questdialog

---

## 9. Recovery Compass als Quest-Hilfe

Der `minecraft:recovery_compass` wird als Quest-Navigationshilfe vorgesehen.

Er ersetzt keinen normalen Questmarker und soll auch kein permanentes GPS-System werden.

Ziel:

```text
Questziel bekannt
      ↓
Recovery Compass
      ↓
Richtung zum Questziel
```

Der Spieler bekommt damit eine klare Richtung, ohne dass die Open World durch Wegpunkte oder permanente HUD-Marker überladen wird.

Die genaue technische Umsetzung wird beim Quest-System festgelegt.

---

## 10. Questtypen

Der Quest-Pool soll langfristig verschiedene sinnvolle Questtypen enthalten.

Geplant sind insbesondere:

- Töte bestimmte Gegner
- Sammle bestimmte Gegenstände
- Liefere Gegenstände an einen NPC
- Sprich mit einem NPC
- Reise zu einem bekannten Dorf / Ort
- Folge einer Dorf-zu-Dorf-Questkette
- Besuche einen definierten Weltpunkt mit erkennbarem Kontext
- Questketten mit mehreren Schritten
- Story-/Dialogquests
- Beruf-bezogene Aufgaben
- Begleiter-bezogene Aufgaben, sofern später sinnvoll

Reine „Gehe zu XYZ“-Quests ohne Kontext gehören nicht zum gewünschten Standard.

---

## 11. Quest-Navigation

Questziele sollen für den Spieler verständlich sein.

Priorität:

```text
Questbeschreibung
      ↓
NPC / Ort / Ziel eindeutig benennen
      ↓
Recovery Compass als Richtungshilfe
      ↓
Spieler erkundet die Welt selbst
```

PixelRPG soll den Spieler führen, aber nicht die gesamte Welt für ihn automatisch aufdecken.

---

## 12. Quest-Progression und Balance

Die Quest-XP muss mit der Spieler-XP und dem Levelsystem harmonieren.

Besonders wichtig:

- keine zu starke XP-Explosion durch Questketten
- keine triviale Levelprogression ausschließlich durch Quests
- Gegner- und Questlevel müssen zusammenpassen
- Belohnungen müssen mit der restlichen Progression harmonieren
- Quests dürfen das Open-World-System nicht in ein lineares Zonensystem verwandeln

Die endgültigen XP-Werte werden erst nach dem funktionalen Quest-System und realen Tests festgelegt.

---

## 13. Spätere Erweiterungen

Phase C wird vorerst **nicht umgesetzt**.

Erweiterungen wie zusätzliche Endgame-/Post-Progression-Systeme werden erst betrachtet, wenn das Plugin angenommen wurde und ausreichend reale Spielerfahrung vorliegt.

Bis dahin bleibt der Fokus auf:

1. Quest-Grundsystem
2. Levelprogression 1–99
3. Quest-NPCs
4. Filler-/Reise-NPC
5. Dorf-zu-Dorf-Questketten
6. verständliche Questtexte
7. Recovery-Compass-Navigation
8. Balance durch Servertests

---

## 14. Aktueller Arbeitsauftrag

Vor der vollständigen Quest-Implementierung müssen zunächst die vorhandenen Quest-NPCs und Questdaten geprüft und in Levelbereiche gegliedert werden.

Danach:

```text
Quest-NPCs prüfen
      ↓
Levelbereiche definieren
      ↓
Quest-Freischaltung -5 Level
      ↓
Questübersicht überarbeiten
      ↓
5-Quest-Limit sicherstellen
      ↓
Filler-/Reise-NPC vorbereiten
      ↓
Recovery-Compass integrieren
      ↓
Questketten erweitern
      ↓
Level 1–99 mit Inhalten füllen
      ↓
Server-Test
      ↓
Balance / Vereinfachung / Erweiterung
```
