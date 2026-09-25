# PixelRPG – RP Phase 0 Baseline / Ist-Matrix

**Status:** Phase 0 abgeschlossen  
**Branch:** `test`  
**Referenzbranch:** `main`  
**Prüfdatum:** 25.09.2026  
**Zweck:** Reine Ist-Aufnahme gemäß RP.md Abschnitt 63/77. Keine Balancewerte wurden durch diese Phase verändert.

---

## 1. Prüfgrundlage

Geprüft wurden der aktuelle `test`-Stand, die RP.md, die relevanten Java-Systeme und die vorhandenen Contentdaten.

Relevante Datenquellen:

- `Level.java`
- `PlayerProfile.java`
- `PlayerProfileManager.java`
- `MobScalingConfig.java`
- `MobExperienceListener.java`
- `Profession.java`
- `ProfessionService.java`
- `ProfessionActivityListener.java`
- `Quest.java`
- `QuestManager.java`
- `QuestRepository.java`
- `StoryManager.java`
- `story_campaign.yml`
- `BossRepository.java`
- `BossManager.java`
- `crafting-recipes.json`
- `item-definitions.json`
- `item-scaling.json`
- `mob-scaling.json`
- `food-definitions.json`
- `boss-reward-items.json`
- `companions.json`
- `equipment-sets.json`
- `config.yml`
- `ShopManager.java`
- `LootDropListener.java`
- `TradeDepotManager.java`

### Branch-Situation

`test` und `main` sind divergent. Zum Prüfzeitpunkt liegt `test` 108 Commits vor `main` und 6 Commits hinter `main`. `main` wurde ausschließlich als Referenz verwendet.

---

# 2. CHARAKTER-XP-MATRIX

## 2.1 Aktuelle Levelgrenze

| Eigenschaft | Ist |
|---|---:|
| Mindestlevel | 1 |
| normales Max-Level | 60 |
| reserviertes Level | 61 |
| Gesamt-XP bis Level 60 | 3.379.400 |
| XP-Tabelle | statisch in `Level.java` |
| XP-Typ | kumulativ |

Die RP.md nennt als Referenz ungefähr 3,67 Mio. XP. Der tatsächliche aktuelle Codezustand benötigt **3.379.400 XP** bis Level 60.

## 2.2 XP-Schwellen

| Level | kumulative XP | XP bis nächstes Level |
|---:|---:|---:|
| 1 | 0 | 400 |
| 10 | 27.600 | 7.600 |
| 20 | 159.100 | 20.800 |
| 30 | 440.800 | 38.800 |
| 40 | 977.700 | 74.300 |
| 50 | 1.921.500 | 120.900 |
| 60 | 3.379.400 | 0 |

Die Levelkurve basiert aktuell auf einer festen Liste von 59 Levelintervallen.

## 2.3 Charakter-XP-Quellen

| Quelle | aktueller Mechanismus |
|---|---|
| normale Mobs | `MaxHealth × xpPerMaxHealth` |
| Party-Mob-XP | +20 % Gesamt-XP vor Aufteilung |
| Quests | `reward.experience` |
| Story-Kapitel | `exp-reward` |
| Biome-Bosse | Boss-`exp` |
| World-Bosse | Boss-`exp` |
| Berufsaktivitäten | primär Berufs-XP, nicht Charakter-XP |
| RP/Handel/Gilden | keine direkte Charakter-XP |

---

# 3. MOB-XP-MATRIX

## 3.1 Aktuelle Skalierung

Die Repository-Daten setzen:

| Parameter | Istwert |
|---|---:|
| Basis-HP | 20 + Level × 2,5 |
| Basis-Damage | 2 + Level × 0,15 |
| XP/MaxHealth | 4,0 |
| Player-Parity-Multiplier | 1,0 |
| Gear-Multiplier min. | 0,75 |
| Gear-Multiplier max. | 1,25 |
| Mob-Levelgrenze | 1–60 |

Damit ergibt sich aktuell:

**Mob-HP = 20 + Level × 2,5**

**Mob-XP = MaxHealth × 4**

Beispiele:

| Mob-Level | HP | XP |
|---:|---:|---:|
| 1 | 22,5 | 90 |
| 10 | 45,0 | 180 |
| 20 | 70,0 | 280 |
| 30 | 95,0 | 380 |
| 40 | 120,0 | 480 |
| 50 | 145,0 | 580 |
| 60 | 170,0 | 680 |

