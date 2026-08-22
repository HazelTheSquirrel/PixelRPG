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

```text
Maximal aktive Quests = 5
```

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
```

Level 100 bleibt der reservierte **Grind-Bereich**.

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

Ein weiteres Beispiel mit einem Spielerdorf:

```text
Dorf A
  ↓
Quest: „Die Händlerroute ist unterbrochen.“
  ↓
Reise zum Spielerdorf B
  ↓
NPC dort
  ↓
Information über Banditen / Ressourcen / lokale Probleme
  ↓
neue Questkette
```

Dadurch können später auch organisch entstandene Orte Teil der Welt werden.

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

Der `minecraft:recovery_compass` soll eine neue Aufgabe bekommen: Quest-Navigation.

Er ersetzt keinen normalen Questmarker und soll kein permanentes GPS-System werden.

Ziel:

```text
Questziel bekannt
      ↓
Recovery Compass
      ↓
Richtung zum Ziel
      ↓
Spieler findet den Ort selbst
```

Der Spieler erhält damit Unterstützung bei der Orientierung, ohne dass die Open World durch permanente Wegpunkte oder eine vollständige automatische Navigation entwertet wird.

Die genaue technische Umsetzung und die Frage, ob der Kompass automatisch ausgerüstet, als Questwerkzeug verwendet oder nur temporär bereitgestellt wird, wird bei der Implementierung entschieden.

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

Begleiter sollen ein eigenständiges Progressionssystem werden, das sich an der Spielerprogression orientiert, ohne das Kampfsystem zu zerstören.

Der wichtigste Grundsatz lautet:

> **Begleiter unterstützen den Spieler. Sie ersetzen ihn nicht.**

Begleiter sollen deshalb grundsätzlich schwächer als ein gleichwertiger Spielercharakter sein und abhängig von ihrer Seltenheit feste Basiswerte besitzen.

Begleiter können optisch normale Minecraft-Entities, Monster oder Spieler-/Mannequin-ähnliche Modelle sein. Das Aussehen bestimmt nicht automatisch die Stärke.

Beispiele:

- Wolf
- Biene
- Zombie
- Schwein
- Warden
- später individuelle Unique-Begleiter

Auch ungewöhnliche Modelle sind grundsätzlich möglich. Ein „Mini Warden“ kann beispielsweise optisch klein sein, ohne dadurch automatisch Bossstärke zu erhalten.

---

## 12. Begleiter-Seltenheiten

Die geplante Zuordnung lautet:

| Seltenheit | Beispiel | Grundidee |
|---|---|---|
| Common | Biene | sehr schwacher, einfacher Begleiter |
| Uncommon | Wolf | solider früher Begleiter |
| Rare | Zombie | stärkerer Kampfbegleiter |
| Epic | Schwein | besonderer Begleiter, perspektivisch reitbar |
| Legendary | Warden | extrem seltener, starker Begleiter, aber weiterhin Spielerunterstützung |
| Unique | Custom-/Mannequin-Begleiter | ausschließlich individuell durch Admin vergeben |

Die Seltenheit bestimmt nicht nur die Optik, sondern die Basis für die Skalierung von Werten und den benötigten Erfahrungsaufwand.

---

## 13. Begleiter-Level und Erfahrung

Begleiter besitzen ein eigenes Level und eigene Erfahrung.

Geplanter Bereich:

```text
Begleiter-Level 1 → 99
Level 100 = Grind-Bereich
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

Der Begleiter soll nicht automatisch durch die komplette Spielerprogression mitgezogen werden, wenn er dauerhaft nicht eingesetzt wird.

Die XP-Kurve muss später so balanciert werden, dass ein aktiver Begleiter sinnvoll mit dem Spieler mitwächst, ohne dass Begleiter-Leveln zum Pflicht-Grind wird.

---

## 14. Feste Begleiterwerte und Skalierung

Begleiter sollen **keine frei zusammenstellbaren Spielerbuilds** werden.

