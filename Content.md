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

`item-definitions.json`: **30 Definitionen** – davon **29 normal spielerisch erreichbare Items** und **1 Admin-/Unique-Relikt**

Kategorien:

- 6 Waffen (inkl. Feuerball und Admin-Relikt)
- 6 Helme
- 6 Brustplatten
- 6 Beinschützer
- 6 Stiefel

Davon:

- 29 normale Spieler-Items
- 1 Unique/Admin-Relikt

Normale Waffen:

- Eisenschwert
- Goldklinge
- Diamantklinge
- Netheritklinge
- Feuerball

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

# 30.1 Konkrete Progressionsmatrix – Erwerbswege der 29 Spieler-Items

> **Abgrenzung:** Die Matrix umfasst die **29 normal spielerisch erreichbaren Custom-Items** aus `item-definitions.json`: 4 Waffen + 24 Rüstungsteile + Feuerball. Das **Admin-Relikt** ist als `adminOnly: true` bewusst nicht Bestandteil der Spielerprogression.

| Level | Item | Quelle | NPC | Quest / Boss / Profession | Kosten | Dropchance |
|---:|---|---|---|---|---|---|
| 3 | Eisenschwert | Crafting + Story + Boss | Schmied-Trainer / Story-NPC | Schmied L3; Quest `story_campaign_village_plains` (L3); Boss **Der Plünderer** (L12) | 2× Eisenbarren + 1× Stock; Unlock 0 | Quest 100%; Boss 100% garantiert; Crafting kein Drop |
| 10 | Donnerwacht-Helm | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L10; Quest `story_campaign_shipwreck` (L5) | 8× Kupferbarren; Unlock 250 | Quest 100%; Crafting kein Drop |
| 10 | Donnerwacht-Brustplatte | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L10; Quest `story_campaign_desert_pyramid` (L9) | 8× Kupferbarren; Unlock 250 | Quest 100%; Crafting kein Drop |
| 10 | Donnerwacht-Beinschutz | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L10; Quest `story_campaign_jungle_pyramid` (L13) | 8× Kupferbarren; Unlock 250 | Quest 100%; Crafting kein Drop |
| 10 | Donnerwacht-Stiefel | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L10; Quest `story_campaign_swamp_hut` (L17) | 6× Kupferbarren; Unlock 250 | Quest 100%; Crafting kein Drop |
| 20 | Schattengeflecht-Helm | Crafting + Story + Boss | Schneider-Trainer / Story-NPC | Schneider L20; Quest `story_campaign_ocean_ruin_warm` (L21); Boss **Die Hexe des Waldes** (L22) | 6× Eisenbarren + 2× Faden; Unlock 900 | Quest 100%; Boss 100% garantiert; Crafting kein Drop |
| 20 | Schattengeflecht-Panzer | Crafting + Story | Schneider-Trainer / Story-NPC | Schneider L20; Quest `story_campaign_monument` (L26) | 8× Eisenbarren + 2× Faden; Unlock 900 | Quest 100%; Crafting kein Drop |
| 20 | Schattengeflecht-Beinschutz | Crafting + Story | Schneider-Trainer / Story-NPC | Schneider L20; Quest `story_trail_ruins_archaeology` (L31) | 7× Eisenbarren + 2× Faden; Unlock 900 | Quest 100%; Crafting kein Drop |
| 20 | Schattengeflecht-Stiefel | Crafting + Story | Schneider-Trainer / Story-NPC | Schneider L20; Quest `story_campaign_mineshaft` (L36) | 5× Eisenbarren + 2× Faden; Unlock 900 | Quest 100%; Crafting kein Drop |
| 22 | Goldklinge | Crafting + Story + Boss | Schmied-Trainer / Story-NPC | Schmied L25; Quest `story_campaign_pillager_outpost` (L42); Boss **Der Sandstein-Koloss** (L40) | 4× Goldbarren + 1× Stock; Unlock 1600 | Quest 100%; Boss 100% garantiert; Crafting kein Drop |
| 30 | Stahlwall-Helm | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L30; Quest `story_campaign_mansion` (L48) | 8× Eisenbarren; Unlock 2200 | Quest 100%; Crafting kein Drop |
| 30 | Stahlwall-Brustplatte | Crafting + Story + Boss | Schmied-Trainer / Story-NPC | Schmied L30; Quest `story_campaign_ruined_portal` (L54); Boss **Der Ravager-Häuptling** (L38) | 8× Eisenbarren; Unlock 2200 | Quest 100%; Boss 100% garantiert; Crafting kein Drop |
| 30 | Stahlwall-Beinschutz | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L30; Quest `story_under_the_stone` (L61) | 8× Eisenbarren; Unlock 2200 | Quest 100%; Crafting kein Drop |
| 30 | Stahlwall-Stiefel | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L30; Quest `story_campaign_fortress` (L68) | 6× Eisenbarren; Unlock 2200 | Quest 100%; Crafting kein Drop |
| 35 | Feuerball | Crafting | Alchemist-Trainer | Alchemist L35; Boss-Katalysator **Die Sumpfhexe** (L32) | 4× Lohenstaub + 2× Schwarzpulver + 1× Ghast-Träne + 1× Sumpftrank; Unlock 3500 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 40 | Diamantklinge | Crafting + Story | Schmied-Trainer / Story-NPC | Schmied L40; Quest `story_campaign_stronghold` (L82) | 3× Diamant + 1× Stock; Unlock 4500 | Quest 100%; Crafting kein Drop |
| 45 | Sonnengewand-Helm | Crafting | Schneider-Trainer | Schneider L45; Boss-Katalysator **Der Sandstein-Koloss** (L40) | 7× Goldbarren + 2× Faden + 1× Goldenes Fossil; Unlock 5000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 45 | Sonnengewand-Brustplatte | Crafting + Story + Boss | Schneider-Trainer / Story-NPC | Schneider L45; Quest `story_campaign_bastion_remnant` (L75); Boss **Der Tiefenwächter** (L58) | 8× Goldbarren + 2× Faden + 1× Goldenes Fossil; Unlock 5000 | Quest 100%; Boss 100% garantiert; Crafting kein Drop |
| 45 | Sonnengewand-Beinschutz | Crafting | Schneider-Trainer | Schneider L45; Boss-Katalysator **Der Sandstein-Koloss** (L40) | 8× Goldbarren + 2× Faden + 1× Goldenes Fossil; Unlock 5000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 45 | Sonnengewand-Stiefel | Crafting | Schneider-Trainer | Schneider L45; Boss-Katalysator **Der Sandstein-Koloss** (L40) | 6× Goldbarren + 2× Faden + 1× Goldenes Fossil; Unlock 5000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 60 | Kristallwache-Helm | Crafting | Schmied-Trainer | Schmied L60; Boss-Katalysator **Der Uralte Wächter** (L65) | 7× Diamant + 1× Echoherz; Unlock 9000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 60 | Kristallwache-Brustplatte | Crafting + Boss | Schmied-Trainer | Schmied L60; Boss **Der Uralte Wächter** (L65) | 8× Diamant + 1× Echoherz; Unlock 9000 | Boss 100% garantiert; Crafting kein Drop |
| 60 | Kristallwache-Beinschutz | Crafting | Schmied-Trainer | Schmied L60; Boss-Katalysator **Der Uralte Wächter** (L65) | 8× Diamant + 1× Echoherz; Unlock 9000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 60 | Kristallwache-Stiefel | Crafting | Schmied-Trainer | Schmied L60; Boss-Katalysator **Der Uralte Wächter** (L65) | 6× Diamant + 1× Echoherz; Unlock 9000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 80 | Höllenschmiede-Helm | Crafting | Schmied-Trainer | Schmied L80; Boss-Katalysator **Der Netherfürst** (L68) | 7× Netheritbarren + 1× Netherkern; Unlock 18000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 80 | Höllenschmiede-Brustplatte | Crafting | Schmied-Trainer | Schmied L80; Boss-Katalysator **Der Netherfürst** (L68) | 8× Netheritbarren + 1× Netherkern; Unlock 18000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 80 | Höllenschmiede-Beinschutz | Crafting | Schmied-Trainer | Schmied L80; Boss-Katalysator **Der Netherfürst** (L68) | 8× Netheritbarren + 1× Netherkern; Unlock 18000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 80 | Höllenschmiede-Stiefel | Crafting | Schmied-Trainer | Schmied L80; Boss-Katalysator **Der Netherfürst** (L68) | 6× Netheritbarren + 1× Netherkern; Unlock 18000 | Katalysator-Boss 100% garantiert; Crafting kein Drop |
| 82 | Netheritklinge | Crafting + Story + Boss | Schmied-Trainer / Story-NPC | Schmied L90; Quest `story_campaign_end_city` (L89); Boss **Der Endkönig** (L78) direkt; zusätzlich Boss-Katalysator **Der Endbote** (L96) | 4× Netheritbarren + 1× Stock + 1× Endriss; Unlock 25000 | Endkönig 100% garantiert; Endbote-Katalysator 100% garantiert; Quest 100%; Crafting kein Drop |

