# PixelRPG — Forensischer Repository-Audit

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Audit-Zielbranch:** `test`  
**Referenzbranch:** `main`  
**Audit-Stand:** 2026-09-11  
**Geprüfter Code-Stand:** `test` zum Audit-Zeitpunkt  
**Plattformziel:** Java 25 + Paper 26.2 + Mojang-Mappings  
**Audit-Typ:** statische, forensische Code-/Architektur-/Build-/Persistenzanalyse

> **Wichtige Korrektur:** Dieser Audit beschreibt PixelRPG als das tatsächlich im Repository vorhandene **Minecraft-Paper-Plugin-/RPG-Projekt**. Es wird keine MMORPG-Server-Architektur unterstellt. Begriffe wie Spieler, Party, Guild, Economy, Quests, Bosse, Companions usw. werden ausschließlich als im Code vorhandene Features bewertet.

---

## 1. Executive Summary

Der `test`-Branch besitzt eine moderne Paper-26.2-/Java-25-Basis mit `paper-plugin.yml`, Mojang-Mappings, nativer Dialog-API, Adventure Components, asynchroner Persistenz und projektspezifischen Build-Verifikationen.

Der Audit bewertet **nicht**, was PixelRPG sein könnte, sondern ausschließlich, was der aktuelle Code tatsächlich implementiert. Es wird insbesondere keine MMORPG-Architektur, kein klassisches MMORPG-Servermodell und keine entsprechende Produktanforderung als Maßstab verwendet.

Der wichtigste technische Befund bleibt die Persistenzfehlerbehandlung: Bei konfiguriertem MySQL kann `PlayerProfileManager` bei der Initialisierung auf YAML zurückfallen. Das ist unabhängig vom Produkttyp ein potenzielles Datenintegritätsproblem, weil damit das aktive Persistenz-Backend während eines Fehlerzustands wechselt.

Weitere Befunde betreffen Lifecycle-Kopplung, Persistenzkosten, Concurrency, Async-/Serverthread-Grenzen, Dialog-/Input-Validierung und Testabdeckung. Diese Punkte sind technische Eigenschaften des vorhandenen Plugins und keine Aussage über eine vermeintliche MMORPG-Zielarchitektur.

### Gesamtbewertung

| Bereich | Bewertung | Priorität |
|---|---|---|
| Build-/Toolchain-Konformität | gut | P1 |
| Paper-26.2-/Dialog-Konformität | gut | P1 |
| Legacy-API-Abgrenzung | gut | P1 |
| Persistenz | gut, aber mit kritischem Fallback | **P0** |
| Datenintegrität | mittel | **P0/P1** |
| Async-/Dirty-State-Architektur | gut | P1 |
| Laufzeitkomplexität | mittel bis gut | P1 |
| Lifecycle | gut, aber zentralisiert | P1 |
| Security-Baseline | gut mit Hardening-Punkten | P1 |
| Wartbarkeit | mittel | P1/P2 |

---

## 2. Prüfgrundlage

Geprüft wurde der aktuelle Inhalt des Branches `test` sowie `main` ausschließlich als Referenz für den Branch-Vergleich.

Direkt betrachtet wurden insbesondere:

- `build.gradle`
- `settings.gradle`
- `src/main/resources/paper-plugin.yml`
- `src/main/resources/config.yml`
- `PixelRPGBootstrap.java`
- `PixelRPGPlugin.java`
- `PlayerProfileManager.java`
- `MySQLPlayerProfileRepository.java`
- `DatabaseManager.java`
- `AsyncFileWriter.java`
- `DialogueEngine.java`
- `BossManager.java`
- `CompanionFollowTask.java`
- `.github/workflows/build.yml`
- `PERFORMANCE_ARCHITECTURE_AUDIT.md`

Zusätzlich wurden Repository-Suchen nach relevanten Legacy-/Gefahrenmustern durchgeführt.

### Aussagegrenze

Der Audit ist statisch. Er ersetzt keinen echten Paper-Runtime-Test, keinen Lasttest und keinen vollständigen Security-Pentest.

---

## 3. Branch-/Versionslage

`test` und `main` sind historisch divergent. Der geprüfte Stand von `test` war zum Audit-Zeitpunkt von einem Vergleich mit `main` geprägt, bei dem `test` deutlich mehr eigene Commits enthielt, aber gleichzeitig Commits hinter `main` lag.

