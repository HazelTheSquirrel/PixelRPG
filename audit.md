# PixelRPG — Stabilitäts-, Architektur- und Security-Audit

**Branch:** `test`
**Zielplattform:** Paper 26.x, aktuell Paper 26.2
**Java:** 25
**Mappings:** Mojang
**Status:** Stabilisierungslauf 1 abgeschlossen; CI-Gate läuft nach dem letzten Commit

## Ziel

Der funktionierende Gameplay-Zustand bleibt die Referenz. Stabilisierung bedeutet: Datenintegrität, Thread-Sicherheit, Lifecycle, Performance und Security verbessern, ohne funktionierende Systeme unnötig neu zu erfinden.

## Stabilisierungslauf 1 — umgesetzt

### PlayerProfile / Persistenz

- Persistenz arbeitet mit synchron erzeugten Deep-Snapshots statt einer gleichzeitig serialisierten Live-Instanz.
- Snapshots werden vor dem Übergang auf den I/O-Executor erzeugt.
- Dirty-State bleibt bei Änderungen während eines Saves erhalten.
- Fehlgeschlagene Storage-Saves markieren den Live-State erneut als dirty.
- Per-UUID-Save-Queues verhindern parallele Saves desselben Spielers.
- Load- und Save-Operationen derselben UUID laufen über dieselbe Sequenz.
- Shutdown ist idempotent.
- Emergency-Repositories werden geschlossen.
- Equipment und Quest-State werden im Snapshot berücksichtigt.

**Verbleibend:** Eine explizite persistente Save-Revision ist noch nicht als DB-Spalte eingeführt. Die aktuelle per-UUID-Sequenz verhindert parallele Rückwärts-Saves innerhalb dieses Prozesses; eine DB-seitige Revision ist für Multi-Process-/Recovery-Szenarien weiterhin ein P1/P0-Hardening.

### Region-System

- Regionabfragen verwenden einen Welt/Chunk-Index.
- Explizite Spawnpunkte verwenden einen eigenen Welt/Chunk-Index und keinen globalen Region-Scan.
- Region-Regelentscheidungen liegen im `RegionPolicyService` statt im Listener.
- Region-Persistenz läuft über einen eigenen I/O-Executor.
- Region-Snapshots werden vor Async-I/O erzeugt.
- Region-YAML wird atomar über temporäre Datei + Move geschrieben.
- Formatmigrationen werden beim Laden nur markiert und anschließend über den asynchronen Persistenzpfad gespeichert; Migration verursacht dadurch kein direktes YAML-Schreiben im Load-Hotpath.
- Region-Persistenz besitzt einen kontrollierten Executor-Lifecycle.

### NPC-System

- NPC-Persistenz läuft asynchron.
- NPC-Saves verwenden temporäre Dateien und atomaren Move, soweit verfügbar.
- NPC-Entities werden per PDC identifiziert.
- NPC-Chunk-Load verarbeitet nur NPCs des betroffenen Chunks statt alle NPCs der Welt zu scannen.
- NPC-Chunk-Index wird beim Load/Create/Delete gepflegt.
- Shutdown ist idempotent.

### Externe Skins / HTTP

- Skin-Download läuft asynchron.
- Externe URLs werden validiert.
- HTTPS ist für externe Quellen erforderlich.
- Redirects werden kontrolliert.
- Loopback/private/link-local/multicast Ziele werden blockiert.
- Responsegrößen sind begrenzt.
- Skin-Cache besitzt TTL und Größenbegrenzung.
- Skin-Cache ist nicht mehr JVM-global statisch.
- Paper-Mannequin-Mutation bleibt auf dem Serverthread.

### Datenbank / JDBC

- DB-I/O ist aus den normalen Gameplay-Hotpaths herauszuhalten.
- Connection/Statement/ResultSet werden über Try-with-Resources behandelt.
- Prepared Statements sind der verbindliche SQL-Weg.
- `autoReconnect` wird nicht als Fehlerbehandlung verwendet.
- TLS wird nicht pauschal deaktiviert; die Konfiguration besitzt einen expliziten SSL-Modus.
- JDBC-Service-Discovery wird beim Shading berücksichtigt.

### Build / ShadowJar / CI

- Java 25 Toolchain und `--release 25`.
- Paper 26.2 Dev Bundle als aktuelle Entwicklungsbasis.
- Gson wird korrekt über `com.google.gson` relocated; interne Gson-Nutzung wird nicht fälschlich als API-Verstoß gewertet.
- HikariCP und MySQL werden relocated.
- JDBC-Service-Dateien werden gemerged.
- Signaturdateien werden aus dem Fat-JAR entfernt.
- Source-Boundary-Checks verbieten Legacy-NMS, CraftBukkit, ChatColor und statische Live-Paper-Referenzen.
- CI prüft Source-Boundaries und das erzeugte Artifact.

