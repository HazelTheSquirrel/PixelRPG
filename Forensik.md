# PixelRPG – Forensische Code- und Architekturprüfung

**Prüfobjekt:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `test`  
**Prüfstand:** `5a479daa6e44d640aa42f51d4060ada99767b296`  
**Prüfdatum:** 2026-09-24  
**Zielplattform laut Projekt:** Java 25 / Paper 26.2 / Mojang-Mappings  
**Prüfart:** statische Repository-Forensik, Architektur-, Concurrency-, Persistenz-, Sicherheits- und Feature-Audit

> Diese Prüfung bewertet ausschließlich den tatsächlich vorliegenden Code und die Daten des aktuellen `test`-Branches. Kein Commit-History-Befund wird als Begründung für ein technisches Urteil verwendet. Ein laufender Minecraft-Server wurde nicht instrumentiert; Runtime-Fuzzing, TPS-Messung und tatsächliche Ingame-Reproduktion sind daher nicht Bestandteil dieses Audits.

---

# 1. Executive Summary

## Gesamtbefund

**Statischer Stabilitäts-/Sicherheitswert: 7,5 / 10**

Das Projekt ist kein einfaches Plugin-Grundgerüst, sondern ein umfangreich modularisiertes MMORPG-System. Die technische Architektur ist überwiegend sauber: Fachbereiche sind getrennt, Persistenz ist größtenteils snapshot-basiert, UUIDs werden statt dauerhafter Player-Referenzen in den wichtigsten Laufzeit-Maps verwendet, Shutdowns sind weitgehend koordiniert und das Build-System besitzt eigene Boundary- und Shadow-Verifikationen.

Es gibt jedoch mehrere konkrete Befunde, die vor einem als vollständig abgesichert zu bezeichnenden Produktionsstand behoben bzw. getestet werden sollten:

### Hohe Priorität

1. **NMS-/Packet-Reflection ist stark versionsgebunden.**  
   `NpcNameVisibilityService` referenziert per String die internen Klassen `net.minecraft.world.entity.Entity`, `net.minecraft.network.syncher.*` und `ClientboundSetEntityDataPacket`. Zusätzlich werden interne Felder und die Connection-Implementierung reflektiert. Das funktioniert als bewusst gewählte Implementierung, ist aber nicht zukunftssicher.
2. **MySQL-Fallback kann eine bereits geöffnete Hikari-Datenbankverbindung offen lassen.**  
   In `PlayerProfileManager.initialize()` wird bei einem Fehler nach `DatabaseManager.connect()` auf YAML zurückgefallen. Der bereits erzeugte `DatabaseManager` wird in diesem Catch-Pfad nicht explizit heruntergefahren.
3. **Synchrones JSON-Schreiben existiert noch in einem Laufzeitpfad.**  
   `JsonDataManager.save()` schreibt synchron. `UniqueItemService.save()` ruft diese Methode aus synchronisierten Gameplay-Methoden auf. Der Dienst wird im aktuellen `PixelRPGPlugin` allerdings nicht instanziiert und ist daher derzeit kein nachgewiesener aktiver TPS-Pfad.
4. **Quest-Navigation kann teure World-Locator-Aufrufe auslösen.**  
   `QuestManager.checkReachLocationQuests()` kann `locateNearestStructure()` bzw. `locateNearestBiome()` aufrufen. Der Passive-Quest-Listener wird bei Blockwechseln geweckt. Für diese direkte Prüfung existiert nicht dieselbe 30-Sekunden-Zielcache-Schicht wie in `QuestNavigationService`.

### Mittlere Priorität

5. **Externe Skin-Auflösung ist sicherheitsbewusst, bleibt aber ein Netzwerk-Trust-Boundary.**  
   `ExternalSkinService` validiert URL, Host, private/local addresses, Antwortgröße und finale URI. Wegen DNS-Auflösung und tatsächlichem HTTP-Zugriff bleibt prinzipbedingt ein TOCTOU-/DNS-Rebinding-Restfenster.
6. **NMS-Reflection ist durch das bestehende Build-Gate nicht verboten.**  
   Die aktuelle Regex-Prüfung erkennt Legacy-NMS-Pakete, aber nicht moderne reflektierte interne NMS-Klassen. Das ist kein Sicherheitsproblem an sich, sollte aber als bewusst erlaubte Sonderzone dokumentiert und separat geprüft werden.
7. **Command-Kompatibilitätsadapter enthalten weiterhin Bukkit-`CommandExecutor`/`TabCompleter`.**  
   Die eigentliche Registrierung erfolgt modern über Paper `LifecycleEvents.COMMANDS` und `BasicCommand`; intern existiert jedoch noch die Adapter-Schicht.
8. **`DebugSubCommand` verwendet `pixelrpg.admin`, während `paper-plugin.yml` und die übrigen Admin-Kommandos `rpg.admin` verwenden.**  
   Dadurch ist der Debug-Unterbefehl praktisch von einer nicht deklarierten Permission abhängig.
9. **CI prüft Build/Artifact, aber keinen gestarteten Paper-Server.**  
   Im aktuellen Workflow gibt es keinen Server-Smoke-Test und keinen automatisierten Login-/GUI-/Dialog-/Persistenz-Reboot-Test.

### Niedrige Priorität

10. Doppelte `PlayerProfile`-Imports in `MannequinSkinResolver`.
11. Mehrere sehr ausführliche INFO-Logs im Skin-Pfad sind für normalen Produktionsbetrieb relativ laut.
12. Einige alte/erklärende Kommentare spiegeln frühere Implementierungsstände wider.

---

# 2. Projektgröße und Architektur

Der aktuelle Branch enthält **247 Java-Dateien** unter `src/main/java`.

Wesentliche Domänen:

- API / Events
- Bootstrap / Lifecycle
- Player / Profile / Storage
- Items / Food / Equipment
- Profession / Crafting
- Quest / Navigation
- Combat / Skills / Scaling
- Boss
- Companion
- NPC / Skin / Behavior
- Dialogue
- GUI
- Party
- Guild / Bank
- Region
- Shop / Economy
- Trade Depot
- Story
- Statistics / Scoreboard
- Travel

Die Trennung ist für die Projektgröße angemessen. `PixelRPGPlugin` ist eine große Composition Root, enthält aber überwiegend Initialisierung und Dependency-Wiring statt komplette Fachlogik.

**Befund: PASS**

---

# 3. Build- und Plattformkonformität

## Gradle

Der Branch verwendet:

- Paperweight Userdev `2.0.0-beta.21`
- Paper Dev Bundle `26.2.build.121-stable`
- Shadow `9.6.1`
- Java 25
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

## Plugin-System

`paper-plugin.yml` ist vorhanden und enthält:

- `name: PixelRPG`
- `main: de.pixelrpg.rpg.PixelRPGPlugin`
- `bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap`
- `api-version: '26.2'`

Das alte `plugin.yml`-System ist nicht die primäre Pluginbeschreibung.

## ShadowJar

Die drei vorgesehenen Relocations sind vorhanden:

- `com.google.gson` → `de.pixelrpg.rpg.libs.gson`
- `com.zaxxer.hikari` → `de.pixelrpg.rpg.libs.hikari`
- `com.mysql` → `de.pixelrpg.rpg.libs.mysql`

Zusätzlich wird der JDBC-Service-Eintrag kontrolliert.

## Source Boundary

`build.gradle` und CI verbieten explizit:

- `ChatColor`
- Legacy-NMS `net.minecraft.server.v...`
- CraftBukkit
- definierte statische Player-/Entity-/World-Referenzen

Im geprüften aktuellen Sourcebestand wurde kein entsprechender Legacy-Befund festgestellt.

**Befund: PASS**

### Einschränkung

Die Prüfungen sind Regex-/Artifact-Gates und keine vollständige semantische Analyse. Insbesondere moderne NMS-Reflection wird dadurch nicht erfasst.

---

