# PixelRPG – Forensische Plugin-Prüfung

**Prüfobjekt:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `test`  
**Prüfstand:** `e144a9005d1c295c237be606e2c49162a7da536d`  
**Referenzbranch:** `main`  
**Referenzstand:** `b02747a21cde6d1308c59a7f027cda80877cfe50`  
**Prüfdatum:** 2026-09-24  
**Zielplattform:** Java 25 / Paper 26.2 / Mojang-Mappings  
**Prüfart:** statische Codeforensik, Architekturprüfung, Persistenz-/Concurrency-Prüfung, Sicherheitsprüfung, Build-/CI-Prüfung, API-Prüfung, Repository- und Betriebsprüfung

---

## 1. Zweck und Abgrenzung

Dieses Dokument ist eine vollständige technische Bestands- und Risikoanalyse des aktuell auf `test` vorhandenen PixelRPG-Plugins.

Wichtig ist die Unterscheidung zwischen drei Evidenzklassen:

1. **Code-/Repository-Nachweis** – direkt aus dem aktuellen Branch geprüft.
2. **CI-Nachweis** – durch GitHub Actions für den aktuellen Commit geprüft.
3. **Runtime-Nachweis** – Verhalten eines tatsächlich laufenden Servers.

Das Plugin ist laut Projektstatus bereits auf einem Server installiert und spielbar. Diese Information wird als Betriebsfaktum des Projekts berücksichtigt. Die GitHub-Forensik kann jedoch keinen laufenden Minecraft-Server selbst beobachten. Runtime-Aussagen werden deshalb nicht als unabhängig reproduziert ausgegeben.

Diese Prüfung ist **kein Anlass für einen Rewrite**. Ziel ist die Feststellung, ob die bestehende Architektur, Datenhaltung, Sicherheitsgrenzen, Lifecycle-Struktur, Buildkette und Wartbarkeit belastbar sind und wo konkrete technische Risiken verbleiben.

---

# 2. Executive Summary

PixelRPG ist kein Prototyp mehr. Der aktuelle Quellbestand umfasst **247 Java-Dateien**, 15 zentrale JSON-Datendateien und 49 Resourcepack-Dateien. Die Architektur ist in zahlreiche Fachmodule aufgeteilt: Player, NPC, Dialogue, Quest, Profession, Item, Equipment, Combat, Boss, Companion, Party, Guild, Region, Shop, Economy, Story, Statistics und Storage.

Der aktuelle `test`-Stand baut erfolgreich und verwendet die vorgegebenen technischen Grundlagen:

- Java 25
- Paper 26.2
- paperweight Userdev 2.0.0-beta.21
- Paper Dev Bundle 26.2.build.121-stable
- Shadow 9.6.1
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0
- `paper-plugin.yml`
- Paper Bootstrapper
- Mojang-Mappings

Der aktuelle GitHub-Actions-Lauf für exakt den geprüften Commit `e144a9005d1c295c237be606e2c49162a7da536d` war erfolgreich.

### Gesamtbefund

**Der Plugin-Code ist technisch fortgeschritten und im Kern belastbar.**

Es wurden keine Hinweise gefunden auf:

- Legacy-NMS-Pakete
- CraftBukkit-Abhängigkeiten
- `ChatColor`
- offensichtliche statische Player-/Entity-/World-Referenzen
- unrelocierte Gson-/Hikari-/MySQL-Klassen im vorgesehenen Shadow-Modell

Besonders solide sind:

- Plugin-Lifecycle
- Snapshot-basierte Persistenz
- MySQL-Transaktionen
- Revision Protection
- NPC-Dateipersistenz
- NPC-Skin-Speicherung
- Paper-Dialog-Registrierung
- ShadowJar-Validierung
- Async Player I/O
- Resource-/Executor-Shutdown

Es existieren jedoch weiterhin konkrete Befunde:

### Hohe Relevanz

1. **MySQL-Fallback-Ressourcenrisiko:** Wenn der MySQL-Pool bereits geöffnet wurde und die anschließende Repository-Initialisierung fehlschlägt, wird im Catch-Pfad auf YAML gewechselt, ohne den bereits erzeugten DatabaseManager explizit zu schließen.
2. **Externe Skin-URLs:** Die SSRF-Abwehr ist deutlich überdurchschnittlich für eine Plugin-Implementierung, aber DNS-Prüfung und tatsächlicher HTTP-Zugriff sind nicht atomar. Ein DNS-Rebinding-/TOCTOU-Fenster bleibt.
3. **Kein echter Server-Smoke-Test in CI:** Die CI beweist Build-/Artifact-Korrektheit, nicht das vollständige Verhalten eines gestarteten Paper-Servers.
4. **Kein automatisierter Restart-/Persistenztest:** Insbesondere der funktionierende NPC-Skin-Persistenzpfad ist nicht durch einen reproduzierbaren CI-Restart bewiesen.
5. **Kein automatisiertes Content-Linting:** JSON-Referenzen zwischen Items, Quests, Rezepten, Bossen, Companions und Resourcepack werden nicht als eigener Build-Gate vollständig validiert.

### Mittlere Relevanz

6. Kein Gradle Wrapper im Repository.
7. Command-System besitzt noch interne Bukkit-Kompatibilitätsadapter, obwohl die Registrierung über Paper `BasicCommand` erfolgt.
8. Externe Skin-HTTP-Clients werden instanzweise erzeugt; ein zentraler Service wäre kontrollierbarer.
9. Reload-/Lifecycle-Verhalten ist statisch gut strukturiert, aber nicht automatisiert getestet.
10. Observability ist überwiegend logbasiert; Metriken für Persistenz, Skin-Resolution, DB-Pool und Task-Latenzen fehlen.

### Niedrige Relevanz / Hygiene

11. Doppelte Imports in `MannequinSkinResolver`.
12. Teilweise veraltete Kommentare zur Skin-Implementierung.
13. Einige INFO-Logs der Skin-Auflösung wären als Debug-/Trace-Ebene geeigneter.

**Wichtig:** Keiner dieser Punkte widerlegt die Aussage, dass das Plugin aktuell spielbar ist. Sie beschreiben die Differenz zwischen **funktionierendem Serverbetrieb** und **forensisch automatisiert abgesichertem Produktionsstand**.

---

# 3. Repository- und Branch-Forensik

## 3.1 Aktueller Branch

`test` zeigt auf:

```
e144a9005d1c295c237be606e2c49162a7da536d
```

`main` zeigt auf:

```
b02747a21cde6d1308c59a7f027cda80877cfe50
```

Der Vergleich ergibt:

```
main -> test
ahead:  1
behind: 0
```

Der einzige Unterschied ist aktuell die bereits vorhandene forensische Dokumentation.

### Befund

Die Implementierung selbst entspricht damit weiterhin dem bisherigen `main`-Stand. Die aktuelle forensische Dokumentation ist die zusätzliche Änderung auf `test`.

Die verbindliche Arbeitsregel bleibt:

- Änderungen nur auf `test`
- `main` nur als Referenz
- keine direkte Änderung an `main`

---

# 4. Projektgröße und Modulstruktur

Der Branch enthält:

- 247 Java-Dateien
- 15 JSON-Content-Dateien
- 49 Resourcepack-Dateien
- `config.yml`
- `paper-plugin.yml`
- GitHub Actions
- Editor-/Projektdateien
- technische Dokumentation

Die größten Fachbereiche sind unter anderem:

- `boss`
- `combat`
- `companion`
- `dialogue`
- `guild`
- `item`
- `npc`
- `profession`
- `quest`
- `region`
- `player`
- `storage`
- `shop`
- `story`

