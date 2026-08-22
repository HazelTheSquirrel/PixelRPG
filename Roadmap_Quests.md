# PixelRPG – Quest & Companion Roadmap

> Verbindliche Planungs- und Umsetzungsgrundlage für Quest- und Begleiter-System.
>
> Zielplattform: Paper 26.x, aktuell Paper 26.2. Java 25. Die Roadmap beschreibt Design, Datenmodell, Reihenfolge, Beispiele und Abnahmekriterien. Kampffähigkeiten für Begleiter bleiben bewusst zurückgestellt, bis das Quest- und Begleiter-Grundsystem stabil getestet ist.

---

# 1. Verbindliche Designregeln

PixelRPG ist eine Open-World-RPG-Welt. Quests sollen den Spieler führen, ohne die Welt in eine GPS-/Marker-Map zu verwandeln.

Nicht gewünscht:

- sinnlose Koordinatenquests
- künstliche Gebietsgrenzen nur wegen Quest-Leveln
- Reputation-System
- Titel-System
- endlose Fetch-Quests ohne Zusammenhang
- ein Begleiter-System, das den Spieler ersetzt
- Begleiter-Kampf-KI in der ersten Begleiterphase

Gewünscht:

- erkennbare NPCs und Rollen
- Dörfer und Spielerprojekte als echte Quest-Orte
- nachvollziehbare Questketten
- Erkundung statt stumpfer Koordinatensuche
- Recovery Compass als Richtungshelfer
- Begleiter als unterstützende, eigene Progression
- vollständig datengetriebene Inhalte
- möglichst viele technisch geeignete Minecraft-/Mojang-Entities als Begleiter

Reputation und Titel werden nicht eingeführt.

---

# 2. Spielerprogression

```text
Level 1 → 99
Level 100 → Grind-/Endgame-Bereich
```

Level 100 ist kein normaler Quest-Level. Die reguläre Questprogression endet bei Level 99.

Die Welt benötigt deshalb langfristig sehr viele Quests. Wir erzeugen aber keine sinnlose Pflichtzahl pro Level, sondern mehrere miteinander verbundene Questketten über Levelbereiche.

Grundstruktur:

```text
1–5      Einführung / lokale Aufgaben
6–10     erster Quest-Hub
11–20    Dorf / Berufe / erste Reisequests
21–30    regionale Konflikte
31–40    mehrere Quest-Hubs
41–50    größere regionale Handlung
51–60    fortgeschrittene Reise- und Storyketten
61–70    große Weltkonflikte
71–80    fortgeschrittene Questketten
81–90    späte Haupt- und Nebeninhalte
91–99    Endphase der regulären Progression
100+     Grind / spätere Systeme
```

Die XP-Kurve wird ausschließlich durch tatsächliche Spieltests finalisiert.

---

# 3. Maximal 5 aktive Quests

Ein Spieler kann maximal **5 aktive Quests** besitzen.

Eine sechste Quest darf nicht angenommen werden.

Eine mehrstufige Quest zählt als eine aktive Quest, solange ihre Schritte Bestandteil derselben Questinstanz sind.

Ziel:

```text
wenige bewusste Aufgaben
        ↓
Spieler entscheidet selbst
        ↓
Questliste bleibt übersichtlich
```

Die Grenze ist datengetrieben konfigurierbar, Standardwert `5`.

---

# 4. Quest-Freischaltung

Eine Quest mit empfohlenem Level `X` wird grundsätzlich bereits bei `X - 5` freigeschaltet.

Beispiel:

```text
Quest-Level 20
      ↓
freigeschaltet ab Level 15
      ↓
empfohlen für Level 20
```

Der Vorlauf von fünf Leveln ist ein fester Designwert für die aktuelle Planung.

Zusätzliche Voraussetzungen sind weiterhin möglich:

- vorherige Quest
- Dialogentscheidung
- benötigter Gegenstand
- NPC-/Dorf-Fortschritt
- Questkettenstatus

---

# 5. Quest-NPCs

Die bestehenden Quest-NPCs bleiben die normalen Questgeber.

