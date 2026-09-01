# PixelRPG — Forensisches Stabilitäts-, Architektur- und Security-Audit

**Branch:** `test`
**Zielplattform:** Paper 26.x, aktuell Paper 26.2
**Java:** 25
**Mappings:** Mojang
**Stand:** Stabilisierungslauf fortgesetzt — PlayerProfile-Revisionsschutz gehärtet

## Urteil

Der Branch ist nach den bisherigen Stabilisierungsläufen deutlich belastbarer und die CI-Gates sind grün. Das ist weiterhin **kein Beweis für 50-/100-Spieler-TPS oder externe Datenbank-Recovery**.

Dieses Audit trennt bewusst zwischen tatsächlich implementierten Maßnahmen und Punkten, die nur durch reale Laufzeit-/Lasttests bewiesen werden können.

---

# 1. PlayerProfile / Persistenz

## Umgesetzt

- Deep-Snapshots vor Async-Handoffs.
- Kein Live-`PlayerProfile` im Repository-Thread.
- Dirty-State wird bei Änderungen während eines laufenden Saves erneut persistiert.
- Fehler markieren den Live-State wieder als dirty.
- Per-UUID Save-Sequenz.
- Idempotenter Shutdown.
- Equipment- und Quest-State werden gesnapshottet.
- Persistente `persistence_revision` in der MySQL-Persistenz.
- MySQL-Saves verwenden jetzt `SELECT persistence_revision ... FOR UPDATE`.
- Ein Save darf nur auf der erwarteten Revision aufbauen.
- Stale Profile aus einer zweiten Serverinstanz werden abgewiesen, bevor Child-State überschrieben wird.
- Revision wird erst nach erfolgreichem Commit am Live-Profil gesetzt.
- Der komplette Save bleibt eine Datenbanktransaktion.

### Wichtiger Befund

Das frühere Modell „Revision erhöhen und speichern“ war alleine nicht ausreichend: Zwei Serverinstanzen konnten theoretisch denselben alten Snapshot speichern. Der Save-Pfad besitzt jetzt einen pessimistischen Row-Lock und einen Revision-Compare, wodurch konkurrierende Saves serialisiert und veraltete Snapshots abgewiesen werden.

### Noch offen

- Konflikt-Recovery für eine tatsächlich abgewiesene Revision ist noch eine Produktentscheidung: Reload/merge/retry darf nicht blind implementiert werden, weil dadurch Gameplay-Mutationen verloren gehen könnten.
- YAML und MySQL sind weiterhin unterschiedliche Persistence-Backends und haben keinen gemeinsamen verteilten Lock.

---

# 2. Region

## Umgesetzt

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

# 3. NPC / HTTP / Skins

## Umgesetzt

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

# 4. Guild / Party

## Umgesetzt

- Runtime-State bleibt serverthreadgebunden.
- Persistenz über eigene sequenzielle I/O-Executors.
- Snapshots vor Dateisystem-I/O.
- Atomische YAML-Writes.
- Kontrollierter Shutdown-Flush.
- Singleton-/Lifecycle-Zustände werden beim Shutdown zurückgesetzt, soweit vorhanden.

---

# 5. Companion

## Umgesetzt

- Companion-YAML-Saves laufen über einen eigenen sequenziellen `PixelRPG-CompanionIO` Executor.
- Gespeichert werden detached `List.copyOf(...)`-Snapshots statt mutable Runtime-Listen.
- Companion-Equipment wird über einen eigenen `PixelRPG-CompanionEquipmentIO` Executor geschrieben.
- Beide Executor werden beim Shutdown kontrolliert beendet und warten auf ausstehende Writes.
- Runtime-Entity-Mutationen bleiben auf dem Serverthread.
- Equipment-Cache bleibt runtimegebunden und wird beim Service-Shutdown verworfen.

### Noch offen

- `CompanionService.load()` liest beim ersten Zugriff weiterhin synchron YAML. Das ist kein periodischer Hotpath, kann bei sehr großen Dateien aber Join-/GUI-Latenz verursachen.
- Companion-Listen sind weiterhin mutable Collections hinter einer Service-Fassade. Ein echtes Actor-/Mailbox-Modell wäre die stärkere Langzeitarchitektur.

