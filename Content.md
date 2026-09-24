# 0. Überarbeitung 2026-09-24

Nach der aktuellen Dialogue-/NPC-Überarbeitung wurde der Contentbestand erneut geprüft:

- Berufsquests wurden vollständig entfernt; es existieren keine separaten Berufsquest-Definitionen mehr.
- Die Questanzahl beträgt dadurch **149** statt 293.
- Das native Dialogsystem besitzt weiterhin eine klare Hierarchie: oberste Menüs verwenden **Schließen**, Untermenüs verwenden **Zurück**.
- DialogTree-Navigation führt bei verschachtelten Nodes zum vorherigen Node zurück.
- Admin-platzierte NPCs erhalten beim ersten Besuch eine persistente, typabhängige Begrüßungs-/Situationsdialogspur mit variierenden Optionen und levelabhängigem Text.
- FILLER-NPCs bleiben bewusst von dieser zusätzlichen Dialogschicht ausgenommen.
- NPC-Interaktionen behalten ihre bestehenden fachlichen Behaviors; der neue Dialoglayer ersetzt diese nicht, sondern führt sie über die gewählte Option aus.

# PixelRPG – Forensisches Content- & Feature-Audit

**Prüfobjekt:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `test`  
**Commit/HEAD:** `1c5b546af6a23829d258d4c9d9e390f8d2897e4e`  
**Prüfstand:** 24.09.2026  
**Prüfmethode:** statische Analyse des tatsächlich im Branch `test` vorhandenen Quellcodes und der mitgelieferten `src/main/resources`-Daten.  
**Abgrenzung:** `main` wurde für diesen Audit nicht als Begründung für Content-Befunde verwendet. Commit-Absichten und historische Annahmen wurden nicht als Evidenz verwendet.

---

# 1. Audit-Ergebnis in Kurzform

Der Branch enthält einen umfangreichen RPG-Kern mit:

- Spielerprofil, Level/XP und Registrierung
- Quests und Story-Kampagne
- neun Berufen
- 329 Crafting-Rezepten
- 30 Itemdefinitionen
- 23 Food-Definitionen
- 6 vollständigen Equipment-Sets
- 32 Boss-Reward-Items
- 32 Bossdefinitionen: 26 Biome-Bosse + 6 World-Bosse
- 28 Companions
- NPC-/Mannequin-System
- Dialogsystem auf Basis der aktuellen Paper-Dialog-API
- Party- und Guild-System
- Bank/Economy
- Shop und Trade Depot
- Combat, Mob Scaling, Weapon Abilities
- Regionen
- Scoreboard/Statistics/Playtime
- Resourcepack-Grundlagen
- Persistenz über YAML bzw. MySQL

Die größten aktuellen Befunde sind nicht fehlende RPG-Systeme, sondern die **Anbindung des vorhandenen Contents an einen frisch gestarteten Server** und ein **konkreter Integrationsfehler im Party-Code**.

---

# 2. Kritische Befunde

## 🔴 C-01 – PartySubCommand referenziert eine im Branch nicht vorhandene Klasse

`src/main/java/de/pixelrpg/rpg/command/impl/PartySubCommand.java` enthält weiterhin:

`new PartyGUI(...)`

Die Datei `src/main/java/de/pixelrpg/rpg/gui/PartyGUI.java` ist im aktuellen Branch nicht vorhanden.

Damit besteht im untersuchten Stand eine direkte Inkonsistenz zwischen:

- entferntem Legacy-Party-GUI
- aktuellem Dialogsystem
- weiterhin vorhandenem `info`-Pfad im Party-Command

**Auswirkung:** Der Branch ist an dieser Stelle nicht konsistent kompilierbar, sofern dieser Pfad nicht anderweitig aufgelöst wird. Der statische Dateibestand enthält keine `PartyGUI.java`.

**Content-Relevanz:** Die gewünschte vollständige Umstellung der Party auf das Dialogsystem ist dadurch noch nicht lückenlos abgeschlossen.

---

## 🔴 C-02 – NPC- und Shop-Content wird nicht als Repository-Default mitgeliefert

Der NPC-Code lädt:

`<plugin-data-folder>/npcs.yml`

Wenn die Datei nicht existiert, beendet `NpcManager.loadAll()` den Ladevorgang ohne NPCs.

