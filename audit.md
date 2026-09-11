# PixelRPG — Forensischer Repository-Audit

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Audit-Zielbranch:** `test`  
**Referenzbranch:** `main`  
**Audit-Stand:** 2026-09-11  
**Geprüfter HEAD:** `27c2f6e2b5e807b7e1c915f9d5b01acf36055367`  
**Plattformziel:** Java 25 + Paper 26.2 + Mojang-Mappings  
**Audit-Typ:** statische, forensische Code-/Architektur-/Build-/Persistenzanalyse auf Basis des aktuell hinterlegten Repository-Inhalts

> Dieser Audit bewertet den tatsächlich in `test` hinterlegten Stand. Er ersetzt keinen Laufzeittest auf einem echten Paper-Server und behauptet keine Fehlerfreiheit, die aus statischer Analyse nicht beweisbar ist.

---

## 1. Executive Summary

Der aktuelle `test`-Branch ist technisch deutlich weiter als der gemeinsame Ausgangspunkt von `main`. Die Build-Konfiguration entspricht der vorgegebenen Plattform: Paper Dev Bundle 26.2, Paperweight `2.0.0-beta.21`, Shadow `9.6.1`, Java 25 sowie Gson/HikariCP/MySQL Connector in den erwarteten Versionen. Das Repository verwendet `paper-plugin.yml` und die native Paper-Dialog-API. Die bisherigen Legacy-API-Schutzprüfungen sind weiterhin vorhanden.

Der Branch hat laut GitHub-Vergleich **137 Commits Vorsprung**, ist jedoch gleichzeitig **3 Commits hinter `main`** und damit historisch divergent. Der gemeinsame Merge-Base ist `6e42255c89966922bea0b111883e252772b97dd6`. `test` ist daher kein einfacher linearer Ersatz für `main`; Änderungen aus `main` müssen bei späteren Integrationsentscheidungen bewusst geprüft werden.

Der wichtigste technische Befund ist nicht ein offensichtlicher Legacy-API-Verstoß, sondern die **Persistenz-Fallback-Strategie**: Wenn MySQL beim Start nicht initialisiert werden kann, fällt `PlayerProfileManager` automatisch auf YAML zurück. Für einen produktiven MMORPG-Server ist das gefährlich, weil dadurch ein Server mit einem anderen Persistenz-Backend starten kann als erwartet. Ein leerer oder veralteter YAML-Stand kann dann wie ein gültiger Spielstand behandelt werden. Das ist ein **P0-Datenintegritätsrisiko**.

Weitere relevante Risiken liegen in der Komplexität der zentralen Manager (`PixelRPGPlugin`, `PlayerProfileManager`, `BossManager`, `CompanionFollowTask`) und in der Tatsache, dass einige Persistenzoperationen zwar sauber transaktional ausgeführt werden, aber weiterhin komplette Teilmengen eines Profils löschen und neu schreiben. Das ist funktional robust genug für den aktuellen Maßstab, wird bei hoher Spielerzahl jedoch zum I/O- und Locking-Kostenpunkt.

Positiv ist insbesondere die bereits vorhandene Event-/Dirty-State-Richtung: `WakeScheduler`, `AsyncFileWriter`, revisionsbasierte Player-Persistenz, lokale NPC-/Spawn-/Companion-Verarbeitung und native Dialogs zeigen eine klare Abkehr von globalem Polling. Die vorhandene Performance-/Architekturanalyse beschreibt diese Richtung bereits und ist mit dem aktuellen Code grundsätzlich konsistent.

**Gesamtbewertung:**

| Bereich | Bewertung | Priorität |
|---|---|---|
| Build-/Toolchain-Konformität | gut | P1 |
| Paper-26.2-/Dialog-Konformität | gut | P1 |
| Legacy-API-Abgrenzung | gut | P1 |
| Persistenz-Architektur | gut, aber mit kritischem Fallback | **P0** |
| Datenintegrität | mittel | **P0/P1** |
| Event-/Dirty-State-Architektur | gut | P1 |
| Laufzeit-Skalierbarkeit | mittel bis gut | P1 |
| Lifecycle/Shutdown | gut, komplex | P1 |
| Security-Baseline | mittel | P1 |
| Wartbarkeit | mittel | P1/P2 |
| Produktionsreife | noch nicht vorbehaltlos | **P0/P1** |

---

## 2. Prüfgrundlage und Beweislage

### 2.1 Direkt geprüfte Repository-Struktur