### Leseregeln der Matrix

- **Level** = `requiredLevel` des Items, nicht zwingend das Level der Quest oder des Bosses.
- **Unlock** = Preis für die Rezeptfreischaltung; er fällt zusätzlich zu den Materialkosten an.
- **Crafting kein Drop** = das Rezept erzeugt das Item deterministisch, sobald Level, Berufslevel, Freischaltung und Kosten erfüllt sind.
- **100% garantiert** gilt für die in `BossRepository.ensureCustomProgressionLoot(...)` hinterlegten garantierten Progressionsdrops sowie für die expliziten Story-Questbelohnungen.
- **Admin-Relikt** ist absichtlich ausgeschlossen: `adminOnly: true`, daher kein normaler Spieler-Erwerbsweg.
- Die **NPCs** für Crafting sind die vorhandenen Profession-Trainer-Typen; die Story-NPCs werden weiterhin dynamisch an den relevanten Story-Strukturen erzeugt.

# 31. Aktuelle Gesamtmarkierung

| Bereich | Technik | Content | Bewertung |
|---|---|---|---|
| Player/Progression | vorhanden | vorhanden | 🟢 |
| Bank | vorhanden | ausreichend | 🟢 |
| Physische Währung | vorhanden | 99er Stack technisch umgesetzt | 🟢 |
| Items | vorhanden | **29 Spieler-Items mit konkretem Erwerbsweg** | 🟢 |
| Equipment | vorhanden | **6 Sets mit konkreten Crafting-/Katalysatorwegen** | 🟢 |
| Food | vorhanden | Testbestand bewusst pausiert | ⚪ |
| Crafting | vorhanden | 300 Rezepte | 🟢 |
| Custom-Item-Crafting | vorhanden | resultItemId + itemCosts + ItemService-Anbindung | 🟢 |
| Professionen | vorhanden | 9 Berufe | 🟢 |
| Quests | vorhanden | 269 + 17 Story | 🟢 |
| Quest-Weltanbindung | vorhanden | Story-Strukturpfade vorhanden, weitere Weltanbindung offen | 🟡 |
| Story | vorhanden | Kampagnenstruktur + Item-Rewards vorhanden | 🟡 |
| Companions | vorhanden | 28, 25 Quest-Unlocks angebunden | 🟢 |
| Bosses | vorhanden | 32 | 🟢 |
| NPC-System | vorhanden | Story-/Profession-Loop vorhanden, globale Population bewusst nicht vorgesehen | 🟢 |
| Dialog-System | vorhanden | Story-/NPC-Dialogpopulation vorhanden | 🟢 |
| Shops | vorhanden | manuelle Sortimente bewusst gepflegt | 🟢 |
| Party | vorhanden | vorhanden | 🟢 |
| Guild | vorhanden | Grundcontent vorhanden | 🟢 |
| Guild City | vorhanden | Weltcontent pausiert | ⚪ |
| Trade Depot | vorhanden | spielergetrieben | 🟢 |
| Combat | vorhanden | ausreichend | 🟢 |
| Weapon Progression | vorhanden | **5 normale Custom-Waffen mit Erwerbswegen** | 🟢 |
| Economy | vorhanden | Balancing offen | 🟡 |
| Regionen | vorhanden | bewusst pausiert | ⚪ |
| Resourcepack | vorhanden | bewusst pausiert | ⚪ |
| Statistics | vorhanden | vorhanden | 🟢 |
| Scoreboard | vorhanden | vorhanden | 🟢 |
| API | vorhanden | vorhanden | 🟢 |

### Ergebnis

Die frühere P0/P1-Lücke **„Custom-Items existieren, sind aber nicht erwerbbar“** ist für die 29 normalen Spieler-Items geschlossen.

Die Erwerbspipeline ist jetzt nachvollziehbar:

**Spielerlevel → Rezeptfreischaltung → Profession → NPC/Profession-Loop → Vanilla-Materialien → ggf. Boss-Katalysator → Crafting → Custom-Item**

Zusätzlich existieren bei einem Teil der Progressionsitems direkte **Story-Questbelohnungen** und bei ausgewählten Items **garantierte Boss-Rewards**.

Die verbleibenden gelben/weißen Bereiche sind bewusstes Produkt-/Balancing-/World-Design und werden nicht als fehlende Erwerbswege der 29 Spieler-Items gewertet.

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


---

# 34. Finaler Umsetzungsstand – Content-Pipeline

**Stand:** 24.09.2026  
**Branch:** `test`

Dieser Abschnitt ist der **maßgebliche aktuelle Status**. Frühere Auditabschnitte dokumentieren den damaligen Befund und werden nicht rückwirkend gelöscht.

## Bewusst ausgeschlossen

| Bereich | Status | Entscheidung |
|---|---|---|
| Food – 23 Definitionen | ⚪ pausiert | bleibt Testbestand |
| globale NPC-Weltpopulation | ⚪ entfernt | keine globale Filler-/Default-Population; Story-NPCs entstehen quest-/strukturbezogen |
| Shops | 🟢 fertig | Sortimente werden manuell über die vorhandene Shop-Administration gepflegt |
| Guild City / konkrete Regionen | ⚪ pausiert | kein weiterer Welt-Ausbau |
| Resourcepack-Ausbau | ⚪ pausiert | keine zusätzliche visuelle Content-Pipeline |

## Abgeschlossen

### Custom-Items

Alle **29 normalen Spieler-Items** besitzen mindestens einen erreichbaren Erwerbsweg.

Die zentrale Prüfung im aktuellen Repository ergibt:

- 29 normale Custom-Items
- 29 Custom-Crafting-Rezepte mit `resultItemId`
- kein normales Custom-Item ohne Rezept-/Quest-Erwerbsweg
- Admin-/Unique-Relikt bleibt absichtlich ausgeschlossen

### Weapon Progression

Die Waffenprogression ist vollständig angebunden:

- Eisenschwert
- Goldklinge
- Diamantklinge
- Netheritklinge
- Feuerball

Jede Waffe besitzt einen Level-/Profession-/Kostenpfad. Die vorhandenen Weapon Abilities bleiben unverändert.

### Rüstungsprogression

Alle sechs Sets sind als Spielerprogression angebunden:

- Donnerwacht – L10
- Schattengeflecht – L20
- Stahlwall – L30
- Sonnengewand – L45
- Kristallwache – L60
- Höllenschmiede – L80

Jedes Set besitzt konkrete Crafting-Kosten; höhere Stufen verwenden zusätzlich vorhandene Boss-Katalysatoren.

### Custom-Crafting

Der Crafting-Loop unterstützt jetzt:

- Vanilla-`result`
- Custom-`resultItemId`
- Vanilla-`costs`
- Custom-`itemCosts`
- zentrale ItemDefinition-Validierung
- ItemService-Erzeugung
- Rezept-Unlock über Berufslevel und Unlockpreis
- Dependency-/Zyklusprüfung

Damit ist Custom-Item-Crafting kein isolierter Definitionseintrag mehr.

### Feuerball

Der Feuerball ist als Alchemist-Progressionsitem integriert:

- Level 35
- Alchemist
- Blaze Powder
- Gunpowder
- Ghast Tear
- vorhandener Boss-Katalysator
- bestehende Fireball-Ability

### Companion-Content

Alle **25 QUEST-Unlocks** besitzen inzwischen eine konkrete Questzuordnung.

Zusätzlich bestehen:

- 1 DEFAULT
- 1 BOSS
- 1 ADMIN

Damit sind alle 28 Companion-Definitionen in einem gültigen Unlock-Modell.

### Boss-Content

Das Boss-System erzeugt bei fehlender Laufzeitdatei weiterhin automatisch:

- 26 Biome-Bosse
- 6 World-Bosse
- insgesamt 32 Bosse

Die Progressions-Loot-Migration bindet ausgewählte Custom-Waffen/-Rüstung additiv an vorhandene Bosse an.

### Story

Die Story-Struktur ist über die bestehende `StoryManager`-/`StoryLocationRegistry`-/`StoryTriggerListener`-Pipeline angebunden.

- relevante Strukturen werden nur bei aktivem Story-/Questpfad geprüft
- Story-NPCs entstehen dynamisch an der gefundenen Struktur
- Story-NPCs sind `NpcType.STORY`
- Sichtbarkeit ist quest- und profilabhängig
- Fortschritt läuft über `PlayerProfile.storyChapterIndex`
- Dialoge nutzen das native Paper-Dialog-System
- bestehende Story-Questketten besitzen konkrete Progressionsbelohnungen

Die globale NPC-Population wird dafür ausdrücklich **nicht** benötigt.

### Physische Währung

Der aktuelle Stand behandelt Currency physisch:

- Pickup bleibt ein physischer Inventargegenstand
- keine automatische Bankeinzahlung beim Pickup
- Bank bleibt der explizite Ein-/Auszahlungspfad
- Stackgröße ist auf den aktuellen Paper-26.2-kompatiblen Wert **99** begrenzt

Eine 999er Stackgröße wird nicht über Legacy-NMS erzwungen.

## Systeme, die jetzt als fertig gelten

| System | Status |
|---|---|
| Player / Level / XP | 🟢 fertig |
| Bank | 🟢 fertig |
| Physische Currency | 🟢 fertig |
| Custom Items | 🟢 fertig |
| Weapon Progression | 🟢 fertig |
| 6 Rüstungssets | 🟢 fertig |
| Custom-Crafting | 🟢 fertig |
| 9 Professionen | 🟢 fertig |
| Quest-System | 🟢 fertig |
| Story-System | 🟢 fertig |
| Story-NPC-Spawning | 🟢 fertig |
| Story-NPC-Sichtbarkeit | 🟢 fertig |
| 28 Companions | 🟢 fertig |
| 32 Bosse | 🟢 fertig |
| Combat / Weapon Abilities | 🟢 fertig |
| Shops / Shop-System | 🟢 fertig |
| Party | 🟢 fertig |
| Guild-Kernsystem | 🟢 fertig |
| Trade Depot | 🟢 fertig |
| Statistics / Scoreboard / Playtime | 🟢 fertig |

## Kein weiterer Content-Blocker aus diesem Audit

Damit verbleibt aus der ursprünglichen Content-Liste **kein nicht-pausierter P0/P1-Blocker**, der zwingend noch durch neue Contentdefinitionen geschlossen werden muss.

Offen bleiben nur:

- Balancing/Playtests
- manuelle Shopbefüllung
- spätere optionale Content-Erweiterungen
- die ausdrücklich pausierten Bereiche

### Definition of Done

Ein Content-System wird ab diesem Stand als fertig behandelt, wenn es:

1. technisch implementiert ist,
2. einen vollständigen Spieler-Loop besitzt,
3. ohne Admin-Eingriff erreichbar ist oder bewusst manuell gepflegt wird,
4. seine benötigten Progressions-/Reward-Verbindungen besitzt,
5. nicht nur als ungenutzte Definition existiert.



---

# CONTENT AUDIT v2 — Verbindlicher Stand 24.09.2026

> **Wichtig:** Dieser Abschnitt ist der aktuelle, gegen den tatsächlichen Branch `test` geprüfte Content-Status. Ältere Aussagen weiter oben in dieser Datei können durch die zwischenzeitlichen Implementierungen überholt sein. Für die Entscheidung **„fertig / Content-Lücke / bewusst pausiert“** ist ausschließlich dieser Abschnitt maßgeblich.

## 1. Prüfgrundlage

Geprüft wurde der tatsächliche Repository-Inhalt von:

- Branch: `test`
- Build: Paper 26.2 / Java 25
- `src/main/java`
- `src/main/resources`
- `resourcepack/`
- `config.yml`
- `data/*.json`
- `data/story_campaign.yml`
- vorhandene Quest-, Item-, Rezept-, Companion-, Boss- und Equipmentdefinitionen

Die Bewertung beantwortet **nicht**, ob das Plugin technisch fehlerfrei ist. Sie beantwortet:

> **Ist die jeweilige Implementierung bereits mit genügend spielbarem Content versehen, oder fehlt noch konkreter Spieler-Content?**

---

# 2. Statuslegende

| Status | Bedeutung |
|---|---|
| 🟢 FERTIG | Der vorgesehene Content-Loop ist vorhanden. Weitere Inhalte sind Erweiterungen/Balancing. |
| 🟡 CONTENT-LÜCKE | Die Technik existiert, aber ein relevanter Spieler-Content-Bestand fehlt. |
| 🟠 INTEGRATIONS-LÜCKE | Content existiert, ist aber noch nicht vollständig mit dem vorgesehenen Gameplay-Loop verbunden. |
| ⚪ PAUSIERT | Bewusst zurückgestellt; nicht als offene Pflichtarbeit behandeln. |
| 🔵 OPTIONALE ERWEITERUNG | Kein notwendiger Fertigstellungsblocker. |

---

# 3. Gesamtübersicht

| System | Aktueller Stand | Status |
|---|---:|---|
| Player / Level 1–99 | vorhanden | 🟢 |
| Player Profile / Story-Fortschritt | vorhanden | 🟢 |
| Custom Items | 30 Definitionen | 🟢 |
| Spieler-Custom-Items | 29 | 🟢 |
| Admin/Unique Item | 1 | 🟢 |
| Waffen | 5 Spielerwaffen | 🟢 |
| Rüstungssets | 6 vollständige Sets | 🟢 |
| Custom-Crafting | 29 Custom-Result-Rezepte | 🟢 |
| gesamtes Crafting | 329 Rezepte | 🟢 |
| Food | 23 Definitionen | ⚪ |
| Berufe | 7 Crafting + 2 passive | 🟢 |
| reguläre Quests | 269 | 🟢 |
| Story-REACH-Quests | 17 | 🟢 Basis / 🟡 Kampagnenausbau |
| Story-Kampagne | umfangreiche Datenstruktur | 🟡 |
| Companions | 28 | 🟢 |
| Companion Quest-Unlocks | 25 | 🟢 |
| Boss-Reward-Items | 32 | 🟢 |
| Boss-System | vorhanden | 🟢 |
| NPC-System | vorhanden | 🟢 |
| globale NPC-Weltpopulation | absichtlich nicht vorgesehen | ⚪ |
| Shops | System vorhanden, manuell zu befüllen | 🟢 |
| Bank | vorhanden | 🟢 |
| physische Currency | vorhanden, Stack 99 | 🟢 |
| Party | vorhanden | 🟢 |
| Guild | vorhanden | 🟢 |
| Trade Depot | vorhanden | 🟢 |
| Combat / Weapon Abilities | vorhanden | 🟢 |
| Statistics / Scoreboard | vorhanden | 🟢 |
| Regionen | Technik vorhanden | ⚪ |
| Resourcepack | teilweise vorhanden | ⚪ / 🔵 |
| Weltplatzierung / finale NPC-Positionen | abhängig von Zielwelt | 🟡 |

---

# 4. Player Progression

## Vorhanden

Das Plugin besitzt:

- Level-System
- XP
- PlayerProfile
- Story-Fortschritt
- Quest-Fortschritt
- Berufsfortschritt
- Companion-Fortschritt
- Statistiken
- Playtime
- Level-Anforderungen für Items/Rezepte