Der Shop-Code lädt:

`<plugin-data-folder>/shops.yml`

Wenn die Datei nicht existiert, bleiben die Shopdaten leer.

Im untersuchten `src/main/resources`-Bestand existieren keine Default-Dateien `npcs.yml` oder `shops.yml`.

Damit gilt:

> NPC-/Shop-Technik vorhanden ≠ frischer Server besitzt automatisch eine vollständig befüllte RPG-Welt.

Die Boss-Implementierung verhält sich anders: `BossRepository` erzeugt bei fehlender `bosses.yml` einen vollständigen Default-Bestand und ergänzt World-Boss-Defaults.

---

## 🟠 C-03 – Custom-Item-Progression ist vorhanden und inzwischen an Crafting angebunden, aber nicht jeder Item-/Food-Bestand ist gleichwertig integriert

Die 329 Rezepte enthalten **29** `resultItemId`-Rezepte. Die `CraftingRecipeRegistry` validiert diese gegen `item-definitions.json`.

Damit sind die normalen 29 spielbaren Custom-Items grundsätzlich über Crafting produzierbar.

Die 29 sind:

- 4 Custom-Waffen:
  - Eisenschwert
  - Goldklinge
  - Diamantklinge
  - Netheritklinge
- 24 Rüstungsteile:
  - 6 komplette Sets mit je 4 Teilen
- 1 zusätzliche normale Waffe:
  - Feuerball

Zusätzlich existiert 1 Admin-/Unique-Relikt.

**Wichtig:** Die 23 Food-Definitionen liegen separat in `food-definitions.json`. Die aktuelle Rezeptdatei besitzt dagegen 329 Rezepte, deren überprüfte Custom-`resultItemId`-Produktion die 29 Itemdefinitionen abdeckt. Die Food-Definitionen sind daher als eigener Content-Katalog zu behandeln; sie sind nicht Teil der 30 `item-definitions.json`-Einträge.

---

# 3. Spielerprogression

## 🟢 Level

Die Levelklasse implementiert:

- Level 1–99 als normale Progression
- Level 100 als reserviertes Transzendenz-Level
- binäre XP-Levelbestimmung
- XP für aktuelles/nächstes Level
- XP bis zur nächsten Stufe
- separate extreme Transzendenzschwelle

## 🟢 Player Profile

Das Profil enthält unter anderem:

- UUID
- Registrierung
- Level/XP
- Geld
- Waypoints
- Story-Fortschritt
- aktive Quests
- abgeschlossene Quests
- Scoreboard-Einstellungen
- Party-HUD-Einstellung
- Quest-Tracker-Einstellung
- Spielzeit
- Equipment
- Berufslevel
- erlernte Berufe
- Companion-Fortschritt
- Persistenzrevision

Persistenz ist für YAML und MySQL vorgesehen.

---

# 4. Items

## Bestand

`item-definitions.json`: **30 Einträge**

### Normale Waffen

| Item | Level | Rarity |
|---|---:|---|
| Eisenschwert | 3 | COMMON |
| Goldklinge | 22 | UNCOMMON |
| Diamantklinge | 34 | RARE |
| Netheritklinge | 82 | LEGENDARY |
| Feuerball | 35 | RARE |

### Rüstung

| Set | Level | Teile |
|---|---:|---:|
| Donnerwacht | 10 | 4 |
| Schattengeflecht | 20 | 4 |
| Stahlwall | 30 | 4 |
| Sonnengewand | 45 | 4 |
| Kristallwache | 60 | 4 |
| Höllenschmiede | 80 | 4 |

### Admin/Unique

- Admin-Relikt
- Level 99
- MACE
- UNIQUE
- soulbound
- adminOnly
- nicht als normales Rezept zugelassen

## Item-Mechaniken

Der Code enthält:

- Item IDs
- Rarity
- Required Level
- Item Level
- Gearscore
- RPG Stats
- Soulbound
- Unique
- Admin-only
- Equipment Slots
- Set IDs
- Weapon Abilities
- Resourcepack IDs
- PDC-basierte Identifikation
- Item-Erzeugung
- Item-Inspektion
- Admin-Tuning

Rarities:

- COMMON
- UNCOMMON
- RARE
- EPIC
- LEGENDARY
- UNIQUE

UNIQUE wird nicht zufällig gerollt.