### Paper/API

- Adventure Components statt `ChatColor`.
- PDC bleibt die primäre Plugin-Metadatenebene für RPG-Items.
- Keine Legacy-NMS-Pakete.
- Keine CraftBukkit-Namen.
- Keine alten 1.21.x-Implementierungen.
- Native Paper-Dialogmechanik bleibt die Zielarchitektur.

## Noch offen nach Stabilisierungslauf 1

### P0/P1 — nächste harte Runde

1. Persistente Save-Revision für PlayerProfile einführen und DB-seitig gegen Rückwärts-Saves absichern.
2. Join/Quit/Rejoin/Shutdown-Concurrency mit automatisierten Tests abdecken.
3. Alle Companion-, Guild-, Trade- und Bank-Persistenzpfade auf Main-Thread-I/O und Lifecycle prüfen.
4. Alle verbleibenden synchronen Repository-/YAML-Aufrufe im Runtime-Code erfassen und entfernen.
5. Companion-Runtime-Maps auf Lifecycle und Größenwachstum prüfen.
6. Companion-Follow-/Combat-/Look-Ticks auf unnötige Entity-/Player-Arbeit reduzieren.
7. Boss-Manager und Boss-Tasks auf globale Entity-Scans und Tick-Budget prüfen.
8. Quest-Manager auf globale Vollscans bei Kill-/Inventory-/Passive-Events prüfen.
9. Scoreboard-Guild-Update auf vermeidbare Spieler×Spieler-Arbeit prüfen.
10. Economy-Operationen logisch atomar machen.
11. Geldmodell langfristig von `double` auf kleinste Währungseinheit (`long`) migrieren.
12. MySQL-Schema versionieren und Migrationen nicht mehr über ad-hoc DDL erweitern.
13. `PixelRPGPlugin` schrittweise weiter zum reinen Composition Root reduzieren.
14. `RPGItemBuilder` von global mutable Scaling-Statik auf immutable `ItemScalingConfig` umstellen.
15. Quest-Text vollständig auf eine einzige `DisplayNameService`-Quelle umstellen.
16. Rezept-IDs vollständig kanonisieren.
17. Paper-Dev-Bundle für Releases auf einen exakten Build pinnen statt `26.2.build.+`.
18. Reale 50-/100-Spieler-Lasttests durchführen; CI-Kompilierung allein beweist keine TPS-Sicherheit.

## Architektur-Soll

```text
Paper Listener / Command / GUI
            ↓
Application / Use Case
            ↓
Domain State / Domain Service
            ↓
Repository Interface
            ↓
Infrastructure
```

Listener extrahieren Eventdaten, rufen einen Use Case auf und wenden dessen Ergebnis an. Business-Regeln gehören in Services/Domain-Logik.

## Threading-Vertrag

- Paper-/Minecraft-Live-API nur auf dem vorgesehenen Serverthread.
- DB-I/O niemals Main Thread.
- HTTP-I/O niemals Main Thread.
- Schweres Filesystem-I/O niemals Main Thread.
- Mutable Domain-State nicht zwischen Main- und I/O-Threads teilen.
- Async Persistenz erhält immutable Snapshots.
- Async Tasks halten keine langlebigen Live-Entity-Referenzen.
- Jeder Executor besitzt einen eindeutigen Owner und wird beim Shutdown beendet.

## Cache-Vertrag

Jeder langlebige Cache braucht mindestens:

- maximale Größe,
- TTL oder definierte Eviction,
- eindeutigen Owner,
- Cleanup beim Shutdown.

## Economy-Vertrag

Ankauf/Verkauf und Bank-/Guild-Operationen müssen serverautoritativ sein. Clientdaten dürfen weder Preis noch Kontostand bestimmen. Item- und Geldmutation müssen gegen doppelte Ausführung und Teiltransaktionen geschützt werden.

## Release-Gate

Production-Ready ist erst erreicht, wenn zusätzlich zum grünen Build nachgewiesen ist:

- Java-25 Clean Build erfolgreich
- Fat-JAR startet auf Paper 26.2
- MySQL/Hikari startet und `SELECT 1` funktioniert
- Join → Load → Mutation → Save → Quit → Reload ohne Datenverlust
- konkurrierende Saves überschreiben sich nicht rückwärts
- keine DB/HTTP/Filesystem-Blockierung im Main-Thread-Hotpath
- keine unbounded Caches
- alle Executor/HTTP/DB-Lifecycle sauber beendet
- 50–100 Spieler Lasttest ohne kritische Tick-Spikes

**Grundsatz:** Ein grüner CI-Build beweist Kompilierbarkeit. Er beweist nicht automatisch Datenintegrität oder TPS-Stabilität. Diese beiden Dinge werden in der nächsten Runde separat verifiziert.
