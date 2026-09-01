# PixelRPG — Forensisches Stabilitäts-, Architektur- und Security-Audit

**Branch:** `test`
**Zielplattform:** Paper 26.x, aktuell Paper 26.2
**Java:** 25
**Mappings:** Mojang
**Stand:** dritter forensischer Stabilisierungslauf

## Urteil

Der Branch ist nach den bisherigen Umbauten deutlich belastbarer. Ein grüner CI-Build bleibt aber ein statisches Gate und beweist weder 50/100-Spieler-TPS noch vollständige Persistenz- und Recovery-Sicherheit.

## Bereits umgesetzt

### PlayerProfile / Persistenz

- Deep-Snapshots vor dem Async-Handoff.
- Kein Live-`PlayerProfile` im Repository-Thread.
- Dirty-State wird bei Änderungen während eines laufenden Saves erneut persistiert.
- Fehler markieren den Live-State wieder als dirty.
- Per-UUID Save-Sequenz.
- Idempotenter Shutdown.
- Equipment- und Quest-State werden gesnapshottet.

### Region

- Welt/Chunk-Index für Region-Lookups.
- Welt/Chunk-Index für explizite Spawnpunkte.
- Region-Regelentscheidungen im `RegionPolicyService`.
- Region-I/O auf eigenem Executor.
- Snapshot vor Async-I/O.
- Atomisches YAML-Schreiben.
- Migration wird nur markiert und außerhalb des Load-Hotpaths persistiert.

### NPC / HTTP / Skins

- NPC-Chunk-Index.
- NPC-Persistenz asynchron.
- PDC-Identität.
- Skin-I/O asynchron.
- URL- und SSRF-Validierung.
- Redirect-/Responsegrößenbegrenzung.
- begrenzter, instanzgebundener Skin-Cache.
- Entity-Mutation auf dem Serverthread.

### Guild / Party

- Runtime-State bleibt serverthreadgebunden.
- Persistenz läuft über eigene sequenzielle I/O-Executors.
- Snapshots vor Dateisystem-I/O.
- Atomische YAML-Writes.
- Kontrollierter Shutdown-Flush.

### Build / Dependency Isolation

- Java 25 Toolchain.
- Paper 26.2 Dev-Bundle.
- Legacy-NMS/CraftBukkit/ChatColor Source-Gates.
- Gson, HikariCP und MySQL werden im Shadow-JAR relocated.
- `mergeServiceFiles()` für JDBC-Service-Provider.
- CI prüft jetzt zusätzlich das erzeugte Shadow-JAR auf unrelocierte Drittanbieter-Klassen.
- CI prüft `META-INF/services/java.sql.Driver` auf einen tatsächlich relocatierten MySQL-Treiber und verhindert den alten `com.mysql.cj.jdbc.Driver`-Eintrag.

## Neue/noch offene Befunde

### P0/P1

1. **PlayerProfile Save-Revision:** DB-seitige monotone Revision fehlt weiterhin. Die Prozess-interne UUID-Sequenz verhindert parallele Rückwärts-Saves, schützt aber nicht gegen externe Prozesse oder Recovery-Replays.
2. **CompanionService:** `load()` und `CompanionEquipmentStore` führen weiterhin synchrone YAML-Lese-/Schreiboperationen aus Gameplay-Aufrufen aus. Das bleibt ein harter Runtime-I/O-Punkt.
3. **CompanionService Shutdown:** Companion-Persistenz verwendet weiterhin den synchronen Save-Pfad.
4. **Companion Domain Ownership:** mutable Companion-Listen sind noch nicht über ein klares Actor-/Snapshot-Modell gekapselt.
5. **GuildManager:** statischer Singleton bleibt bestehen und erschwert Isolation und Tests.
6. **Quest/Shop/Boss/Combat:** vollständige Hotpath-Analyse muss weiterhin mit konkreten Runtime-Profilen verifiziert werden.
7. **Economy:** `double` als Geldmodell ist weiterhin vorhanden. Langfristig `long` in kleinster Währungseinheit.
8. **RPGItemBuilder:** globale mutable Scaling-Konfiguration bleibt bestehen.
9. **Quest-Text:** vollständige Single-Source-of-Truth für Displaynamen ist noch nicht garantiert.
10. **Rezept-IDs:** vollständige Kanonisierung muss weiterhin durchgesetzt werden.
11. **Paper Dev Bundle:** `26.2.build.+` ist für Entwicklung zulässig, für reproduzierbare Releases aber zu breit. Release-Builds müssen pinnen.
12. **Lasttest:** keine echte 50-/100-Spieler-Verifikation durch CI.