Das ist für die Entwicklung relevant:

- `main` bleibt Referenz-/Sollzustand.
- `test` bleibt Entwicklungsbranch.
- Änderungen aus `main` dürfen nicht blind übernommen werden.
- Der Audit bewertet ausschließlich den tatsächlich geprüften `test`-Code.

---

## 4. Build- und Plattform-Audit

### B-01 — Java/Paper-Basis

`build.gradle` verwendet die vorgegebene Plattformbasis:

- Java 25 Toolchain
- Compiler Release 25
- Paperweight Userdev `2.0.0-beta.21`
- Paper Dev Bundle `26.2.build.121-stable`
- Shadow `9.6.1`

**Bewertung:** PASS.

### B-02 — Dependencies

Vorhanden sind die festgelegten Versionen:

- Gson `2.13.1`
- HikariCP `7.0.2`
- MySQL Connector/J `9.7.0`

**Bewertung:** PASS.

### B-03 — Shadowing

Das finale Artefakt wird als `Pixel-RPG.jar` erzeugt. Die Third-Party-Abhängigkeiten werden in die vorgesehenen `de.pixelrpg.rpg.libs.*`-Namespaces relocated. Service-Dateien werden zusammengeführt.

**Bewertung:** PASS.

### B-04 — Build-Verifikation

`check` bindet die projektspezifischen Source-/Dependency-Grenzprüfungen ein. Zusätzlich prüft CI das erzeugte Plugin-Artefakt und die relevanten Legacy-/Relocation-Grenzen.

**Bewertung:** PASS.

---

## 5. Plugin-Bootstrap und Lifecycle

`paper-plugin.yml` verwendet den aktuellen Paper-Plugin-Aufbau mit:

- `main: de.pixelrpg.rpg.PixelRPGPlugin`
- `bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap`
- `api-version: '26.2'`

### P-01 — Native Dialog-Integration

Der Bootstrap registriert die aktuellen Paper-Dialog-Daten/Registry-Strukturen. Die Dialoglogik verwendet die native Paper-API und keine alte 1.21.x-Reimplementierung.

**Bewertung:** PASS.

### P-02 — Zentrale Initialisierung

`PixelRPGPlugin` übernimmt einen großen Teil der Komponenteninitialisierung und Listener-Registrierung. Das ist funktional nachvollziehbar, erzeugt aber hohe Lifecycle-Kopplung.

Das Risiko ist vor allem bei späteren Änderungen relevant: Fehler in Initialisierungsreihenfolge, Shutdown oder Dependency-Wiring können mehrere Features gleichzeitig betreffen.

**Priorität:** P1.

**Empfehlung:** Kein großer Refactor. Nur bei konkreten Änderungen kleine, verhaltensneutrale Lifecycle-Extraktionen vornehmen.

---

## 6. Persistenz — kritischster Befund

### S-01 — Automatischer MySQL→YAML-Fallback

`PlayerProfileManager` versucht bei konfiguriertem MySQL das Datenbank-Repository zu initialisieren. Bei einem Initialisierungsfehler wird auf YAML umgeschaltet und der Plugin-Betrieb fortgesetzt.

Technisch bedeutet das:

1. Konfiguration sagt MySQL.
2. MySQL-Initialisierung scheitert.
3. Runtime-Backend wird auf YAML geändert.
4. Das Plugin läuft mit einem anderen Persistenzsystem weiter.

Das ist unabhängig vom konkreten Produktmodell riskant. Ein leerer, alter oder anderweitig abweichender YAML-Stand kann dadurch als aktiver Zustand behandelt werden.

Mögliche Folgen:

- widersprüchliche Datenstände
- scheinbarer Fortschrittsverlust
- spätere Überschreibung eines korrekten Datenstands
- schwer nachvollziehbare Recovery-Situationen
- unterschiedliche Datenwahrheiten nach Neustarts

**Schweregrad: P0.**

### Zielverhalten

Bei `storage.type=MYSQL` sollte ein fehlgeschlagenes MySQL-Setup nicht stillschweigend YAML als produktives Backend aktivieren.

Sinnvolle Varianten sind:

- kontrollierter Plugin-Startabbruch, oder
- expliziter read-only/degraded mode, oder
- ein klar als Recovery markierter Offline-/Restore-Prozess.

### S-02 — Emergency YAML Backup

Das vorhandene Emergency-YAML-Backup ist als Recovery-Mechanismus sinnvoll.

Die Rollen sollten strikt getrennt bleiben:

`MySQL = konfiguriertes/autoritäres Backend`  
`Emergency YAML = Recovery-Artefakt`

Ein Backup darf nicht implizit zum zweiten aktiven Backend werden.

**Priorität:** P0 im Zusammenhang mit S-01.

---

## 7. PlayerProfileManager — Concurrency und Revisionen

Die Player-Persistenz enthält mehrere solide Schutzmechanismen:

- `ConcurrentHashMap` für aktive Profile
- per-UUID Save Chains
- coalescing `pendingSaves`
- Snapshot-Erstellung vor Async-Persistierung
- Mutation-/Persistence-Revisions
- definierter Shutdown
- Emergency Backup

### S-03 — Revision Locking

Das MySQL-Repository verwendet die `persistence_revision` mit `SELECT ... FOR UPDATE` und prüft die erwartete Revision vor dem Schreiben.

Damit werden stale writes erkannt und nicht blind akzeptiert.

**Bewertung:** POSITIV.

### S-04 — Save-Coalescing

Mehrere ausstehende Saves desselben Profils werden zusammengeführt. Nach Abschluss wird geprüft, ob zwischenzeitlich erneut Änderungen entstanden sind.

**Bewertung:** POSITIV.

### S-05 — Transaktionale Speicherung

Die relevanten Player-/Quest-/Equipment-/Statistics-Daten werden innerhalb einer JDBC-Transaktion persistiert. Fehler führen zum Rollback.

**Bewertung:** PASS.

### S-06 — Delete/Reinsert von Child-State

Bestimmte Teilbereiche werden beim Speichern gelöscht und anschließend aus dem Snapshot neu geschrieben.

Vorteil:

- einfache Konsistenz
- überschaubare Persistenzlogik

Nachteil:

- mehr SQL-Operationen
- größere Transaktionen
- höhere I/O-/Lock-Kosten

**Priorität:** P1, kein akuter Fehler.

---

## 8. Datenbank-Audit

### DB-01 — HikariCP

Der Einsatz eines dedizierten Connection-Pools mit Timeouts, Poolgröße und Lifetime-Konfiguration ist angemessen.

### DB-02 — Prepared Statements

Die Player-Persistenz verwendet Prepared Statements für dynamische Werte.

**Bewertung:** POSITIV.

### DB-03 — Datenbank-/Host-Validierung

Datenbank- und Hostwerte werden vor dem Aufbau der JDBC-Konfiguration eingeschränkt.

**Bewertung:** POSITIV.

### DB-04 — Default-Konfiguration

`config.yml` enthält Entwicklungswerte wie `root` und `CHANGE_ME`. Es ist kein echtes Produktionspasswort erkennbar.

**Priorität:** P1 Hardening.

### DB-05 — SSL

Der Default-SSL-Modus ist restriktiv gewählt. Weniger sichere Modi sind konfigurierbar und müssen daher für reale Deployments bewusst bewertet werden.

**Priorität:** P1 Deployment-Hardening.

---

## 9. AsyncFileWriter

`AsyncFileWriter` kapselt Dateischreibvorgänge über einen dedizierten Executor und führt Coalescing pro Pfad durch.

### F-01 — Serverthread wird entlastet

Reguläre Dateischreibvorgänge laufen nicht direkt im normalen Gameplay-/Eventthread.

**Bewertung:** POSITIV.

### F-02 — Coalescing

Mehrere ausstehende Writes für denselben Pfad werden zusammengeführt. Das verhindert unnötige Zwischenstände.

### F-03 — Shutdown

Die Implementierung berücksichtigt Queue-Drain und Executor-Shutdown explizit.

### Rest-Risiko

Es gibt keine harte globale Speicher-/Bytebegrenzung für extrem große ausstehende Snapshots.

**Priorität:** P2 unter normalen Datenmengen.

---

## 10. Companion-System

