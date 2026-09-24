# PixelRPG – Content-Audit

**Prüfobjekt:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `test`  
**Prüfstand:** 24.09.2026  
**Referenz:** `main`  
**Zweck:** Prüfung auf **Content-Lücken**, nicht auf technische Runtime-/Security-Fehler.

> Dieses Dokument beantwortet die Frage: „Ist die Implementierung bereits sinnvoll mit Content gefüllt, oder existiert zwar Code, aber der Spieler kann das System noch nicht sinnvoll erleben?“
>
> Ein System wird deshalb nicht als unfertig markiert, nur weil Balancing, weitere Varianten oder spätere Erweiterungen möglich sind.

---

# 1. Bewertungsmaßstab

## 🟢 FERTIG

Die technische Implementierung besitzt genügend konkreten Content für einen vollständigen vorgesehenen Gameplay-Loop.

Weitere Inhalte sind optionales Balancing, Erweiterung oder DLC-/Endgame-Content.

## 🟡 CONTENT-LÜCKE

Die Implementierung funktioniert grundsätzlich, aber wesentliche Daten, Bezugsquellen, NPCs, Shops, Rezepte, Weltpositionen oder Varianten fehlen.

Hier sollte **primär Content** ergänzt werden.

## 🟠 CONTENT-/IMPLEMENTIERUNGS-LÜCKE

Der vorhandene Content zeigt die gewünschte Richtung, kann aber mit der aktuellen Implementierung nicht sinnvoll in den vorgesehenen Gameplay-Loop gebracht werden.

Hier muss vor dem Content-Ausbau ggf. eine kleine technische Erweiterung erfolgen.

## ⚪ BEWUSST PAUSIERT

Technik ist vorhanden, die Welt-/Content-Ausgestaltung ist absichtlich zurückgestellt.

---

# 2. Executive Summary

Der aktuelle `test`-Stand besitzt bereits sehr viele Systeme und ungewöhnlich viel Daten-Content.

Die größten **tatsächlichen Content-Lücken** liegen derzeit nicht bei Quests, Bossen oder Companion-Definitionen, sondern bei der Verbindung zwischen vorhandenen Content-Definitionen und einer für Spieler erreichbaren Welt.

Die wichtigsten Befunde:

1. 🟡 **NPC-Weltpopulation fehlt als Default-Content.**
2. 🟡 **Shop-System ist technisch fertig, aber ein frischer Stand besitzt keine mitgelieferten Shop-Inhalte.**
3. 🟠 **Die 29 RPG-Itemdefinitionen sind vorhanden, aber die normalen Custom-Waffen und sechs Rüstungssets haben im Repository keinen erkennbaren normalen Erwerbsweg.**
4. 🟠 **23 Food-Definitionen existieren, sind aber nicht Teil der 29 Itemdefinitionen; die Crafting-Rezepte erzeugen ausschließlich Vanilla-Materialien. Damit ist der Food-Katalog als Spieler-Content nicht ausreichend angebunden.**
5. 🟢 **Crafting besitzt bereits 300 Rezepte über alle sieben aktiven Crafting-Berufe.**
6. 🟢 **Quest-Content ist umfangreich: 269 reguläre Definitionen plus 17 aktive Story-Strukturquests.**
7. 🟢 **Boss-Content ist umfangreich: 26 Biome-Bosse, 6 World-Bosse und 32 Boss-Reward-Items.**
8. 🟢 **Companion-Content ist umfangreich: 28 Definitionen mit Quest-, Default-, Boss- und Admin-Freischaltungen.**
9. 🟢 **Equipment besitzt tatsächlich sechs vollständige Sets, nicht nur ein Set. Das Problem ist primär die Erwerbbarkeit.**
10. ⚪ **Regionen und Resourcepack-Ausbau bleiben bewusst pausiert.**
11. 🟢 **Bank kann als fertige Implementierung behandelt werden. Die geplante physische Währung bis Stackgröße 999 ist eine gezielte Economy-/Drop-Änderung, keine generelle Bank-Content-Lücke.**

---

# 3. Player / Progression

## 🟢 Player Profile / Level / XP

Vorhanden:

- PlayerProfile
- Level
- Erfahrung
- Story-Fortschritt
- Quest-Fortschritt
- Profession-Fortschritt
- Companion-Fortschritt
- Playtime
- YAML/MySQL-Persistenz

### Content-Lücke

Keine wesentliche Content-Lücke.

Levelbereiche und Anforderungen werden bereits von Items, Rezepten, Quests, Bossen und Story verwendet.

**Status: 🟢 FERTIG**

---

# 4. Items

## Bestand

`item-definitions.json`: **29 Definitionen**

Kategorien:

- 5 Waffen
- 6 Helme
- 6 Brustplatten
- 6 Beinschützer
- 6 Stiefel

Davon:

- 28 normale Spieler-Items
- 1 Unique/Admin-Relikt