# 4. Event-Handling und Lifecycle

`PixelRPGPlugin.onEnable()` registriert die Listener explizit. Dazu gehören unter anderem:

- Player Profile Lifecycle
- Region
- NPC Chunk / Interaction
- Equipment
- GUI
- Crafting
- Stats
- Skills
- Loot
- Mob Scaling
- Combat
- Boss
- Quest
- Party
- Guild Currency
- Guild Compass
- Soulbound
- Scoreboard
- Playtime
- Dialog Quick Actions
- Companion Experience

Das ist nachvollziehbar und verhindert einen versteckten Listener-Registry-Mechanismus.

## Shutdown

`LifecycleCoordinator` wird beim Disable geschlossen. Registrierte Ressourcen werden unter anderem für:

- PlayerProfileManager
- CompanionService
- PartyManager
- GuildManager
- RegionManager
- NpcManager
- BossManager
- QuestManager
- Scoreboard
- Playtime
- Mob Scaling
- Boss Spawn
- Quest Passive Checks
- Region Editor
- Region Spawn
- Banker

beendet.

Dynamisch registrierte Listener wie `NpcLookTask` und `QuestPassiveCheckTask` besitzen explizite Stop-/Unregister-Pfade.

**Befund: gut**

## Event-Prioritäten

Viele Region-Regeln laufen auf `HIGHEST`. Das ist funktional nachvollziehbar, kann aber bei Interoperabilität mit anderen Plugins bewusst eingegriffenes Verhalten bedeuten.

**Risiko: niedrig bis mittel, abhängig vom Plugin-Ökosystem.**

---

# 5. Scheduler und Threading

## Positiv

Persistenz wurde weitgehend aus dem Serverthread herausgezogen:

- Player Profile I/O über Executor
- NPC-Persistenz über eigenen Executor
- AsyncFileWriter für atomare Datei-Snapshots
- Party-Persistenz über Executor
- Trade Depot über AsyncFileWriter
- Skin-Netzwerkzugriffe über `HttpClient.sendAsync()`

Die Architektur trennt Serverthread-Aktionen von Datei-/Netzwerk-I/O überwiegend sauber.

## Player Profile

`PlayerProfileManager` verwendet:

- `ConcurrentHashMap<UUID, ...>`
- per-Spieler Save-Chains
- Snapshot-Erzeugung vor Persistenz
- Mutation-/Persistence-Revisions
- Shutdown Flush

Das ist ein solides Muster gegen verlorene Zwischenstände und konkurrierende Saves.

## Datenbank

SQL wird über Prepared Statements ausgeführt. Transaktionen werden beim Speichern explizit mit Commit/Rollback behandelt.

**Befund: PASS**

## Befund: MySQL-Fallback-Ressource

Problem:

1. `databaseManager = new DatabaseManager()`
2. `databaseManager.connect(config)`
3. später schlägt `repository.init()` oder ein anderer Initialisierungsschritt fehl
4. Catch wechselt auf YAML
5. `databaseManager.shutdown()` wird in diesem Pfad nicht aufgerufen

Das kann einen Hikari-Pool offen lassen, bis der Prozess endet.

**Priorität: P1**

---

# 6. Speicherlecks

Die wichtigen Laufzeit-Maps verwenden überwiegend UUID-Schlüssel:

- `PlayerProfileManager`
- `StatEngine`
- `CombatStateService`
- `WeaponAbilityEngine`
- `PartyManager`
- `ScoreboardService`
- `PlaytimeTracker`
- `CompanionService`
- `TradeDepotManager`
- `QuestNavigationService`

Dauerhafte `Map<UUID, Player>`-Muster wurden in den geprüften relevanten Services nicht festgestellt.

Player-Objekte werden lokal gehalten und bei Bedarf über UUID wieder aufgelöst.

## Cleanup

Es existieren explizite Quit-/Shutdown-Cleanups, beispielsweise für:

- Combat
- Scoreboard
- Playtime
- Quest Navigation
- Party
- Companion Runtime
- NPC Look State

**Befund: gut**

### Restrisiko

UUID-Keyed Maps sind nicht automatisch leak-frei. Jede solche Map muss bei Quit/Shutdown entfernt werden. Die relevanten Systeme tun dies überwiegend.

---

# 7. Performance

## NPC Look System

`NpcLookTask` ist event-driven und verwendet keinen permanenten Tick-Loop mehr. Bei Bewegung wird nur bei Blockwechseln gearbeitet.

Der Look-Target-Code iteriert bei einem relevanten NPC über Online-Spieler.

Bei sehr vielen NPCs und Spielern kann das quadratisch wirken, ist durch den Radius aber räumlich begrenzt.

## Companion Combat

Companion Combat verwendet lokale `getNearbyEntities()`-Abfragen. Es gibt keine Suche über alle geladenen Chunks.

## Weapon Abilities

Area-Abilities verwenden `getNearbyEntities()` in kleinen Radien.

**Befund: akzeptabel**

## Quest Navigation

Hier besteht der relevanteste Performance-Hinweis.

`QuestNavigationService` besitzt einen guten Cache:

- 30 Sekunden TTL
- maximal 2048 Einträge
- Chunk-/Quest-basierter Cache-Key

`QuestManager.checkReachLocationQuests()` führt dagegen für REACH_LOCATION separat `locateNearestStructure()` oder `locateNearestBiome()` aus.

Der Aufruf kann über `QuestPassiveCheckTask.onMove()` bei Blockwechseln ausgelöst werden.

**Empfehlung:** dieselbe Target-Resolution-/Cache-Schicht auch für die Completion-Prüfung verwenden.

**Priorität: P2**

---

# 8. Datei-I/O

## Gut

`AsyncFileWriter`:

- Single-Thread-Writer
- Snapshot-Coalescing
- atomisches Move
- temporäre Dateien
- Shutdown Flush
- Race-Recheck beim Übergang in den Idle-Zustand

Das ist sauber implementiert.

## YAML Profile

Spielerprofile werden über den Profile-Executor gespeichert und atomisch ersetzt.

## Region

Regionen werden atomisch geschrieben, aber die Repository-Methoden selbst sind synchron. Der Manager führt die Speicherung in der vorgesehenen Architektur kontrolliert aus.

## JSON

`JsonDataManager.load()` ist bewusst synchron und wird für statische Content-Daten beim Start verwendet. Das ist unproblematisch.

`JsonDataManager.save()` ist dagegen synchron.

Der aktuelle `UniqueItemService` ruft `save()` synchron auf. Dieser Service ist im aktuellen `PixelRPGPlugin` nicht sichtbar verdrahtet und damit aktuell kein nachgewiesener aktiver Laufzeitpfad.

**Priorität: P2 als Architektur-Härtung, P3 solange ungenutzt.**

---

# 9. Netzwerk / externe Ressourcen

Der relevante externe Netzwerkpfad ist:

`ExternalSkinService`

Er verwendet Java `HttpClient`.

Positiv:

- URL-Längenlimit
- nur HTTP/HTTPS
- Host-Prüfung
- private/local address blocking
- Antwortgrößenlimit
- Prüfung der finalen URI
- Candidate-Limit
- asynchroner HTTP-Zugriff
- In-Flight-Caching
- Fehlerbehandlung

Es gibt keine erkennbare Webhook-/Telemetry-/Command-and-Control-Struktur.

## SSRF-Risiko

Die Schutzmaßnahmen sind deutlich vorhanden. Ein DNS-Rebinding-/TOCTOU-Restproblem bleibt jedoch prinzipbedingt möglich, wenn Validierung und tatsächliche Verbindung nicht auf derselben bereits validierten IP-Verbindung beruhen.

**Priorität: P2**

---

# 10. Backdoor- und Schadcode-Audit

Im geprüften aktuellen Codebestand gibt es keine erkennbare Implementierung von:

- `Runtime.getRuntime().exec()`
- `ProcessBuilder`
- dynamischem Shell-Aufruf
- verstecktem Plugin-/JAR-Laden
- offensichtlicher Base64-Obfuskation als Code-Ausführung
- Webhook-/Discord-Command-Channel
- versteckter OP-Vergabe
- offensichtlichem UUID-basiertem Admin-Bypass

Die vorhandene Reflection dient dem NPC-Nameplate-Paketpfad und ist funktional erklärbar.

**Forensischer Befund: kein offensichtlicher Backdoor-/RCE-Mechanismus.**

Das ist eine statische Feststellung, keine mathematische Garantie gegen unbekannte Laufzeit- oder Dependency-Angriffe.

---

# 11. NMS / Packet / Reflection

## Tatsächliche Verwendung

`NpcNameVisibilityService` verwendet Reflection auf:

- `net.minecraft.world.entity.Entity`
- `net.minecraft.network.syncher.EntityDataAccessor`
- `net.minecraft.network.syncher.SynchedEntityData$DataValue`
- `net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket`

Zusätzlich wird über die Connection des Players ein interner Packet-Send-Pfad gesucht.

## Bewertung

Das ist **kein Legacy-NMS-Paket** im Sinne von `net.minecraft.server.v1_XX_RY`.

Es ist jedoch **internes NMS**, nur dynamisch aufgelöst.

Damit besteht ein hohes Upgrade-Risiko:

- Feldnamen können sich ändern
- interne Packet-Konstruktoren können sich ändern
- Synched-Entity-Data-Implementierung kann sich ändern
- Connection-Struktur kann sich ändern
- Reflection kann ohne Compile-Time-Hinweis erst zur Laufzeit brechen

Paper 26.2 stellt weiterhin öffentliche Profile-/Mannequin-/Entity-APIs bereit; die Reflection wird hier speziell für individuelle Name-Visibility-Metadaten eingesetzt.

**Priorität: P1 für Upgrade-Härtung, nicht als akuter Exploit.**

---

# 12. Skin-/Profile-API

`MannequinSkinResolver` nutzt `com.destroystokyo.paper.profile.PlayerProfile` und `ProfileProperty`. Diese API existiert in Paper 26.2 weiterhin, ist also nicht per se ein Legacy-Fehler.

Auffällig sind allerdings:

- doppelte `PlayerProfile`-Imports
- Mischung aus altem Paper-Profile-Namespace und aktuellem `ResolvableProfile`
- Reflection nur im Name-Visibility-Teil

Die Paper-26.2-Dokumentation führt die `com.destroystokyo.paper.profile`-Typen weiterhin. Die API ist daher aktuell vorhanden; eine zukünftige Bereinigung in Richtung der inzwischen stärker zentralisierten Profile-APIs wäre trotzdem wartungsfreundlicher.

---

# 13. Datenvalidierung

## JSON

`JsonDataManager` validiert:

- Pfad darf Data Directory nicht verlassen
- Root muss JSON-Object sein
- Rezeptdatei besitzt Reparaturpfad
- inkompatible Crafting-Rezepte werden gesichert und durch Default-Daten ersetzt

## Items

`ItemDefinitionRegistry` validiert die geladenen Itemdefinitionen.

## Crafting

`CraftingRecipeRegistry` validiert:

- Dependencies
- Circular Dependencies
- Spezialfelder
- Material-/Kostenwerte
- Verzauberungen/Tränke

## Quests

`QuestRepository` validiert:

- IDs
- Quest-Typen
- Level
- Professionen
- Referenzen
- Prerequisites
- Follow-Ups
- Zyklusbedingungen

## SQL

Spielerparameter werden per Prepared Statement gebunden.

**Befund: gut**

---

# 14. Inventory-/Dupe-Audit

Die GUI-Schicht verwendet Holder-basierte Identifikation und annulliert GUI-Klicks zentral.

Companion Equipment behandelt explizit problematische Actions:

- COLLECT_TO_CURSOR
- CLONE_STACK
- MOVE_TO_OTHER_INVENTORY
- UNKNOWN

Zusätzlich werden Slot-Kompatibilität und Mengen angepasst.

Guild Bank prüft Membership und blockiert unberechtigte Click-/Drag-Aktionen.

Trade Depot verifiziert beim Einstellen:

- aktuellen Inventarslot
- RPG-Item-Status
- Item-Similarity
- Stapelgröße

Beim Kauf:

- Listing muss existieren
- Ablauf wird geprüft
- Eigenkauf wird verhindert
- Käuferprofil muss existieren
- Platz im Handelsfach wird geprüft
- Geld wird abgezogen
- Listing wird entfernt
- Verkäufer erhält Geld oder Pending Payout
- bei fehlgeschlagener Einlagerung wird der Vorgang zurückgerollt

**Befund: kein offensichtlicher einfacher Dupe-Pfad im geprüften Handels-/GUI-Code.**

### Restrisiko

Inventory-Exploits lassen sich ohne Client-/Packet-Fuzzing nicht vollständig ausschließen.

---

# 15. Fehlerbehandlung

Das Projekt verwendet wesentlich mehr Fehlerbehandlung als ein typisches Minecraft-Plugin.

Positiv:

- Exceptions werden in Persistenzpfaden geloggt
- DB-Transaktionen rollbacken
- AsyncFileWriter räumt temporäre Dateien auf
- Executor-Shutdowns behandeln Interrupts
- ungültige Content-Einträge werden teilweise übersprungen
- ungültige Enum-/UUID-/Materialwerte werden abgefangen

## Schwäche

Teilweise werden `IllegalArgumentException` bewusst ignoriert, beispielsweise bei einzelnen Content-/Equipment-Feldern.

Das ist bei user-editierbaren Daten sinnvoll, kann aber bei Produktionsproblemen die genaue Ursache verschleiern.

**Empfehlung:** bei Content-Parsing zwischen erwarteten ungültigen Werten und strukturellen Datenfehlern unterscheiden und strukturierte Warnungen mit Datei + ID + Feld ausgeben.

---

# 16. Commands und Permissions

## Registrierte Commands

- `/pixelrpg`
- `/pixelrpgparty`
- `/pixelrpgquestlog`
- `/pixelrpgdialogue`

Legacy-/Kompatibilitäts-Aliase:

- `/rpgadmin`
- `/rpgparty`
- `/questlog`
- `/dialogue`

## Root Subcommands

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

## Permissions

`paper-plugin.yml` definiert:

- `rpg.admin`
- `rpg.member`

Die meisten Admin-Kommandos verwenden korrekt `rpg.admin`.

### Konkreter Fehler

`DebugSubCommand.permission()` gibt:

`pixelrpg.admin`

zurück.

Diese Permission ist in `paper-plugin.yml` nicht definiert. Damit ist der Debug-Unterbefehl inkonsistent zum restlichen Permission-System.

**Priorität: P1 – einfache Korrektur.**

---

# 17. Konfiguration

`config.yml` enthält unter anderem:

## Storage

- YAML/MySQL
- Save Executor Threads
- Load Timeout
- MySQL Host
- Port
- Datenbank
- Username
- Password
- Pool Size
- Connection Timeout
- SSL Mode

## Items/Economy

- Soulbound-Kosten
- Soulbound-Minlevel
- Loot Drop Chance
- Currency Drop Chance
- Currency Min/Max
- Bank Quick Amount
- Currency Stack Size

## Combat

- Boss Max Hit Percent

## Story

- Zeichen pro Zeile
- Zeilen pro Seite

## Mob Scaling

- Nameplate-Dauer
- XP pro Max Health
- Damage pro Level
- HP pro Level
- Player-Parity-Multiplikator
- Dimension-Multiplikatoren
- Level Offsets
- Base Levels

## NPC

- Look Radius
- Nameplate Radius
- Look Interval

## Quest

- Party Share Range

## Boss

- Bar Radius
- Bar Update Interval
- Phase Check Interval
- Spawn Radius
- Spawn Interval
- Max Concurrent

