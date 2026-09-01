# PixelRPG — Forensisches Architektur-, Performance- und Security-Audit

**Branch:** `test`  
**Audit-Basis:** aktueller Branch-Zustand zum Auditzeitpunkt 2026-09-01  
**Zielplattform:** Paper 26.2 / Java 25 / Mojang-Mappings / `paper-plugin.yml`  
**Urteil:** **NICHT PRODUCTION-READY**

---

## 0. Executive Verdict

PixelRPG ist kein schlechter Prototyp. Die Branch hat bereits mehrere richtige Architekturentscheidungen: `paper-plugin.yml`, Paper Bootstrapper, native Dialog-Registrierung, Adventure Components, PDC-basierte Item-Identität, asynchrones Profil-I/O, HikariCP, ein Chunk-basierter Region-Index und eine erkennbare Trennung nach Domänen.

Das Problem ist die nächste Entwicklungsstufe: Die Anwendung ist aktuell **zu stark über `PixelRPGPlugin` verdrahtet**, Listener enthalten noch Domänenlogik, mehrere Repositories führen synchrone Datei-/Datenbankoperationen aus dem Laufzeitpfad aus, PlayerProfile-Daten werden zwischen Main Thread und I/O-Threads ohne sauberes Snapshot-/Actor-Modell geteilt, und einige Systeme skalieren bei wachsender Welt-/NPC-/Region-Anzahl unnötig linear.

Die kritischsten Befunde sind:

1. **CRITICAL — PlayerProfile wird während asynchronem Persistieren weiter mutiert.** Das erzeugt Race Conditions und potenziell inkonsistente Saves.
2. **CRITICAL — Region `create()`/`delete()`/`save()` schreibt YAML synchron.** Admin-Aktionen können den Main Thread blockieren; bei großen Region-Dateien wird das sichtbar.
3. **CRITICAL — `RegionManager.isExplicitSpawnPoint()` scannt sämtliche Regionen und deren Spawnpunkte.** Dieser Pfad ist für ein Spawn-Event absolut nicht akzeptabel, wenn die Datenmenge wächst.
4. **HIGH — `PixelRPGPlugin` ist ein God Object/Composition Root ohne klare Modulgrenzen.** Über 30 Services, Listener und Tasks hängen direkt daran.
5. **HIGH — Listener sind nicht strikt logikfrei.** `RegionListener` entscheidet direkt über PvP, Mob-Spawns, Explosionen und Transition-UI.
6. **HIGH — Shadow-Relocation von MySQL ist runtime-riskant.** `META-INF/services` und der explizite Driver-Name müssen bei Shading/Relocation sauber behandelt werden; die aktuelle Konfiguration enthält keinen `mergeServiceFiles()`-Schritt und setzt `com.mysql.cj.jdbc.Driver` als String.
7. **HIGH — Shadow-Relocation von Gson ist unnötig riskant, wenn Gson-Objekte über Modul-/Paper-Grenzen gelangen.** Gson sollte intern bleiben; die Relocation muss bewusst als Boundary behandelt werden.
8. **HIGH — Externe Mannequin-Skins funktionieren nicht mit beliebigen Bild-URLs direkt.** Die Implementierung verlangt für allgemeine URLs einen MineSkin-API-Key; nur `textures.minecraft.net` wird speziell behandelt.
9. **HIGH — Quest-/Item-Namensauflösung ist zwar zentralisiert, aber nicht wirklich überall single-source-of-truth.** `QuestText` enthält eine große Hardcoded-Namensmap und erst danach den Resolver. Dadurch können alte/abweichende IDs weiterhin als Rohschlüssel erscheinen.
10. **MEDIUM — `ExternalSkinService` besitzt einen statischen, unbounded Cache.** Bei vielen verschiedenen URLs wächst der Cache unbegrenzt über die Lebenszeit des JVM-Prozesses.
11. **MEDIUM — mehrere Tasks/Services besitzen eigene Lifecycle-Mechanismen.** Das ist fehleranfällig; Shutdown muss zentral und idempotent sein.
12. **MEDIUM — globale statische Zustände wie `RPGItemBuilder`-Scaling und `PixelRPGPlugin.instance` erschweren Tests, Reload-/Restart-Sicherheit und Isolation.**

---

# 1. IST-Architektur

## 1.1 Package-Schnitt

Die Branch besitzt bereits folgende erkennbare Domänen:

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
- `quest`
- `region`
- `scoreboard`
- `shop`
- `stats`
- `story`
- `storage`
- `travel`

Das ist grundsätzlich der richtige Ansatz. Der Fehler liegt nicht in den Package-Namen, sondern in der **Abhängigkeitenrichtung**.