Der `test`-Tree enthält unter anderem:

- Gradle-/CI-Konfiguration
- `paper-plugin.yml`
- Bootstrap- und Hauptplugin
- API-Schicht
- Boss-, Combat- und Skill-System
- Companion-System
- Dialog-/NPC-System
- Economy/Guild/Party
- Item-/Equipment-System
- Player/Profile-/Persistence-System
- Professionen/Crafting
- Quests
- Regionen/Spawn-System
- Scoreboard/Statistics
- Trade Depot
- JSON-/YAML-Daten

Die aktuelle Tree-Revision ist `27c2f6e2b5e807b7e1c915f9d5b01acf36055367`.

### 2.2 Build-/CI-Beweislage

Der letzte im Branch beobachtete GitHub-Actions-Build für den aktuellen HEAD lief erfolgreich. Der Build verwendet Java 25 und Gradle 9.2.0 und führt `gradle clean build --no-daemon --stacktrace` aus. Zusätzlich werden Legacy-API-Grenzen und das Shadow-Artefakt geprüft.

Wichtig: Ein erfolgreicher CI-Build beweist **Kompilierbarkeit und die vorhandenen statischen Prüfungen**, aber nicht:

- korrekte Spiellogik unter realer Last
- Race Conditions über externe Server-/Plugin-Interaktionen
- Datenbankfehler unter Netzwerkausfällen
- tatsächliche Paper-Laufzeitsemantik aller Interaktionen
- Speicher-/Tick-Langzeitverhalten
- Spielerexploitbarkeit

### 2.3 Historische Differenz zu `main`

`test` und `main` sind divergent. Der GitHub-Vergleich weist aus:

- `test`: 137 Commits voraus
- `test`: 3 Commits zurück
- gemeinsamer Merge-Base: `6e42255c89966922bea0b111883e252772b97dd6`

Unter den Änderungen befinden sich zahlreiche Laufzeitbereiche, darunter Player Persistence, Companion, Boss, Mob Scaling, Quest, Region Spawn, Scoreboard, Trade Depot und Storage.

Daraus folgt: `main` darf für Architekturvergleiche verwendet werden, aber der hier dokumentierte Audit bewertet **ausschließlich den aktuellen `test`-Stand**.

---

## 3. Build- und Plattform-Audit

### Befund B-01 — Toolchain stimmt mit Zielplattform überein

`build.gradle` verwendet:

- Paperweight Userdev `2.0.0-beta.21`
- Paper Dev Bundle `26.2.build.121-stable`
- Java Toolchain 25
- `options.release = 25`
- Shadow `9.6.1`
- Gson `2.13.1`
- HikariCP `7.0.2`
- MySQL Connector/J `9.7.0`

Das entspricht der verbindlichen Projektbasis.

**Bewertung:** PASS.

### Befund B-02 — Shadow-Konfiguration ist vorhanden und verhärtet

Das Shadow-Artefakt heißt `Pixel-RPG.jar`. Gson, HikariCP und MySQL werden in die vorgesehenen `de.pixelrpg.rpg.libs.*`-Namespaces verschoben. `mergeServiceFiles()` ist aktiviert. Zusätzlich wird geprüft, dass der MySQL-JDBC-Service auf den relozierten Treiber zeigt.

**Bewertung:** PASS.

### Befund B-03 — `check` enthält weiterhin die Schutzprüfungen

`check` hängt sowohl `verifyPixelRpgSourceBoundaries` als auch `verifyPixelRpgShadedDependencies` ein. Damit wurden die projektspezifischen Verifikationsmechanismen nicht versehentlich aus dem Build entfernt.

**Bewertung:** PASS.

### Befund B-04 — CI und Gradle prüfen teilweise unterschiedliche Details

Der Gradle-Task prüft den JDBC-Serviceinhalt explizit auf `de.pixelrpg.rpg.libs.mysql.cj.jdbc.Driver`. Der CI-Schritt prüft zusätzlich das Vorhandensein des Service-Descriptors, validiert dessen konkreten Inhalt aber nicht mit derselben Strenge.

**Risiko:** gering. Der eigentliche `gradle build` führt die Gradle-Prüfung bereits aus.

**Empfehlung:** CI kann später vereinheitlicht werden, ist aber kein akuter Fehler.

---

## 4. Plugin-/Bootstrap-Audit

`paper-plugin.yml` definiert:

- `main: de.pixelrpg.rpg.PixelRPGPlugin`
- `bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap`
- `api-version: '26.2'`

Der Bootstrap registriert native Dialog-Registry-Einträge über die aktuellen Paper-Dialog-Klassen und fügt den Charakterdialog dem Quick-Actions-Tag hinzu.

### Befund P-01 — Native Dialog-API korrekt eingesetzt

Die Dialogimplementierung verwendet `io.papermc.paper.dialog.*` und `io.papermc.paper.registry.data.dialog.*`. Es ist keine 1.21.x-Dialog-Reimplementierung erkennbar.

`DialogueEngine` nutzt `Player.showDialog(...)`, `Dialog.create(...)`, `DialogType.multiAction(...)`, `DialogType.confirmation(...)` und `DialogInput` aus der aktuellen API.

**Bewertung:** PASS.

### Befund P-02 — Hauptplugin ist stark zentralisiert

`PixelRPGPlugin` erzeugt und registriert einen sehr großen Teil der Laufzeitarchitektur selbst: Player, Stats, Professionen, Items, Equipment, Shops, Story, Party, Quests, Regionen, Bosse, Scoreboard, NPCs, Dialoge, Companion, Combat und Listener.

Das ist aktuell nachvollziehbar, erzeugt aber eine hohe Kopplung zwischen Lifecycle und Feature-Initialisierung.

**Risiko:** mittel.

**Empfehlung:** Kein großer Refactor jetzt. Bei zukünftigen Änderungen nur featureweise kleine Lifecycle-Module auslagern.

---

## 5. Kritischer Persistenzbefund

### Befund S-01 — Automatischer MySQL→YAML-Fallback ist für Produktion gefährlich

`PlayerProfileManager.initialize(...)` versucht bei `StorageType.MYSQL`, die Datenbank zu verbinden und das Repository zu initialisieren. Bei **jedem** Fehler wird geloggt und anschließend:

1. `storageType = StorageType.YAML`
2. `YamlPlayerProfileRepository` erstellt
3. YAML-Repository initialisiert
4. Plugin fährt weiter

Das bedeutet: Ein temporärer MySQL-Ausfall kann einen Serverstart nicht verhindern, sondern in ein anderes Persistenzsystem umleiten.

Das ist bei einem MMORPG keine harmlose Graceful-Degradation. Der Server könnte Spielerprofile aus einem leeren oder veralteten YAML-Verzeichnis laden und diese später wieder persistieren. Damit entsteht das Risiko von:

- scheinbarem Fortschrittsverlust
- divergierenden Profilständen
- Überschreiben eines korrekten Datenbankstands durch einen Fallbackstand
- schwer nachvollziehbarer Datenkorruption
- unterschiedlichen Wahrheiten je nach Startzeitpunkt

**Schweregrad: P0.**

**Empfohlene Zielstrategie:** Wenn `storage.type=MYSQL` konfiguriert ist und MySQL nicht initialisiert werden kann, sollte der Plugin-Start kontrolliert fehlschlagen oder ausdrücklich in einen read-only/degraded state gehen. Ein automatischer Wechsel des Persistenz-Backends sollte nicht stillschweigend erfolgen.

### Befund S-02 — Emergency YAML Backup ist sinnvoll, darf aber nicht zur primären Wahrheit werden

Bei einem MySQL-Speicherfehler schreibt `PlayerProfileManager` ein Emergency-YAML-Backup. Das ist als Recovery-Mechanismus sinnvoll.

Die Semantik sollte jedoch eindeutig bleiben:

`MySQL = authoritative storage`  
`Emergency YAML = recovery artifact`

Nicht:

`MySQL failed -> YAML becomes active storage`

**Schweregrad:** P1 als Architekturthema, P0 im Zusammenhang mit S-01.

---

## 6. PlayerProfileManager — Race-/Revision-Audit

Die aktuelle Implementierung besitzt mehrere gute Schutzmechanismen:

- `ConcurrentHashMap` für aktive Profile
- per-UUID Save Chains
- coalescing `pendingSaves`
- Snapshot-Erstellung vor asynchroner Persistierung
- Mutation-/Persistence-Revisions
- definierter Shutdown
- Emergency Backup

### Befund S-03 — Revision Locking ist ein guter Schutz gegen stale writes

Das MySQL-Repository liest die `persistence_revision` mit `SELECT ... FOR UPDATE`, vergleicht sie mit der erwarteten Revision und verweigert veraltete Writes.