Beispiele für Rollen:

- Dorfvorsteher
- Wache
- Jäger
- Schmied
- Versorger
- Alchemist
- Gelehrter
- Händler
- Abenteurer

Ablauf:

```text
Quest-NPC
   ↓
Spielerlevel prüfen
   ↓
verfügbare Levelbereiche
   ↓
Quest auswählen
   ↓
Questdetails
   ↓
Annehmen
```

Die NPCs sollen nicht zu anonymen Koordinatengebern werden.

---

# 6. Filler-/Reise-NPC

Es wird genau ein generischer **Filler-/Reise-/Orts-NPC-Typ** benötigt.

Er ist kein zweites großes Quest-Framework. Er dient als bewusst gesetzter Zielpunkt innerhalb einer Questkette.

Beispiele:

```text
„Reise nach Sonnenhain und sprich mit Mira.“
„Gehe zum Dorf Eisenfels und suche den dortigen Schmied.“
„Finde den Verwalter des Dorfes und frage ihn nach dem verschwundenen Händler.“
```

Der Admin kann diesen NPC in Dörfern, Spielerprojekten oder anderen wichtigen Orten platzieren.

Damit können später auch von Spielern gebaute Dörfer in Questketten aufgenommen werden.

---

# 7. Dorf-zu-Dorf-Questketten

Dörfer können eigene Quest-Hubs sein.

```text
Dorf A
  ↓
Questgeber A
  ↓
Reise nach Dorf B
  ↓
Filler-/Ziel-NPC B
  ↓
neue lokale Handlung
  ↓
Schmied / Händler / Wache / Gelehrter
  ↓
weitere Questkette
```

Dorf B muss nicht die direkte Fortsetzung der Geschichte aus Dorf A sein. Es kann eine eigene lokale Handlung besitzen.

Das ermöglicht später auch vollständig selbst gebaute Dörfer als Teil der Welt.

---

# 8. Koordinatenquests

Keine regulären Quests nach dem Muster:

```text
Gehe zu X:123 Y:64 Z:456.
```

wenn der Spieler nicht weiß, was ihn dort erwartet.

Akzeptabel:

```text
„Reise zum Dorf Sonnenhain.“
„Sprich dort mit Mira.“
```

Koordinaten dürfen intern gespeichert werden. Sie ersetzen aber nicht die verständliche Questbeschreibung.

---

# 9. Recovery Compass

Der `minecraft:recovery_compass` erhält einen echten Questnutzen.

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

Er soll kein vollständiges GPS sein.

Besonders geeignet für:

- Dorfziele
- NPC-Ziele
- definierte Erkundungsorte
- Quest-Hubs

Bei mehreren aktiven Quests wird genau ein aktives Navigationsziel ausgewählt. Die UI muss klar anzeigen, welche Quest aktuell verfolgt wird.

Technische Umsetzung muss die aktuelle Paper-26.2-API verwenden. Kein altes 1.21.x-Verhalten nachbauen.

---

# 10. Questtypen

Der Quest-Pool soll mehrere Typen kombinieren:

- `HUNT` – Gegner besiegen
- `COLLECT` – Gegenstände sammeln
- `TALK_TO_NPC` – mit einem definierten NPC sprechen
- `ESCORT` – Ziel/NPC begleiten
- `REACH_LOCATION` – einen beschriebenen Ort erreichen
- `GLOBAL_EVENT` – serverweites Ereignis

Langfristig vorgesehen:

- Erkundungsquests
- Lieferquests
- Berufsquests
- Dialog-/Storyquests
- Begleiterquests
- mehrstufige Questketten
- Welt-/Eventquests

Neue Questtypen werden erst eingeführt, wenn ihre komplette Progressionslogik vorhanden ist. Keine reinen Enum-Platzhalter ohne funktionierenden Fortschritt.

---

# 11. Questketten

Questketten sollen aus mehreren sinnvollen Schritten bestehen.

Beispiel:

```text
NPC
 ↓
Dialog
 ↓
Mine untersuchen
 ↓
Erz sammeln
 ↓
Gegner besiegen
 ↓
Dorf B besuchen
 ↓
Gelehrten sprechen
 ↓
Belohnung
 ↓
Folgequest freischalten
```

Die Questinstanz bleibt dabei innerhalb des Limits von fünf aktiven Quests eine einzelne Quest.

---

# 12. Questbeispiele

## Level 1–5 – Einführung

```text
„Die erste Lieferung“

Der Versorger des Dorfes braucht einfache Materialien.
Der Spieler sammelt Holz und bringt es zurück.

Belohnung:
Spieler-XP + Geld + erstes Verbrauchsitem
```

## Level 8 – kleine Quest

```text
„Die verschwundenen Werkzeuge“

Ein Schmied vermisst seine Werkzeuge.
Der Spieler untersucht die Werkstatt,
findet Spuren und bringt die Werkzeuge zurück.

Belohnung:
XP + Geld + Item
```

## Level 15 – Dorfreise

```text
„Der Weg nach Sonnenhain“

Der Spieler reist nach Sonnenhain und sucht Mira.

Recovery Compass hilft bei der Richtung.

Bei Mira beginnt eine neue lokale Questkette.
```

## Level 25 – größere Kette

```text
„Das Schweigen von Eisenfels“

Wache befragen
 ↓
Mine untersuchen
 ↓
Erzproben sammeln
 ↓
Ursache der Störung finden
 ↓
Schmied informieren
 ↓
Dorf B besuchen
 ↓
Gelehrten sprechen
 ↓
Folgequest freischalten
```

## Level 50 – regionale Handlung

```text
„Die Handelsroute“

Händler sprechen
 ↓
Route untersuchen
 ↓
Banditenlager finden
 ↓
Banditen besiegen
 ↓
gestohlene Waren sichern
 ↓
Ware nach Dorf B bringen
 ↓
Folgequest
```

## Level 90–99 – späte Handlung

```text
„Das letzte Siegel“

Mehrere bekannte NPCs befragen
 ↓
alte Questinformationen verbinden
 ↓
mehrere Orte untersuchen
 ↓
Elite-Gegner besiegen
 ↓
Endziel erreichen
 ↓
Abschluss der regulären Questprogression
```

---

# 13. Questbelohnungen

Mögliche Belohnungen:

- Spieler-XP
- Geld
- Items
- Verbrauchsgegenstände
- Berufs-/Craftingmaterialien
- Questgegenstände
- Folgequests
- Begleiter
- Begleiter-Freischaltungen

Nicht vorhanden:

- Reputation
- Titel

---

# 14. Datengetriebenes Quest-System

Alle veränderlichen Questdaten sollen außerhalb des Java-Codes bearbeitbar sein.

Primäre Quelle:

```text
src/main/resources/data/quests.json
```

Später beim Plugin-Betrieb:

```text
plugins/PixelRPG/data/quests.json
```

Die Struktur muss mindestens erlauben:

- Quest-ID
- Titel
- Beschreibung
- Typ
- empfohlenes Level
- Kategorie-Level
- Ziel
- Zielmenge
- Voraussetzungen
- Belohnungen
- Folgequests
- Zeitlimit
- Ortsdaten
- NPC-Ziel
- Begleiterbelohnung

Grundregeln wie `maxActive` und `unlockEarlyLevels` bleiben ebenfalls datengetrieben.

Keine Quest-Balance hart im Java-Code verteilen.

---

# 15. Begleiter – Grundsatz

Begleiter werden ein eigenes Progressionssystem.

> **Begleiter unterstützen den Spieler. Sie ersetzen ihn nicht.**

Balance wird immer als Kombination betrachtet:

```text
Spieler + Begleiter
        ↓
Gegner / Elite / Boss
```

Ein Legendary-Begleiter darf einen Boss nicht trivial machen.

Die Werte müssen sich an der geplanten Spieler-/Gegnerprogression orientieren und werden mit echten Tests angepasst.

---

# 16. Begleiter-Pool – möglichst groß

Wolf, Biene, Zombie, Schwein und Warden sind nur Beispiele.

