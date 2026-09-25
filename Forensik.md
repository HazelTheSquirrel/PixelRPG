# PixelRPG – Forensische Feature- & Inhaltsanalyse

**Prüfobjekt:** HazelTheSquirrel/PixelRPG  
**Branch:** `test`  
**Prüfstand:** 25.09.2026  
**Branch-HEAD:** `07ce91803d2f397f81ee606d393edbeafc83a22a`

Dieses Dokument behandelt ausschließlich den tatsächlich vorhandenen Feature-, Content- und Datenbestand. Architektur-/Security-Befunde stehen in `audit.md`.

---

## 1. Bestandsübersicht

Der Branch enthält einen umfangreichen RPG-Kern mit:

- Spielerprofile, Level und XP
- Quests und Story-Kampagne
- 9 Berufen
- 329 Crafting-Rezepten
- 30 Itemdefinitionen
- 23 Food-Definitionen
- 6 Equipment-Sets
- 32 Boss-Reward-Items
- 32 Bossdefinitionen
- 28 Companions
- NPC-/Mannequin-System
- native Paper-Dialoge
- Party
- Guild
- Bank/Economy
- Shop
- Trade Depot
- Combat
- Mob Scaling
- Weapon Abilities
- Regionen
- Statistics
- Scoreboard
- Playtime
- Resourcepack

---

## 2. Spielerprogression

### Level

- Level 1–99
- Level-/XP-Berechnung
- XP bis zum nächsten Level
- Level-Up-Events
- Transzendenz-Schwelle oberhalb der normalen Progression

### PlayerProfile

Gespeichert werden unter anderem:

- UUID
- Registrierung
- Erfahrung
- Geld
- Waypoints
- Story-Fortschritt
- aktive Quests
- abgeschlossene Quests
- Scoreboard-Einstellungen
- Party-HUD
- Quest-Tracker
- Spielzeit
- Equipment
- Berufslevel
- Berufs-XP
- erlernte Berufe
- freigeschaltete Rezepte
- Statistiken
- Navigation Targets
- Persistenzrevision

---

## 3. Items

`item-definitions.json`: **30 Definitionen**

### Waffen

- Eisenschwert
- Goldklinge
- Diamantklinge
- Netheritklinge
- Feuerball

### Equipment

6 Sets mit jeweils 4 Teilen:

1. Donnerwacht
2. Schattengeflecht
3. Stahlwall
4. Sonnengewand
5. Kristallwache
6. Höllenschmiede

### Unique/Admin

- Admin-Relikt

Item-System unterstützt:

- Item ID
- Rarity
- Level
- Required Level
- Gearscore
- RPG Stats
- Soulbound
- Unique
- Admin-only
- Equipment Slots
- Set IDs
- Weapon Abilities
- Resourcepack IDs
- PDC-Identifikation

Rarities:

- COMMON
- UNCOMMON
- RARE
- EPIC
- LEGENDARY
- UNIQUE

---

## 4. Equipment Sets

6 vollständige Sets.

Jedes Set besitzt:

- 2-Teile-Bonus
- 3-Teile-Bonus
- 4-Teile-Bonus
- Set-ID
- Armor Trim
- RPG Stats

Unterstützte Werte umfassen:

- Movement Speed
- Reach
- Attack Power
- Crit
- Crit Damage
- Lifesteal
- Armor
- HP

---

## 5. Food

`food-definitions.json`: **23 Definitionen**

Der Katalog enthält unter anderem:

- Mehl
- roher Speck
- gebratener Speck
- Karottensuppe
- Kartoffel mit Speck
- Hühnersuppe
- Pilzfanne
- Apfelmus
- Kürbisbrot
- Beeren-Muffin
- heißer Kakao
- Getränke
- Marmelade
- Kuchen
- Ei-/Speck-Gerichte
- Käse

Food-Daten:

- Nutrition
- Saturation
- Custom Item ID
- Rarity
- Resourcepack ID
- Consumable-Daten

**Forensischer Restpunkt:** Die Definitionen sind vorhanden; der konkrete Erwerbsweg jedes Foods muss separat über Rezepte, Shops, Drops oder andere Gameplaypfade nachvollzogen werden.

---

## 6. Crafting

`crafting-recipes.json`: **329 Rezepte**

Aktive Berufe:

- BLACKSMITH
- SCHOLAR
- FARMER
- COOK
- TAILOR
- ALCHEMIST
- MASON

Passive Berufe:

- FISHERMAN
- WOODCUTTER

Die Rezeptregistry unterstützt:

- Vanilla-Material-Ergebnisse
- PixelRPG-`resultItemId`
- Materialkosten
- Custom-Item-Kosten
- Beruf
- Kategorie
- Berufslevel
- Unlock-Preis
- Quest-Voraussetzung
- Default-Unlock
- Rarity
- Potion
- Enchantment

Validierungen:

- unbekannte Materialien
- unbekannte Custom Items
- ungültige Berufe
- ungültige Levels
- ungültige Mengen
- ungültige Potion-Typen
- ungültige Enchantments
- Vanilla-Maximallevel
- zirkuläre Custom-Item-Abhängigkeiten

---

## 7. Berufe

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

Das System besitzt:

- ProfessionService
- Level
- XP
- Trainer-NPCs
- Aktivitätslistener
- Crafting
- Unlocks
- Dialoge

---

## 8. Quests

Die vorhandenen Questdateien bilden zusammen **149 Questdefinitionen**.

Quellgruppen:

- Crafting Orders
- Additional
- Content Expansion 01
- Expansion 02
- Story
- V2
- World Expansion

Questtypen:

- HUNT
- COLLECT
- TALK_TO_NPC
- REACH_LOCATION
- GLOBAL_EVENT

Mechaniken:

- Voraussetzungen
- Follow-ups
- Levelanforderungen
- Berufsanforderungen
- Rewards
- Companion-Unlocks
- Party-Quest-Sharing
- Quest-Tracker
- passive Questprüfung
- Mob-Kills
- NPC-Interaktionen
- Locations
- Global Events
- Boss-/Statistik-Verknüpfungen

---

## 9. Story-Kampagne

`story_campaign.yml`:

- Story-Version 7
- Kampagnen-ID `minecraft-lore-campaign`
- maximal 64 Kapitel
- 20 lineare Hauptkampagnenpositionen
- Orders 0–19

Die Kampagne führt über:

1. erste Spuren
2. Dorf
3. Schiffswrack
4. Wüstenpyramide
5. Dschungelpyramide
6. Sumpfhütte
7. warme Ozeanruine
8. Monument
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
19. End City
20. After the End

Jeder Hauptknoten kann besitzen:

- Order
- ID
- Titel
- Arc
- Required Level
- Structure Trigger
- NPC ID
- Quest IDs
- Dialog
- XP Reward

---

## 10. NPC-System

NPC-Typen:

- RECEPTION
- PROFESSION_BLACKSMITH
- PROFESSION_SCHOLAR
- PROFESSION_FARMER
- PROFESSION_COOK
- PROFESSION_TAILOR
- PROFESSION_ALCHEMIST
- PROFESSION_MASON
- PROFESSION_FISHERMAN
- PROFESSION_WOODCUTTER
- QUEST
- SHOP
- TRAVEL
- FILLER
- STORY
- BANKER

Technik:

- persistente NPC-ID
- Mannequin
- Chunk-Index
- Spawn/Despawn
- Nameplate
- Sichtbarkeit
- Skin
- Behavior Registry
- Story-Sichtbarkeit
- Chunk Load/Unload
- Player Resync

### Runtime-Content

Der NPC-Manager erwartet `npcs.yml` im Plugin-Datenordner.

Eine Repository-Default-`npcs.yml` ist im aktuellen Branch nicht vorhanden.

Daraus folgt:

**NPC-Engine vorhanden; automatische Weltpopulation nicht vollständig als Repository-Default vorhanden.**

---

## 11. Shops

Technik:

- ShopManager
- ShopEntry
- Buy/Sell
- NPC-Zuordnung
- ShopGUI
- ShopEditorGUI
- YAML-Persistenz
- Migration

Der Runtime-Manager erwartet `shops.yml`.

Eine Repository-Default-`shops.yml` ist nicht vorhanden.

Daraus folgt:

**Shop-Engine vorhanden; konkreter Shop-Warenbestand ist kein vollständiger statischer Repository-Default.**

---

## 12. Bosse

Der Bossbestand umfasst **32 Bossdefinitionen**.

### Biome-Bosse

Der Code enthält 26 Biome-Bosse, darunter:

- Der Plünderer
- Die Bienenkönigin
- Die Hexe des Waldes
- Das Knarzende Herz
- Der Dschungelwächter
- Die Sumpfhexe
- Der Husk-König
- Der Ravager-Häuptling
- Der Sandstein-Koloss
- Der Frostwolf
- Der Streuner-Krieger
- Der Bergbock
- Der wilde Bergbock
- Der Blütenwächter
- Der Ertrunkene Kapitän
- Der Flusswächter
- Der Tiefenwächter
- Der Myzelkönig
- Der Höhlenjäger
- Der Uralte Wächter
- Der Netherfürst
- Die Karmesinbestie
- Der Endermanfürst
- Der Seelenfürst
- Der Magmakoloss
- Der Endkönig

