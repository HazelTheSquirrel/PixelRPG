# PixelRPG – Forensische Plugin-Prüfung

**Prüfgegenstand:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `test`  
**Prüfstand:** Commit `b02747a21cde6d1308c59a7f027cda80877cfe50`  
**Referenzbranch:** `main`  
**Vergleich main ↔ test:** identisch, 0 Commits ahead/behind  
**Prüfdatum:** 2026-09-24  
**Zielplattform:** Java 25 / Paper 26.2 / Mojang-Mappings  
**Prüfart:** statische Forensik, Architekturprüfung, Build-/CI-Prüfung, API-Prüfung, Persistenzprüfung, Sicherheitsprüfung und gezielte Codepfadprüfung

---

## 1. Executive Summary

PixelRPG ist am geprüften Stand ein bereits umfangreiches Paper-26.2-MMORPG-Plugin mit **247 Java-Quelldateien**, mehreren persistenten Subsystemen, MySQL/YAML-Spielerprofilen, NPC-Mannequins, Dialogsystem, Quests, Regionen, Berufen, Crafting, Bossen, Combat, Companion-System, Guild-System, Shop/Trade-System und Resourcepack.

Der geprüfte Commit ist **buildbar**. Der aktuellste GitHub-Actions-Lauf für exakt diesen Commit war erfolgreich:

- Java 25: erfolgreich
- Gradle 9.2.0: erfolgreich
- `clean build`: erfolgreich
- Source API Boundary Verification: erfolgreich
- ShadowJar/Artifact Verification: erfolgreich

Der Build ist damit **technisch grün**, aber ein grüner Build ist nicht gleichbedeutend mit Produktionsreife.

### Gesamtbefund

**Der Code ist in vielen Kernbereichen bereits solide, aber noch nicht als vollständig forensisch abgesicherter Release-Stand einzustufen.**

Die wichtigsten offenen Punkte sind:

1. **Kein echter Runtime-/Server-Integrationstest im CI.**
   Der Build beweist Kompilierbarkeit und JAR-Struktur, nicht korrektes Verhalten auf einem gestarteten Paper-26.2-Server.

2. **MySQL-Fallback kann eine bereits geöffnete Hikari-Verbindungspool-Ressource verlieren.**
   In `PlayerProfileManager.initialize()` wird bei einem Fehler nach `DatabaseManager.connect()` auf YAML zurückgefallen, ohne den bereits erzeugten MySQL-Pool sicher zu schließen.

3. **Die externe Skin-Auflösung ist sicherheitsbewusst implementiert, aber die SSRF-Abwehr ist nicht vollständig race-safe.**
   DNS wird vor dem HTTP-Aufruf geprüft; danach erfolgt der tatsächliche Netzwerkzugriff über den Hostnamen erneut. Damit besteht grundsätzlich ein DNS-Rebinding-/TOCTOU-Fenster.

4. **Die Befehlsarchitektur enthält weiterhin Bukkit-Kompatibilitätsadapter.**
   Die eigentliche Registrierung erfolgt über Paper `BasicCommand`, jedoch arbeiten `RootCommand` und `PaperBasicCommandAdapter` intern noch mit `CommandExecutor`, `TabCompleter` und `Command`. Das ist funktional, aber nicht die sauberste Paper-26.x-Architektur.

5. **Es gibt keinen reproduzierbaren Gradle-Wrapper im Repository.**
   CI pinnt Gradle 9.2.0, lokale Builds hängen dagegen von der installierten Gradle-Version ab.

6. **Der aktuelle Skin-Persistenzpfad ist wesentlich robuster als die vorherigen Implementierungen.**
   Die exakt aufgelöste Texture-Property wird erfasst und in `npcs.yml` gespeichert, statt beim Shutdown aus einem möglicherweise veränderten Mannequin-Zustand rekonstruiert zu werden. Das ist architektonisch richtig.

7. **Die aktuelle Paper-26.2-Dialogmechanik wird grundsätzlich korrekt über Bootstrap/Lifecycle/Registry verwendet.**
   Das entspricht der aktuellen Paper-Architektur.

**Forensische Einschätzung:**  
Das Plugin ist **kein unfertiger Prototyp**. Es ist ein großes, funktionsreiches System mit funktionierender Build- und Lifecycle-Basis. Gleichzeitig fehlen noch einige Maßnahmen, die notwendig wären, um es als vollständig abgesicherten Produktions-/Release-Stand zu klassifizieren.

---

# 2. Prüfmethodik

Geprüft wurden insbesondere:

- Git-Branch-/Commit-Zustand
- Vergleich `main` ↔ `test`
- Gradle-Konfiguration
- Java-Version
- Paper-Version
- paperweight
- ShadowJar
- Dependency-Relocations
- Plugin-Metadaten
- Bootstrapper
- Lifecycle
- Eventregistrierung
- Commands
- Dialoge
- Persistenz
- MySQL
- YAML
- Async-/Sync-Grenzen
- Thread-Sicherheit
- NPC-Lifecycle
- Skin-Auflösung
- externe HTTP-Kommunikation
- SSRF-Schutz
- Datei-Persistenz
- Shutdown-Verhalten
- Resourcepack
- Content-Daten
- CI/CD
- statische API-Grenzprüfungen
- Release-/Testabdeckung
- technische Wartbarkeit
- mögliche Fehler-/Recovery-Pfade

Die Prüfung unterscheidet bewusst zwischen:

- **nachweislich korrekt**
- **statisch plausibel**
- **nicht ausreichend verifiziert**
- **konkreter Befund / Defekt**

Es wurden keine Runtime-Ergebnisse erfunden.

---

# 3. Repository- und Branch-Forensik

## 3.1 Branch-Zustand

`test` und `main` zeigen auf denselben Commit:

```text
b02747a21cde6d1308c59a7f027cda80877cfe50
```

Der Vergleich ergibt:

```text
main -> test
ahead:  0
behind: 0
status: identical
```

Damit existiert zum Prüfzeitpunkt **kein Unterschied zwischen Referenz- und Entwicklungsbranch**.

### Bedeutung

Das ist wichtig für die weitere Entwicklung:

- `main` ist aktuell kein separater älterer Sollstand.
- `test` enthält aktuell exakt denselben Code.
- Eine neue Änderung auf `test` würde damit gleichzeitig eine Abweichung vom aktuellen Referenzstand erzeugen.

Die Branch-Regel bleibt trotzdem sinnvoll: Änderungen weiterhin ausschließlich auf `test`.

---

# 4. Projektgröße und Struktur

Der geprüfte Java-Bestand umfasst:

**247 Java-Dateien**

Wesentliche Module:

- API
- Boss
- Combat
- Commands
- Companion
- Config
- Core
- Dialogue
- Economy
- Equipment
- GUI
- Guild
- Item
- NPC
- Party
- Player
- Profession
- Quest
- Region
- Scoreboard
- Shop
- Statistics
- Storage
- Story
- Trade
- Travel

Zusätzlich:

- 15 JSON-Datendateien im Plugin-Content
- 49 Dateien im Resourcepack
- `config.yml`
- `paper-plugin.yml`
- GitHub Actions Workflow
- VS-Code-Buildtasks

Die Architektur ist damit bereits modularisiert und nicht als monolithischer Single-Class-Ansatz aufgebaut.

---

# 5. Gradle- und Build-Forensik

## 5.1 Positiv

`build.gradle` entspricht den vorgegebenen technischen Eckdaten:

- Java Plugin
- paperweight-userdev `2.0.0-beta.21`
- Paper Dev Bundle `26.2.build.121-stable`
- Shadow `9.6.1`
- Java 25 Toolchain
- Java 25 Compiler Release
- UTF-8
- `-Xlint:all`

Dependencies:

- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

Shadow Relocations:

- Gson → `de.pixelrpg.rpg.libs.gson`
- Hikari → `de.pixelrpg.rpg.libs.hikari`
- MySQL → `de.pixelrpg.rpg.libs.mysql`

Artifact:

```text
Pixel-RPG.jar
```

Slim-JAR:

```text
*-slim.jar
```

Das entspricht dem gewünschten Buildmodell.

---

## 5.2 Build-Verifikationen

Der Build enthält eigene Prüfungen für:

- Legacy `ChatColor`
- Legacy NMS-Pakete
- CraftBukkit
- statische Live-Server-Referenzen
- unrelocierte Third-Party-Klassen
- JDBC-Service-Descriptor
- ShadowJar-Struktur
- `paper-plugin.yml`

`check` hängt die eigenen Verifikationen ein.

Das ist ein klarer positiver Architekturpunkt.

---

## 5.3 Schwäche der statischen Prüfungen

Die Verifikation arbeitet überwiegend mit regulären Ausdrücken.

Das bedeutet:

- Sie erkennt definierte Muster.
- Sie beweist nicht die semantische Korrektheit des Programms.
- Sie kann indirekte Live-Server-Referenzen nicht vollständig beweisen.
- Sie ist kein Ersatz für Runtime-Tests.

Beispiel:

Eine Regex kann `static Player player;` erkennen, aber nicht jede denkbare Form einer statischen Objektgraph-Referenz.

**Befund:** kein akuter Defekt, aber begrenzte Prüftiefe.

---

# 6. Gradle-Reproduzierbarkeit

Im Repository existiert kein `gradlew`/Gradle-Wrapper.

CI installiert explizit:

```text
Gradle 9.2.0
```

Das macht CI reproduzierbarer, aber lokale Builds sind von der installierten Gradle-Version abhängig.

### Empfehlung

Einen Gradle Wrapper mit der im CI verwendeten Gradle-Version hinzufügen.

**Priorität:** mittel.

---

# 7. Paper-Plugin-Architektur

`src/main/resources/paper-plugin.yml` ist vorhanden.

Wesentliche Werte:

```yaml
name: PixelRPG
version: 1.0.0
main: de.pixelrpg.rpg.PixelRPGPlugin
bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap
api-version: '26.2'
```

Das entspricht dem gewünschten Paper-Plugin-Modell.

Paper dokumentiert `paper-plugin.yml` als Grundlage für Paper Plugins und die Verwendung eines Bootstrappers. Das geprüfte Plugin folgt diesem Modell.

Quelle:
https://docs.papermc.io/paper/dev/getting-started/paper-plugins/

---

# 8. Bootstrapper und Dialog-System

`PixelRPGBootstrap` verwendet:

- `PluginBootstrap`
- Lifecycle Manager
- `RegistryEvents.DIALOG`
- `DialogKeys`
- `DialogType`
- `DialogBase`
- `DialogAction`

Die Charakterkarte wird über die Dialog-Registry registriert.

Die Quick-Actions-Verknüpfung wird über:

```text
LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG)
```

durchgeführt.

Das ist konzeptionell passend zur aktuellen Paper-Dialogarchitektur.

Paper dokumentiert die Dialog-API und die Registry-Registrierung über den Bootstrap-Lifecycle.

Quelle:
https://docs.papermc.io/paper/dev/dialogs/

### Befund

**Kein grundlegender Architekturfehler erkannt.**

Die Dialogarchitektur muss trotzdem bei jedem Paper-Update gegen die tatsächlich verwendete 26.2-API getestet werden, da Dialoge weiterhin ein relativ neuer API-Bereich sind.

---

# 9. Plugin-Lifecycle

`PixelRPGPlugin` verwendet einen eigenen:

```text
LifecycleCoordinator
```

Dieser registriert `AutoCloseable`-Ressourcen und schließt sie in umgekehrter Registrierungsreihenfolge.

Das ist grundsätzlich sauber.

Besonders positiv:

- Shutdown ist idempotent.
- Ressourcen werden rückwärts geschlossen.
- Fehler beim Schließen einer Ressource stoppen nicht automatisch die übrige Shutdown-Kette.

Das ist für ein Plugin mit vielen Services wichtig.

---

# 10. Shutdown-Reihenfolge

Die Reihenfolge wurde auf Abhängigkeitslogik geprüft.

Insbesondere:

- NPC-Look-Task wird vor NPC-Manager beendet.
- NPC-Manager wird vor späteren, globaleren Ressourcen geschlossen.
- PlayerProfileManager wird sehr spät geschlossen.
- Companion-/Party-/Guild-Systeme werden vor der finalen Profilpersistenz beendet.

Das ist grundsätzlich sinnvoll.

### Restrisiko

Asynchrone externe Vorgänge können auch nach Beginn des Shutdowns noch Completion-Callbacks liefern.

Das betrifft insbesondere Skin-Auflösung.

Daher ist die Shutdown-Garantie bei externen HTTP-Operationen nicht absolut.

---

# 11. Player-Persistenz

Die Spielerpersistenz ist eine der stärkeren Komponenten des Projekts.

Vorhanden:

- YAML Repository
- MySQL Repository
- Async I/O Executor
- Pre-Login Loading
- Loading Cache
- Active Profile Cache
- Save Chains
- Pending Save Coalescing
- Mutation Revision
- Persistence Revision
- Snapshot-basierte Speicherung
- Emergency Backup bei MySQL-Fehlern
- Shutdown Flush

---

# 12. Pre-Login Loading

`PlayerProfileLifecycleListener` lädt Profile in:

```text
AsyncPlayerPreLoginEvent
```

und nutzt für das tatsächliche I/O einen separaten Executor.

Das ist wichtig, weil Datenbank-/Datei-I/O nicht auf dem Server-Main-Thread ausgeführt werden sollte.

Das Laden ist außerdem timeoutbegrenzt.

Default:

```text
8 Sekunden
```

### Befund

Architektonisch solide.

---

# 13. Snapshot- und Revision-System

`PlayerProfile` besitzt:

- `persistenceRevision`
- `mutationRevision`
- `dirty`

Das Snapshot-Verfahren kopiert:

- Grunddaten
- Berufe
- Rezepte
- Waypoints
- Quests
- Statistiken
- Equipment
- Playtime
- UI-Zustände

und klont ItemStacks.

