# PixelRPG – Quest & Companion Roadmap

> Verbindliche Ergänzung zur Master-Roadmap für Quest- und Begleiter-System.
>
> Diese Roadmap beschreibt die geplante Open-World-Queststruktur, die Levelprogression 1–99 sowie die langfristige Begleiter-Progression. Sie ist eine Planungsgrundlage und keine Aufforderung, alle Systeme sofort zu implementieren.

---

## 1. Grundprinzip

PixelRPG ist eine offene Welt.

Quests dürfen nicht voraussetzen, dass eine frei gewählte Koordinate für den Spieler verständlich oder interessant ist.

Nicht vorgesehen als regulärer Queststandard:

- „Gehe zu X:123 Y:64 Z:456“ ohne Kontext
- reine Koordinaten-Suche
- künstliche Gebietsgrenzen über Quest-Level
- Reputation-System
- Titel-System

Quests sollen den Spieler stattdessen über erkennbare NPCs, Orte, Dörfer, Gegner, Berufe, Gegenstände und konkrete Handlungen durch die Welt führen.

Der Spieler soll geführt werden, aber die Open World weiterhin selbst erleben.

---

## 2. Quest-Annahme

Ein Spieler kann maximal **5 aktive Quests gleichzeitig** besitzen.

Das Quest-System muss verhindern, dass eine sechste Quest angenommen wird.

Die Begrenzung ist bewusst gewählt: Quests sollen Entscheidungen erzeugen und nicht zu einer riesigen Liste gleichzeitig aktiver Aufgaben werden.

---

## 3. Quest-Freischaltung nach Level

Die vorhandenen Quest-NPCs bleiben die primären Questgeber.

Eine Quest wird bereits **5 Level unter ihrem eigentlichen Quest-Level** freigeschaltet.

Beispiel:

```text
Quest-Level 20
      ↓
freigeschaltet ab Level 15
      ↓
empfohlen für Level 20
```

Damit kann der Spieler frühzeitig erkennen, was als Nächstes kommt, ohne dass die Quest bereits beliebig früh zugänglich wird.

Die konkrete Balance von XP, Gegnern und Belohnungen wird erst durch reale Tests festgelegt.

---

## 4. Quest-Struktur Level 1–99

Die Spielerprogression läuft über:

```text
Level 1 → 99
Level 100 → Grind-Bereich
```

Langfristig wird deshalb eine große Menge an Quests benötigt. Ziel ist nicht, zwanghaft jedem Level exakt dieselbe Anzahl an Quests zu geben, sondern über die gesamte Progression regelmäßig sinnvolle Inhalte anzubieten.

Die Queststruktur soll sich aus mehreren kleinen und größeren Questketten zusammensetzen:

```text
Levelbereich
   ↓
Quest-NPC
   ↓
kleine Aufgaben
   ↓
Questkette
   ↓
neuer Ort / NPC / Handlungsabschnitt
   ↓
weitere Questkette
```

Die Quest-XP darf dabei nicht so hoch ausfallen, dass Spieler ausschließlich durch das Abarbeiten von Quests ohne andere Aktivitäten durch die Progression fliegen.

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
verfügbare Levelbereiche
   ↓
Quests anzeigen
   ↓
Quest auswählen
```

Die Questübersicht soll den relevanten Levelbereich sichtbar machen, bevor die konkrete Quest ausgewählt wird.

Quest-NPCs sollen nicht zu austauschbaren Koordinatengebern werden. Ihre Funktion ist es, erkennbare Personen und Rollen in der Welt darzustellen.

---

## 6. Neuer Filler-NPC-Typ: Reise-/Orts-NPC

Es wird **ein zusätzlicher generischer NPC-Typ** vorgesehen, der später für Reise- und Ortsquests verwendet werden kann.

Er ist bewusst einfach gehalten und soll keinen zweiten großen Questgeber-Framework benötigen.

Beispiele:

```text
„Gehe zum Dorf Sonnenhain und sprich mit Mira.“
„Reise zum Dorf Eisenfels und finde den dortigen Händler.“
„Finde den Verwalter des Dorfes und frage ihn nach dem Weg.“
```

Der Admin kann diesen NPC an einem definierten Ort platzieren und in eine Questkette einbinden.

Damit können auch von Spielern gebaute Dörfer später sinnvoll in Questketten integriert werden.

---

## 7. Dorf-zu-Dorf-Questketten

Dörfer werden als mögliche Quest-Hubs behandelt.

Eine Quest kann den Spieler bewusst von einem Dorf in ein anderes schicken. Dort beginnt nicht zwingend dieselbe Questlinie, sondern es kann eine neue lokale Handlung entstehen.

Beispiel:

```text
Dorf A
  ↓