Die Progression reicht bis Level 99.

### Bewertung

Kein grundlegendes Progressionssystem fehlt.

**Status: 🟢 FERTIG**

---

# 5. Custom Items

## Tatsächlicher Bestand

`item-definitions.json` enthält **30 Items**.

### 29 normale Spieleritems

#### Waffen

1. Eisenschwert — Level 3
2. Goldklinge — Level 25
3. Diamantklinge — Level 40
4. Netheritklinge — Level 90
5. Feuerball — Level 35, Alchemist

#### Donnerwacht — Level 10

6. Helm
7. Brustplatte
8. Beinschutz
9. Stiefel

#### Schattengeflecht — Level 20

10. Helm
11. Brustplatte
12. Beinschutz
13. Stiefel

#### Stahlwall — Level 30

14. Helm
15. Brustplatte
16. Beinschutz
17. Stiefel

#### Sonnengewand — Level 45

18. Helm
19. Brustplatte
20. Beinschutz
21. Stiefel

#### Kristallwache — Level 60

22. Helm
23. Brustplatte
24. Beinschutz
25. Stiefel

#### Höllenschmiede — Level 80

26. Helm
27. Brustplatte
28. Beinschutz
29. Stiefel

### 1 Admin-/Unique-Item

30. `unique/admin_relic`

### Bewertung

Die frühere Content-Lücke „nur ein Rüstungsset / Waffen existieren nur als Definition“ ist für den aktuellen `test`-Stand **geschlossen**.

Die 29 Spieleritems besitzen inzwischen Custom-Crafting-Rezepte.

**Status: 🟢 FERTIG**

---

# 6. Weapon Progression — was „fertig“ hier konkret bedeutet

Die Weapon Progression ist aktuell:

| Waffe | Level | Beruf | Erwerb |
|---|---:|---|---|
| Eisenschwert | 3 | Schmied | Crafting |
| Goldklinge | 25 | Schmied | Crafting |
| Diamantklinge | 40 | Schmied | Crafting |
| Feuerball | 35 | Alchemist | Crafting |
| Netheritklinge | 90 | Schmied | Crafting |

Damit bedeutet Weapon Progression im aktuellen Plugin:

**Level → Berufsfortschritt → Rezeptfreischaltung → Materialien/Boss-Katalysator → Crafting → Weapon Ability**

Das ist ein vollständiger Progressionsloop.

### Optionale Erweiterung

Mehr Waffen wären Vielfalt, aber keine fehlende Implementierung.

**Status: 🟢 FERTIG**

---

# 7. Rüstungssets

## Tatsächlicher Bestand

Es existieren **6 vollständige Sets**:

1. Donnerwacht — Kupfer — Level 10
2. Schattengeflecht — Kette — Level 20
3. Stahlwall — Eisen — Level 30
4. Sonnengewand — Gold — Level 45
5. Kristallwache — Diamant — Level 60
6. Höllenschmiede — Netherit — Level 80

Die Sets besitzen Set-Boni.

Zusätzlich sind alle 24 Rüstungsteile als Custom-Crafting-Ergebnisse angebunden.

### Bewertung

Die Set-Vielfalt ist ausreichend für die aktuelle Progression.

Weitere Sets sind **Content-Erweiterung**, nicht Lückenbehebung.

**Status: 🟢 FERTIG**

---

# 8. Custom Crafting

## Tatsächlicher Stand

`crafting-recipes.json` enthält **329 Rezepte**.

Davon:

- **300 ursprüngliche/Vanilla-orientierte Rezepte**
- **29 Custom-Item-Result-Rezepte**

### Custom-Rezepte

Alle 29 Spieler-Custom-Items besitzen ein `resultItemId`.

Damit sind insbesondere:

- alle 24 Rüstungsteile
- alle 4 Progressionsschwerter
- Feuerball

erreichbar.

### Berufe

| Beruf | Rezepte |
|---|---:|
| Schmied | 81 |
| Gelehrter | 68 |
| Landwirt | 25 |
| Koch | 23 |
| Schneider | 34 |
| Alchemist | 40 |
| Steinmetz | 58 |
| **Gesamt** | **329** |

Passive Berufe:

- Fischer
- Holzfäller

### Bewertung

Crafting ist nicht mehr nur ein Framework.

Es besitzt einen großen Content-Bestand und eine Custom-Item-Progression.

**Status: 🟢 FERTIG**

---

# 9. Food

## Bestand

`food-definitions.json` enthält **23 Food-Definitionen**.

Darunter:

- Mehl
- Speck roh/gebraten
- Karottensuppe
- Kartoffel mit Speck
- Hühnersuppe
- Pilzfanne
- Apfelmus
- Kürbisbrot
- Beeren-Muffin
- Keks mit Speck
- Süßbeeren-Marmelade
- Getrocknete Melone
- heißer Kakao
- Melonensaft
- Leuchtbeerentee
- Kürbis-Latte
- Spiegelei mit Speck
- Honig mit Milch
- Kaktus-Saft
- Apfelkuchen
- Spiegelei
- Käse

Die Food-Items besitzen eigene Definitionen und teilweise Effekte.

### Entscheidung

Diese **23 Food-Definitionen bleiben ausdrücklich pausiert**, wie vorgegeben.

Sie werden daher **nicht** als offene Content-Lücke gewertet.

**Status: ⚪ BEWUSST PAUSIERT**

---

# 10. Professionen

## Crafting-Berufe

- Schmied
- Gelehrter
- Landwirt
- Koch
- Schneider
- Alchemist
- Steinmetz

## Passive Berufe

- Fischer
- Holzfäller

Die Berufsinfrastruktur umfasst:

- Level
- XP
- Rezepte
- Freischaltungen
- Trainer
- Dialog
- Profession Services

### Bewertung

**Status: 🟢 FERTIG**

Weitere Rezepte = Erweiterung.

---

# 11. Quest-System

## Tatsächlicher Content-Bestand

Questdateien:

| Quelle | Definitionen |
|---|---:|
| Crafting Orders | 60 |
| Additional | 84 |
| Content Expansion 01 | 46 |
| Expansion 02 | 19 |
| Story | 17 |
| V2 | 36 |
| World Expansion | 29 |
| **Gesamt** | **291** |

Davon sind 17 die dedizierten Story-Strukturquests.

### Questtypen / Inhalte

Vorhanden sind u.a.:

- HUNT
- COLLECT
- REACH_LOCATION
- TALK_TO_NPC
- globale Events
- Voraussetzungen
- Folgequests
- Level-Anforderungen
- Berufsanforderungen
- Geldbelohnungen
- XP
- Itembelohnungen
- Companion-Unlocks

### Bewertung

Es besteht kein allgemeiner Questmangel.

**Status: 🟢 FERTIG**

---

# 12. Story-Kampagne

Hier liegt aktuell die **größte echte Content-/Progressionslücke**.

## Vorhanden

`story_campaign.yml` enthält:

- Story-Kapitel
- Levelanforderungen
- Strukturtrigger
- NPC-IDs
- Dialogdaten
- Folgebeziehungen
- Loretexte

Die Datei umfasst rund 549 Zeilen.

## Aktive Haupt-Questkette

17 Story-Quests sind tatsächlich als `REACH_LOCATION` definiert:

1. Village Plains
2. Shipwreck
3. Desert Pyramid
4. Jungle Pyramid
5. Swamp Hut
6. Warm Ocean Ruin
7. Ocean Monument
8. Trail Ruins
9. Mineshaft
10. Pillager Outpost
11. Woodland Mansion
12. Ruined Portal
13. Ancient City / Unter dem Stein
14. Nether Fortress
15. Bastion
16. Stronghold
17. End City

Diese Kette besitzt echte Custom-Item-Belohnungen.

## Aber:

Die Kapitelstruktur enthält weiterhin zusätzliche Kapitelpositionen, die bewusst **keine aktive Strukturquest** besitzen, z.B.:

- Cold Ocean Ruins
- Buried Treasure
- Mineshaft Mesa
- Igloo
- einzelne Ruined-Portal-Varianten
- Nether Fossil
- Trial Chambers
- weitere Village-/Shipwreck-Varianten

