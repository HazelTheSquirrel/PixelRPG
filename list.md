# PixelRPG – Implementierungsübersicht (IST-Zustand)

Stand: 20.09.2026  
Branch: `test`  
Referenz: `main`

> Statischer Repository-Audit. „Implementiert“ bedeutet: Code-/Datenbestand ist im Repository vorhanden. Das ist nicht gleichbedeutend mit vollständig ingame getestet, final gebalanced oder release-ready.

## Gesamtstatus

- `test` ist **48 Commits vor `main`**, 0 Commits dahinter.
- **247 Java-Dateien**
- **26 Hauptbereiche/Packages**
- **15 JSON-Datendateien**
- Paper 26.2 / Java 25 / paper-plugin.yml / Paperweight / Shadow sind vorhanden.
- Der Projektstand ist klar über dem Grundgerüst: der Schwerpunkt verschiebt sich zunehmend von Systemimplementierung zu Content, Weltbefüllung, Balancing und Tests.

## Legende

- 🟢 **FERTIG / CONTENT VORHANDEN** – konkreter Bestand vorhanden; nur finale Tests/Balancing können noch offen sein.
- 🟡 **FAST FERTIG** – System weitgehend vorhanden, aber Content-/World-/Balance-Lücken.
- 🔴 **OFFEN / NICHT FERTIG** – wesentlicher geplanter Content fehlt.
- ⚪ **VALIDIEREN** – vorhanden, aber tatsächliche Ingame-Funktion muss geprüft werden.

---

# 1. Kernsysteme

## 🟢 Plugin / Build
- Paper Plugin System über `paper-plugin.yml`
- Bootstrap + Hauptplugin
- Paper 26.2 API
- Java 25
- Paperweight 2.0.0-beta.21
- Shadow 9.6.1
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0
- Dependency Relocations
- Source-/Shaded-Dependency-Verifikation
- JDBC-Service-Verifikation
- `check` führt die Verifikationen aus

## 🟢 Player / Persistenz
- PlayerProfile
- PlayerProfileManager
- YAML-Repository
- MySQL-Repository
- DatabaseManager
- StorageType
- Player Lifecycle
- Level / Experience
- Story-Fortschritt
- Quest-Fortschritt
- Profession-Fortschritt
- Companion-Fortschritt
- Playtime

---

# 2. Items / Equipment / Food

## 🟢 Items – 29 Definitionen
`src/main/resources/data/item-definitions.json`

Vorhanden:
- Waffen
- Rüstung
- Rarity
- Item-Level
- Required-Level
- Gearscore
- Soulbound
- Unique
- Admin-only
- Weapon Abilities
- Equipment Slots
- Set-Zuordnung
- Resourcepack IDs

Beispiele: Eisenschwert, Goldklinge, Diamantklinge, Netheritklinge, Donnerwacht, Schattengeflecht, Stahlwall, Sonnengewand, Kristallwache, Höllenschmiede.

## 🟢 Equipment – 6 Sets
`equipment-sets.json`

- EquipmentService
- EquipmentSetService
- EquipmentSetEffectService
- Equipment Slots
- Level Requirements
- Set Effects
- Trim Mappings

## 🟢 Food – 23 Definitionen
`food-definitions.json`

- FoodDefinition
- FoodDefinitionRegistry
- FoodService
- Food Resourcepack Models/Definitions

## 🟢 Unique/Soulbound
- UniqueItemService
- SoulboundService
- Unique-Item Tracking

---

# 3. Crafting / Professionen

## 🟢 Crafting – 7 Crafting-Berufe / aktueller Rezeptbestand
`recipes/crafting-recipes.json`

- 7 Crafting-Berufe
- aktueller Rezeptbestand
- Vanilla-Materialien als Inputs und Outputs
- maximale Vanilla-Verzauberungsstufen für den Gelehrten
- stärkste verfügbare Vanilla-Trankstufen für den Alchemisten
- CraftRecipe
- CraftingRecipeRegistry
- CraftingCategory
- CraftingRarityRoller
- CraftingService
- CraftingGUI
- ProfessionDialog
- Rezept-/Item-Verknüpfung

Crafting-Berufe:
- Blacksmith
- Scholar
- Farmer
- Cook
- Tailor
- Alchemist
- Mason