Normale Waffen:

- Eisenschwert
- Goldklinge
- Diamantklinge
- Netheritklinge

Zusätzlich existiert das Admin-Relikt.

Die Rüstungsprogression umfasst:

1. Donnerwacht – Kupfer – Level 10
2. Schattengeflecht – Ketten – Level 20
3. Stahlwall – Eisen – Level 30
4. Sonnengewand – Gold – Level 45
5. Kristallwache – Diamant – Level 60
6. Höllenschmiede – Netherit – Level 80

## 🟠 Erwerbs-Content fehlt

Die Itemdefinitionen existieren, aber die überprüften Content-Dateien referenzieren diese Custom-Item-IDs nicht in:

- Crafting-Rezepten
- Questdefinitionen
- Companion-Definitionen
- Food-Definitionen
- Boss-Reward-Definitionen

Die Crafting-Implementierung ist derzeit ausdrücklich auf Vanilla-Material-Ergebnisse ausgelegt.

Damit sind die Custom-Waffen und Rüstungssets zwar **technisch erzeugbar**, aber als normaler Spieler-Content noch nicht ausreichend in die Progression eingebunden.

### Konkret benötigt

Für die 28 normalen Custom-Items sollten Erwerbswege definiert werden.

Beispiel:

| Itemgruppe | Mögliche Erwerbsquelle |
|---|---|
| Eisenschwert | Schmied / frühe Quest |
| Donnerwacht | Schmied / frühe Boss-/Questbelohnung |
| Goldklinge | Schmied / Quest |
| Schattengeflecht | Schneider / Quest / Boss |
| Diamantklinge | Schmied / Midgame-Boss |
| Stahlwall | Schmied |
| Sonnengewand | Schneider / Midgame |
| Kristallwache | Endgame-Schmied |
| Netheritklinge | Endgame-Schmied |
| Höllenschmiede | Endgame-Schmied / Boss |

Das sind **Vorschläge**, noch keine Festlegung.

**Status: 🟠 CONTENT-/IMPLEMENTIERUNGS-LÜCKE**

---

# 5. Equipment-Sets

## Bestand

Es existieren **6 vollständige Sets**:

- Donnerwacht
- Schattengeflecht
- Stahlwall
- Sonnengewand
- Kristallwache
- Höllenschmiede

Alle besitzen:

- 2-Piece-Bonus
- 3-Piece-Bonus
- 4-Piece-Bonus
- teilweise Armor-Trim
- Set-ID
- Level-Anforderung

Die technische Set-Logik ist vorhanden.

## Wichtig

Das frühere Bild „nur ein Rüstungsset existiert“ ist für den aktuellen `test`-Stand nicht mehr korrekt.

Das Problem ist **nicht Vielfalt der Setdefinitionen**, sondern die fehlende bzw. nicht erkennbare normale Beschaffung.

**Status Technik: 🟢**  
**Status Content-Erwerb: 🟡**

---

# 6. Food

## Bestand

`food-definitions.json`: **23 Food-Definitionen**

Beispiele:

- Mehl
- Speck
- Karottensuppe
- Kartoffel mit Speck
- Hühnersuppe
- Pilzpfanne
- Apfelmus
- Kürbisbrot
- Beeren-Muffin
- Kakao
- Melonensaft
- Kürbis-Latte
- Honig mit Milch
- Apfelkuchen
- Spiegelei
- Käse

Technisch vorhanden:

- FoodDefinition
- FoodDefinitionRegistry
- FoodService
- Food Properties
- Consumable Data Components
- Trinken/Essen-Animationen
- Effekte

## 🟠 Content-Anbindung

Die 23 Food-Definitionen sind **nicht Bestandteil der 29 Itemdefinitionen**.

Die 300 Crafting-Rezepte erzeugen Vanilla-Materialien; die Crafting-Registry erlaubt aktuell keine Custom-`resultItemId`-Ausgaben.

Damit existiert zwar ein umfangreicher Food-Katalog, aber die Spielerquelle dieses Katalogs ist nicht vollständig sichtbar.

### Benötigt

Für jedes Food:

- Erzeugungsweg
- Rezept
- NPC/Profession
- ggf. Shop
- ggf. Quest-/Drop-Quelle

und anschließend die tatsächliche Verbindung mit dem Item-System.

**Status: 🟠 CONTENT-/IMPLEMENTIERUNGS-LÜCKE**

---

# 7. Crafting

## Bestand

**300 Rezepte**

Aufgeteilt auf:

| Beruf | Rezepte |
|---|---:|
| Schmied | 61 |
| Gelehrter | 68 |
| Landwirt | 25 |
| Koch | 23 |
| Schneider | 26 |
| Alchemist | 39 |
| Steinmetz | 58 |
| **Gesamt** | **300** |

Maximale Rezept-Level reichen je nach Beruf bis Level 55–100.

Vorhanden:

- Rezeptfreischaltung
- Beruf-Level
- Unlock-Preis
- Rezeptkategorien
- Kosten
- Tränke
- Verzauberungsbücher
- Crafting-GUI
- Crafting-Service
- Rezeptvalidierung

## 🟢 Technischer Content

Der Crafting-Bestand ist groß genug, um das System als gefüllt zu betrachten.

## 🟡 Inhaltliche Erweiterung

Es fehlt nicht „Crafting-Content“ allgemein.

Es fehlt vielmehr die Verbindung zwischen Crafting und den **PixelRPG-Custom-Items**.

Aktuell sind die Rezepte auf Vanilla-Ergebnisse ausgelegt.

### Feuerball-Beispiel

Der gewünschte Feuerball wäre deshalb eine **neue Custom-Item-/Crafting-Kette**:

- Feuerball als RPG-Item definieren
- Alchemist-Rezept
- benötigte Materialien
- Berufslevel
- ggf. Unlock-Preis/Quest
- ggf. Waffe/Ability
- ggf. Resourcepack

Das ist eine konkrete Content-Erweiterung, bei der die Crafting-Implementierung eventuell geringfügig erweitert werden muss.

**Status Crafting allgemein: 🟢**  
**Status Custom-Item-Crafting: 🟡**

---

# 8. Professionen

## 9 Berufe

Aktiv craftend:

- Schmied
- Gelehrter
- Landwirt
- Koch
- Schneider
- Alchemist
- Steinmetz

Passiv:

- Fischer
- Holzfäller

Alle neun besitzen:

- Definition
- Level 1–100
- Profession Service
- Trainer-NPC-Typ
- Dialog
- Fortschritt

Die passiven Berufe benötigen keine Crafting-Rezepte.

**Status: 🟢 FERTIG**

### Content-Ausbau

Zusätzliche Rezepte oder Berufsinhalte sind Erweiterungen, keine notwendige Fertigstellung.

---

# 9. Quests

## Bestand

Reguläre Questdateien:

- Crafting Orders: 60
- Additional: 84
- Content Expansion 01: 46
- Expansion 02: 19
- V2: 31
- World Expansion: 29

**Summe regulär: 269**

Zusätzlich:

- **17 aktive Story-Strukturquests**

Damit existieren im aktuellen Stand **286 aktive Questdefinitionen**, wenn Story separat mitgezählt wird.

Vorhandene Typen:

- HUNT
- COLLECT
- GLOBAL_EVENT
- TALK_TO_NPC
- REACH_LOCATION
- Prerequisites
- Follow-Ups
- Profession Requirements
- Level Requirements
- Rewards
- Companion Unlocks
- World/Expansion Content

## 🟢 Content-Menge

Es gibt keinen allgemeinen Quest-Content-Mangel.

## 🟡 Hauptlücke

Der Quest-Content muss an die Welt angebunden werden:

- NPC existiert
- NPC steht an sinnvoller Position
- Quest-NPC passt thematisch
- Belohnung passt zur Progression
- Questkette ist spielbar
- Zielgebiet ist erreichbar
- Navigation funktioniert

**Status Daten: 🟢**  
**Status Welt-Anbindung: 🟡**

---

# 10. Story / Kampagne

## Bestand

Aktuelle Story-Kampagne:

- 35 Kapitelpositionen
- 17 aktive Struktur-Questknoten
- Story-Version 6
- Story-NPC-System
- automatische Story-NPC-Erzeugung
- Dialoge
- XP-Rewards
- Story-Fortschritt
- alte Story-Quest-Migration
- Nebenwissen/Dialog-Kapitel für entfernte Strukturvarianten

Aktive Hauptstrukturen umfassen unter anderem:

- Dorf
- Schiffswrack
- Wüstenpyramide
- Dschungelpyramide
- Sumpfhütte
- warme Ozeanruine
- Monument
- Trail Ruins
- Mineshaft
- Pillager Outpost
- Woodland Mansion
- Ruined Portal
- Ancient City
- Nether Fortress
- Bastion
- Stronghold
- End City

## 🟢 Story-System + Kampagnenstruktur

Die Story ist nicht mehr nur ein Drei-Zeilen-Prolog.

Die Kampagnenstruktur ist vorhanden.

## 🟡 Welt-/Dialog-Füllung

Was weiterhin benötigt wird:

- Story-NPCs an sinnvoller Weltposition
- finale Dialogüberarbeitung
- Neben-NPCs für die lore-orientierten Kapitel
- Belohnungen passend zur Story-Progression
- Story-Quests mit normalem Spieler-Content verbinden

**Status: 🟡 CONTENT AUSBAU / WELTANBINDUNG**

---

# 11. Companions

## Bestand

**28 Companion-Definitionen**

Freischaltungen:

- 25 Quest
- 1 Default
- 1 Boss
- 1 Admin

Vorhanden:

- Rarities
- Level/XP
- Stats
- Follow
- Combat-System
- Passive Stats
- Equipment
- Mounts
- Rename
- Quest Unlocks
- Boss Unlock
- Dialog
- Runtime Registry

## 🟢 Content-Menge

28 Begleiter sind für einen initialen Content-Stand ausreichend.

## 🟡

Die 25 Quest-Unlocks müssen mit den tatsächlich vorhandenen Questketten/NPCs/Weltpositionen verbunden werden.

Das ist primär **World-/Progression-Integration**, nicht Companion-Code.

**Status: 🟢 Content vorhanden / 🟡 Integration**

---

# 12. Bosse

## Bestand

**26 Standard-Biome-Bosse**

Zusätzlich **6 World-Bosse**:

- Risskoloss
- Sturmherrscher
- Abgrundfürst
- Seelenverschlinger
- Endbote
- Uralter Weltenwächter

Gesamt:

**32 Bosse**

Zusätzlich:

**32 Boss-Reward-Items**

Standard-Bosse decken zahlreiche Overworld-, Nether- und End-Biome ab.

## 🟢 Content

Es besteht kein offensichtlicher Mangel an Boss-Anzahl.

## 🟡 Integration

Noch relevant:

- Boss-Spawn-Gebiete
- Spawnfrequenz
- Loot-Zuordnung
- Companion-Unlocks
- Quest-Zuordnung
- tatsächliche Welt-/Progressionsstufe

**Status: 🟢 Content / 🟡 Weltintegration**

---

# 13. NPCs

## Technische Basis

Vorhanden:

- NpcManager
- persistente IDs
- Chunk-Handling
- Spawn/Despawn
- Skins
- Nameplates
- Look-System
- Interaction
- Behavior Registry

NPC-Rollen:

- Reception
- Banker
- Quest
- Shop
- Travel
- Story
- Filler
- 9 Profession Trainer

## 🔴 Größte Content-Lücke

Der Repository-Stand enthält **keine mitgelieferte Weltpopulation**, die einen frischen Server mit einer vollständigen RPG-Welt versieht.

`NpcManager` lädt `npcs.yml` aus dem Plugin-Datenordner.

Das bedeutet:

> NPC-System fertig ≠ Welt besitzt bereits alle benötigten NPCs.

### Benötigt

Eine vollständige NPC-Population:

- Rathaus/Rezeption
- Bank
- Shop-NPCs
- alle 9 Berufstrainer
- Questgeber
- Reise-NPCs
- Story-NPCs
- Filler-NPCs
- ggf. Guild-/Companion-/Spezial-NPCs

Jeder NPC benötigt:

- ID
- Typ
- Name
- Position
- Welt
- Skin
- Rolle
- ggf. Quest-/Shop-Zuordnung
- Dialog

**Status: 🟡 CONTENT-LÜCKE**

---

# 14. Shops

## Technik

Vorhanden:

- ShopManager
- ShopEntry
- Buy-Preis
- Sell-Preis
- Shop GUI
- Shop Editor GUI
- NPC-Zuordnung
- Persistenz
- Migration alter Shopformate

## Content

Der ShopManager erwartet `shops.yml` im Datenordner.

Eine solche Default-Shoppopulation ist nicht Bestandteil des Repository-Contentbestands.

Damit ist das System technisch fertig, aber ein frischer Stand besitzt nicht automatisch ein vollständiges Sortiment.

### Benötigt

Shop-Kataloge, z.B.:

- allgemeiner Händler
- Schmied
- Alchemist
- Koch
- Farmer
- Reisender
- ggf. Endgame-/Spezialhändler

Für jedes Angebot:

- Item
- Kaufpreis
- Verkaufspreis
- NPC
- Progressionsstufe

**Status Technik: 🟢**  
**Status Shop-Content: 🟡**

---

# 15. Bank / persönliche Economy

## 🟢 Bank

Vorhanden:

- Banker-NPC
- Bank Dialog
- Bank GUI/Inventory
- Bank Storage
- persistente Bank
- Einzahlung/Auszahlung

Die Bank kann als **fertige Implementierung** behandelt werden.

## 🟡 Geplante Währungsänderung

Aktuell ist die Währung auf:

`economy.currency.max-stack-size: 64`

eingestellt.

Gewünschtes Verhalten:

- Geld als physischer Item-Drop
- Geld liegt tatsächlich im Inventar
- Stackgröße 999
- kein automatisches Einsammeln in die virtuelle Bank
- Bank bleibt Einzahlungsmöglichkeit

Das ist **eine konkrete Economy-/Drop-Änderung**, keine fehlende Bank-Implementierung.

**Status Bank: 🟢**  
**Status physische Währung: 🟡 GEPLANTE ÄNDERUNG**

---

# 16. Guild

## 🟢 Grundsystem

Vorhanden:

- Guild-Erstellung
- Mitglieder
- Einladungen
- Verlassen
- Guild Bank
- Guild Currency
- Guild Compass
- Guild API
- Commands
- Persistenz

Spieler erzeugen den Content dynamisch.

## 🟡 Guild-City / Weltcontent

Falls Guild Cities Bestandteil der finalen Spielwelt sein sollen, fehlen:

- konkrete Guild-City-Flächen
- Regeln
- Spawnpunkte
- NPCs
- Gebäude
- Progression
- visuelle Gestaltung

Dieser Teil ist bereits als World-/Region-Ausbau zurückgestellt.

**Status Kernsystem: 🟢**  
**Guild-World-Content: ⚪ PAUSIERT**

---

# 17. Party

## 🟢 FERTIG

Vorhanden:

- Party-Erstellung
- Mitglieder
- Disconnect Handling
- GUI
- Commands
- Quest-Sharing
- Share Range
- API

Kein offensichtlicher Content-Mangel.

Weitere Party-Features wären Erweiterungen.

---

# 18. Trade Depot

## 🟢 FERTIG

Vorhanden:

- Listings
- Verkauf
- Kauf
- Buy/Sell-Prozess
- Preise
- Inventarprüfung
- Ablauf
- Pending Payout
- GUI
- Persistenz

Content-Lücke:

Keine zwingende.

Das System lebt vom Spielerangebot.

**Status: 🟢 FERTIG**

---

# 19. Combat / Weapon Abilities

## 🟢 System

Vorhanden:

- Damage
- Combat State
- Mob Scaling
- XP
- Loot
- Weapon Ability Engine
- Skill Input
- Cooldowns
- konkrete Abilities

Vorhandene Item-Abilities umfassen u.a.:

- IRON_BASTION
- GOLDEN_FLURRY
- DIAMOND_CLEAVE
- NETHERITE_ERUPTION
- ADMIN_RELIC

## 🟡 Content-Ausbau

Der Combat-Kern ist ausreichend gefüllt.

Die größte Lücke liegt bei der Anzahl sinnvoll erreichbarer **Custom-Waffen**.

Die technischen Fähigkeiten existieren, aber nur wenige Spielerwaffen nutzen sie.

**Status System: 🟢**  
**Status Waffen-Progression: 🟡**

---

# 20. Economy allgemein

Vorhanden:

- Money
- Bank
- Guild Currency
- Shop Buy/Sell
- Trade Depot
- Loot Currency
- Quest Rewards
- Guild Economy

## 🟡 Economy-Content

Es gibt bereits viele Quellen und Senken.

Noch zu definieren/balancen:

- tatsächliche Geldquellen
- Quest-Auszahlungen
- Boss-Geld
- Shoppreise
- Reparatur-/Crafting-Kosten, falls gewünscht
- Geld-Progression
- Endgame-Geldsenken

Das ist kein fehlendes Economy-System, sondern **Balance-/Design-Content**.

**Status: 🟡 BALANCING / DESIGN**

---

# 21. Regionen / Welt

## ⚪ Bewusst pausiert

Region-Engine vorhanden:

- Wilderness
- Village
- City
- Guild City
- Ruins
- Fortress
- Dungeon
- Danger Zone
- Boss Zone
- Other

Technik:

- RegionManager
- RegionRepository
- Editor
- Geometry
- Flags
- Spawn
- Transitions
- Policies

Aber konkrete Weltregionen sind nicht vollständig angelegt.

Das ist aktuell **bewusst pausiert** und daher keine versehentliche Content-Lücke.

**Status: ⚪ PAUSIERT**

---

# 22. Resourcepack

## ⚪ Bewusst pausiert

Vorhanden:

- Resourcepack-Struktur
- Food-Modelle
- Item-Modelle
- Texturstruktur
- pack.mcmeta

Fehlt:

- vollständige Custom-Item-Abdeckung
- finale Texturen
- visuelle Konsistenz

Da der Ausbau bewusst pausiert ist:

**Status: ⚪ PAUSIERT**

---

# 23. Statistics / Scoreboard / Playtime

## 🟢 FERTIG

Vorhanden:

- Mob Kill Statistics
- Death Statistics
- Boss Statistics
- Quest/Boss Statistics
- Scoreboard
- Playtime

Kein zwingender Content-Mangel.

---

# 24. API / externe Plugin-Nutzung

## 🟢 FERTIG

Vorhanden:

- ItemAPI
- EconomyAPI
- GuildAPI
- PartyAPI
- StatisticsAPI
- Boss Events
- Combat Events
- Level-Up Events
- Player Lifecycle Events
- Quest Completed Event

Kein Content-Blocker.

---

# 25. Konkrete Content-Lücken – Prioritäten

## 🔴 P0 – Spieler kann vorhandenen Content sonst nicht sinnvoll erleben

### 1. NPC-Weltpopulation

Benötigt:

- Basis-NPCs
- Trainer
- Banker
- Shops
- Questgeber
- Reise-NPCs
- Story-NPCs
- Filler