## Statistics / Scoreboard

- Autosave Interval
- Scoreboard Update Interval

---

# 18. Feature-/Content-Audit

## Items

**29 Itemdefinitionen**

Unterstützt werden:

- Waffen
- Rüstung
- Rarities
- Item-Level
- Required Level
- Gearscore
- Soulbound
- Unique
- Equipment Slots
- Set-Zuordnung
- Resourcepack IDs
- Weapon Abilities

## Food

**23 Fooddefinitionen**

## Equipment Sets

**6 Sets**

## Crafting

**300 Rezeptdefinitionen**

Berufe:

- Blacksmith
- Scholar
- Farmer
- Cook
- Tailor
- Alchemist
- Mason

## Passive Professionen

- Fisherman
- Woodcutter

**9 Professionen insgesamt**

## Quests

**269 Questdefinitionen**

Verwendete Questklassen/-mechaniken:

- HUNT
- COLLECT
- GLOBAL_EVENT
- TALK_TO_NPC
- REACH_LOCATION
- Prerequisites
- Follow-Up-Quests
- Level Requirements
- Profession Requirements
- Quest Navigation
- Quest Log
- Mob-Kills
- Passive Checks
- Rewards
- Companion Unlocks

## Companions

**28 Companiondefinitionen**

Mechaniken:

- Rarity
- Level
- XP
- Progression
- Follow
- Combat
- Passive Stats
- Equipment
- Mount
- Rename
- Boss Unlocks
- Quest Unlocks

## Bosses

Der Code unterstützt:

- Biome Bosses
- World Bosses
- Phasen
- Attack Patterns
- Slam
- Summon Adds
- Projectile Volley
- Enrage
- Damage Contribution
- Boss Bar
- Loot
- Reward Items
- Spawn Limits
- Boss Protection
- Scaling

## NPC

Unterstützt:

- Reception
- Quest
- Shop
- Travel
- Story
- Banker
- Filler
- 9 Profession Trainer-Typen

## Dialoge

Vorhanden:

- Character Card
- Profile
- Quests
- Companions
- Professions
- Guild
- Bank
- Companion
- Profession
- Reception
- Travel
- Story
- Quick Actions

## Guild

- Create
- Invite
- Accept
- Leave
- Info
- Disband
- Guild Bank
- Guild Currency
- Guild Compass
- API

## Party

- Invite
- Accept
- Leave
- Kick
- Transfer
- Disband
- Info
- Quest Sharing

## Region

- Polygon-/Geometry-System
- Region Flags
- Ownership
- Members
- Properties
- Spawn Points
- Region Types
- World-wide Global Regions
- Editor
- Dialog-basierte Flag-Verwaltung

## Shop

- Buy
- Sell
- Shop Editor
- NPC-Zuordnung
- Preis-Persistenz

## Trade Depot

- Listings
- 7-Tage-Laufzeit
- 5-%-Gebühr
- Kauf
- Verkauf
- Pending Payout
- Ablauf
- Persistenz

## Story

- Story Manager
- Chapters
- Story Progress
- Story NPC Behavior
- XP Rewards

## Statistics / Scoreboard

- Mob Kills
- Player Deaths
- Boss Statistics
- Playtime
- Scoreboard
- Quest-/Party-HUD-Status

---

# 19. Forensische Sicherheitsbewertung

| Bereich | Befund |
|---|---|
| Backdoor / RCE | kein offensichtlicher Befund |
| Shell Execution | kein offensichtlicher Befund |
| Webhook / C2 | kein offensichtlicher Befund |
| Hardcoded Admin UUID | kein Befund in den geprüften Pfaden |
| SQL Injection | Prepared Statements vorhanden |
| SSRF | Schutz vorhanden, Rest-Risiko |
| File Traversal | JSON-Pfade geschützt |
| Inventory Dupes | kein einfacher offensichtlicher Pfad |
| Player Reference Leaks | überwiegend UUID-basiert |
| Reflection | vorhanden, aber gezielt für NPC-Packet-Pfad |
| NMS | indirekt per Reflection |
| Async I/O | überwiegend sauber |
| Shutdown | überwiegend sauber |

---

# 20. Priorisierter Action Plan

## P1 – zuerst beheben

### P1.1 Debug-Permission korrigieren

Datei:

`src/main/java/de/pixelrpg/rpg/command/impl/DebugSubCommand.java`

`pixelrpg.admin` auf das im Projekt verwendete Admin-Recht `rpg.admin` vereinheitlichen.

### P1.2 MySQL-Fallback schließen

Datei:

`src/main/java/de/pixelrpg/rpg/player/PlayerProfileManager.java`

Bei einem Fehler nach erfolgreicher Erstellung/Verbindung des `DatabaseManager` diesen vor dem YAML-Fallback explizit schließen.

### P1.3 NMS-Reflection isolieren

Datei:

`src/main/java/de/pixelrpg/rpg/npc/NpcNameVisibilityService.java`

Die Reflection in eine klar abgegrenzte Paper-26.2-Kompatibilitätsschicht verschieben und einen kontrollierten Fallback definieren.

Zusätzlich sollte ein automatisierter Start-/Smoke-Test prüfen, dass der Name-Visibility-Pfad tatsächlich funktioniert.

---

## P2 – danach

### P2.1 Quest-Locator-Cache vereinheitlichen

Die Completion-Prüfung und die Navigation sollten dieselbe gecachte Zielauflösung verwenden.

### P2.2 ExternalSkinService weiter härten

- DNS-Auflösung kontrollierter behandeln
- Netzwerkziel nach Redirect erneut strikt validieren
- Timeout-/Rate-Limit-Strategie dokumentieren
- optional zentralen HTTP-Service verwenden

### P2.3 CI-Smoke-Test

CI sollte zusätzlich:

1. Artifact bauen
2. Paper 26.2 starten
3. Plugin laden
4. Serverstart ohne Exception prüfen
5. Plugin-Disable sauber prüfen

### P2.4 Persistenz-Reboot-Test

Automatisiert prüfen:

- Player Profile
- NPC
- NPC Skin
- Quest Progress
- Companion
- Guild
- Party
- Trade Depot
- Region

über einen vollständigen Stop/Start-Zyklus.

---

## P3 – Wartbarkeit

### P3.1 Gradle Wrapper

Einen festen Gradle Wrapper ins Repository aufnehmen.

### P3.2 Content-Linter

Automatisiert prüfen:

- Quest → NPC
- Quest → Item
- Quest → Mob
- Recipe → Item
- Companion → Reward
- Boss → Reward
- Resourcepack → Item ID

### P3.3 Reflection-Logging reduzieren

Normale Skin-/NPC-Erfolgsmeldungen eher auf Debug-Level bzw. weniger ausführliche Logs reduzieren.

### P3.4 Code-Hygiene

Doppelte Imports und veraltete Kommentare entfernen.

---

# 21. Schlussurteil

**Statischer Gesamtwert: 7,5 / 10**

Das Plugin ist architektonisch deutlich über einem typischen Minecraft-Plugin-Prototypen. Die wichtigsten positiven Eigenschaften sind:

- moderne Paper-Plugin-Struktur
- Java 25
- Paper 26.2
- native Dialog-Registrierung
- klare Domänenmodule
- Prepared Statements
- Snapshot-Persistenz
- Async File I/O
- kontrollierte Executor-Lifecycles
- UUID-basierte Runtime-Zustände
- umfangreiche Input-/Content-Validierung
- Shadow-/Artifact-Verifikation
- kein offensichtlicher Backdoor-/RCE-Mechanismus

Die größten verbleibenden Risiken liegen nicht in offensichtlichem Schadcode, sondern in **Betriebs- und Upgrade-Robustheit**:

1. interne NMS-Reflection,
2. DB-Fallback-Lifecycle,
3. World-Locator-Kosten,
4. fehlende End-to-End-Smoke-/Restart-Tests,
5. externe Skin-Netzwerkgrenze.