---

# 6. Build / Dependency Isolation

## Umgesetzt

- Java 25 Toolchain.
- Paper 26.2 Dev-Bundle.
- Legacy-NMS/CraftBukkit/ChatColor Source-Gates.
- Gson, HikariCP und MySQL werden im Shadow-JAR relocated.
- `mergeServiceFiles()` für JDBC-Service-Provider.
- CI prüft erzeugte Shadow-JARs auf unrelocierte Drittanbieter-Klassen.
- CI prüft `META-INF/services/java.sql.Driver` auf den relocatierten MySQL-Treiber.
- CI verhindert den alten `com.mysql.cj.jdbc.Driver` Service-Eintrag.
- Build-Gate verwendet die tatsächliche `Set<File>`-Semantik des Gradle-ZipTree-Ergebnisses.

### Noch offen

`paperweight.paperDevBundle("26.2.build.+")` ist nicht reproduzierbar genug für Release-Builds. Für einen echten Release muss ein exakter Dev-Bundle-Build festgeschrieben werden.

---

# 7. Threading-Vertrag

Verbindlich:

- Paper-/Minecraft-Live-API nur auf dem vorgesehenen Serverthread.
- DB-I/O niemals Main Thread.
- HTTP-I/O niemals Main Thread.
- schweres Filesystem-I/O niemals Main Thread.
- mutable Domain-State nicht zwischen Main- und I/O-Threads teilen.
- Async Persistenz erhält Snapshots.
- Jeder Executor besitzt einen eindeutigen Owner.
- Jeder Executor wird beim Shutdown beendet.
- Async Tasks dürfen keine langlebigen Live-Entity-Referenzen halten.

---

# 8. Persistenz-Vertrag

```text
Main-thread mutation
        ↓
immutable snapshot
        ↓
ordered persistence queue
        ↓
atomic repository write
```

Für MySQL-PlayerProfile zusätzlich:

```text
expected revision N
        ↓
SELECT ... FOR UPDATE
        ↓
compare database revision == N
        ↓
transactional write
        ↓
commit revision N+1
```

Ein Snapshot mit Revision 42 darf keinen bereits gespeicherten Zustand 43 überschreiben.

---

# 9. Listener-Vertrag

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

Region wurde entsprechend aufgeteilt. Combat, Quest, NPC, Boss und Inventory müssen weiterhin auf versteckte Business-Logik geprüft werden.

---

# 10. Economy-Vertrag

## Noch offen — nächster Implementierungsblock

- Server ist autoritativ.
- Clientdaten bestimmen niemals Preis oder Kontostand.
- Geldmutation und Itemmutation müssen gegen doppelte Ausführung geschützt sein.
- Kaufen/Verkaufen/Bank/Guild müssen bei Fehlern atomar sein.
- `double` ist für ein dauerhaftes Geldmodell zu vermeiden.

### Aktueller Befund

`PlayerProfile` verwendet weiterhin `double money`. Das ist für ein langlebiges Wirtschaftssystem technisch nicht akzeptabel, sobald exakte Währungswerte garantiert werden müssen. Dieser Teil wird bewusst nicht durch kosmetische Validierung als „gelöst“ markiert.

Der richtige nächste Schritt ist ein zentraler Money-/Transaction-Typ mit Ganzzahl-Untereinheiten und eine atomare Economy-Service-Schicht, bevor Shops und Bank weiter ausgebaut werden.

---

# 11. Quest / Recipe / Displayname Single Source of Truth

## Bereits vorhanden

- Crafting-Rezepte werden über `CraftingRecipeRegistry` zentral geladen.
- Recipe-IDs werden kanonisiert.
- Item-IDs werden kanonisiert.
- `CraftingRecipeRegistry.findDisplayNameByResultItemId(...)` liefert den kanonischen player-facing Namen eines eindeutigen Rezeptoutputs.
- Quest-`COLLECT`-Validierung akzeptiert Vanilla-Materialien, registrierte PixelRPG-Items und bekannte Recipe-Outputs.
- Quest- und Recipe-Referenzen werden beim Load validiert.
- Unbekannte Prerequisites und Follow-Ups werden erkannt.
- Prerequisite-Zyklen werden erkannt.

