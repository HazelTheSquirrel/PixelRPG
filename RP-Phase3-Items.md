# PixelRPG – RP Phase 3 – Items

**Status:** Phase 3 abgeschlossen  
**Branch:** `test`  
**Referenz:** `main` ausschließlich als Sollzustand  
**Datum:** 25.09.2026

## Ziel

Phase 3 aus `RP.md`:

- Tiermatrix
- Slotmatrix
- deterministische Basiswerte
- Rarity
- Setboni
- Weapon Abilities
- Unique Items
- Boss Items

## Ist-Aufnahme

Der Test-Branch enthält:

- 30 zentrale Itemdefinitionen
- 32 Boss-Reward-Definitionen
- 6 Equipment-Sets
- 1 Unique-Item-Definition
- 2 soulbound Itemdefinitionen
- 62 konkrete Definitionen über zentrale und Boss-Registry

Kein Item liegt oberhalb Charakter-/Itemlevel 60.

## Item-Tier-Matrix

| Tier | Itemlevel | Bedeutung |
|---|---:|---|
| STARTER | 1–10 | Starter |
| COPPER | 11–20 | Kupfer |
| IRON | 21–30 | Eisen |
| DIAMOND | 31–40 | Diamant |
| NETHERITE | 41–50 | Netherite |
| CUSTOM_ENDGAME | 51–60 | Custom-Endgame |

Technische Quelle: `ItemTier`.

Die Zuordnung erfolgt ausschließlich aus dem Itemlevel und ist damit deterministisch. Das Tier erzeugt **kein Regions- oder Welt-Gating**.

## Slotmatrix

Die zentralen Equipmentdefinitionen verwenden:

- HELMET
- CHEST
- LEGS
- FEET
- MAINHAND/OFFHAND über die vorhandene Equipment-Infrastruktur

Die sechs bestehenden Sets bestehen jeweils aus vier Rüstungsteilen.

## Deterministische Basiswerte

Die bisherige generische Itemerzeugung verwendete zufällige Stat-Auswahl und einen zufälligen Rollfaktor von 0,5–1,5.

Das wurde für die Phase-3-Basisprogression entfernt.

Ein Item erhält jetzt:

- deterministische Stat-Auswahl abhängig vom Itemprofil,
- deterministische Werte aus Itemlevel × Rarity-Multiplikator,
- kontrollierte Varianz über Rarity statt über einen zufälligen Basis-Roll.

Damit besitzt dasselbe definierte Item bei jeder Erstellung dieselben Basiswerte.

### Waffen

Feste Priorität:

1. ATTACK_POWER
2. CRIT
3. CRIT_DAMAGE
4. REACH
5. LIFESTEAL
6. HP

### Rüstung

Feste Priorität:

1. ARMOR
2. HP
3. MOVEMENT_SPEED
4. CRIT
5. CRIT_DAMAGE
6. LIFESTEAL

### Schild

Feste Priorität:

1. ARMOR
2. HP
3. REACH
4. CRIT
5. CRIT_DAMAGE
6. LIFESTEAL

Die vorhandene Rarity bestimmt weiterhin die Anzahl der aktiven Werte:

- COMMON: 2
- UNCOMMON: 3
- RARE: 4
- EPIC: 5
- LEGENDARY: 6
- UNIQUE: 6

Damit ist die Stat-Auswahl nicht mehr zufällig.

## Rarity

Bestehende sechs Rarities bleiben:

- COMMON
- UNCOMMON
- RARE
- EPIC
- LEGENDARY
- UNIQUE

Bestehende Multiplikatoren bleiben erhalten:

- COMMON 1,00
- UNCOMMON 1,10
- RARE 1,22
- EPIC 1,38
- LEGENDARY 1,60
- UNIQUE 1,60

LEGENDARY und UNIQUE sind weiterhin nicht normale Zufallsdrops.

## Gearscore

Gearscore bleibt deterministisch:

`Itemlevel × Rarity-Multiplikator × GearscoreModifier`

Die bestehende Definition erhält dadurch einen nachvollziehbaren Ausrüstungswert.

## Equipment-Sets

Alle sechs vorhandenen Sets bleiben erhalten:

1. Donnerwacht
2. Schattengeflecht
3. Stahlwall
4. Sonnengewand
5. Kristallwache
6. Höllenschmiede

Jedes Set besitzt:

- 2-Teile-Bonus
- 3-Teile-Bonus
- 4-Teile-Bonus

Jedes Set besitzt aktuell vier konkrete Ausrüstungsteile.

Die vorhandenen Setwerte wurden nicht ohne Messung neu skaliert.

## Setbonus-Audit

Die Setdefinitionen bleiben datengetrieben über `equipment-sets.json`.

Die sechs Sets verwenden unterschiedliche Rollen:

- Donnerwacht: Mobilität/Reichweite/Angriff
- Schattengeflecht: Krit/Lebensraub
- Stahlwall: Defensive + Angriff
- Sonnengewand: Mobilität/Krit
- Kristallwache: Angriff/Defensive/Kritschaden
- Höllenschmiede: HP/Angriff/Lebensraub

