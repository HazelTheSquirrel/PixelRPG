# PixelRPG – Forensische Code-/Architekturanalyse
## Zielbranch: `Pixel-Region`
## Ablage: `test/audit.md`

**Audit-Zeitpunkt:** 2026-09-22  
**Repository:** `HazelTheSquirrel/PixelRPG`  
**Analysierter Commit:** `4d3d44b17fca5a8ee8c9296b26f8260ee5f3bf23`  
**Branch:** `Pixel-Region`  
**Referenz:** `main` / `test` bei Commit `1b5ac3dad55a1dde71e1e90413a54fe95b60d9fb`

> Dieses Audit bewertet den tatsächlich im Branch vorhandenen Quellstand. Es wurden keine Annahmen über nicht vorhandene Klassen oder geplante Systeme getroffen.

---

# 1. Executive Summary

`Pixel-Region` ist aktuell kein kleiner Patch am bestehenden PixelRPG, sondern faktisch ein **Extraktions-/Standalone-Branch für das Region-System**.

Der Branch liegt laut Git-Vergleich **7 Commits vor `main` und 0 Commits dahinter**. Gleichzeitig wurden große Teile des bisherigen RPG-Systems entfernt und durch eine deutlich kleinere Region-spezifische Struktur ersetzt.

Das Region-System selbst besitzt bereits eine brauchbare Grundarchitektur:

- zentrale `RegionManager`-Instanz
- immutable Polygon-Geometrie
- Chunk-basierter Spatial Index
- separate Policy-Schicht
- separates Persistence-Modul
- separates Editor-System
- separates Spawn-System
- separates Transition-System
- natives Paper-Dialog-System
- atomisches YAML-Schreiben
- sequenzielle Hintergrund-Persistenz
- Adventure Components
- Paper-Lifecycle-Command-Registrierung
- Java-25-/Paper-26.2-Buildbasis
- CI-Grenzprüfungen gegen Legacy-APIs

**Die größten Risiken liegen nicht in der Grundidee, sondern in Edge Cases, Datenmigration, Sicherheitssemantik, Skalierung und Testabdeckung.**

Besonders relevant:

1. **Besitzer/Mitglieder umgehen aktuell praktisch sämtliche Region-Flags**, darunter auch Gameplay-Regeln wie PvP, Fallschaden, Hunger und natürliche Heilung.
2. **`PlayerTeleportEvent` wird global als Region-Grenzprüfung verwendet**, wodurch auch administrative/technische Teleports von ENTRY/EXIT-Regeln betroffen sein können.
3. **Polygon-Indizierung basiert auf der kompletten Bounding-Box**, wodurch ein sehr großes oder dünnes Polygon extrem viele Chunk-Einträge erzeugen kann.
4. **Polygonvalidierung ist O(n²)** und besitzt kein explizites Punktemaximum.
5. **Die deklarierte Migration ist keine echte Migration**: `migrateLegacyFlags()` ist faktisch leer und alte Daten können beim Rewrite verloren gehen.
6. **Spawn-Editor akzeptiert alle LivingEntities, der Spawn-Service erzeugt aber ausschließlich Monster.**
7. **Region-Editor-Sessions werden bei PlayerQuit nicht entfernt** und Editor-Items können beim Disconnect/Disable liegen bleiben.
8. **Die aktuelle Region-Auflösung bei gleicher Priorität verwendet die UUID als Tie-Breaker**, obwohl die UUID keine fachliche Priorität ausdrückt.
9. **Es fehlen belastbare automatisierte Tests für Geometrie, Overlap, Flag-Vererbung, Teleport-Grenzen, Persistence und Spawnlogik.**
10. Der Branch ist als **Standalone-Extraction** sinnvoll, muss aber vor einer Integration in den Haupt-RPG-Code klar als Modulgrenze behandelt werden.

---

# 2. Branch-/Repository-Forensik

## 2.1 Branch-Zustand

Der analysierte Branch:

`Pixel-Region`
- HEAD: `4d3d44b17fca5a8ee8c9296b26f8260ee5f3bf23`
- Commit message: `Adapt CI verification for Pixel-Region`
- Parent: `dfe7d3694ee906c0ad99d9dc0202784752be08a5`

Der Branch basiert direkt auf dem damaligen `main`/ `test`-Stand.

GitHub-Vergleich:
- ahead: **7**
- behind: **0**
- merge base: `1b5ac3dad55a1dde71e1e90413a54fe95b60d9fb`

Damit ist `Pixel-Region` kein veralteter Fork des aktuellen Referenzstands, sondern eine lineare Weiterentwicklung.

## 2.2 Umfang der Extraktion

Der Vergleich mit `main` zeigt massive Löschungen im bestehenden RPG-System:

- PixelRPG Bootstrap/Plugin entfernt
- APIs entfernt
- Boss-System entfernt
- Combat-System entfernt
- Economy entfernt
- Equipment entfernt
- GUI-System entfernt
- Guild-System entfernt
- Item-System entfernt
- NPC-System entfernt
- Party-System entfernt
- Player/Profile-System entfernt
- Profession/Crafting-System entfernt
- Quest-System entfernt
- Shop/Trade/Travel-System entfernt
- diverse Daten-/Resourcepack-Dateien entfernt

Neu hinzugekommen bzw. erhalten:

- `de.pixelrpg.region.PixelRegionPlugin`
- `de.pixelrpg.region.PixelRegionCommand`
- Region-Domain
- Region-Editor
- Region-Policies
- Region-Persistence
- Region-Spawn-System
- Region-Transitions

### Bewertung

Das ist für einen **Standalone-Region-Extraktionsbranch** nachvollziehbar.

Für eine spätere Rückführung in `main` wäre ein einfacher Merge jedoch riskant. Die Änderung sollte konzeptionell als eigenes Modul behandelt werden und nicht als normales Feature-Commit.

---

# 3. Build-System

## 3.1 Positives

`build.gradle` verwendet:

- Java 25 Toolchain
- `options.release = 25`
- Paperweight Userdev `2.0.0-beta.21`
- Paper Dev Bundle `26.2.build.121-stable`
- Shadow `9.6.1`
- Gson `2.13.1`
- HikariCP `7.0.2`
- MySQL Connector/J `9.7.0`

Die Dependencies werden korrekt relocated:

- `com.google.gson` → `de.pixelrpg.rpg.libs.gson`
- `com.zaxxer.hikari` → `de.pixelrpg.rpg.libs.hikari`
- `com.mysql` → `de.pixelrpg.rpg.libs.mysql`

Der Sourcecode verwendet die nicht-relocated Namen.

## 3.2 Verifikationssystem

Vorhanden sind:

- Legacy-ChatColor-Prüfung
- Legacy-NMS-Prüfung
- CraftBukkit-Prüfung
- statische Player/Entity/World-Prüfung
- Shadow-JAR-Prüfung
- JDBC-Service-Prüfung
- `check`-Integration
- CI-Gegenprüfung

Das ist für die gewünschte Architektur sehr gut.

## 3.3 Auffälligkeit

Der Shadow-Artefaktname lautet:

`Pixel-Region.jar`

Das passt zum Branch/Standalone-Modul, weicht aber von der ursprünglichen PixelRPG-Namenskonvention ab.

**Empfehlung:** Wenn das System dauerhaft standalone bleiben soll, Namen konsistent auf `Pixel-Region` halten. Wenn es später wieder Teil von PixelRPG wird, Artefakt-/Plugin-Namen nicht vorschnell fest verdrahten.

---

# 4. Plugin Bootstrap

## `PixelRegionPlugin`

Ablauf:

1. Repository erzeugen
2. RegionManager erzeugen
3. Regionen laden
4. Editor starten
5. Spawn-Service starten
6. Listener registrieren
7. Commands über Paper Lifecycle registrieren
8. Shutdown in umgekehrter Reihenfolge

Das ist sauber.

### Positiv

Der Command wird über:

`LifecycleEvents.COMMANDS`

registriert und nicht über das alte PluginCommand-System.

Das entspricht der gewünschten modernen Paper-Architektur.

### Verbesserung

Die Initialisierung sollte perspektivisch einen expliziten Bootstrap-/Service-Container erhalten:

`RegionRepository`
→ `RegionManager`
→ `RegionPolicyService`
→ `RegionSpawnService`
→ `RegionTransitionService`
→ `RegionEditor`
→ Commands/Listeners

Aktuell wird diese Abhängigkeit teilweise direkt in Listenern erzeugt. Für das kleine Modul ist das noch vertretbar, langfristig aber weniger testbar.

---

# 5. Domainmodell: PixelRegion

## Stärken

`PixelRegion` besitzt:

- UUID
- Welt
- Polygon
- Y-Grenzen
- Name
- RegionType
- Beschreibung
- Owner
- Members
- Enter-/Leave-Messages
- Priorität
- Flags
- Properties
- SpawnPoints

Collections werden nach außen überwiegend als Kopien exponiert.

Das verhindert direkte externe Mutation.

## Auffälligkeit: Flag-Semantik

Die Methode:

`allowsFlag()` / `hasFlag()` / `flag()`

kombiniert:

- lokales explizites Flag
- globale Welt-Defaults
- Default-Flag

Das ist grundsätzlich gut.

Das eigentliche Problem ist die **Spieler-Ausnahme**, siehe Abschnitt Security.

---

# 6. Polygon-Geometrie

## 6.1 Algorithmus

`RegionGeometry` verwendet:

- Shoelace-Flächenberechnung
- Bounding-Box
- Ray-Casting für `contains()`
- Segment-Schnittprüfung
- EPSILON-basierte numerische Toleranz