Das ist grundsätzlich okay, wenn sie nur Lore-/Nebenüberlieferungen darstellen.

### Größere Lücke: Level-/Arc-Struktur

Die aktuelle Kampagne entspricht **noch nicht vollständig** der ursprünglich gewünschten sauberen Level-1–99-Aufteilung.

Beispielsweise:

- frühe Kapitel liegen bei Level 3–29,
- Mineshaft bei 32,
- Outpost bei 40,
- Mansion bei 43,
- Ruined Portal bei 64,
- Ancient City / Eryn bei **67**,
- Fortress bei 72,
- Bastion bei 75,
- Stronghold bei 81,
- End City bei 84.

Damit fehlen als explizit gefüllte Endgame-Storyabschnitte insbesondere:

- **85–90**
- **91–99**

Außerdem ist die Ancient-City-/Deep-Dark-Erzählung derzeit nicht im ursprünglich vorgesehenen Levelbereich 11–20, sondern als späterer Kampagnenknoten bei Level 67 eingeordnet.

### Inhaltlich fehlt daher noch:

- vollständiger Arc 81–90 rund um End / Enderdrache
- vollständiger Arc 91–99 als Abschluss der Kampagne
- endgültige Verbindung zwischen Stronghold → End → Enderdrache → End City → Abschlusslore
- finales Story-Finale
- finale Endgame-Questkette

**Status: 🟡 CONTENT-LÜCKE**

Das Story-System selbst ist fertig. **Der noch fehlende Content ist die endgültige Kampagnenausarbeitung.**

---

# 13. Minecraft-Lore

## Bereits abgedeckt

Die Story verarbeitet:

- alte Zivilisation
- Archäologie
- Trail Ruins
- Ancient Cities
- Sculk
- Warden
- Nether
- Piglins
- Bastions
- Nether-Fortress
- Strongholds
- End
- End Cities
- Enderdragon-Kontext

### Offene Lorebereiche

Minecraft liefert an mehreren Stellen bewusst keine eindeutige Erklärung.

Das Plugin darf diese weiterhin als:

- In-World-Hypothesen
- Forschungsberichte
- Überlieferungen
- widersprüchliche Zeugenaussagen
- ungelöste Rätsel

erzählen.

### Noch auszuarbeiten

Besonders:

1. Warum die Ancient-City-Zivilisation verschwand.
2. Verbindung zwischen Sculk und dem Untergang.
3. Warum die Erbauer Strongholds errichteten.
4. Was genau zur Flucht ins End führte.
5. Verbindung zwischen End Cities und den alten Erbauern.
6. Ursprung bzw. Entwicklung der Endermen als In-World-Hypothese.
7. Geschichte des Withers.
8. Verbindung zwischen Nether, Piglins und alter Zivilisation.
9. Bedeutung des Enderdrachen für den Untergang.
10. Was nach dem Enderdrachen geschieht.

**Status: 🟡 CONTENT-AUSBAU**

Nicht das Lore-System fehlt – die **finale narrative Ausarbeitung** fehlt.

---

# 14. NPC-System

## Technik

Vorhanden:

- NPC Manager
- NPC Typen
- Mannequin-System
- Skins
- Nameplates
- Blickverhalten
- Interaktionen
- Verhalten
- Story-NPCs
- Quest-NPCs
- Shop-NPCs
- Banker
- Reception
- Profession-Trainer
- Travel
- Filler

### NpcType

Enthalten sind u.a.:

- RECEPTION
- 9 Profession-Typen
- QUEST
- SHOP
- TRAVEL
- FILLER
- STORY
- BANKER

## Weltpopulation

Eine globale, automatisch verteilte FILLER-NPC-Population wird **nicht mehr benötigt**.

Die Story-NPCs werden gezielt über das Story-System an relevanten Strukturen erzeugt.

### Bewertung

**NPC-System: 🟢 FERTIG**

**Globale NPC-Weltpopulation: ⚪ bewusst entfernt/nicht erforderlich**

### Offene Content-Arbeit

Die tatsächlichen NPC-Positionen der finalen Zielwelt müssen weiterhin gesetzt werden.

Das ist **Weltkonfiguration**, nicht NPC-System-Neubau.

**Status: 🟡 Weltcontent**

---

# 15. Story-NPCs / Sichtbarkeit

Vorhanden:

- `NpcType.STORY`
- strukturgebundenes Spawning
- stabile Story-NPC-IDs
- Questabhängigkeit
- Profilprüfung
- selektive Sichtbarkeit
- Chunk-/Tracking-orientierte Aktualisierung
- Story-Dialoge
- Folgequests

Die Story-NPCs sollen nur für Spieler sichtbar sein, die den entsprechenden Story-/Questzustand erfüllen.

### Bewertung

Technischer Story-NPC-Loop ist vorhanden.

**Status: 🟢 FERTIG**

Was noch fehlt, ist primär mehr **fertiger Dialog-/Lore-Content**.

---

# 16. Shops

## Technik

Vorhanden:

- ShopManager
- ShopEntry
- Shop GUI
- Shop Editor
- Kaufen
- Verkaufen
- Preise
- NPC-Anbindung
- Persistenz

## Produktentscheidung

Shops werden **manuell befüllt**.

Deshalb ist ein fehlender Default-Katalog **keine Content-Lücke**, solange die Zielwelt manuell eingerichtet wird.

**Status: 🟢 FERTIG**

### Optional

Weitere Sortimente/Balancing sind später möglich.

---

# 17. Bank

Vorhanden:

- Banker-NPC
- Bankdialog
- Bankinventar
- persistente Speicherung
- Einzahlen
- Auszahlen

Die Bank ist als System vollständig.

## Currency

Die physische Currency ist jetzt:

- physischer Drop
- physisches Inventaritem
- nicht automatisch virtuelle Bankwährung
- Bankeinzahlung separat
- maximaler aktueller Stackwert: **99**

### Wichtig

Die gewünschte Stackgröße 999 ist technisch mit dem aktuellen Paper-26.2-Stacklimit nicht direkt abbildbar.

Daher:

**99 = technische Obergrenze des aktuellen Item-Data-Component-Wegs.**

Das ist keine Content-Lücke.

**Status Bank: 🟢 FERTIG**

---

# 18. Combat

Vorhanden:

- Damage Calculation
- Combat State
- Mob Scaling
- Mob Nameplates
- XP
- Loot
- Weapon Abilities
- Skill Input
- Cooldowns
- Soulbound
- Death Handling

### Waffen-Abilities

Die vorhandenen Custom-Waffen besitzen konkrete Ability-Pfade.

### Bewertung

**Status: 🟢 FERTIG**

Weitere Waffen/Abilities sind Erweiterungen.

---

# 19. Boss-System

Vorhanden:

- Boss Definition
- Boss Manager
- Biome Boss Spawning
- World Bosses
- Boss Phases
- Attack Patterns
- Damage Contribution
- Boss Death
- Boss Loot
- Boss Rewards
- Boss Protection
- Boss Statistics
- Boss Events

### Content

32 Boss-Reward-Items sind vorhanden.

### Bewertung

Das System besitzt genügend Content für einen vollständigen Boss-Loop.

**Status: 🟢 FERTIG**

### Optional

- mehr Bosse
- zusätzliche Phasen
- zusätzliche Lootvarianten
- spezielle Story-Bosse

sind Erweiterungen.

---

# 20. Companions

## Bestand

`companions.json`: **28 Definitionen**

Davon:

- 25 QUEST
- 1 DEFAULT
- 1 BOSS
- 1 ADMIN

Questbasierte Begleiter umfassen u.a.:

- Huhn
- Kuh
- Schaf
- Kaninchen
- Fledermaus
- Katze
- Fuchs
- Ziege
- Kupfergolem
- Papagei
- Gürteltier
- Panda
- Schwein
- Pferd
- Zombiepferd
- Skelettpferd
- Kamel
- Nautilus
- Knarzer
- Glücklicher Ghast
- Eisengolem
- Sumpfskelett
- Spinne
- Creeper
- Schwefelwürfel