Das verhindert, dass ein älterer Profilstand einen neueren Stand blind überschreibt.

**Bewertung:** POSITIV.

### Befund S-04 — Save-Coalescing ist korrekt auf UUID-Ebene modelliert

Ein bereits ausstehender Save wird nicht für denselben Spieler dupliziert. Nach Abschluss wird erneut geprüft, ob das Live-Profil wieder dirty ist.

Das ist deutlich besser als ein Save pro Mutation.

**Bewertung:** POSITIV.

### Befund S-05 — Datenbankprofil speichert mehrere Tabellen in einer Transaktion

Player, aktive Quests, Equipment und Statistics werden innerhalb derselben JDBC-Transaktion persistiert. Bei SQLException erfolgt Rollback.

**Bewertung:** PASS.

### Befund S-06 — Vollständiges Löschen/Neuaufbauen von Child-State ist skalierbar nur bis zu einem gewissen Punkt

Beim Speichern werden aktive Quests, Equipment und Statistics zunächst gelöscht und danach erneut geschrieben.

Vorteile:

- einfache Konsistenz
- wenig komplexe Diff-Logik
- Snapshot ist vollständig

Nachteile:

- unnötige DELETE/INSERT-Arbeit bei kleinen Änderungen
- größere Transaktionen
- mehr Lock-/Binlog-/I/O-Kosten
- Skalierungsproblem bei großen Profilen

**Priorität:** P1, nicht P0.

**Empfehlung:** Nicht sofort umbauen. Erst bei realer DB-Last oder Profilwachstum auf delta-basierte Upserts wechseln.

---

## 7. Datenbank-Audit

### Positiv

- HikariCP statt eigener Connection-Pools
- Prepared Statements
- Connection Timeout
- Poolgröße begrenzt
- Keepalive/MaxLifetime konfiguriert
- SSL-Modus validiert
- Datenbank-/Host-Eingaben werden eingeschränkt
- Schema-Versionierung vorhanden
- Migrationen sind explizit

### Befund DB-01 — Default-Credentials sind offensichtlich Entwicklungswerte

`config.yml` enthält:

`username: "root"`  
`password: "CHANGE_ME"`

Das ist kein geleaktes Secret, weil es ein Platzhalter ist. Für eine Distribution ist es dennoch wichtig, dass keine echte Produktionskonfiguration daraus entsteht.

**Bewertung:** P1 Hardening.

### Befund DB-02 — YAML als Default Storage ist entwicklungsfreundlich, aber für große Produktion nicht ideal

`storage.type` steht standardmäßig auf YAML.

Das ist als lokale Entwicklungsbasis sinnvoll. Für einen MMORPG-Produktionsbetrieb muss MySQL bewusst konfiguriert und der Storage-Modus explizit validiert werden.

### Befund DB-03 — SSL ist standardmäßig REQUIRED

Das ist positiv. Gleichzeitig erlaubt die Konfiguration bewusst `DISABLED`, `PREFERRED`, `VERIFY_CA` und `VERIFY_IDENTITY`.

Das ist angemessen, solange `DISABLED` nicht als Produktionsstandard verwendet wird.

---

## 8. AsyncFileWriter-Audit

`AsyncFileWriter` verwendet:

- einen dedizierten Single-Thread-Executor
- coalescing `Map<Path, String>`
- atomare Dateiersetzung, sofern vom Dateisystem unterstützt
- Fallback auf normale Move-Operation
- definierte Shutdown-Logik

### Befund F-01 — File writes sind sauber vom Gameplay entkoppelt

Der Writer blockiert den Serverthread nicht mit regulären Dateioperationen.

### Befund F-02 — Shutdown-Race wurde sinnvoll berücksichtigt

Die Implementierung schützt die Kombination aus `closed`, `draining`, `pending` und Executor-Shutdown gegen typische Queue-/Drain-Rennen.

**Bewertung:** gut.

### Rest-Risiko

Die Queue ist zwar nach Schlüssel coalesced, besitzt aber keine globale Byte-/Speichergrenze. Ein extrem großer Snapshot kann weiterhin viel Heap beanspruchen.

Für normale Player-/Guild-/Trade-Dateien ist das derzeit kein akuter Befund.

---

## 9. Companion-System

Der Companion-Code verwendet inzwischen `WakeScheduler` und verarbeitet Companion-Runtime lokal pro Owner. `PlayerMoveEvent` löst nur bei Blockgrenzen eine Bewegungsauswertung aus.

