# PixelRPG — 100%-Forensischer Static Audit

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `refactor/central-content-pipeline-v3`  
**Audit-Commit:** `2312d944cbfd5708ef32f84f17cdde31665c07b0`  
**Audit-Zeitpunkt:** 2026-08-30 03:14 CEST  

## 1. Verbindliche Regel

Dieser Audit ist ausschließlich eine forensische Bestandsaufnahme. Er ist **kein Refactoring-Auftrag**.

Für jedes vorhandene Feature gilt:

- **SOLL:** Zielwirkung aus vorhandener Implementierung, Datenmodell und relevanter Commit-Historie.
- **IST:** tatsächlich im geprüften Branch vorhandene Implementierung **und deren Runtime-Verdrahtung**.
- Keine bestehende Funktion darf durch diesen Audit verändert, verbessert, neu balanciert oder umverdrahtet werden.
- Abweichungen werden nur dokumentiert.
- Ein „Befund“ ist ausdrücklich **keine** Änderungsfreigabe.

## 2. Prüfgrenze

Der komplette rekursive Git-Tree des Branches wurde inventarisiert; der Tree ist nicht als `truncated` markiert. Geprüft wurden Build/Plugin-Lifecycle, APIs, Player/Storage, Items, Gear, Equipment Sets, Combat, Skills, Loot, Mob Scaling, Professions, Crafting, Quests, NPCs, native Dialogs, Shops, Bank, Trade Depot, Party, Guild, World Bosses, Companions, Statistics, Scoreboard, Story, Travel, Economy, Ressourcen/JSON, Persistenz und Runtime-Wiring.

**Aussagegrenze:** statischer Forensik-Audit. Kein echter Minecraft-Server mit Welt, Spielern, Datenbank und Resource Pack wurde als Live-Test betrieben. Runtime-Aussagen sind daher statisch aus Code und Verdrahtung verifiziert.

## 3. Plattform / Build

### SOLL
- Java 25
- Paper 26.x, verbindlich Paper 26.2
- `io.papermc.paperweight.userdev` 2.0.0-beta.21
- `paperweight.paperDevBundle("26.2.build.+")`
- `paper-plugin.yml`
- aktuelle Paper-/Mojang-Mappings
- Adventure Components statt `ChatColor`

### IST
- Java 25 und `options.release = 25`
- Paper Dev Bundle `26.2.build.+`
- userdev `2.0.0-beta.21`
- `paper-plugin.yml` mit API `26.2`
- ShadowJar mit Gson/Hikari/MySQL
- keine Treffer für `ChatColor`
- keine Legacy-Pakete `net.minecraft.server.v1_XX_RY`
- GitHub Actions Build für Commit `2312d944cbfd5708ef32f84f17cdde31665c07b0`: **success**

**Status:** ✅ SOLL/IST konsistent.

## 4. Feature-Matrix — SOLL & IST