## 🟢 Professionen – 9 gesamt
- BLACKSMITH
- SCHOLAR
- FARMER
- COOK
- TAILOR
- ALCHEMIST
- MASON
- FISHERMAN
- WOODCUTTER

Die letzten beiden sind als **passive Berufe** modelliert und brauchen daher nicht zwingend Crafting-Rezepte.

Vorhanden:
- ProfessionSystem
- ProfessionService
- ProfessionActivityListener
- Level 1–100
- NPC Trainer
- Dialog/GUIs
- Aktivitätsverarbeitung
- Crafting-Rezepte für alle 7 aktiven Berufe
- Passive Mechaniken für Fischer und Holzfäller

Status:
- Alle 9 Berufe sind im aktuellen `test`-Stand vollständig deklariert.
- Aktive Berufe verfügen über ihren vorgesehenen Crafting-Content.
- Fischer und Holzfäller sind als passive Berufe umgesetzt und benötigen keine Crafting-Rezepte.

---

# 4. Quests

## 🟢 269 Questdefinitionen

| Datei | Anzahl |
|---|---:|
| quests_crafting_orders.json | 60 |
| quests_additional.json | 84 |
| quests_content_expansion_01.json | 46 |
| quests_expansion_02.json | 19 |
| quests_v2.json | 31 |
| quests_world_expansion.json | 29 |
| **Gesamt** | **269** |

Vorhanden:
- HUNT
- COLLECT
- GLOBAL_EVENT
- TALK_TO_NPC
- REACH_LOCATION
- Prerequisites
- Follow-up Quests
- Level Requirements
- Profession Requirements
- Quest Navigation
- Quest Log
- Quest GUI
- Mob Kill Tracking
- Passive Checks
- Rewards
- Companion Unlocks
- World/Expansion Content

QuestRepository validiert IDs, Typen, Level, Ziele, Item-/Entity-Referenzen, Navigation, Voraussetzungen und Zyklen.

Offen:
- jede Questkette ingame vollständig durchspielen
- NPC-/World-Zuordnung prüfen
- Navigation prüfen
- Rewards balancen

---

# 5. Companions

## 🟢 28 Companion-Definitionen
`companions.json`

Vorhanden:
- CompanionDefinition / Registry
- CompanionService
- Runtime Registry
- Companion Instances
- Rarity
- Stats
- XP / Level
- Progression
- Follow
- Combat
- Passive Stats
- Equipment
- Mounts
- Rename
- Boss Rewards
- Quest Unlocks
- Companion Dialog

Offen:
- Passive-Verhalten geprüft: normale Companion-Definitionen sind passiv; Combat ist für sie standardmäßig deaktiviert.
- Companion-Werte werden im Runtime-Companion tatsächlich auf MAX_HEALTH / ATTACK_DAMAGE / MOVEMENT_SPEED / ARMOR usw. angewendet.
- Unique Companions verwenden kein Level-Scaling mehr und bleiben auf Level 1 / festen Werten.
- Hazel nutzt als Skin-Quelle `HazelTheSquirrel` und schaut horizontal.
- Offen: alle 28 Unlocks ingame prüfen
- Offen: Combat-/Mount-Balancing
- Offen: Equipment-Balancing

---

# 6. Combat / Skills

## 🟢 Combat Core
Vorhanden:
- Damage Calculator
- Damage Context
- Combat State
- Damage Listener
- Mob XP
- Mob Level Scaling
- Loot Drops
- Soulbound Death Handling
- Mob Nameplates
- Combat Events

## 🟢 Weapon Abilities / Skills
Vorhanden:
- WeaponAbilityEngine
- SkillInputListener
- Cooldowns
- Ability-Verknüpfung mit Items
- konkrete Weapon Abilities
- Ability-Trigger und Effekte

Beispiele:
- IRON_BASTION
- GOLDEN_FLURRY
- DIAMOND_CLEAVE
- NETHERITE_ERUPTION
- ADMIN_RELIC

Status:
- Combat-Core und Weapon-Abilities sind vollständig implementiert und als fertig deklariert.
- Weitere Anpassungen an Schaden, Cooldowns oder Effekten gehören zum Balancing.

---

# 7. Bosses

## 🟢 Boss Content
Vorhanden:
- BossDefinition
- BossRepository
- BossManager
- ActiveBoss
- Boss Phases
- Attack Pattern Registry
- Slam
- Summon Adds
- Projectile Volley
- Enrage Buff
- Damage Contribution
- Boss Death
- Boss Loot
- Boss Rewards
- Boss Bar
- Biome Spawn
- World Boss
- Boss Protection
- Boss Statistics