Aktuell ist `PixelRPGPlugin` der zentrale Verdrahtungsknoten für praktisch alles. Die Domänen werden damit faktisch zu einem Modul mit vielen Packages statt zu echten Modulen.

### SOLL

```text
pixelrpg
├── bootstrap
│   └── Paper Bootstrap + Registry/Lifecycle
├── application
│   ├── PixelRpgApplication
│   ├── ServiceRegistry
│   └── LifecycleCoordinator
├── domain
│   ├── player
│   ├── quest
│   ├── item
│   ├── profession
│   ├── combat
│   ├── boss
│   ├── region
│   ├── npc
│   └── economy
├── application-service
│   └── Use-Case-Services
├── infrastructure
│   ├── mysql
│   ├── yaml
│   ├── http
│   └── serialization
├── paper
│   ├── listener
│   ├── command
│   ├── gui
│   ├── dialog
│   └── entity
└── api
```

Wichtig: Das ist **keine kosmetische Package-Umbenennung**. Die Abhängigkeit muss einseitig werden:

```text
Paper Listener / Command / GUI
            ↓
Application Services / Use Cases
            ↓
Domain
            ↓
Repository Interfaces
            ↓
Infrastructure
```

Nie umgekehrt.

---

# 2. `PixelRPGPlugin` — God Object

`PixelRPGPlugin` erzeugt und registriert direkt PlayerProfileManager, ProfessionSystem, ItemService, EquipmentService, QuestManager, RegionManager, BossManager, NPC-System, Scoreboard, Companion-System, mehrere Listener und zahlreiche Tasks.

Das ist als Composition Root akzeptabel, aber die aktuelle Klasse ist bereits so groß, dass Lifecycle-Fehler praktisch vorprogrammiert sind.

Besonders problematisch ist die lange Liste von Feldern und das manuelle Shutdown-Reverse-Engineering.

## SOLL

```java
public final class LifecycleCoordinator {
    private final List<AutoCloseable> resources;

    public void shutdown() {
        for (AutoCloseable resource : resources.reversed()) {
            try {
                resource.close();
            } catch (Exception exception) {
                // log
            }
        }
    }
}
```

Noch besser: Jede Infrastrukturkomponente besitzt eine eindeutige `start()`/`close()`-Semantik und wird genau einmal registriert.

Keine zufällige Mischung aus:

- `stop()`
- `shutdown()`
- `shutdownNow()`
- implizitem Bukkit Lifecycle
- statischen Singletons.

---

# 3. Listener-Architektur — nicht strikt logikfrei

Die Vorgabe „Listener sind logikfrei“ wird aktuell nicht eingehalten.

`RegionListener` entscheidet beispielsweise direkt:

- ob PvP erlaubt ist,
- ob Monster spawnen dürfen,
- ob Explosionen erlaubt sind,
- ob Creeper/Ghast Sonderregeln auslösen,
- wie Region-Transitions angezeigt werden.

Das ist Business Logic im Listener.

## SOLL

```text
PlayerMoveEvent
      ↓
RegionEventAdapter
      ↓
RegionTransitionService
      ↓
RegionPolicy
      ↓
RegionTransitionResult
      ↓
Paper UI Adapter
```

Der Listener darf höchstens:

1. Event entgegennehmen.
2. relevante primitive/domain Daten extrahieren.
3. Use Case aufrufen.
4. Ergebnis anwenden.

Dasselbe gilt für Combat, Quest, Boss, NPC und Inventory.

---

# 4. PlayerProfileManager — CRITICAL CONCURRENCY BUG

Der wichtigste technische Befund liegt hier.

`activeProfiles` enthält mutable `PlayerProfile`-Objekte. Änderungen passieren auf dem Server/Main Thread. `persistAsync(profile)` übergibt **dieselbe mutable Instanz** an den I/O-Thread.

Damit kann folgendes passieren:

```text
MAIN THREAD
profile.addExperience(100)
       ↓
persistAsync(profile)
       ↓
I/O THREAD beginnt profile zu serialisieren
       ↑
MAIN THREAD verändert profile erneut
```

Der Repository-Thread sieht dadurch möglicherweise einen Zwischenzustand.

Das ist nicht durch `ConcurrentHashMap` gelöst. Die Map ist thread-safe; das darin liegende mutable Objekt ist es nicht.

## Konsequenz

Mögliche Fehler:

- verlorene Updates,
- widersprüchliche Felder in einem Save,
- Quest-/Stat-/Money-State aus unterschiedlichen Zeitpunkten,
- nicht reproduzierbare Datenbankzustände,
- schwierige Race Conditions unter Last.

