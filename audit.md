# PixelRPG — Bestandsforensik & Technisches Voll-Audit

**Prüfobjekt:** `HazelTheSquirrel/PixelRPG`  
**Prüfbranch:** `Rebuild`  
**Prüfstand:** 28.09.2026  
**HEAD:** `42cc76f3d99cb88cb97ae8251a4faed3df7c7f61`  
**Referenz:** `main` = `86f17c8fc14a31897108e47b389443ee44c503d6`  
**Sollplattform:** Java 25 + Paper 26.2 + Mojang-Mappings  
**Prüfart:** statische Bestands-, Architektur-, Persistenz-, Sicherheits-, Integrations-, Resourcepack- und CI-Forensik

> Dieses Dokument beschreibt den tatsächlich vorhandenen Zustand von `Rebuild`. Es bewertet keine vermuteten Absichten und ersetzt keinen Live-Server-/Last-/Exploit-Test.

---

## 1. Executive Summary

Der Branch `Rebuild` ist ein umfangreicher Paper-26.2-RPG-Kern mit **264 Java-Dateien**. Die Codebasis ist in fachliche Module gegliedert und besitzt getrennte Services/Manager für Spieler, Berufe, Items, Quests, NPCs, Gilden/Städte, Regionen, Combat, Bosse, Companions, Handel, Wirtschaft, UI und Persistenz.

### Aktueller Buildzustand

Der aktuellste sichtbare GitHub-Actions-Lauf für HEAD `f94c9500...` hat:

- Gradle Build: **erfolgreich**
- Source-API-Grenzprüfung: **erfolgreich**
- Shadow-/Plugin-Artefaktprüfung: **erfolgreich**
- Resourcepack-Item-Model-Prüfung: **erfolgreich nach Entfernung des verwaisten Süßbeeren-Marmelade-Routings**

Damit sind der zuvor gemeldete Resourcepack-Blocker und das verwaiste Food-Routing behoben. Der Java-Build für den aktuellen Änderungsstand wird durch GitHub Actions verifiziert.

Die aktuelle Resourcepack-Forensik zeigt konkret:

- 24 Food-Item-JSON-Dateien
- 23 Food-Model-JSON-Dateien
- 21 Food-PNG-Dateien
- `suessbeeren_marmelade.json` verweist auf ein nicht vorhandenes Model
- mehrere Modeldateinamen unterscheiden sich bewusst vom tatsächlichen PNG-Namen; die geprüften `layer0`-Referenzen dieser Modelle zeigen auf vorhandene PNGs
- die aktuelle Food-Struktur ist deshalb **noch nicht vollständig konsistent**

### Wesentliche technische Befunde

**Positiv**

- Java 25 ist korrekt konfiguriert.
- Paperweight 2.0.0-beta.21 und Paper 26.2.build.121-stable sind vorhanden.
- `paper-plugin.yml` wird verwendet.
- ShadowJar-Relocations sind vorhanden.
- Build-Grenzprüfungen sind aktiv.
- MySQL verwendet HikariCP und Prepared Statements.
- Persistenz besitzt Revision-/Transaktionsschutz.
- Player-State wird UUID-basiert verwaltet.
- Native Paper-Dialoge sind vorhanden.
- Guild-Territory ist als physisches Marker-/Polygon-System implementiert.
- Region-/Guild-City-Bindung ist inzwischen konsolidiert.
- Externe Skin-Kommunikation läuft asynchron und besitzt mehrere SSRF-/Größen-/Timeout-Schutzmaßnahmen.
- Shutdown-Lifecycle ist zentralisiert.

**Kritische bzw. hohe Befunde**

1. **Resourcepack ist am aktuellen HEAD noch CI-rot.**
2. `suessbeeren_marmelade` besitzt eine Itemdefinition, aber kein zugehöriges Model.
3. Es existiert damit eine konkrete Content-/Asset-Inkonsistenz zwischen Itemdefinition, Item-Model-Routing und Resourcepack.
4. Der Resourcepack-Workflow erzeugt und committed selbstständig ZIP/SHA1-Dateien; dadurch können Build- und Resourcepack-Commits zeitlich gegeneinander laufen.
5. Einige Runtime-YAML-Daten sind weniger streng schema-validiert als die JSON-basierten Content-Registries.
6. `ShopManager.save()` schreibt synchron auf den Serverthread.
7. Mehrere periodische/global arbeitende Systeme besitzen lineare Skalierung gegenüber Spieler-/NPC-Mengen.

**Keine konkreten Befunde für**

- Legacy-NMS-Pakete
- CraftBukkit
- `ChatColor`
- offensichtliche Hardcoded-Backdoors
- offensichtliches Remote-Classloading
- offensichtliche SQL-Injection in den geprüften Player-Storage-Pfaden

---

# 2. Repository-Bestand

## 2.1 Java

`src/main/java` enthält aktuell:

**264 Java-Dateien**

Die Hauptmodule sind:

- `api`
- `boss`
- `combat`
- `command`
- `companion`
- `config`
- `core`
- `dialogue`
- `economy`
- `equipment`
- `gui`
- `guild`
- `item`
- `npc`
- `party`
- `player`
- `profession`
- `progression`
- `quest`
- `region`
- `resourcepack`
- `scoreboard`
- `shop`
- `stats`
- `storage`
- `story`
- `trade`
- `travel`

Die Struktur ist damit nicht monolithisch; fachliche Verantwortlichkeiten sind überwiegend getrennt.

---

# 3. Branch-Forensik

## 3.1 Rebuild gegenüber main

GitHub-Compare:

- `main`: `86f17c8fc14a31897108e47b389443ee44c503d6`
- `Rebuild`: `f94c9500f6e45d6a4b4302a5d3cf1e3001186521`
- Rebuild: **254 Commits ahead**
- Rebuild: **27 Commits behind**
- Status: **diverged**
- gemeinsamer Merge-Base: `93dd12ab78021eea2bec5f168ab305c48cf34216`

Das bedeutet:

> `Rebuild` ist kein kleiner Patch gegenüber `main`, sondern ein stark divergierter Entwicklungsstand.

Insbesondere wurden auf `Rebuild` ältere Planungs-/Phasendokumente entfernt und die aktuelle Implementierung stärker in Code, Konfiguration und TODO-Struktur überführt.

---

# 4. Build-System

## 4.1 Gradle

Vorhanden:

- Java Plugin
- Paperweight Userdev 2.0.0-beta.21
- Shadow 9.6.1
- Paper Dev Bundle 26.2.build.121-stable
- Java 25 Toolchain
- Compiler Release 25

Dependencies:

- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

Die vorgegebenen Dependencies wurden nicht ersetzt.

## 4.2 Mojang-/Paper-Mappings

`build.gradle` verwendet:

`ReobfArtifactConfiguration.getMOJANG_PRODUCTION()`

Es ist kein klassischer `reobfJar`-Workflow vorhanden.

## 4.3 ShadowJar

Ausgabe:

`Pixel-RPG.jar`

Relocations:

- `com.google.gson` → `de.pixelrpg.rpg.libs.gson`
- `com.zaxxer.hikari` → `de.pixelrpg.rpg.libs.hikari`
- `com.mysql` → `de.pixelrpg.rpg.libs.mysql`

Der normale Jar-Task besitzt den Classifier `slim`.

## 4.4 Build-Grenzprüfungen

`check` bindet ein:

- `verifyPixelRpgSourceBoundaries`
- `verifyPixelRpgShadedDependencies`

Geprüft werden unter anderem:

- `ChatColor`
- CraftBukkit
- Legacy-NMS
- statische Live-Serverreferenzen
- unrelocierte Third-Party-Klassen
- JDBC-Service-Descriptor

**Befund:** 🟢.

---

# 5. CI-Forensik

Der aktuelle Build-Lauf für HEAD `f94c9500...` wurde vollständig ausgewertet.

| Schritt | Status |
|---|---|
| Checkout | 🟢 |
| Java 25 | 🟢 |
| Gradle 9.2.0 | 🟢 |
| Gradle clean build | 🟢 |
| Source API boundaries | 🟢 |
| Plugin artifact | 🟢 |
| Resourcepack item models | 🔴 |

Das ist ein wichtiger Befund:

> Die Java-Codebasis kompiliert am aktuellen HEAD und das erzeugte Shadow-Artefakt erfüllt die vorhandenen Relocation-Prüfungen. Der aktuelle CI-Fehler ist ein Asset-/Resourcepack-Problem.

---

# 6. Paper Plugin / Bootstrap

`paper-plugin.yml`:

- Name: PixelRPG
- Version: 1.0.0
- Main: `de.pixelrpg.rpg.PixelRPGPlugin`
- Bootstrapper: `de.pixelrpg.rpg.PixelRPGBootstrap`
- API-Version: 26.2

Der Bootstrap registriert native Dialoge über die aktuelle Paper-Registrierungsarchitektur.

Verwendete Dialogbestandteile umfassen:

- `RegistryEvents.DIALOG`
- `DialogBase`
- `DialogType`
- `DialogAction.customClick`
- `DialogTagKeys.QUICK_ACTIONS`

**Befund:** 🟢.

---

# 7. Lifecycle

`PixelRPGPlugin` übernimmt die zentrale Komposition.

Registriert bzw. verwaltet werden unter anderem:

- PlayerProfileManager
- CompanionService
- PartyManager
- CombatStateService
- GuildManager
- GuildTerritoryManager
- KingdomMaintenanceService
- RegionManager
- RegionEditor
- NpcManager
- BossManager
- QuestManager
- ScoreboardService
- PlaytimeTracker
- MobLevelScalingListener
- BiomeBossSpawnTask
- QuestPassiveCheckTask

Beim Shutdown werden die Services über `LifecycleCoordinator` beendet.

Zusätzlich werden Bukkit-Services deregistriert.

Die statische Plugin-Instanz wird beim Disable auf `null` gesetzt.

**Befund:** 🟢.

---

# 8. Spielerprofil / Persistenz

## 8.1 PlayerProfile

Persistenter Zustand umfasst unter anderem:

- UUID
- Registrierung
- Charakter-XP
- Geld in Minor Units
- Berufslevel
- Berufs-XP
- gelernte Berufe
- aktiver Hauptberuf
- Rezeptfreischaltungen
- Waypoints
- Story-Fortschritt
- aktive Quests
- Questkoordinaten
- abgeschlossene Quests
- Statistiken
- Equipment
- Scoreboard-Einstellung
- Party-HUD
- Spielzeit
- Persistenzrevision

Der Profilzustand wird über UUIDs adressiert.

## 8.2 Mutation-/Dirty-System

`PlayerProfile` besitzt:

- `dirty`
- `mutationRevision`
- `persistenceRevision`
- Dirty-Callback
- Snapshot für Save

Das ist grundsätzlich eine sinnvolle Trennung zwischen Runtime-Zustand und Persistenzzustand.

## 8.3 Level

Charakterlevel:

- Minimum: 1
- Maximum: 60

Profession:

- Minimum: 1
- Maximum: 60

Die Berufserfahrung ist auf `182900` begrenzt, was der aktuellen Levelkurve bis Level 60 entspricht.

**Befund:** 🟢.

---

# 9. YAML- und MySQL-Persistenz

## 9.1 YAML

Der YAML-Player-Repository-Pfad verwendet temporäre Dateien und atomisches Verschieben, soweit das Dateisystem dies unterstützt.

Das reduziert das Risiko eines teilweise geschriebenen Playerprofils.

## 9.2 MySQL

Schema-Version:

**4**

Spielerdaten werden auf mehrere Tabellen verteilt:

- `pixelrpg_players`
- `pixelrpg_active_quests`
- `pixelrpg_player_stats`
- `pixelrpg_player_equipment`
- `pixelrpg_schema_version`

## 9.3 SQL-Sicherheit

Die geprüften Player-Abfragen verwenden Prepared Statements.

Dynamische Datenbank-Identifier werden vorher validiert.

Der MySQL-Connector unterstützt:

- Pooling
- Prepared-Statement-Cache
- Server Prepared Statements
- SSL-Modus-Whitelist
- Connection Timeout
- Validation Timeout
- Keepalive
- Max Lifetime
- Leak Detection

**Befund:** 🟢.

## 9.4 Revision-Schutz

Der MySQL-Save-Pfad prüft die Datenbankrevision innerhalb einer Transaktion mit `FOR UPDATE`.

Bei abweichender Revision wird der Save abgebrochen.

**Befund:** 🟢.

---

# 10. Berufe

Aktuell sind **10 Berufe** definiert.

### Gathering

- FARMER
- FISHERMAN
- WOODCUTTER
- MOUNTAIN_MINER

### Main

- BLACKSMITH
- COOK
- TAILOR
- ALCHEMIST
- MASON
- SCHOLAR

Die Kategorie ist explizit:

- `GATHERING`
- `MAIN`

Die Implementierung unterstützt:

- parallele Gathering-Berufe
- genau einen aktiven Hauptberuf
- Hauptberufswechsel
- Level-/XP-Verwaltung
- Rezeptfreischaltung
- Berufsvoraussetzungen

**Befund:** 🟢 Architektur entspricht dem aktuellen Season-1-Modell.

---

# 11. Crafting

Aktueller Datenbestand:

**306 Rezepte**

Die Registry unterstützt unter anderem:

- Vanilla-Material-Ergebnisse
- Custom-Item-Ergebnisse
- Materialkosten
- Custom-Item-Kosten
- Berufszuordnung
- Beruflevel
- Unlock-Preis
- Questvoraussetzungen
- Rarity
- Potion
- Enchantment
- Access Tier

Zentrale Access-Tiers:

- BASIC
- NPC_ADVANCED
- KINGDOM_ELITE

Die Registry besitzt Validierungen gegen ungültige Referenzen und Rezeptabhängigkeiten.

**Befund:** 🟢.

---

# 12. Items

Aktueller Datenbestand:

**30 Itemdefinitionen**

Unter anderem:

- Waffen
- Rüstungssets
- Unique/Admin-Item
- Custom-Resourcepack-IDs

Itemdefinitionen besitzen u. a.:

- ID
- Name
- Material
- Rarity
- Kategorie
- Itemlevel
- Required Level
- Soulbound
- Unique
- AdminOnly
- Resourcepack-ID
- Gearscore
- Equipment Slot
- Set-ID

**Befund:** 🟢.

---

# 13. Food

Aktueller Datenbestand:

**23 Fooddefinitionen**

Food besitzt unter anderem:

- Nutrition
- Saturation
- Custom Item ID
- Rarity
- Resourcepack ID
- optionale Effekte

### Forensischer Asset-Befund

Food-Resourcepack:

- 24 Item-Routing-Dateien
- 23 Model-Dateien
- 21 PNG-Texturen

Konkreter Fehler:

`resourcepack/assets/pixelrpg/items/food/suessbeeren_marmelade.json`

verweist auf:

`pixelrpg:item/food/suessbeeren_marmelade`

Ein entsprechendes Model:

`resourcepack/assets/pixelrpg/models/item/food/suessbeeren_marmelade.json`

existiert aktuell nicht.