Die 2-/3-/4-Teile-Struktur bleibt bestehen.

## Weapon Abilities

Die vorhandenen Weapon Abilities bleiben erhalten.

Definitionen können weiterhin:

- Ability-ID
- Cooldown
- Kategorie
- Material
- Itemlevel

festlegen.

Die Ability wird nicht zur Grundlage der numerischen Itemskalierung gemacht. Dadurch bleiben Basiswerte und aktive Fähigkeit getrennte Balanceachsen.

## Unique Items

Aktuell existiert eine zentrale UNIQUE-Definition:

`pixelrpg:unique/admin_relic`

Sie ist:

- UNIQUE
- soulbound
- adminOnly
- serverweit über `UniqueItemService` claimbar

Die vorhandene One-Instance-Regel bleibt erhalten.

Es wurden keine zusätzlichen Spieler-Unique-Items erfunden, weil dafür kein belastbarer Content-/Erwerbsweg in der bestehenden Datenbasis existiert.

## Soulbound

Aktuell sind zwei zentrale Itemdefinitionen soulbound.

Soulbound wird nicht pauschal auf alle starken Items angewandt.

Das unterstützt die RP-Economy:

- hochwertige normale Items bleiben handelbar,
- persönliche Items können gebunden werden,
- Unique-/Admin-Content bleibt kontrolliert.

## Boss Items

Die 32 Boss-Reward-Items bleiben vollständig erhalten.

Sie verwenden eigene `pixelrpg:boss/*` IDs und besitzen definierte:

- Rarity
- Itemlevel
- Required Level
- Ability
- Ability-Cooldown

Keines liegt oberhalb Level 60.

Die Bossitems werden nicht in normale Crafting-/NPC-Standardware umgewandelt.

## Levelverteilung

Alle 62 zentral geprüften Item-/Bossdefinitionen:

- Level >= 1
- Level <= 60
- Required Level >= 1
- Required Level <= 60
- Required Level <= Itemlevel

Tierverteilung:

| Tier | Anzahl |
|---|---:|
| STARTER | 5 |
| COPPER | 6 |
| IRON | 8 |
| DIAMOND | 6 |
| NETHERITE | 11 |
| CUSTOM_ENDGAME | 26 |

Die hohe Anzahl im Endgame entsteht insbesondere durch die vorhandenen Boss-Rewards und ist deshalb nicht automatisch ein Fehler. Eine spätere Phase kann deren Drop-/Erwerbsfrequenz wirtschaftlich messen.

## Bewusst nicht gemacht

Keine erfundenen neuen Items.

Keine erfundenen neuen Sets.

Keine neue Rarity.

Keine zufällige Statvergabe.

Keine Level über 60.

Keine künstlichen Regionsgates.

Keine pauschale Soulbound-Pflicht.

Keine Entfernung bestehender Weapon Abilities.

Keine Änderung der sechs Setidentitäten ohne vorherige Balance-Messung.

## Technische Änderungen

- neue `ItemTier`
- ItemDefinition auf Level 1–60 begrenzt
- ItemService auf Level 1–60 begrenzt
- RPGItemBuilder erzeugt deterministische Equipmentstats
- zufällige Stat-Auswahl entfernt
- zufälliger 0,5–1,5-Roll entfernt
- Itemtier wird in der Itemdarstellung angezeigt
- doppelte Armor-Crit-Anwendung im ItemService entfernt
- bestehende Rarity-/Gearscore-Logik erhalten
- bestehende UniqueItemService-Logik erhalten
- bestehende Set-/Boss-Daten erhalten

## Definition of Done

| Punkt | Status |
|---|---|
| Tiermatrix | ERFÜLLT |
| Itemlevel 1–60 | ERFÜLLT |
| Slotmatrix geprüft | ERFÜLLT |
| deterministische Basiswerte | ERFÜLLT |
| kontrollierte Rarity-Varianz | ERFÜLLT |
| sechs Rarities | ERFÜLLT |
| sechs Sets | ERFÜLLT |
| 2/3/4-Setboni | ERFÜLLT |
| Weapon Abilities | ERFÜLLT |
| Unique-System | ERFÜLLT |
| Boss Items | ERFÜLLT |
| Level > 60 | 0 |
| main verändert | NEIN (Endzustand geprüft) |
| test verändert | JA |

## Build-Verifikation

Commit `883303aad43eead67eed51252d10740efa75286d` wurde durch den bestehenden GitHub-Build vollständig verifiziert:

- Build PixelRPG: **SUCCESS**
- Verify source API boundaries: **SUCCESS**
- Verify plugin artifact: **SUCCESS**
- Java 25: **SUCCESS**

## Abgrenzung

Phase 3 definiert die **technische Itemprogression**.

Die tatsächliche Kampfparität, DPS, TTK, Bossbalance, Goldwertigkeit und Drop-Effizienz gehören gemäß `RP.md` in die späteren Combat-/Economy-Phasen.

Ein Item mit Level 60 ist deshalb nicht automatisch als „balanciert“ gegen Level-60-Mobs zu betrachten. Die mathematische Kampfbalance wird separat gemessen.