Das ist für polygonale Minecraft-Regionen grundsätzlich passend.

## 6.2 Kritisches Skalierungsproblem

Die Validierung prüft alle Kantenpaare:

`O(n²)`

Bei normalen Admin-Polygonen ist das unproblematisch.

Es gibt aber **kein maximales Punktlimit**.

### Empfehlung

Ein hartes Limit einführen, beispielsweise:

- Warnung ab 128 Punkten
- harte Grenze bei 256 oder 512 Punkten

Die genaue Grenze sollte abhängig vom gewünschten Editor-UX festgelegt werden.

## 6.3 Spatial Index

Der RegionManager indiziert nicht die tatsächliche Polygonfläche, sondern die komplette Bounding-Box.

Bei einem Polygon:

- Länge: 10.000 Blöcke
- Breite: 10 Blöcke

kann trotzdem eine sehr große rechteckige Chunk-Fläche indiziert werden.

### Empfehlung

Mittelfristig:

- Bounding-Box als grober Filter behalten
- danach Polygonprüfung
- für sehr große Regionen zusätzlich räumliche Unterteilung einführen

Mögliche spätere Lösungen:

- Region-Chunk-Index mit Maximalgröße
- Quadtree
- R-Tree
- Polygon-Tiling

Für die aktuelle Größenordnung reicht zunächst ein Bounding-Box-Limit.

---

# 7. RegionManager

## 7.1 Architektur

Der Manager ist die zentrale Laufzeitquelle:

- UUID → Region
- World → Global Region
- Chunk → Region IDs
- Chunk → SpawnPointRefs

Das ist grundsätzlich eine gute Trennung.

## 7.2 Positiv: Snapshot-Persistence

Vor dem Hintergrundspeichern wird ein Snapshot erzeugt.

Das verhindert, dass der IO-Thread direkt mit veränderlichen Domainobjekten arbeitet.

Das ist wichtig.

## 7.3 Persistence Queue

Die Schreibvorgänge werden über eine Single-Thread-Executor-Kette serialisiert.

Das verhindert parallele YAML-Schreibzugriffe.

Gut.

## 7.4 Kritischer Punkt: große Region-Bounding-Boxes

Siehe Geometrieabschnitt.

## 7.5 Tie-Breaker

Regionen werden bei gleicher Priorität mit der UUID aufgelöst:

`Comparator.comparingInt(priority).thenComparing(id)`

Das ist deterministisch, aber fachlich zufällig.

### Empfehlung

Eine explizite Regel einführen, beispielsweise:

1. höhere Priorität
2. kleinere Fläche / spezifischere Region
3. expliziter Tie-Breaker
4. UUID nur als letzte technische Sicherheit

Alternativ sollte im Datenmodell ein eindeutiger Layer-/Order-Wert existieren.

---

# 8. Persistence / RegionRepository

## 8.1 Positiv

Die YAML-Datei wird atomar geschrieben:

1. `.tmp`
2. Move
3. ATOMIC_MOVE, wenn unterstützt
4. Fallback auf REPLACE_EXISTING

Das ist deutlich sicherer als direktes Überschreiben.

## 8.2 Format-Version

Aktuell:

`CURRENT_FORMAT_VERSION = 8`

Das ist grundsätzlich richtig.

### Kritischer Befund

`migrateLegacyFlags()` enthält keine echte Migration.

Der Code dokumentiert Migration, führt aber keine konkrete Umwandlung alter Strukturen durch.

Damit ist:

> Format Migration

aktuell eher:

> Daten neu laden + im aktuellen Format wieder speichern.

Das ist nicht dasselbe.

### Risiko

Alte oder unbekannte Felder können beim Rewrite verschwinden.

### Empfehlung

Migrationen explizit versionieren:

`v1 → v2`
`v2 → v3`
...
`v7 → v8`

Jede Migration sollte:

- Input prüfen
- Daten transformieren
- Output validieren
- erst danach speichern

## 8.3 Ungültige Regionen

Ungültige Regionen werden beim Laden übersprungen.

Das verhindert einen kompletten Plugin-Crash.

Allerdings besteht das Risiko stiller Datenverluste.

### Empfehlung

Zusätzlich:

- genaue Region-ID loggen
- Datei/Backup erhalten
- `regions.yml.invalid-backup` oder vergleichbares Quarantäne-System
- Startsummary ausgeben:
  - geladen
  - übersprungen
  - migriert
  - korrigiert

---

# 9. RegionEditor

## Positiv

Der Editor besitzt:

- Session pro Player
- PDC-markiertes Tool
- Polygonpunkte
- Spawnpoint-Tool
- Validierung vor Commit
- Live-Partikelvisualisierung
- keine Persistenz bis `confirm`

Das ist eine gute UX-Struktur.