Das System soll grundsätzlich möglichst viele technisch geeignete Entities der tatsächlich verwendeten Paper-/Minecraft-Version unterstützen.

Die aktuelle Paper-26.2-API stellt unter anderem `EntityType.MANNEQUIN` bereit und führt zahlreiche weitere lebende Entity-Typen. Für den tatsächlichen Begleiterpool gilt:

```text
EntityType vorhanden
        ↓
LivingEntity / technisch geeignet
        ↓
spawnbar und kontrollierbar
        ↓
passiv verwendbar
        ↓
Balance geprüft
        ↓
als Begleiter freigeben
```

Die Minecraft-Wiki dient nur als Inspirationsquelle. Der Code darf ausschließlich tatsächlich vorhandene und technisch nutzbare Entity-Typen der Zielversion verwenden.

Auch ungewöhnliche bzw. ursprünglich als April-Fools-, Test- oder Spaßfeature gedachte Entities werden geprüft, wenn sie in der Zielversion vorhanden und technisch nutzbar sind.

Nicht geeignet sind beispielsweise reine Projektile, kurzlebige Effekte oder Entities ohne sinnvolle persistente Begleiterdarstellung.

---

# 17. Begleiter-Kategorien

Mögliche Kategorien:

- Haustiere
- Tiere
- neutrale Mobs
- feindliche Mobs
- fliegende Mobs
- aquatische Mobs
- humanoide Mobs
- große Mobs
- kleine Mobs
- ungewöhnliche Mobs
- Mannequin-/Spielermodelle
- Custom-/Unique-Begleiter

Darstellung und Verhalten sind getrennt.

Ein Zombie-Begleiter kann wie ein Zombie aussehen und trotzdem vollständig passiv sein.

---

# 18. Seltenheiten

Verbindliche Grundbeispiele:

| Seltenheit | Beispiel | Grundidee |
|---|---|---|
| Common | Biene | einfacher, schwacher Begleiter |
| Uncommon | Wolf | solider früher Begleiter |
| Rare | Zombie | stärkerer Begleiter |
| Epic | Schwein | besonderer Begleiter, perspektivisch reitbar |
| Legendary | Warden | extrem selten und stark, aber kontrolliert |
| Unique | Custom/Mannequin | ausschließlich individuell durch Admin |

Das sind Beispiele und keine vollständige Entity-Zuordnung.

Jeder geeignete Mob kann abhängig von Rolle, Verfügbarkeit und Balance einer Seltenheit zugeordnet werden.

Seltenheit darf nicht einfach bedeuten „alles ×10“. Die Skalierung muss kontrolliert bleiben.

---

# 19. Begleiter-Level

Begleiter haben eigene Level:

```text
Begleiter Level 1 → 99
Level 100 → Grind-Bereich
```

Der Begleiter wird nicht automatisch auf das Spielerlevel gesetzt.

Beispiel:

```text
Spieler Level 50
Wolf Level 27
```

Der Begleiter besitzt seine eigene Progression.

---

# 20. Begleiter-XP

Ein Begleiter erhält XP nur, wenn er aktiv beim Spieler ist.

```text
Begleiter aktiv
    ↓
XP möglich

Begleiter weggeschickt
    ↓
keine Begleiter-XP
```

Mögliche XP-Quellen:

- Questabschluss
- besiegte Gegner, sobald Kampfsystem aktiv ist
- Erkundung
- Weltaktivitäten
- spezielle Begleiterquests

AFK-XP und XP ohne aktive Begleiterinstanz müssen verhindert werden.

Die konkrete XP-Verteilung wird erst nach Tests endgültig festgelegt.

---

# 21. Feste Werte und Skalierung

Begleiter sind keine zweiten frei konfigurierbaren Spielercharaktere.

Grundmodell:

```text
Entity-Typ
 ↓
Seltenheit
 ↓
Basiswerte
 ↓
Level-Skalierung 1–99
 ↓
finale Begleiterwerte
```

Die Werte werden vollständig datengetrieben definiert.

Vorgesehene Trennung:

```text
src/main/resources/data/companions.json
src/main/resources/data/companion-stats.json
src/main/resources/data/mob-scaling.json
```

So kann ein Serverbetreiber die Begleiterwerte ändern, ohne Java-Code anzufassen.

Seltenheitsmultiplikatoren bleiben kontrolliert und werden mit der Gegner-/Bossbalance getestet.

---

# 22. Größe und Darstellung

Begleiter sollen ihre visuelle Größe unabhängig von ihren Kampfstärken verändern können, sofern die jeweilige Entity dies unterstützt.

Paper 26.2 stellt hierfür die Entity-Attribute `Attribute.SCALE` bereit.

Beispiele:

```text
Wolf   → 0.50 → Mini-Wolf
Warden → 0.50 → Mini-Warden
Biene  → 2.00 → große Biene
```

Wichtig:

```text
Darstellungsgröße ≠ Kampfstärke
```

Ein kleiner Warden ist nicht automatisch schwächer.
Ein großer Wolf ist nicht automatisch stärker.

Die Größe darf nicht unbemerkt als Damage-/HP-Multiplikator dienen.

Die gewünschte Größe gehört in die Begleiterdefinition und darf nicht hart im Java-Code stehen.

---

# 23. Passive Begleiter – Phase 1

Die erste Begleiterphase ist bewusst passiv.

Zuerst müssen stabil funktionieren:

- Besitz
- Freischaltung
- Persistenz
- Auswahl
- Rufen
- Wegschicken
- Folgen
- Name
- Level
- XP
- UI
- Unique-Ausnahme
- Größe

Noch nicht implementieren:

- komplexe Kampf-KI
- Fähigkeiten
- Skills
- Aggro-System
- Bossmechaniken
- automatische Zielauswahl

Erst wenn das Fundament und das Quest-System vollständig getestet sind, beginnt die Kampffunktion.

---

# 24. Begleiter-Befehle

Normale Begleiter:

```text
Rufen
Wegschicken
Umbenennen
```

Unique-Begleiter:

```text
Rufen
Wegschicken
```

Der feste Unique-Name darf nicht geändert werden.

---

# 25. Unique-Begleiter

Unique ist eine Sonderklasse.

Nur ein Admin darf einen Unique-Begleiter für einen Spieler freischalten.

Beispiel:

```text
Admin
 ↓
Unique Companion vergeben
 ↓
Spieler besitzt ihn dauerhaft
 ↓
Name bleibt fest
 ↓
Rufen / Wegschicken möglich
```

Ein Unique-Begleiter kann beispielsweise ein Mannequin mit festem Namen und festem Skin sein.

Der Spieler darf den Namen nicht ändern.

Unique-Begleiter dürfen nicht über normale Questbelohnungen zufällig freigeschaltet werden, sofern sie ausdrücklich als Admin-only definiert sind.

---

# 26. Begleiter als Questbelohnung

Begleiter sollen nicht nur über einen Shop verfügbar sein.

Sie können als besondere Questbelohnung auftauchen.

Beispiele:

### Common

```text
Quest: „Das Bienennest“
Level 7

Hilf einem Imker, sein beschädigtes Nest zu retten.

Belohnung:
Biene – Common
```

### Uncommon

```text
Quest: „Ein treuer Freund“
Level 20

Hilf einem verletzten Wolf und bringe ihn sicher zurück.

Belohnung:
Wolf – Uncommon
```

### Rare

```text
Questkette: „Die verlassene Gruft“
Level 35

Untersuche die Gruft und löse die Ursache der Untotenaktivität.

Belohnung:
Zombie – Rare
```

### Epic

```text
Questkette: „Der verlorene Sattel“
Level 50

Finde den verschwundenen Händler und seinen besonderen Reitgefährten.

Belohnung:
Schwein – Epic
```

### Legendary

```text
Sehr späte Questkette / außergewöhnliches Weltgeschehen

Die Belohnung darf nicht einfach aus einer normalen Daily-Quest stammen.

Belohnung:
Warden – Legendary
```

### Unique