---

# 5. Equipment-Sets

Es existieren **6 vollständige Sets**:

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
- Armor Trim
- Set-ID

Enthaltene Stats sind unter anderem:

- Movement Speed
- Reach
- Attack Power
- Crit
- Crit Damage
- Lifesteal
- Armor
- HP

Die Sets sind somit nicht nur kosmetische Definitionen.

---

# 6. Food

`food-definitions.json`: **23 Definitionen**

Der Bestand umfasst unter anderem:

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
- Kakao
- Getränke
- Marmelade
- Kuchen
- Ei-/Speck-Gerichte
- Käse

Implementiert sind:

- Nutrition
- Saturation
- Custom Item ID
- Rarity
- Resourcepack ID
- Food-Service
- Consumable-Daten
- Essen/Trinken

---

# 7. Crafting

`crafting-recipes.json`: **329 Rezepte**

Aktive Crafting-Berufe:

1. BLACKSMITH
2. SCHOLAR
3. FARMER
4. COOK
5. TAILOR
6. ALCHEMIST
7. MASON

Passiv:

- FISHERMAN
- WOODCUTTER

Die Rezeptregistry unterstützt:

- Vanilla-Material-Ergebnisse
- PixelRPG-`resultItemId`
- Materialkosten
- Custom-Item-Kosten
- Beruf
- Kategorie
- benötigtes Berufslevel
- Unlock-Preis
- Quest-Voraussetzung
- Default-Unlock
- Rarity
- Tränke
- Verzauberungsbücher
- Dependency-Prüfung

Die Registry prüft außerdem:

- unbekannte Materialien
- ungültige Zahlenwerte
- unbekannte Custom-Items
- Admin-/Unique-Items als Rezeptbestandteil
- falsches Result-Material
- ungültige Berufe
- ungültige Levels
- ungültige Mengen
- ungültige Potion-Typen
- ungültige Verzauberungen
- Überschreitung des Vanilla-Maximallevels
- zirkuläre Custom-Item-Rezeptabhängigkeiten

---

# 8. Berufe

Es gibt **9 Berufe**, alle mit Levelbereich 1–100:

### Aktiv

- Schmied
- Gelehrter
- Landwirt
- Koch
- Schneider
- Alchemist
- Steinmetz

### Passiv

- Fischer
- Holzfäller

Das Berufssystem besitzt:

- Profession Service
- Level
- XP/Fortschritt
- Trainer-NPCs
- Dialoge
- Crafting
- Aktivitätslistener
- Freischaltungen

---

# 9. Quests

Aktuelle Quelldateien:

| Quelle | Definitionen |
|---|---:|
| Crafting Orders | 60 |
| Additional | 84 |
| Content Expansion 01 | 46 |
| Expansion 02 | 19 |
| Story | 19 |
| V2 | 36 |
| World Expansion | 29 |
| **Gesamt** | **149** |

Questtypen:

- HUNT
- COLLECT
- TALK_TO_NPC
- REACH_LOCATION
- GLOBAL_EVENT

Unterstützte Questlogik umfasst:

- Voraussetzungen
- Follow-ups
- Levelanforderungen
- Berufsanforderungen
- Rewards
- Companion-Unlocks
- Party-Quest-Sharing
- Quest-Tracker
- Quest-Passivprüfung
- Mob-Kills
- NPC-Interaktion
- Locations
- Global Events
- Boss-/Statistik-Verknüpfungen

## Quest-NPC-GUI

Der Quest-NPC verwendet feste Levelbereiche:

- 1–10
- 11–20
- 21–30
- 31–40
- 41–50
- 51–60
- 61–70
- 71–80
- 81–90
- 91–99

Die Kategorien werden anhand der Quest-Level gefiltert.

---

# 10. Story-Kampagne

`story_campaign.yml` enthält:

- Story-Version 7
- Kampagnen-ID `minecraft-lore-campaign`
- maximal 64 Kapitel
- 20 lineare Hauptkampagnen-Positionen
- Orders 0–19
- 20 NPC-IDs
- 20 Structure-Trigger

Progression:

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
16. Bastion
17. Stronghold
18. End
19. End City
20. After the End

Story-Mechaniken:

- Kapitel
- Arcs
- Levelanforderungen
- Structure-Trigger
- NPC-Zuordnung
- Quests
- Dialogseiten
- XP-Rewards
- Story-Fortschritt
- Story-NPC-Sichtbarkeit
- Story-Trigger

---

# 11. Dialogsystem

Der Branch enthält einen zentralen `DialogueEngine` sowie mehrere native Dialogdienste.

Vorhandene Bereiche umfassen unter anderem:

- Reception
- Quest
- Story
- Banker
- Profession
- Party
- Guild
- Companion
- Quick Actions
- Region-Editor
- Invite Dialogs

Die aktuelle Party-/Guild-Einladung verwendet native Paper-Dialoge und UUID-basierte Zustände.

Das System unterstützt außerdem:

- Dialog-Buttons
- Zurück-Navigation
- Input-Felder
- Spielername-/Prefix-Suche
- Accept/Decline
- Combat-Deferred Invitations
- Revalidierung bei Annahme

Es wird kein TAB-Autocomplete für die Dialog-Eingabe benötigt; die Dialogsuche verwendet Prefix-/Namensauflösung.

---

# 12. NPC-System

`NpcType` enthält:

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
- Sichtbarkeit
- Nameplate/Name
- Skin-Quelle
- persistierte Skin-Textur
- Profession-Zuordnung
- Chunk Load/Unload
- Player Resync
- Story-NPC-Sichtbarkeit
- Behavior Registry

### Content-Befund

Die Runtime erwartet `npcs.yml` im Plugin-Datenordner.

Eine Repository-Default-`npcs.yml` ist nicht vorhanden.

Damit ist die NPC-Technik vollständig angelegt, die Weltpopulation aber nicht als Resource-Default mitgeliefert.

---

# 13. Shops

Technik:

- ShopManager
- ShopEntry
- Buy-Preis
- Sell-Preis
- NPC-Zuordnung
- ShopGUI
- ShopEditorGUI
- YAML-Persistenz
- Migration alter Preisformate
- ItemStack-Serialisierung

### Content-Befund

`shops.yml` wird aus dem Plugin-Datenordner geladen.

Eine Repository-Default-`shops.yml` ist nicht vorhanden.

Ein frischer Server hat daher kein fest definiertes Shop-Sortiment aus dem Repository.

---

# 14. Bosse

Der BossRepository erzeugt bei fehlender `bosses.yml` einen Default-Bestand.

## Biome-Bosse

Im Code sind **26 Biome-Bosse** definiert, unter anderem:

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

## World-Bosse

6 World-Bosse:

- Der Risskoloss
- Der Sturmherrscher
- Der Abgrundfürst
- Der Seelenverschlinger
- Der Endbote
- Der Uralte Weltenwächter

## Bossmechaniken

- Level
- HP-/Damage-Multiplikatoren
- Scale
- Attack Intervals
- Attack Patterns
- Phasen bei World-Bossen
- Announcements
- Summons
- Enrage
- Projectile Volley
- Slam
- Damage Contribution
- Boss-Bar
- Loot
- Geld
- XP
- Custom Boss Items
- World-Boss-Schutz

---

# 15. Boss Rewards

`boss-reward-items.json`: **32 Custom-Boss-Items**

Die IDs reichen von:

- `pixelrpg:boss/pluenderer_siegel`
- über biomebezogene Materialien
- bis `pixelrpg:boss/weltenherz`

Die Items dienen außerdem als Kosten in der Custom-Progression.

Aktuell verwenden **14 Crafting-Rezepte** Custom-Boss-Items als `itemCosts`.

---

# 16. Companions

`companions.json`: **28 Companion-Definitionen**

Das System enthält:

- Companion Registry
- Definitionen
- Rarity
- Level
- XP
- Stats
- Follow
- Combat
- passive Stats
- Equipment
- Mounts
- Rename
- Quest Unlocks
- Boss Unlock
- Default Companion
- Admin Companion
- persistente Companion-Daten
- Skins
- Runtime Entities

---

# 17. Combat

Implementiert:

- RPG-Damage
- Combat State
- Combat Timeout
- Mob Scaling
- Player-Parity
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
- Loot Drops

Weapon Abilities sind unter anderem:

- IRON_BASTION
- GOLDEN_FLURRY
- DIAMOND_CLEAVE
- NETHERITE_ERUPTION
- FIREBALL
- ADMIN_RELIC