## Problem 1: PlayerQuit

Eine Editor-Session wird bei Disconnect nicht explizit gelöscht.

Dadurch kann eine Session über den Disconnect hinaus im Speicher bleiben.

### Empfehlung

`PlayerQuitEvent`:

- Session entfernen
- temporäre Zustände entfernen
- optional Werkzeug erkennen und aufräumen

## Problem 2: Shutdown

`shutdown()` leert Sessions, entfernt aber keine Tools aus Spielerinventaren.

Bei Plugin-Reload/Disable kann das Editor-Item im Inventar verbleiben.

### Empfehlung

Beim Shutdown alle aktiven Spieler prüfen und Editor-/Spawn-Tools entfernen.

## Problem 3: keine Punktgrenze

Siehe Geometrie.

## Problem 4: Partikelkosten

Die Visualisierung läuft alle 2 Ticks.

Bei vielen Punkten und mehreren parallelen Sessions kann die Partikelzahl stark steigen.

### Empfehlung

- nur bei sichtbarer Distanz rendern
- adaptive Renderfrequenz
- maximaler Perimeter
- maximale Punktanzahl
- Partikelanzahl pro Segment begrenzen

---

# 10. RegionPolicyService

Hier liegt eine der wichtigsten fachlichen Fragen.

## 10.1 Owner-/Member-Bypass

`allowsFlag()` macht:

- Owner → erlaubt
- Member → erlaubt
- sonst → Flag prüfen

Das gilt dadurch indirekt für sehr viele Flags.

Damit können Owner/Members unter anderem Regeln umgehen, die normalerweise Gameplay-Regeln darstellen:

- PvP
- Mob Damage
- Animal Damage
- Fallschaden
- Item Drop
- Item Pickup
- Hunger
- natürliche Heilung
- Interaktionen
- Blockschutz

### Bewertung

Für Build-/Protection-Flags ist das sinnvoll.

Für Gameplay-Flags ist es möglicherweise falsch.

### Empfehlung: Flag-Klassen trennen

Beispielsweise:

**Owner/Member bypassbar**
- BLOCK_BREAK
- BLOCK_PLACE
- CHEST_USE
- DOOR_USE
- ENTITY_INTERACTION

**Nicht bypassbar**
- PVP
- MOB_DAMAGE
- DAMAGE_ANIMALS
- FALL_DAMAGE
- NATURAL_HEALTH_REGEN
- NATURAL_HUNGER_DRAIN
- ENTRY
- EXIT
- MOB_SPAWNING

Damit wird die Semantik explizit.

---

# 11. Teleport-/Boundary-System

## Aktuelles Verhalten

`PlayerTeleportEvent` prüft:

- Enderpearl
- danach allgemeine Region-Grenzen

Das bedeutet, dass auch andere Teleportarten durch ENTRY/EXIT beeinflusst werden können.

### Risiko

Administratives Teleportieren oder serverseitige Teleports können unerwartet blockiert werden.

### Empfehlung

Explizite Bypass-Regel:

- `rpg.admin`
- oder System-/Plugin-Teleports mit internem Bypass

Alternativ eine separate Policy:

`RegionTransitionCause`

mit Regeln pro Cause.

## Zweites Problem

Die Grenzerkennung arbeitet auf Regionwechseln.

Die Transition-Erkennung wird nur ausgelöst, wenn sich der Block ändert.

Polygongrenzen können jedoch theoretisch innerhalb eines Blocks liegen.

### Empfehlung

Für normale Minecraft-Blockregionen ist die aktuelle Lösung wahrscheinlich ausreichend.

Wenn geometrische Präzision wichtig wird:

- Position statt Blockposition auswerten
- nur bei relevanter Bewegung prüfen
- optional Distanzschwelle verwenden

---

# 12. RegionTransitionService

Die Zustandsverwaltung ist einfach und verständlich.

Positiv:

- Player UUID → Region UUID
- Enter/Leave getrennt
- World Change berücksichtigt
- Quit-State wird entfernt
- Title-Ausgabe ist Adventure-basiert

## Verbesserung

Bei Plugin-/Serverstart existiert noch kein initialer Zustand für Spieler.

Der Zustand wird erst beim nächsten relevanten Move/World-Change aktualisiert.

### Empfehlung

Beim Start:

`onlinePlayers.forEach(transitions::update)`

oder nach Listener-/Service-Initialisierung einen initialen Scan ausführen.

---

# 13. RegionSpawnService

## Positiv

Das System ist deutlich besser als ein globaler `runTaskTimer`-Spawnloop.

Es verwendet:

- Chunk-basierten Spawnpoint-Index
- WakeScheduler
- Player-Nähe
- Death-triggered respawn
- PDC-Marker
- gezielte Punktprüfung

Das ist eine gute Architektur.

## Kritisches Problem: Entity-Auswahl