### Architektururteil

Die Aufteilung ist für ein MMORPG-Plugin angemessen. Es existieren klare Domänengrenzen und zahlreiche kleine Services statt einer einzigen zentralen God-Class.

`PixelRPGPlugin` übernimmt allerdings weiterhin einen großen Composition-/Bootstrap-Anteil. Das ist für die Hauptklasse akzeptabel, solange die eigentliche Fachlogik in den Managern und Services bleibt. Genau das ist überwiegend der Fall.

---

# 5. Gradle-Forensik

## 5.1 Technische Basis

`build.gradle` verwendet exakt die vorgegebene Basis:

```
io.papermc.paperweight.userdev 2.0.0-beta.21
Paper 26.2.build.121-stable
Shadow 9.6.1
Java 25
```

Dependencies:

```
Gson 2.13.1
HikariCP 7.0.2
MySQL Connector/J 9.7.0
```

### Befund

**PASS**

Die Buildbasis entspricht der Projektvorgabe.

---

## 5.2 Java 25

Toolchain:

```
JavaLanguageVersion.of(25)
```

Compiler:

```
options.release.set(25)
```

Zusätzlich:

```
-Xlint:all
```

### Befund

**PASS**

Es existiert keine erkennbare Rücksichtnahme auf ältere Java-Versionen.

---

## 5.3 ShadowJar

Die Relocations sind:

```
com.google.gson -> de.pixelrpg.rpg.libs.gson
com.zaxxer.hikari -> de.pixelrpg.rpg.libs.hikari
com.mysql -> de.pixelrpg.rpg.libs.mysql
```

Artifact:

```
Pixel-RPG.jar
```

Der normale Jar-Task erhält den Classifier:

```
slim
```

### JDBC-Service

Die Buildprüfung kontrolliert zusätzlich:

```
META-INF/services/java.sql.Driver
```

und verlangt:

```
de.pixelrpg.rpg.libs.mysql.cj.jdbc.Driver
```

statt des unrelozierten:

```
com.mysql.cj.jdbc.Driver
```

### Befund

**PASS**

Die Shadow-Konfiguration ist nicht nur vorhanden, sondern wird anschließend auch strukturell geprüft.

---

# 6. Build Boundary Verification

Die Gradle-Prüfung verbietet:

- `org.bukkit.ChatColor`
- Bungee `ChatColor`
- Legacy-NMS `net.minecraft.server.v...`
- CraftBukkit
- definierte statische Player-/Entity-/World-Referenzen

Zusätzlich existiert eine parallele CI-Prüfung.

### Befund

**PASS**

Im geprüften Source-Bestand wurden keine entsprechenden verbotenen Muster festgestellt.

### Einschränkung

Regex-Prüfungen sind keine semantische Analyse. Sie erkennen definierte Muster, können aber keinen beliebigen Objektgraphen oder jede indirekte Serverreferenz beweisen.

Das ist eine Grenze der Prüfung, kein aktueller Defekt.

---

# 7. Gradle Wrapper

Im Repository ist kein `gradlew`/Gradle-Wrapper erkennbar.

CI installiert dagegen explizit Gradle 9.2.0.

### Risiko

CI ist reproduzierbar, lokale Builds sind von der installierten Gradle-Version abhängig.

### Einstufung

**P2 – Wartbarkeit / Reproduzierbarkeit**

Empfehlung: Gradle Wrapper hinzufügen und CI denselben Wrapper verwenden.

---

# 8. Paper Plugin System

`paper-plugin.yml` enthält:

```yaml
name: PixelRPG
version: 1.0.0
main: de.pixelrpg.rpg.PixelRPGPlugin
bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap
api-version: '26.2'
```

Damit wird das moderne Paper Plugin Modell verwendet.

Es wurde keine alte `plugin.yml`-Architektur als primäre Pluginbeschreibung festgestellt.

### Befund

**PASS**

---

# 9. Bootstrapper

`PixelRPGBootstrap` implementiert:

```
PluginBootstrap
```

und verwendet den Lifecycle Manager.

Die Dialog-Registrierung erfolgt über:

```
RegistryEvents.DIALOG
DialogKeys
DialogType
DialogBase
DialogAction
```

Zusätzlich wird der Quick-Actions-Tag über:

```
LifecycleEvents.TAGS.postFlatten
```

bearbeitet.

### Befund

**PASS**

Die Dialogarchitektur basiert auf dem aktuellen Paper-Registrierungsmodell.

Paper 26.2 stellt `Mannequin#getProfile()`, `Mannequin#setProfile(ResolvableProfile)` und `ResolvableProfile` bereit. Diese API-Linie ist im geprüften Code nachvollziehbar.

---

# 10. Dialogsystem

Das Plugin verwendet native Dialoge für unter anderem:

- Charakterkarte
- Charakterprofil
- aktive Quests
- Questdetails
- Companion-Funktionen
- Professionen
- weitere Quick Actions

Die Dialoge werden nicht als alte 1.21.x-Workaround-Implementierung nachgebaut.

### Befund

**PASS**

### Restrisiko

Dialoge sind ein API-Bereich, der eng an die jeweilige Paper-/Minecraft-Version gebunden ist. Bei jedem Upgrade muss der tatsächliche Dev-Bundle-Stand erneut gebaut werden.

---

# 11. Command-System

Die Registrierung erfolgt über Paper Lifecycle:

```
LifecycleEvents.COMMANDS
```

und `BasicCommand`-basierte Adapter.

Registrierte Root-Kommandos umfassen unter anderem:

- `pixelrpg`
- `pixelrpgparty`
- `pixelrpgquestlog`
- `pixelrpgdialogue`

### Positiv

Die öffentliche Registrierung folgt dem aktuellen Paper Command Lifecycle.

### Wartungspunkt

Intern existieren weiterhin Adapter zu:

- `CommandExecutor`
- `TabCompleter`
- Bukkit-`Command`

Das ist funktional, aber architektonisch nicht vollständig auf die modernste Paper-Command-Schicht reduziert.

### Einstufung

**P2**

Kein akuter Funktionsfehler.

---

# 12. Lifecycle

Der `LifecycleCoordinator` besitzt:

- registrierte `AutoCloseable`-Ressourcen
- idempotentes Schließen
- Reverse-Order Shutdown
- Fehlerisolierung pro Ressource

Das ist ein gutes Muster.

### Befund

**PASS**

---

# 13. Plugin Enable-Reihenfolge

`PixelRPGPlugin.onEnable()` initialisiert unter anderem:

1. Config
2. RPG Keys
3. Player Profile Manager
4. Stat Engine
5. Profession System
6. Item System
7. Equipment
8. Shop
9. Story
10. Party
11. Quest
12. Global Events
13. Guild
14. Region
15. Boss
16. Statistics
17. Scoreboard
18. Playtime
19. NPC
20. Dialogue
21. Companion
22. Listener
23. Scheduler
24. Commands

Die Reihenfolge folgt überwiegend den Abhängigkeiten.

### Befund

**PASS / plausibel**

---

# 14. Shutdown-Reihenfolge

Beim Disable wird der Lifecycle Coordinator geschlossen.

Unter anderem werden beendet:

- Player Profile Manager
- Companion
- Party
- Guild
- Region
- NPC
- NPC Look Task
- Boss
- Quest
- Scoreboard
- Playtime
- Mob Scaling
- Boss Spawn
- Quest Passive Task
- Region Editor
- Region Spawn
- Banker

Danach werden Services explizit unregistert.

### Positiv

Die Hauptressourcen besitzen eigene Shutdown-Pfade.

