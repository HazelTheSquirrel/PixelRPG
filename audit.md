# PixelRPG — Forensisches Stabilitäts-, Architektur- und Security-Audit

**Branch:** `test`
**Zielplattform:** Paper 26.x, aktuell Paper 26.2
**Java:** 25
**Mappings:** Mojang
**Stand:** vollständiger Stabilisierungslauf — Implementierungsdurchlauf 1 abgeschlossen

## Urteil

Der Branch ist nach dem Stabilisierungslauf deutlich belastbarer und die CI-Gates sind grün. Der Code ist damit in einem guten Zustand für die nächste Testphase, aber ein grüner Build beweist weiterhin keine reale 50-/100-Spieler-Last oder externe Datenbank-Recovery.

**Wichtig:** Dieses Dokument trennt bewusst zwischen bereits technisch umgesetzten Maßnahmen und Punkten, die nur durch einen echten Laufzeittest bzw. Produktionsähnliche Infrastruktur verifiziert werden können.

---

# 1. Umgesetzte Stabilisierung

## PlayerProfile / Persistenz

- Deep-Snapshots vor Async-Handoffs.
- Kein Live-`PlayerProfile` im Repository-Thread.
- Dirty-State wird bei Änderungen während eines laufenden Saves erneut persistiert.
- Fehler markieren den Live-State wieder als dirty.
- Per-UUID Save-Sequenz.
- Idempotenter Shutdown.
- Equipment- und Quest-State werden gesnapshottet.

### Noch offen

- Persistente DB-Revision fehlt. Die Prozess-interne Save-Reihenfolge schützt nicht gegen externe Prozesse, Restore-Replays oder mehrere Serverinstanzen.

---

## Region

- Welt-/Chunk-Index für Region-Lookups.
- Welt-/Chunk-Index für explizite Spawnpunkte.
- Region-Regelentscheidungen im `RegionPolicyService`.
- Region-I/O auf eigenem Executor.
- Snapshot vor Async-I/O.
- Atomisches YAML-Schreiben.
- Migration wird nur markiert und außerhalb des Load-Hotpaths persistiert.

### Ergebnis

Der ursprüngliche lineare Region-Scan im Spawn-/Lookup-Hotpath ist architektonisch beseitigt. Weitere Lastmessungen sind trotzdem erforderlich.

---

## NPC / HTTP / Skins

- NPC-Chunk-Index.
- NPC-Persistenz asynchron.
- PDC-Identität.
- Skin-I/O asynchron.
- URL- und SSRF-Validierung.
- Redirects werden nicht automatisch verfolgt.
- Response-Größenlimit.
- Begrenzter instanzgebundener Skin-Cache mit TTL und LRU-artiger Eviction.
- Entity-Mutation erfolgt auf dem Serverthread.

### Noch offen

DNS-Rebinding ist bei externen HTTP-Zielen grundsätzlich ein gesondertes Infrastrukturthema. Für maximale Sicherheit sollte die HTTP-Schicht langfristig einen kontrollierten Resolver/Outbound-Proxy verwenden, statt sich ausschließlich auf eine vorgelagerte DNS-Prüfung zu verlassen.

---

## Guild / Party

- Runtime-State bleibt serverthreadgebunden.
- Persistenz über eigene sequenzielle I/O-Executors.
- Snapshots vor Dateisystem-I/O.
- Atomische YAML-Writes.
- Kontrollierter Shutdown-Flush.
- Singleton-/Lifecycle-Zustände werden beim Shutdown zurückgesetzt, soweit vorhanden.

---

## Companion

Der Companion-Bereich wurde in diesem Durchlauf weiter stabilisiert.

- Companion-YAML-Saves laufen jetzt über einen eigenen sequenziellen `PixelRPG-CompanionIO` Executor.
- Gespeichert werden detached `List.copyOf(...)`-Snapshots statt mutable Runtime-Listen.
- Companion-Equipment wird über einen eigenen `PixelRPG-CompanionEquipmentIO` Executor geschrieben.
- Beide Executor werden beim Shutdown kontrolliert beendet und warten auf ausstehende Writes.
- Runtime-Entity-Mutationen bleiben auf dem Serverthread.
- Equipment-Cache bleibt runtimegebunden und wird beim Service-Shutdown verworfen.

### Noch offen

- `CompanionService.load()` liest beim ersten Zugriff weiterhin synchron YAML. Das ist kein periodischer Hotpath, kann bei sehr großen Dateien aber Join-/GUI-Latenz verursachen. Der nächste sinnvolle Schritt ist ein asynchroner Player-Load mit Main-Thread-Handoff.
- Companion-Listen sind weiterhin mutable Collections hinter einer Service-Fassade. Ein echtes Actor-/Mailbox-Modell wäre die stärkere Langzeitarchitektur.