## SOLL

Nie das mutable Live-Profil an den I/O-Thread übergeben.

Stattdessen:

```text
Main Thread
    ↓
immutable ProfileSnapshot
    ↓
I/O Queue
    ↓
Repository.save(snapshot)
```

Oder Actor-Modell:

```text
UUID → PlayerActor
        ↓
serialisierte Mutationen
        ↓
periodischer Snapshot
```

Für 100+ Spieler ist ein Snapshot-Modell vorzuziehen.

---

# 5. Save-Queue

Die aktuelle UUID-basierte Save-Kette ist grundsätzlich ein guter Versuch, aber sie ist unnötig komplex und nicht vollständig atomar.

`saveRequested` verhindert mehrere parallele Saves, während `saveChain` Reihenfolge erzwingt. Trotzdem bleibt das zentrale Problem bestehen: Der gespeicherte Objektzustand ist nicht immutable.

## SOLL

```text
ProfileState
    ↓ mutate on main thread
SnapshotFactory
    ↓
ProfileSnapshot
    ↓
per-player serial executor / mailbox
    ↓
Repository
```

Jeder Save muss eine definierte Version besitzen:

```text
revision = 1
revision = 2
revision = 3
```

Ein späterer Save darf niemals durch einen älteren Snapshot überschrieben werden.

---

# 6. Datenbank-I/O

Positiv: `PlayerProfileManager` führt Storage-Operationen über einen separaten Executor aus. Das ist richtig.

Positiv: `DatabaseManager.createTables()` nutzt Try-with-Resources:

```text
Connection
Statement
ResultSet
```

Das muss überall gelten.

## Zwangsregel

```java
try (Connection connection = dataSource.getConnection();
     PreparedStatement statement = connection.prepareStatement(sql)) {
    // I/O
}
```

Kein:

```java
Connection connection = dataSource.getConnection();
```

ohne garantiertes `close()`.

## Zusätzlich

Alle SQL-Statements müssen `PreparedStatement` verwenden.

Keine String-Konkatenation für Benutzer-/Spielerdaten.

---

# 7. DatabaseManager — Sicherheits- und Runtime-Befunde

Die Hikari-Konfiguration ist brauchbar:

- Maximum Pool Size
- Connection Timeout
- Idle Timeout
- Max Lifetime
- Keepalive
- Validation Timeout
- Leak Detection

Das ist positiv.

Aber:

```text
jdbc:mysql://host:port/database?useSSL=false
```

ist für Produktion falsch.

TLS darf nicht pauschal deaktiviert werden.

## SOLL

```text
sslMode=VERIFY_IDENTITY
```

oder eine bewusst konfigurierte TLS-Policy.

Außerdem sollte die JDBC-URL nicht manuell aus unvalidierten Fragmenten zusammengebaut werden, wenn die Konfiguration später aus Admin-/Remote-Quellen stammen kann.

---

# 8. ShadowJar — CRITICAL RUNTIME REVIEW

Aktuell:

```text
relocate com.google.gson → de.pixelrpg.rpg.libs.gson
relocate com.zaxxer.hikari → de.pixelrpg.rpg.libs.hikari
relocate com.mysql → de.pixelrpg.rpg.libs.mysql
```

## Hikari

Hikari selbst zu relocaten ist grundsätzlich sinnvoll, wenn Konflikte mit anderen Plugins verhindert werden sollen.

## MySQL

MySQL ist wesentlich gefährlicher.

Der Code setzt explizit:

```text
com.mysql.cj.jdbc.Driver
```

gleichzeitig wird `com.mysql` relocatet.

Das ist eine klassische Shading-Gefahrenstelle.

Zusätzlich muss bei JDBC-Treibern die `META-INF/services/java.sql.Driver`-Registrierung berücksichtigt werden.

### SOLL

Entweder:

1. MySQL nicht relocaten und sauber als eigene Runtime-Library isolieren,

oder:

2. vollständiges Shading mit Service-File-Merging und verifizierter Runtime-Class-Resolution.

Ein Build ist nicht ausreichend. Es muss getestet werden:

```text
fat jar
 → Class.forName
 → DriverManager
 → HikariDataSource
 → SELECT 1
```

## Gson

Gson intern zu relocaten ist okay, sofern Gson niemals Teil einer externen API wird.

Das muss garantiert werden.

Kein API-Typ darf sein:

```text
com.google.gson.JsonObject
```

oder ein anderes Gson-Typdetail.

---

# 9. Dependency Pinning

Aktuell:

```text
paperDevBundle("26.2.build.+")
```

Das ist für lokale Entwicklung bequem, aber für reproduzierbare Releases schlecht.

`+` bedeutet: derselbe Commit kann morgen einen anderen Dev-Bundle-Stand bauen.

## SOLL

Production Build muss einen exakten Build verwenden.

Beispielprinzip:

```text
26.2.build.<EXACT_BUILD>
```

CI und Release müssen reproduzierbar sein.

Java 25 ist korrekt als Toolchain gesetzt. CI verwendet ebenfalls Java 25. Das ist konsistent.

---

# 10. Gradle / CI

Die CI setzt:

```text
Temurin 25
Gradle 9.2.0
```

und führt `gradle build --no-daemon --stacktrace` aus.

Das ist sauber.

Es fehlt jedoch eine harte Runtime-Validation des erzeugten JARs.

## SOLL-CI

Zusätzlich:

```text
./gradlew clean build
jar tf Pixel-RPG.jar
```

und automatisierte Checks für:

- duplicate classes,
- unrelocated MySQL/Hikari/Gson packages,
- META-INF/services,
- paper-plugin.yml,
- Java 25 bytecode,
- Forbidden API patterns.

---

# 11. Item-System — überwiegend richtig

Das Item-System verwendet `PersistentDataContainer`.

Das ist korrekt.

Beispielhaft werden gespeichert:

```text
identified
itemId
instanceId
rarity
itemLevel
requiredLevel
category
guildItem
weaponAbility
weaponAbilityCooldownMillis
```

Das ist der richtige Weg.

## Zwang

Keine NBT-/NMS-Hacks für normale Item-Identität.

PDC bleibt die primäre Plugin-Metadatenebene.

NBT/NMS nur dort, wo die Paper API ausdrücklich keine passende Abstraktion besitzt.

---

# 12. Item-Generator — globale mutable Configuration

`RPGItemBuilder` besitzt statische Skalierungswerte:

```text
growthMultiplier
weaponBaseDamage
...
```

Das ist funktional, aber architektonisch schlecht.

Ein Builder darf keine globale Application-State-Registry sein.

## SOLL

```text
ItemScalingConfig
        ↓
ItemGenerator
        ↓
ItemStack
```

`ItemScalingConfig` ist immutable.

Damit werden Tests deterministisch und Reload-/Multi-Context-Probleme verschwinden.

---

# 13. Quest-Namenssystem — konkrete Schwachstelle

Die Branch besitzt inzwischen einen `ItemDisplayNameResolver`, was richtig ist.

Aber `QuestText` besitzt weiterhin eine große statische Map mit Vanilla-Namen und versucht erst danach den zentralen Resolver.

Damit existieren mehrere Namensquellen:

```text
QuestText.GERMAN_ITEM_NAMES
CraftingRecipeRegistry
ItemDefinitionRegistry
Material.pretty name
Quest target key
```

Das ist genau die Architektur, die zu sichtbaren Roh-IDs wie

```text
scholar:book
```

führt, wenn ein anderer Pfad die zentrale Auflösung umgeht.

## SOLL

Nur ein Resolver:

```text
DisplayNameService.resolveItem(itemId)
```

Priorität:

```text
PixelRPG Item Definition
        ↓
Recipe Result Definition
        ↓
Vanilla Material Registry
        ↓
Unknown Item
```

`QuestText` darf niemals selbst Namen kennen.

Es darf nur:

```java
itemNameService.resolve(quest.targetKey())
```

aufrufen.

---

# 14. Rezept-ID-Normalisierung

`CraftingRecipeRegistry` versucht IDs zu kanonisieren.

Das ist gut, aber die Daten selbst verwenden sowohl:

```text
pixelrpg:scholar:book
```

als auch:

```text
pixelrpg:scholar/inked_book
```

Das ist unnötig inkonsistent.

## SOLL

Ein einziges ID-Format:

```text
pixelrpg:scholar/book
```

oder vollständig Namespaced-Key-konform.

Nicht drei verschiedene Semantiken für:

```text
scholar:book
pixelrpg:scholar:book
pixelrpg:scholar/book
```

Canonicalization darf Fehler tolerieren, aber Content muss bereits kanonisch sein.

---

# 15. Region-System — gute Basis, falsche Hotpaths

Der Chunk-Index ist eine gute Entscheidung:

```text
world + chunkX + chunkZ → region IDs
```

Dadurch muss `find()` nicht jede Region prüfen.

Das ist bei 100+ Spielern grundsätzlich skalierbar.

## Aber

`isExplicitSpawnPoint()` ignoriert diesen Index und macht:

```text
alle Regionen
 → alle Spawnpunkte
 → Vergleich
```

Das ist O(R × S).

Wenn ein CreatureSpawnEvent sehr häufig feuert, ist das ein schlechter Hotpath.

## SOLL

Spawnpunkte müssen ebenfalls räumlich indiziert werden:

```text
ChunkKey → SpawnPointIndex
```

Dann:

```text
CreatureSpawnEvent
 → chunk lookup
 → mob type lookup
 → exact position
```

O(1) bzw. O(k) für wenige Kandidaten.

---

# 16. Region Persistence — Main Thread I/O

`RegionManager.create()` ruft direkt:

```text
save()
```

und `RegionRepository.save()` führt synchron `YamlConfiguration.save(file)` aus.

Das ist Main-Thread-I/O.

Bei einer kleinen Datei merkt man das nicht. Bei hunderten Regionen, häufigen Editor-Aktionen oder langsamer Disk ist das ein TPS-Risiko.

## SOLL

```text
Main Thread
 → immutable RegionSnapshot
 → async YAML serialization/write
```

Die World-/Bukkit-Objekte dürfen nicht vom I/O-Thread angefasst werden.

---

# 17. Region Listener — weitere Performance-Befunde

`PlayerMoveEvent` wird zwar auf Blockwechsel reduziert, aber jede relevante Bewegung kann trotzdem `regions.find()` auslösen.

Das ist okay, weil der Chunk-Index klein ist.

Die Transition-Map ist korrekt UUID-basiert und wird bei Quit bereinigt.

Das ist ein positiver Punkt.

Der Listener sollte dennoch nur einen Use Case aufrufen.

---

# 18. NPC-System — Entity Lifecycle

`NpcManager` verwendet UUID-Mappings:

```text
npcId → entity UUID
entity UUID → npcId
```

Das ist vernünftig.

NPCs werden bei Chunk Load/Unload synchronisiert.

Positiv: Mannequins werden mit PDC identifiziert.

Positiv: `setPersistent(false)` verhindert dauerhaftes Entity-Persistieren als Weltzustand.

## Problem

`NpcManager.saveAll()` ist synchrones YAML-I/O.

Auch NPC-Erstellung, Umbenennung und Skin-Update schreiben sofort.

Das muss bei Admin-Aktionen asynchron werden.

---

# 19. Mannequin Skin System — konkreter Befund

Die Branch kann **keine beliebige Bild-URL direkt als Minecraft-Skin verwenden**.

Das ist kein einfacher Bug, sondern eine Protokollgrenze.

Minecraft erwartet Profile-/Texture-Daten, nicht:

```text
https://example.com/my-image.png
```

Die Implementierung löst das über MineSkin.

Für beliebige URLs ist aktuell ein API-Key zwingend:

```text
npc.skin.mineskin.api-key
```

oder:

```text
PIXELRPG_MINESKIN_API_KEY
```

Ohne Key wird der Request abgelehnt.

Das erklärt unmittelbar das Verhalten „egal welche Website“.

## Weiteres Problem

`ExternalSkinService.CACHE` ist:

```text
static ConcurrentHashMap<String, ProfileProperty>
```

ohne Größenlimit und ohne TTL.

Bei vielen unterschiedlichen Skin-URLs entsteht ein dauerhafter Memory-Cache.

## SOLL

```text
Caffeine / bounded cache
max entries
TTL
negative caching
```

oder ein eigener LRU/TTL-Cache.

---

# 20. Externe HTTP-Sicherheit

Die URL wird auf HTTP/HTTPS und Host validiert, aber beliebige externe Hosts sind weiterhin erlaubt.

Das ist bei einem Minecraft-Plugin eine SSRF-relevante Boundary.

Ein Spieler/Admin könnte URLs auf interne Ressourcen zeigen lassen, wenn der Skin-Endpunkt von einem nicht vertrauenswürdigen Nutzer ausgelöst werden kann.

## SOLL

Mindestens:

- nur HTTPS,
- DNS/IP-Rebinding-Schutz,
- private/reserved IP ranges blockieren,
- Redirects kontrollieren,
- maximale Response-Größe,
- MIME-Type prüfen,
- harte Request-Limits,
- Rate Limit pro NPC/Quelle.

Noch besser: ausschließlich vertrauenswürdige Skin-Provider oder serverseitiger Proxy.

---

# 21. Static Player References

In den bereits geprüften Kernklassen wird kein offensichtlicher statischer `Player`-Cache gefunden.

Das ist gut.

`PlayerProfileManager` verwendet UUIDs und `Bukkit.getPlayer(uuid)` nur punktuell.