### Restrisiko

Nicht jeder mögliche externe Vorgang ist zentral cancellbar. Das betrifft besonders HTTP-Skin-Requests.

---

# 15. Player-Persistenz

Die Player-Persistenz besitzt zwei Repository-Implementierungen:

- YAML
- MySQL

Der Manager besitzt:

- Active Profile Cache
- Loading Cache
- Save Chain
- Pending Save Coalescing
- Dirty State
- Mutation Revision
- Persistence Revision
- Async Executor
- Shutdown Flush
- Emergency Backup

### Befund

**Stark**

Die Architektur ist für ein persistent stateful Plugin angemessen.

---

# 16. Async Player I/O

Player-Daten werden nicht einfach auf dem Server-Main-Thread in Dateien oder MySQL geschrieben.

Es existiert ein eigener Executor:

```
PixelRPG-ProfileIO
```

Pre-Login verwendet diesen I/O-Pfad ebenfalls.

### Positiv

Das verhindert typische Hauptthread-Blockierungen durch:

- SQL
- Datei-I/O
- größere Serialisierung

### Befund

**PASS**

---

# 17. Snapshot-Persistenz

Vor dem asynchronen Speichern wird ein Snapshot des PlayerProfiles erzeugt.

Damit wird nicht das mutable Live-Profil direkt an einen Hintergrundthread übergeben.

Das ist entscheidend.

### Befund

**PASS**

Dies reduziert Race Conditions zwischen:

- Gameplay-Mutation
- Autosave
- Quit-Save
- Shutdown-Save

---

# 18. Save Coalescing

`pendingSaves` verhindert mehrere gleichzeitige Saves desselben Spielers.

Zusätzlich existiert eine Save Chain pro UUID.

Das ist ein sinnvolles Backpressure-/Ordering-Modell.

### Risiko

Bei sehr hoher Mutationsrate bleibt die Semantik komplex. Der Code benötigt weiterhin Integrationstests, die schnelle Mutation + Save + Shutdown simulieren.

### Einstufung

**P1 Testbedarf, kein erkannter akuter Architekturbruch**

---

# 19. MySQL-Persistenz

MySQL verwendet:

- HikariCP
- Transaktionen
- Prepared Statements
- Revision Protection
- Tabellenmigration
- Shutdown

Die Speicherung umfasst unter anderem:

- Player
- aktive Quests
- Stats
- Equipment

### SQL-Injection

Dynamische Datenwerte werden überwiegend über Prepared Statements gebunden.

Tabellen-/Spaltennamen, die dynamisch benötigt werden, werden über strenge Identifier-Validierung geschützt.

### Befund

**PASS**

---

# 20. MySQL Revision Protection

Der Save-Pfad liest:

```
persistence_revision ... FOR UPDATE
```

und vergleicht den Datenbankstand mit der erwarteten Memory-Revision.

Bei Abweichung wird nicht still überschrieben.

### Bedeutung

Das schützt gegen Lost Updates und gegen veraltete Snapshots.

### Befund

**Sehr positiv**

---

# 21. Konkreter MySQL-Fallback-Befund

In `PlayerProfileManager.initialize()` wird:

1. DatabaseManager erstellt
2. Hikari-Pool geöffnet
3. MySQL Repository erstellt
4. Repository initialisiert

Wenn die Initialisierung danach fehlschlägt, wird auf YAML gewechselt.

Der Catch-Pfad schließt den zuvor erzeugten DatabaseManager nicht explizit.

### Risiko

Möglicher Ressourcenpfad:

```
Hikari geöffnet
   ↓
MySQL Repository init schlägt fehl
   ↓
YAML Fallback
   ↓
alte DataSource bleibt offen
```

### Einstufung

**P1 – konkreter Ressourcenfehler**

Dieser Punkt sollte behoben werden, ohne die bestehende Fallback-Funktionalität zu verändern.

---

# 22. Emergency Backup

Bei MySQL-Save-Fehlern wird ein YAML-Emergency-Repository verwendet.

Das verhindert, dass ein einzelner transienter SQL-Fehler automatisch als erfolgreicher Datenverlust endet.

### Positiv

- eigener Ordner
- separater Repository-Pfad
- eigenes Close im finally
- Fehlerlogging

### Fehlende Stufe

Es gibt kein vollständiges automatisches Recovery-/Reconciliation-System.

### Einstufung

**P2**

---

# 23. Datenbankmigrationen

Aktueller Schema-Stand:

```
CURRENT_SCHEMA_VERSION = 2
```

Es existieren Migrationen für:

- alte Player-Spalten
- Persistence Revision
- Geld auf Minor Units

Schema-Version wird in einer eigenen Tabelle gespeichert.

### Risiko

Migrationen verändern reale Datenstrukturen. Ein Backup vor Migration wird nicht innerhalb dieses Codes erzwungen.

### Empfehlung

Production-Migration sollte immer mit externem DB-Backup kombiniert werden.

### Befund

**Architektur PASS, Betriebsprozess ausbaufähig**

---

# 24. NPC-Persistenz

NPCs werden in:

```
npcs.yml
```

persistiert.

Gespeichert werden unter anderem:

- ID
- Typ
- Name
- Welt
- Position
- Rotation
- Skin Source
- Skin Value
- Skin Signature
- Profession

### Snapshot-Modell

Die Runtime-Daten werden auf dem Serverthread erfasst und anschließend off-thread serialisiert.

### Befund

**PASS**

---

# 25. Atomisches NPC-Dateischreiben

Der NPC-Manager schreibt zuerst:

```
npcs.yml.tmp
```

und versucht anschließend:

```
ATOMIC_MOVE
```

mit Fallback auf normalen Replace-Move.

### Bedeutung

Ein Crash während des Schreibens reduziert das Risiko einer halb geschriebenen Zieldatei.

### Befund

**Sehr positiv**

---

# 26. NPC Entity Lifecycle

NPCs werden als Mannequins erzeugt.

Sie werden:

- getrackt
- bei Chunk Load erzeugt
- bei Chunk Unload aus dem Tracking entfernt
- beim Shutdown entfernt
- explizit als nicht persistent markiert

Damit ist die Plugin-Datei die dauerhafte Wahrheit und die Minecraft-Entity nur das Runtime-Repräsentat.

### Befund

**Saubere Single-Source-of-Truth-Architektur**

---

# 27. NPC Skin Persistence

Der aktuelle Skinpfad ist forensisch besonders relevant.

Die Implementierung verwendet:

```
Skin Source
    ↓
Resolver
    ↓
ProfileProperty
    ↓
StoredSkin
    ↓
NpcSnapshot
    ↓
npcs.yml
```

Beim Neustart:

```
npcs.yml
    ↓
skin-value / skin-signature
    ↓
StoredSkin
    ↓
MannequinSkinResolver.applyStoredTexture()
    ↓
Mannequin.setProfile()
```

### Wichtiger Befund

Der Code speichert die **exakt akzeptierte Texture-Property**.

Er versucht nicht, nach einem Neustart das Runtime-Mannequin erneut als Quelle für die persistente Skininformation zu benutzen.

Das ist fachlich die richtige Richtung.

### Status

**Architektur PASS**

Die Tatsache, dass das Plugin bereits auf dem Server läuft und der aktuelle Stand spielbar ist, ergänzt diesen statischen Befund durch reale Betriebsverwendung.

Ein automatisierter CI-Restarttest fehlt trotzdem.

---

# 28. Skin-Resolver – Player Names

Spielernamen werden validiert auf:

- 3–16 Zeichen
- `[A-Za-z0-9_]+`