Die XP-Matrix stimmt damit mit der Baseline aus RP.md überein.

## 3.2 Party-XP

Die Mob-XP wird bei einer Party zunächst um 20 % erhöht und anschließend auf gültige Empfänger verteilt.

Das bedeutet: Party ist kein reiner Multiplikator pro Spieler; die Gesamtbelohnung wird verteilt.

## 3.3 Noch nicht statisch bestimmbar

Folgende Werte sind im Repository nicht als feste Rate definiert:

- Kills/Stunde
- Killzeit
- Spawnrate
- reale XP/Stunde
- reale Gold/Stunde
- Gearabhängigkeit im Livebetrieb

Diese Werte benötigen Laufzeitmessung und dürfen in Phase 0 nicht erfunden werden.

---

# 4. QUEST-XP-MATRIX

## 4.1 Contentbestand

Aktuell sind **149 Questdefinitionen** vorhanden.

| Questtyp | Anzahl |
|---|---:|
| COLLECT | 53 |
| HUNT | 77 |
| REACH_LOCATION | 17 |
| TALK_TO_NPC | 1 |
| GLOBAL_EVENT | 1 |
| **Gesamt** | **149** |

Verteilung nach Datenquelle:

| Datei | Quests |
|---|---:|
| quests_story.json | 19 |
| quests_v2.json | 36 |
| quests_content_expansion_01.json | 46 |
| quests_expansion_02.json | 19 |
| quests_world_expansion.json | 29 |
| quests_additional.json | 0 |
| **Gesamt** | **149** |

## 4.2 Questbelohnungen

| Wert | Ist |
|---|---:|
| minimale XP-Belohnung | 60 |
| maximale XP-Belohnung | 18.000 |
| Summe aller definierten Quest-XP | 340.975 |
| Durchschnitt pro Quest | 2.288,42 |
| minimale Goldbelohnung | 0 |
| maximale Goldbelohnung | 6.000 |
| Summe aller definierten Quest-Goldbelohnungen | 108.965 |
| Durchschnitt Gold/Quest | 731,31 |

Die Questdaten reichen aktuell bis zur empfohlenen Stufe **99**, obwohl die normale Charakterprogression in `Level.java` bei 60 endet.

Das ist ein zentraler Phase-0-Befund.

## 4.3 Quest-Rewards

- 49 Quests besitzen Itembelohnungen.
- 25 Quests besitzen Companion-Belohnungen.
- Story-, Companion- und normale Questbelohnungen sind im selben Charakter-XP-System angebunden.

---

# 5. STORY-XP-MATRIX

Die Story-Kampagne enthält aktuell 20 definierte Kapitel.

| Wert | Ist |
|---|---:|
| Kapitel | 20 |
| minimale Kapitel-XP | 100 |
| maximale Kapitel-XP | 5.000 |
| gesamte Kapitel-XP | 30.740 |
| niedrigstes Required-Level | 1 |
| höchstes Required-Level | 99 |

Die Kampagne enthält damit ebenfalls Inhalte oberhalb des aktuellen normalen Charakter-Maxlevels 60.

Besonders relevant:

- Kapitel 13: Required Level 61
- Kapitel 14: Required Level 68
- Kapitel 15: Required Level 75
- Kapitel 16: Required Level 82
- Kapitel 17: Required Level 90
- Kapitel 18: Required Level 94
- Kapitel 19: Required Level 99

Das ist kein Balanceurteil, sondern eine dokumentierte Inkonsistenz zwischen aktuellem Content und aktuellem Level-Cap.

---

# 6. BOSS-XP-MATRIX

## 6.1 Biome-Bosse

Aktuell sind **26 Biome-Bosse** definiert.

Levelspanne: **1–60**

XPspanne: **300–5.000**

Goldspanne: **150–1.800**

Gesamte statisch definierte Biome-Boss-XP: **53.750**

Gesamtes statisch definiertes Biome-Boss-Gold: **20.550**

Die Biome-Boss-Level sind:

1, 4, 6, 9, 11, 14, 16, 19, 21, 24, 26, 29, 31, 34, 36, 39, 41, 44, 46, 49, 51, 54, 56, 58, 59, 60.