Der Editor akzeptiert:

`LivingEntity`

Der Spawn-Service akzeptiert später:

`Monster`

Damit können gültige Editor-Eingaben erzeugt werden, die niemals spawnen.

### Empfehlung

SpawnMobType sollte explizit auf die tatsächlich unterstützten Spawnklassen eingeschränkt werden.

Zum Beispiel:

`Monster.class.isAssignableFrom(type.getEntityClass())`

bereits im Editor validieren.

## Zweites Problem

Die Spawnlogik markiert das Entity erst **nach** `spawnEntity()`.

Wenn ein Spawn während des Events sofort anderweitig verarbeitet wird, kann es theoretisch ein kurzes unmarkiertes Zeitfenster geben.

Für die aktuelle Architektur wahrscheinlich geringes Risiko, aber bei komplexeren Spawnketten sollte der Marker über eine kontrollierte Spawnroutine gesetzt werden.

## Drittes Problem

`hasManagedMob()` sucht nur in:

`1.5 x 2.5 x 1.5`

Das ist für den Spawnpunkt plausibel.

Es sollte aber dokumentiert werden, dass ein Mob den Spawnpunkt verlassen kann und dann ein neuer Mob entstehen darf.

---

# 14. Dialog-System

Die aktuelle Implementierung verwendet das native Paper-Dialog-System:

- `Dialog`
- `DialogBase`
- `DialogRegistryEntry`
- `DialogType`
- `ActionButton`
- `DialogAction.customClick`

Das entspricht der gewünschten modernen Paper-Architektur.

Die Kategorien-/Detailnavigation ist nachvollziehbar.

## Verbesserung

Die Dialoglogik könnte stärker von der Domain getrennt werden:

`RegionFlagDialogService`

sollte möglichst nur:

- ViewModel erzeugen
- Dialog darstellen
- Action an Service delegieren

Die tatsächliche Mutation sollte über einen dedizierten `RegionFlagService` laufen.

Dadurch wäre:

- Permission
- Mutation
- Persistence
- Audit-Logging

an einer Stelle zentralisiert.

---

# 15. Command-System

## Positiv

`BasicCommand` wird verwendet.

Subcommands sind getrennt.

Tab-Completion ist vorhanden.

Permissions werden pro Subcommand berücksichtigt.

## Kritischer Permission-Befund

Root command:

`permission() = rpg.member`

Admin-Subcommands benötigen:

`rpg.admin`

Ein Server kann daher theoretisch einem Admin `rpg.admin` geben, ohne `rpg.member`.

Dann kann der Root-Command bereits auf der Root-Ebene geblockt werden.

### Empfehlung

Root-Command ohne harte Permission registrieren und Permission ausschließlich pro Subcommand prüfen.

Oder eine explizite Permission-Hierarchie:

`rpg.member`
`rpg.admin` ⇒ `rpg.member`

nicht nur implizit über OP-Defaults.

---

# 16. RegionFlag-Modell

Das Flag-System ist umfangreich und fein granular.

Das ist positiv.

Kategorien für:

- Region
- Blocks
- Interaction
- Container
- Combat
- Mob Spawn
- Environment
- Explosions
- Player
- Movement

ermöglichen gute Admin-UX.

## Verbesserung

Die Flag-Menge sollte nicht ausschließlich über Event-Code wachsen.

Empfehlung:

Ein deklaratives Flag-Metadatenmodell:

- Kategorie
- Displayname
- Default
- OwnerBypass
- MemberBypass
- Eventfamilie
- Sicherheitsstufe

Dadurch könnte Dialog, Policy und Default-Verhalten dieselben Metadaten verwenden.

---

# 17. Event-Abdeckung

Das Listener-System deckt bereits viele Fälle ab:

- Block Break
- Block Place
- Interactions
- Inventory
- PvP
- Mob Damage
- Animal Damage
- Fall Damage
- Spawn
- Fire
- Lava
- Water
- Explosions
- TNT
- Enderman
- Lightning
- Crops
- Leaves
- Trampling
- Sleep
- Chorus
- Enderpearl
- Hunger
- Regen
- Region Boundary
- Enter/Leave

Das ist eine solide Basis.

## Aber:

Bei einem so großen Flag-Modell sollte es eine **Coverage-Matrix** geben.

Beispiel:

| Flag | Policy | Listener | Test |
|---|---|---|---|
| BLOCK_BREAK | ja | ja | fehlt |
| BLOCK_PLACE | ja | ja | fehlt |
| PVP | ja | ja | fehlt |
| ENTRY | ja | ja | fehlt |
| EXIT | ja | ja | fehlt |
| SPAWN_CREEPER | ja | ja | fehlt |
| CHEST_USE | ja | ja | fehlt |

Das sollte automatisiert geprüft werden.

---

# 18. Datenintegrität

## Gute Punkte