### Positiv

- keine globale Companion-Tick-Schleife für alle Spieler erkennbar
- lokale Owner-Wakeups
- Runtime-State-Caching
- Equipment-Hash als Change Detection
- PDC-basierte Companion-Identität
- Lifecycle für Join/Quit/World Change

### Befund C-01 — CompanionFollowTask ist weiterhin komplex und physiknah

Die Bewegungslogik kombiniert Teleport, Velocity, Step-Up, Step-Down, Gravity-/Fallzustand und Mannequin-Sonderlogik.

Das ist kein Architekturfehler, aber ein Hotspot für Regressionen.

**Priorität:** P1 Testabdeckung.

### Befund C-02 — `HashMap`/`HashSet` für Runtime-State sind kontextabhängig sicher

Die Runtime-Strukturen sind nicht generell thread-safe. Der Code ist jedoch als serverthreadbasierte Laufzeitlogik strukturiert. Sollte künftig aus asynchronen Callbacks auf diese Methoden zugegriffen werden, wäre eine harte Thread-Grenze erforderlich.

**Empfehlung:** Async-Code niemals direkt auf Bukkit-Entities oder diese Runtime-Maps loslassen.

---

## 10. Boss-System

`BossManager` besitzt:

- aktive Bosses nach UUID
- Bossbars
- Phasen
- Angriffsmuster
- Schadensbeiträge
- Loot
- Party-/Registered-Player-Auflösung
- Cleanup

### Positiv

- aktive Bosse sind indexiert
- Bossbars werden lokal über Radius aktualisiert
- squared-distance wird wiederverwendet
- Boss-Phasen sind zeit-/zustandsabhängig statt globaler Spielerscan
- Loot läuft über den ItemService

### Befund BO-01 — Boss-Timer bleiben bewusst periodisch

Das ist hier gerechtfertigt: Phasen und Attack-Cooldowns sind echte zeitliche Zustände.

Die Architektur muss nicht zwanghaft event-driven werden, wenn Zeit selbst der Input ist.

### Befund BO-02 — Cleanup über nahe Entitäten kann teuer werden

Beim World-Boss-Cleanup wird der relevante Weltbereich nach Entities durchsucht. Das ist ein lokaler Vorgang und deutlich besser als ein globaler Scan.

**Priorität:** P2/P1 abhängig von Boss-Frequenz.

---

## 11. Combat-Audit

Der Combat-Bereich trennt Damage Calculation, Context, State und Listener. Die Performance-Architektur nutzt indexierte Combat-Zustände und zeitbasierte Ablaufpunkte.

### Befund CB-01 — Keine offensichtliche Datenbankkopplung im Hit-Hotpath

Die zentrale Combat-Struktur arbeitet mit in-memory Player-/Stat-Zuständen. Das ist korrekt.

### Befund CB-02 — Damage-System ist ein kritischer Balancing-Hotspot

Damage-Formeln sollten bei zukünftigen Änderungen mit deterministischen Tests abgesichert werden. Statische Analyse kann mathematische Balancefehler nicht zuverlässig erkennen.

**Priorität:** P1.

---

## 12. NPC-/Dialog-Audit

Die NPC-Architektur ist in Manager, Registry, Behavior und Interaktionslistener getrennt. Das ist eine brauchbare Domain-Trennung.

`DialogueEngine` zentralisiert native Dialogerzeugung und normalisiert Text/Body-Komponenten.

### Positiv

- native Dialog-API
- `ClickCallback.Options.builder().uses(1)` bei Input-Aktionen
- serverseitige Player-Prüfung im Callback
- Escape-/Close-Verhalten explizit
- NPC-Behaviors sind registrierbar

### Befund NPC-01 — Input-Validierung muss weiterhin an den fachlichen Endpunkten stattfinden

Die Dialog-API verhindert nicht automatisch negative Zahlen, ungültige IDs oder unzulässige Aktionen. Jeder Behavior-/Command-Endpunkt muss seine Geschäftsregeln selbst validieren.

**Priorität:** P1 Security/Logic Review.

---

## 13. Quest-System

Der Quest-Bereich ist umfangreich und enthält Repository, Manager, Progress, Inventory Tracker, Navigation und Listener.

Die aktuelle Architektur bewegt sich weg von permanentem globalem Polling hin zu invalidierungs- und wake-basierten Prüfungen.

### Positiv