„Reise nach Dorf B und sprich mit Mira.“
  ↓
NPC in Dorf B
  ↓
lokale Questlinie
  ↓
„Hilf dem Schmied von Dorf B.“
  ↓
weiterer Ort / NPC
```

Auch Spielerdörfer können später über entsprechend gesetzte NPCs in solche Questketten eingebunden werden.

---

## 8. Koordinatenquests

Koordinatenquests werden nicht als allgemeiner Queststandard verwendet.

Eine konkrete Position ist nur sinnvoll, wenn der Spieler weiß, **was** sich dort befindet.

Akzeptabel:

```text
„Gehe zum Dorf Sonnenhain.“
„Sprich dort mit dem Dorfvorsteher.“
```

Nicht akzeptabel:

```text
„Gehe zu X:123 Y:64 Z:456.“
```

Für klar definierte, vom Admin gesetzte Orte darf intern natürlich mit Positionen gearbeitet werden. Diese technische Position soll aber nicht die eigentliche Spielerbeschreibung ersetzen.

---

## 9. Recovery Compass als Quest-Hilfe

Der `minecraft:recovery_compass` soll als Quest-Navigationshilfe dienen.

```text
Questziel bekannt
      ↓
Recovery Compass
      ↓
Richtung zum Ziel
      ↓
Spieler findet den Ort selbst
```

Er ersetzt kein vollständiges GPS-System. Die Open World soll weiterhin selbst erkundet werden.

---

## 10. Questtypen

Der Quest-Pool soll langfristig verschiedene sinnvolle Questtypen enthalten:

- Töte bestimmte Gegner
- Sammle bestimmte Gegenstände
- Liefere Gegenstände an einen NPC
- Sprich mit einem NPC
- Reise zu einem bekannten Dorf / Ort
- Folge einer Dorf-zu-Dorf-Questkette
- Besuche einen definierten Weltpunkt mit erkennbarem Kontext
- Mehrstufige Questketten
- Story-/Dialogquests
- Berufsbezogene Aufgaben
- Erkundungsquests mit erkennbarem Ziel
- Begleiter-bezogene Aufgaben, sofern sie sinnvoll in die Welt passen

Reine „Gehe zu XYZ“-Quests ohne Kontext gehören nicht zum gewünschten Standard.

---

# 11. Begleiter – Grundkonzept

Begleiter werden ein eigenständiges Progressionssystem, das sich an der Spielerprogression orientiert, ohne das Kampfsystem zu zerstören.

Der wichtigste Grundsatz lautet:

> **Begleiter unterstützen den Spieler. Sie ersetzen ihn nicht.**

Begleiter können optisch normale Minecraft-Entities, Tiere, Monster, NPC-/Mannequin-Modelle oder spielerähnliche Modelle sein. Das Aussehen bestimmt nicht automatisch die Stärke.

Wichtig: Die bisher genannten Beispiele **Wolf, Biene, Zombie, Schwein und Warden sind nur Beispiele und keine abschließende Begleiterliste.**

Langfristig soll grundsätzlich eine möglichst große Auswahl aus den tatsächlich in der verwendeten Minecraft-/Paper-Version vorhandenen und technisch geeigneten Mob-/Entity-Typen möglich sein. Die Begleiterarchitektur darf deshalb nicht auf eine feste Handvoll Mob-Klassen zugeschnitten werden.

---

## 12. Begleiter-Pool – alle geeigneten Minecraft-Mobs

Die Auswahl soll sich an den tatsächlich vorhandenen Mob-/Entity-Typen der verwendeten Serverversion orientieren, nicht an einer manuell fest verdrahteten Liste von fünf Beispielen.

Grundidee:

```text
Minecraft Entity-/Mob-Pool
          ↓
technisch geeignete Entity-Typen
          ↓
als Begleiter konfigurierbar
          ↓
Seltenheit + Basiswerte + Skalierung
          ↓