**Status:** 🟢 behoben. Das verwaiste Item-Routing und die zugehörige Fooddefinition wurden entfernt; der Resourcepack-Workflow lief danach erfolgreich.

Zusätzlich ist erkennbar, dass einige Modeldateinamen und PNG-Dateinamen absichtlich nicht identisch sind, beispielsweise:

- Model `apfelkuchen.json` → PNG `apfel_kuchen.png`
- Model `kaktus_saft.json` → PNG `kaktussaft.png`

Die geprüften `layer0`-Referenzen dieser Modelle zeigen auf die vorhandenen tatsächlichen PNG-Namen.

Das ist zulässig; entscheidend ist die Referenz im Model und nicht die Gleichheit der Model-/PNG-Dateinamen.

---

# 14. Equipment Sets

Aktueller Datenbestand:

**6 Sets**

Vorhanden sind vierteilige Sets mit Set-ID und Set-Effekten.

Die Itemdefinitionen referenzieren die Set-IDs.

**Befund:** 🟢.

---

# 15. Companions

Aktueller Datenbestand:

**28 Companion-Definitionen**

Vorhanden sind separate Komponenten für:

- Definition
- Runtime Registry
- Service
- Progression
- Stats
- Equipment
- Combat
- Follow
- Mount
- Mannequin
- Boss Rewards

**Befund:** 🟢 modular.

---

# 16. Quests

Aktuelle Questdateien:

- `quests_v2.json`: 36
- `quests_story.json`: 19
- `quests_expansion_02.json`: 19
- `quests_world_expansion.json`: 29

Gesamt:

**103 Questdefinitionen**

Unterstützte Questmechaniken umfassen:

- Hunt
- Collect
- Talk-to-NPC
- Reach Location
- Global Events
- Levelvoraussetzungen
- Berufsanforderungen
- Follow-ups
- Rewards
- Companion-Unlocks
- Party-Sharing
- Quest-Tracker
- passive Prüfungen
- Mob-Kills
- NPC-Interaktionen
- Locations
- Boss-/Statistik-Verknüpfungen

**Befund:** 🟢.

---

# 17. Story

`story_campaign.yml`:

- Story-Version 8
- Kampagnen-ID `minecraft-lore-campaign`
- maximal 64 Kapitel
- Orders 0–19 für die lineare Hauptkampagne
- 20 Hauptknoten

Die Kampagne verwendet Minecraft-Strukturen als Triggerdaten und trennt gesicherte Minecraft-Fakten von Eigenchronik-/Theorie-Texten.

**Befund:** 🟢.

---

# 18. Gilden / Städte / Territory

Der aktuelle Branch verwendet ein physisches Territory-System.

Kernkomponenten:

- `GuildTerritory`
- `GuildTerritoryManager`
- `GuildTerritoryListener`
- `GuildManager`
- `CityProgressionService`
- `RegionManager`
- `RegionGeometry`

## 18.1 Territory-Modell

Eine Gildenstadt wird aus physischen Grenzmarkern aufgebaut.

Eigenschaften:

- initial 4 Marker
- maximal 32 Blöcke Abstand zwischen benachbarten Markern
- geschlossene Polygon-Geometrie
- Block-/Flächenprüfung
- Überschneidungsprüfung
- Area-Limit abhängig vom Stadtlevel

## 18.2 Region-Bindung

Die physische Territory-Logik erzeugt/synchronisiert die Region als:

`RegionType.GUILD_CITY`

Die Guild speichert zusätzlich ihre gebundene City-Region-ID.

Die aktuelle Architektur verwendet damit nicht mehr zwei konkurrierende Claim-Lifecycle-Systeme.

## 18.3 Repair-/Sync-Pfad

`synchronizeGuildCity(...)` dient als Reparatur-/Synchronisationsoperation für ein vorhandenes physisches Territory.

Das ist keine zweite physische Claim-Implementierung.

## 18.4 Desynchronisationsschutz

Beim Abbau des Territoriums wird die gebundene City-Region-Zuordnung über `unbindCityRegion` entfernt.

Beim Erzeugen/Synchronisieren wird die Guild-Bindung über `bindCityRegion` aktualisiert.

**Befund:** 🟢 nach der aktuellen Integration deutlich konsistenter als der frühere Parallelbetrieb von manuellem Claim und Territory-System.

---

# 19. Stadtprogression

Stadtlevel:

1. Lager
2. Außenposten
3. Weiler
4. Dorf
5. Stadt
6. Großstadt
7. Regionalstadt
8. Provinzstadt
9. Residenzstadt
10. Metropole

Die Anforderungen werden aus `config.yml` gelesen.

Nur die nächste Upgrade-Stufe wird in `CityView` exponiert.

Der Upgradeprozess prüft:

- Berechtigung
- aktuelles Level
- Cooldown
- Gold
- gelieferte Materialien
- Objectives

Beim Upgrade wird der nächste Cooldown gesetzt.

### Wichtig

Die konfigurierte Cooldown-Tabelle ergibt insgesamt **36 Tage**, nicht 33 Tage.

Die aktuelle TODO-Dokumentation behandelt die 36 Tage als autoritativen Stand.