## Threading-Vertrag

- Paper-/Minecraft-Live-API nur auf dem vorgesehenen Serverthread.
- DB-I/O niemals Main Thread.
- HTTP-I/O niemals Main Thread.
- schweres Filesystem-I/O niemals Main Thread.
- mutable Domain-State nicht zwischen Main- und I/O-Threads teilen.
- Async Persistenz erhält Snapshots.
- Executor besitzt eindeutigen Owner und wird beim Shutdown beendet.
- Async Tasks dürfen keine langlebigen Live-Entity-Referenzen halten.

## Persistenz-Vertrag

Jeder persistente Runtime-State benötigt:

```text
Main-thread mutation
        ↓
immutable snapshot
        ↓
ordered persistence queue
        ↓
atomic repository write
```

Für PlayerProfile muss zusätzlich eine persistente Revision eingeführt werden:

```text
revision 41
revision 42
revision 43
```

Ein Save mit Revision 42 darf niemals einen bereits gespeicherten Zustand 43 überschreiben.

## Cache-Vertrag

Jeder langlebige Cache braucht:

- maximale Größe,
- TTL oder andere definierte Eviction,
- eindeutigen Owner,
- Shutdown-Cleanup.

## Listener-Vertrag

Listener dürfen keine vollständige Business-Logik enthalten.

```text
Paper Event
   ↓
Adapter
   ↓
Use Case / Domain Service
   ↓
Result
   ↓
Paper side effect
```

Region ist in diesem Punkt verbessert. Dieselbe Trennung muss für Combat, Quest, NPC, Boss und Inventory konsequent weitergeführt werden.

## Economy-Vertrag

- Server ist autoritativ.
- Clientdaten bestimmen niemals Preis oder Kontostand.
- Geldmutation und Itemmutation müssen gegen doppelte Ausführung geschützt sein.
- Kaufen/Verkaufen/Bank/Guild müssen bei Fehlern atomar sein.
- `double` ist für dauerhaftes Geldmodell zu vermeiden.

## Build-/Runtime-Gate

Ein grüner CI-Build bedeutet:

- Java-25-Kompilierung erfolgreich.
- Paper-Dev-Bundle auflösbar.
- Source-Boundary-Regeln eingehalten.
- Shadow-JAR-Relocation geprüft.
- JDBC-Service-Descriptor geprüft.

Er bedeutet **nicht** automatisch:

- Paper-Server startet mit dem Fat-JAR.
- MySQL/Hikari verbindet sich real.
- `SELECT 1` funktioniert.
- Join/Quit/Rejoin verliert keine Daten.
- 100 Spieler verursachen keine kritischen Tick-Spikes.

## Release-Gate

Production-Ready erst nach Nachweis von:

- Java-25 Clean Build.
- Fat-JAR startet auf Paper 26.2.
- MySQL/Hikari startet und `SELECT 1` funktioniert.
- Join → Load → Mutation → Save → Quit → Reload ohne Datenverlust.
- konkurrierende Saves können nicht rückwärts schreiben.
- keine DB/HTTP/Filesystem-Blockierung im Main-Thread-Hotpath.
- keine unbounded Caches.
- alle Executor/HTTP/DB-Lifecycle sauber beendet.
- 50–100-Spieler-Lasttest ohne kritische Tick-Spikes.

## Nächster Codeblock

1. Companion-Persistenz vollständig aus dem Gameplay-Thread entfernen.
2. PlayerProfile persistente Revision einführen.
3. Economy auf atomare Mutation + Ganzzahl-Währung umstellen.
4. Quest/Shop/Boss/Combat-Hotpaths erneut prüfen.
5. Release-Dependency-Versionen exakt pinnen.
6. Erst danach realen 50-/100-Spieler-Lasttest durchführen.