Der Resolver verwendet Paper/Bukkit Profile-Erzeugung und asynchrone Profilauflösung.

Das eigentliche Mannequin-Setzen wird zurück auf den Serverthread gelegt.

### Befund

**PASS**

---

# 29. Skin Resolver – aktuelles Paper Profilmodell

Paper 26.2 stellt weiterhin:

- `Mannequin#getProfile()`
- `Mannequin#setProfile(ResolvableProfile)`
- `ResolvableProfile`

bereit.

Der Code verwendet genau diese Mannequin-/ResolvableProfile-Linie.

Die `com.destroystokyo.paper.profile.ProfileProperty`-Klasse existiert in Paper 26.2 weiterhin. Die in der aktuellen API als deprecated markierten mutierenden Methoden `setId()` und `setName()` werden im geprüften Skin-Code nicht verwendet.

### Befund

**PASS mit kleinem Wartungshinweis**

---

# 30. Externe Skin-URLs

Externe URLs werden über `ExternalSkinService` verarbeitet.

Der Dienst beinhaltet:

- HTTP/HTTPS-Prüfung
- URL-Limit
- Request Timeout
- Response-Limit
- Redirect-Prüfung
- DNS/IP-Prüfung
- private-IP-Filter
- Candidate-Limit
- Content-Type-Prüfung
- MineSkin Response Validation

Das ist deutlich sicherer als ein einfacher ungeschützter URL-Fetch.

---

# 31. SSRF-Prüfung

Die Funktion `isSafeExternalUri()` prüft DNS-Adressen vor dem Zugriff.

Zusätzlich wird nach Redirects erneut geprüft.

Das ist gut.

### Verbleibendes Problem

Die Prüfung und der tatsächliche Netzwerkzugriff sind nicht derselbe atomare Vorgang:

```
DNS-Auflösung / Prüfung
        ↓
Zeitfenster
        ↓
HttpClient-Auflösung
        ↓
TCP-Verbindung
```

Ein kontrollierter DNS-Rebinding-/TOCTOU-Angriff ist daher prinzipiell nicht ausgeschlossen.

### Bewertung

- Für vertrauenswürdige Admin-Eingaben: **akzeptabler Basisschutz**
- Für untrusted User Input: **weiter härten**

### Einstufung

**P1 bei untrusted URLs, sonst P2**

---

# 32. Externe Skin-Dienstabhängigkeit

Für Bild-Skins kann MineSkin verwendet werden.

Damit existiert eine externe Laufzeitabhängigkeit.

Vorhanden:

- Timeout
- Fehlerbehandlung
- Response-Limit
- JSON-Strukturprüfung
- kein API-Key im Repository

Nicht vorhanden:

- globales Rate Limiting
- Circuit Breaker
- Backoff
- zentrale Serviceinstanz
- dauerhafter externer Skin-Cache

### Risiko

Viele gleichzeitige neue Skin-Auflösungen können unnötige externe Requests erzeugen.

### Einstufung

**P2**

---

# 33. HTTP-Client Lifecycle

`ExternalSkinService` erzeugt einen eigenen Java `HttpClient`.

Der Resolver wird bei Skin-Auflösung instanzweise erzeugt.

Der HTTP-Client besitzt keinen expliziten Plugin-Lifecycle-Shutdown.

Das ist nicht automatisch ein Ressourcenleck; Java verwaltet den HTTP-Client selbst.

Architektonisch wäre jedoch ein zentraler Skin/HTTP-Service besser:

```
Plugin
  ↓
SkinService
  ↓
ein HttpClient
  ↓
zentraler Cache
  ↓
Rate Limit / Metrics / Shutdown
```

### Einstufung

**P2**

---

# 34. Threading

Das Projekt trennt grundsätzlich:

- Bukkit/Paper Main Thread
- Player Profile I/O
- NPC Persistence
- externe HTTP-Auflösung

Die Skin-Callbacks werden vor dem Persistieren zurück auf den Serverthread gebracht.

### Befund

**Gut**

---

# 35. Dirty Callback Semantik

PlayerProfile kann Dirty-/Change-Callbacks auslösen.

Diese Callbacks werden synchron auf dem mutierenden Thread ausgeführt.

Das ist nicht per se falsch.

Es bedeutet jedoch:

- Callback-Code muss sehr leichtgewichtig bleiben.
- Listener dürfen dort kein blockierendes I/O durchführen.
- zukünftige Änderungen müssen diesen Vertrag respektieren.

### Einstufung

**P1 – Architekturvertrag dokumentieren**

---

# 36. Statische Live-Server-Objekte

Die Buildprüfung sucht statische:

- Player
- Entity
- World

Es wurde keine entsprechende problematische statische Speicherung gefunden.

Es existiert:

```
private static PixelRPGPlugin instance;
```

Das ist eine Plugin-Instanz und kein Live-Player-/Entity-/World-Objekt.

Beim Disable wird sie auf `null` gesetzt.

### Befund

**PASS**

---

# 37. Caches

Vorhandene Caches betreffen unter anderem:

- Player Profiles
- Loading Profiles
- Save Chains
- Pending Saves
- Player Skin/Profile Resolution
- NPC Resolved Skins
- externe Skin Properties

Die wichtigen Runtime-Caches werden beim Shutdown weitgehend geleert.

### Risiko

Der externe Skin-Cache ist instanzgebunden und damit nicht global nutzbar.

### Befund

**Kein akuter Memory-Leak-Nachweis**

---

# 38. Resourcepack

Das Repository enthält 49 Resourcepack-Dateien.

Darunter:

- `pack.mcmeta`
- Item Assets
- Item Models
- Food Assets
- Textur-/README-Dateien

### Positiv

Resourcepack ist versioniert und liegt im selben Repository.

### Fehlende Automatisierung

Der Plugin-Build prüft nicht vollständig:

- JSON-Syntax aller Resourcepack-Dateien
- Asset-Cross-References
- Model → Texture
- Plugin Item-ID → Resourcepack Asset

### Einstufung

**P1/P2 je nach Releaseprozess**

---

# 39. Content Pipeline

Plugin-Content liegt teilweise in JSON:

- Items
- Unique Items
- Food
- Equipment Sets
- Mob Scaling
- Boss Rewards
- Companions
- Quests
- Recipes

Die Trennung von Code und Content ist architektonisch sinnvoll.

### Risiko

Cross-References werden nicht durch einen dedizierten Build-Gate vollständig geprüft.

Beispiele:

```
Quest -> Item
Quest -> Companion
Recipe -> Item
Boss -> Loot
Item -> Resourcepack
```

### Empfehlung

Ein deterministischer Content-Linter sollte vor `build` laufen.

---

# 40. Content Validation

Minimal notwendige Checks:

- JSON syntaktisch gültig
- IDs eindeutig
- Pflichtfelder vorhanden
- Quest-Referenzen existent
- Item-Referenzen existent
- Rezept-Referenzen existent
- Companion-Referenzen existent
- Boss-Loot gültig
- Resourcepack-Assets vorhanden

### Status

**OFFEN**

---

# 41. Command Security

Administrative Commands verwenden Permission:

```
rpg.admin
```

Player-Funktionen:

```
rpg.member
```

`rpg.admin` ist standardmäßig OP.

### Positiv

Die Berechtigungsgrenze ist im Plugin-Metadatensystem sichtbar.

### Restrisiko

Die eigentliche Autorisierung muss zusätzlich in jedem sensiblen Subcommand korrekt durchgesetzt werden. Die statische Forensik kann nicht jede mögliche Command-State-Kombination als Runtime-Test ersetzen.