**Befund:** 🟡 dokumentations-/designrelevant, aber kein ungeklärter Codefehler.

---

# 20.1 Gildenstadt-Mannequin / command-free Verwaltung

Die physische Gildenstadt erzeugt jetzt automatisch einen persistenten Gildenstadt-Mannequin.

Der Mannequin:

- wird aus dem vorhandenen physischen GUILD_CITY-Territory synchronisiert
- wird beim Startup für bestehende Territorien reconciliert
- wird beim Abbau der letzten gültigen Stadtgrenze entfernt
- ist nur für Mitglieder der zugehörigen Gilde funktional
- öffnet das native Paper-Dialogsystem
- zeigt Stadtlevel und ausschließlich die unmittelbar nächste Upgrade-Stufe
- erlaubt Materialbeiträge, Stadtaufstieg, Grenzmarkerkauf, PvE/PvP-Modus, Wartung und Gildenverwaltung
- nutzt den bestehenden Invite-Dialog statt eines Pflichtbefehls

Damit ist der normale Stadtprogressionspfad nicht mehr auf /pixelrpg guild city ... angewiesen.

# 20. Professionelle NPCs

Vorhanden:

- `ProfessionNpcProgressionService`
- `ProfessionNpcBuffService`
- `ProfessionNpcRank`
- `GrandmasterRegistry`
- `MeisterbriefService`
- `MeisterbriefSpawnListener`

Das System unterstützt:

- Apprentice
- Journeyman
- Expert
- Master
- Grandmaster
- Kosten
- Ressourcen
- Stadtlevel-Voraussetzungen
- Spezialisierung
- globale Grandmaster-Grenze für Main-Professions
- Buffs

Gathering-Grandmasters und Main-Profession-Grandmasters werden getrennt behandelt.

**Befund:** 🟢.

---

# 21. Regionen

Region-Komponenten:

- `PixelRegion`
- `RegionEditor`
- `RegionFlag`
- `RegionFlagCategory`
- `RegionFlagDialogService`
- `RegionGeometry`
- `RegionListener`
- `RegionManager`
- `RegionPolicyService`
- `RegionRepository`
- `RegionSpawnService`
- `RegionTransitionService`
- `RegionType`

Das System unterstützt:

- Regiontypen
- Flags
- Geometrie
- Spawnpunkte
- Transitionen
- Policies
- Persistenz

**Befund:** 🟢.

---

# 22. Combat

Vorhanden:

- Damage Calculator
- Damage Context
- Combat State
- Mob XP
- Mob Scaling
- Loot
- Soulbound
- Weapon Abilities

Combat-Tag und Kingdom-Combat-Mode sind integriert.

Die konfigurierten Kingdom-Werte umfassen:

- Mode Switch Delay: 300 Sekunden
- Mode Cooldown: 1800 Sekunden
- Combat Tag: 5 Sekunden

**Befund:** 🟢 statisch konsistent; echte Exploitfreigabe benötigt Laufzeittests.

---

# 23. Boss-System

Vorhanden:

- Boss Registry/Repository
- Active Boss
- Boss Manager
- Attack Patterns
- Phasen
- Loot
- Rewards
- Damage Contribution
- Schutzlistener
- Biome Spawn Task

Aktuelle Contentdaten:

- 32 Boss-Reward-Items

Mob-Scaling-Daten liegen separat in:

`mob-scaling.json`

**Befund:** 🟢.

---

# 24. Economy / Trade

Vorhanden:

- Money
- Player Economy API
- Guild Currency
- Guild Bank
- Shop
- Trade Depot
- Player Trade

Geld wird intern in Minor Units gespeichert.

Das reduziert Floating-Point-Probleme bei persistenter Währung.

## Kritischer Performance-Restpunkt

`ShopManager.save()` ruft synchron:

`yaml.save(file)`

auf.

Das kann den Serverthread blockieren.

**Priorität:** 🟠.

Empfehlung:

1. Bukkit-/Inventory-Zustand synchron snapshotten.
2. YAML-String außerhalb des Serverthreads serialisieren/schreiben.
3. Bestehenden `AsyncFileWriter` verwenden.

---

# 25. NPC-System

Vorhanden:

- NPC Manager
- RPGNpc
- Behavior Registry
- Dialog Service
- Interaction Listener
- Chunk Listener
- Look Task
- Name Visibility
- Profession Trainer
- Banker
- Quest
- Shop
- Travel
- Story
- Reception

Die Runtime ist damit fachlich stark aufgeteilt.

### Skalierungsrisiko

Bestimmte Resynchronisationspfade können gegen den gesamten bekannten NPC-Bestand arbeiten.

Bei sehr großen NPC-Mengen sollte konsequent ein Chunk-/Region-Index verwendet werden.

**Priorität:** 🟠 bei großen Serverpopulationen.

---

# 26. Externe Skin-Infrastruktur

`ExternalSkinService` verwendet:

- `textures.minecraft.net`
- MineSkin API
- externe Bildquellen

Vorhandene Schutzmechanismen:

- URL-Limit
- Response-Limit
- Connect Timeout
- Request Timeout
- Scheme-Prüfung
- Host-Prüfung
- lokale/private/reservierte Adressen werden berücksichtigt
- Kandidatenlimit
- asynchrone HTTP-Requests

Die Bukkit-Mannequin-Anwendung wird zurück auf den Serverthread verschoben.

**Befund:** 🟢/🟠.

Rest-Risiken:

- externe Verfügbarkeit
- Rate Limits
- DNS-Rebinding-/TOCTOU-Szenarien
- Bild-Decoding-Ressourcen
- externe Content-Änderungen

---

# 27. Command-System

Vorhandene Hauptbereiche umfassen:

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

Die Root-Kommandos werden über Paper Lifecycle Commands registriert.

Permissions:

- `rpg.admin`
- `rpg.member`

### Party

Die frühere Audit-Dokumentation behauptete eine fehlende `PartyGUI`.

Das ist im aktuellen `Rebuild`-Stand **nicht mehr korrekt**.

`PartySubCommand` verwendet aktuell:

`PartyDialog`

und besitzt keine `PartyGUI`-Referenz.

**Befund:** 🟢 dieser frühere Befund ist behoben.

---

# 28. Dialog-/UI-System

Native Paper-Dialoge werden für zentrale Interaktionen eingesetzt.

Zusätzlich existieren klassische Inventory-GUIs für:

- Crafting
- Questlog
- Questdetails
- Shop
- Trade Depot
- Shop Editor

Damit existieren zwei UI-Kanäle:

1. native Dialoge
2. Inventory-GUIs

Das ist technisch zulässig, sollte aber fachlich bewusst getrennt bleiben.

---

# 28.1 Quick-Actions / Berufe

Die bestehende minecraft:quick_actions-Struktur bleibt unverändert. Das vorhandene Berufe-Menü wird player-spezifisch aufgebaut und zeigt nur tatsächlich erlernte Berufe.

Unerlernte Berufe werden nicht mehr als Platzhalter oder gesperrte Einträge dargestellt.

**Befund:** 🟢 behoben.

# 29. Scoreboard / EXP-Bar

`ScoreboardService` verwendet die Vanilla-XP-Bar als PixelRPG-Fortschrittsanzeige.

Damit konkurriert PixelRPG bewusst mit Vanilla-XP-Anzeigezuständen.

Das ist kein Sicherheitsproblem.

Es ist ein Integrationspunkt:

- andere Plugins können `setExp` überschreiben
- PixelRPG kann fremde EXP-Bar-Zustände überschreiben

**Priorität:** 🟡.

---

# 30. Threading / Async-Sicherheit

Der Branch besitzt mehrere getrennte Async-Bereiche:

- Player Persistence
- File Writer
- HTTP

Die untersuchten externen HTTP-Pfade wenden Bukkit-Mutationen nicht direkt im HTTP-Thread an.

Bei persistenter Spielerlogik werden Snapshots/Revisionen verwendet.

**Befund:** 🟢.

Eine vollständige Race-Condition-Freigabe ist statisch nicht möglich; dafür sind parallele Runtime-Tests erforderlich.

---

# 31. Memory-/Leak-Forensik

Die untersuchten Runtime-Maps verwenden überwiegend:

- UUID
- primitive/immutable IDs
- Profile
- State-Objekte

Nicht als dauerhaftes Primärschlüsselmodell:

- `Player`
- `World`
- `Entity`

Temporäre Player-Referenzen in Eventmethoden sind normal.

Mehrere Quit-Listener räumen zustandsbezogene Daten auf.

**Befund:** 🟢 kein offensichtlicher zentraler Player-Leak.

---

# 32. Input- und Content-Validierung

Validiert werden unter anderem:

- UUIDs
- Level
- XP
- Money
- Material
- Profession
- Item IDs
- Rarity
- Rezeptlevel
- Mengen
- Potion-/Enchantment-Werte
- SQL-Identifier
- SSL-Modi
- URL-Schemata
- externe Response-Größen

### Restproblem

Einige YAML-Laufzeitdaten sind weniger streng typisiert/validiert als die zentralen JSON-Registries.

Das betrifft besonders dynamische Shop-/NPC-Daten.

**Priorität:** 🟡.

---

# 33. Parser-Fehler

Mehrere Loader verwenden bewusst:

`catch (IllegalArgumentException ignored)`

Das verhindert bei alten/ungültigen Daten teilweise einen kompletten Startup-Abbruch.

Forensisch entsteht dadurch aber das Risiko, dass Contentfehler still verschwinden.

Empfehlung:

- Warnung
- Datei
- Key
- erwarteter Typ
- tatsächlicher Wert

loggen.

**Priorität:** 🟡.

---

# 34. Resourcepack-System

Vorhanden:

- Resourcepack-Dateibaum
- ZIP-Archiv
- SHA1-Datei
- Join Listener
- CI-Verifikation
- Item Definition Routing
- Item Model Routing
- Texturen

`resourcepack.sha1` ist im Plugin enthalten.

Der Resourcepack-Workflow:

1. erstellt ZIP
2. berechnet SHA1
3. schreibt SHA1 in `src/main/resources/resourcepack.sha1`
4. committed Änderungen
5. pusht den Branch