Das ist die richtige Richtung.

Zwang für das gesamte Projekt:

```text
niemals static Player
niemals static Entity
niemals static World
niemals static Location mit langlebiger Entity-Referenz
```

UUID ist der langlebige Schlüssel.

---

# 22. Event Cleanup

`RegionListener` räumt seine UUID-Transition-Map bei Quit auf.

`PlayerProfileManager` entfernt aktive Profile bei Quit.

Das ist gut.

Aber jedes System mit:

```text
Map<UUID, ...>
Set<UUID>
Map<UUID, CompletableFuture<...>>
```

muss einen expliziten Lifecycle-Test besitzen:

```text
join
quit
join
quit
100x
```

und danach:

```text
size == 0
```

für alle transienten Strukturen.

---

# 23. Threading-Regeln — verbindlich

## Main Thread darf

- Bukkit Entity API
- World API
- Inventory API
- ItemStack API
- Dialog/UI
- Event Dispatch
- PDC auf aktiven Bukkit-Objekten

## Async darf

- SQL
- Datei-I/O
- HTTP
- JSON Serialization
- reine mathematische Berechnungen
- immutable Domain Snapshots

## Async darf NICHT

- Bukkit Entity manipulieren
- World laden
- Spieler teleportieren
- Inventory verändern
- `Bukkit.getOnlinePlayers()` für Gameplay-Manipulation verwenden
- Bukkit Events aus beliebigen Threads auslösen, sofern die API keine Thread-Sicherheit garantiert

---

# 24. Boss-System — Architektur-Risiko

Der BossManager ist mit vielen Dependencies verdrahtet und wird von mehreren Listenern angesprochen.

Das System benötigt eine klare Actor-/State-Machine-Struktur:

```text
BossDefinition
      ↓
BossInstance
      ↓
BossState
      ↓
BossTickDecision
      ↓
PaperAdapter
```

Keine Listener dürfen Boss-State direkt manipulieren.

Boss-Phasen sollten deterministisch und tick-budgetiert sein.

---

# 25. Tick-Budget

Für 100+ Spieler muss jedes periodische System ein Budget besitzen.

Aktuell existieren mehrere periodische Tasks, u.a.:

- NPC Look
- Mob Scaling
- Quest Passive Checks
- Boss Bars
- Boss Phase Checks
- Boss Spawning
- Scoreboard
- Playtime Autosave
- Quest Timer

Das ist einzeln okay. Gemeinsam entsteht Tick-Fragmentierung.

## SOLL

Nicht:

```text
20 einzelne Scheduler-Loops
```

sondern möglichst:

```text
1 Scheduler Tick Bus
       ↓
System buckets
```

Beispiel:

```text
1 tick: combat
5 ticks: NPC visibility/look
20 ticks: UI
100 ticks: persistence scheduling
1200 ticks: maintenance
```

Jedes System bekommt eine maximale Laufzeit.

---

# 26. Scoreboard

Ein 20-Tick-Update für jeden Spieler ist grundsätzlich machbar.

Aber es muss differenziert werden:

```text
nur bei dirty state
```

Kein blindes Rebuild jedes Intervalls.

Bei 100 Spielern sind 5 Updates/s × 100 Spieler = 500 Updates/s.

Das ist unnötig, wenn sich nichts verändert.

---

# 27. NPC Look

Ein 5-Tick-Intervall entspricht 4 Updates/s.

Bei 100+ Spielern/NPCs kann das schnell teuer werden.

Die Lösung ist nicht einfach ein längeres Intervall.

Es braucht:

```text
nearby-player index
→ distance check
→ target selection
→ only if target changed
```

Keine vollständige NPC × Player-Matrix.

---

# 28. Memory Management

Besonders zu kontrollieren:

```text
ExternalSkinService.CACHE
PlayerProfileManager.activeProfiles
PlayerProfileManager.loadingCache
PlayerProfileManager.saveChain
PlayerProfileManager.saveRequested
NpcManager.npcsById
NpcManager.spawnedEntityByNpcId
NpcManager.entityToId
RegionManager.regions
RegionManager.globalRegions
RegionManager.index
```

Die meisten sind logisch begrenzt.

Der Skin-Cache ist derzeit nicht begrenzt.

---

# 29. YAML als Runtime Storage

YAML ist als Content-/Config-Format gut.

YAML ist als hochfrequent mutierender Player-State-Store schlecht.

Für 100+ Spieler sollte Player-State entweder:

```text
MySQL
```

oder ein sauberer lokaler DB-Store sein.

YAML sollte primär für:

- Config
- Content
- Region Definitions
- NPC Definitions
- statische Admin-Daten

genutzt werden.

---

# 30. Datenmodell — Geld

`DOUBLE` für Geld ist fachlich falsch.

```text
money DOUBLE
```

kann Rundungsfehler erzeugen.

## SOLL

```text
BIGINT cents
```

oder kleinste Währungseinheit.

Beispiel:

```text
1250 = 12.50
```

Keine Floating-Point-Währung.

---

# 31. SQL Schema

UUID als `CHAR(36)` funktioniert, ist aber nicht optimal.

Bei hoher Spielerzahl wäre binäre UUID-Speicherung kompakter.

Wichtiger als die Mikrooptimierung ist allerdings die Indexierung.

Alle Queries müssen geprüft werden auf:

- UUID index
- quest index
- stat key index
- equipment lookup
- foreign key consistency

---

# 32. Migration System

Die Region-Migration besitzt eine Versionierung (`CURRENT_FORMAT_VERSION`).

Das ist gut.

Das gleiche Prinzip muss für Player DB Schema gelten.

Keine ad-hoc:

```text
CREATE TABLE IF NOT EXISTS
ALTER TABLE DROP COLUMN
```

ohne Migrationshistorie.

Empfohlen:

```text
schema_version
migration_001
migration_002
migration_003
```

Migrationen müssen:

- transaktional,
- idempotent,
- versioniert,
- testbar
sein.

---

# 33. Security — Permissions

`paper-plugin.yml` verwendet:

```text
rpg.admin → op
rpg.member → true
```

Das ist grundsätzlich sauber.

Aber Admin Commands müssen zusätzlich im Command-Code selbst sauber validieren.

Ein Permission Node in YAML ist kein Ersatz für:

- Input Validation
- Ownership Validation
- Region Ownership
- Guild Ownership
- NPC Ownership
- Item Ownership

---

# 34. Security — Content Injection

Quest-, NPC-, Region- und Item-Daten kommen teilweise aus JSON/YAML.

Keine Content-Datei darf ungeprüft:

- SQL
- Commands
- JavaScript
- HTTP URLs
- arbitrary class names
- Reflection Targets

steuern.

Bei externen URLs insbesondere:

```text
https
host validation
size limits
rate limits
```

---

# 35. Adventure / Chat

Die geprüften aktuellen Kernklassen verwenden Adventure Components.

Das ist korrekt.

Kein `ChatColor`.

Keine `§`-Strings.

MiniMessage nur dort einsetzen, wo Content tatsächlich Rich Formatting benötigt.

Für Content-Dateien ist ein zentraler Text-/Component-Service sinnvoll.

---

# 36. Native Dialogs

Der Bootstrapper verwendet die aktuelle Paper Dialog Registry API und registriert einen nativen Dialog.

Das entspricht der Zielarchitektur.

Kein alter Inventory-/Pseudo-Dialog-Ersatz für das native Dialogsystem ist hier erforderlich.

---

# 37. Paper Bootstrap

`paper-plugin.yml` enthält:

```text
main: de.pixelrpg.rpg.PixelRPGPlugin
bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap
api-version: 26.2
```

Das ist konsistent mit dem Projektziel.

Die Registry-/Dialog-Arbeit gehört in den Bootstrapper; runtime services gehören in den Plugin Lifecycle.

Diese Trennung ist richtig und sollte beibehalten werden.

---

# 38. Keine Legacy-NMS-Abhängigkeit in geprüften Kernpfaden

Das Item-System nutzt PDC.

Der Dialog-Bootstrapper nutzt aktuelle Paper APIs.

Die Mannequin-Skin-Implementierung nutzt aktuelle Paper Data Components (`ResolvableProfile`).

Das entspricht der Vorgabe Mojang-/Paper-API statt CraftBukkit-Legacy.

---

# 39. Produktionsreife — harte Gate-Kriterien

Branch `test` darf erst als production-ready gelten, wenn alle folgenden Punkte erfüllt sind:

```text
[ ] PlayerProfile Snapshot-System
[ ] Keine mutable PlayerProfile-Referenz auf I/O-Threads
[ ] Alle DB-Zugriffe async
[ ] Alle File-I/O im Runtime-Path async
[ ] Try-with-Resources für jeden JDBC Resource Scope
[ ] Money als Integer-Minor-Units
[ ] MySQL TLS aktiviert/konfiguriert
[ ] ShadowJar JDBC ServiceLoader verifiziert
[ ] ShadowJar Runtime Class Loading verifiziert
[ ] paperDevBundle exakt gepinnt
[ ] Skin Cache bounded + TTL
[ ] SSRF-Schutz für externe Skin URLs
[ ] Region Spawnpoint Index
[ ] Listener nur Adapter
[ ] zentraler LifecycleCoordinator
[ ] keine statischen Bukkit Entity/Player Referenzen
[ ] keine Hardcoded Item Display Names in QuestText
[ ] einheitliches Item-ID-Format
[ ] Content Validation beim Startup
[ ] DB Schema Migration System
[ ] Tick-Budget je periodischem System
[ ] dirty-state UI updates
[ ] Last-/Load-Test mit 100+ simulierten Spielern
```