Aktuell im Repository:
- **26 Standard-Biome-Bossdefinitionen**
- **6 World-Bosse**
- **32 Boss-Reward-Items**

World-Bosse:
- Risskoloss
- Sturmherrscher
- Abgrundfürst
- Seelenverschlinger
- Endbote
- Uralter Weltenwächter

Offen:
- Spawn-/Difficulty-Tests
- Loot-Balance
- Phasen/Mechaniken testen
- tatsächliche Erreichbarkeit
- **Health- und Damage-Multiplikatoren wurden für den aktuellen Stand technisch um 50 % reduziert.**

---

# 8. NPCs

## 🟡 NPC-System FAST FERTIG

Vorhanden:
- NpcManager
- Persistenz
- stabile IDs
- Spawn/Despawn
- Chunk Handling
- Skins
- Nameplates
- Look Task
- Interaktion
- Behavior Registry

NPC-Typen:
- Reception
- 9 Profession Trainer
- Quest
- Shop
- Travel
- Filler
- Story
- Banker

Behaviors:
- Reception
- Profession Trainer
- Quest
- Shop
- Travel
- Story
- Banker
- Filler

Offen:
- vollständige World-Population
- konkrete Positionen
- NPC-Zuordnung
- finale Dialoge

---

# 9. Dialoge

## 🟡 FAST FERTIG

Vorhanden:
- DialogueEngine
- DialogueTree
- DialogueNode
- DialogueOption
- DialogueCondition
- DialogueProgressStore
- DialogueTreeService
- DialogueCommand

Spezialisierte Dialoge:
- Bank
- Guild
- Companion
- Profession
- Reception
- Travel
- Story
- Quick Actions

Offen:
- finale Dialogtexte
- vollständige NPC-Abdeckung
- Story-Dialoge
- UX-Polishing

---

# 10. Shops / Economy

## 🟢 Shops – FERTIG

Vorhanden:
- ShopEntry
- ShopManager
- Shop GUI
- Shop Editor GUI
- NPC-Zuordnung
- Buy/Sell Preise
- Item-Daten
- Migration alter Formate
- shops.yml Persistenz

Status:
- Shop-System und bestehende Shop-Funktionalität werden ab jetzt als **FERTIG** geführt.
- Keine weitere Shop-Implementierung geplant; spätere Preis-/Economy-Anpassungen gehören zum Balancing.

## 🟡 Economy FAST FERTIG

Vorhanden:
- Money
- Guild Currency
- Currency Item Factory
- Currency Pickup
- Item Economy Config
- Shop Buy/Sell
- Guild Bank
- Trade Depot

Offen:
- Geldquellen/-senken
- Inflation
- Preisstruktur
- Loot/Quest/Shop-Verhältnis

---

# 11. Guild / Party / Bank / Trade

## 🟢 Guild
- Guild
- GuildManager
- Guild Bank
- Guild Bank Storage
- Guild Currency
- Guild API
- Guild Commands
- Guild City-Unterstützung
- Guild Compass

## 🟢 Party
- Party
- PartyManager
- Party GUI
- Party Commands
- Disconnect Handling
- Quest Share
- Share Range
- Party API

## 🟢 Bank
- Bank Dialog
- Bank Inventory
- Bank Storage
- Banker NPC
- Guild Bank Access

## 🟢 Trade Depot
- Listings
- TradeDepotManager
- Trade Depot GUI
- Sell GUI

---

# 12. Regionen / World Rules

## ⚪ REGIONEN – PAUSIERT

Die Region-Engine ist vorhanden, die weitere Welt-/Regionen-Ausgestaltung wird bewusst pausiert.

Vorhanden:
- PixelRegion
- RegionManager
- RegionRepository
- RegionEditor
- RegionGeometry
- RegionPoint
- RegionFlags
- Flag Categories
- Flag Dialog
- RegionPolicyService
- RegionListener
- RegionSpawnService
- RegionSpawnPoint
- RegionTransitionService
- Mob Spawn Types

Region-Typen:
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

Pausiert:
- konkrete Weltregionen
- Grenzen
- Flags
- Spawnpunkte
- Städte
- Dungeons
- Boss-/Danger-Zonen
- Übergänge
- vollständige World-Population