- Quest-State wird zentral verwaltet
- Inventory-Ziele werden zusammen verarbeitet
- Navigation ist separat
- Mob-Kills haben explizite Listener
- aktive Quests werden persistiert

### Befund Q-01 — Passive Checks bleiben ein Bereich für Langzeitoptimierung

Die Existenz von `QuestPassiveCheckTask` ist nicht automatisch falsch. Einige Ziele sind tatsächlich zeit-/zustandsabhängig. Entscheidend ist, dass es keine unnötigen globalen Wiederholungsprüfungen ausführt.

**Priorität:** P1 Performance-Verifikation.

---

## 14. Region-/Spawn-System

Die Region-Schicht besteht aus Geometry, Manager, Repository, Policy, Transition und Spawn-Service.

Die Performance-Architektur weist bereits eine Chunk-/Spawnpoint-orientierte Verarbeitung aus.

### Positiv

- Spawnpoints sind räumlich adressierbar
- Respawn ist ein zeitlicher Zustand und kann lokal geplant werden
- Spielerbewegung kann lokale Spawnpoints aktivieren

### Risiko

Spawnlogik ist inhärent komplex, weil World-Loaded-State, Mob-Lifecycle und Spielerreichweite zusammenwirken. Ein Runtime-Stresstest ist hier wichtiger als weitere abstrakte Refactorings.

---

## 15. Item-/Equipment-/Economy-Audit

Das Repository trennt ItemDefinition, ItemService, Builder, Rarity, Soulbound, Unique Items und Equipment-Service.

### Positiv

- Item-Definitionen sind datengetrieben
- Item-Erstellung ist zentralisiert
- Economy verwendet eigene Geldrepräsentation
- Equipment ist von Item-Definitionen getrennt

### Befund E-01 — Geldspeicherung als Minor Units ist richtig

Die MySQL-Struktur verwendet `BIGINT money_minor_units`. Die Migration von einem älteren Geldfeld erfolgt explizit.

Das reduziert Floating-Point-Probleme bei persistiertem Geld.

### Befund E-02 — Migration muss fachlich einmalig validiert werden

Die Migration verwendet `ROUND(money * 100)`. Bei historischen Floating-Point-/Dezimalständen kann Rundung erwartbar sein, muss aber fachlich akzeptiert werden.

**Priorität:** P1 bei Live-Datenmigration.

---

## 16. Guild-/Party-/Trade-Persistenz

Diese Systeme wurden laut aktuellem Tree und Architektur-Audit auf revisions-/dirty-orientierte Persistenz umgestellt.

### Positiv

- Persistenz ist nicht direkt an jeden UI-Read gekoppelt
- Guild-/Party-Zustände werden in-memory gehalten
- Dateioperationen sind über `AsyncFileWriter` entkoppelt

### Risiko

Guild Bank, Party und Trade Depot sind wirtschaftlich sensible Systeme. Ein statischer Audit kann nicht beweisen, dass jede mögliche Doppeltransaktion unter gleichzeitigen Spieleraktionen korrekt ist.

**Empfehlung:** gezielte concurrency/property tests für:

- gleichzeitig kaufen/verkaufen
- gleichzeitig einzahlen/abheben
- gleichzeitig verlassen/einladen
- gleichzeitig Item transferieren
- Crash direkt nach Mutation

---

## 17. API-/Service-Audit

Die API-Schicht bietet unter anderem Economy, Guild, Item, Party und Statistics.

Services werden über Bukkit/Paper ServiceManager registriert.

### Positiv

Das ermöglicht Feature-Entkopplung und verhindert, dass externe Systeme direkt auf interne Manager zugreifen müssen.

### Befund API-01 — API-Verträge sollten weiterhin live-object-frei bleiben

Die bestehenden Build-Grenzen verhindern statische Live-Referenzen auf `Player`, `Entity` und `World`. Für öffentliche API-Verträge sollte weiterhin bevorzugt mit UUIDs, IDs und immutable snapshots gearbeitet werden.

**Priorität:** P1 Architekturregel.

---

## 18. Security-Audit

### Geprüfte Risikoklassen

- Legacy-ChatColor
- CraftBukkit
- Legacy-NMS
- statische Live-Serverreferenzen
- SQL Injection durch dynamische Identifikatoren
- unverschlüsselte Standard-DB-Verbindung
- Credential-Leaks
- unkontrollierte HTTP-Abhängigkeiten
- Input-Callbacks

### Befund SEC-01 — Legacy-API-Suche ohne Treffer