---

# 18. Economy / Bank

Implementiert:

- Spieler-Geld
- Bank
- Banker-NPC
- Einzahlung
- Auszahlung
- persistentes Geld
- Quest-Rewards
- Mob-/Boss-Rewards
- Shop Buy/Sell
- Trade Depot
- Guild Currency
- Guild Bank

Aktuelle Config:

`economy.currency.max-stack-size: 99`

Loot-Config:

- Item Drop Chance: 0.60
- Currency Drop Chance: 0.20
- Currency Drop: 1–8

---

# 19. Party

Implementiert:

- Party-Erstellung
- Leader
- Mitglieder
- Invite
- Accept
- Leave
- Kick
- Leadership Transfer
- Disband
- Party Full
- Disconnect Handling
- Quest Sharing
- Share Range
- Party API
- native Dialogsystem

Party-Quest-Share-Range:

`quests.party-share-range: 24.0`

### Aktueller Integrationsfehler

Der normale Party-Command besitzt weiterhin einen `info`-Pfad, der `PartyGUI` instanziiert, obwohl diese Klasse im Branch nicht vorhanden ist.

Das ist ein konkreter Rest des alten GUI-Systems und muss bei der nächsten technischen Bereinigung entfernt bzw. auf den neuen Dialogpfad umgestellt werden.

---

# 20. Guild

Implementiert:

- Guild-Erstellung
- Guild-Name-Validierung
- Guild-Mitglieder
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

Guild-World-Content wie konkrete Guild Cities ist im Repository nicht als fertige Weltpopulation enthalten.

---

# 21. Trade Depot

Implementiert:

- Listings
- Verkauf
- Kauf
- Buy/Sell-Prozess
- Preis
- Ablauf
- Pending Payout
- Inventarprüfung
- Persistenz
- GUI

Das System ist spielergetrieben; ein statischer Warenkatalog ist nicht erforderlich.

---

# 22. Regionen

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

Implementiert:

- RegionManager
- Repository
- Geometry
- Flags
- Editor
- Policies
- Spawn Points
- Spawn Service
- Transitions
- Listener

### Content-Status

Die Regionengine existiert, aber konkrete Weltregionen sind nicht als vollständiger Repository-Weltbestand definiert.

---

# 23. Statistics / Scoreboard / Playtime

Implementiert:

- Mob Kill Statistics
- Death Statistics
- Boss Statistics
- Quest/Boss Statistics
- Scoreboard
- Scoreboard Update Task
- Playtime Tracking
- Autosave

Config:

- Scoreboard: 20 Ticks
- Statistics Autosave: 6000 Ticks

---

# 24. Resourcepack

Repository enthält eine Resourcepack-Struktur mit:

- `pack.mcmeta`
- PixelRPG Item Models
- Food Models
- Food Textures
- Item Model-Struktur
- Resourcepack README

Für die 23 Food-Definitionen existieren entsprechende Model-Dateien.

Der Resourcepack-Ausbau für den vollständigen RPG-Itembestand ist noch nicht vollständig.

---

# 25. Commands

Der Plugin-Hauptcommand wird über Paper Lifecycle Commands registriert.

Im Plugin werden registriert:

- `/pixelrpg`
- `/pixelrpgparty`
- `/pixelrpgquestlog`
- `/pixelrpgdialogue`

Der zentrale `/pixelrpg`-Baum enthält im aktuellen Code unter anderem:

- `item`
- `player`
- `debug`
- `party`
- `questlog`
- `dialogue`
- `guild`
- `companion`
- `npc`
- `shop`
- `questadmin`
- `boss`
- `region`
- `edit`

Weitere player-facing Guild-/Party-Funktionen sind innerhalb der jeweiligen Command-Bäume als Subcommands implementiert.

Beispiele aus dem aktuellen Code:

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

### Player/Admin

- info
- set-level
- set-xp
- set-gold
- set-profession
- reset

### Item/Admin

- list
- give
- create
- inspect
- set

### NPC/Admin

- Verwaltung/Erstellung
- Spawn
- Edit-Funktionen

### Quest/Boss/Region

Administrative Unterbefehle für Quest-, Boss- und Regionverwaltung sind im Code vorhanden.

---

# 26. Permissions

`paper-plugin.yml` definiert:

## `rpg.admin`