Das Companion-System ist lokal pro Besitzer organisiert und nutzt Wake-/Event-orientierte Verarbeitung statt einer offensichtlichen globalen Companion-Tick-Schleife.

### C-01 — Bewegungslogik ist komplex

`CompanionFollowTask` behandelt unter anderem Teleport, Velocity, Step-Up/Step-Down, Fallzustände und Sonderfälle.

Das ist kein konzeptioneller Fehler, aber ein klarer Regression-Hotspot.

**Priorität:** P1 Testabdeckung.

### C-02 — Thread-Grenzen

Die Runtime-Strukturen sind auf serverthreadbasierte Nutzung ausgelegt. Async-Code darf diese Zustände und Minecraft-Entities nicht unkontrolliert manipulieren.

**Priorität:** P1 als Architekturregel.

---

## 11. Boss-System

Der Boss-Bereich besitzt aktive Boss-Instanzen, Bossbars, Phasen, Angriffsmuster, Schadensbeiträge, Loot und Cleanup.

### BO-01 — Periodische Bosslogik ist fachlich begründet

Phasen und Cooldowns sind zeitabhängige Zustände. Eine periodische Prüfung ist daher nicht automatisch ein Performancefehler.

### BO-02 — Räumliche Verarbeitung

Aktive Boss- und Bossbar-Operationen werden lokalisiert, statt pauschal alle relevanten Spieler/Entities global zu verarbeiten.

### BO-03 — Cleanup-Kosten

Lokale Entity-Suchen können bei hoher Entity-Dichte teuer werden.

**Priorität:** P2, abhängig von tatsächlicher Boss-/Entity-Frequenz.

---

## 12. Combat-System

Der Combat-Bereich trennt Damage-Berechnung, Kontext und Runtime-State.

### CB-01 — Kein offensichtlicher DB-Hotpath

Die zentrale Combat-Verarbeitung basiert auf In-Memory-Zuständen und koppelt einzelne Treffer nicht direkt an eine Datenbankoperation.

**Bewertung:** POSITIV.

### CB-02 — Balancing ist nicht statisch beweisbar

Schadensformeln und Interaktionen können syntaktisch korrekt und fachlich trotzdem falsch sein.

**Empfehlung:** deterministische Tests für Damage-/Mitigation-/Modifier-Ketten.

**Priorität:** P1.

---

## 13. NPC und Dialoge

Die NPC-/Dialog-Struktur trennt Registry, Manager, Verhalten und Interaktion. `DialogueEngine` bündelt Dialogerzeugung und Komponentenaufbau.

### NPC-01 — Native Dialog-API

Die Implementierung basiert auf der aktuellen Paper-Dialog-API.

**Bewertung:** PASS.

### NPC-02 — Input ist nicht gleich Business-Validation

Ein Dialog-Input schützt nicht automatisch vor ungültigen fachlichen Werten. IDs, Mengen, Zahlenbereiche und Berechtigungen müssen am jeweiligen Fachendpunkt validiert werden.

**Priorität:** P1.

### NPC-03 — Quit-/Lifecycle-Rennen

Dialogaktionen müssen auch dann sicher sein, wenn der Spieler zwischen Öffnen und Callback die Welt verlässt oder disconnectet.

**Priorität:** P1 Testfall.

---

## 14. Quest-System

Das Quest-System umfasst Quest-Definitionen, Progress, Repository, Navigation, Inventory-Tracking und Listener.

### Q-01 — Event-/Wake-Richtung

Die Architektur vermeidet an vielen Stellen unnötiges permanentes globales Polling und verarbeitet Zustandsänderungen gezielter.

### Q-02 — Passive Checks

`QuestPassiveCheckTask` ist nicht automatisch falsch: Zeit- oder zustandsabhängige Ziele benötigen unter Umständen periodische Prüfung.

Entscheidend ist die tatsächliche Reichweite und Frequenz der Prüfung.

**Priorität:** P1 Runtime-Verifikation.

---

## 15. Region und Spawn

Die Region-/Spawn-Struktur besitzt Geometry, Manager, Repository, Policy, Transition und Spawn-Service.

### R-01 — Räumliche Adressierung

Spawnpoints und lokale Verarbeitung sind strukturell auf räumliche Bereiche ausgerichtet.

### R-02 — Lifecycle-Komplexität