Begleiter
```

Damit können neben klassischen Tieren und Monstern auch ungewöhnliche Begleiter entstehen, sofern die jeweilige Entity technisch als Begleiter sinnvoll dargestellt und kontrolliert werden kann.

Beispiele für mögliche Kategorien:

- Haustiere und Tiere
- neutrale Mobs
- feindliche Mobs
- fliegende Mobs
- aquatische Mobs
- seltene / außergewöhnliche Mobs
- große oder kleine Modelle
- humanoide Mobs
- besondere Entity-Typen, sofern sie für das Begleitersystem technisch geeignet sind
- Custom-/Mannequin-/Spielermodelle für Unique

Die Minecraft-Wiki-Mob-Liste dient dabei als Inspirations- und Referenzquelle. Für die tatsächliche Implementierung gilt ausschließlich, was in der verwendeten Minecraft-/Paper-Version tatsächlich vorhanden und technisch nutzbar ist.

**Keine feste Whitelist nur für Wolf/Biene/Zombie/Schwein/Warden.** Neue geeignete Mobs sollen später durch Konfiguration bzw. das Begleiter-Datenmodell ergänzt werden können, ohne das komplette System umzubauen.

Nicht jede Entity muss automatisch ein Begleiter werden. Boss-, Projektil-, Partikel-, reine Effekt- oder technisch ungeeignete Entity-Typen können ausgeschlossen werden, wenn sie keine sinnvolle Begleiterdarstellung ermöglichen oder die Balance bzw. Performance gefährden.

---

## 13. Begleiter-Seltenheiten

Die geplante Seltenheitsstruktur lautet:

| Seltenheit | Beispiel | Grundidee |
|---|---|---|
| Common | Biene | sehr schwacher, einfacher Begleiter |
| Uncommon | Wolf | solider früher Begleiter |
| Rare | Zombie | stärkerer Begleiter |
| Epic | Schwein | besonderer, perspektivisch reitbarer Begleiter |
| Legendary | Warden | extrem seltener Begleiter, aber weiterhin Spielerunterstützung |
| Unique | Custom-/Mannequin-Begleiter | ausschließlich individuell durch Admin vergeben |

Diese Beispiele definieren **nicht**, welche Mobs ausschließlich zu einer Seltenheit gehören. Jeder geeignete Mob kann abhängig von seiner vorgesehenen Rolle, Verfügbarkeit und Balance einer passenden Seltenheit zugeordnet werden.

Ein Warden muss beispielsweise nicht zwingend der einzige Legendary-Begleiter sein. Ebenso kann ein weiterer ungewöhnlicher Mob Legendary werden, wenn seine Freischaltung und Werte entsprechend selten und kontrolliert gestaltet sind.

---

## 14. Begleiter-Level und Erfahrung

Begleiter besitzen ein eigenes Level und eigene Erfahrung.

```text
Begleiter-Level 1 → 99
Level 100 → Grind-Bereich
```

Ein Begleiter erhält nur Erfahrung, **wenn er aktiv beim Spieler ist**.

```text
Begleiter draußen
    ↓
XP wird gesammelt

Begleiter weggeschickt
    ↓
keine Begleiter-XP
```

Der Begleiter wird nicht automatisch auf das Spielerlevel gesetzt.

---

## 15. Feste Begleiterwerte und Skalierung

Begleiter sollen **keine frei zusammenstellbaren Spielerbuilds** werden.

Jede Begleiterdefinition erhält kontrollierte Basiswerte und eine definierte Skalierung pro Level.

Beispiel:

```text
Mob-Typ
   ↓
Seltenheit
   ↓
Basiswerte
   ↓
Level-Skalierung 1–99
   ↓
finale Begleiterwerte
```

Die Skalierung muss konservativ gewählt werden. Ein Legendary-Begleiter darf nicht dazu führen, dass ein Spielerboss trivial wird.

Die Balance wird immer anhand von:

```text
Spieler
   +
Begleiter
   vs.
Gegner
   vs.
Boss
```

bewertet.

---

## 16. Größen- und Modellfreiheit

Die verwendete Entity muss nicht automatisch in ihrer normalen Größe erscheinen.

Wenn die technische Umsetzung es erlaubt, können Begleiter perspektivisch über kontrollierte Attribute/Modelle angepasst werden.

Beispiel:

```text
Warden
   ↓
Begleiterdefinition
   ↓
kleinere Darstellung
   ↓