- Beschreibung: administrative PixelRPG commands
- Default: `op`

Wird für administrative Subcommands verwendet, darunter Item-/Player-Verwaltung.

## `rpg.member`

- Beschreibung: registrierte Spieler-Commands
- Default: `true`

Wird für player-facing PixelRPG-Commands wie Questlog/Dialog verwendet.

Mehrere Subcommands besitzen explizit `permission() == null` und werden damit nicht zusätzlich über eine eigene Permission eingeschränkt.

---

# 27. Konfiguration

## `config.yml`

### Storage

- `storage.type`
- `storage.save-executor-threads`
- `storage.load-timeout-seconds`
- MySQL host
- port
- database
- username
- password
- pool-size
- connection-timeout-ms
- ssl-mode

### Items/Loot

- Soulbound Cost
- Soulbound Minimum Level
- Item Drop Chance
- Currency Drop Chance
- Currency Drop Min
- Currency Drop Max

### Economy

- Bank Quick Amount
- Currency Max Stack Size

### Combat

- Boss Max Hit Percent of Max HP

### Story

- Characters per line
- Lines per page

### Mob Scaling

- Nameplate duration
- XP per max health
- Damage per level
- HP per level
- Player parity multiplier
- Dimension multipliers
- Dimension level bases/offsets

### NPC

- Look Radius
- Nameplate Radius
- Look Interval

### Quests

- Party Share Range

### Bosses

- Bar Radius
- Bar Update Interval
- Phase Check Interval
- Biome Spawn Radius
- Biome Spawn Check Interval
- Max Concurrent Biome Bosses

### Statistics

- Autosave Interval

### Scoreboard

- Update Interval

---

# 28. Content-Datenformate

Im Repository vorhandene zentrale Daten:

- `item-definitions.json`
- `equipment-sets.json`
- `food-definitions.json`
- `boss-reward-items.json`
- `companions.json`
- `item-scaling.json`
- `mob-scaling.json`
- `unique-items.json`
- `crafting-recipes.json`
- Quest-Dateien
- `story_campaign.yml`

Runtime-generierte/persistente Daten:

- `npcs.yml`
- `shops.yml`
- `bosses.yml`
- Spielerprofile
- Companion-Daten
- Guild-Daten
- Party-Zustände
- weitere Managerdaten

---

# 29. Forensische Validierung des Contents

## 🟢 Stark validiert

### Crafting

Die Rezeptregistry validiert strukturell:

- Pflichtfelder
- IDs
- Duplikate
- Beruf
- Kategorie
- Material
- Rarity
- Kosten
- Custom-Item-Kosten
- ResultItem
- Level
- Preis
- Mengen
- Potion
- Enchantment
- Dependency-Zyklen

### Daten-IDs

Custom-Rezept-Resultate werden gegen die ItemDefinitionRegistry geprüft.

### Bossdaten

BossRepository validiert bzw. verwirft:

- unbekannte Biome
- fehlende Biome bei Biome-Bossen
- ungültige Phasen
- fehlende Phasen bei World-Bossen
- doppelte Biome-Zuordnungen

### MySQL-Konfiguration

DatabaseManager validiert:

- Host
- Port
- Datenbank-Identifier
- Poolgröße
- Connection Timeout
- SSL-Modus

Der SSL-Modus ist auf eine definierte Whitelist beschränkt.

---

# 30. Spieler-Input-Validierung

Im Command-/Service-Code sind mehrere konkrete Prüfungen vorhanden.

Beispiele:

- Spieler muss online sein
- Spieler muss registriert sein
- Selbst-Einladung wird verhindert
- Party Leader wird geprüft
- volle Party wird verhindert
- UUID-basierte Zustände
- Zahlen werden geparst und geprüft
- negative XP werden abgewiesen
- negative Goldwerte werden abgewiesen
- nicht-finite Double-Werte werden abgewiesen
- Level wird auf 1–99 geprüft
- Profession wird gegen Enum geprüft
- Gildenname wird im GuildManager validiert
- Item-ID wird gegen Registry geprüft
- UNIQUE-Items werden mengenmäßig eingeschränkt
- Item-Stats werden auf finite/nichtnegative Werte geprüft

## Restbefund