| Feature | SOLL | IST | Status |
|---|---|---|---|
| Plugin Bootstrap | Paper-26.2-Bootstrap/Lifecycle | `PixelRPGBootstrap` + `PixelRPGPlugin` | ✅ |
| Player Profile | PreLogin → Join → Quit-Persistenz | Async Load, Loading-Cache, Save-Chain | ✅ |
| YAML/MySQL | Umschaltbare Profile-Persistenz | Beide Repositorys, YAML Default | ✅ |
| Central Item Pipeline | zentrale Item-Definition/Erzeugung | `ItemDefinitionRegistry` → `ItemService` → `RPGItemBuilder` | ✅ |
| Item IDs | stabile `pixelrpg:` IDs | Normalisierung + PDC | ✅ |
| Item Scaling | deterministische Level/Rarity-Skalierung | `item-scaling.json` + Builder | ✅ |
| Rarity | Common bis Unique | Definition/Enum/Validierung | ✅ |
| Gearscore | Level/Rarity/Modifier | PDC + Lore | ✅ |
| Soulbound | gebundene Items | PDC + Death Listener | ✅ |
| Unique Items | admin-only Unique | Registry + `UniqueItemService` | ✅ |
| Weapon Abilities | Vanilla-Waffen mit RPG-Abilities | Material-Mapping, PDC, 6000 ms Default | ✅ |
| Combat | RPG Damage/Crit/Lifesteal/Armor | `CombatDamageListener` + Calculator + Stats | ✅ |
| Boss Hit Cap | Trefferlimit relativ zu Max-HP | `0.12` Max-HP | ✅ |
| Mob Scaling | Level/HP/Damage/Parity/Gear | JSON + Listener + Nameplate | ✅ |
| Mob XP | skalierte XP | `MobExperienceListener` | ✅ |
| Loot | RPG-Items + Currency | Loot Listener + ItemService | ✅ |
| Professions | vier Berufe, Level 1–100 | Blacksmith/Provisioner/Scholar/Alchemist | ✅ |
| Crafting | PixelRPG-Crafting getrennt von Vanilla | `CraftingService` | ✅ |
| Crafting Rarity | Qualitäts-Roll | `CraftingRarityRoller` | ✅ |
| Native Dialogs | native Minecraft/Paper Dialog API | `Dialog`, `DialogBase`, `DialogType`, `ActionButton`, `DialogInput`, `showDialog()` | ✅ |
| NPCs | persistente Mannequin-NPCs | `NpcManager`, Chunk Lifecycle, Behaviors | ✅ |
| NPC Quest Progress | TALK_TO_NPC beim Interact | `NpcInteractListener` → `progressTalkToNpc()` | ✅ |
| Quests | HUNT/COLLECT/REACH/TALK/GLOBAL_EVENT | Repository + Manager | ✅ |
| Quest Limit | maximal 5 aktiv | `MAX_ACTIVE_QUESTS = 5` | ✅ |
| Party Quest Progress | Party-Radius-Sharing | Quest-Propagation | ✅ |
| Profession Quests | Berufsquests über Berufssystem | expliziter Profession-Quest-Pfad | ✅ |
| World Quest Expansion | Biom/Structure Navigation | `quests_world_expansion.json` geladen | ✅ |
| Content Expansion 01 | zusätzlicher Quest-Content-Pack | Datei vorhanden, **nicht geladen** | ⚠️ |
| Shops | NPC-Shops + Persistenz | `ShopManager` + GUIs + Behavior | ✅ |
| Shop Migration | Base64 `item-data` + Legacy-Fallback | beides implementiert | ✅ |
| Bank | Storage/Dialog | Bank Services/Dialogs + Banker Behavior | ✅ |
| Trade Depot | Einstellen/Kaufen/Cancel/Expiry | `TradeDepotManager` implementiert | ⚠️ nicht verdrahtet |
| Party | Invite/Join/Kick/Leader/Persistenz | `PartyManager` + Commands | ✅ |
| Guild | Create/Invite/Join/Leave/Persistenz | `GuildManager` implementiert | ⚠️ nicht verdrahtet |
| Registered Player Gate | RPG-Systeme nur für registrierte Spieler | `PlayerProfileManager` als aktive `GuildAPI` | ✅ |
| World Bosses | World/Biome, Phasen, Patterns, Bossbar, Loot | `BossManager` + Spawn/Patterns/Protection | ✅ |
| Boss Rewards | konkrete RPG-Reward-Items | JSON + ItemService + Listener | ✅ |
| Companions | Ownership/Active/XP/Level/Equipment/Mount | `CompanionService` + Controller/Listener | ✅ |
| Companion Passive Stats | aktiver Companion liefert Boni | `StatEngine` | ✅ |
| Equipment Sets | Boni + Armor Trims | `EquipmentSetService` | ✅ |
| Statistics | Mob/Death/Boss/Quest Stats | Listener + Service | ✅ |
| Scoreboard/Playtime | laufende Spieleranzeige | `ScoreboardService` + `PlaytimeTracker` | ✅ |
| Story | Book/Chapter/Manager/NPC | `StoryManager` + `StoryBehavior` | ✅ |
| Travel | Compass/Travel Dialog | Travel Factory/Listener/Behavior | ✅ |
| Economy | Gold/Currency | Economy/Profile/Guild Currency | ✅ |
| Commands | moderne Paper Lifecycle Registration | `LifecycleEvents.COMMANDS` | ✅ |
| Region System | produktives Region-Feature | nur `docs/ideas/...` | ❌ nicht implementiert |