### 2. Shop-Sortimente

Technik ist vorhanden, aber Default-Sortimente fehlen.

### 3. Erwerbswege für Custom-Items

Die 28 normalen Custom-Items müssen mindestens einen normalen Spieler-Erwerbsweg erhalten.

---

# 26. 🟠 P1 – Content ist vorhanden, aber noch nicht sinnvoll angebunden

### Custom-Rüstungssets

Nicht mehr sechs Sets „bauen“.

Es existieren bereits sechs.

Stattdessen:

- Erwerbsquellen
- Progression
- ggf. Set-Quests
- Boss-Drops
- Shop-/Crafting-Anbindung

### Custom-Waffen

Vorhandene vier normalen Progressionswaffen müssen in die Welt gebracht werden.

Danach können weitere Waffen wie der gewünschte Feuerball hinzukommen.

### Food

Die 23 Food-Definitionen benötigen eine sichtbare Spielerquelle.

### Story

Story-Struktur ist vorhanden.

Jetzt:

- NPCs
- Weltpositionen
- Dialog-Polishing
- Belohnungen
- Questintegration

### Companion Unlocks

Die 25 Quest-Unlocks müssen mit den tatsächlich erreichbaren Questketten verbunden sein.

---

# 27. 🟡 P2 – Sinnvoller Content-Ausbau, aber kein Blocker

- zusätzliche Custom-Waffen
- zusätzliche Food-Rezepte
- weitere Boss-Loot-Varianten
- zusätzliche Shop-Angebote
- mehr Filler-NPCs
- zusätzliche Nebenquests
- zusätzliche Companion-Varianten
- weitere Story-Nebenberichte
- zusätzliche Economy-Sinks

Diese Punkte sind Erweiterungen und sollten erst nach den P0/P1-Lücken kommen.

---

# 28. Was ich ausdrücklich NICHT als Content-Lücke bewerte

Folgende Systeme müssen nicht künstlich weiter aufgebläht werden:

### Bank
🟢 Fertig.

Die 999er physische Währung ist eine gezielte Designänderung.

### Party
🟢 Fertig.

### Trade Depot
🟢 Fertig.

### Profession-System
🟢 Fertig.

### Crafting
🟢 Fertig als Vanilla-Crafting-System.

Nur Custom-Item-Crafting ist noch offen.

### Boss-System
🟢 Fertig und bereits umfangreich gefüllt.

### Companion-System
🟢 Fertig und umfangreich gefüllt.

### Equipment-System
🟢 Technisch fertig.

Es existieren bereits sechs Sets.

### Quest-System
🟢 Umfangreich gefüllt.

Die offene Arbeit ist hauptsächlich Welt-/NPC-Anbindung und Playthrough.

---

# 29. Empfohlene Content-Reihenfolge

Nicht einfach weitere Items oder Quests hinzufügen.

Die sinnvollere Reihenfolge ist:

1. **NPC-Weltpopulation**
2. **Basis-Shop-Sortimente**
3. **Erwerbswege für die bestehenden 28 normalen Custom-Items**
4. **Food-Quellen und Food-Rezepte**
5. **Custom-Item-Crafting erweitern**
6. **Feuerball als Alchemist-Content**
7. **Story-NPCs und Story-Weltanbindung**
8. **Quest-NPC-Zuordnung**
9. **Companion-Unlocks**
10. **Boss-/Loot-Zuordnung**
11. **Economy-Feintuning**
12. **erst danach zusätzliche neue Content-Pakete**

---

# 30. Definition of Content Complete

Ein System darf künftig als **Content-fertig** markiert werden, wenn:

- [ ] technische Implementierung existiert
- [ ] mindestens ein vollständiger Spieler-Gameplay-Loop existiert
- [ ] der Spieler den Content ohne Admin-Befehle erreichen kann
- [ ] benötigte NPCs vorhanden sind
- [ ] benötigte Rezepte/Quellen vorhanden sind
- [ ] Belohnungen/Preise existieren
- [ ] erforderliche Quest-/Progressionsverbindungen existieren
- [ ] der Content nicht nur als JSON/Java-Definition herumliegt
- [ ] keine offensichtliche „tote“ Contentdefinition übrig bleibt

**Wichtig:** Mehr Content ist nicht automatisch besser. Wenn ein System bereits einen vollständigen Loop besitzt, wird es als fertig behandelt.

---

# 31. Aktuelle Gesamtmarkierung