Jede Begleiterart und Seltenheit erhält definierte Basiswerte.

Beispielhafte Struktur:

```text
Biene – Common
Basiswerte
  ↓
leichte Skalierung pro Level

Wolf – Uncommon
Basiswerte
  ↓
mittlere Skalierung pro Level

Warden – Legendary
Basiswerte
  ↓
höhere Skalierung pro Level
```

Die Skalierung muss konservativ gewählt werden. Ein Legendary-Begleiter darf nicht dazu führen, dass ein Spielerboss trivial wird.

Wichtig ist die Harmonie zwischen:

```text
Spieler
   +
Begleiter
   vs.
Gegner
   vs.
Boss
```

Das System soll sich an der bereits geplanten WoW-/WotLK-inspirierten Progressionslogik orientieren: klare Stufen, kontrollierte Werte und keine exponentielle Eskalation durch zusätzliche Einheiten.

---

## 15. Passive Begleiter als erster Schritt

Der erste Begleiter-Ausbau soll bewusst **passiv bzw. nicht kämpfend** bleiben.

Das bedeutet:

- Begleiter können wie Monster, Tiere oder Spieler aussehen.
- Sie folgen dem Spieler.
- Sie können gerufen und weggeschickt werden.
- Sie müssen nicht sofort eigene komplexe Kampflogik besitzen.
- Das Aussehen ist unabhängig von ihrer tatsächlichen Rolle.

Damit kann das Begleiter-System zunächst sicher getestet werden, bevor offensive Fähigkeiten und KI hinzugefügt werden.

Ein Wolf kann also zunächst als sichtbarer Begleiter funktionieren, ohne dass sein Kampfschaden Teil des ersten Tests sein muss.

---

## 16. Begleiter-Befehle

Die grundlegenden Aktionen sollen einheitlich sein:

```text
Begleiter rufen
Begleiter wegschicken
Begleiter umbenennen
```

Die Umbenennung gilt grundsätzlich nur für normale Begleiter.

### Unique-Ausnahme

Unique-Begleiter können einen festen Namen besitzen.

```text
Unique Begleiter
    ↓
Admin vergibt Begleiter
    ↓
Name ist fest definiert
    ↓
Spieler kann ihn nicht umbenennen
    ↓
nur rufen / wegschicken
```

Das ist insbesondere für individuelle Geschenk-/Event-Begleiter gedacht.

---

## 17. Unique-Begleiter

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

Unique-Begleiter dürfen technisch stärker oder spezieller sein, müssen aber trotzdem in die allgemeine Progression passen. „Unique“ bedeutet nicht automatisch „zerstört jede Balance“.

---

## 18. Begleiter als Questbelohnung

Begleiter sollen nicht einfach wahllos aus jeder Quest fallen.

Ein Begleiter als Belohnung soll sich **thematisch aus der Quest ergeben**.

Beispiele:

### Beispiel A – Wolf / Uncommon

Questkette:

```text
„Der verletzte Wolf“
      ↓
Finde Nahrung
      ↓
Verteidige den Bau
      ↓
Bringe den Wolf zurück
      ↓
Questabschluss
      ↓
Belohnung: Wolf-Begleiter
```

Der Spieler bekommt den Begleiter nicht als zufälligen Gegenstand, sondern als Ergebnis einer kleinen Geschichte.

### Beispiel B – Biene / Common

```text
Dorf-Imker
   ↓
Honigproduktion gestört
   ↓
Sammle Blumen / beschütze Bienenstöcke
   ↓
Imker bedankt sich
   ↓
Belohnung: Bienen-Begleiter
```

### Beispiel C – Zombie / Rare

Ein Zombie-Begleiter sollte nicht aus einer normalen „Töte 5 Zombies“-Quest fallen.

Besser:

```text
Questkette über einen ungewöhnlichen Zombie
   ↓
Untersuche sein Verhalten
   ↓
Finde die Ursache
   ↓
mehrere Schritte
   ↓
Freischaltung eines besonderen Zombie-Begleiters
```