## 5. Forensische Hauptbefunde

### F-001 — `quests_content_expansion_01.json` ist nicht Teil der aktiven Quest-Pipeline

**SOLL:** Der vorhandene Content-Pack ist strukturell für die zentrale Quest-Pipeline vorbereitet.

**IST:** `QuestRepository.load()` lädt nur:

1. `quests_v2.json`
2. `quests_additional.json`
3. `quests_world_expansion.json`

`quests_content_expansion_01.json` wird nicht geladen.

**Bewertung:** 🟠 Mittel — Content-/Wiring-Abweichung.

**Wichtig:** Nicht eigenmächtig aktivieren. Das würde das bestehende Feature-Verhalten verändern.

### F-002 — `GuildManager` existiert, ist aber nicht der aktive Guild-Startup-Pfad

**SOLL:** Guild-System mit Create/Invite/Join/Leave/Disband/Persistenz.

**IST:** `GuildManager` implementiert diese Funktionen. In der zentralen `PixelRPGPlugin`-Initialisierung wird er jedoch nicht erzeugt/verdrahtet. Stattdessen ist `PlayerProfileManager` die konkrete aktive `GuildAPI`-Implementierung und wird z. B. an `BossManager` übergeben.

**Bewertung:** 🔴 Hoch — implementierter, aber nicht aktiv verdrahteter Featurepfad.

**Verhaltensschutz:** Nicht einfach registrieren. Das würde die Semantik von `GuildAPI.isRegistered()` und damit RPG-/Boss-Gates verändern.

### F-003 — `TradeDepotManager` ist implementiert, aber im zentralen Startup nicht als aktiver Pfad nachgewiesen

**SOLL:** RPG-Item-Auswahl, Preisdialog, 7 Tage Laufzeit, 5 % Verkaufsgebühr, Kauf, Cancel, Expiry und Pending Payouts.

**IST:** Diese Logik ist in `TradeDepotManager` vorhanden. Eine entsprechende zentrale Erstellung/Command-/NPC-Verdrahtung ist im `PixelRPGPlugin`-Startup nicht erkennbar.

**Bewertung:** 🟠 Mittel/Hoch — Verdrahtungsbefund.

**Verhaltensschutz:** Nicht ohne separate Funktionsentscheidung aktivieren.

### F-004 — Mob Scaling: JSON ist die primäre Quelle

`MobScalingConfig.load(FileConfiguration)` lädt zuerst `mob-scaling.json` und nutzt `config.yml` nur als Fallback. Damit sind die Werte in `data/mob-scaling.json` die effektive Primärquelle, solange deren Laden funktioniert. Die abweichenden Legacy-Werte in `config.yml` sind nicht automatisch die aktiven Werte.

**Status:** 🟢 kein Funktionsfehler; wichtig für die korrekte IST-Rekonstruktion.

### F-005 — Native Dialogs entsprechen exakt dem geforderten Modell

`DialogueEngine` verwendet direkt Paper/Minecraft-native Dialogklassen und `Player.showDialog()`. Es gibt keinen alten 1.21.x-Dialog-Nachbau. Empty-Action-Listen werden ebenfalls entsprechend der aktuellen Dialog-Anforderung abgesichert.

**Status:** ✅.