---

# 42. Datenvalidierung

Mehrere Systeme validieren IDs und Eingaben explizit.

Beispiele:

- NPC IDs
- NPC Typen
- Professionen
- MySQL Host
- MySQL Database Identifier
- Ports
- Skin URLs
- Player Skin Names

### Befund

**Gut**

---

# 43. SQL Injection

Datenwerte werden überwiegend über Prepared Statements verarbeitet.

Dynamische SQL-Identifier werden validiert.

### Befund

**Kein offensichtlicher SQL-Injection-Pfad im geprüften Storage-Code**

---

# 44. Secret Handling

Die MySQL-Konfiguration enthält Passwortfelder, aber es wurde kein hartcodiertes Secret im geprüften Code festgestellt.

GitHub Actions verwendet keine offensichtlichen Repository-Secrets für den Plugin-Build.

### Hinweis

Produktionsdatenbankpasswörter gehören ausschließlich in Serverkonfiguration bzw. Secret Management, niemals in Git.

---

# 45. Logging

Das Plugin besitzt umfangreiches Logging.

Positiv:

- Skin-Resolution-Fehler
- Persistence-Fehler
- Shutdown-Fehler
- DB-Fehler
- NPC-Persistenz
- Migrationen

### Skin Logging

Texture-Werte werden gekürzt, statt vollständige Base64-Daten dauerhaft auszugeben.

Das ist sinnvoll.

### Problem

Einige Skin-Ereignisse sind `INFO` statt Debug.

### Einstufung

**P2**

---

# 46. Fehlerbehandlung

Das Plugin verwendet:

- `CompletableFuture.failedFuture`
- Exception Logging
- Dirty Recovery
- Emergency Backup
- Shutdown Fallback
- Transaktionsrollback

### Positiv

Fehler werden nicht überall still verschluckt.

### Restrisiko

Es existiert keine zentrale Fehlerklassifikation wie:

- ConfigurationError
- PersistenceError
- ContentError
- ExternalServiceError
- RuntimeStateError

### Einstufung

**P2 Wartbarkeit**

---

# 47. Shutdown-Timeouts

Player I/O wartet bis zu:

```
30 Sekunden
```

anschließend wird der Executor heruntergefahren und nach weiteren 10 Sekunden ggf. hart beendet.

NPC Persistence wartet bis zu:

```
10 Sekunden
```

### Bewertung

Das ist besser als ein unbegrenztes Blockieren.

Es bedeutet aber:

**Keine absolute Garantie, dass jeder Save erfolgreich abgeschlossen wurde.**

Für Production ist deshalb Recovery genauso wichtig wie Shutdown.

---

# 48. NPC Skin Shutdown Race

Ein Skin kann während des Shutdowns noch in einem externen HTTP-Request sein.

Möglicher Ablauf:

```
HTTP request gestartet
      ↓
Server shutdown
      ↓
NPC save snapshot
      ↓
HTTP response kommt später
      ↓
skin callback versucht persistieren
```

Der Code besitzt Schutz gegen `shuttingDown` im Persistenzpfad.

Damit wird ein sauberer Shutdown bevorzugt.

### Konsequenz

Ein gerade erst aufgelöster Skin kann bei Shutdown theoretisch nicht mehr persistiert werden, wenn die Resolution zu spät abgeschlossen wird.

### Einstufung

**P2 Edge Case**

---

# 49. Reload-Sicherheit

Der Shutdown ist relativ gut strukturiert.

Nicht vollständig bewiesen ist jedoch ein kompletter Reload-Lifecycle:

```
enable
↓
disable
↓
enable
```

Zu prüfen wären:

- doppelte Listener
- doppelte Commands
- doppelte Scheduler
- alte Executor
- alte HTTP-Clients
- alte Services
- statische Referenzen

### Einstufung

**P1 Testbedarf**

---

# 50. Event Listener

Das Plugin registriert viele Listener direkt in `onEnable()`.

Das ist grundsätzlich normal.

Bei Listener-Klassen wurde projektseitig die gewünschte Trennung in einzelne Verantwortlichkeiten weitgehend eingehalten.

### Positiv

Fachlogik ist nicht vollständig in einem gigantischen Event Listener konzentriert.

### Hinweis

Für zukünftige Änderungen sollte die bestehende Listener-Grenze beibehalten werden.

---

# 51. Scheduler

Vorhanden sind:

- periodische Tasks
- One-shot Tasks
- Executor Services
- Wake-/Coalescing-Mechanismen

### Positiv

Mehrere Tasks besitzen explizite Stop-/Shutdown-Methoden.

### Risiko

Die Gesamtzahl der periodischen Tasks macht Runtime-Smoke-Tests wertvoll.

---

# 52. Boss-System

Das Boss-System umfasst:

- Definitionen
- Phasen
- Attack Patterns
- Spawn Task
- Damage Contribution
- Death Handling
- Loot
- Protection

Die Architektur ist modular.

### Befund

**Architektur PASS**

Eine vollständige Balance-/Gameplay-Bewertung ist nicht Gegenstand dieser Codeforensik.

---

# 53. Combat-System

Vorhanden:

- Damage Calculator
- Damage Context
- Combat State
- Mob Experience
- Scaling
- Skills
- Weapon Ability Engine
- Loot

### Architektur

Die Berechnung ist von Eventhandling getrennt.

### Befund

**Gut modularisiert**

---

# 54. Quest-System

Vorhanden:

- Quest Repository
- Quest Manager
- passive Checks
- Mob Kill Listener
- globaler Event State
- Player Persistence
- Dialogintegration

### Hauptrisiko

Cross-Reference-Fehler zwischen Questdefinitionen und Content können ohne Content-Linter erst beim Runtime-Load auffallen.

---

# 55. Profession / Crafting

Das System umfasst:

- Profession Registry/System
- Crafting Services
- Crafting GUI
- Rezeptdaten
- Player Unlocks

### Befund

Architektur ist datengetrieben.

### Testbedarf

Rezept-Linting sollte sicherstellen:

- Kategorie vorhanden
- gültige Zutaten
- gültige Result-IDs
- eindeutige IDs

---

# 56. Item-System

Vorhanden:

- Item Definitions
- Item Builder
- Economy
- Display Names
- Equipment
- Unique Items
- Scaling

### Positiv

Items sind überwiegend datengetrieben.

### Risiko

Datenkonsistenz zwischen Code und Resourcepack ist nicht vollständig als Build-Gate abgesichert.

---

# 57. Companion-System

Vorhanden:

- Companion Service
- Experience
- Dialogintegration
- Contentdaten
- Quest Rewards

### Befund

Modular.

### Testbedarf

Companion IDs und Quest Rewards sollten vom Content-Linter geprüft werden.

---

# 58. Guild / Party / Economy

Services werden über die Bukkit/Paper Service Registry registriert.

Beispiele:

- GuildAPI
- EconomyAPI
- PartyAPI
- StatisticsAPI
- ItemAPI

Beim Shutdown werden die registrierten Services teilweise explizit entfernt.

### Befund

**PASS**

---

# 59. Service Registry Hygiene

Service-Registrierung ist grundsätzlich korrekt.

Zu beachten ist:

- jeder Service muss beim Disable wieder entfernt werden
- Reload darf keine doppelte Registrierung erzeugen
- Consumers müssen nicht auf eine bereits deaktivierte Instanz zeigen

Der aktuelle Code setzt beim Shutdown entsprechende unregister-Schritte um.

### Testbedarf

**Reload-Integrationstest fehlt.**

---

# 60. Adventure / Chat