Zusätzlich:

- Bienenkönigin als Boss-Unlock
- Hazel als Admin-Companion

### Bewertung

**Status: 🟢 FERTIG**

---

# 21. Equipment

Technisch vorhanden:

- EquipmentService
- Slots
- Level Requirements
- Set Effects
- Set Service
- 6 Sets
- 24 Rüstungsteile

### Bewertung

**Status: 🟢 FERTIG**

---

# 22. Party

Vorhanden:

- Party-Erstellung
- Einladung
- Mitglieder
- Verlassen
- Disconnect Handling
- GUI
- Quest-Sharing
- Share Range

**Status: 🟢 FERTIG**

---

# 23. Guild

Vorhanden:

- Guild-Erstellung
- Mitglieder
- Einladungen
- Guild Bank
- Guild Currency
- Guild Compass
- Guild API
- Commands
- Persistenz

### Guild City

Die konkrete Welt-/Regionsgestaltung ist weiterhin nicht Bestandteil des aktuellen Content-Scope.

**Guild-System: 🟢**

**Guild-City-Weltcontent: ⚪ PAUSIERT**

---

# 24. Trade Depot

Vorhanden:

- Listings
- Kaufen
- Verkaufen
- Preise
- Ablauf
- Pending Payout
- GUI
- Persistenz

Das System erzeugt seinen Content aus Spielerangeboten.

**Status: 🟢 FERTIG**

---

# 25. Statistics / Scoreboard / Playtime

Vorhanden:

- Mob Kill Statistics
- Death Statistics
- Quest/Boss Statistics
- Scoreboard
- Playtime

**Status: 🟢 FERTIG**

---

# 26. Travel / Guild Compass

Vorhanden:

- Guild Compass
- Travel Dialog
- Reise-NPC-Typ
- Travel-System

### Offener Content

Konkrete Reisepunkte müssen zur tatsächlichen Welt definiert werden.

**Status System: 🟢**

**Status Weltpunkte: 🟡 Weltkonfiguration**

---

# 27. Regionen

Technisch vorhanden:

- Region Manager
- Region Repository
- Region Types
- Geometry
- Flags
- Spawn Points
- Policies
- Transitions
- Editor

Konkreter Weltcontent ist jedoch nicht vollständig ausgebaut.

### Entscheidung

Regionen bleiben bewusst pausiert.

**Status: ⚪ PAUSIERT**

---

# 28. Resourcepack

Im Repository existiert bereits ein Resourcepack-Verzeichnis.

Aktuell vorhanden sind insbesondere Food-Modelle/-Definitionen.

Da Food ausdrücklich pausiert wurde, ist auch dessen visuelle Pipeline kein aktueller Pflichtblocker.

### Bewertung

**Status: ⚪ PAUSIERT**

---

# 29. Commands

Die aktuelle Command-Struktur enthält u.a.:

- `item`
- Player Admin
- Debug
- Party
- Quest Log
- Dialogue
- Guild
- Boss
- Companion
- Edit
- NPC
- Quest Admin
- Region
- Shop

Damit sind die administrativen und spielerischen Verwaltungssysteme grundsätzlich erreichbar.

**Status: 🟢 FERTIG**

---

# 30. Content-Lücken — tatsächlich noch offen

Nach Bereinigung der inzwischen erledigten Punkte bleiben aktuell folgende **echte** Content-Arbeiten:

## 🔴 P0 — Kampagnenabschluss

### 1. Story Level 81–99 finalisieren

Der aktuelle Storypfad endet bei:

**End City / Level 84**

Es fehlen noch explizite Abschlussabschnitte für:

- 85–90
- 91–99

Insbesondere:

- Enderdrache als zentraler Storyhöhepunkt
- Abschlussquest
- Rückkehr/Consequences
- finale Lore-Verknüpfung
- Endgame-Belohnung
- Epilog

---

## 🟠 P1 — Story-Lore ausformulieren

Die Story-Infrastruktur steht.

Noch benötigt werden vor allem die **finalen Texte**:

- Ancient-City-Erklärung
- Sculk/Warden-Mysterium
- alte Erbauer
- Wither
- Nether/Piglins
- Stronghold
- End
- Endermen
- Enderdrache
- End Cities
- finale Verbindung aller Hinweise

---

## 🟠 P1 — Weltplatzierung

Für den finalen Spielbetrieb müssen die vorhandenen Systeme mit der konkreten Zielwelt verbunden werden:

- Reception
- Banker
- Shop-NPCs
- Profession-Trainer
- Questgeber
- Travel-NPCs
- Story-NPC-Strukturen

Das ist kein neuer NPC-Code.

Es ist **Content-/Weltkonfiguration**.

---

## 🟡 P2 — Story-Nebencontent

Optional, aber sinnvoll:

- mehr Nebenquests
- NPC-Gerüchte
- Bücher
- Archäologie-Funde
- versteckte Lore
- zusätzliche Companion-Quests
- alternative Dialogzweige
- Endgame-Geheimnisse

---

# 31. Bewusst NICHT als Lücke behandeln

Diese Punkte werden ausdrücklich **nicht** als offen gewertet:

### Food

23 Definitionen vorhanden, aber bewusst pausiert.

### Globale NPC-Weltpopulation

Nicht mehr gewünscht.

Story-NPCs werden gezielt erzeugt.

### Shops

Technik fertig; Sortimente werden manuell gepflegt.

### Rüstungssets

6 vollständige Sets vorhanden.

### Weapon Progression

5 Spielerwaffen + 29 Custom-Rezepte vorhanden.

### Bank

System fertig.

### Guild City

Pausiert.

### Regionen

Pausiert.

### Resourcepack

Pausiert.

---

# 32. Was aktuell NICHT mehr gemacht werden sollte

Die folgenden Systeme brauchen keinen weiteren grundlegenden Umbau:

- NPC-System
- Bank
- Shop-System
- Crafting-System
- Equipment-System
- Companion-System
- Boss-System
- Party-System
- Guild-Kernsystem
- Trade Depot
- Combat-Kern
- Quest-Kern
- Item-System

Hier sollte ab jetzt **nicht mehr aus Prinzip Code erweitert werden**.

Neue Arbeit sollte nur erfolgen, wenn ein konkreter Content-Loop sie benötigt.

---

# 33. Aktuelle Fertigstellungsdefinition

PixelRPG kann einen Bereich als **fertig** behandeln, wenn:

1. das technische System existiert,
2. mindestens ein vollständiger Spieler-Loop existiert,
3. der Content tatsächlich erreichbar ist,
4. Belohnungen/Progression definiert sind,
5. keine zweite technische Sonderlösung erforderlich ist.

Nach diesem Maßstab sind aktuell die meisten Kernsysteme **fertig**.

Die verbleibende Hauptbaustelle ist nicht mehr das Framework.

> **Die verbleibende Hauptbaustelle ist die finale Welt- und Storybefüllung.**

---

# 34. Empfohlene Reihenfolge der letzten Content-Arbeiten

## Phase 1 — Kampagne fertig machen

1. Level 85–90
2. Level 91–99
3. Enderdragon-Story
4. finales Lore-Netz
5. Epilog
6. finale Storybelohnung

## Phase 2 — Welt platzieren

1. Reception
2. Berufstrainer
3. Banker
4. Shops
5. Quest-NPCs
6. Travel-NPCs
7. Story-NPC-Zielpunkte

## Phase 3 — Story-Nebencontent

1. Bücher
2. Archäologie
3. NPC-Gerüchte
4. Nebenquests
5. Geheimdialoge
6. optionale Lore

## Phase 4 — Balancing

1. XP
2. Quest Rewards
3. Crafting Costs
4. Boss Loot
5. Shoppreise
6. Economy
7. Level Gates

---

# 35. Abschlussbefund

### 🟢 Bereits als fertig behandelbar

- Player/Level
- Items
- Weapon Progression
- 6 Armor Sets
- Custom Crafting
- 9 Berufe
- Quest-System
- Companion-System
- Boss-System
- Combat
- Bank
- Shops
- Party
- Guild-Kern
- Trade Depot
- Statistics
- NPC-System