Repository-Code-Suche ergab keine Treffer für `ChatColor` und `org.bukkit.craftbukkit`.

Die Build-Prüfungen decken zusätzlich entsprechende Muster ab.

**Bewertung:** PASS.

### Befund SEC-02 — SQL verwendet überwiegend Prepared Statements

Die Player-Persistenz verwendet Prepared Statements für UUIDs, Werte und Inserts. Datenbank-/Host-Eingaben werden vor dem JDBC-URL-Aufbau validiert.

### Befund SEC-03 — Dynamische Schema-Identifier sind eingeschränkt

Datenbankname und Host werden gegen zulässige Zeichen geprüft. Das reduziert URL-/Identifier-Injection-Risiken.

### Befund SEC-04 — Produktions-Credentials gehören nicht ins Repository

Die aktuelle Konfiguration enthält nur `CHANGE_ME`. Das ist akzeptabel als Platzhalter, solange niemals echte Passwörter in `config.yml` oder Git committed werden.

---

## 19. Codequalität und Wartbarkeit

### Größte Klassen-/Komplexitäts-Hotspots

- `PixelRPGPlugin`
- `PlayerProfileManager`
- `PlayerProfile`
- `BossManager`
- `QuestManager`
- `CompanionService`
- `CompanionFollowTask`
- `RegionManager`
- `TradeDepotManager`
- `ScoreboardService`
- `RPGItemBuilder`

Diese Klassen sind nicht automatisch falsch. Sie sind jedoch die Stellen, an denen zukünftige Features am ehesten Seiteneffekte erzeugen.

### Befund QL-01 — Keine großflächigen Refactorings ohne Anlass

Die aktuelle Architektur funktioniert als modularisiertes Feature-System. Ein aggressives "Clean Architecture"-Refactoring würde momentan mehr Regressionsrisiko erzeugen als Nutzen.

**Empfehlung:** kleine, verhaltensneutrale Extraktionen nur bei konkreten Problemen.

---

## 20. Legacy-/Verifikations-Audit

Die folgenden Schutzmechanismen sind im aktuellen `test`-Build vorhanden:

- ChatColor-Prüfung
- Bungee-ChatColor-Prüfung
- Legacy-NMS-Musterprüfung
- CraftBukkit-Prüfung
- statische Live-Serverreferenz-Prüfung
- Shadow-Relocation-Prüfung
- MySQL-JDBC-Service-Prüfung
- `paper-plugin.yml`-Artefaktprüfung in CI

**Bewertung:** PASS.

Die Repository-Code-Suche ergab keine offensichtlichen Treffer für die geprüften Legacy-Muster.

---

## 21. Bekannte Grenzen dieses Audits

Dieser Audit ist statisch. Nicht beweisbar sind ohne Server-/Lastumgebung insbesondere:

1. tatsächliche TPS-/MSPT-Werte
2. Speicherwachstum über mehrere Stunden/Tage
3. Chunk-/World-Lifecycle unter hoher Spielerzahl
4. tatsächliche MySQL-Latenz
5. Datenbankausfall während Live-Mutationen
6. doppelte Events durch andere Plugins
7. Paper-/Minecraft-Laufzeitverhalten aller Entities
8. Clientseitige Dialogdarstellung über alle Eingabefälle
9. tatsächliche Race Conditions zwischen Serverthread und externen Async-Tasks
10. wirtschaftliche Exploits durch kombinierte Spieleraktionen

Diese Punkte dürfen nicht als "bestanden" markiert werden, nur weil der Code kompiliert.

---

## 22. Priorisierte Maßnahmenliste

### P0 — vor produktivem Einsatz

#### P0-01 — MySQL-Fallback abschalten

Wenn `storage.type=MYSQL` gesetzt ist und MySQL nicht initialisiert werden kann:

- Plugin-Start kontrolliert abbrechen, oder
- explizit read-only/degraded mode verwenden.

Kein stiller Wechsel auf YAML.

#### P0-02 — Persistenz-Verlustszenarien testen

Testmatrix:

- MySQL down beim Start
- MySQL down während Save
- Connection timeout während Save
- Server kill während Save
- Server kill während Emergency Backup
- zwei Saves mit schneller Mutation
- stale revision

#### P0-03 — Produktionsdatenmigration separat validieren

Vor Einsatz von Schema-Version 2 mit echten Daten eine Kopie der Produktionsdaten migrieren und Geldstände prüfen.