Damit fühlt sich Rare tatsächlich selten an.

### Beispiel D – Schwein / Epic

```text
Questkette eines reisenden Händlers
   ↓
Suche sein verlorenes Reittier
   ↓
Schütze das Tier
   ↓
Bringe es zurück
   ↓
Belohnung: Epic-Schwein-Begleiter
```

Das Schwein kann perspektivisch als besonderer reitbarer Begleiter umgesetzt werden.

### Beispiel E – Warden / Legendary

Ein Legendary-Begleiter sollte **nicht** als gewöhnlicher Questabschluss verteilt werden.

Beispiel:

```text
lange Questkette
   ↓
mehrere schwierige Abschnitte
   ↓
Ende einer besonderen Story
   ↓
sehr seltene Freischaltung
   ↓
Warden-Begleiter
```

Die Belohnung muss dabei so balanciert sein, dass der Begleiter zwar mächtig und besonders ist, aber Bosse und Endgame-Inhalte nicht trivialisiert.

---

## 19. Begleiter-Freischaltung – geplantes Modell

Noch nicht jede Freischaltungsquelle ist endgültig festgelegt. Das System soll aber mehrere kontrollierte Quellen ermöglichen.

Mögliche Kategorien:

### Questbelohnung

Besonders geeignet für thematische Common-, Uncommon- und einzelne Rare-Begleiter.

### Seltene Inhalte / besondere Gegner

Für höherwertige Begleiter können seltene Freischaltungen über spezielle Inhalte vorgesehen werden.

Wichtig: Kein Begleiter soll durch einen beliebigen Standard-Mobdrop massenhaft verfügbar werden, wenn er laut Seltenheit besonders sein soll.

### Besondere Questketten

Für Epic und Legendary besonders geeignet.

### Admin-Vergabe

Ausschließlich für Unique.

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

Die konkreten Dropchancen und Freischaltbedingungen werden erst festgelegt, wenn das Grundsystem getestet werden kann.

---

## 20. Begleiter und Quests müssen zusammenpassen

Begleiter sollen nicht als isoliertes System neben den Quests stehen.

Sie können Bestandteil der Welt und der Queststruktur werden.

Mögliche Questarten:

```text
Begleiter kennenlernen
Begleiter retten
Begleiter freischalten
Begleiter trainieren
Begleiter-Level erhöhen
Begleiter zu einem NPC bringen
Begleiter in einer Quest dabeihaben
```

Dabei darf niemals das Gefühl entstehen, dass ein Spieler einen Begleiter zwingend besitzen muss, um normale Quests zu schaffen.

Begleiter sind eine zusätzliche Progressions- und Komfortebene.

---

## 21. Begleiter und Open World

Die Welt soll auch durch Begleiter interessanter werden.

Beispiel:

```text
Spieler entdeckt einen alten Wald
        ↓
NPC erzählt von Wölfen
        ↓
Questkette
        ↓
Wolf-Begleiter wird freigeschaltet
        ↓
Wolf begleitet den Spieler künftig
```

Ein anderer Spieler kann dieselbe Welt anders erleben:

```text
Spieler B
        ↓
findet eine seltene Questlinie
        ↓
schaltet Zombie-Begleiter frei
```

So können unterschiedliche Spieler unterschiedliche Begleiter besitzen, ohne dass daraus automatisch ein klassisches „Pay-to-win“- oder Pflichtsystem entsteht.

---

## 22. Begleiter-Skalierung und Gegner/Bosse

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

Ein Begleiter darf den Einzelspieler stärker machen, aber nicht die gesamte Encounter-Balance verdoppeln.

Besonders bei Legendary und Unique müssen deshalb folgende Punkte getestet werden:

- DPS des Spielers ohne Begleiter
- DPS des Spielers mit Begleiter
- Überlebensfähigkeit
- Bossdauer
- Aggro-/Target-Verhalten
- Flächenschaden gegen Gruppen
- mögliche Ausnutzung durch AFK-/Grind-Situationen
- XP-Gewinn des Begleiters

