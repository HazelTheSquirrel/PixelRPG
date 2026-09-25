# PixelRPG – RP Phase 2 – Berufe

**Status:** Phase 2 abgeschlossen  
**Branch:** `test`  
**Referenz:** `main` ausschließlich als Soll-/Vergleichszustand  
**Prüfstand:** 25.09.2026

---

## 1. Ziel der Phase

Phase 2 aus `RP.md`:

- Berufe auf 1–60 ausrichten
- Rezepte zuordnen
- Materialtiers definieren
- Berufs-XP definieren
- Unlocks erhalten und validieren
- Produktionsökonomie prüfen

Die vorhandene Crafting-Struktur mit **329 Rezepten** wurde weiterverwendet.

---

## 2. Berufsmodell

Die RP.md definiert neun Berufe:

### Aktiv
1. BLACKSMITH
2. SCHOLAR
3. FARMER
4. COOK
5. TAILOR
6. ALCHEMIST
7. MASON

### Passiv
8. FISHERMAN
9. WOODCUTTER

Der bisherige zehnte Code-Beruf `MOUNTAIN_MINER` wurde aus der tatsächlichen Enum-Professionenliste entfernt.

### Kompatibilität

Bestehende Daten werden nicht blind verworfen:

- historische `mountain_miner`-YAML-Daten werden in BLACKSMITH übernommen,
- historische MySQL-Statkeys `MOUNTAIN_MINER` werden beim Laden auf BLACKSMITH gemappt,
- ein veralteter Java-Kompatibilitätsalias bleibt vorübergehend vorhanden,
- BLACKSMITH übernimmt damit die Rohstoff-/Metallprogression des früheren Bergbauerpfads.

Damit entstehen keine zwei konkurrierenden Bergbau-/Metallberufe.

---

## 3. Berufslevel 1–60

Alle neun Berufe verwenden:

- Mindestlevel: **1**
- Maximallevel: **60**
- maximale Berufs-XP: **182.900**

Die bestehende quadratische XP-Kurve wurde nicht willkürlich ersetzt:

`XP(Level n) = 50 × (n−1)² + 150 × (n−1)`

Meilensteine:

| Level | kumulative Berufs-XP |
|---:|---:|
| 1 | 0 |
| 10 | 5.400 |
| 20 | 20.900 |
| 30 | 46.400 |
| 40 | 81.900 |
| 50 | 127.400 |
| 60 | 182.900 |

XP und Level werden oberhalb Level 60 nicht mehr erhöht.

---

## 4. Materialtiers

Die sechs Berufstiers sind jetzt als zentrale technische Struktur vorhanden:

| Tier | Berufslevel | Material-/Rollenstufe |
|---|---:|---|
| I | 1–10 | Leder/Holz |
| II | 11–20 | Kupfer |
| III | 21–30 | Eisen |
| IV | 31–40 | Diamant |
| V | 41–50 | Netherite |
| VI | 51–60 | Custom-Endgame |

Technische Umsetzung:

`ProfessionTier`

mit einer deterministischen `forLevel(int)`-Zuordnung.

---

## 5. Rezeptbestand

Der Bestand bleibt vollständig erhalten:

**329 Rezepte**

| Beruf | Rezepte |
|---|---:|
| BLACKSMITH | 81 |
| SCHOLAR | 68 |
| FARMER | 25 |
| COOK | 23 |
| TAILOR | 34 |
| ALCHEMIST | 40 |
| MASON | 58 |
| FISHERMAN | 0 |
| WOODCUTTER | 0 |
| **Gesamt** | **329** |

FISHERMAN und WOODCUTTER bleiben passive Berufe und erhalten keine künstlichen Crafting-Rezepte.

---

## 6. Rezept-Levelnormalisierung

Der vorherige Rezeptbestand enthielt Berufslevel bis 100.

Statt die Werte blind auf 60 zu kappen, wurden sie **pro Beruf proportional auf 1–60 normalisiert**.