„Mini Warden“
```

Wichtig ist die Trennung von **Darstellung und Kampfbalance**. Ein kleiner Warden wird nicht automatisch schwächer oder stärker, nur weil sein Modell kleiner dargestellt wird.

Ebenso kann ein kleiner Mob nicht durch reine Modellgröße zu einem Mini-Boss werden.

---

## 17. Passive Begleiter als erster Schritt

Der erste Begleiter-Ausbau bleibt bewusst **passiv bzw. nicht kämpfend**.

Das bedeutet:

- Begleiter können wie Tiere, Monster oder Spieler aussehen.
- Sie folgen dem Spieler.
- Sie können gerufen und weggeschickt werden.
- Sie benötigen zunächst keine komplexe Kampflogik.
- Das Aussehen ist unabhängig von ihrer Rolle.

Erst wenn dieses Fundament stabil ist, werden aktive Angriffe, KI und Spezialfähigkeiten betrachtet.

---

## 18. Begleiter-Befehle

Die grundlegenden Aktionen sollen einheitlich sein:

```text
Begleiter rufen
Begleiter wegschicken
Begleiter umbenennen
```

Die Umbenennung gilt grundsätzlich nur für normale Begleiter.

### Unique-Ausnahme

Unique-Begleiter können einen festen Namen besitzen:

```text
Admin vergibt Unique-Begleiter
        ↓
Name / Skin / Modell fest definiert
        ↓
Spieler kann Namen nicht ändern
        ↓
nur rufen / wegschicken
```

---

## 19. Unique-Begleiter

Unique ist keine normale Farm-/Drop-Seltenheit.

Unique-Begleiter werden ausschließlich durch einen Admin vergeben.

Mögliche Verwendung:

- besonderes Geschenk
- Eventbelohnung
- Community-Meilenstein
- individueller NPC-/Story-Begleiter
- einmalige Sonderbelohnung

Ein Beispiel:

```text
Unique: „Arven“
Modell: Mannequin / Spieler
Skin: fest definiert
Name: fest definiert
Verhalten: passiv
Besitz: nur durch Admin-Vergabe
```

Unique bedeutet nicht automatisch „unbalanciert“. Auch Unique-Begleiter müssen in die allgemeine Progression passen.

---

## 20. Begleiter-Freischaltung

Begleiter sollen nicht wahllos aus jeder Quest oder jedem Mobdrop fallen.

Mögliche Freischaltungswege:

### Questbelohnung

Besonders geeignet für thematische Common-, Uncommon- und einzelne Rare-Begleiter.

### Besondere Questketten

Geeignet für seltene, thematisch wichtige Begleiter.

### Seltene Inhalte / besondere Gegner

Für höherwertige Begleiter können seltene Freischaltungen über spezielle Inhalte vorgesehen werden.

### Admin-Vergabe

Ausschließlich für Unique.

Grundmodell:

```text
Common / Uncommon
    → normale Inhalte / Quests

Rare
    → besondere Quest / seltene Inhalte

Epic
    → umfangreiche Quest / besonderer Inhalt

Legendary
    → außergewöhnliche, sehr seltene Freischaltung

Unique
    → ausschließlich Admin
```

Die konkreten Dropchancen und Bedingungen werden erst nach Tests des Grundsystems festgelegt.

---

## 21. Begleiter als Questbelohnung

Ein Begleiter als Belohnung soll sich **thematisch aus der Quest ergeben**.

### Wolf – Uncommon

```text
„Der verletzte Wolf“
      ↓
Finde Nahrung
      ↓
Verteidige den Bau
      ↓
Bringe den Wolf zurück
      ↓
Belohnung: Wolf-Begleiter
```

### Biene – Common

```text
Dorf-Imker
   ↓
Honigproduktion gestört
   ↓
Sammle Blumen / beschütze Bienenstöcke
   ↓
Belohnung: Bienen-Begleiter
```

### Zombie – Rare

```text
Ungewöhnlicher Zombie
   ↓
Untersuche sein Verhalten
   ↓
Finde die Ursache
   ↓
mehrere Questschritte
   ↓
Freischaltung des Zombie-Begleiters
```

### Schwein – Epic

```text
Reisender Händler
   ↓
Verlorenes Reittier suchen
   ↓
Tier schützen
   ↓
Zum Händler zurückbringen
   ↓
Epic-Schwein-Begleiter
```

### Warden – Legendary

```text
lange Questkette
   ↓
mehrere schwierige Abschnitte
   ↓
besondere Story / Prüfung
   ↓
sehr seltene Freischaltung
   ↓
Legendary-Warden-Begleiter
```

Weitere Mobs sollen nach demselben Prinzip verwendet werden können. Die Quest muss zum jeweiligen Mob passen.

Beispiele:

```text
Axolotl
→ Unterwasser-/Rettungsquest

Fox
→ Wald-/Beute-/Schutzquest

Bee
→ Imker-/Blumen-/Honigquest

Horse
→ Reise-/Stall-/Rettungsquest