### Noch offen

Die gesamte UI-/Quest-/Shop-Kette muss noch auf tatsächlich ausschließlich kanonische `CraftRecipe.displayName()`-/Item-Definition-Namen geprüft werden. Legacy-Stringfragmente dürfen nicht stillschweigend als Fallback weiterleben.

---

# 12. Hotpath- und Skalierungsprüfung

## Bereits entschärft

- Region-Spawn-Lookups indexiert.
- NPC-Lookups indexiert.
- Region-/NPC-/Companion-Persistenz aus kritischen Runtime-Schreibpfaden herausgezogen.
- HTTP-Skin-Auflösung blockiert den Serverthread nicht.
- Skin-Cache ist begrenzt.
- PlayerProfile-DB-Saves besitzen jetzt konkurrierenden Revisionsschutz.

## Nächster Profiling-Block

- Boss-Tick-Logik und Entity-Scans.
- Combat-/Skill-Hotpaths.
- Quest-Vollscans und Objective-Auswertung.
- Shop-/Trade-/Bank-GUIs.
- Companion-Follow-Tick.
- Scoreboard-Updates.
- World-/Biome-Boss-Spawning.

Ohne Spark-/Timings-Daten wäre jede Aussage über 100-Spieler-TPS Spekulation.

---

# 13. Cache-Vertrag

Jeder langlebige Cache benötigt:

- maximale Größe,
- TTL oder definierte Eviction,
- eindeutigen Owner,
- Shutdown-Cleanup.

Der External-Skin-Cache erfüllt diese Anforderungen.

Andere Cache-Strukturen müssen bei einem weiteren Repository-weiten Durchlauf auf diese Kriterien geprüft werden.

---

# 14. Security-Vertrag

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

# 15. Priorisierte Restarbeiten

| Priorität | Bereich | Status |
|---|---|---|
| P0 | PlayerProfile Cross-Instance Revision | umgesetzt |
| P0 | Economy Money-Typ + atomare Transaktionen | offen |
| P0 | Reale DB-Recovery-/Rollback-Tests | offen |
| P1 | Quest/Recipe/Displayname Single Source of Truth | teilweise umgesetzt |
| P1 | Companion Async Initial Load | offen |
| P1 | Boss/Combat Hotpath Profiling | offen |
| P1 | Shop/Trade/Bank Atomizität | offen |
| P2 | Scoreboard Update-Budgetierung | offen |
| P2 | Companion Actor/Mailbox-Modell | offen |
| P2 | Exakter Paper-26.2-Build für Release | offen |
| P2 | Kontrollierter HTTP-Resolver/Outbound-Proxy | offen |

---

# 16. Definition of Done für den Stabilitätsumbau

Der Branch gilt erst dann als stabilisierungsseitig abgeschlossen, wenn:

1. CI reproduzierbar grün ist.
2. PlayerProfile-Saves unter konkurrierenden Instanzen keine stale Writes zulassen.
3. Economy-Transaktionen atomar sind.
4. Geld nicht über `double` als fachliche Wahrheit geführt wird.
5. Shop-/Sell-/Buy-Operationen bei Fehlern vollständig rollbackfähig sind.
6. Quest-/Recipe-/Item-Namen aus einer kanonischen Quelle stammen.
7. Boss-/Combat-/Quest-/Companion-/Scoreboard-Hotpaths real profiliert wurden.
8. 50-Spieler- und 100-Spieler-Tests mit realistischem Gameplay durchgeführt wurden.
9. DB-Ausfall, Reconnect und Save-Queue-Stau getestet wurden.
10. Der Release-Build einen exakt gepinnten Paper-Dev-Bundle-Build verwendet.

**Kein grüner Compile allein darf einen dieser Punkte automatisch auf „erledigt“ setzen.**