Die Werte werden nicht theoretisch endgültig festgelegt. Sie werden anhand echter Serverkämpfe angepasst.

---

## 23. Quest- und Begleiter-XP nicht koppeln

Spieler-XP und Begleiter-XP müssen getrennt behandelt werden.

Eine Quest darf nicht automatisch zu einer unverhältnismäßigen Doppelbelohnung führen.

Beispiel:

```text
Questabschluss
   ↓
Spieler erhält Quest-XP
   ↓
aktiver Begleiter erhält nur den vorgesehenen Begleiter-XP-Anteil
```

Der Begleiter soll seine Progression hauptsächlich durch tatsächliche aktive Teilnahme aufbauen.

---

## 24. Quest-Navigation und Begleiter gemeinsam denken

Questnavigation und Begleiter dürfen sich ergänzen.

Beispiel:

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

Der Begleiter darf dabei nicht zum automatischen Questfinder werden. Er soll den Spieler begleiten, nicht die Welt für ihn durchsuchen.

---

## 25. Balance-Ziel

PixelRPG orientiert sich bei der Progressionslogik an klassischen MMORPG-Prinzipien, insbesondere der klaren Stufenprogression von WoW/WotLK.

Übernommen werden soll vor allem das Prinzip:

- klare Levelprogression
- definierte Stärke pro Stufe
- kontrollierte Item-/Begleiter-Skalierung
- seltene besondere Belohnungen
- langfristige Progression

Nicht übernommen werden sollen Systeme wie Reputation oder Titel, da diese für PixelRPG ausdrücklich nicht vorgesehen sind.

Die Begleiter müssen so skaliert werden, dass sie sich wie ein sinnvoller Teil des MMORPG-Systems anfühlen und nicht wie ein zweiter vollständiger Spielercharakter.

---

# 26. Spätere Erweiterungen

Phase C wird vorerst **nicht umgesetzt**.

Erweiterungen wie zusätzliche Endgame-/Post-Progression-Systeme werden erst betrachtet, wenn das Plugin angenommen wurde und ausreichend reale Spielerfahrung vorliegt.

Bis dahin bleibt der Fokus auf den Kernsystemen.

---

# 27. Aktueller Arbeitsauftrag – Quests

Vor der vollständigen Quest-Implementierung:

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

---

# 28. Aktueller Arbeitsauftrag – Begleiter

Die Begleiter werden bewusst schrittweise entwickelt.

### Stufe 1 – Grundlage

```text
Begleiter-Datenmodell
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

Testobjekte:

```text
Bee     → Common
Wolf    → Uncommon
Zombie  → Rare
Pig     → Epic
Warden  → Legendary
```

### Stufe 3 – Unique

Admin kann einen individuellen Begleiter vergeben:

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

# 29. Gesamtbild

Die Quest- und Begleiter-Systeme sollen langfristig ineinandergreifen:

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
   Dörfer / Orte                 Seltenheiten
          │                             │
   Recovery Compass              feste Werte
          │                             │
   5 aktive Quests               Rufen / Wegschicken
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

# 30. Wichtigste Designregeln

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
12. **Begleiter sollen den Spieler unterstützen, nicht ersetzen.**
13. **Common bis Legendary können kontrolliert über normale bzw. besondere Inhalte freigeschaltet werden.**
14. **Unique-Begleiter werden ausschließlich von Admins vergeben.**
15. **Unique-Namen können fest und nicht veränderbar sein.**
16. **Passive Begleiter kommen vor komplexen Kampffähigkeiten.**
17. **Quest- und Begleiter-XP müssen gemeinsam auf Balance geprüft werden.**
18. **Jede Begleiter-Seltenheit muss gegen reale Gegner- und Bosskämpfe getestet werden.**
19. **Keine Begleitermechanik darf zur Pflicht werden, um normale Quests abschließen zu können.**
20. **Phase C bleibt vorerst außen vor.**