## 6.2 World-Bosse

Aktuell sind **6 World-Bosse** definiert.

| Boss | Level | Gold | XP |
|---|---:|---:|---:|
| rift_colossus | 40 | 2.500 | 6.000 |
| storm_lord | 44 | 3.000 | 7.000 |
| abyss_lord | 48 | 3.400 | 8.000 |
| soul_devourer | 52 | 3.800 | 9.000 |
| end_harbinger | 56 | 4.200 | 10.000 |
| ancient_world_warden | 60 | 5.000 | 12.000 |

Gesamt:

- **47.000 XP**
- **21.900 Gold**

World-Bosse sind technisch vom Biome-Boss-System getrennt.

---

# 7. BERUFS-XP-MATRIX

## 7.1 Tatsächlicher Berufsbestand

Der aktuelle Code enthält **10 Berufe**, nicht 9:

1. BLACKSMITH
2. SCHOLAR
3. FARMER
4. COOK
5. TAILOR
6. ALCHEMIST
7. MASON
8. FISHERMAN
9. MOUNTAIN_MINER
10. WOODCUTTER

Die RP.md nennt aktuell 9 Berufe und führt keinen MOUNTAIN_MINER in der Liste. Der Code registriert den Bergbauer jedoch als eigenen Beruf.

## 7.2 Tatsächliche Berufslevelgrenze

Aktueller Code:

- MIN_LEVEL = 1
- MAX_LEVEL = 100

Damit ist die aktuelle Berufsprogression **1–100**, nicht 1–60.

Die XP-Kurve lautet:

**XP(Level n) = 50 × (n−1)² + 150 × (n−1)**

| Berufslevel | kumulative XP |
|---:|---:|
| 1 | 0 |
| 10 | 5.400 |
| 20 | 20.900 |
| 30 | 46.400 |
| 40 | 81.900 |
| 50 | 127.400 |
| 60 | 182.900 |
| 70 | 248.400 |
| 80 | 323.900 |
| 90 | 409.400 |
| 100 | 504.900 |

## 7.3 Aktuelle Berufs-XP-Aktivitäten

| Beruf | dokumentierte Aktivität | XP |
|---|---|---:|
| BLACKSMITH | Erz/Metallabbau | 8–35 |
| MOUNTAIN_MINER | Erzabbau | 8–35 |
| FARMER | Pflanzen-/Ernteblöcke | 8 |
| WOODCUTTER | Holzabbau | 8 |
| MASON | Stein-/Baumaterialabbau | 8 |
| ALCHEMIST | Pflanzen/Pilze/Fungus | 8 |
| SCHOLAR | Bookshelves | 5 |
| FISHERMAN | erfolgreicher Fang | 18 |
| COOK | Nutztier-Kill | 8–12 |
| TAILOR | Sheep-Kill | 8 |
| SCHOLAR | Verzaubern | 20 |
| BLACKSMITH | Amboss-Ergebnis | 15 |
| berufsspezifische Quest | Questabschluss | 40 |

Die Werte sind statische Codewerte. Reale XP/Stunde sind nicht gemessen.

## 7.4 Passive Berufe

FISHERMAN, MOUNTAIN_MINER und WOODCUTTER besitzen zusätzliche passive Effekte.

Aktuell:

- Holzfäller: zusätzlicher Holzertrag ab Level 20.
- Bergbauer: zusätzlicher Erz-Ertrag ab Level 20.
- Fischer: zusätzliche Fangmenge ab Level 20 und Schatzersatz/-chance ab Level 40.

Die aktuelle Implementierung skaliert diese Effekte bis Level 100.

---

# 8. CRAFTING-MATRIX

## 8.1 Gesamtbestand

**329 Rezepte**

Aktive Rezeptberufe:

| Beruf | Rezepte | niedrigstes Berufslevel | höchstes Berufslevel |
|---|---:|---:|---:|
| BLACKSMITH | 81 | 1 | 95 |
| SCHOLAR | 68 | 1 | 95 |
| MASON | 58 | 1 | 95 |
| ALCHEMIST | 40 | 1 | 100 |
| TAILOR | 34 | 1 | 65 |
| FARMER | 25 | 1 | 55 |
| COOK | 23 | 1 | 100 |
| **Gesamt** | **329** | | |