### F-006 — Central Item Pipeline ist tatsächlich zentralisiert

`ItemDefinitionRegistry` lädt `item-definitions.json` und `boss-reward-items.json`; `ItemService` ist die zentrale Erzeugungs-/Identifikationsschicht; `RPGItemBuilder` bildet die deterministische Item-Struktur und Skalierung. IDs werden normalisiert, Duplicate IDs abgelehnt und Material/Kategorie validiert.

**Status:** ✅.

### F-007 — Quest-Items unterstützen Vanilla und konkrete PixelRPG-Definitionen

`QuestRepository.isQuestItem()` akzeptiert Vanilla-Materialien und bekannte `pixelrpg:`-Definitionen. `QuestManager.countQuestItems()` unterscheidet entsprechend zwischen Material- und konkreter RPG-Item-ID.

**Status:** ✅.

### F-008 — Crafting bleibt von Vanilla-Crafting getrennt

`CraftingService` arbeitet über registrierte PixelRPG-Rezepte und Inventarkosten. Potion-/Enchantment-Ergebnisse werden separat erzeugt; RPG-Items laufen über `ItemService`.

**Status:** ✅.

### F-009 — World Boss Protection ist mehrschichtig

BossManager, WorldBossProtectionListener, Combat-Gates, registrierte Spieler, Bossbar-Gates und Add-Cleanup greifen zusammen. Nicht registrierte Spieler werden aus den RPG-Boss-Pfaden ausgeschlossen.

**Status:** ✅.

## 6. Runtime-Wiring

Der aktive `PixelRPGPlugin.onEnable()`-Pfad verdrahtet u. a.:

- Player Profile Lifecycle
- Profession System
- Item/Equipment/Crafting
- Mob Scaling/XP/Nameplates
- Shops
- Story
- Party
- Quests/Global Events
- World Bosses/Patterns/Protection/Rewards
- Statistics/Scoreboard/Playtime
- NPC Manager/Chunk/Look/Interaction
- Native Dialogs/Quick Actions
- Reception/Quest/Shop/Travel/Story/Banker/Filler/Profession-NPC-Behaviors
- Combat/Weapon Skills/Loot
- Soulbound
- Guild Currency/Compass
- Companion XP/Dialoge
- moderne Paper Lifecycle Commands

`GuildManager`, `TradeDepotManager` und `quests_content_expansion_01.json` sind davon abzugrenzen.

## 7. Persistenz-Befund

### Player Profiles — ✅
- PreLogin Load mit Timeout.
- Join Activation.
- Quit Save.
- Per-Player Save-Chain.
- MySQL/YAML Repository.
- YAML-Fallback bei MySQL-Initialisierungsfehler.
- Emergency-YAML bei MySQL-Savefehler.

### NPCs — ✅
- `npcs.yml`
- stabile IDs
- Chunk Spawn/Despawn
- Mannequin PDC-Marker

### Parties — ✅
- `parties.yml`
- Invite Timeout 60 s
- Empty Cleanup 30 min

### Shops — ✅
- `shops.yml`
- Base64 Item-Daten
- Legacy-Fallback/Migration

### Trade Depot — ⚠️
- `trade-depot.yml`
- Base64 Items
- Pending Payouts
- Expiry
- Implementierung vorhanden, zentrale Aktivierung nicht nachgewiesen

### Guilds — ⚠️
- `guilds.yml`
- Load/Save vorhanden
- Manager im aktuellen Startup nicht verdrahtet

## 8. Datenintegrität

Vorhandene harte Prüfungen umfassen:

- Item-Level 1–99
- Required-Level ≤ Item-Level
- Unique = UNIQUE + admin-only
- nichtnegative Weapon-Cooldown
- positiver Gearscore-Modifier
- keine Duplicate Item IDs
- Material/Kategorie-Konsistenz
- Quest-ID-Duplikatprüfung
- Pflichtfelder für Quest-Titel/Beschreibung
- Levelbereich 1–99
- gültige HUNT-EntityTypes
- Vanilla- oder bekannte PixelRPG-COLLECT-Items
- Navigation für REACH_LOCATION
- Quest-Prerequisite-/Follow-Up-Referenzen
- Erkennung von Prerequisite-Zyklen

**Status:** ✅.

## 9. Historische SOLL-Rekonstruktion

Die Commit-Historie bestätigt als bereits definierte Zielverhalten unter anderem:

- konkrete Item-Definitionen und zentrale Loot-Pipeline
- Vanilla/PixelRPG-Loot-Trennung
- Loot-Level-Begrenzung
- sechs Sekunden Weapon Ability Cooldown
- getrennte Ability-/Vanilla-Cooldowns
- vollständige vier Profession-Rezeptbäume
- getrenntes PixelRPG-Crafting
- Crafting-Rarity-Rolls
- Potion-/Enchantment-Rezeptmetadaten
- Profession-Quest-Lifecycle und Content Packs
- einheitliche Quest-Texte/Progress-Anzeigen
- World-Boss-Phasen, Loot, Protection und Add-Cleanup
- Produktions-Companions
- stabiles Quick-Actions-Wiring
- sichere Trade-Depot-Itemauswahl
- native Dialog-Fixes
- player-lokalisierte Character Dialogs

Diese Historie ist **SOLL-Beleg**, kein Änderungsauftrag.

## 10. Nicht implementiert / nicht aktiv

### Nicht implementiert

- Region-System: `docs/ideas/pixelrpg-region-system.md` ist eine Idee/Dokumentation, kein produktiver Runtime-Service.

### Implementiert, aber nicht aktiv verdrahtet

- `GuildManager`
- `TradeDepotManager`
- `quests_content_expansion_01.json`

Diese Punkte dürfen nicht allein aufgrund ihrer Existenz als aktive Features gezählt werden.

## 11. Gesamturteil

| Bereich | Urteil |
|---|---|
| Build | ✅ erfolgreich |
| Paper 26.2 / Java 25 | ✅ |
| Native Dialog API | ✅ |
| Central Item Pipeline | ✅ |
| Quest Pipeline | ⚠️ ein vorhandenes Content-Pack wird nicht geladen |
| Player/Profile Persistence | ✅ |
| Combat/Scaling/Loot | ✅ |
| Professions/Crafting | ✅ |
| NPC/Dialogue | ✅ |
| Party | ✅ |
| World Boss | ✅ |
| Companions | ✅ |
| Guild | ⚠️ Implementierung vorhanden, nicht aktiver Startup-Pfad |
| Trade Depot | ⚠️ Implementierung vorhanden, zentrale Aktivierung nicht nachgewiesen |
| Region System | ❌ nicht implementiert |

### Verbindliche Schlussfolgerung

`refactor/central-content-pipeline-v3` besitzt einen erfolgreich gebauten Paper-26.2-/Java-25-Kern mit umfangreicher RPG-Funktionalität. Die aktiven Features sind statisch nachvollziehbar und weitgehend zentral verdrahtet.

Die wichtigsten forensischen Abweichungen sind **Content-/Wiring-Befunde**:

1. `quests_content_expansion_01.json` vorhanden, aber nicht geladen.
2. `GuildManager` implementiert, aber nicht im aktuellen zentralen Startup verdrahtet.
3. `TradeDepotManager` implementiert, aber im aktuellen zentralen Startup nicht als aktiver Pfad nachgewiesen.

**Unveränderliche Audit-Regel:** Keine dieser Abweichungen darf durch eigenmächtiges Refactoring, Aktivieren, Umverdrahten, Balancing oder API-Umbau „behoben“ werden. Das bestehende SOLL/IST-Verhalten bleibt unverändert, bis eine separate fachliche Änderungsentscheidung getroffen wurde.