World-Loaded-State, Entity-Lifecycle, Respawn-Zustände und Spielerreichweite greifen ineinander.

**Empfehlung:** Runtime-Stresstest statt vorschnellem Architekturumbau.

**Priorität:** P1.

---

## 16. Items, Equipment und Economy

Die Item-Schicht trennt Definitionen, Builder/Service, Rarity, Soulbound/Unique-Eigenschaften und Equipment.

### E-01 — Zentraler ItemService

Zentrale Item-Erzeugung reduziert verteilte, inkonsistente Itemkonstruktion.

**Bewertung:** POSITIV.

### E-02 — Minor Units

Persistiertes Geld wird als `BIGINT money_minor_units` geführt. Das vermeidet Floating-Point-Probleme bei Geldbeträgen.

**Bewertung:** POSITIV.

### E-03 — Geldmigration

Die Migration aus einem älteren Geldfeld verwendet eine explizite Rundung auf Minor Units. Reale Bestandsdaten sollten vor einer Migration separat validiert werden.

**Priorität:** P1 bei realer Migration.

---

## 17. Guild, Party und Trade

Diese Systeme besitzen persistente und wirtschaftlich relevante Zustände.

Der Audit behauptet hier **nicht**, dass jede mögliche Kombination paralleler Aktionen bereits formal bewiesen korrekt ist.

### T-01 — Concurrency-Risiko

Besonders zu testen sind:

- parallele Transfers
- parallele Ein-/Auszahlungen
- gleichzeitige Käufe/Verkäufe
- gleichzeitige Änderungen desselben Zustands
- Disconnect unmittelbar nach einer Mutation
- Serverabbruch während einer Mutation

**Priorität:** P1.

---

## 18. API und Services

Die API-Schicht stellt unter anderem Economy, Guild, Item, Party und Statistics bereit. Services werden über den ServiceManager registriert.

### API-01 — Entkopplung

Die Service-Schicht ist grundsätzlich sinnvoll, weil externe Integrationen nicht direkt interne Managerstrukturen benötigen.

### API-02 — Öffentliche Verträge

Die vorhandenen Build-Grenzen gegen statische `Player`-/`Entity`-/`World`-Referenzen unterstützen eine robuste Trennung zwischen API-Vertrag und Live-Serverobjekten.

Für öffentliche APIs sollten weiterhin UUIDs, IDs und immutable Daten bevorzugt werden.

**Priorität:** P1 Architekturregel.

---

## 19. Security-Audit

### SEC-01 — Legacy ChatColor

Keine offensichtliche `ChatColor`-Verwendung im geprüften Repository-Code gefunden.

**Bewertung:** PASS.

### SEC-02 — CraftBukkit

Keine offensichtliche `org.bukkit.craftbukkit`-Verwendung gefunden.

**Bewertung:** PASS.

### SEC-03 — Legacy-NMS

Keine offensichtlichen `net.minecraft.server.v1_XX_RY`-Pakete gefunden.

**Bewertung:** PASS.

### SEC-04 — SQL

Prepared Statements werden für Datenwerte verwendet. Dynamische Konfigurationswerte für DB-Aufbau werden eingeschränkt.

**Bewertung:** gut.

### SEC-05 — Secrets

Im Repository ist im geprüften `config.yml` nur ein Platzhalterpasswort vorhanden. Produktionscredentials dürfen nicht committed werden.

---

## 20. Legacy-/Verifikationsschutz

Die Build-Konfiguration enthält weiterhin Prüfungen für relevante unerwünschte Abhängigkeiten und API-Muster, darunter:

- `ChatColor`
- Bungee-ChatColor
- Legacy-NMS-Muster
- CraftBukkit
- statische Live-Serverreferenzen
- Third-Party-Relocations
- MySQL-JDBC-Service-Descriptor

`check` bindet die projektspezifischen Verifikations-Tasks weiterhin ein.

**Bewertung:** PASS.

---

## 21. Codequalität / Wartbarkeit

Auffällige Komplexität konzentriert sich unter anderem in:

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

Diese Klassen sind nicht automatisch fehlerhaft. Sie sind jedoch besonders regressionsgefährdet, weil viele Verantwortlichkeiten bzw. Zustände zusammenlaufen.