Allay
→ verlorene Gegenstände / Hilfe für einen NPC

Goat
→ Bergdorf / Tierherde / Transport

Dolphin
→ Küsten-/Schiffs-/Meeresquest

Iron Golem
→ Dorfverteidigung / Schmied / Schutzauftrag

Skeleton
→ verfluchte Ruinen / Untoten-Questkette

Enderman
→ seltene End-/Teleport-/Erkundungsquest
```

Diese Beispiele sind nur Inspiration. Der Pool bleibt offen für weitere technisch geeignete Minecraft-Mobs.

---

## 22. Begleiter und Quests müssen zusammenpassen

Begleiter sollen nicht als isoliertes System neben den Quests stehen.

Mögliche Questarten:

```text
Begleiter kennenlernen
Begleiter retten
Begleiter freischalten
Begleiter trainieren
Begleiter-Level erhöhen
Begleiter in einer Quest dabeihaben
```

Ein Begleiter darf niemals zwingend erforderlich sein, um normale Quests abschließen zu können.

---

## 23. Begleiter und Open World

Die Welt soll auch durch Begleiter interessanter werden.

Unterschiedliche Spieler können durch unterschiedliche Questlinien unterschiedliche Begleiter freischalten.

```text
Spieler A
   ↓
findet Wald-Questlinie
   ↓
Wolf-Begleiter

Spieler B
   ↓
findet seltene Ruinen-Questlinie
   ↓
Skeleton-Begleiter
```

Damit entsteht Vielfalt, ohne dass ein einzelner Begleiter zwingend die einzig richtige Wahl wird.

---

## 24. Begleiter und Gegner/Bosse

Dieser Punkt ist besonders kritisch.

Die Begleiter müssen mit den Gegnern und Bossen des Servers harmonieren.

Grundregel:

```text
Spielerstärke
    +
Begleiterunterstützung
    <
Spielergruppe / vorgesehene Endgame-Leistung
```

Besonders bei Epic, Legendary und Unique müssen getestet werden:

- DPS des Spielers ohne Begleiter
- DPS des Spielers mit Begleiter
- Überlebensfähigkeit
- Bossdauer
- Aggro-/Target-Verhalten
- Flächenschaden gegen Gruppen
- AFK-/Grind-Ausnutzung
- Begleiter-XP

---

## 25. Quest- und Begleiter-XP nicht koppeln

Spieler-XP und Begleiter-XP müssen getrennt behandelt werden.

Eine Quest darf nicht automatisch zu einer unverhältnismäßigen Doppelbelohnung führen.

```text
Questabschluss
   ↓
Spieler erhält Quest-XP
   ↓
aktiver Begleiter erhält nur den vorgesehenen Begleiter-XP-Anteil
```

Der Begleiter soll seine Progression hauptsächlich durch tatsächliche aktive Teilnahme aufbauen.

---

## 26. Quest-Navigation und Begleiter gemeinsam denken

```text
Questziel: Dorf Sonnenhain
      ↓
Recovery Compass zeigt Richtung
      ↓
Spieler reist selbstständig
      ↓
Begleiter folgt
      ↓
NPC im Dorf
      ↓
neue Questkette
```

Der Begleiter darf nicht zum automatischen Questfinder werden.

---

## 27. Balance-Ziel

PixelRPG orientiert sich bei der Progressionslogik an klassischen MMORPG-Prinzipien, insbesondere der klaren Stufenprogression von WoW/WotLK.

Übernommen werden soll vor allem:

- klare Levelprogression
- definierte Stärke pro Stufe
- kontrollierte Item-/Begleiter-Skalierung
- seltene besondere Belohnungen
- langfristige Progression

Nicht übernommen werden Reputation und Titel.

---

## 28. Spätere Erweiterungen

Phase C wird vorerst **nicht umgesetzt**.

Erweiterungen wie zusätzliche Endgame-/Post-Progression-Systeme werden erst betrachtet, wenn das Plugin angenommen wurde und ausreichend reale Spielerfahrung vorliegt.

---

# 29. Aktueller Arbeitsauftrag – Quests

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
Dorf-/Ortsverknüpfungen vorbereiten
      ↓
Recovery Compass integrieren
      ↓
Questketten erweitern
      ↓
Level 1–99 mit Inhalten füllen
      ↓
Server-Test
      ↓
Balance / Vereinfachung / Erweiterung
```

---

# 30. Aktueller Arbeitsauftrag – Begleiter

### Stufe 1 – Grundlage