### World-Bosse

- Der Risskoloss
- Der Sturmherrscher
- Der Abgrundfürst
- Der Seelenverschlinger
- Der Endbote
- Der Uralte Weltenwächter

Bossmechaniken:

- Level
- HP Scaling
- Damage Scaling
- Phasen
- Attack Patterns
- Summons
- Enrage
- Projectile Volley
- Slam
- Damage Contribution
- Boss Bar
- Loot
- XP
- Geld
- Custom Boss Items
- World-Boss-Schutz

---

## 13. Boss-Rewards

`boss-reward-items.json`: **32 Custom Items**

Sie dienen als:

- Boss-Loot
- Progressionsmaterial
- Crafting-Kosten

Im aktuellen Content werden Custom-Boss-Items auch als Kosten in Crafting-Rezepten verwendet.

---

## 14. Companions

`companions.json`: **28 Definitionen**

Das System besitzt:

- Registry
- Definition
- Rarity
- Level
- XP
- Stats
- Follow
- Combat
- Passive Stats
- Equipment
- Mounts
- Rename
- Quest Unlocks
- Boss Unlocks
- Runtime Entities
- Skins
- Persistenz

---

## 15. Combat

Vorhanden:

- RPG Damage
- Combat State
- Combat Timeout
- Mob Scaling
- Player Parity
- XP
- Loot
- Weapon Abilities
- Skill Input
- Cooldowns
- Boss Damage Contribution
- Boss Damage Limits
- Mob Nameplates
- Soulbound
- Death Handling

Weapon Abilities:

- IRON_BASTION
- GOLDEN_FLURRY
- DIAMOND_CLEAVE
- NETHERITE_ERUPTION
- FIREBALL
- ADMIN_RELIC

---

## 16. Economy / Bank

Vorhanden:

- Spieler-Geld
- Bank
- Banker-NPC
- Einzahlung
- Auszahlung
- Quest-Rewards
- Mob-/Boss-Rewards
- Shop Buy/Sell
- Trade Depot
- Guild Currency
- Guild Bank

Aktuelle Konfiguration:

- Currency Max Stack Size: 99
- Loot Item Drop Chance: 0.60
- Loot Currency Drop Chance: 0.20
- Currency Drop: 1–8

---

## 17. Party

Vorhanden:

- Party-Erstellung
- Leader
- Invite
- Accept
- Leave
- Kick
- Transfer
- Disband
- Info
- Disconnect Handling
- Quest Sharing
- Party API
- native Dialoge

Share Range:

`24.0`

### Integrationsbefund

`PartySubCommand` enthält weiterhin einen Pfad zur nicht vorhandenen `PartyGUI`.

Die aktuelle Party-Architektur besitzt gleichzeitig Dialogsysteme.

Das ist eine technische Restinkonsistenz und muss bereinigt werden.

---

## 18. Guild

Vorhanden:

- Create
- Invite
- Accept
- Leave
- Info
- Disband
- Guild Bank
- Guild Storage
- Guild Currency
- Guild Compass
- Guild API
- Persistenz

---

## 19. Trade Depot

Vorhanden:

- Listings
- Verkauf
- Kauf
- Preise
- Ablauf
- Pending Payout
- Inventarprüfung
- Persistenz
- GUI

Listing-Dauer:

**7 Tage**

Verkaufsgebühr:

**5 %**

---

## 20. Regionen

Regiontypen:

- WILDERNESS
- VILLAGE
- CITY
- GUILD_CITY
- RUINS
- FORTRESS
- DUNGEON
- DANGER_ZONE
- BOSS_ZONE
- OTHER

Vorhanden:

- Geometry
- Flags
- Policy
- Editor
- Spawn Points
- Spawn Service
- Transitions
- Repository

Konkrete Weltregionen sind kein vollständiger statischer Repository-Weltbestand.

---

## 21. Statistics / Scoreboard / Playtime

Vorhanden:

- Mob Kill Statistics
- Death Statistics
- Boss Statistics
- Quest/Boss Statistics
- Scoreboard
- Playtime
- Autosave

Scoreboard:

**20 Ticks**

Statistics Autosave:

**6000 Ticks**

Die Scoreboard-Komponente übernimmt außerdem die Vanilla-EXP-Bar über `Player#setExp` und `Player#setLevel`.