FISHERMAN, MOUNTAIN_MINER und WOODCUTTER besitzen aktuell keine Crafting-Rezepte.

## 8.2 Rezept-Unlocks

| Beruf | Default-Unlocks | maximale Unlockkosten | Summe Unlockkosten |
|---|---:|---:|---:|
| BLACKSMITH | 5 | 25.000 | 369.700 |
| FARMER | 2 | 5.400 | 44.700 |
| COOK | 1 | 9.900 | 53.800 |
| TAILOR | 1 | 6.400 | 98.600 |
| ALCHEMIST | 1 | 9.900 | 191.200 |
| MASON | 1 | 9.400 | 308.600 |
| SCHOLAR | 1 | 9.400 | 274.800 |

Gesamte definierte Unlockkosten über alle 329 Rezepte: **1.341.400 Gold**.

Diese Summe ist eine theoretische Summe aller Unlockpreise und keine reale Goldsenke pro Spieler, da Rezepte optional, teilweise default-unlocked und abhängig von Berufsleveln sind.

## 8.3 Crafting-XP

Die aktuelle `CraftRecipe`-Definition besitzt keinen eigenen XP-Wert.

Crafting-XP ist daher in Phase 0 **nicht als expliziter Rezeptwert vorhanden**.

Die Berufs-XP-Vergabe muss deshalb separat über `CraftingService`/Profession-System geprüft werden.

---

# 9. ITEM-MATRIX

## 9.1 Itemdefinitionen

Aktuell: **30 Itemdefinitionen**

| Rarity | Anzahl |
|---|---:|
| COMMON | 5 |
| UNCOMMON | 9 |
| RARE | 6 |
| EPIC | 4 |
| LEGENDARY | 5 |
| UNIQUE | 1 |

Levelspanne der definierten Items: **3–60**

Kategorien:

| Kategorie | Anzahl |
|---|---:|
| MELEE_WEAPON | 5 |
| HELMET | 6 |
| CHESTPLATE | 6 |
| LEGGINGS | 6 |
| BOOTS | 6 |
| RANGED_WEAPON | 1 |

## 9.2 Item-Tier-Istzustand

Der Content ist aktuell nicht vollständig an die RP.md-Tiermatrix gebunden.

Beispiele aus den Definitionen:

- Eisenschwert: Itemlevel 3 / Required 3 / COMMON
- Kupfer-Set: Itemlevel 10 / Required 10 / COMMON
- Ketten-Set: Itemlevel 20 / Required 20 / UNCOMMON
- Eisen-Set: Itemlevel 30 / Required 30 / UNCOMMON
- Gold-Set: Itemlevel 45 / Required 45 / RARE
- Diamant-Set: Itemlevel 60 / Required 60 / EPIC
- Netherite-Set: Itemlevel 60 / Required 60 / LEGENDARY

Die Dateinamen einiger Netherite-Items enthalten historische höhere Levelbezeichnungen, die tatsächlichen `itemLevel`/Required-Level-Werte sind jedoch 60.

## 9.3 Dynamische Itemstats

`RPGItemBuilder` erzeugt Stats dynamisch.

Aktuelle Skalierung:

- growthMultiplier = 2,0
- weaponDamage Basis = 3,0
- crit chance Basis = 0,5
- crit damage Basis = 0,05
- reach Basis = 0,10
- lifesteal Basis = 0,25
- armor Basis = 0,7
- health Basis = 1,2
- movement speed Basis = 0,001
- tool efficiency Basis = 1,0

Rarity bestimmt die Anzahl der ausgewählten Stats:

| Rarity | Stats |
|---|---:|
| COMMON | 2 |
| UNCOMMON | 3 |
| RARE | 4 |
| EPIC | 5 |
| LEGENDARY | 6 |
| UNIQUE | 6 |

Wichtig für die spätere Phase:

Die Werte werden derzeit mit einem Zufallsfaktor von **0,50–1,50** gerollt. Damit ist die aktuelle Itemstat-Erzeugung noch nicht vollständig deterministisch, obwohl RP.md als Ziel „deterministische Basiswerte + kontrollierte Varianz“ festlegt.

## 9.4 Item-Scaling