Das ist wichtig, weil der Snapshot anschließend asynchron verarbeitet wird.

### Positiv

Es wird nicht einfach das mutable Live-Objekt auf einen Hintergrundthread gegeben.

Das reduziert Race Conditions erheblich.

---

# 14. MySQL Revision Protection

Die MySQL-Speicherung verwendet:

```text
SELECT persistence_revision ... FOR UPDATE
```

und prüft anschließend, ob der Datenbankstand mit der erwarteten Revision übereinstimmt.

Das verhindert stille Überschreibung eines fremden/älteren Zustands.

Die Speicherung erfolgt in einer Transaktion.

Das ist ein sehr guter Schutz gegen Lost Updates.

---

# 15. Konkreter MySQL-Fallback-Befund

In:

```text
PlayerProfileManager.initialize()
```

wird:

1. `DatabaseManager` erzeugt
2. MySQL-Verbindungspool geöffnet
3. Repository erzeugt
4. `repository.init()` ausgeführt

Wenn danach ein Fehler auftritt, wird auf YAML gewechselt.

Problem:

Der bereits erzeugte `DatabaseManager` bzw. Hikari-Pool wird im Fallback-Pfad nicht explizit geschlossen.

Damit ist folgender Fehlerpfad möglich:

```text
MySQL connect()
        ↓
HikariDataSource geöffnet
        ↓
repository.init() schlägt fehl
        ↓
YAML fallback
        ↓
alte DataSource bleibt möglicherweise offen
```

Das ist ein konkreter Ressourcenleck-Befund.

**Priorität: hoch**

### Korrekturziel

Im Catch-Pfad muss ein bereits initialisierter MySQL-`DatabaseManager` sauber heruntergefahren werden, bevor auf YAML gewechselt wird.

---

# 16. Emergency Backup

Bei MySQL-Speicherfehlern existiert ein YAML-Emergency-Backup.

Das ist grundsätzlich positiv.

Der Backup-Pfad verwendet ein separates Verzeichnis:

```text
plugins/PixelRPG/emergency/
```

### Restrisiko

Die Emergency-Dateien sind ein Recovery-Mechanismus, aber aktuell kein vollständiges Recovery-/Reconciliation-System.

Es gibt keine erkennbare automatische:

- Recovery-Aggregation
- Konfliktauflösung
- Quarantäneverwaltung
- Integritätsprüfung
- Benutzer-/Admin-Benachrichtigung

**Priorität:** mittel.

---

# 17. NPC-Persistenz

Die NPC-Persistenz ist inzwischen deutlich robuster aufgebaut.

Der Manager:

- erzeugt Snapshots auf dem Serverthread
- serialisiert diese off-thread
- verwendet eine Persistence Chain
- schreibt zunächst eine temporäre Datei
- verwendet `ATOMIC_MOVE` wenn möglich
- fällt auf normalen Replace-Move zurück
- behandelt Schreibfehler

Das ist eine gute Datei-Persistenzstrategie.

---

# 18. NPC-Skin-Persistenz

Der aktuelle Skin-Pfad verwendet einen wichtigen Ansatz:

Nicht das mutable Mannequin-Profil wird später erneut ausgelesen.

Stattdessen wird die exakt aufgelöste Texture-Property direkt nach erfolgreicher Auflösung erfasst:

```text
resolved texture value
resolved signature
        ↓
StoredSkin
        ↓
saveAll()
        ↓
npcs.yml
```

Das ist der richtige Datenfluss.

Die Skin-Daten werden beim Speichern als:

- `skin-value`
- `skin-signature`

gespeichert.

Damit kann der Server nach Neustart den exakten Property-Zustand wiederherstellen.

---

# 19. NPC-Skin-Restart-Verhalten

Beim Start wird zuerst geprüft, ob eine gespeicherte Texture vorhanden ist.

Wenn ja:

```text
persisted texture
        ↓
applyStoredTexture()
        ↓
Mannequin.setProfile()
```

Erst wenn kein persistierter Skin vorhanden ist, wird die externe Quelle erneut aufgelöst.

Das ist wesentlich robuster als eine reine Source-Re-Resolution.

### Befund

**Der aktuelle Persistenzansatz ist fachlich plausibel und deutlich besser als ein reines Re-Resolve-System.**

Eine vollständige Runtime-Garantie kann statisch trotzdem nicht gegeben werden, solange kein automatisierter Restart-Test existiert.

---

# 20. Skin-Resolver

Der Resolver verwendet aktuelle Paper-Mannequin-/ResolvableProfile-Mechaniken.

Paper 26.2 stellt `Mannequin#getProfile()`, `Mannequin#setProfile(ResolvableProfile)` und `ResolvableProfile` bereit.

Quelle:
https://jd.papermc.io/paper/26.2/org/bukkit/entity/Mannequin.html

Quelle:
https://jd.papermc.io/paper/26.2/io/papermc/paper/datacomponent/item/ResolvableProfile.html

Die verwendeten `com.destroystokyo.paper.profile.ProfileProperty`-Klassen sind in Paper 26.2 weiterhin vorhanden. Die alten mutierenden `setId()`/`setName()` APIs werden dagegen vom aktuellen Paper als deprecated for removal geführt. Der geprüfte Code verwendet diese beiden mutierenden Methoden nicht.

---

# 21. Skin-Resolver – technischer Wartungspunkt

`MannequinSkinResolver` enthält doppelte Imports:

```java
import com.destroystokyo.paper.profile.PlayerProfile;
```

erscheint zweimal.

Das ist kein Laufzeitfehler, aber ein kleiner Code-Hygiene-Befund.

Außerdem beschreibt ein Kommentar noch eine alte direkte `PlayerTextures#setSkin`-Implementierung, obwohl der aktuelle externe URL-Pfad über `ExternalSkinService`/MineSkin arbeitet.

Das kann zu falscher Dokumentation führen.

**Priorität:** niedrig.

---

# 22. Externe Skin-Auflösung

Die externe Skinauflösung ist ungewöhnlich sicherheitsrelevant, weil der Plugin-Code:

- fremde URLs akzeptiert
- HTTP-Verbindungen ausführt
- Redirects verfolgt
- HTML analysiert
- Bild-URLs aus HTML extrahiert
- externe Skin-Generierung an MineSkin sendet

Positiv vorhanden:

- URL-Längenlimit
- HTTP/HTTPS-Beschränkung
- Fragment-Verbot
- Userinfo-Verbot
- Response-Größenlimit
- Timeout
- Redirect-Prüfung
- DNS/IP-Prüfung
- private/loopback/link-local Filter
- Candidate-Limit

Das ist deutlich besser als eine naive URL-Fetch-Implementierung.

---

# 23. SSRF-Befund

Die Funktion `isSafeExternalUri()` löst den Host per DNS auf und prüft die erhaltenen IP-Adressen.

Problematisch ist das Prinzip:

```text
DNS prüfen
   ↓
Zeit vergeht
   ↓
HttpClient löst Host erneut auf
   ↓
Verbindung
```

Das ist ein klassisches DNS-Rebinding-/TOCTOU-Fenster.