- UUIDs
- immutable Geometry
- defensive copies
- atomisches Schreiben
- sequenzielle Persistence
- Format-Version
- ungültige Daten führen nicht direkt zum Plugin-Absturz

## Verbesserungen

### Backup

Vor jeder Migration:

`regions.yml.bak`

erstellen.

### Checksums

Optional:

- SHA-256 der gespeicherten Datei
- Startprüfung gegen letzte bekannte Version

### Recovery

Bei YAML-Fehler:

1. aktuelle Datei behalten
2. Backup laden
3. Fehler melden
4. Plugin mit Recovery-State starten

---

# 19. Threading

## Aktuell

Gameplay-/Domain-Mutationen erfolgen auf dem Serverthread.

Persistence läuft über einen dedizierten Executor.

Das ist richtig.

## Zu beachten

`RegionRepository.load()` ist synchron.

Bei sehr großen `regions.yml` kann der Serverstart blockieren.

### Zukunftslösung

Zwei Phasen:

1. Async:
   - YAML lesen
   - parse
   - validate
2. Main Thread:
   - World-Abgleich
   - Index veröffentlichen

Das sollte erst umgesetzt werden, wenn die Datenmenge dies tatsächlich rechtfertigt.

---

# 20. Performance-Risiken

## Hoch

### Bounding-Box-Index

Große Regionen erzeugen viele Chunk-Einträge.

### Partikelvisualisierung

Perimeter × Sessions × 2-Tick-Frequenz.

## Mittel

### Polygon-Validierung

O(n²).

### `all()`

Sortiert bei jedem Aufruf alle Regionen.

Bei kleinen Mengen egal, bei großen Mengen unnötig.

### Verbesserung

Cached sorted view oder gezielte Iteration.

---

# 21. Security-/Abuse-Risiken

## 21.1 Admin-Tool

Das Tool ist über PDC markiert.

Gut.

## 21.2 Session Ownership

Die Session wird über UUID identifiziert.

Gut.

## 21.3 Werkzeug-Duplizierung

Die Erkennung erfolgt über PDC.

Bei einem Spieler mit mehreren identischen Tools kann `removeTools()` alle entfernen.

Das ist korrekt für Session-Start, kann aber bei unerwarteten Disconnects zu Inventarverlust führen, wenn fremde/ähnliche Items dieselben NamespacedKeys tragen.

Da die Key unter Plugin-Kontrolle steht, ist das interne Risiko klein.

## Empfehlung

Zusätzliche eindeutige Session-ID im PDC:

`region_editor_session`

Dann können nur Session-eigene Items entfernt werden.

---

# 22. Logging

Aktuell wird vor allem bei Persistence-/Load-Problemen geloggt.

Empfehlung:

Startsummary:

```
Pixel-Region:
  Regions: 42
  Global worlds: 3
  Spawn points: 17
  Invalid regions: 0
  Migrated: false
```

Bei Adminänderungen optional:

- Region erstellt
- Region gelöscht
- Owner geändert
- Member geändert
- Flag geändert

Das erleichtert spätere Forensik erheblich.

---

# 23. Fehlende Tests

Das ist aktuell der größte Qualitätslücke.

Es sollten mindestens Unit-Tests existieren für:

## Geometry

- Dreieck
- Quadrat
- konkaves Polygon
- Selbstüberschneidung
- Kantenberührung
- Punkt auf Kante
- Punkt außerhalb
- Punkt innerhalb
- sehr große Koordinaten
- NaN
- Infinity
- doppelte Punkte
- collineare Punkte

## Manager

- Create
- Delete
- Overlap
- Priority
- World isolation
- Global fallback
- Flag inheritance

## Persistence

- Save/load
- Empty file
- Corrupt region
- old format
- missing fields
- invalid UUID
- invalid flags
- migration

## Policy

- Owner
- Member
- Guest
- Global
- local override
- inheritance

## Boundary

- enter allowed
- enter denied
- exit allowed
- exit denied
- same region
- world change
- teleport

## Spawn

- valid monster
- invalid entity
- player proximity
- respawn delay
- managed marker
- point removal

---

# 24. Empfohlene Testpyramide

## Unit Tests

Sofort:

`RegionGeometryTest`
`RegionManagerTest`
`RegionPolicyServiceTest`
`RegionRepositoryTest`

## Integration Tests

Danach:

- Paper Test Server
- Region creation
- commands
- dialogs
- event cancellation
- teleport
- spawning

## End-to-End

Zum Schluss:

1. Server starten
2. Region anlegen
3. Region speichern
4. Server stoppen
5. Server starten
6. Region laden
7. Flags ändern
8. Spieler bewegen
9. Spawn prüfen
10. Restart erneut

---

# 25. Empfohlene Architektur nach Stabilisierung

Zielstruktur:

```
region/
├── api/
│   ├── Region.java
│   ├── RegionService.java
│   └── RegionQuery.java
│
├── domain/
│   ├── PixelRegion.java
│   ├── RegionGeometry.java
│   ├── RegionPoint.java
│   ├── RegionFlag.java
│   └── RegionType.java
│
├── policy/
│   ├── RegionPolicyService.java
│   ├── RegionFlagMetadata.java
│   └── RegionAccessService.java
│
├── persistence/
│   ├── RegionRepository.java
│   ├── RegionMigration.java
│   └── RegionSnapshot.java
│
├── runtime/
│   ├── RegionManager.java
│   ├── RegionSpatialIndex.java
│   └── RegionTransitionService.java
│
├── spawn/
│   ├── RegionSpawnService.java
│   └── RegionSpawnPoint.java
│
├── editor/
│   ├── RegionEditor.java
│   └── RegionEditorSession.java
│
└── command/
    ├── PixelRegionCommand.java
    └── ...
```

Das ist keine zwingende Sofortmaßnahme. Der aktuelle Code ist noch klein genug, um zunächst funktional stabilisiert zu werden.

---

# 26. Priorisierte Maßnahmen

## P0 – vor produktivem Einsatz

### P0.1 Flag-Bypass korrigieren

Owner/Member nicht pauschal von allen Flags ausnehmen.

### P0.2 Teleport-Bypass definieren

Admin-/System-Teleports dürfen nicht unbeabsichtigt durch Region-Grenzen blockiert werden.

### P0.3 Echte Datenmigration

`migrateLegacyFlags()` und Formatmigration tatsächlich implementieren.

### P0.4 Spawn-Typen konsistent machen

Editor und Runtime müssen dieselbe erlaubte Entity-Menge verwenden.

---

## P1 – Stabilität

### P1.1 EditorSession bei Quit entfernen

### P1.2 Editor-Tools bei Shutdown bereinigen

### P1.3 Region-Punktlimit

### P1.4 Bounding-Box-/Chunk-Index-Limit

### P1.5 Unit-Test-Suite

### P1.6 Persistence-Recovery

---

## P2 – Architektur

### P2.1 Flag-Metadaten zentralisieren

### P2.2 RegionSpatialIndex extrahieren

### P2.3 RegionFlagService einführen

### P2.4 Command-Permissions hierarchisch modellieren

---

## P3 – Optimierung

### P3.1 Quadtree/R-Tree für riesige Welten

### P3.2 Async Loading

### P3.3 gecachter Region-Selector

### P3.4 adaptive Editor-Visualisierung

---

# 27. Konkrete Zielregeln für Flags

Empfohlene Semantik:

| Flaggruppe | Owner | Member | Gast |
|---|---:|---:|---:|
| BLOCK_* | bypass | bypass | Flag |
| CONTAINER_* | bypass | bypass | Flag |
| INTERACTION | bypass | bypass | Flag |
| ENTITY_INTERACTION | bypass | bypass | Flag |
| PVP | Flag | Flag | Flag |
| MOB_DAMAGE | Flag | Flag | Flag |
| DAMAGE_ANIMALS | Flag | Flag | Flag |
| FALL_DAMAGE | Flag | Flag | Flag |
| ENTRY | optional | optional | Flag |
| EXIT | optional | optional | Flag |
| SPAWN_* | Flag | Flag | Flag |
| EXPLOSION | Flag | Flag | Flag |
| FIRE/LAVA/WATER | Flag | Flag | Flag |
| HUNGER/REGEN | Flag | Flag | Flag |

Das verhindert, dass Eigentum automatisch Gameplay-Unverwundbarkeit bedeutet.

---

# 28. Konkrete Zielregeln für Regionauflösung

Empfohlene Reihenfolge:

1. Welt
2. Y-Bereich
3. Polygon
4. Priorität
5. Spezifität
6. stabiler expliziter Order-Wert
7. UUID als letzter Tie-Breaker

Damit wird das Verhalten für überlappende Regionen nachvollziehbarer.

---

# 29. Konkrete Zielregeln für Migration

Jede Migration sollte ungefähr so aufgebaut sein:

```
if version < 2:
    migrateV1ToV2()

if version < 3:
    migrateV2ToV3()

...

validateCompleteModel()

writeBackup()

writeNewFormat()
```

Migrationen dürfen keine unbekannten Daten stillschweigend zerstören.

---

# 30. CI-Erweiterungen

Aktuell ist die CI-Grundlage gut.

Zusätzlich empfehlenswert:

1. `gradle test`
2. `gradle check`
3. Unit-Test-Coverage
4. YAML-Roundtrip-Test
5. RegionGeometry-Fuzz-Test
6. Test des finalen Shadow-JAR
7. Prüfung auf `audit.md` nur falls gewünscht
8. Paper-Testserver-Smoke-Test