---

# 40. Empfohlene Refactoring-Reihenfolge

## Phase 1 — Datenintegrität

1. PlayerProfile immutable Snapshot.
2. Revision-based persistence.
3. DB repositories strikt async.
4. Money `BIGINT`.
5. DB migrations.

## Phase 2 — Lifecycle

1. LifecycleCoordinator.
2. Alle Tasks registrieren.
3. Idempotentes shutdown.
4. ServicesManager sauber unregisteren.
5. Executor shutdown zentralisieren.

## Phase 3 — Performance

1. Region spawn index.
2. NPC nearby index.
3. Dirty scoreboard.
4. Unified tick scheduling.
5. Boss state machine.

## Phase 4 — Content Integrity

1. Single DisplayNameService.
2. Canonical Item IDs.
3. Startup schema validation.
4. Quest target validation.
5. Recipe dependency validation.

## Phase 5 — Security

1. JDBC TLS.
2. ShadowJar service verification.
3. Skin SSRF hardening.
4. Skin cache limits.
5. Admin ownership validation.
6. Rate limiting.

---

# 41. Architektur, die PixelRPG am Ende besitzen sollte

```text
                         ┌──────────────────────┐
                         │      Paper 26.x      │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │ Paper Adapters       │
                         │ Events / Commands    │
                         │ GUI / Dialog / Entity│
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │ Application Services  │
                         │ Use Cases             │
                         └──────────┬───────────┘
                                    │
             ┌──────────────────────▼──────────────────────┐
             │                Domain Core                  │
             │ Player / Quest / Item / Combat / Region   │
             │ NPC / Boss / Profession / Economy         │
             └──────────────────────┬──────────────────────┘
                                    │
                         ┌──────────▼───────────┐
                         │ Repository Interfaces │
                         └──────────┬───────────┘
                                    │
               ┌────────────────────┴────────────────────┐
               │                                         │
       ┌───────▼────────┐                       ┌────────▼───────┐
       │ MySQL Adapter  │                       │ File Adapter   │
       │ Hikari / JDBC  │                       │ YAML / JSON    │
       └────────────────┘                       └────────────────┘
```

---

# 42. Schlussurteil

Die Branch ist **architektonisch weit genug entwickelt, dass jetzt keine weitere Feature-Orgie sinnvoll ist**.

Der nächste Schritt muss Infrastruktur- und Datenintegrität sein.

Die größte Gefahr ist nicht ein einzelner offensichtlicher Crash. Die größte Gefahr ist, dass das Plugin bei 10 Spielern hervorragend aussieht und bei 100+ Spielern anfängt, durch kumulierte kleine Fehler zu versagen:

```text
mutable profile state
+ synchronous YAML
+ unbounded cache
+ linear spawn checks
+ many independent schedulers
+ God Object lifecycle
+ duplicated content-name resolution
+ fragile shading boundary
= schwer reproduzierbares Produktionsverhalten
```

Die gute Nachricht: Die Branch muss nicht weggeworfen werden.

Die Domain-Struktur ist brauchbar. PDC ist richtig. Paper Dialogs sind richtig. Hikari ist richtig. Der Region-Chunk-Index ist richtig. UUID-basierte Runtime-Referenzen sind richtig. Async Profile-I/O ist die richtige Richtung.

Aber diese Grundlagen müssen jetzt **konsequent zu einer echten Architektur zusammengezogen werden**.

## Final Rating

```text
Architecture:        6/10
Code organization:   7/10
Paper API usage:     8/10
Persistence design:  4/10
Concurrency safety:  4/10
Runtime performance: 5/10
Security:            5/10
Content integrity:   5/10
Lifecycle handling:  5/10
Production readiness: 4/10
```

**Endurteil: TEST/DEVELOPMENT READY — NOT PRODUCTION READY.**

Die Branch besitzt genug Substanz für einen ernsthaften Refactor. Sie besitzt noch nicht genug technische Härte für einen dauerhaft laufenden 100+-Spieler-MMORPG-Server.