Zusätzlich ist die IPv6-Reservierungsprüfung weniger vollständig als eine explizite, vollständige RFC-/Adressraum-Allowlist.

### Einschätzung

Für ein internes Plugin ist der Schutz bereits relativ stark.

Für einen öffentlich betriebenen Server, auf dem Administratoren oder andere vertrauensarme Quellen Skin-URLs setzen können, sollte der Resolver trotzdem als sicherheitskritischer Netzwerkcode behandelt werden.

**Priorität:** hoch, wenn Skin-URLs nicht ausschließlich vertrauenswürdigen Administratoren offenstehen; ansonsten mittel.

---

# 24. Externe API-Abhängigkeit

Der Skin-Resolver nutzt:

```text
https://api.mineskin.org/v2/generate
```

Damit hängt ein Teil der Funktionalität von einem externen Dienst ab.

Positiv:

- Timeout
- Response-Limit
- Fehlerbehandlung
- keine API-Keys im Repository

Fehlend bzw. nicht erkennbar:

- Rate Limiting
- Circuit Breaker
- globales Backoff
- persistenter Resolver-Cache
- zentrale HTTP-Service-Verwaltung

Außerdem erzeugt `MannequinSkinResolver` für externe URLs jeweils eine neue `ExternalSkinService`-Instanz. Dadurch sind deren In-Memory-Caches nicht global über alle Auflösungen hinweg.

**Priorität:** mittel.

---

# 25. HTTP-Datenvolumen

Es gibt ein Response-Limit von 1.500.000 Bytes.

Das ist ein guter Schutz.

Allerdings existiert kein explizites globales Request-/Skin-Rate-Limit.

Ein Admin oder ein automatisierter NPC-Updatepfad könnte damit viele externe Requests erzeugen.

**Priorität:** mittel.

---

# 26. Command-System

Die Commands werden über Paper Lifecycle registriert.

Das ist positiv.

`PaperBasicCommandAdapter` implementiert `BasicCommand`.

Paper 26.2 stellt genau diese API bereit und dokumentiert die Registrierung über `LifecycleEvents.COMMANDS`.

Quelle:
https://docs.papermc.io/paper/dev/command-api/basics/registration/

Quelle:
https://jd.papermc.io/paper/26.2/io/papermc/paper/command/brigadier/BasicCommand.html

---

# 27. Command-Architektur – Wartungspunkt

Die Command-Implementierungen arbeiten intern weiterhin mit:

- `CommandExecutor`
- `TabCompleter`
- `Command`

und werden anschließend durch einen Paper-`BasicCommand`-Adapter verpackt.

Das funktioniert, ist aber eine Kompatibilitäts-/Adapterarchitektur.

Paper stellt bereits `BasicCommand` und die aktuellen Lifecycle-Registrierungsmechanismen bereit.

### Einschätzung

Kein akuter Defekt.

Aber wenn das Plugin langfristig vollständig auf Paper-26.x ausgerichtet werden soll, sollte die Command-Schicht schrittweise direkt auf `BasicCommand` bzw. Brigadier abstrahiert werden.

**Priorität:** mittel.

---

# 28. Permissions

Das Plugin verwendet:

- `rpg.admin`
- `rpg.member`

Subcommands besitzen eigene Permission-Checks.

Das ist grundsätzlich sauber.

Wichtig ist, dass Root- und Subcommand-Checks nicht widersprüchlich werden.

Aktuell ist kein grundlegender Bypass im geprüften zentralen Command-Code sichtbar.

---

# 29. Dialog- und Command-Duplikation

`RootCommand` erzeugt teilweise eigene Adapter/Instanzen, während `PixelRPGPlugin` separat Command-Objekte erzeugt.

Beispielsweise existieren mehrere Wege zum Dialogue-System.

Das erhöht langfristig das Risiko, dass:

- Zustände nicht geteilt werden
- Änderungen nur an einem Pfad vorgenommen werden
- Verhalten zwischen Command-Entrypoints auseinanderläuft

**Priorität:** mittel.

---

# 30. Eventregistrierung

Die zentrale Plugin-Klasse registriert zahlreiche Listener explizit.

Das macht den Startpfad nachvollziehbar.

Positiv:

- keine versteckte automatische Classpath-Discovery
- Services und Listener werden sichtbar aufgebaut
- Shutdown-Ressourcen werden zentral gesammelt

Nachteil:

`PixelRPGPlugin.onEnable()` ist inzwischen sehr lang.

Das erschwert:

- Dependency-Audit
- Startfehleranalyse
- isolierte Tests
- modulare Initialisierung

**Priorität:** mittel.

---

# 31. Startreihenfolge

Die Reihenfolge ist grundsätzlich dependency-orientiert.

Beispiele:

- Profile vor Stats
- Profession vor Item Display Resolver
- Party vor QuestManager
- QuestRepository vor QuestManager
- RegionManager vor RegionEditor/SpawnService
- BossRepository vor BossManager
- NPCManager vor NPC-Interaktionssystem
- CompanionService vor Companion Listener

Das ist positiv.

---

# 32. Event-Listener-Hygiene

Die Listener sind überwiegend über `registerEvents()` an die Plugin-Lifecycle gebunden.

Damit übernimmt Paper/Bukkit die Listener-Deaktivierung beim Plugin-Shutdown.

Zusätzliche periodische Tasks werden explizit gestoppt.

Das ist korrekt.

---

# 33. Async-/Sync-Trennung

Es existiert eine erkennbare Trennung:

Serverthread:

- Entities
- Mannequins
- Bukkit/Paper-Objekte
- UI
- Player-Interaktionen

Background:

- Datei-I/O
- MySQL
- HTTP
- Profilauflösung

Das ist grundsätzlich richtig.

---

# 34. Kritischer Threading-Punkt: Profiländerungs-Callbacks

`PlayerProfile` ruft bei Mutationen einen Dirty-Callback aus.

Dieser Callback kann Listener synchron auf dem Thread ausführen, auf dem die Mutation stattfindet.

Damit hängt die Thread-Sicherheit des Gesamtsystems davon ab, dass alle registrierten `profileChangeListeners` selbst thread-safe bzw. serverthread-sicher sind.

Die zentrale Profilklasse allein kann diese Garantie nicht geben.

### Empfehlung

Profiländerungs-Callbacks sollten entweder:

1. ausschließlich auf dem Serverthread erfolgen, oder
2. über einen expliziten Scheduler-Handoff verfügen.

**Priorität:** hoch, wenn externe Services Profile auch außerhalb des Main-Threads mutieren können.

---

# 35. PlayerProfile-Threading

Die Datenstruktur selbst ist relativ gut abgesichert:

- `synchronized` Getter/Mutationen
- Snapshot unter Lock
- ConcurrentHashMap im Manager
- per-UUID Save Chain

Das ist positiv.

Die größere Gefahr liegt in der Verwendung der Profile außerhalb dieser Klasse.

---

# 36. Datenbank-Transaktionen

MySQL-Speicherung erfolgt transaktional.

Folgende Bereiche werden gemeinsam gespeichert:

- Player
- Quests
- Equipment
- Statistics

Bei Fehler:

```text
rollback()
```

Danach wird der Fehler an den Manager zurückgegeben.

Das verhindert Teil-Commit innerhalb eines einzelnen Save-Vorgangs.

---

# 37. Datenbank-Schema

Es existieren Tabellen für:

- Spieler
- aktive Quests
- Statistiken
- Equipment
- Schema-Version

Das Schema besitzt eine Versionsverwaltung.

Aktuell:

```text
CURRENT_SCHEMA_VERSION = 2
```

Migrationen:

- Legacy-Spalten
- Persistence Revision
- Money → Minor Units

Das ist für ein Plugin dieser Größe angemessen.

---

# 38. Geld-/Economy-Persistenz

Geld wird als:

```money_minor_units BIGINT
```

gespeichert.

Das vermeidet typische Floating-Point-Persistenzprobleme.

Die Migration verwendet eine Umrechnung auf Minor Units.

Das ist fachlich sinnvoll.

---

# 39. YAML-Persistenz

YAML wird weiterhin als alternativer Player-Store genutzt.

Vorteile:

- kein externer Datenbankserver
- einfache Installation
- Emergency-Recovery möglich

Nachteile:

- Dateipersistenz skaliert schlechter
- kein transaktionaler Mehrtabellenstatus
- Recovery ist stärker dateibasiert

Für kleine Installationen okay, für größere Server sollte MySQL die primäre Produktionsoption bleiben.

---

# 40. Config-Sicherheit

`config.yml` enthält:

```yaml
username: "root"
password: "CHANGE_ME"
ssl-mode: "REQUIRED"
```

Positiv:

- kein echtes Passwort im Repository
- SSL-Modus standardmäßig REQUIRED
- Host/Port/DB werden validiert
- Datenbankname wird gegen Identifier-Injection validiert

Das ist gut.

### Empfehlung

Die Dokumentation sollte explizit darauf hinweisen, dass:

- kein Root-DB-Account verwendet werden sollte
- ein dedizierter MySQL-Benutzer mit minimalen Rechten verwendet werden sollte

**Priorität:** mittel.

---

# 41. SQL Injection

Die eigentlichen Player-Abfragen verwenden Prepared Statements.

Das ist positiv.

Dynamische SQL-Identifikatoren werden zumindest beim DB-Namen validiert.

Die Legacy-Migrationsspalten stammen aus einer festen internen Liste.

Im geprüften DB-Code wurde kein offensichtlicher ungebundener User-Input als SQL-Wert gefunden.

---

# 42. Datei-Persistenz und Crash-Sicherheit

NPC-Dateien verwenden:

1. Snapshot
2. temporäre Datei
3. `ATOMIC_MOVE`
4. Fallback auf Replace

Das ist eine gute Crash-Resistance-Strategie.

Für Player-YAML und andere Content-Dateien sollte dieselbe Strategie geprüft bzw. vereinheitlicht werden, falls diese ebenfalls häufig geschrieben werden.

---

# 43. Resourcepack

Das Repository enthält ein integriertes Resourcepack.

Bestand:

- 46 JSON-Dateien
- 1 `pack.mcmeta`
- README-Dokumentation
- Food-Modelle
- Item-Modelle
- Texturenpfade

### Nicht vollständig automatisiert verifiziert

Es existiert im CI keine erkennbare Prüfung auf:

- JSON-Schema
- referenzierte Assets
- fehlende Texturdateien
- fehlende Modellziele
- Resourcepack-Laufzeittest

**Priorität:** mittel.

---

# 44. Content-Daten

Plugin-Daten:

- Boss Rewards
- Companions
- Equipment Sets
- Food
- Items
- Mob Scaling
- Quests
- Recipes
- Unique Items

Der Datenbestand ist umfangreich.

Die Daten werden teilweise runtimeseitig geladen.

### Risiko

Ein erfolgreicher Java-Build beweist nicht automatisch:

- dass jede JSON-Datei semantisch gültig ist
- dass jede referenzierte ID existiert
- dass jede Rezeptkomponente vorhanden ist
- dass alle Questziele auf existierende Inhalte zeigen

**Empfehlung:** Content-Linting als separaten Buildtask ergänzen.

---

# 45. Crafting

Die Crafting-Architektur besitzt eine zentrale Registry und Service-Schicht.

Das ist grundsätzlich korrekt.

Der frühere Fehler `Missing 'category' in crafting recipe` zeigt allerdings, dass Content-Schema-Validierung in der Vergangenheit ein realer Fehlerpunkt war.

Daraus folgt:

**Content sollte vor Runtime-Start vollständig validiert werden.**

Ein Build sollte ungültige Rezeptdaten bereits im CI ablehnen.

**Priorität:** hoch.

---

# 46. Runtime-Fehlerisolierung

Viele Subsysteme behandeln eigene Fehler und loggen diese.

Positiv.

Problematisch ist jedoch, dass ein Build keine Aussage darüber macht, ob:

- ein einzelnes kaputtes Quest-JSON
- ein kaputtes NPC-File
- ein ungültiger Shop
- eine ungültige Region
- ein inkompatibler MySQL-Schema-Stand

den Pluginstart vollständig blockiert oder nur teilweise degradiert.

Diese Recovery-Matrix sollte explizit dokumentiert und getestet werden.

---

# 47. Security – externe Eingaben

Besonders relevante externe Eingaben:

- NPC-Skin-URL
- Player-Skin-Name
- Command-Argumente
- Quest-/Content-IDs
- Item-IDs
- Region-Daten
- Shopdaten
- MySQL-Konfiguration

Viele Eingaben werden validiert.

Die Skin-URL ist dabei der sicherheitskritischste externe Netzwerkpfad.

---

# 48. Logging

Das Plugin loggt insbesondere Skin-Auflösungen sehr ausführlich.

Das hilft der aktuellen Forensik.

Zu beachten:

- Logs dürfen keine vollständigen geheimen Tokens enthalten.
- Skin-Properties sind große Base64-Werte und sollten nicht vollständig geloggt werden.

Der aktuelle Code kürzt Texture-Werte an mehreren Stellen.

Das ist gut.

---

# 49. Logging-Lautstärke

Die Skin-Auflösung verwendet viele `info`-Logs.

Auf einem Server mit vielen NPCs kann das Logvolumen erheblich werden.

Für Production wäre eine Differenzierung sinnvoll:

- INFO: wichtige Lifecycle-Ereignisse
- DEBUG: Skin-Resolution
- WARNING: Fehler
- SEVERE: Datenverlust/Recovery

**Priorität:** niedrig bis mittel.

---

# 50. Build-/CI-Forensik

Der aktuelle GitHub-Actions-Lauf:

```text
Run: 35845271613
Commit: b02747a21cde6d1308c59a7f027cda80877cfe50
Result: success
```

Steps:

- Checkout: OK
- Java 25: OK
- Gradle 9.2.0: OK
- Build: OK
- Source API validation: OK
- Artifact validation: OK

Das ist ein belastbarer positiver Befund.

---