### Architektur-Risiko

Der Workflow schreibt selbst in denselben Entwicklungsbranch, auf dem der Build-Workflow läuft.

Dadurch können mehrere Pushes unmittelbar hintereinander entstehen.

Die Build-Workflow-Concurrency ist zwar aktiviert, aber bei Resourcepack-Selbstcommits entstehen bewusst weitere Builds.

Das ist kein Sicherheitsproblem, aber eine CI-Komplexitätsquelle.

---

# 35. Konkreter Resourcepack-P0

Aktueller Zustand:

`resourcepack/assets/pixelrpg/items/food/suessbeeren_marmelade.json`

referenziert:

`pixelrpg:item/food/suessbeeren_marmelade`

Das referenzierte Model fehlt.

Damit schlägt der vorhandene CI-Test korrekt fehl.

### Erforderliche Entscheidung

Es gibt zwei technisch saubere Möglichkeiten:

1. **Süßbeeren-Marmelade soll weiterhin existieren:**  
   Ein tatsächliches passendes Model und eine passende tatsächliche PNG-Textur müssen vorhanden sein.

2. **Süßbeeren-Marmelade soll nicht mehr existieren:**  
   Dann müssen Itemdefinition, Item-Routing und alle Gameplay-/Rezeptreferenzen konsistent entfernt werden.

Nur das Model zu löschen, während das Item-Routing erhalten bleibt, ist kein konsistenter Endzustand.

---

# 36. Sicherheitsforensik

## Backdoor

Kein konkreter Befund für:

- versteckte OP-Freischaltung
- geheime UUID
- geheime Passwortprüfung
- versteckte Admin-Command-Kombination
- Remote-Classloading

**Befund:** 🟢.

## Remote-Code-Ausführung

Kein konkreter Mechanismus für dynamisches Nachladen ausführbaren Java-Codes gefunden.

**Befund:** 🟢.

## SQL Injection

Die geprüften Player-Queries verwenden Prepared Statements.

**Befund:** 🟢.

## SSRF

Skin-Service besitzt mehrere Schutzmaßnahmen.

**Befund:** 🟢/🟠 Rest-Risiko durch externe Netzwerk-/DNS-Komplexität.

## Dupe

Kein konkreter Dupe wurde aus der statischen Prüfung bewiesen.

Das bedeutet ausdrücklich nicht, dass parallele Trade-/Inventory-Race-Exploits ausgeschlossen sind.

**Erforderlich:** Belastungstest mit parallelen Klicks, Disconnects und gleichzeitigen Saves.

---

# 37. Legacy-API-Forensik

Die vorhandene Build-Prüfung sucht nach:

- `org.bukkit.ChatColor`
- Bungee ChatColor
- Legacy-NMS
- CraftBukkit
- statischen Player-/Entity-/World-Referenzen

Der aktuelle CI-Lauf meldete die Source-Grenzprüfung als erfolgreich.

**Befund:** 🟢.

---

# 38. Persistenzrisiken

## YAML

Positiv:

- temporäre Datei
- atomisches Move, soweit unterstützt
- Player-spezifische Dateien

## MySQL

Positiv:

- Transaktion
- Revision Lock
- Prepared Statements
- getrennte Tabellen
- Migrationen

## Restpunkt

Für einen vollständigen Produktionsfreigabetest fehlen weiterhin reale:

- Crash-Tests
- Serverkill während Save
- DB-Verbindungsabbruch
- gleichzeitige Save-Requests
- Restart-Tests

**Priorität:** 🟠 Testlücke.

---

# 39. Shutdown-Forensik

Der Lifecycle besitzt explizite Shutdown-Pfade für die wesentlichen Runtime-Systeme.

Das reduziert:

- offene Executor
- offene DB-Pools
- laufende Tasks
- persistente Runtime-Caches

**Befund:** 🟢.

---

# 40. Dokumentations-/Bestandsabweichungen

Die bisher vorhandene `audit.md` war veraltet:

- Branch `test` statt `Rebuild`
- alter Prüfstand
- alter HEAD
- 249 statt aktuell 264 Java-Dateien
- falscher Questbestand
- falscher Crafting-Bestand
- bereits behobener `PartyGUI`-Befund
- Verweis auf nicht mehr vorhandene `Forensik.md`

Dieses Dokument ersetzt den veralteten Auditstand.

---

# 41. Priorisierte Befundliste

## 🔴 P0

### F-001 — Resourcepack: verwaistes Food-Item

**Pfad:** `resourcepack/assets/pixelrpg/items/food/suessbeeren_marmelade.json`

**Problem:** Referenziertes Model fehlt.

**Auswirkung:** Resourcepack-Verifikationsschritt schlägt fehl.

**Nachweis:** aktueller Repository-Baum + aktueller GitHub-Actions-Lauf.

**Status:** offen.

---

## 🟠 P1

### F-002 — Shop YAML synchron

**Pfad:** `ShopManager.save()`

**Problem:** `yaml.save(file)` auf dem Serverthread.

**Auswirkung:** mögliche Main-Thread-Spikes bei größeren Dateien oder häufigen Änderungen.