`item-scaling.json`:

- growthMultiplier: 2,0
- deterministische Levelskalierung: Level 1 → Faktor 1,0; Level 60 → Faktor 2,0
- danach zusätzlicher Zufallsroll 0,50–1,50

Das bedeutet, dass die effektive Statvarianz aktuell größer ist als nur eine kontrollierte kleine Varianz.

---

# 10. EQUIPMENT-SETS

Aktuell: **6 Sets**

Die Definitionen werden in `equipment-sets.json` gehalten.

Vorhandene Setstruktur:

- 2-Teile-Bonus
- 3-Teile-Bonus
- 4-Teile-Bonus

Die sechs Sets sind in den Itemdefinitionen referenziert.

Eine vollständige mathematische Set-Balance wurde in Phase 0 nicht verändert.

---

# 11. FOOD-MATRIX

Aktuell: **23 Food-Definitionen**

Die Definitionen liegen in `food-definitions.json`.

Phase-0-Befund:

- Content existiert.
- Erwerbswege müssen in der Economy-/Contentphase gegen Farm, Koch, Drops, NPC, Crafting, Quest und Spielerhandel abgeglichen werden.
- Diese Prüfung wurde nicht durch erfundene Laufzeitwerte ersetzt.

---

# 12. ECONOMY-MATRIX

## 12.1 Charaktergeld

Das Geld wird als Minor-Unit-Wert im `PlayerProfile` gespeichert.

Vorhanden:

- Balance
- Deposit
- Withdraw
- Quest-Gold
- Boss-Gold
- Trade-Depot-Auszahlungen
- Rezept-Unlocks

## 12.2 Dokumentierte Goldquellen

| Quelle | aktueller Wert/Mechanismus |
|---|---|
| Quests | 0–6.000 Gold je Definition |
| Biome-Bosse | 150–1.800 Gold |
| World-Bosse | 2.500–5.000 Gold |
| Trade Depot | Verkaufspreis, abzüglich 5 % Gebühr |
| Spielerhandel | über Economy-/Trade-System |

Normale Hostile-Mob-Kills erzeugen zusätzlich ein **Gildenwährungs-Item**, nicht direkt Charaktergeld.

## 12.3 Dokumentierte Goldsenken

| Senke | Ist |
|---|---|
| Rezept-Unlocks | bis 25.000 je Rezept |
| Trade Depot | 5 % Verkaufsgebühr |
| NPC-Shops | vorhandene Kaufpreise |
| Spielerhandel | kein Systempreis; Marktpreis |
| Guild-/Bank-System | technisch vorhanden, weitere Preisstruktur separat prüfen |

Default-NPC-Baustoffshop:

- 38 Standardangebote
- Kaufpreise ca. 0,50–10,00 Gold pro Stackdefinition
- Verkaufspreis standardmäßig 50 % des Kaufpreises

---

# 13. TRADE-DEPOT-MATRIX

Aktuell:

| Parameter | Ist |
|---|---:|
| Laufzeit | 7 Tage |
| Verkaufsgebühr | 5 % |
| Mindestpreis | > 0 |
| Spieler darf eigenes Angebot nicht kaufen | ja |
| Auszahlung Verkäufer | 95 % |
| Ablauf ohne Verkauf | Ware zurück an Verkäufer |

Das Trade Depot ist damit eine Goldtransaktion mit expliziter Goldsenke von 5 % pro erfolgreichem Verkauf.

---

# 14. BOSS-/ITEM-ECONOMY

Boss-Rewards sind aktuell getrennt von den normalen PixelRPG-Custom-Items.

`BossManager` ignoriert aktuell `pixelrpg:`-Custom-Item-Rewards aus der normalen Boss-Lootliste.

Die 32 vorhandenen Boss-Reward-Itemdefinitionen existieren separat.

Das ist für die spätere Item-/Boss-Matrix relevant, da Contentdefinition und tatsächliche Rewardausgabe getrennt geprüft werden müssen.

---

# 15. RP-MATRIX

## 15.1 Spielerrollen

Technisch unterstützt werden unter anderem:

- Schmied
- Gelehrter
- Farmer
- Koch
- Schneider
- Alchemist
- Steinmetz
- Fischer
- Bergbauer
- Holzfäller
- Questspieler
- Monsterjäger
- Händler
- Gildenmitglied
- Solo-Spieler