# 51. Was CI NICHT beweist

CI beweist aktuell nicht:

- Plugin startet auf einem echten Paper-26.2-Server
- MySQL-Verbindung funktioniert real
- YAML-Persistenz funktioniert nach Neustart
- NPC-Skin bleibt nach Restart erhalten
- Dialoge öffnen korrekt
- Commands funktionieren ingame
- Resourcepack funktioniert clientseitig
- Companion-System läuft korrekt
- Bosskampf funktioniert
- Questfluss funktioniert
- Region-Transitions funktionieren
- Datenmigration funktioniert gegen echte Alt-Schemas

Das ist die größte Lücke zwischen Build-Qualität und Runtime-Qualität.

---

# 52. Fehlende Integrationstests

Es gibt keine erkennbare Test-Suite, die einen Paper-Server startet und einen grundlegenden Smoke-Test durchführt.

Für dieses Projekt sollte mindestens ein automatisierter Smoke-Test vorhanden sein:

```text
Server starten
↓
Plugin laden
↓
Plugin enable erfolgreich
↓
keine SEVERE/Exception
↓
Command registrierbar
↓
NPC-Daten laden
↓
Content laden
↓
Server sauber stoppen
```

**Priorität: sehr hoch.**

---

# 53. Fehlender Persistenz-Restart-Test

Gerade wegen der aktuellen Skin-Problematik sollte CI mindestens einen deterministischen Restart-Test haben:

```text
1. Server starten
2. NPC mit Player-Skin laden
3. Skin auflösen
4. NPC-Datei prüfen
5. Server stoppen
6. Server neu starten
7. NPC-Datei laden
8. persisted skin-value prüfen
9. Mannequin-Profil prüfen
```

Damit wäre der aktuelle Hauptproblemkomplex tatsächlich automatisiert beweisbar.

---

# 54. Fehlender MySQL-Integrationstest

Die SQL-Implementierung ist relativ umfangreich.

Es fehlt aber ein reproduzierbarer Test gegen einen echten MySQL-Service.

Empfohlen:

- GitHub Actions Service Container
- temporäre Datenbank
- Migrationstest
- Insert/Load/Update
- Revision Conflict
- Rollback
- Shutdown Flush

---

# 55. API-Kompatibilität

Die Build-Konfiguration verwendet die vorgegebene Paper-26.2-Basis.

Die geprüften Dialog- und Mannequin-APIs existieren in Paper 26.2.

Die Paper-26.2-Dokumentation bestätigt:

- Lifecycle API
- Dialog API
- BasicCommand
- Mannequin/ResolvableProfile

Das Projekt ist damit technisch auf der vorgesehenen API-Linie aufgebaut.

---

# 56. Legacy-API-Befund

Die vorhandenen Buildprüfungen finden keine:

- `ChatColor`
- Legacy NMS
- CraftBukkit
- `net.minecraft.server.vX_X_RX`

Der geprüfte Quellcode verwendet Adventure Components.

Das entspricht den Projektvorgaben.

---

# 57. NMS

Im geprüften Quellbaum wurde keine Legacy-NMS-Struktur nach dem Muster:

```text
net.minecraft.server.v1_XX_RY
```

gefunden.

Die aktuelle Architektur nutzt Paper-API/ResolvableProfile/Mannequin statt eines Legacy-NMS-Skin-Hacks.

Das ist positiv.

---

# 58. Statische Live-Server-Referenzen

Die Buildprüfung schützt explizit gegen bestimmte statische Referenzen auf:

- Player
- Entity
- World

Der geprüfte Code enthält keine offensichtliche problematische statische Player-/Entity-/World-Instanz.

Es existiert jedoch:

```private static PixelRPGPlugin instance;
```

Das ist keine Player/Entity/World-Referenz und stellt keinen unmittelbaren Verstoß gegen die bestehende Boundary-Prüfung dar.

Langfristig wäre eine explizite Prüfung aller statischen servergebundenen Objekte stärker als die aktuelle Regex.

---

# 59. API-Service-Registrierung

Services werden über Bukkit/Paper Services registriert.

Beispiele:

- ItemAPI
- PartyAPI
- StatisticsAPI
- GuildAPI
- EconomyAPI

Die Services werden beim Shutdown teilweise explizit wieder entfernt.

Das ist wichtig für Reload-/Lifecycle-Sicherheit.

---

# 60. Reload-Sicherheit

Das Plugin besitzt zahlreiche globale Manager, Caches und externe Ressourcen.

Der Shutdown ist vergleichsweise gut strukturiert.

Ein vollständiger `/reload`-Test wurde jedoch nicht durchgeführt und ist auch nicht durch CI abgesichert.

Da Paper Plugin Lifecycle und Commands reloadfähige Registrierungen besitzen, sollte insbesondere geprüft werden:

- doppelte Listener
- doppelte Commands
- alte Scheduler
- alte HTTP-/Executor-Threads
- alte Services
- alte statische Referenzen

**Priorität:** hoch für Produktionsbetrieb.

---

# 61. Scheduler

Das Plugin verwendet sowohl:

- periodische Tasks
- one-shot Scheduler
- eigene ExecutorServices

`WakeScheduler` coalesct Wakeups pro Key.

Das ist ein gutes Muster gegen Task-Spam.

---

# 62. Task-Shutdown

Mehrere Manager besitzen eigene `stop()`/`shutdown()`-Methoden.

Diese werden in den LifecycleCoordinator aufgenommen.

Das ist positiv.

Es reduziert Zombie-Tasks nach Plugin-Deaktivierung.

---

# 63. Executor-Shutdown

Player Profile:

- Fixed Thread Pool
- Shutdown
- Await
- ShutdownNow-Fallback

NPC Persistence:

- eigener Persistence Executor
- Shutdown
- Await
- ShutdownNow-Fallback

Das ist korrekt aufgebaut.

---

# 64. Async HTTP-Shutdown

Der `HttpClient` in `ExternalSkinService` besitzt keinen expliziten Lifecycle-Shutdown.

Der Java HTTP Client verwaltet seine Ressourcen intern.

Das ist nicht automatisch ein Leak.

Da jedoch pro externer Skin-Auflösung neue `ExternalSkinService`-Instanzen erzeugt werden, ist die zentrale Verwaltung eines einzigen Clients langfristig sauberer.

**Priorität:** mittel.

---

# 65. Memory-/Cache-Management

Caches existieren für:

- aktive Profile
- Loading Profile
- Save Chains
- Pending Saves
- Player Profile Resolution
- NPC resolved skins
- externe Skin Properties

Viele werden beim Shutdown geleert.

Das ist gut.

Der Skin-Resolver-Cache ist allerdings nicht zentral, weil `ExternalSkinService` instanzweise erzeugt wird.

---

# 66. Player Profile Cache

Der PlayerProfileManager verwendet:

```ConcurrentHashMap
```

für aktive und loading Profile.

Das ist angemessen.

Bei Shutdown werden die Maps geleert.

---

# 67. Datenverlust-Risiko bei Save-Failures

Bei einem Save-Fehler wird das Profil wieder dirty markiert.