## Forensisches Fazit

**Kein offensichtlicher Schadcode oder Backdoor-Befund.**  
**Keine offensichtliche einfache SQL-Injection oder klassische Player-Objekt-Memory-Leak-Struktur.**  
**Die Persistenzarchitektur ist überwiegend solide.**  
**Die größte technische Schwachstelle ist die bewusste, aber stark versionsgebundene NMS-Reflection im NPC-Name-Visibility-System.**

Das Projekt ist damit statisch **brauchbar bis fortgeschritten produktionsnah**, aber noch nicht vollständig durch automatisierte Runtime-, Upgrade- und Restart-Tests abgesichert.

---

# 22. Prüfgrenzen

Nicht durch diesen Audit nachgewiesen:

- reale TPS unter Last
- tatsächliche Client-Packet-Fuzzing-Ergebnisse
- Exploitbarkeit durch modifizierte Minecraft-Clients
- Verhalten bei 100+ gleichzeitigen Spielern
- Verhalten bei Servercrash während eines Writes
- tatsächliche MySQL-Netzwerkfehler unter Last
- tatsächliche Paper-26.2-Serverstart-Reproduktion in dieser Prüfung
- Laufzeitverhalten externer Skin-Server

Diese Punkte benötigen reproduzierbare Runtime-/Integrationstests und werden nicht aus statischem Code allein als Fakt behauptet.


---

# 23. Post-Forensik Refactoring / Hardening Pass

**Ausgangslage:** Das Plugin ist funktional bereits als SOLL-Zustand verifiziert. Deshalb wurden ausschließlich Änderungen vorgenommen, die die beobachtete Spielerfunktion nicht verändern sollen. Es wurden keine Features, Commands, Rezepte, Questdefinitionen, Dialoge, NPC-Daten, Rewards oder Spielregeln geändert.

## 23.1 DebugSubCommand.java

### Änderung
Die Permission des Debug-Subcommands wurde von:

pixelrpg.admin

auf die im Projekt tatsächlich deklarierte und für die übrigen Admin-Kommandos verwendete Permission:

rpg.admin

vereinheitlicht.

### Wirkung
- Behebt die im ursprünglichen Audit festgestellte Permission-Inkonsistenz.
- Keine Änderung an Debug-Ausgaben oder Command-Syntax.
- Der Command erhält damit lediglich wieder die bereits vorgesehene Admin-Zugriffskontrolle.

**Status: erledigt — P1.1**

## 23.2 PlayerProfileManager.java

### Änderung
Beim Fehlerpfad der MySQL-Initialisierung wird ein bereits aufgebauter DatabaseManager vor dem YAML-Fallback explizit geschlossen.

Zusätzlich wird die Referenz nach dem Cleanup auf null gesetzt. Das Schließen selbst ist erneut gegen Runtime-Fehler abgesichert und wird nur geloggt.

### Wirkung
- Verhindert, dass ein erfolgreich aufgebauter Hikari-Pool beim anschließenden YAML-Fallback offen bleibt.
- Keine Änderung am erfolgreichen MySQL-Pfad.
- Keine Änderung am YAML-Pfad.
- Keine Änderung an Save-/Load-Timing oder Profilformat.

**Status: erledigt — P1.2**

## 23.3 NpcNameVisibilityService.java

### Änderung
Die vorhandene NMS-Reflection wurde nicht durch eine neue API ersetzt und ihre funktionale Packet-Logik wurde nicht verändert.

Stattdessen werden die bereits verwendeten Reflection-Objekte nach erfolgreicher Auflösung gecacht:

- DATA_CUSTOM_NAME_VISIBLE
- DataValue.create(...)
- ClientboundSetEntityDataPacket-Konstruktor
- packetListener-Field für den tatsächlichen Connection-Typ
- send(Packet)-Methode für den tatsächlich aufgelösten Listener-Typ

Die ursprüngliche Runtime-Typauflösung für Connection und Packet Listener bleibt erhalten. Zusätzlich werden null-Argumente defensiv ignoriert.

### Wirkung
Vorher wurden die gleichen Klassen-/Feld-/Methoden-Reflectionen bei jedem sichtbarkeitsbezogenen Aufruf erneut aufgelöst. Jetzt erfolgt die teure Suche einmalig und wird anschließend wiederverwendet.

Damit sinken:
- Reflection-Lookups,
- Klassenauflösungen,
- Field-/Method-Scans,
- unnötige temporäre Reflection-Objekte.

Der tatsächlich gesendete Packet-Typ und dessen Werte bleiben unverändert.

**Wichtig:** Die NMS-Abhängigkeit bleibt weiterhin versionssensitiv. Die Änderung reduziert den Laufzeit-Overhead, beseitigt aber nicht das grundsätzliche Upgrade-Risiko der internen NMS-Reflection.

**Status: gehärtet — P1.3 teilweise umgesetzt**

## 23.4 Bewusst nicht geändert: Quest-Locator

Der ursprüngliche Audit hatte QuestManager.checkReachLocationQuests() als möglichen Performance-Hotspot identifiziert.

Eine aggressive Verlagerung von Bukkit-World-Lookups in Async-Threads wurde hier nicht vorgenommen, weil World-/Location-Operationen auf dem Serverthread verbleiben müssen und eine unbedachte Asynchronisierung den funktionalen SOLL-Zustand gefährden könnte.

Auch eine zusätzliche Completion-Cache-Schicht wurde nicht eingeführt, weil dadurch bei sich verändernden World-/Structure-/Biome-Zielen die bisherige exakte Completion-Semantik verändert werden könnte.

**Ergebnis:** Kein riskantes Performance-Refactoring auf Kosten der funktionalen Identität.

## 23.5 Bewusst nicht geändert: ExternalSkinService

Der bestehende SSRF-Schutz, URL-/Host-Validierung, Response-Limitierung und asynchrone HTTP-Pfad waren bereits verhältnismäßig robust.

Eine weitere Änderung hätte eine neue Netzwerksemantik eingeführt und wurde deshalb in diesem SOLL-erhaltenden Refactoring nicht erzwungen.

## 23.6 Gesamtumfang der Änderungen

Geändert wurden ausschließlich:

- DebugSubCommand.java
- PlayerProfileManager.java
- NpcNameVisibilityService.java

Nicht geändert wurden:

- Commandsyntax
- Gameplay
- Quests
- Rezepte
- Items
- NPC-Konfigurationen
- Dialoge
- Rewards
- Datenformate
- Economy-Regeln
- Party-/Guild-Regeln
- Bossmechaniken
- Resourcepack-Inhalte

## 23.7 Verifikation

Der aktuelle Entwicklungsstand liegt weiterhin ausschließlich auf Branch test.

Code-Hardening-Stand vor dieser Dokumentationsaktualisierung:

e2179e7ff65a64e9488b64edb60fafcfb468ecab

Der GitHub-Status dieses Commits liefert derzeit keine hinterlegten CI-Statusmeldungen. Ein vollständiger Paper-26.2-Runtime-Smoke-Test wurde in dieser Umgebung daher nicht behauptet.

Für den finalen produktiven Einsatz gilt weiterhin: die neu gebaute JAR sollte nach dem Build einmal gegen denselben bereits erfolgreichen Server-/Gameplay-Testlauf geprüft werden.

## 23.8 Aktualisierte Bewertung

Der ursprüngliche statische Wert von **7,5/10** bleibt als konservative Basisbewertung bestehen, weil die Änderungen zwar konkrete P1-Risiken beheben bzw. entschärfen, aber keine zusätzliche Runtime-/Load-/Restart-Verifikation ersetzen.

Der technische Zustand ist damit unter den geprüften statischen Kriterien verbessert, während die wesentlichen verbleibenden Risiken weiterhin sind:

1. versionssensitives internes NMS im NPC-Name-Visibility-Pfad,
2. World-Locator-Kosten bei sehr vielen aktiven REACH_LOCATION-Quests,
3. externe Skin-Netzwerkgrenze,
4. fehlende vollständige automatische Paper-26.2-Runtime-/Restart-Tests.


---

# 24. Story / Lore / Native Dialog Erweiterung

## 24.1 Umfang

Auf Branch `test` wurde das bestehende native Paper-Dialog-System um eine persistente, verzweigte Lore-Kampagne erweitert.

Neue bzw. geänderte Komponenten:

- `StoryDialogueManager.java`
- `StoryNpcDialogue.java`
- `StoryBehavior.java`
- `StoryManager.java`
- `PixelRPGPlugin.java`

Die vorhandene `DialogueEngine` wurde bewusst wiederverwendet. Es wurde kein separates Inventar-/Chat-GUI-System eingeführt.

## 24.2 Native Dialog API

Die Story verwendet:

- `io.papermc.paper.dialog.Dialog`
- `DialogBase`
- `DialogBody`
- `DialogType.multiAction(...)`
- `ActionButton`
- `DialogAction.customClick(...)`
- Adventure `Component`

Damit bleibt die Interaktion auf dem nativen Paper-26.2-Dialogpfad.

Die Buttons verwenden Single-Use-Callbacks. Callback-Aktionen validieren den aktuellen Spielerstatus erneut, bevor Storyfortschritt geschrieben wird.

## 24.3 Persistenter Story-Fortschritt

Der Storyfortschritt wird weiterhin über `PlayerProfile.storyChapterIndex` gespeichert.

Vorteile:

- keine zusätzliche Player-Referenz
- UUID-basierter Profilzustand
- vorhandene Dirty-/Snapshot-/Async-Persistenz wird wiederverwendet
- kein neuer synchroner Dateischreibpfad
- kein zusätzlicher PDC-Datenbestand notwendig

Ein Kapitel kann nur abgeschlossen werden, wenn der aktuelle Profilindex exakt auf das unmittelbar vorherige Kapitel zeigt.

## 24.4 Validierung / Exploit-Schutz

Der Storypfad prüft:

- Player ist vorhanden und online
- Player ist registriertes PixelRPG-Mitglied
- NPC und Kapitel sind nicht null
- Kapitel ist tatsächlich das nächste freigeschaltete Kapitel
- Kapitel-IDs und Titel aus `story.yml` sind nicht leer
- Kapitelreihenfolge muss monoton sein
- maximal 32 Kapitel werden geladen
- einzelne Dialogzeilen sind auf 1000 Zeichen begrenzt
- XP-Belohnungen werden auf nichtnegative Werte begrenzt
- ein bedingter Echo-Splitter-Dialog prüft das Inventar sowohl beim Anzeigen als auch beim Callback erneut

Dadurch kann ein veralteter oder manipuliert ausgelöster Callback nicht einfach Storyfortschritt ohne aktuelle Bedingung erzeugen.

## 24.5 Lore-Abdeckung

Die erste Storykampagne besteht aus fünf Kapiteln:

1. **Die Stimmen unter Stein**
   - verschwundene/alte Erbauer
   - Ruinen
   - Ancient Cities
   - Unterschied zwischen Beobachtung und Theorie

2. **Die Stadt ohne Himmel**
   - Sculk
   - Warden
   - Ancient-City-Mysterium
   - bedingter Echo-Splitter-Dialog

3. **Die schwarze Flamme**
   - Piglin-Zivilisation und Bastions
   - Wither-Erschaffung
   - vorsichtige Trennung von gesicherter Information und Theorie

4. **Das Tor jenseits der Sterne**
   - Strongholds
   - Endportal
   - Enderdrache
   - End Cities / Endschiffe
   - Elytren
   - offene Frage nach früheren Reisenden

5. **Was hinter dem Ende bleibt**
   - Zusammenführung der bekannten Spuren
   - bewusst offene Lore-Fragen
   - Abschluss als Forschungs-/Archivmotiv

Wo Minecraft keine eindeutige kanonische Erklärung liefert, formuliert der Dialog dies ausdrücklich als offene Frage oder Theorie. Es werden keine Fan-Theorien als gesicherte offizielle Tatsachen ausgegeben.

## 24.6 Bestehende Story-Daten

Die alte automatisch erzeugte Ein-Kapitel-Default-Story (`prologue`) wird beim Laden erkannt und einmalig auf Story-Version 2 migriert.

Individuell angelegte Story-Dateien mit mehreren Kapiteln werden nicht pauschal überschrieben.

## 24.7 Performance

Der Storypfad führt ausschließlich kleine In-Memory-Prüfungen auf dem Serverthread aus:

- Profil-/UUID-Zugriff
- Kapitelstatus
- Inventarprüfung auf einen einzelnen Materialtyp
- Erzeugung kleiner Dialogobjekte

Es wurden keine World-Locator-, Chunk-Scan-, Datenbank- oder Dateisystemoperationen in Dialog-Callbacks verschoben.

Der eigentliche Profil-Persistenzpfad bleibt der bestehende asynchrone PlayerProfileManager.

## 24.8 Forensische Bewertung

**Befund: sauberer Add-on-Pfad unter den bekannten statischen Kriterien.**

Besonders positiv:

- keine statischen `Player`-Referenzen im Storysystem
- keine neue synchron persistierte Spielerdatenbank
- keine NMS-/Reflection-Erweiterung
- keine Kommandoausführung
- keine externen Netzwerkzugriffe
- native Paper-Dialoge statt eigener GUI-Protocol-Implementierung
- Story-Callbacks sind single-use und statusgeprüft

Weiterhin erforderlich:

- tatsächlicher Paper-26.2-Build
- Serverstart mit der neuen JAR
- Clienttest der Dialoge
- kompletter Durchlauf aller fünf Kapitel
- Test mit und ohne Echo-Splitter
- Stop/Start-Test für Storyfortschritt

Die statische Forensik ersetzt diese Runtime-Tests nicht.


# 25. Story / Lore / Dynamic Structure NPC System

## 25.1 Architektur

Die Story-Kampagne verwendet jetzt zehn level-gated Arcs von Level 1 bis 99. Kapitel werden in `story.yml` über:

- `id`
- `required-level`
- `structure-trigger`
- `npc-id`
- `quests`
- `dialogue`
- `exp-reward`

definiert.

Der Story-Fortschritt bleibt ausschließlich in `PlayerProfile.storyChapterIndex`.

## 25.2 Struktur-Trigger

`StoryTriggerListener` reagiert nur auf relevante Bewegungs-/Teleport-/Chunk-Ereignisse.

`StoryLocationRegistry` verwendet `Chunk.getStructures()` und die bereits generierten `GeneratedStructure`-Instanzen. Es gibt keinen permanenten Scan und keinen neuen `locateNearestStructure`-Aufruf im Story-Triggerpfad.

Ein Trigger wird nur geprüft, wenn:

1. der Spieler registriert ist,
2. das Kapitel level-seitig freigeschaltet ist,
3. eine Quest des Kapitels aktiv ist,
4. der aktuelle Chunk bereits geladen ist.

## 25.3 Persistentes Story-NPC-Spawning

Story-NPCs werden über den bestehenden `NpcManager` als `NpcType.STORY` erzeugt.

Die ID wird aus Kapitel, Welt und Strukturposition stabil gebildet. Dadurch verhindert `NpcManager.createWithId` doppelte Spawn-Einträge und persistiert die NPC-Definition über den bestehenden asynchronen NPC-Speicherpfad.

## 25.4 Quest-Fortschritt

Für Structure-Reach-Quests existiert ein gezielter `QuestManager.markReachLocationReached(...)`-Pfad. Dadurch muss der Story-Trigger nicht die bestehende teure World-Locator-Logik verwenden.

Story-Quest-Abgaben über einen Story-NPC laufen über `completeQuestAtNpc(...)` und prüfen erneut:

- registriertes Profil,
- aktive Quest,
- Ablaufstatus,
- erforderlichen Fortschritt,
- existierenden Story-NPC,
- NPC-Typ,
- Distanz zum NPC.

Die eigentliche Profilmutation nutzt weiterhin die bestehende Dirty-/Snapshot-/asynchrone Persistenz.

## 25.5 Native Dialoge

Eryn verwendet ausschließlich die vorhandene `DialogueEngine` mit Paper-26.2-Native-Dialogs und `DialogAction.customClick`.

Alle Buttons sind Single-Use-Callbacks. Inventar-/Queststatus wird im Callback erneut aus dem aktuellen Profil geprüft.

## 25.6 Lore

Die Dialoge trennen beobachtbare Minecraft-Fakten von offenen In-World-Hypothesen. Ancient Cities, Sculk, Warden, Echo Shards, Bastions, Strongholds, End Cities und der Enderdrache bilden die dokumentierte Grundlage der Kampagne.

Die konkrete Erklärung, wer die Ancient-City-Erbauer waren oder warum sie verschwanden, wird bewusst nicht als kanonische Tatsache behauptet.

## 25.7 Beispielkapitel

Der Arc `under_the_stone` (Level 11–20) verwendet die Struktur `minecraft:ancient_city` und den persistenten Story-NPC Eryn.

Ablauf:

Reception -> `story_under_stone_expedition` -> Ancient City -> Eryn -> Expeditionsabgabe -> `story_under_stone_echo_fragments` -> 3 Echo Shards -> Eryn -> Kapitelabschluss -> nächster Arc.

## 25.8 Runtime-Verifikation

Weiterhin erforderlich:

- Paper-26.2-Build
- Serverstart
- Ancient-City-Trigger mit aktivem Quest
- NPC-Deduplizierung nach Chunk-Unload/Load
- Quest-Reboot-Persistenz
- Echo-Shard-Callback mit verändertem Inventar
- kompletter Level-Gate-Test der zehn Kapitel

Die statische Implementierung ersetzt keinen vollständigen Runtime-Smoke-Test.


## 26. Story-Kampagne – Strukturknoten und Nebenüberlieferungen

Der Story-Audit wurde nach der Vollprüfung der bisherigen 35 Kapitel bereinigt. Ziel ist nicht, jede Vanilla-Struktur als eigenen Questknoten zu behandeln, sondern nur Strukturen zu verwenden, die für den roten Lore-Faden einen eigenständigen Informationswert liefern.

## 26.1 Beibehaltene Haupt-Strukturen

Die aktive Strukturkampagne verfolgt diese 17 Strukturknoten:

- Plains Village
- Shipwreck
- Desert Pyramid
- Jungle Pyramid
- Swamp Hut
- Warm Ocean Ruins
- Ocean Monument
- Trail Ruins / Archäologie
- Mineshaft
- Pillager Outpost
- Woodland Mansion
- Ruined Portal
- Ancient City
- Nether Fortress
- Bastion Remnant
- Stronghold / End Portal
- End City / End Ship

Damit bleiben die vom Projekt geforderten Lore-Bereiche abgedeckt: Oberwelt-Siedlungen und Reisen, Archäologie, Illager, Dimensionsübergänge, Deep Dark, Nether, Endzugang und End-Endgame. Offizielle Minecraft-Quellen bestätigen die eigenständigen Erkundungs-/Lorebezüge insbesondere für Trail Ruins, Ocean Monuments, Ancient Cities, Strongholds, Bastions und End Cities. citeturn5search1turn0search2turn2search2turn2search5turn3search2turn4search2

## 26.2 Aus der aktiven Strukturkampagne entfernt

Folgende Varianten bzw. Nebenstrukturen erzeugen keinen eigenen Story-NPC und keine eigene REACH_LOCATION-Quest mehr:

- Cold Ocean Ruins
- Buried Treasure
- Badlands/Mesa-Mineshaft-Variante
- Igloo
- die einzelnen Ruined-Portal-Varianten (Desert, Jungle, Swamp, Mountain, Ocean, Nether)
- Nether Fossil
- Trial Chambers
- Beach-/Strand-Schiffswrack-Variante
- Desert, Savanna, Snowy und Taiga Village als separate Storyknoten

Das sind keine gelöschten Vanilla-Strukturen. Sie bleiben Bestandteil der Welt und werden als Nebenüberlieferungen über den Archivar bzw. andere NPCs erzählt. Die offizielle Archäologie-Dokumentation ordnet warme und kalte Ozeanruinen sowie Trail Ruins gemeinsam in die Archäologie-Fundorte ein; deshalb ist ein gemeinsamer Hauptstrang ausreichend. citeturn5search0turn5search3

Ruined-Portal-Varianten werden ebenfalls gemeinsam über einen Hauptknoten behandelt. Minecraft beschreibt Ruined Portals allgemein als Reste alter Portalverbindungen in Oberwelt und Nether. citeturn3search0turn3search7

## 26.3 Nebenüberlieferungen und Migration

Die entfernten Strukturquests werden nicht mehr geladen. Die entsprechenden Kapitelpositionen bleiben als reine, questlose Dialogkapitel erhalten, damit bestehende storyChapterIndex-Werte nicht verschoben werden. StoryManager entfernt beim Zugriff auf die Story außerdem veraltete aktive Storyquests aus der vorherigen Kampagnenversion.

Die Kampagne steigt damit auf Story-Version 6. Eine bestehende, unveränderte Version-5-Kampagne wird automatisch durch die neue Bundled-Kampagne ersetzt; benutzerdefinierte Storydaten mit abweichender Kapitelstruktur werden nicht pauschal überschrieben.

## 26.4 Clientseitige Story-NPC-Sichtbarkeit

- Story-NPCs werden weiterhin mit Entity#setVisibleByDefault(false) angelegt.
- StoryNpcVisibilityListener nutzt Player#showEntity/hideEntity und PlayerTrackEntityEvent als native Sichtbarkeitsschranke.
- Ein Story-NPC wird nur bei registriertem PixelRPG-Profil und aktiver Kapitelquest sichtbar.
- Join, Weltwechsel, Chunk-Wechsel, Profiländerung und Tracking bleiben die gezielten Aktualisierungspunkte; kein permanenter Tick-Scan.

## 26.5 Runtime-Verifikation offen

Vor produktivem Einsatz bleiben Build-/Server-Smoke-Tests auf der verbindlichen Paper-26.2-Dev-Bundle-Basis erforderlich:

1. Migration von Story-Version 5 auf Version 6.
2. Entfernte Storyquests werden aus aktiven Profilen bereinigt.
3. Questannahme erzeugt den korrekten Story-NPC und macht ihn nur für berechtigte Spieler sichtbar.
4. Unregistrierte/Vanilla-Spieler sehen keine Story-NPCs.
5. Weltwechsel, Chunk-Wechsel, Reconnect und Entity-Tracking.
6. Abschluss eines Hauptstrukturknotens und Weitergabe an das nächste Kapitel.
7. check inklusive aller bestehenden Forensik-/Relocation-Prüfungen.


---

# 19. Forensik-Update – Content-Pipeline 24.09.2026

## Scope

Der Umbau wurde ausschließlich auf **test** durchgeführt. **main** wurde nicht verändert.

Die Änderungen beschränken sich auf die Vervollständigung der bestehenden Content-Pipeline und respektieren die bestehenden Architektur-/Build-Grenzen.

## Neue technische Pfade

### Custom-Crafting

Die bestehende Crafting-Pipeline wurde erweitert, ohne den Vanilla-Rezeptpfad zu ersetzen.

Validiert werden:

- Custom-`resultItemId`
- Custom-`itemCosts`
- zentrale ItemDefinitionRegistry
- Verbot von Admin-/Unique-Items als Crafting-Ergebnis oder Kosten
- Materialgleichheit zwischen Recipe-Result und ItemDefinition