Dabei gilt:

- ursprüngliches Level 1 bleibt Level 1,
- das bisher höchste Level des jeweiligen Berufs wird Level 60,
- die Reihenfolge der Rezepte bleibt erhalten,
- Rezepte desselben ursprünglichen Levels bleiben auf demselben neuen Level,
- kein Rezept bleibt oberhalb Level 60.

Aktueller maximaler Rezeptlevel:

**60 bei allen sieben Crafting-Berufen.**

Damit wird beispielsweise die bisherige Blacksmith-95-Progression nicht einfach in Level 60 zusammengequetscht, sondern über den gesamten 1–60-Bereich verteilt.

---

## 7. Berufs-XP pro Rezept

Crafting-XP ist jetzt ein **expliziter Bestandteil jedes Rezeptdatensatzes**:

`professionXp`

Die bisherigen impliziten Werte wurden als Ausgangsbasis materialisiert:

`max(20, requiredLevel × 6 + Materialmenge × 5 + CustomItemmenge × 10)`

Damit gilt:

- XP ist auditierbar,
- XP ist nicht mehr ausschließlich in Java versteckt,
- spätere Balanceänderungen können pro Rezept erfolgen,
- Rezept-XP bleibt Berufs-XP und wird nicht automatisch Charakter-XP.

Gesamte aktuell definierte Crafting-XP über einmalige Herstellung aller 329 Rezepte:

**61.390 Berufs-XP**

Das ist eine statische Content-Summe und keine reale XP/Stunde.

---

## 8. Unlocks

Die bestehenden Unlockkosten wurden erhalten.

Gesamte definierte Unlockkosten:

**1.341.400 Gold**

Diese Summe bedeutet ausdrücklich nicht, dass jeder Spieler diese Summe ausgeben muss.

Sie umfasst:

- optionale Rezepte,
- bereits standardmäßig freigeschaltete Rezepte,
- verschiedene Berufslevel,
- optionale Questfreischaltungen.

Die vorhandene Unlocklogik bleibt:

1. Beruf muss erlernt sein.
2. Berufslevel muss ausreichen.
3. Questvoraussetzungen werden geprüft.
4. Optionaler Goldpreis wird geprüft.
5. Rezept wird dauerhaft freigeschaltet.

---

## 9. Produktionsökonomie

Jedes Rezept besitzt weiterhin einen validierten Produktionspfad:

`Materialien / Custom Items → Rezept → Output`

Die Registry erzwingt:

- mindestens einen Material-/Itemkostenblock,
- positive Mengen,
- gültige Vanilla-Materialien,
- gültige PixelRPG-Items,
- keine Admin-/Unique-Items als normale Rezeptkosten,
- keine Admin-/Unique-Items als normale Rezeptausgabe,
- gültiges Berufslevel,
- positive Berufs-XP,
- gültige Unlockkosten,
- keine zirkulären Custom-Item-Rezeptabhängigkeiten.

Damit kann kein Rezept als kostenlose Endlosschleife definiert werden.

---

## 10. Spielerabhängigkeiten

Die Berufssystematik bleibt auf Spezialisierung ausgerichtet:

- BLACKSMITH → Metall / Werkzeuge / Waffen / Rüstung
- SCHOLAR → Wissen / Bücher / Verzauberung
- FARMER → Pflanzen / landwirtschaftliche Rohstoffe
- COOK → Nahrung
- TAILOR → Wolle / Leder / tragbare Produkte
- ALCHEMIST → Pflanzen / Tränke / alchemistische Produkte
- MASON → Stein / Baublöcke
- FISHERMAN → Angeln
- WOODCUTTER → Holz

Passiv bedeutet nicht „wertlos“:

FISHERMAN und WOODCUTTER erzeugen ihre Progression über Weltaktivitäten und passive Vorteile statt über eine künstliche Rezeptliste.