Das ist richtig.

Bei MySQL wird zusätzlich Emergency YAML geschrieben.

Damit wird ein transienter DB-Fehler nicht sofort still als erfolgreich behandelt.

Das ist ein guter Schutz.

---

# 68. Datenverlust-Risiko beim Shutdown

Der Shutdown versucht aktive Profile zu speichern und wartet bis zu 30 Sekunden.

Danach wird der Executor beendet bzw. nach weiteren 10 Sekunden hart beendet.

Das bedeutet:

**Es gibt eine endliche Shutdown-Garantie, aber keine mathematische Garantie, dass jeder Save innerhalb des Shutdown-Fensters erfolgreich beendet wird.**

Das ist normal und sollte als Recovery-Fall behandelt werden.

---

# 69. NPC-Skin-Shutdown-Risiko

Ein Skin kann während des Shutdowns noch in einem externen HTTP-Request hängen.

Wenn der Request erst nach dem relevanten Save-Fenster fertig wird, kann die neue Skin-Auflösung nicht mehr persistiert werden.

Das ist ein Randfall.

Die Lösung sollte nicht darin bestehen, den Server-Shutdown unbegrenzt zu blockieren.

Besser:

- Skin-Resolution vor Shutdown kontrolliert canceln
- oder bereits vorhandene Source-Daten persistieren
- oder einen persistenten Resolution-State führen

**Priorität:** mittel.

---

# 70. Datenintegrität NPC

Die NPC-Snapshot-Struktur enthält:

- ID
- Typ
- Name
- Welt
- Position
- Rotation
- Skin Source
- Profession
- Skin Value
- Skin Signature

Das ist ausreichend detailliert für die aktuelle Funktionalität.

---

# 71. NPC Entity Tracking

NPCs werden über Maps zwischen:

```NPC ID ↔ Entity UUID
```

zugeordnet.

Chunk-Unload entfernt Tracking.

Chunk-Load respawnt NPCs.

Das ist ein sinnvolles Modell.

---

# 72. Entity-Persistenz

Die Mannequins werden explizit als nicht persistent gesetzt:

```entity.setPersistent(false)
```

Das ist korrekt, wenn der Plugin-NPC über die eigene Datenquelle rekonstruiert wird.

Damit wird vermieden, dass Vanilla/Paper-Entity-Persistenz und Plugin-Persistenz zwei konkurrierende Wahrheiten darstellen.

---

# 73. NPC-Persistenz Single Source of Truth

Die Datenquelle ist:

```npcs.yml
```

Die World-Entity ist ein Runtime-Repräsentant.

Das ist architektonisch sauber.

---

# 74. Content Single Source of Truth

Für Items/Quests/Recipes/Bosse existieren externe JSON-Daten.

Das ist grundsätzlich gut.

Das Risiko besteht weniger in der Architektur als in fehlender automatisierter Cross-Reference-Validierung.

---

# 75. Fehlende Content-Linting-Schicht

Empfohlen:

```validateContent
```

mit Prüfungen für:

- JSON-Syntax
- Pflichtfelder
- IDs
- Duplikate
- Referenzen
- Rezeptzutaten
- Item-IDs
- Questziele
- Bossloot
- Companion-Definitionen
- Resourcepack-Referenzen

Diese Prüfung sollte vor `build` laufen.

**Priorität: sehr hoch.**

---

# 76. Resourcepack-Integrität

Empfohlen:

- JSON Parse Test
- Model → Texture Cross Reference
- Item Definition → Model Cross Reference
- fehlende Assets erkennen
- ungültige `pack.mcmeta` erkennen

Der aktuelle Build prüft nur das Plugin-JAR, nicht die komplette Resourcepack-Integrität.

---

# 77. Dokumentationsqualität

Das Repository enthält:

- `list.md`
- `rezeptliste.md`
- Resourcepack README

Das ist hilfreich.

Es fehlt jedoch eine zentrale technische Betriebsdokumentation mit:

- unterstützter Paper-Version
- Java-Version
- MySQL-Version
- Datenbankrechte
- Backup/Restore
- Migrationen
- Recovery
- Production Configuration
- bekannte externe Dienste
- Rate Limits
- Restart-Verhalten

**Priorität:** mittel.

---

# 78. Production Configuration

Die Config besitzt bereits sinnvolle Defaults.

Allerdings sollte ein Production Guide definieren:

- MySQL statt YAML
- DB User
- Pool Size
- SSL
- Backup
- Executor Threads
- Skin Resolver Verhalten
- Logging

---

# 79. Observability

Vorhanden:

- Logger
- Fehlerlogs
- Skin-Diagnostik
- Shutdown-Logs

Nicht erkennbar:

- Metriken
- Save-Latenzen
- DB-Pool-Auslastung
- Anzahl aktiver Profile
- Skin-Resolution-Dauer
- Task-Dauer
- Quest-/Boss-/NPC-Fehlerzähler

Für ein großes MMORPG-Plugin wäre Telemetrie langfristig sinnvoll.

---

# 80. Fehlerklassifikation

Aktuell werden viele Fehler nur über Exceptions/Logs transportiert.

Empfohlen wäre eine klare Trennung:

- ConfigurationError
- ContentError
- PersistenceError
- ExternalServiceError
- RuntimeStateError
- DataMigrationError

Das würde Forensik und Support erleichtern.

---

# 81. Build-Reife

Der Build ist aktuell:

**GRÜN**

Das bedeutet:

- Kompilation funktioniert
- ShadowJar funktioniert
- Verifikationsaufgaben funktionieren
- Artifact-Struktur stimmt

Es bedeutet ausdrücklich nicht:

**Runtime vollständig verifiziert**

---

# 82. Release-Reife

Aktueller Zustand:

**Technisch fortgeschritten, aber noch nicht vollständig release-forensisch abgesichert.**

Für einen internen Testserver ist der Stand geeignet.

Für einen öffentlichen Production-Server fehlen insbesondere:

1. Runtime Smoke Test
2. Restart/Persistence Integration Test
3. MySQL Integration Test
4. Content Linting
5. sauberer MySQL-Fallback
6. verschärfte externe Skin-SSRF-Abwehr
7. optionaler Gradle Wrapper

---

# 83. Priorisierte Befundliste

## P0 – vor Production zwingend

### P0-1: Runtime-Smoke-Test fehlt

Der Build startet keinen echten Paper-26.2-Server.

**Folge:** Plugin-Enable-/Runtime-Probleme können trotz grüner CI unentdeckt bleiben.

---

### P0-2: Persistenz-Restart-Test fehlt

Gerade die NPC-Skin-Persistenz ist aktuell ein zentraler Funktionsbereich, aber kein automatisierter Restart-Test beweist den gesamten Ablauf.

---

### P0-3: Content-Linting fehlt

Ungültige Rezept-/Quest-/Itemdaten können erst beim Serverstart entdeckt werden.

---

## P1 – dringend

### P1-1: MySQL-Fallback kann Hikari-Ressource verlieren

Konkreter Ressourcenleck-Pfad in `PlayerProfileManager.initialize()`.