| Bereich | Technik | Content | Bewertung |
|---|---|---|---|
| Player/Progression | vorhanden | vorhanden | 🟢 |
| Bank | vorhanden | ausreichend | 🟢 |
| Physische Währung | vorhanden | Designänderung offen | 🟡 |
| Items | vorhanden | Erwerbswege fehlen | 🟠 |
| Equipment | vorhanden | 6 Sets vorhanden, Quellen fehlen | 🟡 |
| Food | vorhanden | 23 Definitionen, Quellen/Anbindung offen | 🟠 |
| Crafting | vorhanden | 300 Rezepte | 🟢 |
| Custom-Item-Crafting | teilweise | fehlt | 🟡 |
| Professionen | vorhanden | 9 Berufe | 🟢 |
| Quests | vorhanden | 269 + 17 Story | 🟢 |
| Quest-Weltanbindung | vorhanden | NPC/Welt fehlt teilweise | 🟡 |
| Story | vorhanden | Kampagnenstruktur vorhanden | 🟡 |
| Companions | vorhanden | 28 | 🟢 |
| Bosses | vorhanden | 32 | 🟢 |
| NPC-System | vorhanden | Weltpopulation fehlt | 🟡 |
| Dialog-System | vorhanden | NPC-/Dialogpopulation offen | 🟡 |
| Shops | vorhanden | Default-Sortimente fehlen | 🟡 |
| Party | vorhanden | vorhanden | 🟢 |
| Guild | vorhanden | Grundcontent vorhanden | 🟢 |
| Guild City | vorhanden | Weltcontent pausiert | ⚪ |
| Trade Depot | vorhanden | spielergetrieben | 🟢 |
| Combat | vorhanden | ausreichend | 🟢 |
| Weapon Progression | vorhanden | zu wenige erreichbare Custom-Waffen | 🟡 |
| Economy | vorhanden | Balancing offen | 🟡 |
| Regionen | vorhanden | bewusst pausiert | ⚪ |
| Resourcepack | vorhanden | bewusst pausiert | ⚪ |
| Statistics | vorhanden | vorhanden | 🟢 |
| Scoreboard | vorhanden | vorhanden | 🟢 |
| API | vorhanden | vorhanden | 🟢 |

---

# 32. Schlussfolgerung

Der aktuelle PixelRPG-Stand braucht **nicht allgemein mehr Systeme**.

Die größte offene Aufgabe ist die **Verknüpfung des bereits vorhandenen Contents mit einer spielbaren Welt**.

Besonders wichtig:

> **Nicht noch sechs Rüstungssets programmieren. Es existieren bereits sechs.**

> **Nicht noch 100 Quests programmieren. Es existieren bereits sehr viele.**

> **Nicht noch mehr Boss-Code schreiben. 32 Bosse und 32 Reward-Items existieren bereits.**

Stattdessen:

**NPCs platzieren → Shops füllen → vorhandene Items erreichbar machen → Food anbinden → Custom-Crafting erweitern → Story/Quests mit der Welt verbinden → danach erst neuen Content produzieren.**

Damit dient diese Datei künftig als Content-Gegenstück zu `Forensik.md`:

- `Forensik.md` = „Ist die technische Implementierung sauber?“
- `Content.md` = „Ist die Implementierung mit ausreichend spielbarem Content gefüllt?“



---

# 33. Umsetzungsstand – Content-Pipeline 24.09.2026

Dieser Abschnitt **überschreibt ältere offene Content-Befunde**, soweit sie durch die aktuelle Umsetzung oder durch eine bewusste Produktentscheidung geändert wurden.

## Bewusst pausiert / nicht mehr als Lücke behandeln

- **Food:** Die 23 Food-Definitionen bleiben bewusst als Testbestand pausiert. Es werden aktuell keine Food-Quellen, Food-Rezepte oder Food-Progressionsketten ergänzt.
- **NPC-Weltpopulation:** Eine globale Default-NPC-Population wird nicht benötigt. NPCs/Mannequins werden gezielt dort erzeugt, wo ein Quest-/Story-Loop sie benötigt; insbesondere die bestehende Story-Struktur-NPC-Erzeugung bleibt dafür maßgeblich.
- **Shops:** Das Shop-System gilt als content-fertig. Sortimente werden bewusst manuell über die vorhandene Shop-Administration gepflegt.
- **Regionen, Guild-City und Resourcepack-Ausbau:** weiterhin bewusst pausiert.

## Umgesetzt

### Custom-Item-Erwerb / Weapon Progression

Die bestehende Custom-Item-Progression ist jetzt als echter Spieler-Loop angebunden:

- Eisenschwert – Level 3
- Goldklinge – Level 25
- Diamantklinge – Level 40
- Netheritklinge – Level 90
- Feuerball – Level 35, Alchemist

Die vier vorhandenen Progressionswaffen besitzen weiterhin ihre bestehenden Weapon Abilities. Der Feuerball nutzt die vorhandene Fireball-Ability und wird als normales PixelRPG-Item über das Alchemist-Rezept hergestellt.

**Weapon Progression bedeutet damit nicht „mehr Waffen programmieren“, sondern:**

