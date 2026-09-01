# PixelRPG — Stabilitäts-, Architektur- und Security-Audit

**Branch:** `test`  
**Zielplattform:** Paper 26.x, aktuell Paper 26.2  
**Java:** 25  
**Mappings:** Mojang  
**Status:** laufende Stabilisierung für 50–100+ Spieler

## Ziel

Der funktionierende Gameplay-Zustand ist die Referenz. Stabilisierung bedeutet: Datenintegrität, Thread-Sicherheit, Lifecycle, Performance und Security verbessern, ohne funktionierende Systeme unnötig neu zu erfinden.

## Prioritäten

- **P0:** Datenverlust, Race Conditions, Main-Thread-I/O, beschädigte Persistenz
- **P1:** Memory-Leaks, Executor-Lifecycle, Security, Hotpath-Skalierung
- **P2:** Architektur, Testbarkeit, Dependency-Reproduzierbarkeit
- **P3:** Komfort und kosmetisches Refactoring

## Bereits gehärtet

### PlayerProfile

- Persistenz arbeitet mit Snapshots statt einer gleichzeitig mutierten Live-Instanz.
- Dirty-State bleibt bei Änderungen während eines Saves erhalten.
- Equipment und Quest-State werden beim Snapshot berücksichtigt.
- Aktive Profile und zugehörige Services werden beim Shutdown bereinigt.

### Region

- Region-Persistenz wurde aus dem normalen Main-Thread-Laufzeitpfad herausgezogen.
- Region-Snapshots werden vor Async-I/O erstellt.
- Spawnpunkte werden räumlich über Welt/Chunk indiziert statt global durchsucht.
- Region-I/O besitzt einen definierten Executor-Lifecycle.

### NPC

- YAML-Persistenz läuft über einen eigenen I/O-Executor.
- Saves werden über temporäre Datei und Move robuster ausgeführt.
- NPC-Lifecycle wird beim Shutdown beendet.

### Externe Skins / HTTP

- Responsegröße ist begrenzt.
- Externe URLs werden validiert.
- HTTPS ist für externe Quellen erforderlich.
- Redirects werden nicht blind verfolgt.
- Loopback, private, link-local und multicast Ziele werden blockiert.
- Cache besitzt TTL und Größenbegrenzung.
- Externe Response-Bodies werden nicht vollständig geloggt.

### Datenbank / JDBC

- DB-Zugriffe gehören ausschließlich in I/O-Pfade.
- Try-with-Resources ist für Connection/Statement/ResultSet Pflicht.
- Prepared Statements sind Pflicht.
- TLS darf nicht pauschal deaktiviert werden.
- `autoReconnect` ist kein Ersatz für sauberes Connection-Lifecycle-Handling.
- Shaded JDBC-Driver-Discovery wurde explizit gehärtet.

### ShadowJar

- Service-Dateien werden beim Shading berücksichtigt.
- Signaturdateien werden aus dem Fat-JAR ausgeschlossen.
- Gson/Hikari/MySQL-Relocation bleibt eine bewusst zu testende Runtime-Grenze.

### Quest-Navigation

- Teure World-Navigation wird gecacht und darf nicht unkontrolliert pro Event wiederholt werden.
- Cache-Lifecycle wird berücksichtigt.

### Plugin-Lifecycle

- Service-Registrierungen werden beim Shutdown nur dann deregistriert, wenn die jeweilige Instanz tatsächlich initialisiert wurde.
- Service-Deregistrierung ist aus `onDisable()` in eine dedizierte Lifecycle-Methode ausgelagert.
- Die globale Plugin-Referenz wird beim Disable weiterhin aktiv entfernt.

## Noch zu erledigen — P0

1. Jede Profilmutation muss auf dem vorgesehenen Serverthread erfolgen.
2. Snapshot-Erzeugung braucht eine monotone Revision.
3. Ein älterer Snapshot darf niemals einen neueren Persistenzstand überschreiben.
4. Join/Quit/Shutdown müssen gegen gleichzeitige Load/Save-Rennen abgesichert werden.
5. Jeder Repository-Pfad muss garantiert Connection, Statement und ResultSet schließen.
6. Kein synchrones DB-, HTTP- oder schweres Filesystem-I/O in Event-/Tick-Hotpaths.

## Noch zu erledigen — P1

1. Alle Scheduler, Executor und HTTP-Clients benötigen einen eindeutigen Owner und Shutdown.
2. Keine dauerhaften `Player`, `Entity`, `World` oder vergleichbaren Live-Objekte in Async-Tasks/Caches.
3. UUID-basierte temporäre Zustände beim Quit freigeben.
4. Jeder Cache benötigt TTL, Größenlimit oder explizite Eviction.
5. Keine globalen Regionscans in Hotpaths.
6. Keine Vollscans aller Quests pro Event.
7. Keine unbounded Entity-/Boss-Scans pro Tick.
8. Teure Biome-/Structure-Suchen cachen oder eventgetrieben auslösen.
9. HTTP connect/read/write timeouts und maximale Responsegrößen zentral erzwingen.
10. SSRF-Schutz gegen Redirect- und DNS-Rebinding-Umgehungen weiter prüfen.
11. Economy-Operationen serverautoritativ und logisch atomar durchführen.
12. `PixelRPGPlugin` weiter zum Composition Root reduzieren, ohne funktionierende Registrierung zu brechen.

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