---

## 22. Resourcepack

Repository enthält:

- `pack.mcmeta`
- PixelRPG Item Models
- Food Models
- Food Textures
- Item Model-Struktur
- README

Für die Food-Definitionen sind entsprechende Model-Dateien vorhanden.

Der vollständige visuelle Ausbau aller RPG-Items ist noch nicht als abgeschlossen anzusehen.

---

## 23. Commands

Registrierte Root-Kommandos:

- `/pixelrpg`
- `/pixelrpgparty`
- `/pixelrpgquestlog`
- `/pixelrpgdialogue`

Zentraler `/pixelrpg`-Baum:

- item
- player
- debug
- party
- questlog
- dialogue
- guild
- companion
- npc
- shop
- quest
- boss
- region
- edit

### Party

- invite
- accept
- leave
- kick
- transfer
- disband
- info

### Guild

- create
- invite
- accept
- leave
- info
- disband

### Player Admin

- info
- set-level
- set-xp
- set-gold
- set-profession
- reset

### Item Admin

- list
- give
- create
- inspect
- set

### Quest Admin

- reload
- list
- give
- remove
- reset
- progress
- complete

### Region

- info
- flags
- member
- owner
- create
- finish
- confirm
- cancel
- delete
- edit
- list

---

## 24. Permissions

`paper-plugin.yml`:

### rpg.admin

Default:

`op`

### rpg.member

Default:

`true`

Player-facing Commands können zusätzlich eigene Permission-Definitionen besitzen; Subcommands mit `null` sind bewusst ohne zusätzliche Permission.

---

## 25. Konfigurationsoberfläche

### Storage

- type
- save-executor-threads
- load-timeout-seconds
- MySQL host
- port
- database
- username
- password
- pool-size
- connection-timeout-ms
- ssl-mode

### Items/Loot

- soulbound cost
- soulbound min-level
- item drop chance
- currency drop chance
- currency min/max

### Economy

- bank quick amount
- currency max stack size

### Combat

- boss max hit percent

### Story

- chars per line
- lines per page

### Mob Scaling

- nameplate duration
- XP per max health
- damage per level
- HP per level
- player parity multiplier
- dimension multipliers
- dimension offsets
- dimension base levels

### NPC

- look radius
- nameplate radius
- look interval

### Quests

- party share range

### Bosses

- bar radius
- bar update interval
- phase check interval
- spawn radius
- spawn check interval
- max concurrent

### Statistics

- autosave interval

### Scoreboard

- update interval

---

## 26. Datenquellen

Im Repository vorhanden:

- item-definitions.json
- equipment-sets.json
- food-definitions.json
- boss-reward-items.json
- companions.json
- item-scaling.json
- mob-scaling.json
- unique-items.json
- crafting-recipes.json
- Quest-Dateien
- story_campaign.yml

Runtime-Daten:

- npcs.yml
- shops.yml
- bosses.yml
- Spielerprofile
- Companion-Daten
- Guild-Daten
- Party-Zustände
- weitere Managerdaten

---

## 27. Content-Lücken

### Kritisch

1. Repository-Default für NPC-Weltpopulation fehlt.
2. Repository-Default für Shops fehlt.
3. PartyGUI-Referenz muss auf das aktuelle Dialogsystem bereinigt werden.

### Mittel

4. Questdaten benötigen vollständige Welt-/NPC-Anbindung.
5. Storydaten benötigen vollständige Story-NPC-/Structure-Weltanbindung.
6. Erwerbswege aller 23 Food-Definitionen sollten pro Item eindeutig dokumentiert sein.
7. Regionengine besitzt noch keinen vollständigen statischen Weltregionsbestand.
8. Resourcepack-Ausbau aller RPG-Items ist noch nicht vollständig.

---

## 28. Forensisches Content-Fazit

Der Branch besitzt bereits einen großen, strukturierten Contentkern. Die wesentliche offene Arbeit liegt weniger in der Existenz einzelner RPG-Systeme als in der **durchgängigen Verbindung**

```
Content-Daten
    ↓
Registry
    ↓
Runtime-Service
    ↓
NPC / Quest / GUI / Dialog
    ↓
Weltposition
    ↓
Spielerprogression
```

Die größten nachweisbaren Content-Integrationslücken sind derzeit:

- NPC-Weltpopulation
- Shop-Weltpopulation
- PartyGUI-Rest
- konkrete Weltregionen
- vollständige Weltanbindung der Story-/Questdaten

Dieses Dokument bewertet ausschließlich den tatsächlichen Stand des Branches `test`.