1. Spieler erreicht die erforderliche Stufe.
2. Spieler levelt den passenden Beruf.
3. Rezept wird über den bestehenden Unlock-Mechanismus freigeschaltet.
4. Vanilla-Materialien und ggf. Boss-Katalysatoren werden beschafft.
5. Custom-Waffe wird über den normalen Crafting-Loop erzeugt.
6. Die Waffe bringt ihre definierte Ability in den Combat-Loop ein.

### Custom-Rüstungssets

Alle sechs vorhandenen Sets besitzen jetzt konkrete Crafting-Erwerbswege:

- Donnerwacht – Schmied, Level 10
- Schattengeflecht – Schneider, Level 20
- Stahlwall – Schmied, Level 30
- Sonnengewand – Schneider, Level 45
- Kristallwache – Schmied, Level 60
- Höllenschmiede – Schmied, Level 80

Höhere Sets verwenden zusätzlich vorhandene Boss-Reward-Items als Crafting-Katalysatoren. Damit entsteht eine echte Verbindung zwischen Boss- und Equipment-Progression.

### Custom-Item-Crafting

Die Crafting-Pipeline unterstützt jetzt zusätzlich:

- `resultItemId`
- Custom-PixelRPG-Itemkosten
- Validierung gegen die zentrale ItemDefinitionRegistry
- Verbrauch von Custom-Item-Kosten
- Erzeugung über den zentralen ItemService
- weiterhin vollständige Vanilla-Rezepte

Der bestehende Vanilla-Crafting-Bestand bleibt erhalten.

### Feuerball

Der Feuerball wurde als neues Custom-Item integriert:

- Material: FIRE_CHARGE
- Kategorie: RANGED_WEAPON
- Level: 35
- Beruf: Alchemist
- Recipe-Kosten: Blaze Powder, Gunpowder, Ghast Tear
- zusätzlicher Boss-Katalysator: Sumpftrank
- bestehende Fireball-Ability
- 3 Sekunden Cooldown

Damit ist der Feuerball kein losgelöstes Beispiel mehr, sondern ein echter Content-Loop.

### Story-Integration

Die 17 aktiven Story-Strukturquests geben jetzt konkrete Custom-Item-Belohnungen aus der bestehenden Progressionskette.

Die bestehende automatische Story-NPC-Erzeugung an den relevanten Strukturen bleibt erhalten. Eine globale NPC-Population wurde ausdrücklich nicht eingeführt.

### Companion-Unlocks

Die 25 questbasierten Companion-Freischaltungen sind jetzt mit Quest-Content verbunden:

- 20 bestehende Companion-Quests wurden direkt mit `reward.companionId` verdrahtet.
- 5 fehlende Unlock-Loops wurden als dedizierte Quests ergänzt:
  - Copper Golem
  - Pig
  - Horse
  - Zombie Horse
  - Skeleton Horse

Damit sind die 25 QUEST-Unlocks nicht mehr nur Definitionen, sondern besitzen konkrete Questpfade.

### Boss-/Loot-Integration

Progressionsausrüstung wurde zusätzlich in die Boss-Loot-Pipeline eingebunden.

Bestehende Bossdefinitionen werden bei der Initialisierung migriert, sodass ausgewählte Bosse konkrete Progressionswaffen/-rüstung als garantierte Loot-Komponenten erhalten. Bestehende Lootdefinitionen werden dabei nicht ersetzt.

### Physische Währung

Die Währung wird jetzt als physisches Inventar-Item behandelt:

- Currency bleibt ein physischer Drop.
- Pickup transferiert **nicht automatisch** in die virtuelle Bank.
- Einzahlung erfolgt weiterhin bewusst über die Bank.
- Auszahlung erzeugt wieder physische Currency-Stacks.
- Paper 26.2 unterstützt für den Item-`MAX_STACK_SIZE`-Data-Component Werte bis **99**. Daher wurde die gewünschte 999er Stackgröße auf den technisch/API-seitig unterstützten Wert 99 begrenzt, statt eine nicht unterstützte Legacy-/NMS-Lösung einzubauen.

### Economy-Baseline

Die vorhandenen Economy-Senken wurden in die neue Progression eingebunden:

- Rezept-Unlock-Preise
- Story-Geld
- Boss-Geld
- physische Currency-Drops
- Bank-Einzahlung/-Auszahlung
- Trade Depot

Weitere Economy-Anpassungen bleiben Balancing nach Playtests und sind kein fehlendes Kernsystem.

## Aktueller Content-Fokus

Die verbleibenden offenen Arbeiten sind jetzt überwiegend:

1. tatsächliche Welt-/Strukturplatzierung der benötigten NPCs/Mannequins,
2. manuelle Shop-Befüllung,
3. Playtest und Balancing der neuen Progressionskosten,
4. spätere Food-/Resourcepack-/Regions-Erweiterungen.

Die großen bisherigen „Definition liegt herum, ist aber nicht erreichbar“-Lücken bei Custom-Waffen, Rüstungssets und Companions sind damit geschlossen.