**Status:** offen.

### F-003 — Restart-/Crash-Persistenz nicht vollständig verifiziert

**Problem:** statische Prüfung zeigt robuste Save-Mechanismen, aber keinen echten Serverkill-/Restart-Test.

**Status:** Testlücke.

### F-004 — NPC-Skalierung

**Problem:** einzelne Resync-Pfade konnten global über bekannte NPCs arbeiten.

**Status:** 🟢 behoben. Login-Resync verwendet jetzt den vorhandenen Chunk-Index und prüft nur den lokalen 3×3-Chunk-Bereich.

### F-005 — Runtime-YAML-Schema

**Problem:** weniger strenge Validierung als zentrale JSON-Contentdaten.

**Status:** offen.

---

## 🟡 P2

### F-006 — Parserfehler teilweise still

`IllegalArgumentException` wird an mehreren Stellen bewusst ignoriert.

### F-007 — EXP-Bar als reservierter UI-Kanal

PixelRPG übernimmt die Vanilla-XP-Bar.

### F-008 — Resourcepack-Selbstcommit

Der Resourcepack-Workflow pusht automatisch neue Commits in `Rebuild`.

### F-009 — Externe Skinprovider

Externe Netzwerkabhängigkeit bleibt ein Betriebs-/Verfügbarkeitsrisiko.

### F-010 — Parallel-/Race-Tests fehlen

Insbesondere:

- Trade
- Inventory
- Guild Territory
- Player Save
- DB Revision

---

# 42. Positivbefunde

| Bereich | Status |
|---|---|
| Java 25 | 🟢 |
| Paper 26.2 | 🟢 |
| Paperweight | 🟢 |
| paper-plugin.yml | 🟢 |
| Mojang Production Mapping | 🟢 |
| Shadow Relocation | 🟢 |
| Source Boundary Checks | 🟢 |
| Artifact Checks | 🟢 |
| Native Dialogs | 🟢 |
| UUID Player State | 🟢 |
| YAML Atomic Save | 🟢 |
| MySQL Transactions | 🟢 |
| Prepared Statements | 🟢 |
| Revision Locking | 🟢 |
| Lifecycle Shutdown | 🟢 |
| Guild Territory Polygon | 🟢 |
| Region/Guild binding | 🟢 |
| Profession separation | 🟢 |
| Quest system | 🟢 |
| Boss system | 🟢 |
| Companion system | 🟢 |
| External skin hardening | 🟢/🟠 |
| Resourcepack | 🔴 |

---

# 43. Abschlussbewertung

Der aktuelle `Rebuild`-Branch ist technisch bereits ein umfangreiches, modularisiertes Paper-26.2-RPG-System.

Der wichtigste aktuelle Blocker ist **nicht der Java-Build**, sondern die Resourcepack-Konsistenz.

Die CI-Evidenz zeigt:

> Java kompiliert. Source-Grenzen bestehen. Shadow-Artefakt besteht. Resourcepack-Prüfung besteht noch nicht.

Die Architektur besitzt außerdem einige mittlere technische Restpunkte:

- synchrones Shop-I/O
- fehlende vollständige Crash-/Restart-Verifikation
- globale NPC-Resync-Kosten
- teilweise schwache Runtime-YAML-Schemata
- externe Skinprovider
- fehlende parallele Race-/Dupe-Tests

Es wurde **kein konkreter Backdoor-, Legacy-NMS-, CraftBukkit-, ChatColor- oder SQL-Injection-Befund** in den geprüften Bereichen festgestellt.

---

# 44. Empfohlene Reihenfolge für die weitere Entwicklung

1. **F-001 beheben:** `suessbeeren_marmelade` konsistent aus Item → Model → Texture → Gameplay bringen.
2. Resourcepack-CI erneut ausführen.
3. Danach vollständigen Gradle-/Artifact-Build erneut prüfen.
4. Restart-/Crash-Persistenz testen.
5. Trade-/Inventory-Race-Tests durchführen.
6. Shop-Speicherung auf Snapshot + Async Write umstellen.
7. Runtime-YAML-Schema-Validierung ergänzen.
8. NPC-Resync mit Chunk-/Region-Index profilieren.
9. Externe Skin-Infrastruktur weiter härten.
10. Erst danach die nächste größere Feature-Erweiterung aufsetzen.

---

## Audit-Grenzen

Dieses Audit ist eine **statische Bestandsforensik des Repository-Zustands**.

Nicht beweisbar allein durch Repository-Analyse sind insbesondere:

- echte TPS unter Last
- JVM-Memory-Verhalten über lange Laufzeiten
- alle Race Conditions
- alle Inventory-/Trade-Dupes
- World-/Chunk-Performance
- reale Server-Restarts
- Datenverlust bei hartem Prozesskill
- tatsächliches Spielerlebnis
- reale Minecraft-Weltbevölkerung
- Netzwerkverhalten unter Provider-Ausfall

Diese Punkte sind als Testlücken und nicht als bewiesene Fehler zu behandeln.

**Audit-Stand:** HEAD `f94c9500f6e45d6a4b4302a5d3cf1e3001186521` auf `Rebuild`.