### ⚪ Bewusst pausiert

- Food
- globale NPC-Weltpopulation
- Guild-City-Weltcontent
- Regionen
- Resourcepack-Ausbau

### 🟡 Noch mit Content füttern

**Hauptsächlich:**

1. Story 85–99
2. Enderdragon-/Endgame-Abschluss
3. finale Lore-Verknüpfung
4. konkrete NPC-/Weltplatzierung
5. optionale Nebenstory

Damit ist die Liste wesentlich kleiner als bei der vorherigen Content-Prüfung.

**Der größte Fehler wäre jetzt, erneut die bereits fertigen Systeme umzubauen. Ab diesem Stand sollte der Fokus fast vollständig auf Weltplatzierung, Storytexten und anschließendem Balancing liegen.**


---

# 36. Content-Audit Update — Story-Kampagne Level 1–99 abgeschlossen

**Stand:** 24.09.2026  
**Branch:** `test`

Die zuvor offene P0/P1-Lücke der linearen Hauptkampagne wurde geschlossen.

## Hauptkampagne

Die Kampagne besitzt jetzt eine durchgängige `order`-Kette von **0 bis 19**:

1. Die ersten Spuren
2. Dorf der ersten Stimmen
3. verlorene Flotte
4. Wüstenpyramide
5. Dschungeltempel
6. Hexenhütte
7. Ozeanruinen
8. Ozeanmonument
9. Trail Ruins
10. Mineshaft
11. Pillager Outpost
12. Woodland Mansion
13. Ruined Portal
14. Ancient City
15. Nether Fortress
16. Bastion Remnant
17. Stronghold
18. Enderdrache
19. End City / Archiv nach dem Ende

Die zehn gewünschten Arcs sind über das YAML-Feld `arc` abgebildet.

## Endgame

### Level 81–90 — Der Drache

- Stronghold
- Endportal
- Enderdrache
- HUNT-Quest `story_campaign_dragon`
- vorhandenes Quest-Mob-Kill-System
- keine zusätzliche parallele Killpipeline

### Level 91–99 — Was hinter dem Ende bleibt

- End City / Endschiff
- Elytra-/End-City-Lore
- Endermen als ausdrücklich gekennzeichnete In-World-Hypothese
- abschließendes Archiv
- Level-99-Epilog
- Quest `story_campaign_after_the_end`

Der Epilog wird nach Abschluss der End-City-Kampagne automatisch als Folgequest aktiviert und bleibt bis Level 99 aktiv. Dadurch kann derselbe bereits gespawnte Story-NPC Silex weiterhin sichtbar bleiben, ohne einen zusätzlichen globalen NPC-Spawn zu benötigen.

## Status

**Story-Kampagne: 🟢 CONTENT-SEITIG VOLLSTÄNDIG**

Noch offen sind ausschließlich Runtime-/Playtest-/Balancing-Fragen.

Die bewusst pausierten Bereiche bleiben unverändert:
- Food
- globale NPC-Weltpopulation
- Guild City
- Regionen
- Resourcepack-Ausbau
- manuelle Shop-Befüllung


---

# 37. Forensischer Abgleich — verbindlicher aktueller Content-Stand

**Prüfdatum:** 24.09.2026  
**Branch:** `test`  
**Forensik-Referenz:** `Forensik.md` auf demselben Branch  
**Regel:** Dieser Abschnitt überschreibt ältere, inzwischen widersprüchliche Zählungen/Statusangaben in den historischen Audit-Abschnitten.

Die folgende Prüfung wurde direkt gegen den aktuellen Repository-Stand durchgeführt. Es werden nur tatsächliche Befunde dokumentiert. Die FILLER-NPC-Thematik wird ausdrücklich **nicht verändert und nicht als offene Baustelle geführt**.

## 37.1 Questbestand — korrigierte Zählung

Der aktuelle Datenbestand enthält:

| Quelle | Definitionen |
|---|---:|
| Crafting Orders | 60 |
| Additional | 84 |
| Content Expansion 01 | 46 |
| Expansion 02 | 19 |
| V2 | 36 |
| World Expansion | 29 |
| Story | 19 |
| **Gesamt** | **293** |

Die Story-Datei enthält **19 Questdefinitionen**, obwohl `story_campaign.yml` insgesamt **20 Kapitelknoten (Order 0–19)** besitzt. Das erste Kapitel `first_traces` ist ein reines Story-/Dialogkapitel ohne eigene Questdefinition.

**Verbindlicher Stand: 293 aktive Questdefinitionen.**

### Quest-GUI

Der aktuelle `QuestLogGUI` zeigt die Quests weiterhin als **eine flache, status-/alphabetisch sortierte Liste** und begrenzt die Darstellung auf 45 Einträge.

Die gewünschte Progressionsdarstellung in **10 Levelbereichen** ist aktuell noch nicht umgesetzt:

1. Level 1–10
2. Level 11–20
3. Level 21–30
4. Level 31–40
5. Level 41–50
6. Level 51–60
7. Level 61–70
8. Level 71–80
9. Level 81–90
10. Level 91–99

Damit ist die Questmenü-Struktur weiterhin eine **offene Content-/UI-Integrationsaufgabe**. Die Questdaten selbst sind vorhanden.

**Status Questdaten: 🟢**  
**Status 10-Level-Bereiche: 🟠 OFFEN**

---

## 37.2 Story-Kampagne — Datenbestand ist vollständig, UI-Workflow noch nicht vollständig

Die Hauptkampagne besitzt aktuell:

- Orders 0–19
- 10 definierte Arcs
- Story-Level 1–99
- Stronghold
- Enderdrache
- End City
- Level-99-Epilog
- `story_campaign_dragon`
- `story_campaign_after_the_end`

Damit ist die frühere Lücke „Story endet bei Level 84“ **nicht mehr aktuell**.

**Status Hauptkampagne: 🟢 CONTENT-SEITIG VOLLSTÄNDIG**

### Reception → Story

Der Reception-Dialog besitzt bereits einen separaten **Story-Eintrag** und kann eine aktive Story-Kampagne öffnen.

Aktuell vorhanden:

- Story anzeigen
- Storydialog öffnen
- Storyquest annehmen
- Storyfortschritt/Questzustand anzeigen

Aktuell **nicht vollständig** umgesetzt ist der gewünschte vollständige Workflow direkt aus der Reception:

- Storyquest annehmen: **vorhanden**
- Storyquest abgeben: **nicht als eigener Reception-Workflow vorhanden**
- Storyquest abbrechen: **nicht als eigener Reception-Workflow vorhanden**

Das allgemeine Questdetail besitzt zwar eine Abbruchfunktion, und Story-NPCs besitzen Abgabe-/Berichtsfunktionen. Das ersetzt jedoch nicht den ausdrücklich gewünschten **Reception → Story → Annehmen / Abgeben / Abbrechen**-Loop.

**Status: 🟠 CONTENT-/UI-INTEGRATIONS-LÜCKE**

---

## 37.3 PixelRPG-Progressionsitems — korrigierter Bestand

`item-definitions.json` enthält aktuell **30 Definitionen**:

- **28 klassische Progressionsitems:** 4 Progressionsschwerter + 24 Rüstungsteile
- **1 zusätzliches spielbares Custom-Item:** Feuerball
- **1 Admin-/Unique-Relikt**

Damit ist die frühere Formulierung „29 normale Progressionsitems“ fachlich zu präzisieren.

Die sechs Rüstungssets sind weiterhin:

1. Donnerwacht — Level 10
2. Schattengeflecht — Level 20
3. Stahlwall — Level 30
4. Sonnengewand — Level 45
5. Kristallwache — Level 60
6. Höllenschmiede — Level 80

---

## 37.4 Custom-Crafting — 329 Rezepte, aber 14 Boss-Objekt-Abhängigkeiten

Der aktuelle Crafting-Bestand enthält **329 Rezepte**.

Davon sind **14 Rezepte direkt von `pixelrpg:boss/*`-Items abhängig**.

Das widerspricht der inzwischen festgelegten Designregel:

> **Normale Crafting-Rezepte dürfen keine Boss-Objekte als Zutaten verwenden.**