### QL-01 — Kein Big-Bang-Refactor

Die aktuelle Struktur sollte nicht ohne konkreten Anlass komplett umgebaut werden. Kleine, verhaltensneutrale Extraktionen sind sinnvoller.

**Priorität:** P2.

---

## 22. Priorisierte Maßnahmen

### P0 — zuerst

**P0-01: MySQL-Fallback ändern**

Kein automatischer Wechsel von konfiguriertem MySQL auf YAML als aktives Backend.

**P0-02: Persistenz-Fehlerszenarien testen**

Mindestens:

- MySQL nicht erreichbar beim Start
- Timeout während Save
- Connection-Abbruch während Save
- Serverabbruch während Save
- Save direkt nach einer vorherigen Mutation
- stale revision
- Recovery nach einem fehlgeschlagenen Save

### P1 — danach

- Concurrency-Tests für Economy/Guild/Party/Trade
- Dialog-/Command-Input-Fuzzing
- Companion-Lifecycle-Tests
- Region-/Spawn-Stresstest
- Quest-Passive-Check-Messung
- Damage-/Combat-Determinismustests
- Lifecycle-Test jeder Executor-/Task-/Repository-Komponente
- reale DB-Latenz und Pool-Auslastung messen

### P2 — später

- Delta-Upserts für große Child-State-Tabellen
- weitere kleine Lifecycle-Extraktionen
- Observability/Metriken
- Heap-/Allocation-Optimierung nach Messung

---

## 23. Was nicht ohne Auftrag geändert werden sollte

Erhalten bleiben sollten insbesondere:

- Paper 26.2
- Java 25
- `paper-plugin.yml`
- Mojang-Mappings
- Adventure Components
- native Dialog-API
- Shadow-Relocations
- bestehende Build-Verifikationen
- revisionsbasierte Player-Persistenz
- `AsyncFileWriter`
- Wake-/Dirty-State-Architektur
- bestehende Feature-Semantik

Es gibt keinen Grund, funktionierende Systeme nur aufgrund von Architekturästhetik umzubauen.

---

## 24. Grenzen des Audits

Nicht statisch beweisbar sind insbesondere:

1. tatsächliche MSPT/TPS unter Last
2. Langzeit-Speicherverbrauch
3. reale MySQL-Ausfall- und Recovery-Semantik
4. Race Conditions mit externen Plugins
5. tatsächliche Client-/Dialogdarstellung in allen Situationen
6. alle wirtschaftlichen Exploits aus kombinierten Spieleraktionen
7. Paper-Runtime-Verhalten, das nicht aus dem Quellcode ableitbar ist
8. tatsächliche Welt-/Chunk-/Entity-Kosten unter realer Belastung

Diese Punkte müssen durch Runtime-, Integrations- und Lasttests validiert werden.

---

## 25. Schlussurteil

PixelRPG besitzt im aktuellen `test`-Stand eine technisch moderne Grundlage für ein Paper-26.2-RPG-Plugin. Die Architektur enthält bereits mehrere gute Entscheidungen: aktuelle Paper-APIs, native Dialoge, Adventure Components, asynchrone Dateiverarbeitung, revisionsbasierte Persistenz, Service-Schnittstellen und Build-Grenzprüfungen.

Der zentrale technische Risikopunkt ist die Fehlersemantik der Persistenz. Ein konfiguriertes MySQL-Backend sollte bei einem Initialisierungsfehler nicht stillschweigend durch YAML ersetzt werden.

Die weiteren Risiken sind überwiegend klassische Themen für ein wachsendes Minecraft-Plugin: Concurrency, Lifecycle-Komplexität, Runtime-Kosten, fachliche Validierung und fehlende Beweise durch Last-/Integrationstests.

**Finales Urteil:**

> **Solide technische Basis. Kein Anlass für einen großen Architekturumbau. Zuerst Persistenz-Fallback absichern, danach gezielt Concurrency-, Failure-Recovery-, Lifecycle- und Runtime-Tests ergänzen.**

---

## Audit-Evidenz

Dieser Audit basiert auf dem tatsächlich geprüften Repository-Inhalt des Branches `test` und wurde nach der Korrektur ausdrücklich von MMORPG-Annahmen bereinigt.

**Audit-Ende.**