---

## 11. Bergbau-Migration

Der frühere `MOUNTAIN_MINER` war eine Abweichung von der RP.md.

Die Rohstoffaktivität wird deshalb nicht als elfter bzw. zehnter Beruf weitergeführt.

Erz-/Metallabbau liefert jetzt BLACKSMITH-Berufs-XP.

Historische Profile werden migriert, damit vorhandener Fortschritt nicht verloren geht.

Das verhindert:

`Bergbauer → Schmied → doppelte Metallprogression`

und führt zu:

`Rohstoffgewinnung → BLACKSMITH → Metallverarbeitung`

---

## 12. Technische Änderungen

Auf `test` geändert:

- `Profession`
  - 9 tatsächliche Enum-Berufe
  - Level 1–60
  - XP-Cap 182.900
  - historischer Kompatibilitätsalias für MOUNTAIN_MINER

- `ProfessionTier`
  - neue sechsstufige Berufstier-Matrix

- `ProfessionActivityListener`
  - Bergbau nicht mehr als separater Beruf
  - Erz-XP läuft über BLACKSMITH
  - obsolete separate Bergbauer-Dropprogression entfernt

- `PlayerProfile`
  - Berufs-XP wird auf Level-60-Cap begrenzt

- `YamlPlayerProfileRepository`
  - historische Mountain-Miner-Daten werden in BLACKSMITH migriert

- `MySQLPlayerProfileRepository`
  - historische Mountain-Miner-Statkeys werden auf BLACKSMITH gemappt

- `CraftRecipe`
  - explizites `professionXp`

- `CraftingRecipeRegistry`
  - `professionXp` wird validiert
  - bestehende Berechnung bleibt als Fallback für alte Daten

- `CraftingService`
  - verwendet den expliziten Rezept-XP-Wert

- `crafting-recipes.json`
  - alle 329 Rezepte auf Berufslevel 1–60 normalisiert
  - alle 329 Rezepte besitzen explizite Berufs-XP

---

## 13. Was bewusst NICHT gemacht wurde

Keine künstliche Produktionszeit eingeführt.

Der bestehende Crafting-Service produziert momentan unmittelbar. Ein Produktions-Timer wäre ein neues Gameplay-System und würde nicht nur eine Balancekorrektur darstellen.

Ebenso wurden keine Verkaufspreise erfunden. Die tatsächliche Produktionswirtschaft hängt vom bestehenden Shop-/Spielerhandelsmarkt und den realen Materialwerten ab und wird in Phase 4 als eigene Economy-Phase vermessen.

---

## 14. Definition of Done – Phase 2

| Punkt | Status |
|---|---|
| alle 9 Berufe 1–60 | ERFÜLLT |
| Berufs-XP definiert | ERFÜLLT |
| Level-60-XP-Cap | ERFÜLLT |
| Materialtiers 1–60 | ERFÜLLT |
| 329 Rezepte erhalten | ERFÜLLT |
| Rezepte den Berufen zugeordnet | ERFÜLLT |
| alle Rezeptlevel ≤ 60 | ERFÜLLT |
| Rezept-XP explizit | ERFÜLLT |
| Unlockkosten erhalten/validiert | ERFÜLLT |
| Produktionskosten validiert | ERFÜLLT |
| Custom-Item-Abhängigkeiten validiert | ERFÜLLT |
| Mining-Duplikat bereinigt | ERFÜLLT |
| historische Mining-Progression migrierbar | ERFÜLLT |
| main verändert | NEIN |
| test verändert | JA |

### Ergebnis

**Phase 2 ist technisch abgeschlossen.**

Die tatsächliche XP/Stunde und Gold/Stunde der Berufe bleiben reale Messwerte und werden nicht aus statischen Rezeptdaten erfunden. Die technische Grundlage für diese spätere Wirtschaftsmessung ist jetzt deterministisch und auditierbar.