Der Code verwendet Adventure Components.

Es wurde kein `ChatColor`-Verstoß festgestellt.

Dialogtexte verwenden ebenfalls Components.

### Befund

**PASS**

---

# 61. NMS Boundary

Es wurde keine Legacy-Struktur gefunden:

```
net.minecraft.server.v1_XX_RY
```

Ebenso kein CraftBukkit.

### Befund

**PASS**

Das Plugin hält die gewünschte API-Grenze gut ein.

---

# 62. Statische Serverreferenzen

Keine offensichtlichen statischen:

- Player
- Entity
- World

Die Plugin-Singleton-Referenz wird beim Disable gelöscht.

### Befund

**PASS**

---

# 63. Build- und CI-Forensik

Der aktuelle Workflow:

```
.github/workflows/build.yml
```

führt aus:

1. Checkout
2. Java 25
3. Gradle 9.2.0
4. `gradle clean build`
5. Source Boundary Verification
6. Artifact Verification

Der aktuelle Lauf für Commit `e144a9005d1c295c237be606e2c49162a7da536d`:

```
Run: 35950748101
Status: completed
Conclusion: success
```

### Befund

**CI PASS**

---

# 64. Was die CI tatsächlich beweist

Die CI beweist aktuell:

- Projekt kompiliert
- Build läuft durch
- eigene Verification Tasks laufen
- ShadowJar wird gebaut
- `paper-plugin.yml` ist im Artifact
- Plugin-Klassen sind vorhanden
- Third-Party-Relocations werden geprüft
- JDBC Service Descriptor wird geprüft

Das ist ein belastbarer Build-Nachweis.

---

# 65. Was die CI nicht beweist

Die CI beweist nicht:

- Paper-Server startet erfolgreich
- Plugin aktiviert vollständig
- MySQL-Verbindung funktioniert real
- Migration funktioniert gegen reale Daten
- NPC Skin bleibt nach Restart sichtbar
- Dialoge öffnen clientseitig korrekt
- Commands funktionieren ingame
- Resourcepack funktioniert im Client
- Bossmechaniken funktionieren
- Questflows funktionieren
- Regiontransitions funktionieren
- Shutdown speichert jeden Zustand

Diese Lücke ist wichtig, aber sie ist kein Beweis für einen Runtime-Defekt.

---

# 66. Testbestand

Im Repository ist keine erkennbare `src/test`-Test-Suite vorhanden.

Damit fehlt eine klassische automatisierte Unit-/Integration-Testschicht.

### Konsequenz

Der Build ist stark static/build-orientiert, aber runtime-orientierte Regressionen sind schwer automatisch reproduzierbar.

### Einstufung

**P1**

---

# 67. Empfohlener Smoke-Test

Minimal:

```
Paper 26.2 Server starten
        ↓
PixelRPG laden
        ↓
keine Enable Exception
        ↓
Commands registriert
        ↓
Content geladen
        ↓
NPC Daten geladen
        ↓
Server sauber stoppen
```

Das sollte in CI reproduzierbar laufen.

---

# 68. Empfohlener NPC-Skin-Restart-Test

Deterministischer Test:

1. Server starten.
2. NPC mit Skin Source laden.
3. Skin auflösen.
4. `npcs.yml` prüfen.
5. `skin-value` vorhanden.
6. Server stoppen.
7. Server neu starten.
8. NPC laden.
9. `skin-value` erneut laden.
10. `Mannequin.getProfile()` prüfen.
11. Texture Property vergleichen.

### Ziel

Damit wird exakt der aktuelle Hauptpersistenzpfad automatisiert bewiesen.

---

# 69. Empfohlener MySQL Integration Test

CI sollte einen temporären MySQL-Service verwenden.

Testfälle:

- Schema creation
- Migration v0 -> v1
- Migration v1 -> v2
- Load
- Save
- Transaction rollback
- Revision Conflict
- Equipment
- Active Quests
- Statistics
- Shutdown

---

# 70. Empfohlener Content Linter

Ein Build Task wie:

```
validatePixelRpgContent
```

sollte prüfen:

- JSON parse
- Pflichtfelder
- IDs
- Duplikate
- Referenzen
- Rezepte
- Quest Items
- Companion IDs
- Boss Loot
- Item Scaling
- Resourcepack Assets

und vor `build` laufen.

---

# 71. Security – Gesamtbewertung

### Stark

- SQL Prepared Statements
- Identifier Validation
- Permission Boundaries
- URL Validation
- Response Limits
- HTTP Timeouts
- Redirect Validation
- Private Address Filtering
- kein hardcodierter DB Secret
- kein Legacy NMS

### Offen

- DNS Rebinding
- Rate Limiting externer Skin Requests
- zentraler HTTP Service
- Content Cross-Reference Validation

### Gesamt

**Gute Sicherheitsbasis mit einem klaren kritischen Netzwerkbereich bei externen Skin-URLs.**

---

# 72. Datenintegrität

Starke Maßnahmen:

- Player Revision
- SQL Transactions
- NPC Snapshot
- Atomic File Move
- Dirty State
- Emergency Backup
- Save Chain

### Schwächen

- kein allgemeines File Integrity Manifest
- kein automatisiertes Recovery-Verifikationssystem
- keine umfassende Content-Linting-Schicht

### Befund

**Gut bis sehr gut**

---

# 73. Memory Safety / Leak Review

Geprüft wurden insbesondere:

- statische Live-Objekte
- Executor Lifecycle
- Cache Clearing
- Service Unregister
- NPC Entity Removal
- HTTP Client Ownership

Kein offensichtlicher schwerer dauerhafter Player-/Entity-/World-Leak wurde im geprüften Code gefunden.

### Bekannte Risiken

- MySQL fallback pool
- instanzweise HTTP Clients
- Reload nicht automatisiert geprüft

---

# 74. Concurrency Review

### Positiv

- ConcurrentHashMap
- Save Chain
- Snapshot
- single-thread NPC persistence
- async HTTP
- main-thread handoff
- shutdown guards

### Risiken

- Callback Thread Contract
- HTTP completion während Shutdown
- komplexe Save Chain Semantik
- fehlende Integrationstests für konkurrierende Mutationen

### Befund

**Gut strukturiert, aber testseitig unterbelegt**

---

# 75. File I/O

NPC:

- Snapshot
- Temporary File
- Atomic Move
- Fallback Move

Player YAML:

- eigenes Repository
- Emergency Repository

### Befund

**Gute Grundlage**

---

# 76. YAML Risiken

YAML ist für:

- NPCs
- Emergency Profile
- ggf. Player Storage

im Einsatz.

YAML ist leicht administrierbar, aber nicht so transaktionssicher wie SQL.

Die NPC-Implementierung kompensiert dies teilweise durch atomisches Schreiben.

### Empfehlung

Für kritische Player-Persistenz bleibt MySQL die geeignetere Production-Variante, wenn DB-Betrieb vorhanden ist.

---

# 77. Observability

Vorhanden:

- Logger
- Warnungen
- Severe Errors
- Skin-Diagnostik
- Shutdown Logs
- Persistence Logs

Fehlend:

- Metrics
- Save latency
- DB pool utilization
- Skin resolution latency
- external request count
- failed save count
- NPC load count
- active profile count

### Einstufung

**P2**

---

# 78. Betriebsdokumentation

Vorhandene Dokumente helfen bei:

- Content
- Rezepten
- Resourcepack
- Forensik

Eine zentrale Production-Betriebsdokumentation sollte zusätzlich beschreiben:

- Java
- Paper
- MySQL
- DB Rechte
- SSL
- Backup
- Restore
- Migration
- Emergency Recovery
- Skin Resolver
- externe Dienste
- Restart-Verhalten

### Einstufung

**P2**

---

# 79. Upgrade-Sicherheit

Die Bindung an Paper 26.2 ist explizit.

Das ist positiv, weil die Zielplattform eindeutig ist.

Bei einem Upgrade müssen besonders geprüft werden:

- Dialog API
- ResolvableProfile
- Mannequin
- Registry Events
- BasicCommand
- Paper Plugin Lifecycle
- Data Components

### Befund

**Aktuell sauber versioniert**

---

# 80. API-Modernität

Kein Legacy-NMS.

Kein CraftBukkit.

Kein ChatColor.

Paper Plugin Lifecycle.

Paper Dialog Registry.

Mannequin + ResolvableProfile.

Adventure Components.

### Befund

**Sehr gut**

Ein kleiner Wartungspunkt bleibt bei historischen `com.destroystokyo.paper.profile` Typen; sie sind in Paper 26.2 weiterhin vorhanden, aber die API-Landschaft bewegt sich weiter Richtung aktuelle Profile-/Data-Component-APIs.

---

# 81. Resourcepack Build Gate

Aktuell wird das Resourcepack nicht als eigener CI-Artefakt-/Lint-Schritt geprüft.

Das bedeutet:

```
Plugin build PASS

eq
Resourcepack vollständig validiert
```

### Einstufung

**P1/P2**

---

# 82. Backup / Recovery

Vorhanden:

- Emergency YAML
- DB Transaktionen
- atomare NPC Files

Fehlend:

- automatische Backup Rotation
- Restore Tool
- Recovery Audit
- Backup Integrity Hash
- Admin Recovery Command

### Einstufung

**P2**

---

# 83. Release-Prozess

Aktuell ist der Buildprozess:

```
compile
↓
verification
↓
shadow
↓
artifact validation
```

Ein vollständiger Release Gate wäre:

```
compile
↓
static checks
↓
content lint
↓
shadow validation
↓
Paper boot
↓
plugin enable
↓
smoke test
↓
persistence test
↓
MySQL test
↓
restart test
↓
shutdown test
↓
artifact
```

---

# 84. Positive Architekturmerkmale

Besonders positiv:

- klare Modulstruktur
- Java 25
- Paper 26.2
- Paper Plugin
- Bootstrapper
- Dialog Registry
- Adventure Components
- BasicCommand Lifecycle Registration
- Snapshot Persistence
- Revision Protection
- SQL Transactions
- Emergency Backup
- Atomic NPC Writes
- Async Profile I/O
- Executor Shutdown
- NPC Single Source of Truth
- Skin Property Persistence
- Shadow Relocation
- Build Boundary Checks
- Artifact Checks
- keine Legacy-NMS-Struktur
- keine ChatColor
- keine offensichtlichen statischen Live-Entity-Leaks

---

# 85. Konkrete Befundmatrix

| ID | Bereich | Befund | Schwere | Status |
|---|---|---|---|---|
| F-01 | Build | Java/Paper/Shadow Basis korrekt | niedrig | PASS |
| F-02 | API | Legacy NMS/CraftBukkit/ChatColor nicht gefunden | hoch | PASS |
| F-03 | Plugin | paper-plugin.yml + Bootstrapper | hoch | PASS |
| F-04 | Dialog | native Paper Dialog Registry | hoch | PASS |
| F-05 | Lifecycle | idempotenter Coordinator | hoch | PASS |
| F-06 | Player I/O | Async Snapshot Persistenz | hoch | PASS |
| F-07 | MySQL | Transaction + Revision Protection | hoch | PASS |
| F-08 | MySQL | Fallback schließt möglichen Hikari-Pool nicht explizit | hoch | OFFEN |
| F-09 | NPC | Atomic Snapshot File | hoch | PASS |
| F-10 | NPC Skin | exact texture persistence | hoch | PASS |
| F-11 | Skin Security | SSRF-Basis vorhanden | hoch | PASS mit Rest-Risiko |
| F-12 | Skin Security | DNS-Rebinding/TOCTOU möglich | hoch | OFFEN |
| F-13 | Commands | Paper Lifecycle Registration | mittel | PASS |
| F-14 | Commands | interne Bukkit Adapter | niedrig | OFFEN |
| F-15 | CI | Build erfolgreich | hoch | PASS |
| F-16 | CI | kein Paper Runtime Smoke Test | hoch | OFFEN |
| F-17 | Tests | keine erkennbare src/test Suite | hoch | OFFEN |
| F-18 | Content | kein vollständiger Content Linter | hoch | OFFEN |
| F-19 | Resourcepack | kein vollständiger Asset Linter | mittel | OFFEN |
| F-20 | Reload | kein automatisierter Reload Test | mittel | OFFEN |
| F-21 | HTTP | Client instanzweise erzeugt | mittel | OFFEN |
| F-22 | Logging | Skin INFO-Logging relativ laut | niedrig | OFFEN |
| F-23 | Build | Gradle Wrapper fehlt | niedrig | OFFEN |
| F-24 | Docs | Production Betriebsdokumentation fehlt | niedrig | OFFEN |

---

# 86. Priorisierung

## P0 – vor einem Anspruch auf vollständig automatisierte Release-Sicherheit

### P0-01 Runtime Smoke Test
Ein echter Paper-26.2-Server muss in CI starten.

### P0-02 Restart Persistence Test
NPC-Skin und Player Persistence müssen über einen echten Restart geprüft werden.

### P0-03 Content Linting
Content muss vor Runtime geladen werden können und alle Referenzen müssen valide sein.

---

## P1 – konkrete technische Risiken

### P1-01 MySQL Fallback Pool schließen

Im Fallback-Catch muss ein bereits initialisierter DatabaseManager geschlossen werden.

### P1-02 Skin SSRF härten

Wenn URLs aus nicht vollständig vertrauenswürdigen Quellen stammen, muss die DNS-/Verbindungsstrategie gegen Rebinding härter werden.

### P1-03 Concurrency Contract

Dirty-/Change-Callbacks müssen klar als synchroner, nicht-blockierender Callback-Vertrag behandelt werden.

### P1-04 Reload Test

Enable -> Disable -> Enable automatisieren.

---

## P2 – Wartbarkeit

### P2-01 Gradle Wrapper

### P2-02 Zentraler HTTP/Skin Service

### P2-03 Content/Resourcepack Cross-Reference Tooling

### P2-04 Debug-Level für Skin Diagnostics

### P2-05 Production Operations Documentation

### P2-06 Metrics

---

# 87. Was NICHT getan werden sollte

Auf Basis dieser Forensik besteht **kein Anlass für einen großflächigen Rewrite**.

Insbesondere nicht:

- Player Persistence komplett ersetzen
- NPC-System neu bauen
- Dialogsystem neu bauen
- Skin-System erneut komplett umbauen
- MySQL durch eine andere Datenbank ersetzen
- ShadowJar-Konzept ersetzen
- Paper Plugin System zurück auf Legacy umbauen

Die bestehenden Architekturen sind überwiegend sinnvoll.

Die richtige nächste Stufe ist Absicherung, nicht Neuimplementierung.

---

# 88. Spezieller Skin-Befund

Der aktuelle Skin-Code hat eine wichtige qualitative Verbesserung gegenüber einem reinen Source-ReResolve-Ansatz:

```
Source
  ↓
Resolution
  ↓
accepted ProfileProperty
  ↓
persisted exact value/signature
  ↓
restart
  ↓
restore exact property
```