## 15.2 Aktuelle direkte Progressionsabhängigkeit

| Handlung | Charakter-XP | Berufs-XP | Geld | soziale Funktion |
|---|---:|---:|---:|---|
| RP-Gespräch | nein | nein | nein | hoch |
| Spielerhandel | nein | nein | ja | hoch |
| Crafting | nicht als Rezeptwert definiert | systemabhängig | Material/Unlock | hoch |
| Mob-Kill | ja | teilweise | Gildenwährung | mittel |
| Quest | ja | bei Präfixquests teilweise | ja | mittel/hoch |
| Boss | ja | nein | ja | hoch |
| Gilde | nein | nein | systemabhängig | sehr hoch |
| Reisen | nein | nein | nein | mittel |

---

# 16. PHASE-0-BEFUNDE

Die folgenden Punkte sind reine Ist-Fakten und keine bereits beschlossenen Balanceänderungen.

### Befund A – Charakterlevel

RP.md Ziel: 1–60.

Code: 1–60.

Ist-XP bis 60: **3.379.400**.

### Befund B – Questcontent

149 Quests vorhanden.

Questdaten enthalten empfohlene Level bis **99**.

### Befund C – Storycontent

20 Storykapitel vorhanden.

Storyanforderungen reichen bis **99**.

### Befund D – Berufe

RP.md beschreibt 9 Berufe.

Code enthält **10 Berufe**.

Der zusätzliche Beruf ist **MOUNTAIN_MINER**.

### Befund E – Berufslevel

RP.md Ziel: Berufe 1–60.

Code: **1–100**.

Mehrere Rezepte reichen ebenfalls bis Berufslevel 100.

### Befund F – Itemstats

RP.md Ziel: deterministische Basiswerte + kontrollierte Varianz.

Aktueller ItemBuilder verwendet zusätzlich einen Zufallsroll von **0,50–1,50**.

### Befund G – Crafting-XP

`CraftRecipe` enthält keinen expliziten XP-Wert.

Crafting-XP ist daher nicht als reine Rezeptdatenmatrix vorhanden.

### Befund H – Laufzeitmetriken

XP/Stunde, Gold/Stunde, Killzeit, Kills/Stunde, Produktionszeit und reale Nachfrage sind nicht vollständig statisch aus dem Repository ableitbar.

Diese Werte müssen für die späteren Balancephasen instrumentiert/gemessen werden.

### Befund I – Buildstruktur

Der aktuelle `test`-Tree enthält `build.gradle`, aber keine `settings.gradle`.

Die verbindliche Projektvorgabe fordert eine `settings.gradle`. Dieser Befund wird dokumentiert, in Phase 0 aber nicht eigenmächtig repariert.

---

# 17. PHASE-0-DEFINITION-OF-DONE

| Punkt | Status |
|---|---|
| Charakter-XP-Matrix | ERFÜLLT |
| Quest-XP-Matrix | ERFÜLLT |
| Mob-XP-Matrix | ERFÜLLT |
| Boss-XP-Matrix | ERFÜLLT |
| Berufs-XP-Matrix | ERFÜLLT |
| Crafting-Matrix | ERFÜLLT |
| Gold-Matrix | ERFÜLLT |
| Item-Stat-Matrix | ERFÜLLT |
| RP-Matrix | ERFÜLLT |
| Ist-/Soll-Abweichungen dokumentiert | ERFÜLLT |
| Balancewerte verändert | NEIN |
| main verändert | NEIN |
| test verändert | NUR diese Baseline-Dokumentation |

---

# 18. Übergabe an Phase 1

Phase 0 liefert damit die verbindliche Ist-Baseline.

Phase 1 darf auf dieser Grundlage:

1. Charakterlevel 1–60 finalisieren.
2. XP-Kurve gegen reale Spielzeit messen.
3. XP-Quellen aufeinander abstimmen.
4. Levelzeiten 10/20/30/40/50/60 bestimmen.
5. Die dokumentierten 99er-Contentanforderungen gegen das Level-60-Ziel prüfen.
6. Erst danach konkrete Zahlen verändern.

**Keine Phase-1-Balanceentscheidung wurde in Phase 0 vorweggenommen.**