---

# 13. Story

## 🔴 NICHT FERTIG

Technisch vorhanden:
- StoryManager
- StoryChapter
- StoryBookFactory
- Story NPC Behavior
- Story-Fortschritt im PlayerProfile
- Chapter Completion
- XP Reward

Aber der Default-Fallback erzeugt nur:

**Prologue – The Summoning**

mit 3 Dialogzeilen und 100 XP.

Bewertung:

- Story-System: 🟢 IMPLEMENTIERT
- Story-Content/Kampagne: 🔴 NICHT FERTIG

---

# 14. Scoreboard / Commands / API

## 🟢 Scoreboard
- ScoreboardService
- PlaytimeTracker
- regelmäßige Updates

## 🟢 Commands
Vorhanden:
- Root
- Item
- NPC
- Shop
- Quest Admin
- Boss
- Region
- Edit
- Companion
- Party
- Guild
- Debug
- Player Admin
- Quest Log
- Dialogue

## 🟢 API
Vorhanden:
- PixelRPGProvider
- ItemAPI
- EconomyAPI
- GuildAPI
- PartyAPI
- StatisticsAPI
- BossDefeatedEvent
- Combat Enter/Exit Events
- Player Level Up Event
- Player Registration/Unregistration Events
- Quest Completed Event

---

# 15. Resourcepack

## ⚪ RESOURCEPACK – PAUSIERT

Die weitere Resourcepack-Ausarbeitung wird bewusst pausiert.

Vorhanden:
- `resourcepack/`
- pack.mcmeta
- Food Item Models
- Food Item Definitions
- Item Models
- Textures-Struktur
- README

Pausiert:
- vollständige Abdeckung aller PixelRPG-Items
- fehlende Modelle/Texturen
- visuelle Konsistenz
- Ingame-Test

---

# 16. Kompakte Content-Bilanz

| Bereich | Bestand | Status |
|---|---:|---|
| Items | 29 | 🟢 |
| Equipment-Sets | 6 | 🟢 |
| Food | 23 | 🟢 |
| Crafting-Rezepte | aktueller Bestand | 🟢 |
| Crafting-Berufe | 7 | 🟢 |
| Passive Berufe | 2 | 🟢 |
| Professionen gesamt | 9 | 🟢 |
| Quests | 269 | 🟢 |
| Companions | 28 | 🟢 |
| Boss Reward Items | 32 | 🟢 |
| Biome-Bosse | 26 | 🟢 |
| World-Bosse | 6 | 🟢 |
| Story-Kapitel Default | 1 | 🔴 |
| NPC-Typen | 16 | 🟡 |
| Region-Typen | 10 | 🟡 |

---

# 17. Meine Arbeitsmarkierung für das Projektboard

## 🟢 Als Content-Basis abschließbar
- [ ] Items
- [ ] Equipment / Rüstungssets
- [ ] Food
- [x] Crafting
- [x] Professionen
- [ ] Quest-Content
- [x] Companion-Content
- [x] Boss-Content
- [x] Boss-Rewards

## 🟡 Noch einmal anfassen / offen
- [ ] NPC-World-Population
- [ ] Dialoge
- [ ] Economy
- [ ] Guild-City-/World-Content

## ⚪ Pausiert
- [ ] Regionen
- [ ] Resourcepack

## 🔴 Noch offen
- [ ] Story / Kampagne
- [ ] vollständige Weltbevölkerung
- [ ] finales Gesamt-Balancing
- [ ] Release-/Regressionstest

---

# 18. Wichtigste Schlussfolgerung

PixelRPG befindet sich aktuell nicht mehr primär in der Phase **„Systeme bauen“**.

Die vorhandene Architektur deckt bereits einen großen MMORPG-Kern ab.

Der nächste große Block ist:

**Content vervollständigen → Welt befüllen → Gameplay-Loops testen → Balancing → Polish → Release-Abnahme**

Für weitere Arbeiten sollte deshalb jeder Bereich getrennt bewertet werden nach:

1. **System vorhanden?**
2. **Content vorhanden?**
3. **Gameplay-Loop komplett?**
4. **Ingame getestet?**
5. **Gebalanced?**

Damit kann „fertig“ deutlich sauberer definiert werden, ohne funktionierende Systeme unnötig erneut zu bauen.