---

# 2. Build / Dependency Isolation

- Java 25 Toolchain.
- Paper 26.2 Dev-Bundle.
- Legacy-NMS/CraftBukkit/ChatColor Source-Gates.
- Gson, HikariCP und MySQL werden im Shadow-JAR relocated.
- `mergeServiceFiles()` für JDBC-Service-Provider.
- CI prüft erzeugte Shadow-JARs auf unrelocierte Drittanbieter-Klassen.
- CI prüft `META-INF/services/java.sql.Driver` auf den relocatierten MySQL-Treiber.
- CI verhindert den alten `com.mysql.cj.jdbc.Driver` Service-Eintrag.
- Build-Gate verwendet die tatsächliche `Set<File>`-Semantik des Gradle-ZipTree-Ergebnisses; kein ungültiger `.singleFile`-Zugriff mehr.

### Noch offen

`paperweight.paperDevBundle("26.2.build.+")` ist nicht reproduzierbar genug für Release-Builds. Für einen echten Release muss ein exakter Dev-Bundle-Build festgeschrieben werden.

---

# 3. Threading-Vertrag

Verbindliche Regeln für den Branch:

- Paper-/Minecraft-Live-API nur auf dem vorgesehenen Serverthread.
- DB-I/O niemals Main Thread.
- HTTP-I/O niemals Main Thread.
- schweres Filesystem-I/O niemals Main Thread.
- mutable Domain-State nicht zwischen Main- und I/O-Threads teilen.
- Async Persistenz erhält Snapshots.
- Jeder Executor besitzt einen eindeutigen Owner.
- Jeder Executor wird beim Shutdown beendet.
- Async Tasks dürfen keine langlebigen Live-Entity-Referenzen halten.

Der Companion-Stabilisierungslauf erfüllt jetzt zusätzlich den Filesystem-Teil für Companion-Saves.

---

# 4. Persistenz-Vertrag

Zielmodell:

```text
Main-thread mutation
        ↓
immutable snapshot
        ↓
ordered persistence queue
        ↓
atomic repository write
```

Für PlayerProfile zusätzlich:

```text
revision 41
revision 42
revision 43
```

Ein Save mit Revision 42 darf niemals einen bereits gespeicherten Zustand 43 überschreiben.

---

# 5. Listener-Vertrag

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

Region wurde entsprechend aufgeteilt. Combat, Quest, NPC, Boss und Inventory müssen bei weiteren Durchläufen weiterhin auf versteckte Business-Logik geprüft werden.

---

# 6. Economy-Vertrag

- Server ist autoritativ.
- Clientdaten bestimmen niemals Preis oder Kontostand.
- Geldmutation und Itemmutation müssen gegen doppelte Ausführung geschützt sein.
- Kaufen/Verkaufen/Bank/Guild müssen bei Fehlern atomar sein.
- `double` ist für ein dauerhaftes Geldmodell zu vermeiden.

## Offener technischer Punkt

Die Economy benötigt weiterhin eine endgültige Prüfung auf Ganzzahl-Währung und atomare Transaktionen. Das wird nicht durch einen erfolgreichen Build bewiesen.

---

# 7. Hotpath- und Skalierungsprüfung

## Bereits entschärft

- Region-Spawn-Lookups wurden indexiert.
- NPC-Lookups wurden indexiert.
- Region-/NPC-/Companion-Persistenz wurde aus kritischen Runtime-Schreibpfaden herausgezogen.
- HTTP-Skin-Auflösung blockiert den Serverthread nicht.
- Skin-Cache ist begrenzt.

## Noch zu profilieren

- Boss-Tick-Logik und Entity-Scans.
- Combat-/Skill-Hotpaths.
- Quest-Vollscans und Objective-Auswertung.
- Shop-/Trade-/Bank-GUIs.
- Companion-Follow-Tick.
- Scoreboard-Updates.
- World-/Biome-Boss-Spawning.

Ohne Spark-/Timings-Daten wäre jede Aussage über 100-Spieler-TPS an dieser Stelle Spekulation.

---

# 8. Cache-Vertrag

Jeder langlebige Cache benötigt:

- maximale Größe,
- TTL oder definierte Eviction,
- eindeutigen Owner,
- Shutdown-Cleanup.

Der External-Skin-Cache erfüllt diese Anforderungen.

Andere Cache-Strukturen müssen bei einem weiteren Repository-weiten Durchlauf auf diese Kriterien geprüft werden.

---

# 9. Security-Vertrag

Verbindlich:

- Keine `ChatColor`-/Legacy-Farbcodes.
- Keine Legacy-CraftBukkit-/Versioned-NMS-Pakete.
- PDC für RPG-Identität statt frei erfundener NBT-Abhängigkeiten.
- Prepared Statements für SQL.
- Keine unkontrollierten SQL-Strings aus Userdaten.
- HTTP nur mit explizitem Timeout.
- Keine automatischen Redirects für untrusted Skin-URLs.
- Response-Größen begrenzen.
- Externe URL-Ziele gegen private/reservierte Netze prüfen.
- Keine Secrets in Git.

---

# 10. Vollständiger Durchlauf — Statusmatrix

| Phase | Bereich | Status | Aussage |
|---|---|---:|---|
| 1 | Architektur / Package-Schnitt | 🟡 | Struktur verbessert; God-Object bleibt als Composition Root bestehen |
| 2 | Lifecycle / Shutdown | 🟢 | zentrale Shutdown-Aufrufe und Executor-Cleanup vorhanden |
| 3 | PlayerProfile | 🟡 | Snapshot-Persistenz umgesetzt; persistente Revision offen |
| 4 | Region | 🟢 | Index + Policy + Async/Atomic Persistence umgesetzt |
| 5 | NPC | 🟢 | Index + Async Persistence + Lifecycle-Härtung umgesetzt |
| 6 | External Skin / HTTP | 🟢 | Async + Limits + SSRF-Basis + bounded cache |
| 7 | Guild | 🟢 | sequenzielle Async-Persistenz + Flush |
| 8 | Party | 🟢 | sequenzielle Async-Persistenz + Flush |
| 9 | Companion | 🟡 | Saves async; initiales Laden noch synchron |
| 10 | Build / ShadowJar | 🟢 | Relocation- und JDBC-Service-Gates aktiv |
| 11 | Listener SoC | 🟡 | Region verbessert; weitere Domänen noch zu entkoppeln |
| 12 | Economy | 🟡 | Architekturprüfung offen; `double`/Atomizität weiter prüfen |
| 13 | Quest / Recipe / Names | 🟡 | Resolver vorhanden; vollständige Kanonisierung offen |
| 14 | Combat / Boss | 🟡 | Code vorhanden; Hotpath-Profiling offen |
| 15 | Release Reproducibility | 🔴 | `26.2.build.+` muss für Release gepinnt werden |
| 16 | 50/100 Player Load | 🔴 | realer Lasttest fehlt |

---

# 11. Production-Ready-Gate

Der Branch darf erst als Production-Ready bezeichnet werden, wenn alle folgenden Punkte nachgewiesen sind:

- [x] Java-25 Clean Build.
- [x] Paper-26.2-Dev-Bundle auflösbar.
- [x] Source-Boundary-Gates grün.
- [x] Shadow-JAR-Relocation-Gates grün.
- [x] JDBC-Service-Descriptor validiert.
- [x] Companion-Saves laufen nicht mehr synchron.
- [x] Companion-Equipment-Saves laufen nicht mehr synchron.
- [ ] Companion-Initialload vollständig asynchron.
- [ ] PlayerProfile persistente Revision.
- [ ] Economy auf atomare Ganzzahl-Währung umgestellt.
- [ ] Quest-/Recipe-Kanonisierung vollständig.
- [ ] Combat-/Boss-/Shop-Hotpaths profiliert.
- [ ] Release-Dependency-Versionen exakt gepinnt.
- [ ] Fat-JAR auf einem echten Paper-26.2-Server gestartet.
- [ ] MySQL/Hikari real getestet (`SELECT 1`).
- [ ] Join → Load → Mutation → Save → Quit → Reload ohne Datenverlust getestet.
- [ ] Concurrent-save-Recovery getestet.
- [ ] 50-Spieler-Lasttest.
- [ ] 100-Spieler-Lasttest.

---

# 12. Nächster technischer Durchlauf

Nach dem grünen CI-Build ist die richtige Reihenfolge:

1. Companion-Initialload asynchron machen.
2. PlayerProfile persistente Revision einführen.
3. Economy-Atomizität und Währungstyp endgültig härten.
4. Quest-/Recipe-/Displayname-Single-Source-of-Truth erzwingen.
5. Combat/Boss/Shop/Quest/Scoreboard-Hotpaths mit realen Profiling-Daten prüfen.
6. Release-Build auf exakten Paper-Dev-Bundle-Build pinnen.
7. Echtes Paper-26.2-Server-Smoke-Testprofil.
8. MySQL-Recovery-/Restart-Test.
9. 50-Spieler-Test.
10. 100-Spieler-Test.

**Kein weiterer großer Umbau sollte diese Reihenfolge überspringen.** Nach diesem Punkt ist reale Laufzeitmessung wertvoller als weitere theoretische Architekturkritik.