```text
Begleiter-Datenmodell
      ↓
Entity-/Mob-Typ konfigurierbar
      ↓
Seltenheit
      ↓
Level 1–99
      ↓
Begleiter-XP
      ↓
Rufen / Wegschicken
      ↓
Umbenennen für normale Begleiter
```

### Stufe 2 – Passive Begleiter

Zunächst nur Begleiter, die den Spieler begleiten und sich passiv verhalten.

Die bisherigen Testobjekte:

```text
Bee     → Common
Wolf    → Uncommon
Zombie  → Rare
Pig     → Epic
Warden  → Legendary
```

Diese fünf sind **Testbeispiele und keine Begrenzung des Systems**. Weitere geeignete Minecraft-Mobs sollen später ohne grundlegenden Umbau ergänzt werden können.

### Stufe 3 – Unique

```text
Custom Modell / Mannequin
Custom Skin
Fester Name
Nicht umbenennbar
Nur rufen / wegschicken
```

### Stufe 4 – Balance

```text
Levelskalierung testen
      ↓
Spieler + Begleiter testen
      ↓
Gegner testen
      ↓
Boss testen
      ↓
XP-Kurve testen
      ↓
Seltenheiten überprüfen
```

### Stufe 5 – spätere aktive Fähigkeiten

Erst wenn das passive Grundsystem stabil ist, werden komplexere Kampffunktionen, Angriffe oder Spezialfähigkeiten betrachtet.

---

# 31. Gesamtbild

```text
                 PIXELRPG OPEN WORLD
                         │
          ┌──────────────┴──────────────┐
          │                             │
       QUESTS                       BEGLEITER
          │                             │
   Quest-NPCs                    Freischaltung
          │                             │
   Level 1–99                    Level 1–99
          │                             │
   Questketten                   eigene XP
          │                             │
   Dörfer / Orte                 offene Mob-Auswahl
          │                             │
   Recovery Compass              Seltenheiten
          │                             │
   5 aktive Quests               feste Werte
          │                             │
          └──────────────┬──────────────┘
                         │
                  Spielerprogression
                         │
              Gegner / Bosse / Welt
```

Das langfristige Ziel ist ein System, bei dem Quests den Spieler sinnvoll durch die Welt führen und Begleiter eine zusätzliche, kontrollierte Progression darstellen.

Keines der beiden Systeme soll die anderen Systeme überrollen. Die Welt, der Spieler und die Encounter-Balance bleiben die Grundlage.

---

# 32. Wichtigste Designregeln

1. **Maximal 5 aktive Quests.**
2. **Questfreischaltung 5 Level vor dem empfohlenen Quest-Level.**
3. **Level 1–99 ist die reguläre Progression; Level 100 ist Grind.**
4. **Keine sinnlosen nackten Koordinatenquests.**
5. **Ein generischer Reise-/Orts-NPC bleibt als Filler-Typ vorgesehen.**
6. **Dörfer können miteinander verbundene Quest-Hubs bilden.**
7. **Der Recovery Compass dient als Richtungsunterstützung.**
8. **Reputation und Titel werden nicht eingeführt.**
9. **Begleiter besitzen eigene XP und Level 1–99.**
10. **Begleiter erhalten nur XP, solange sie aktiv draußen sind.**
11. **Begleiterwerte sind grundsätzlich fest definiert und werden kontrolliert skaliert.**
12. **Der Begleiter-Pool ist nicht auf fünf Beispiel-Mobs begrenzt.**
13. **Technisch geeignete Minecraft-Mobs sollen grundsätzlich als Begleiter konfigurierbar sein.**
14. **Nicht jede Entity ist automatisch geeignet; technische und Balance-Ausschlüsse sind erlaubt.**
15. **Begleiter sollen den Spieler unterstützen, nicht ersetzen.**
16. **Common bis Legendary können kontrolliert über normale bzw. besondere Inhalte freigeschaltet werden.**
17. **Unique-Begleiter werden ausschließlich von Admins vergeben.**
18. **Unique-Namen können fest und nicht veränderbar sein.**
19. **Passive Begleiter kommen vor komplexen Kampffähigkeiten.**
20. **Quest- und Begleiter-XP müssen gemeinsam auf Balance geprüft werden.**
21. **Jede Begleiter-Seltenheit muss gegen reale Gegner- und Bosskämpfe getestet werden.**
22. **Keine Begleitermechanik darf zur Pflicht werden, um normale Quests abschließen zu können.**
23. **Phase C bleibt vorerst außen vor.**