---

### P1 — kurzfristig

#### P1-01 — Concurrency Tests für Economy/Guild/Party/Trade

Gezielte Paralleltests auf Doppeltransaktionen.

#### P1-02 — Runtime-Stresstest

Empfohlenes Szenario:

- 50/100/200 simulierte Spieler
- viele NPCs
- viele Mob-Spawns
- aktive Companions
- mehrere offene GUIs
- Quests
- Bosskampf
- Guild-/Party-Aktivität
- gleichzeitige Saves

Messwerte:

- MSPT p50/p95/p99
- Heap
- GC
- DB connections
- DB query latency
- async queue depth
- active scheduled wakeups

#### P1-03 — Zentralisierte Lifecycle-Tests

Für jede Komponente prüfen:

`construct -> start -> runtime -> stop -> repeated stop`

Insbesondere:

- executors
- scheduler tasks
- listeners
- file writers
- repositories
- boss tasks
- NPC look task
- spawn services

#### P1-04 — Command-/Dialog-Input-Fuzzing

Negative Zahlen, große Zahlen, leere Strings, Unicode, ungültige IDs, nicht registrierte Spieler, Race zwischen Dialogöffnung und Player Quit.

---

### P2 — später

- Child-table Delta-Upserts
- weitere kleine Manager-Aufteilungen
- Cache-/Index-Konsolidierung
- zusätzliche statische Analysen
- Metriken/Observability
- gezielte Heap-/Allocation-Optimierung

---

## 23. Was ausdrücklich nicht geändert werden sollte

Ohne konkreten Auftrag sollten folgende funktionierende Eigenschaften erhalten bleiben:

- Paper 26.2 als Zielplattform
- Java 25
- `paper-plugin.yml`
- native Dialog-API
- Mojang-Mappings
- Adventure Components
- Shadow-Relocations
- bestehende Build-Verifikationen
- UUID-/Revision-basierte Player Persistence
- AsyncFileWriter
- WakeScheduler
- event-/dirty-state-orientierte Architektur
- bestehende Gameplay-Balance
- bestehende Quest-/Companion-/Boss-/Region-Semantik

---

## 24. Schlussurteil

Der `test`-Branch ist kein ungepflegter Prototyp mehr. Er besitzt bereits eine erkennbare, moderne Paper-26.2-Architektur mit Daten-/Feature-Trennung, asynchroner Persistenz, revisionsbasierter Speicherung, nativen Dialogen und event-/wake-orientierter Verarbeitung.

Der größte Fehler ist derzeit nicht ein veraltetes API-Muster, sondern eine **falsche Fehlersemantik bei der Persistenz**: Ein MySQL-Initialisierungsfehler darf bei einem produktiven RPG-System nicht automatisch dazu führen, dass YAML zum aktiven Ersatz-Backend wird.

Abgesehen davon sind die wesentlichen Risiken überwiegend Skalierungs-, Testabdeckungs- und Komplexitätsthemen. Die vorhandenen Performance-Optimierungen sind grundsätzlich in die richtige Richtung gegangen; ein weiterer großer Architekturumbau wäre aktuell weniger sinnvoll als gezielte Tests und die Absicherung der kritischen Persistenzgrenzen.

**Finales Urteil:**

> **Technisch solide Entwicklungsbasis, aber noch nicht vorbehaltlos produktionssicher. P0: Persistenz-Fallback korrigieren. Danach gezielte Concurrency-, Failure-Recovery- und Lasttests durchführen.**

---

## 25. Audit-Evidenz

Die wichtigsten direkt geprüften Artefakte im aktuellen Branch sind:

- `build.gradle`
- `settings.gradle`
- `paper-plugin.yml`
- `PixelRPGBootstrap.java`
- `PixelRPGPlugin.java`
- `PlayerProfileManager.java`
- `MySQLPlayerProfileRepository.java`
- `DatabaseManager.java`
- `AsyncFileWriter.java`
- `DialogueEngine.java`
- `BossManager.java`
- `CompanionFollowTask.java`
- `config.yml`
- `.github/workflows/build.yml`
- `PERFORMANCE_ARCHITECTURE_AUDIT.md`

Der Repository-Tree des geprüften `test`-Standes ist nicht als vollständig fehlerfrei zu interpretieren; die Aussagekraft der einzelnen Befunde ist jeweils auf die direkt beobachtete Implementierung und deren statische Beziehungen begrenzt.

**Audit-Ende.**