---

### P1-2: Externe Skin-URL SSRF-Absicherung

DNS-Prüfung ist vorhanden, aber nicht vollständig gegen DNS-Rebinding/TOCTOU abgesichert.

---

### P1-3: Threading-Vertrag der Profile-Callbacks

Dirty-/Change-Callbacks können synchron auf dem mutierenden Thread ausgeführt werden.

---

### P1-4: Reload-/Lifecycle-Test fehlt

Der Shutdown ist gut strukturiert, aber nicht automatisiert gegen doppelte Registrierungen und Zombie-Ressourcen getestet.

---

## P2 – Wartbarkeit

### P2-1: Gradle Wrapper fehlt

CI ist gepinnt, lokale Builds nicht vollständig reproduzierbar.

### P2-2: Command-Kompatibilitätsadapter

Bukkit-`CommandExecutor`/`TabCompleter`-Schicht könnte langfristig direkt auf Paper-`BasicCommand` umgebaut werden.

### P2-3: Externer HTTP-Service nicht zentral

Ein zentraler Skin/HTTP-Service würde Cache und Ressourcen besser kontrollieren.

### P2-4: Logging der Skin-Auflösung

Ein Teil der INFO-Logs sollte langfristig Debug-Level erhalten.

---

# 84. Positive Architekturmerkmale

Besonders positiv bewertet wurden:

- Paper 26.2
- Java 25
- paperweight Userdev
- Mojang-Mappings
- paper-plugin.yml
- Bootstrapper
- Dialog Registry
- Lifecycle Coordinator
- Snapshot-Persistenz
- Revision Protection
- MySQL Transactions
- Emergency Backup
- Atomic NPC File Replacement
- Async Profile I/O
- explicit executor shutdown
- ShadowJar Relocations
- Build Boundary Verification
- Artifact Verification
- aktuelle Mannequin/ResolvableProfile API
- Adventure Components
- kein Legacy NMS
- kein ChatColor
- keine offensichtlichen statischen Player/Entity/World-Leaks

---

# 85. Forensische Einschätzung der aktuellen Skin-Implementierung

Der aktuelle Skin-Persistenzansatz ist konzeptionell richtig:

```text
Skin Source
    ↓
Resolver
    ↓
exact texture property
    ↓
server-thread handoff
    ↓
StoredSkin
    ↓
NPC snapshot
    ↓
npcs.yml
    ↓
server restart
    ↓
StoredSkin
    ↓
Mannequin profile
```

Damit wird die ursprüngliche Fehlerklasse – dass beim Restart nur eine Source erneut aufgelöst oder aus einem veränderten Runtime-Objekt rekonstruiert wird – wesentlich reduziert.

Die noch offene Frage ist nicht mehr primär die Persistenzarchitektur, sondern die **vollständige Runtime-Verifikation des End-to-End-Flows**.

---

# 86. Was als nächstes technisch sinnvoll ist

Die nächste Entwicklungsphase sollte nicht aus blindem Refactoring bestehen.

Reihenfolge:

1. MySQL-Fallback-Leak beheben.
2. Content-Linter einführen.
3. Paper-26.2 Smoke-Test in CI einführen.
4. NPC-Skin Restart-Test einführen.
5. MySQL Integration-Test einführen.
6. SSRF/DNS-Rebinding-Schutz härten.
7. Reload-/Shutdown-Test einführen.
8. erst danach größere Architektur-Refactorings.

Damit wird zuerst die Beweislage verbessert und danach der Code weiter optimiert.

---

# 87. Release-Gate

Ein zukünftiger Release sollte erst als vollständig geprüft gelten, wenn mindestens folgende Pipeline grün ist:

```text
compile
  ↓
unit/static checks
  ↓
content validation
  ↓
shadow validation
  ↓
Paper 26.2 boot
  ↓
plugin enable
  ↓
smoke tests
  ↓
NPC persistence
  ↓
player persistence
  ↓
MySQL migration
  ↓
server restart
  ↓
shutdown flush
  ↓
artifact validation
```

---

# 88. Schlussurteil

Der aktuelle PixelRPG-Stand ist **technisch wesentlich reifer als ein gewöhnlicher Entwicklungsprototyp**.

Die Kernarchitektur für:

- Persistenz
- Lifecycle
- NPCs
- Dialoge
- Datenbank
- ShadowJar
- Paper-Plugin
- Async-I/O

ist bereits vorhanden und in mehreren Bereichen gut durchdacht.

Die größten verbleibenden Probleme liegen nicht darin, dass die Architektur grundsätzlich falsch wäre, sondern darin, dass **zu viel Verhalten nur statisch bzw. über den Build abgesichert ist**.

Die wichtigste nächste Stufe ist deshalb nicht ein großflächiger Rewrite.

Die wichtigste nächste Stufe ist:

**Runtime beweisbar machen.**

Insbesondere müssen Serverstart, Datenladen, NPC-Skin-Auflösung, Persistenz, Restart und Shutdown automatisiert getestet werden.

Bis diese Gates existieren, sollte das Plugin forensisch als:

```text
BUILD-READY
≠
RUNTIME-PROVEN
≠
PRODUCTION-SEALED
```

bezeichnet werden.

## Endstatus

**Build:** PASS  
**Paper-26.2-Basis:** PASS  
**Java-25-Basis:** PASS  
**Paper Plugin:** PASS  
**Dialog Architecture:** PASS  
**Legacy-NMS Boundary:** PASS  
**Shadow Relocation:** PASS  
**Player Persistence Architecture:** PASS mit offenem Fallback-Leak  
**NPC Persistence:** PASS / Runtime-Test fehlt  
**NPC Skin Persistence:** ARCHITEKTUR PASS / End-to-End-Test fehlt  
**Security:** GOOD BASIS / externe URL-Härtung offen  
**CI:** PASS  
**Runtime Integration:** NICHT AUSREICHEND VERIFIZIERT  
**Content Validation:** OFFEN  
**Release Readiness:** NOCH NICHT VOLLSTÄNDIG ABGESICHERT

---

## Primäre technische Referenzen

- Paper Plugin System: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
- Paper Dialog API: https://docs.papermc.io/paper/dev/dialogs/
- Paper Command Registration: https://docs.papermc.io/paper/dev/command-api/basics/registration/
- Paper 26.2 API: https://jd.papermc.io/paper/26.2/
- Paper 26.2 BasicCommand: https://jd.papermc.io/paper/26.2/io/papermc/paper/command/brigadier/BasicCommand.html
- Paper 26.2 Mannequin: https://jd.papermc.io/paper/26.2/org/bukkit/entity/Mannequin.html
- Paper 26.2 ResolvableProfile: https://jd.papermc.io/paper/26.2/io/papermc/paper/datacomponent/item/ResolvableProfile.html
- Paper 26.2 deprecated API list: https://jd.papermc.io/paper/26.2/deprecated-list.html

---

**Forensik-Stand:** 2026-09-24  
**Geprüfter Commit:** `b02747a21cde6d1308c59a7f027cda80877cfe50`