Die tatsächliche Erzeugung läuft weiterhin über `ItemService`.

### Quest-Rewards

Die Quest-Pipeline unterstützt den bestehenden `reward.companionId`-Mechanismus jetzt mit tatsächlichem Content.

Custom-Item-Questbelohnungen verwenden die zentrale ItemService-Erzeugung. Die Canonicalisierung der PixelRPG-Item-IDs wurde dabei so korrigiert, dass die vorhandenen Slash-basierten IDs der zentralen ItemDefinitionRegistry unverändert erhalten bleiben.

### Boss-Loot

Boss-Loot-Migration ergänzt ausgewählte Progressionsitems additiv. Vorhandene Boss-Rewards werden nicht entfernt.

### Physische Currency

Die bestehende Currency-Factory verwendet jetzt den aktuellen Paper-26.2-Data-Component-Pfad für `MAX_STACK_SIZE`.

Die automatische Pickup-Überführung in die virtuelle Economy wurde aus der Plugin-Registrierung entfernt. Damit bleibt die Währung beim normalen Spieler-Pickup physisch im Inventar.

Der technisch unterstützte Stackwert wurde auf 99 begrenzt. Eine 999er Lösung wurde bewusst nicht über NMS/Legacy-Mechaniken erzwungen.

## Unveränderte Forensik-Grenzen

Weiterhin gültig:

- keine CraftBukkit-Klassen
- kein Legacy-NMS
- kein `ChatColor`
- Mojang-Mappings
- Paper 26.x API
- Java 25
- bestehende Shadow-Relocations
- bestehende Build-/Boundary-Verifikationen
- keine statischen Live-Server-Referenzen
- keine Entfernung der vorhandenen Verification-Tasks

## Bewusst nicht angefasst

- Food-Testbestand
- globale NPC-Weltpopulation
- manuelle Shop-Inhalte
- Regionen/Guild-City
- Resourcepack-Ausbau

Diese Punkte sind Produkt-/Content-Entscheidungen und keine unbeabsichtigten technischen Lücken.

## Verifikation

Der GitHub-Workflow enthält weiterhin den bestehenden vollständigen `gradle clean build`- und API-/Artifact-Verification-Pfad.

Für den aktuellen Stand wurde über den verfügbaren GitHub-Connector **kein Workflow-Run zurückgeliefert**. Deshalb wird hier kein erfolgreicher Build/Server-Smoke-Test behauptet. Die Änderungen wurden stattdessen gegen die vorhandenen Quell-/Datenstrukturen und die Paper-26.2-API-Grenzen geprüft.



---

# 35. Forensik-Update – Abschluss der Content-Pipeline

**Stand:** 24.09.2026  
**Branch:** `test`

Dieser Abschnitt ergänzt die vorherigen forensischen Befunde und beschreibt den aktuellen Abschluss der Content-Vervollständigung.

## Änderungsprinzip

Es wurden ausschließlich Content-Pfade ergänzt bzw. vervollständigt. Bestehende Kernsysteme wurden nicht durch parallele Ersatzimplementierungen ersetzt.

Besonders geschützt blieben:

- PlayerProfileManager
- bestehende MySQL-/YAML-Persistenz
- Story-Fortschritt über `PlayerProfile.storyChapterIndex`
- NpcManager
- DialogueEngine
- QuestManager
- ItemService
- BossRepository
- bestehende API-/Event-Strukturen

## Custom-Crafting – Forensik

Die Crafting-Pipeline validiert Custom-Items gegen die zentrale ItemDefinitionRegistry.

Ein Rezept kann nicht beliebige interne IDs erzeugen. Custom-Ergebnisse laufen über `resultItemId` und werden zentral über `ItemService` erzeugt.

Custom-Kosten werden separat von Vanilla-Materialkosten behandelt.

Die Rezeptabhängigkeitsprüfung erkennt Produzenten von Custom-Items und verhindert zyklische Abhängigkeiten.

Damit werden insbesondere folgende Klassen von Content-Exploits reduziert:

- unbekannte Custom-Item-IDs
- nicht registrierte Crafting-Ergebnisse
- ungültige Custom-Kosten
- zirkuläre Rezeptketten

## Quest-Rewards

Questbelohnungen für Custom-Items werden über den vorhandenen ItemService-Pfad erzeugt.

Companion-Rewards verwenden die vorhandene `reward.companionId`-Struktur. Alle 25 questbasierten Companion-Definitionen besitzen aktuell eine passende Quest.

Es wurde keine neue persistente Player-Referenzstruktur eingeführt.

## Boss-Loot

Die Progressions-Loot-Migration arbeitet additiv.

Vorhandene Boss-Lootdefinitionen werden nicht durch eine neue parallele Lootpipeline ersetzt. Fehlende Progressionsbelohnungen werden nur ergänzt.

Bei fehlender `bosses.yml` erzeugt `BossRepository` weiterhin die definierten 26 Biome- und 6 World-Bosse.

## Story-/NPC-Pipeline

Story-NPCs werden ausschließlich über den bestehenden `NpcManager` erzeugt.

Der Triggerpfad:

1. registriertes Profil prüfen,
2. Story-/Queststatus prüfen,
3. relevanten Chunk prüfen,
4. vorhandene Strukturinformationen verwenden,
5. stabile Story-NPC-ID erzeugen,
6. NPC nur einmalig registrieren,
7. vorhandenen NPC-/Persistenzpfad verwenden.

Es gibt keinen permanenten globalen World-Scan und keinen zusätzlichen synchronen Datenbankpfad.

## Story-NPC-Sichtbarkeit

`NpcType.STORY` bleibt clientseitig selektiv sichtbar.

Ein Spieler ohne gültiges PixelRPG-Profil bzw. ohne passende aktive Quest erhält keine Story-NPC-Sichtbarkeit.

Die Aktualisierung ist ereignisbasiert:

- Join
- Weltwechsel
- Chunk-/Tracking-Ereignisse
- Profiländerung

Kein globaler permanenter Tick-Scan.

## Physische Currency

Die automatische Currency-Pickup-Überführung in die virtuelle Bank wurde aus dem Plugin-Registrierungspfad entfernt.

Die Currency bleibt ein physisches Inventarobjekt.

Der Stack-Limit-Pfad verwendet den aktuellen Paper-26.2-Data-Component-Mechanismus. Der konfigurierte Wert wird auf den API-seitig unterstützten Bereich begrenzt.

Eine künstliche 999er NMS-/Legacy-Implementierung wurde bewusst vermieden.

## Bewusst nicht implementiert

Die folgenden Punkte sind Produktentscheidungen und keine forensischen Defekte:

- Food-Testbestand
- globale NPC-Weltpopulation
- automatische Default-Shops
- Guild-City-Weltcontent
- Regionen-Ausbau
- Resourcepack-Ausbau

## Build-/Runtime-Hinweis

Die statische Forensik kann keinen vollständigen Server-Smoke-Test ersetzen.

Vor Produktionsdeployment sind weiterhin sinnvoll:

- `gradle clean check`
- Shadow-JAR-Erzeugung
- Serverstart auf Paper 26.2
- Story-Quest-Durchlauf
- Custom-Crafting-Durchlauf
- Boss-Loot-Durchlauf
- Currency-Pickup-/Bank-Test
- Companion-Unlock-Test
- Sichtbarkeitstest mit registriertem und nicht registriertem Spieler

Die vorhandenen Build-Grenzprüfungen bleiben unverändert Bestandteil von `check`.

## Abschlussbewertung

Aus Sicht der **Content-Pipeline** gibt es im aktuellen, nicht pausierten Scope keinen bekannten technischen Blocker mehr.

Die nächsten Änderungen sollten deshalb nicht mehr als „fehlende Grundimplementierung“ behandelt werden, sondern als:

- Balancing,
- Playtesting,
- manuelle Welt-/Shop-Konfiguration,
- oder optionale neue Content-Pakete.