Ein besonders wertvoller Test wäre ein Geometrie-Fuzzer:

- zufällige Polygonpunkte
- extrem große Koordinaten
- kollineare Punkte
- degenerierte Polygone
- Selbstüberschneidungen

---

# 31. Nicht verifizierte Punkte

Dieser Audit wurde statisch gegen den GitHub-Quellstand durchgeführt.

Nicht als erfolgreich ausgeführt bewertet wurden:

- lokales `gradle clean build`
- tatsächlicher Paper-Serverstart
- tatsächliche Dialogausführung im Client
- tatsächliche Eventreihenfolge auf einem laufenden Paper-26.2-Server
- tatsächliches YAML-Recovery unter Prozessabbruch
- Lasttest mit sehr großen Polygonen

Für den analysierten Commit wurde über GitHub außerdem kein zuordenbarer Workflow-Run zurückgegeben.

Daher wird **kein grüner Buildstatus behauptet**.

---

# 32. Gesamtbefund

## Architektur

**Gut als Basis für ein eigenständiges Region-Modul.**

Die Verantwortlichkeiten sind bereits deutlich besser getrennt als bei einer monolithischen Implementierung.

## Codequalität

**Solide, aber noch nicht produktionshart.**

Die größten offenen Punkte sind nicht Syntax, sondern Semantik und Edge Cases.

## Datenhaltung

**Grundsätzlich solide.**

Atomisches Schreiben und sequenzielle Persistence sind gute Entscheidungen.

Die Migration muss allerdings echte Transformationen erhalten.

## Performance

**Für normale Regiongrößen ausreichend.**

Große Bounding-Boxes und unlimitierte Polygonpunkte sind die wesentlichen Risiken.

## Security/Gameplay

**Hier besteht der höchste Handlungsbedarf.**

Der pauschale Owner-/Member-Bypass ist zu breit, wenn Flags tatsächlich Schutzregeln darstellen sollen.

## Testbarkeit

**Deutlich ausbaufähig.**

Das System besitzt gute natürliche Unit-Test-Grenzen, nutzt sie aber aktuell noch nicht ausreichend.

---

# 33. Empfohlene Reihenfolge der nächsten Entwicklung

### Phase 1 – Semantik stabilisieren

1. Owner/Member Flag-Bypass klassifizieren
2. Teleport-Bypass definieren
3. Spawn-Typen konsistent machen
4. Permission-Hierarchie korrigieren

### Phase 2 – Daten sichern

5. echte Migrationen
6. Backup/Recovery
7. Load-/Save-Roundtrip-Tests

### Phase 3 – Stabilität

8. Geometry Tests
9. Policy Tests
10. Manager Tests
11. Spawn Tests
12. Boundary Tests

### Phase 4 – Skalierung

13. Punktlimit
14. Bounding-Box-Limit
15. Index-Optimierung
16. adaptive Visualisierung

### Phase 5 – Integration

17. Region-Modul wieder sauber in den PixelRPG-Hauptstand integrieren
18. klare API-Schnittstelle definieren
19. keine direkte Abhängigkeit von entfernten RPG-Subsystemen

---

# 34. Schlussfolgerung

Der `Pixel-Region`-Branch hat bereits eine brauchbare technische Grundlage für ein eigenständiges Region-System.

Die wichtigste Verbesserung ist aktuell **nicht ein großer Refactor**, sondern die fachliche Stabilisierung:

- klare Flag-Semantik
- saubere Teleport-Regeln
- echte Migration
- konsistente Spawnregeln
- robuste Sessionverwaltung
- automatisierte Tests

Danach kann die räumliche Optimierung erfolgen.

Ein vollständiger Rewrite ist **nicht erforderlich**. Die bestehende Architektur kann weiterverwendet und schrittweise gehärtet werden.

---

## Audit-Evidenz

Analysierte Kernartefakte:

- `build.gradle`
- `settings.gradle`
- `src/main/resources/paper-plugin.yml`
- `PixelRegionPlugin.java`
- `PixelRegionCommand.java`
- `RegionSubCommand.java`
- `EditSubCommand.java`
- `PixelRegion.java`
- `RegionGeometry.java`
- `RegionManager.java`
- `RegionRepository.java`
- `RegionPolicyService.java`
- `RegionListener.java`
- `RegionEditor.java`
- `RegionFlag.java`
- `RegionFlagDialogService.java`
- `RegionSpawnService.java`
- `RegionSpawnPoint.java`
- `SpawnMobType.java`
- `RegionTransitionService.java`
- `RegionType.java`
- `.github/workflows/build.yml`

Git-Vergleich:

- Base: `main`
- Head: `Pixel-Region`
- Ergebnis: 7 Commits ahead / 0 behind
- Head: `4d3d44b17fca5a8ee8c9296b26f8260ee5f3bf23`

**Ende des Audits.**