Die betroffenen Progressionsrezepte betreffen insbesondere die höheren Ausrüstungsstufen.

Beispiele aus dem aktuellen Stand:

- Sonnengewand → `pixelrpg:boss/goldenes_fossil`
- Kristallwache → `pixelrpg:boss/echoherz`
- Höllenschmiede → `pixelrpg:boss/netherkern`
- Netheritklinge → `pixelrpg:boss/endriss`
- Feuerball → `pixelrpg:boss/sumpftrank`

### Verbindliche Zielrichtung

Diese Boss-Objekte sollen aus den normalen Rezeptkosten verschwinden.

Stattdessen müssen die Rezepte auf normale Minecraft-Materialien und/oder bereits vorhandene reguläre RPG-Ressourcen umgestellt werden.

Die konkrete Materialauswahl ist noch nicht als verbindlicher Content festgelegt und darf daher nicht erfunden werden.

**Status: 🟠 OFFENE CONTENT-/CRAFTING-ÄNDERUNG**

---

## 37.5 Boss-Belohnungen — 32 Custom-Bossobjekte vorhanden

`boss-reward-items.json` enthält **32 Boss-Reward-Definitionen**.

Der aktuelle Bestand verwendet durchgehend `BRUSH` als Vanilla-Basis und kennzeichnet die Gegenstände über `pixelrpg:boss/*`-IDs und eigene Fähigkeiten.

Damit ist die technische Anzahl der Bossbelohnungen ausreichend, aber das gewünschte neue Reward-Design ist **noch nicht umgesetzt**:

- Vanilla-Basisitem
- RPG-Stats / Ability
- keine künstliche „Bossobjekt“-Materialklasse als zentrale Progressionsressource

Wichtig: Die konkrete Ersatzmaterialverteilung ist noch offen. Der frühere Iron-Block-Beispielwert ist **keine verbindliche Vorgabe**.

**Status Anzahl: 🟢**  
**Status gewünschtes Reward-Design: 🟠 OFFEN**

---

## 37.6 Monster-Loot — aktuell weiterhin sehr großzügig

`config.yml` enthält aktuell:

`items.loot.item-drop-chance: 0.60`

Der `LootDropListener` würfelt diese Chance bei registrierten Spielern auf Monster-Tode und erzeugt bei Erfolg ein RPG-Item bzw. vorhandene Itemdefinitionen.

Damit liegt aktuell eine **60-%-Chance auf zusätzlichen PixelRPG-Item-Loot** vor.

Das entspricht nicht dem inzwischen gewünschten **grindigeren Progressionsgefühl**.

Die Loot-Logik besitzt bereits eine Levelgewichtung für das Itemlevel; das Problem ist primär die hohe Grundchance und damit die Häufigkeit.

**Status: 🟠 BALANCING-/CONTENT-LÜCKE**

Zielrichtung:

- Vanilla-Loot bleibt häufig.
- RPG-Loot wird seltener.
- Höhere Progressionsstufen dürfen bessere Chancen/Qualität erhalten.
- Bossloot bleibt davon getrennt.

Die konkrete Zielchance sollte erst nach dem gewünschten Economy-/Progressions-Balancing festgelegt werden.

---

## 37.7 NPC-Weltcontent — FILLER ausdrücklich ausgeschlossen

Das NPC-System enthält weiterhin:

- RECEPTION
- 9 Profession-Trainer
- QUEST
- SHOP
- TRAVEL
- STORY
- BANKER
- FILLER

Die FILLER-NPCs werden in diesem Audit **nicht verändert**.

### Travel

Der `TRAVEL`-Typ und dessen Interaktion sind vorhanden. Eine allgemeine automatische „ein Travel-NPC in jedes Dorf“-Logik ist nicht als verbindlicher Content festgelegt.

Die gewünschte Weltpopulation lautet weiterhin:

- nur gelegentlich Travel-NPCs
- bevorzugt in Dörfern mit vorhandener Glocke
- nicht jedes Dorf
- Story-/Struktur-NPCs ebenfalls selektiv
- keine globale FILLER-Population als Ersatz

Das ist primär **Welt-/Spawn-Konfiguration**, nicht ein fehlendes NPC-Grundsystem.

**Status Travel-System: 🟢**  
**Status finale Weltplatzierung: 🟡**

---

## 37.8 Story-NPC-Strukturen

Das Story-System arbeitet mit strukturbasierten Storykapiteln und stabilen Story-NPC-IDs.

Der aktuelle `NpcManager` behandelt Story-Mannequins grundsätzlich separat von normalen NPCs; Story-NPCs sind zunächst nicht standardmäßig für alle Spieler sichtbar und werden über den Story-/Trackingpfad synchronisiert.

Die zuvor geklärte FILLER-NPC-Frage wird hiervon **nicht neu aufgerollt**.

**Status technischer Story-NPC-Loop: 🟢**  
**Status finale Welt-/Strukturplatzierung: 🟡**

---

## 37.9 Forensische Befunde, die NICHT als Content-Lücken umetikettiert werden dürfen

Die `Forensik.md` nennt mehrere technische Befunde:

- MySQL-Fallback kann einen geöffneten Hikari-Pool im Fehlerpfad zurücklassen.
- Synchrones JSON-Speichern existiert in einem derzeit nicht nachgewiesen aktiven Pfad.
- `QuestManager.checkReachLocationQuests()` kann teure Structure-/Biome-Locator-Aufrufe auslösen.
- modernes NMS-Reflection im Name-Visibility-Pfad ist versionsgebunden.
- externe Skin-Auflösung bleibt eine Netzwerk-Trust-Boundary.
- Debug-Permission ist inkonsistent (`pixelrpg.admin` vs. `rpg.admin`).
- CI führt keinen vollständigen gestarteten Paper-Server-Smoke-Test aus.

Diese Punkte sind **keine Content-Lücken** und werden in dieser Datei nur zur Abgrenzung dokumentiert. Sie gehören in die technische Forensik und dürfen nicht durch Content-Änderungen kaschiert werden.

---

# 38. Verbindlicher aktueller Prioritätsstand

## 🔴 P0

### 1. Custom-Crafting von Boss-Objekten entkoppeln
**14 Rezepte** müssen von `pixelrpg:boss/*`-Zutaten befreit werden.

### 2. Boss-Reward-Design umstellen
**32 Bossbelohnungen** auf Vanilla-Basis + RPG-Stats/Abilities neu definieren.

## 🟠 P1

### 3. Monster-RPG-Loot grinden statt inflationär machen
Aktuelle Grundchance: **60 %**.  
Ziel: deutlich seltenerer RPG-Loot bei weiterhin funktionierender Progression.

### 4. Questlog in 10 Levelbereiche aufteilen
Die Daten besitzen `categoryLevel`; die GUI nutzt diese Information aktuell nicht für die gewünschte Bereichsdarstellung.

### 5. Reception-Story-Workflow vervollständigen
Story-Annehmen ist vorhanden; Abgeben und Abbrechen müssen für den gewünschten Reception-Loop ergänzt werden.

## 🟡 P2

### 6. Travel-/Story-Weltplatzierung
Selektive Travel-NPCs und selektive Story-Strukturen/NPCs in der finalen Zielwelt setzen.

### 7. Story-Nebencontent
Weitere Bücher, Gerüchte, optionale Lore und Nebenquests sind Erweiterungen, keine Kernblocker.

---

# 39. Bewusst unverändert / ausgeschlossen

Diese Punkte werden bei der nächsten Umsetzung **nicht** unnötig angefasst:

- FILLER-NPC-System
- globale FILLER-Population
- grundlegender NPC-Kern
- Bank-Kern
- Quest-Kern
- Boss-Kern
- Companion-Kern
- Party-Kern
- Guild-Kern
- Trade-Depot-Kern
- pausierte Food-Content-Pipeline
- pausierte Regionen
- pausierter Resourcepack-Ausbau
- pausierter Guild-City-Weltcontent

Der Fokus liegt damit nicht auf einem weiteren Komplettumbau, sondern auf den konkret belegten Lücken aus diesem Abgleich.