Nicht jede Runtime-Eingabe wird in einer zentralen Schema-Schicht validiert. Ein großer Teil der Validierung erfolgt unmittelbar in den jeweiligen Managern/Registries.

Für Content-Daten ist das bei Crafting/Bosses bereits relativ streng; für YAML-basierte Welt-/Shopdaten ist die Validierung teilweise permissiver.

---

# 31. Performance- und I/O-Forensik

## 🟢 Positiv

### Player Profile

Player-I/O besitzt einen separaten Executor:

`PixelRPG-ProfileIO`

Speichern erfolgt asynchron.

### NPC Persistence

NPC-Snapshots werden auf dem Serverthread erfasst und anschließend über:

`PixelRPG-NpcIO`

geschrieben.

Das vermeidet YAML-Schreib-I/O innerhalb des eigentlichen Snapshot-Schritts.

### Companion Persistence

Companion-Speicherung verwendet einen eigenen I/O-Executor.

### MySQL

HikariCP wird verwendet.

Zusätzlich vorhanden:

- Connection Pool
- Prepared Statements
- Connection Timeout
- Validation Timeout
- Leak Detection
- Keepalive
- Max Lifetime

---

## 🟠 Potenzielle Hauptthread-I/O-Stellen

### ShopManager

`save()` führt `YamlConfiguration.save(file)` synchron aus.

Das betrifft:

- addEntry
- removeEntry
- replaceEntries
- shutdown
- Migration

Bei großen Shopdaten oder langsamen Storage-Systemen kann dies den Serverthread blockieren.

### Startup

Mehrere Datenquellen werden beim Plugin-Start synchron geladen:

- Config
- Itemdefinitionen
- Craftingdaten
- Questdaten
- Bossdaten
- NPC-Daten
- Shopdaten
- Storydaten

Startup-I/O ist grundsätzlich weniger kritisch als wiederkehrende Tick-I/O, sollte aber bei sehr großen Datenmengen berücksichtigt werden.

---

# 32. Laufzeitkomplexität / mögliche Skalierungsstellen

## NPC Player Resync

`NpcManager.resyncPlayer()` iteriert über alle bekannten NPCs.

Bei sehr großen NPC-Beständen entsteht:

`O(number of NPCs)`

pro Resync.

Der vorhandene Chunk-Index wird an anderen Stellen genutzt, aber der Player-Resync läuft über die gesamte NPC-Menge.

## Boss Biome Lookup

Der Repository-Zugriff auf einen Biome-Boss sucht über die vorhandenen Bossdefinitionen.

Bei 32 Bossen ist dies klein; aktuell kein relevanter Skalierungsbefund.

## Quest-/Recipe-Registries

Die Datenmengen liegen im niedrigen bis mittleren dreistelligen Bereich und werden überwiegend aus In-Memory-Strukturen bedient.

Aktuell kein offensichtlicher Speicherleck-Befund aus den statisch geprüften Registries.

---

# 33. Persistenz / Shutdown

Das Plugin besitzt einen LifecycleManager und fährt unter anderem kontrolliert herunter:

- PlayerProfileManager
- CompanionService
- PartyManager
- CombatStateService
- GuildManager
- RegionManager
- NpcManager
- BossManager
- QuestManager
- Scoreboard
- Playtime
- Mob Scaling
- Boss Spawn Task
- Quest Passive Check
- Region Editor
- Region Spawn
- Banker

Mehrere Komponenten flushen Daten vor dem Executor-Shutdown.

---

# 34. Content-Lücken

## 🔴 Priorität 0

### 1. NPC-Weltpopulation

Es fehlt eine Repository-Defaultpopulation in `npcs.yml`.

Benötigt sind insbesondere:

- Reception
- Banker
- Questgeber
- Shop-NPCs
- Reise-NPCs
- neun Berufstrainer
- Story-NPCs
- ggf. Spezial-NPCs

### 2. Shop-Weltpopulation

Es fehlt ein Repository-Default-`shops.yml`.

Benötigt:

- Händlerangebote
- NPC-Zuordnung
- Kaufpreis
- Verkaufspreis
- Progressionsstufe

### 3. Party-Code bereinigen

`PartyGUI`-Referenz entfernen/ersetzen, da die Datei im aktuellen Branch fehlt.

---

## 🟠 Priorität 1

### 4. Weltintegration der Quest-/Storydaten

Der Datenbestand ist groß genug.