Damit ist der Persistenzzustand unabhängig von:

- späterer HTTP-Erreichbarkeit
- Änderung der externen Webseite
- Änderung eines Player-Skins
- Änderung der Runtime-Mannequin-Property

Das ist für persistente NPC-Skins die richtige Datenhaltung.

---

# 89. Runtime-Aussage zum aktuellen Projekt

Der Projektstatus besagt, dass das Plugin bereits auf einem Server installiert und spielbar ist.

Die statische Prüfung bestätigt dazu:

- vollständige Plugin-Struktur
- registrierte Systeme
- aktuelle Paper APIs
- Persistence Paths
- Shutdown Paths
- Build PASS

Damit gibt es keinen forensischen Hinweis darauf, dass der aktuelle Branch nur ein theoretischer oder nicht startbarer Prototyp wäre.

Die GitHub-Forensik kann allerdings nicht selbst behaupten, den Server gestartet und alle Gameplay-Funktionen reproduziert zu haben.

Diese Grenze wird bewusst nicht verwischt.

---

# 90. Release-/Production-Einschätzung

Der aktuelle Stand ist:

**technisch fortgeschritten und serverfähig, aber noch nicht vollständig automatisiert release-proven.**

Das ist eine präzise Unterscheidung:

```
BUILD PASS
+
ARCHITECTURE STRONG
+
RUNTIME IN USE
=
aktueller produktiver Entwicklungsstand
```

aber:

```
BUILD PASS
≠
vollständige automatisierte Runtime-Verifikation
```

---

# 91. Empfohlene Reihenfolge für weitere Arbeit

1. Bestehendes funktionierendes Verhalten unangetastet lassen.
2. MySQL-Fallback-Ressource schließen.
3. Content Linter ergänzen.
4. Paper-26.2 Smoke Test ergänzen.
5. NPC Skin Restart Test ergänzen.
6. MySQL Integration Test ergänzen.
7. Reload/Shutdown Test ergänzen.
8. SSRF Schutz weiter härten, sofern Skin URLs untrusted sein können.
9. Gradle Wrapper ergänzen.
10. erst danach kleinere Hygiene-/Refactoringarbeiten.

---

# 92. Finaler technischer Status

## Build

**PASS**

## Java 25

**PASS**

## Paper 26.2

**PASS**

## Paper Plugin System

**PASS**

## Bootstrapper

**PASS**

## Dialog System

**PASS**

## Adventure

**PASS**

## Legacy NMS

**PASS / keine Funde**

## CraftBukkit

**PASS / keine Funde**

## ChatColor

**PASS / keine Funde**

## ShadowJar

**PASS**

## Third-Party Relocation

**PASS**

## JDBC Relocation

**PASS**

## Player Persistence

**PASS – solide Architektur**

## MySQL

**PASS – mit Fallback-Ressourcenbefund**

## NPC Persistence

**PASS**

## NPC Skin Persistence

**PASS – Architektur fachlich sauber**

## NPC Skin Security

**GOOD – SSRF-Härtung weiter möglich**

## Lifecycle

**PASS**

## Shutdown

**PASS – begrenzte Zeitfenster**

## Concurrency

**GOOD – Integrationstests fehlen**

## Content Validation

**OFFEN**

## Resourcepack Validation

**OFFEN**

## Runtime CI

**OFFEN**

## Restart CI

**OFFEN**

## MySQL Integration CI

**OFFEN**

## Reload CI

**OFFEN**

## Documentation

**GUT – Production Operations ausbaufähig**

---

# 93. Schlussbefund

PixelRPG besitzt am geprüften Stand eine **reale, umfangreiche und bereits nutzbare Plugin-Architektur**.

Die wichtigsten Systeme sind nicht nur als Platzhalter vorhanden. Es existieren echte:

- Persistenz
- Datenbank
- NPCs
- Skins
- Dialoge
- Quests
- Items
- Berufe
- Combat
- Bosse
- Companions
- Regionen
- Gilden
- Parties
- Shops
- Commands
- Resourcepack-Integration

Die Forensik findet keinen Anlass, diese funktionierenden Systeme grundsätzlich neu zu schreiben.

Der zentrale technische Nachholbedarf liegt in der **Beweisführung**:

```
statischer Code
      +
grüner Build
      +
laufender Server
      ↓
noch nicht vollständig automatisierte Release-Evidenz
```

Die stärkste technische Verbesserung für die nächste Phase wäre deshalb eine Test- und Verification-Pipeline, die das bereits funktionierende System reproduzierbar beweist.

Der aktuelle Stand sollte forensisch nicht als "kaputt" oder "unfertig" bezeichnet werden.

Die präzisere Einstufung lautet:

```
FUNCTIONALLY IN USE
BUILD VERIFIED
ARCHITECTURE STRONG
RUNTIME PARTIALLY PROVEN
RELEASE AUTOMATION NOT SEALED
```

Das ist der belastbare Befund des aktuellen `test`-Branches.

---

## 94. Primäre geprüfte Dateien

Besonders tief geprüft wurden:

- `build.gradle`
- `settings.gradle`
- `src/main/resources/paper-plugin.yml`
- `.github/workflows/build.yml`
- `PixelRPGPlugin.java`
- `PixelRPGBootstrap.java`
- `LifecycleCoordinator.java`
- `PlayerProfileManager.java`
- `MySQLPlayerProfileRepository.java`
- `DatabaseManager.java`
- `NpcManager.java`
- `RPGNpc.java`
- `MannequinSkinResolver.java`
- `ExternalSkinService.java`
- Dialog-/Quick-Action-Komponenten
- Content-/Resourcepack-Struktur

Zusätzlich wurde der vollständige Git Tree des Branches hinsichtlich Umfang, Modulstruktur, Java-Dateien, Content und Resourcepack untersucht.

---

## 95. Externe API-Verifikation

Für die aktuelle Paper-26.2-API wurden insbesondere überprüft:

- Mannequin Profile API
- ResolvableProfile
- aktuelle Profile-/Property-Struktur
- Paper Deprecated API List

Die Prüfung bestätigt, dass `Mannequin#getProfile()`, `Mannequin#setProfile(ResolvableProfile)` und `ResolvableProfile` in Paper 26.2 vorhanden sind.

Die Paper-API führt weiterhin `com.destroystokyo.paper.profile` als vorhandenen API-Bereich. Die konkret problematischen mutierenden Profilmethoden `setId()` und `setName()` werden vom geprüften Code nicht verwendet.

---

## 96. Forensische Gesamtklassifikation

```
Repository Integrity       PASS
Branch Discipline          PASS
Build Configuration        PASS
Java 25                    PASS
Paper 26.2                 PASS
Paper Plugin System        PASS
Dialog Architecture        PASS
Command Registration       PASS
Adventure Text             PASS
Legacy API Boundary        PASS
ShadowJar                  PASS
JDBC Relocation            PASS
Player Persistence         PASS / one fallback issue
NPC Persistence            PASS
NPC Skin Persistence       PASS
Security                   GOOD / external URL hardening open
Concurrency                GOOD / test coverage open
Lifecycle                  PASS
Shutdown                   PASS / bounded
CI Build                   PASS
Runtime CI                 NOT PRESENT
Restart CI                 NOT PRESENT
MySQL Integration CI       NOT PRESENT
Content Linting            NOT PRESENT
Resourcepack Linting       NOT PRESENT
Reload Test                NOT PRESENT
Release Automation         NOT SEALED
```

**Forensik-Stand:** 2026-09-24  
**Geprüfter Commit:** `e144a9005d1c295c237be606e2c49162a7da536d`