`PixelRPGPlugin` darf Composition Root bleiben, soll aber nicht selbst zum Business-Service werden. Listener sollen Eventdaten extrahieren, einen Use Case aufrufen und dessen Ergebnis anwenden.

## Threading-Vertrag

- Paper-/Minecraft-Live-API nur auf dem dafür vorgesehenen Serverthread.
- DB-I/O niemals Main Thread.
- HTTP-I/O niemals Main Thread.
- Schweres Filesystem-I/O niemals Main Thread.
- Mutable Domain-State nicht zwischen Main und I/O teilen.
- Async Persistenz erhält immutable Snapshots.
- Async Code hält keine dauerhaften Live-Entity-Referenzen.

## Item-Vertrag

`PersistentDataContainer` bleibt die primäre Plugin-Metadatenebene für RPG-Items. Technische IDs und Anzeige-Namen sind getrennt. Item-ID, Rezept-ID und Display-Name benötigen eine eindeutige Source of Truth. Rohschlüssel wie `Scholar:book` dürfen nicht als sichtbarer Fallback erscheinen.

Keine `ChatColor`- oder Legacy-NMS-Lösungen. Adventure Components verwenden; MiniMessage nur für komplexe Formatierung.

## Quest-Vertrag

Quest-ID ist technische Identität. Display-Name ist Präsentation. Ziele und Fortschritt sind strukturierte Daten. Häufige Events dürfen nicht durch unnötige globale Quest-Vollscans skaliert werden.

## Region-Vertrag

Regionabfragen benötigen räumliche Indizes. Create/Load/Delete/Save müssen Index und Persistenz konsistent halten. Hotpaths dürfen nicht mit der Gesamtzahl aller Regionen wachsen.

## NPC/Manequin-Vertrag

Skin-Download ist externe I/O und muss asynchron erfolgen. URL, Responsegröße, Redirects, Timeouts und Cache-Lifecycle müssen kontrolliert werden. NPC-Persistenz darf den Serverthread nicht blockieren.

## Economy/Shop-Vertrag

Ankauf und Verkauf müssen serverautoritativ sein. Clientwerte dürfen nicht als Preis oder Kontostand vertraut werden. Itemtransfer und Geldtransfer müssen logisch atomar sein.

## Boss-Vertrag

Biome-Boss-Spawning benötigt Cooldowns, Spawnbedingungen, Welt-/Chunk-Limits und keine unbounded globale Suche.

## Build / Release

`paperDevBundle("26.2.build.+")` ist für laufende Entwicklung akzeptabel, aber kein reproduzierbarer Release-Pin. Vor einem Release muss ein exakter Dev-Bundle-Build festgelegt werden.

Release-Gate:

- Java-25 Clean Build erfolgreich
- Fat-JAR startet auf Ziel-Paper
- `paper-plugin.yml` vorhanden und korrekt
- Hikari/MySQL initialisiert und `SELECT 1` erfolgreich
- Join → Load → Mutation → Save → Quit → Reload ohne Datenverlust
- konkurrierende Saves überschreiben sich nicht rückwärts
- keine DB/HTTP/Filesystem-Hotpath-Blockierung des Main Threads
- keine unbegrenzten Caches
- Executor/HTTP/DB-Lifecycle beim Shutdown vollständig beendet
- keine `ChatColor`
- keine CraftBukkit-Namen
- keine `net.minecraft.server.v1_*`-Pakete
- keine alten 1.21.x-Mechaniken

## Bewusst keine pauschalen Verbote

Ein großer Composition Root ist nicht automatisch ein Produktionsfehler. Ein Cache ist kein Leak, wenn er begrenzt und lifecycle-sicher ist. NMS ist nicht pauschal verboten, wenn Paper 26.x keine passende API besitzt. Funktionierende Gameplay-Logik wird nicht nur aus Stilgründen neu geschrieben.

## Definition of Done

Die Stabilitätsphase ist abgeschlossen, wenn die kritischen P0/P1-Invarianten im tatsächlichen Code verifiziert und durch Tests oder reproduzierbare Lastszenarien abgesichert sind. Erst danach sollte der Fokus wieder vollständig auf neue Inhalte wie NPC/Manequins, weitere Berufe und Rezepte, Questketten, Shop-Ankauf/Verkauf, Economy-Balancing, Biome-Boss-Spawning und Region-Erweiterungen wechseln.

**Grundsatz:** Nicht möglichst viel Code ändern. Möglichst viele reale Fehlerklassen eliminieren, während das gewünschte Endprodukt erhalten bleibt.