Der verbleibende Content-Aufwand liegt hauptsächlich bei:

- NPC-Positionen
- Questgeber
- erreichbaren Zielgebieten
- Story-NPC-Positionen
- Reisewegen
- Weltregionen
- Quest-/Reward-Progression

### 5. Food-Erwerbswege

Für die 23 Food-Definitionen sollte der tatsächliche Spieler-Erwerbsweg eindeutig angebunden sein:

- Crafting
- NPC
- Shop
- Drop
- Quest
- oder definierter anderer Weg

### 6. Custom-Item-Progression vollständig verifizieren

Die 29 Custom-Rezeptresultate sind inzwischen technisch vorhanden. Für die finale Progression sollte trotzdem pro Item ein eindeutiger Gameplay-Weg dokumentiert werden:

- benötigtes Beruflevel
- Unlock
- Materialkosten
- Boss-Item-Kosten
- Quest-/Story-Kontext

---

# 35. Bewusst nicht als Content-Lücke gewertet

Folgende Systeme existieren technisch, sind aber als Welt-/Visualausbau nicht vollständig:

- konkrete Regionpopulation
- vollständige Guild-City-Welt
- vollständiger Resourcepack-Ausbau

Diese Punkte werden hier nicht als fehlende Kernmechanik gewertet.

---

# 36. Gesamtstatus

| Bereich | Status |
|---|---|
| Player/Level/XP | 🟢 |
| Player Persistence | 🟢 |
| Items | 🟢 |
| Equipment Sets | 🟢 |
| Food Definitions | 🟢 Daten / 🟠 Anbindung prüfen |
| Crafting | 🟢 |
| Professionen | 🟢 |
| Quests | 🟢 Datenbestand |
| Quest-Weltanbindung | 🟠 |
| Story-Kampagne | 🟢 Struktur / 🟠 Weltanbindung |
| Companions | 🟢 |
| Bosse | 🟢 |
| Boss Rewards | 🟢 |
| Combat | 🟢 |
| Party | 🟠 wegen PartyGUI-Rest |
| Guild | 🟢 Kernsystem |
| Shop-Technik | 🟢 |
| Shop-Content | 🔴 Defaultbestand fehlt |
| NPC-Technik | 🟢 |
| NPC-Content | 🔴 Defaultbestand fehlt |
| Economy/Bank | 🟢 |
| Trade Depot | 🟢 |
| Statistics | 🟢 |
| Scoreboard | 🟢 |
| Region-Technik | 🟢 |
| Region-Weltcontent | ⚪/🟠 |
| Resourcepack | 🟠 Ausbau |
| Dialogsystem | 🟢 |
| Datenvalidierung | 🟢/🟠 je nach Datenquelle |
| Runtime-I/O | 🟢 überwiegend asynchron / 🟠 Shop-Saves synchron |

---

# 37. Verifikationshinweis

Der untersuchte Branch-HEAD ist:

`1c5b546af6a23829d258d4c9d9e390f8d2897e4e`

Für diesen Commit liefert der abgefragte GitHub-Combined-Status aktuell keine Status-Checks zurück.

Daher wird in diesem Dokument **kein Build-Erfolg behauptet**.

Der statische Befund C-01 bleibt unabhängig davon bestehen: `PartySubCommand.java` referenziert `PartyGUI`, während `PartyGUI.java` im aktuellen Branch-Dateibestand fehlt.

---

# 38. Schlussbefund

Der aktuelle `test`-Stand besitzt bereits einen **umfangreichen, datengetriebenen RPG-Content-Kern**. Die großen Zahlenbereiche sind nicht mehr die Hauptbaustelle:

- 149 Questdefinitionen
- 20 Story-Kampagnenknoten
- 329 Crafting-Rezepte
- 30 Itemdefinitionen
- 23 Fooddefinitionen
- 6 Equipment-Sets
- 32 Boss-Reward-Items
- 32 Bosse
- 28 Companions
- 9 Berufe
- 16 NPC-Typen

Die entscheidenden offenen Punkte sind die **Erreichbarkeit und Weltanbindung des vorhandenen Contents** sowie die **Beseitigung des verbliebenen PartyGUI-Verweises**.

Der Audit wurde ausschließlich aus dem tatsächlich vorliegenden Branch `test` abgeleitet.