```text
Admin-Geschenk
 ↓
fest definierter Name
 ↓
fest definierter Skin / Entity
 ↓
Spieler kann nur rufen / wegschicken
```

Die Begleiterbelohnung wird über `companionId` in der Questdefinition angegeben.

---

# 27. Freischaltungsquellen für normale Begleiter

Normale Begleiter können langfristig über mehrere Wege verfügbar werden:

1. Questbelohnung
2. besondere Questketten
3. seltene Welt-/Eventquests
4. besondere NPC-Aufgaben
5. definierte Entdeckungs-/Erkundungsinhalte
6. spätere spezielle Systeme, sofern sie zur Spielbalance passen

Kein Begleiter wird ausschließlich deshalb verfügbar, weil der Spieler irgendeinen Mob getötet hat.

Die Freischaltung soll sich wie eine Belohnung oder Entdeckung anfühlen.

---

# 28. JSON-Konzept – Begleiter

Alle veränderlichen Begleiterdaten müssen editierbar sein.

Beispielstruktur:

```json
{
  "id": "test-wolf",
  "entityType": "WOLF",
  "rarity": "UNCOMMON",
  "name": "PixelRPG Wolf",
  "renameable": true,
  "adminOnly": false,
  "scale": 1.0,
  "stats": {
    "health": 1.0,
    "damage": 1.0,
    "speed": 1.0
  }
}
```

Für Unique:

```json
{
  "id": "admin_unique_example",
  "entityType": "MANNEQUIN",
  "rarity": "UNIQUE",
  "name": "Fester Name",
  "renameable": false,
  "adminOnly": true,
  "scale": 1.0
}
```

Die Java-Logik darf keine konkreten Begleiterwerte voraussetzen.

---

# 29. JSON-Konzept – Questdaten

Beispiel:

```json
{
  "id": "village_sonnenhain_mira",
  "title": "Eine Nachricht aus Sonnenhain",
  "description": "Finde Mira in Sonnenhain und sprich mit ihr.",
  "type": "TALK_TO_NPC",
  "recommendedLevel": 16,
  "categoryLevel": 10,
  "targetKey": "example_filler_mira",
  "requiredAmount": 1,
  "requirements": {
    "previousQuest": "example_sonnenhain"
  },
  "reward": {
    "money": 35.0,
    "experience": 120,
    "items": [],
    "companionId": ""
  }
}
```

Questdesigner sollen neue Inhalte schreiben können, ohne Java-Klassen für jede einzelne Quest zu erstellen.

---

# 30. Quest- und Begleiterbalance

Die Systeme werden gemeinsam getestet.

```text
Spielerlevel
      ↓
Spielerwerte
      ↓
Quest-XP
      ↓
Begleiterlevel
      ↓
Begleiterwerte
      ↓
Gegner / Elite / Boss
```

Ziel ist nicht maximale Stärke, sondern ein stimmiges WotLK-inspiriertes Progressionsgefühl.

Besonders zu testen:

- Level 1–10
- Level 20–30
- Level 40–50
- Level 60–70
- Level 80–90
- Level 99

Jede Seltenheit muss sich sinnvoll anfühlen, ohne die Spielerklasse zu entwerten.

---

# 31. Datenvalidierung

Beim Laden der JSON-Dateien müssen ungültige Daten erkannt und sauber geloggt werden.

Beispiele:

- unbekannte Quest-ID
- unbekannter Questtyp
- Level außerhalb 1–99
- unbekannte EntityType
- ungültige Seltenheit
- negative XP
- ungültige Belohnung
- Unique ohne `adminOnly`
- Unique mit `renameable: true`

Ein fehlerhafter Datensatz darf nicht den gesamten Serverstart zerstören, sofern ein sicherer Fallback möglich ist.

---

# 32. Testreihenfolge

Die Implementierung und Prüfung erfolgt in dieser Reihenfolge:

```text
1. JSON-Laden / Validierung
2. Quest-NPC-Auswahl
3. Level-Freischaltung
4. 5-Quest-Limit
5. Questannahme
6. Questfortschritt
7. Questabschluss
8. Questbelohnungen
9. Questketten
10. Dorf-/Filler-NPCs
11. Recovery Compass
12. Begleiter-Freischaltung
13. Rufen / Wegschicken
14. Folgen
15. Umbenennen
16. Unique-Regeln
17. Begleiter-XP
18. Begleiterlevel
19. Begleitergröße
20. Balanceprüfung
```

Kampffunktionen für Begleiter kommen erst danach.

---

# 33. Phase C / spätere Systeme

Die ursprünglich geplante Phase C bleibt vorerst ausdrücklich zurückgestellt.

Wir investieren zunächst in:

- Quest-System
- Questdaten
- Quest-NPCs
- Filler-/Reise-NPCs
- Recovery Compass
- Begleiter-Grundsystem
- Begleiterdaten
- UI
- Tests

Erst wenn das Plugin angenommen wurde und über längere Zeit stabil läuft, wird Phase C neu bewertet.

---

# 34. Aktueller Implementierungsstand

Stand: **22.08.2026**

Bereits als Fundament vorhanden:

- Levelsystem 1–99
- datengetriebene `quests.json`
- datengetriebene `companions.json`
- maximal 5 aktive Quests
- Questfreischaltung mit 5-Level-Vorlauf
- Questtypen `HUNT`, `COLLECT`, `TALK_TO_NPC`, `ESCORT`, `REACH_LOCATION`, `GLOBAL_EVENT`
- Quest-XP / Geld / Item-Belohnungen
- Begleiterbelohnungen über `companionId`
- eigener Begleiter-XP-Fortschritt
- Begleiterlevel 1–99
- aktive Begleiter erhalten XP, inaktive nicht
- normale Begleiter können umbenannt werden
- Unique-Begleiter sind als Admin-only-Konzept vorgesehen
- passive Begleiterphase
- Test-Begleiter für Wolf, Biene, Zombie, Schwein und Warden
- Questbeispiel für einen Filler-/NPC-Folgepunkt

Noch abzuarbeiten:

- vollständige Questketten über die Levelbereiche 1–99
- weitere Questtypen und deren echte Progressionslogik
- Filler-/Reise-NPC vollständig als Admin-Werkzeug
- Recovery-Compass-Zielsystem
- saubere Begleiter-Folgebewegung
- vollständige Begleiter-Definitionen und Größenwerte
- separates Begleiter-Statdatenmodell
- Unique-Mannequin mit festem Namen/Skin
- vollständige Freischaltungslogik für normale Begleiter
- Questbelohnungs- und Begleiter-XP-Verzahnung
- umfangreiche Balance- und Regressionstests
- spätere Begleiter-Kampffunktionen

---

# 35. Definition of Done

Die Quest-/Begleiter-Grundphase gilt erst als fertig, wenn:

- der Compiler grün durchläuft
- JSON-Dateien ohne Java-Codeänderung bearbeitet werden können
- neue Quests nur über Daten hinzugefügt werden können
- neue Begleiterdefinitionen nur über Daten hinzugefügt werden können
- maximal fünf aktive Quests zuverlässig erzwungen werden
- die Level-5-Frühfreischaltung funktioniert
- Questketten sauber fortgesetzt werden
- Filler-NPCs als echte Ziele funktionieren
- Recovery Compass ein klares Questziel verfolgt
- Begleiter persistent gespeichert werden
- Rufen / Wegschicken zuverlässig funktioniert
- Begleiter dem Spieler folgen
- normale Begleiter umbenannt werden können
- Unique-Begleiter nicht umbenannt werden können
- Begleiter-XP nur bei aktiver Begleitung vergeben wird
- Level 1–99 korrekt funktioniert
- Größe getrennt von Kampfstärke funktioniert
- Quest- und Begleiterbelohnungen zuverlässig gespeichert werden
- keine Duplizierung oder XP-Ausnutzung möglich ist
- die Systeme mit realen Spieler-, Gegner- und Bosswerten getestet wurden

Erst danach wird die Kampffunktion der Begleiter begonnen.
