# PixelRPG — Clean Rebuild Audit

## 0. Verbindlicher Auftrag

Diese Datei ist die **alleinige Master-Spezifikation für den vollständigen Clean Rebuild** auf dem Branch `rebuild`.

### Ziel

`main` ist die vollständige Referenz für das bestehende PixelRPG-System.

Der Auftrag lautet ausdrücklich:

> **PixelRPG aus `main` funktional, visuell und verhaltensseitig vollständig neu aufbauen — 1:1 im extern beobachtbaren Ergebnis, aber intern sauber neu entwerfen und Altlasten, totes Zeug, Duplikate und überholte Strukturen entfernen.**

Das ist **kein partieller Rebuild**, keine Auswahl einzelner Systeme und kein bloßes Beheben dokumentierter Lücken.

Es wird **das komplette System aus `main` neu gebaut**.

Dabei gilt:

- Verhalten von `main` ist SOLL.
- Darstellung von `main` ist SOLL.
- Spielerabläufe von `main` sind SOLL.
- Commands und deren Oberfläche von `main` sind SOLL.
- GUIs und Dialoge von `main` sind SOLL.
- NPC-Verhalten von `main` ist SOLL.
- Persistence und Datenflüsse von `main` sind SOLL.
- Content und Ressourcen von `main` sind SOLL.
- externe/public APIs von `main` sind SOLL.
- Integrationen und Verbindungen zwischen den Systemen von `main` sind SOLL.

**Nicht SOLL ist die interne historische Architektur von `main`.**

Der Rebuild darf und soll intern besser strukturiert sein.

---

## 1. Branch- und Repository-Regeln

Repository:

`https://github.com/HazelTheSquirrel/PixelRPG`

Verbindlich:

- `main` = Referenz / SOLL
- `test` = bestehender Entwicklungszweig, nicht verändern
- `rebuild` = einziger Arbeitszweig für diesen Clean Rebuild
- `main` niemals verändern
- `test` niemals verändern
- Änderungen dieses Auftrags ausschließlich auf `rebuild`

Referenz-Snapshot des bisher auditierten `main`:

`b02747a21cde6d1308c59a7f027cda80877cfe50`

Wenn sich `main` während des Rebuilds ändert, muss der neue Referenzstand ausdrücklich auditiert und als neue Referenzversion dokumentiert werden.

---

## 2. Radikaler Neustart des Sourcecodes

### Verbindliche Anweisung

Der bisherige Rebuild-Sourcecode unter:

`src/`

wird **vollständig entfernt**.

Es wird **kein bisheriger Rebuild-Java-Code als Ausgangsbasis vorausgesetzt**.

Das bedeutet:

- kein schrittweises Weiterflicken der bisherigen Rebuild-Implementierung;
- keine Übernahme alter Rebuild-Manager nur weil sie bereits existieren;
- keine Übernahme bisheriger Domainmodelle ohne erneute Begründung;
- keine Übernahme bisheriger Listener ohne erneute Prüfung;
- keine Übernahme bisheriger Adapter/Fallbacks;
- keine Übernahme bisheriger UI-Entscheidungen ohne Vergleich mit `main`.

### Was erhalten bleibt

Nur die ausdrücklich technische Projektbasis bleibt außerhalb von `src/` erhalten, soweit sie dem verbindlichen Projektsetup entspricht:

- `settings.gradle`
- `build.gradle`
- `.github/workflows/`
- `.vscode/`
- Repository-/Projekt-Metadaten
- sonstige reine Entwicklungsinfrastruktur, sofern sie nicht veraltet oder tot ist.

Auch diese Dateien werden gegen den tatsächlichen Referenzstand geprüft.

**Das Löschen von `src/` ist kein Verlust des SOLL-Zustands. `main` bleibt die vollständige Referenz.**

---

## 3. Technische Zielplattform

Unveränderlich:

- Java 25
- Paper 26.2
- Paper Dev Bundle `26.2.build.121-stable`
- `io.papermc.paperweight.userdev` 2.0.0-beta.21
- Shadow 9.6.1
- Mojang-Mappings
- `paper-plugin.yml`
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

Relocations:

- `com.google.gson` -> `de.pixelrpg.rpg.libs.gson`
- `com.zaxxer.hikari` -> `de.pixelrpg.rpg.libs.hikari`
- `com.mysql` -> `de.pixelrpg.rpg.libs.mysql`

Artefakte:

- ShadowJar: `Pixel-RPG.jar`
- normaler Jar: Classifier `slim`

Nicht erlaubt:

- Legacy-Bukkit-System
- CraftBukkit
- Legacy-NMS
- Spigot-Mappings
- `ChatColor`
- erfundene Paper-APIs
- 1.21.x-Workarounds
- alte Dialogimplementierungen
- `reobfJar`

---

## 4. Was „1:1“ bedeutet

„1:1“ bedeutet **nicht**, dass dieselben Klassen, Dateinamen oder Manager erneut entstehen.

„1:1“ bedeutet, dass ein Spieler, Serveradministrator oder externer Integrator keinen unbeabsichtigten funktionalen Unterschied zum Referenzzustand erkennen darf.

Das umfasst insbesondere:

### Spieleroberfläche

- Menüs
- GUIs
- Dialoge
- Buttons
- Slotbelegung
- Texte
- Farben
- Icons
- Navigation
- Rückkehrverhalten
- Fehlermeldungen
- Erfolgsmeldungen
- Statusanzeigen
- Sichtbarkeit
- Aktivierungsbedingungen.

### Commands

- Root-Commands
- Untercommands
- Aliase
- Permissions
- Tab-Completion
- Argumente
- Validierung
- Fehlermeldungen
- Admin-Funktionen
- sichtbare Ausgaben
- Folgeaktionen.

### Gameplay

- NPCs
- Quests
- Story
- Dialoge
- Berufe
- Crafting
- Items
- Equipment
- Stats
- Economy
- Party
- Guild
- Companions
- Shops
- Trade Depot
- Bank
- Regionen
- Bosse
- Navigation
- Scoreboard
- Playtime
- Resourcepack
- Player Lifecycle.

### Technik mit sichtbarer oder fachlicher Wirkung

- Laden
- Speichern
- Migration
- Revisionen
- Recovery
- Shutdown
- Join/Quit
- Chunk Lifecycle
- Entity Lifecycle
- Skin-Auflösung
- Skin-Persistenz
- Event-Verbindungen
- APIs
- Service-Registrierung
- externe Integrationen.

---

## 5. Was entfernt werden darf

Eine bestehende Struktur darf nur entfernt werden, wenn die Analyse nachweist, dass sie:

- tatsächlich unbenutzt ist;
- keine externe API darstellt;
- keinen sichtbaren oder fachlichen Ablauf liefert;
- durch eine andere Implementierung vollständig ersetzt wird;
- redundant ist;
- ein historisches/legacy Artefakt ohne SOLL-Wirkung ist.

### Harte Regel

**„Alt“ bedeutet nicht automatisch „tot“.**

**„Fehlt im neuen Code“ bedeutet nicht automatisch „nicht benötigt“.**

**„Kann einfacher gebaut werden“ bedeutet nicht automatisch „Verhalten darf sich ändern“.**

Vor jeder Entfernung muss die Funktion betrachtet werden, die durch die Struktur ermöglicht wird.

---

## 6. Vollständiges Audit von main

Der Rebuild beginnt mit einer vollständigen Inventarisierung von `main`.

Nicht nur Java-Dateien werden betrachtet.

### Zu inventarisieren

1. Gradle-/Build-System
2. Plugin-Manifest
3. Bootstrap
4. Plugin-Lifecycle
5. alle Java-Klassen
6. alle Interfaces
7. alle Enums
8. alle Records
9. alle Events
10. alle Listener
11. alle Scheduler/Tasks
12. alle Commands
13. alle GUIs
14. alle Dialoge
15. alle NPC-Systeme
16. alle Services
17. alle Repositories
18. alle APIs
19. alle Datenbanken/Tabellen/Migrationen
20. alle YAML-/JSON-/Text-Ressourcen
21. alle Konfigurationen
22. alle Content-Dateien
23. alle Resourcepack-Dateien
24. alle Permissions
25. alle externen Integrationen
26. alle öffentlichen Einstiegspunkte
27. alle Player-Lifecycle-Pfade
28. alle Server-Lifecycle-Pfade.

Für jedes Artefakt ist die tatsächliche Rolle zu bestimmen.

---

## 7. Verbindliche Funktionsmatrix

Für **jedes** relevante Artefakt aus `main` muss im Audit ein Nachweis entstehen:

| main-Artefakt/Funktion | Externes Verhalten | Abhängigkeiten | Persistence/Content | Zielimplementierung | Status |
|---|---|---|---|---|---|
| jedes relevante Artefakt | konkret beschreiben | konkret erfassen | konkret erfassen | neu definieren | OPEN/REBUILT/VERIFIED |

Statusbedeutungen:

- **OPEN** — noch nicht neu gebaut oder nicht geprüft
- **REBUILT** — neu implementiert
- **VERIFIED** — gegen `main` funktional und präsentationsseitig geprüft
- **REMOVED** — nachweislich ohne SOLL-Wirkung
- **MERGED** — Funktion in eine neue Struktur integriert und geprüft
- **REPLACED** — technisch anders umgesetzt, gleiche Wirkung nachgewiesen.

**Ein Status ohne Nachweis ist ungültig.**

---

## 8. Zielarchitektur

Die neue Architektur wird nicht aus der Klassenstruktur von `main` kopiert.

Sie wird aus den tatsächlichen fachlichen Verantwortlichkeiten abgeleitet.

Grundprinzip:

`Content -> Domain -> Application/Services -> Runtime -> Presentation -> Persistence/Integration`

Nicht jedes System muss exakt diese Pakete verwenden. Die Verantwortungsgrenzen müssen aber nachvollziehbar sein.

### Grundregeln

- keine God-Manager;
- keine versteckten globalen Abhängigkeiten;
- keine statischen Live-Server-Objekte;
- keine fachliche Logik in der Composition Root;
- keine UI-Logik in Persistence;
- keine Persistence-Logik in NPC-Entity-Verwaltung;
- keine Questregeln im Dialog-Renderer;
- keine Content-Texte unnötig in Java;
- klare Thread-Grenzen;
- eindeutige Source of Truth je Zustand.

---

## 9. Plugin-Lifecycle

Der neue Plugin-Lifecycle muss alle von `main` benötigten Systeme korrekt:

- initialisieren;
- registrieren;
- verknüpfen;
- laden;
- aktivieren;
- deaktivieren;
- speichern;
- schließen.

Die Composition Root darf orchestrieren, aber keine fachliche God-Object-Rolle übernehmen.

Jede Ressource erhält:

- einen Besitzer;
- einen Startpunkt;
- einen Shutdown-Punkt;
- Fehlerbehandlung;
- definierte Abhängigkeiten.

---

## 10. Persistence und Daten

Alle persistierten Daten aus `main` müssen vollständig erfasst werden.

Für jeden Datenbestand:

- Source of Truth
- Schema
- Migration
- Load
- Save
- Revision
- Concurrency
- Fehlerfall
- Recovery
- Shutdown
- Cache
- Invalidierung.

Persistenz muss das Verhalten von `main` erhalten, darf intern aber neu strukturiert werden.

MySQL/Hikari bleibt Bestandteil der technischen Basis.

Datei-/JSON-/YAML-Daten werden nicht automatisch als Runtime-Persistence behandelt. Es muss festgestellt werden, ob es sich um:

- Konfiguration;
- statischen Content;
- persistenten Zustand;
- Cache;
- Migration;
- Entwicklungsartefakt

handelt.

---

## 11. Performance und Threading

Ziel ist ein stabiler Serverbetrieb mit Fokus auf 20 TPS.

Verbindlich:

- DB-I/O async
- Datei-I/O async
- externe HTTP-Aufrufe async
- Paper-/World-/Entity-/Player-Mutationen auf dem Serverthread
- keine unnötigen Polling-Loops
- eventbasierte Verarbeitung bevorzugen
- keine Scheduler-Kaskaden ohne fachlichen Grund
- keine permanenten Tasks, wenn Events genügen
- Snapshots vor asynchronem Schreiben
- klare Cancellation-/Shutdown-Strategien
- Caches mit definierter Invalidierung
- keine Live-Entity-/Player-/World-Objekte als globale Persistenzdaten.

Performanceoptimierung darf die Referenzfunktionalität nicht verändern.

---

## 12. Vollständige fachliche Bereiche

Alle folgenden Bereiche gelten als Bestandteil des Clean Rebuilds und dürfen nicht als „optional“ oder „später vielleicht“ betrachtet werden:

### Core
- Bootstrap
- Plugin Lifecycle
- Config
- Scheduler
- Resource Ownership
- Shutdown

### Player
- Profile
- Join/Quit
- Stats
- XP
- Level
- Geld
- Playtime
- Character Card
- Equipment
- Settings
- Persistenz

### Items
- Definitionen
- Instanzen
- PDC
- Data Components
- Rarity
- Level
- Gearscore
- Stats
- Unique State
- Soulbound
- Food
- Equipment Sets
- Boss Rewards

### Economy
- Währung
- Transaktionen
- Sicherheit
- Pickups
- Persistenz

### NPC
- Definition
- Runtime
- Spawn
- Despawn
- Chunk Lifecycle
- Interaction
- Look
- Name
- Protection
- Skin
- Skin-Persistenz
- alle NPC-Typen

### Dialog
- native Paper Dialog API
- Dialogdefinition
- Bedingungen
- Optionen
- Aktionen
- Navigation
- Quest-Verknüpfung
- Story-Verknüpfung
- NPC-Verknüpfung.

### Quests
- Definitionen
- Progress
- HUNT
- COLLECT
- TALK_TO_NPC
- Crafting
- passive Bedingungen
- globale Bedingungen
- Zeitbedingungen
- Voraussetzungen
- Folgequests
- Belohnungen
- Companion-Rewards
- Navigation
- Questlog
- Questdetails
- Start
- Abbruch
- Abschluss
- Persistence.

### Story/Lore
- Kapitel
- Fortschritt
- NPC-Dialoge
- Bücher
- Voraussetzungen
- Content
- Persistenz.

### Party
- Erstellung
- Invite
- Accept
- Leave
- Kick
- Transfer
- Disband
- Info
- GUI
- Leader
- Mitglieder
- Share Range
- Lifecycle
- Quick Actions.

### Guild
- Erstellung
- Invite
- Accept
- Leave
- Info
- Disband
- Mitglieder
- Leader
- Gildenbank
- Guild Compass
- Quick Actions
- Persistence.

### Bank/Trading
- persönliche Bank
- Bankseiten
- Einzahlen
- Auszahlen
- Handelsfach
- Handelsfach-Persistence
- Trade Depot
- Kauf
- Verkauf
- Preise
- Laufzeiten
- Rücknahme
- Ablauf
- Auszahlungen
- Migration.

### Professions/Crafting
- Berufe
- Level
- Rezepte
- Kategorien
- Zutaten
- Mengen
- Freischaltungen
- Crafting
- Fehler
- Ergebnisse
- NPC-Einstieg
- Quest-Integration
- Darstellung.

### Shops
- Spieler-Shop
- Admin-Shop
- Shop Editor
- Kauf
- Verkauf
- Preise
- Inventar
- Darstellung
- Navigation.

### Companions
- Definition
- Summon
- Dismiss
- Rename
- Follow
- Mount
- Combat
- Targeting
- Schaden
- Progression
- XP
- Rarity
- Stats
- Boss Unlock
- Skin
- Equipment
- Equipment UI
- Equipment Persistence
- Runtime
- Rewards
- Quick Actions.

### Regionen/World
- Region Definition
- Flags
- Policy
- Spawn
- Transition
- Schutz
- Editor
- Commands
- Persistence.

### Combat/Bosse
- Bossdefinitionen
- Combat
- Rewards
- Stats
- Events
- Progression.

### Presentation
- sämtliche GUIs aus `main`
- sämtliche Dialoge
- sämtliche sichtbaren Texte
- Farben
- Icons
- Navigation
- Fehlermeldungen
- Statusanzeigen
- Scoreboard.

### Administration
- Debug
- Edit
- Quest Administration
- Dialogue Administration
- NPC
- Boss
- Item
- Player
- Shop
- Region
- Permissions
- Tab Completion.

### API
- `ApiVersion`
- `PixelRPGProvider`
- öffentliche Services
- Service Registration
- Kompatibilitätsverhalten.

### Resourcepack / Content
- alle Dateien
- Models
- Textures
- Sounds
- Fonts
- GUI-Assets
- Content-Referenzen
- IDs.

---

## 13. UI-Regel

Die Präsentation aus `main` ist Teil des SOLL.

Es ist **nicht erlaubt**, vorhandene GUIs pauschal durch Dialoge zu ersetzen.

Native Paper Dialoge sind für die Dialog-/Interaktionsmechanik verbindlich.

Inventar-GUIs bleiben Inventar-GUIs, wenn `main` sie so verwendet.

Insbesondere müssen die Referenzsysteme vollständig geprüft und reproduziert werden, darunter soweit in `main` vorhanden:

- PartyGUI
- QuestLogGUI
- QuestDetailGUI
- CraftingGUI
- ShopGUI
- ShopEditorGUI
- TradeDepotGUI
- TradeDepotSellGUI
- Bank-/Inventory-Oberflächen
- Companion-Equipment-Oberflächen
- sonstige sichtbare Menüs.

Die interne Implementierung darf neu sein. Die Spielererfahrung darf nicht unbeabsichtigt verändert werden.

---

## 14. NPC- und Dialogregel

NPC-Systeme werden neu gebaut, aber das Verhalten aus `main` bleibt verbindlich.

Für jeden NPC-Typ ist vollständig zu prüfen:

- Spawn
- Despawn
- Chunk
- Skin
- Skin-Persistenz
- Name
- Look
- Interaktion
- Bedingungen
- Permission
- Dialog
- Folgeaktion
- GUI-Einstieg
- Quest-/Story-/Shop-/Bank-/Travel-Verknüpfung.

Dialoge verwenden ausschließlich die aktuelle Paper-26.2-Native-API.

---

## 15. Content-Regel

Statischer Content muss vom Engine-Code getrennt werden.

Das bedeutet nicht, dass jede Ressource in eine einzelne JSON-Datei gezwungen wird.

Ziel ist:

- stabile IDs
- eindeutige Definitionen
- keine ungewollten Duplikate
- nachvollziehbare Referenzen
- migrationssichere Struktur
- leicht erweiterbare Content-Formate.

Beim Konsolidieren darf **kein fachlicher Content verloren gehen**.

---

## 16. Rebuild-Vorgehen

### Phase A — Reset

1. `src/` auf `rebuild` vollständig löschen.
2. Build-/Projektgrundlage prüfen.
3. `main` als Referenz einfrieren.
4. Audit neu aufbauen.

### Phase B — Forensische Inventarisierung

Für jeden Bereich:

1. Dateien aus `main` erfassen.
2. Klassenrollen bestimmen.
3. öffentliche APIs bestimmen.
4. Eventketten bestimmen.
5. Datenflüsse bestimmen.
6. Persistence bestimmen.
7. Content bestimmen.
8. sichtbare Wirkung bestimmen.
9. Abhängigkeiten bestimmen.
10. tote/redundante Strukturen beweisen.

### Phase C — Zielarchitektur

Erst nach der Inventarisierung:

- Domain schneiden
- Services definieren
- Runtime-Grenzen definieren
- Persistence definieren
- Presentation definieren
- API definieren
- Content definieren.

### Phase D — Neuimplementierung

Abhängigkeitsorientiert neu bauen.

Nicht nach Dateinamen kopieren.

### Phase E — Integration

Jedes System muss mit den anderen Systemen wieder korrekt verbunden werden.

### Phase F — Paritätsprüfung

Nicht nur Klassen vergleichen.

Prüfen:

- gleiche Inputs
- gleiche Bedingungen
- gleiche Zustände
- gleiche Aktionen
- gleiche Outputs
- gleiche Darstellung
- gleiche Persistence
- gleiche Lifecycle-Reaktionen.

### Phase G — Cleanup

Erst wenn die Parität bewiesen ist:

- unbenutzte Klassen löschen
- doppelte Services entfernen
- tote Adapter entfernen
- alte Migrationsreste entfernen, sofern fachlich nicht benötigt
- temporäre Rebuild-Strukturen entfernen.

---

## 17. Harte Anti-Abkürzungsregeln

Nicht zulässig:

1. `main/src` blind nach `rebuild/src` kopieren.
2. Einen alten Manager nur umbenennen.
3. Ein fehlendes Feature als „legacy“ abstempeln, ohne Wirkung zu prüfen.
4. GUI durch Dialog ersetzen, nur weil Dialog moderner erscheint.
5. Einen vorhandenen Service-Namen als Funktionsnachweis verwenden.
6. Fake-API-/Fallback-Implementierungen behalten.
7. Build-Erfolg als Funktionsparität werten.
8. Dateianzahl als Vollständigkeitsnachweis verwenden.
9. Content-Dateien zusammenwerfen, ohne IDs/Referenzen zu prüfen.
10. Listener ohne nachgewiesene Registrierung als fertig markieren.
11. funktionale Unterschiede stillschweigend einführen.
12. alte technische Altlasten in den Clean Rebuild retten, nur weil sie existieren.

---

## 18. Listener-/Event-Nachweis

Für jeden Listener/Eventpfad muss nachvollziehbar sein:

`Event -> Listener -> Domain/Service -> Zustand -> Persistenz/Presentation`

Für jeden Scheduler:

`Trigger -> Task -> Wirkung -> Lifecycle/Cancellation`

Für jeden Command:

`Input -> Permission -> Validation -> Action -> Result -> Presentation`

Für jede GUI:

`Open -> State -> Click/Input -> Action -> State update -> Navigation/Close`

Für jeden Dialog:

`NPC/Trigger -> Dialog -> Auswahl -> Aktion -> nächster Zustand`

Für Persistence:

`Runtime State -> Snapshot -> I/O -> Storage -> Load -> Runtime State`

---

## 19. Public API

Der externe API-Vertrag von `main` wird vollständig inventarisiert.

Interne Architektur darf anders sein.

Eine neue Provider-/Facade-Struktur ist ausdrücklich erlaubt und erwünscht, solange:

- Services korrekt registriert werden;
- Verhalten erhalten bleibt;
- keine Fake-Werte entstehen;
- keine God-Manager zurückkehren.

---

## 20. Build- und Qualitätsprüfung

Pflicht:

`gradle clean build --no-daemon --stacktrace`

Zusätzlich müssen alle vorhandenen Verifikationstasks erhalten bleiben.

Prüfen:

- Java 25
- Paper 26.2
- Mojang-Mappings
- kein CraftBukkit
- kein Legacy-NMS
- kein `ChatColor`
- keine statischen Live-Server-Referenzen
- korrekte Shadow-Relocations
- korrekter JDBC-Service-Eintrag
- `check` führt alle Prüfungen aus
- CI erfolgreich.

Build-Erfolg ist nur ein technischer Mindestnachweis.

---

## 21. Release-Abnahme

Der Clean Rebuild ist **nicht fertig**, solange nicht jede relevante SOLL-Funktion aus `main` einen Nachweis besitzt.

### Release nur wenn

- vollständiges main-Inventar abgeschlossen;
- jede Funktion einer Zielimplementierung zugeordnet;
- alle sichtbaren Spielerabläufe reproduziert;
- alle Commands reproduziert;
- alle GUIs reproduziert;
- alle Dialoge reproduziert;
- alle NPC-Typen reproduziert;
- alle Quest-/Story-Abläufe reproduziert;
- alle Persistence-Flows reproduziert;
- alle APIs reproduziert;
- alle Content-Referenzen geprüft;
- alle Integrationen geprüft;
- alle toten/redundanten Strukturen entfernt;
- keine Fake-/Fallback-Produktivpfade vorhanden;
- Build/CI vollständig grün;
- keine unbeabsichtigten Unterschiede zu `main`.

### Abschlussdefinition

> **CLEAN REBUILD COMPLETE = main-Verhalten 1:1 nachgewiesen + interne Architektur vollständig neu und sauber + tote/Legacy-Strukturen entfernt + Build/CI verifiziert.**

---

## 22. Aktueller Status

**RESET / CLEAN REBUILD START**

Der bisherige `rebuild/src`-Stand ist ausdrücklich **nicht mehr die Basis**.

Nächster verbindlicher Schritt:

1. `src/` vollständig entfernen.
2. Audit als Master-Spezifikation verwenden.
3. `main` vollständig inventarisieren.
4. neue Zielarchitektur erst nach dieser Inventarisierung festlegen.
5. anschließend den kompletten Rebuild neu implementieren.

Keine frühere Fortschrittsangabe aus einem älteren Audit darf den neuen Clean-Rebuild-Scope verkleinern.

---

## 23. Arbeitsregel für alle folgenden Änderungen

Vor jeder Änderung auf `rebuild`:

1. diese `audit.md` lesen;
2. `main` prüfen;
3. tatsächlichen aktuellen `rebuild`-Stand prüfen;
4. betroffene Funktion vollständig verstehen;
5. Zielarchitektur definieren;
6. neu implementieren statt blind kopieren;
7. End-to-End prüfen;
8. Audit aktualisieren.

Bei Unsicherheit wird geprüft oder nachgefragt.

**Nicht raten. Keine erfundenen APIs. Keine stillen Funktionsverluste.**

---

# MASTER-SATZ

> **Wir bauen `main` komplett neu.**
>
> **Alles, was `main` funktional, visuell, technisch als Schnittstelle oder im Spieler-/Adminablauf bereitstellt, gehört zum Rebuild.**
>
> **Wir übernehmen nicht die alte Architektur. Wir übernehmen das SOLL-Verhalten.**
>
> **Wir entfernen Altlasten und totes Zeug erst dann, wenn nachgewiesen ist, dass dadurch kein SOLL-Verhalten verloren geht.**
>
> **`rebuild` wird ein vollständiger Clean Rebuild von `main` — nicht ein Teil-Rebuild und nicht eine Sammlung einzelner Fixes.**

---

## 24. Rebuild-Fortschritt — 2026-09-24

### RESET
**VERIFIED**

- `rebuild/src/` war vor Beginn des neuen Rebuilds leer bzw. nicht vorhanden.
- Die historische Rebuild-Implementierung wurde nicht als technische Basis weiterverwendet.
- `main` Referenz bleibt der Commit `b02747a21cde6d1308c59a7f027cda80877cfe50`.

### Referenzinventar
**OPEN**

Aktuell aus `main` verifiziert:

- 247 Java-Dateien
- 17 Dateien unter `src/main/resources`
- vollständiger Resourcepack-Bestand unter `resourcepack/`
- technische Buildbasis und `paper-plugin.yml`
- öffentliche API-Oberfläche
- Plugin-/Bootstrap-Lifecycle
- bestehende Persistence-Grundlage
- bestehende Player-/Economy-/Item-/Party-/Guild-/Statistics-Einstiegspunkte.

Die 247 Java-Dateien sind **nicht** als Rebuild übernommen worden. Sie bleiben ausschließlich Referenzmaterial für Verhalten, Datenflüsse und Paritätsprüfung.

### Rebuild Foundation
**REBUILT**

Neu aufgebaut wurde eine eigenständige Foundation unter `rebuild/src/main/java`:

- neuer Plugin-Einstiegspunkt;
- separater Bootstrap-Einstiegspunkt;
- Lifecycle-Besitzmodell;
- API-Vertrag für Economy, Guild, Item, Party und Statistics;
- Player-Profile-Domainmodell;
- asynchroner Profile-Lifecycle;
- YAML-Persistence;
- MySQL/Hikari-Persistence;
- Datenbankschema-Grundlage;
- Economy-Service;
- Guild-Service-Grundlage;
- Party-Service-Grundlage;
- Statistics-Service-Grundlage;
- Item-Definition-Registry;
- Item-Service mit PDC-basierter Identität;
- Java-25-Virtual-Thread-I/O für Persistence.

### Content
**REBUILT**

Die vollständigen bestehenden statischen Content- und Resourcepack-Dateien aus `main` wurden als Referenz-/Content-Bestand in den Clean-Rebuild übernommen. Sie wurden nicht mit neuer Java-Architektur vermischt.

### Noch OPEN

Die folgenden Bereiche sind weiterhin vollständig offen und dürfen nicht als erledigt betrachtet werden:

- vollständige NPC-/Skin-Runtime;
- native Dialogsysteme;
- Character Card;
- alle GUIs;
- Commands;
- Quests und Quest-Navigation;
- Story;
- Berufe/Crafting;
- vollständige Items/Equipment;
- Combat;
- Bosse;
- Companions;
- Party-Funktionalität;
- Guild-Funktionalität;
- Bank;
- Trade Depot;
- Shops;
- Regionen;
- Scoreboard/Playtime;
- Content-Konsolidierung und Referenzvalidierung;
- öffentliche Events;
- vollständige Persistence aller Domänen;
- vollständige Integrationsprüfung;
- End-to-End-Paritätsprüfung gegen `main`;
- Clean-Rebuild-Abnahme.

### Nachweisregel

Der Status **REBUILT** bedeutet hier ausschließlich, dass eine neue Zielimplementierung für den jeweiligen Foundation-Bereich angelegt wurde. Er bedeutet ausdrücklich **nicht**, dass funktionale Parität mit `main` bereits nachgewiesen ist.

Der Release-Status bleibt daher:

**NOT COMPLETE**


## 25. Forensische Paket-Inventarisierung — 2026-09-24

**VERIFIED**

Die vollständige Java-Inventur von main wurde erneut direkt gegen den Git-Tree b02747a21cde6d1308c59a7f027cda80877cfe50 geprüft: **247 Java-Dateien**.

Erfasste fachliche Cluster:

- Core / API / Events
- Boss / Boss Patterns
- Combat / Loot / Scaling / Skills
- Commands / Subcommands
- Companions
- Config
- Dialogue / Bank / Character Card / Quick Actions
- Economy
- Equipment
- GUI
- Guild
- Items / Food / Unique / Soulbound
- NPC / NPC Behaviors / Skin
- Party
- Player / Persistence
- Profession / Crafting
- Quest / Navigation / Passive Checks
- Region / World Rules
- Scoreboard / Playtime
- Shop
- Statistics
- Storage
- Story
- Trade Depot
- Travel / Guild Compass

Die Inventur bestätigt, dass der Rebuild-Scope tatsächlich wesentlich größer als die bisherige Foundation ist. Kein Cluster wird aufgrund vorhandener Foundation als erledigt markiert.

## 26. Item-Content-Rebuild — 2026-09-24

**REBUILT / NOT VERIFIED**

Die Item-Foundation wurde gegen die tatsächlichen main-Daten nachgeschärft.

Neu bzw. korrigiert:

- ItemCategory auf die tatsächlichen Referenzkategorien erweitert;
- ItemStatProfile neu eingeführt;
- GearCategoryRegistry neu eingeführt;
- ItemDefinition um die tatsächlich benötigten Definitionsfelder erweitert;
- ItemDefinitionRegistry lädt jetzt item-definitions.json, boss-reward-items.json und food-definitions.json;
- IDs werden normalisiert;
- Material-/Kategorie-Konsistenz wird validiert;
- doppelte IDs werden abgewiesen;
- ungültige Level-/Cooldown-/Gearscore-Werte werden abgewiesen.

Wichtig: Der Item-Runtime-Stack ist damit noch **nicht** funktional paritätsgeprüft. Insbesondere Equipment, Food-Effekte, Unique-Claims, Soulbound-Verhalten, Weapon Abilities, Set Effects und vollständige Item-Presentation bleiben OPEN.

## 27. Nächste verbindliche Rebuild-Reihenfolge

Nach der forensischen Gesamtinventur wird nicht nach Dateinamen, sondern nach Abhängigkeiten weitergebaut:

1. Core / Player Lifecycle / Persistence vollständig abschließen
2. Item / Equipment / Food / Unique / Soulbound
3. Stats / Economy
4. Profession / Crafting
5. Quest / Navigation
6. Party / Guild / Bank / Trade Depot
7. NPC / Skin / Behavior
8. Dialog / Character Card / Quick Actions
9. Shop
10. Companion
11. Combat / Skills / Boss
12. Region / World Rules
13. Story
14. GUI / Scoreboard / Playtime
15. Commands / Administration
16. Integration / Parity / Cleanup / Build / CI

Kein Bereich erhält den Status VERIFIED, bevor der konkrete Ablauf gegen main nachvollzogen und nachgewiesen wurde.


## 28. Public Event Contract — 2026-09-24

**REBUILT / NOT VERIFIED**

Die sieben im main-Inventar dokumentierten öffentlichen Eventtypen wurden als eigenständiger API-Vertrag neu angelegt:

- BossDefeatedEvent
- PlayerCombatEnterEvent
- PlayerCombatExitEvent
- PlayerLevelUpEvent
- PlayerRegistrationEvent
- PlayerUnregistrationEvent
- QuestCompletedEvent

Die Events sind aktuell noch nicht vollständig an alle fachlichen Produzenten gekoppelt. Deshalb bleibt der Status bewusst REBUILT und nicht VERIFIED.


## 29. Technische CI-Prüfung — 2026-09-24

**IN PROGRESS**

Für den Clean-Rebuild wurde ein **Draft-PR #19** ausschließlich als CI-Prüfpfad gegen main geöffnet. Der PR wird nicht gemerged; rebuild bleibt der einzige Arbeitsbranch.

- Referenzbasis: main / b02747a21cde6d1308c59a7f027cda80877cfe50
- Arbeitsbranch: rebuild
- aktueller Rebuild-Head: 8ff450bf0b90f016d8e9b8510f6994dcd3e9701f
- GitHub Actions Build Run: 35946662849
- aktueller Run-Stand: Build-Schritt läuft

Der Build-Run wird nicht als erfolgreich gewertet, solange Gradle Build und die anschließenden Boundary-/Artifact-Prüfungen nicht erfolgreich abgeschlossen sind.


## 30. Core-/Persistence-Nachschärfung — 2026-09-24

**REBUILT / NOT VERIFIED**

Der Core-/Player-Lifecycle wurde gegenüber der ursprünglichen Foundation nach dem erneuten Abgleich mit `main` nachgeschärft:

- native Paper-Dialog-Registrierung aus dem Referenz-Bootstrap wiederhergestellt;
- JSON-Content-Manager für sichere, plugin-owned Content-Dateien ergänzt;
- PlayerProfile um Dirty-State, Mutation-Revision und Save-Snapshots erweitert;
- PlayerProfileManager auf UUID-serielle asynchrone I/O-Ketten umgestellt;
- Pre-Login-Load, Join-Aktivierung, Quit-Persistenz und Shutdown-Flush zusammengeführt;
- EconomyAPI und GuildAPI werden durch den PlayerProfileManager registriert, statt durch konkurrierende parallele Foundation-Services;
- PlayerLevelUpEvent, PlayerRegistrationEvent und PlayerUnregistrationEvent sind an die Profile-Lifecycle-Operationen gekoppelt;
- Party Share Range verwendet jetzt die tatsächlichen Online-Spielerpositionen und keine Platzhalterantwort mehr;
- StatisticsAPI verwaltet Standard-, Custom- und Character-Stat-Zustände statt Null-/Fixwert-Platzhaltern.

Die Änderungen sind **noch nicht VERIFIED**. Insbesondere fehlen weiterhin die vollständige Domain-Persistence aus `main`, die fachliche Guild-/Party-Anwendungsschicht, Equipment/Quest/Profession-Daten im Profile-Snapshot und die End-to-End-Paritätsprüfung.

### Aktueller Rebuild-Head

`ff0654854a20c777bac73b0a81430e7395147946`

### CI

Die technische CI-Prüfung muss nach diesem Stand erneut vollständig erfolgreich durchlaufen werden. Ein grüner Build gilt weiterhin nur als technischer Mindestnachweis und nicht als Rebuild-Abnahme.


## 31. Item / Equipment / Profession Foundation — 2026-09-24

**REBUILT / NOT VERIFIED**

Der nächste abhängige Block wurde neu aufgebaut und mit dem vorhandenen main-Content abgeglichen:

### Items / Equipment

- zentrale `RPGKeys` für Item-PDC und Runtime-PDC eingeführt;
- Item-Rarity mit den Referenz-Multiplikatoren und Drop-Gewichten nachgezogen;
- Item-Builder für Level-/Rarity-Scaling und Item-Stat-Rolls ergänzt;
- ItemService auf definierte Items, generierte Items, Unique-Claims und Economy-Sicherheitsprüfung ausgerichtet;
- FoodDefinition/FoodDefinitionRegistry/FoodService ergänzt;
- aktuelle Paper-Data-Components für Food verwendet;
- Soulbound- und Unique-Item-Grundlage ergänzt;
- EquipmentSlot, StatEngine und reaktive EquipmentService-Grundlage ergänzt;
- CharacterStatType wird auf die Runtime-Statberechnung geführt.

### Player / Professions

- PlayerProfile um Berufslevel, Berufs-XP, erlernte Berufe, freigeschaltete Rezepte und Waypoints erweitert;
- YAML- und MySQL-Persistence um diese Zustände erweitert;
- Profession-Domain für alle neun Referenzberufe ergänzt;
- CraftingCategory, CraftRecipe und CraftingRecipeRegistry ergänzt;
- Crafting-Rezeptdaten werden aus dem bestehenden Content geladen und validiert;
- ProfessionService und CraftingService ergänzt;
- Recipe-Unlock und Profession-XP sind an die Profile-Persistence angebunden.

### Noch offen

Der Block ist **nicht VERIFIED**. Es fehlen insbesondere die vollständige Equipment-Persistence in der Profile-Domain, Set-Effekte, vollständige Food-/Unique-/Soulbound-Lifecycle-Integration, die vollständige Crafting-GUI/NPC-/Quest-Verknüpfung sowie die End-to-End-Paritätsprüfung gegen main.

### Aktueller Rebuild-Head

`f397d97fe18ebe06e4524df2db977bb1b53c091f`


## 32. Profession Runtime Integration — 2026-09-24

**REBUILT / NOT VERIFIED**

Die Profession-Foundation wurde bis zur Runtime-Anbindung erweitert:

- ProfessionSystem ergänzt;
- CraftingRecipeRegistry lädt die bestehende große Rezeptdefinition und validiert Kategorien, Materialien, Level, Tränke und Verzauberungen;
- CraftingService führt freigeschaltete Rezepte aus und vergibt Berufs-XP;
- ProfessionService verwaltet Lernen, Rezeptfreischaltung und XP/Level;
- ProfessionActivityListener verbindet relevante Vanilla-Aktivitäten mit den neun Berufen;
- PlayerProfile-Persistence enthält Berufslevel, Berufs-XP, erlernte Berufe, Rezepte und Waypoints;
- Plugin-Lifecycle registriert den Profession-Runtime-Listener.

Der Status bleibt **NOT VERIFIED**, da insbesondere die vollständige CraftingGUI, NPC-Einstiege, Quest-Verknüpfungen, sämtliche Rezept-Sonderfälle und End-to-End-Parität gegen main noch fehlen.

### Aktueller Rebuild-Head

`dac9162117a66634ce7b27174c3759943b770989`


## 33. Quest Persistence / Lifecycle Foundation — 2026-09-24

**REBUILT / NOT VERIFIED**

Der Quest-Kern wurde erweitert:

- QuestType, Quest, QuestProgress und QuestRepository ergänzt;
- bestehende Quest-JSON-Dateien werden über JsonDataManager geladen und referenziert die aktuelle Item-/Profession-Domain;
- PlayerProfile enthält completed quests und aktive QuestProgress-Zustände;
- YAML- und MySQL-Persistence speichern aktive und abgeschlossene Quests;
- QuestService übernimmt Accept/Abandon/Progress/Complete, Party-Share und zeitgesteuerte Ablaufverwaltung;
- QuestLifecycleListener stellt Timer nach Join wieder her und stößt Persistence beim Quit an;
- QuestCompletedEvent wird beim Abschluss ausgelöst.

Dieser Block ist **NOT VERIFIED**. Navigation zu Strukturen/Biomen, NPC-Questgeber, vollständige Quest-GUIs, globale Events, Companion-Rewards und vollständige Verhaltensparität zu main sind weiterhin offen.

### Aktueller Rebuild-Head

`2ef8c093daee16667e2e1082a91603cd18c73b9b`


## 34. MySQL Schema Initialization Correction — 2026-09-24

**REBUILT / NOT VERIFIED**

Der PlayerProfileManager initialisiert vor der ersten MySQL-Repository-Nutzung jetzt explizit das vorhandene Schema inklusive Migrationen. Damit ist die bestehende DatabaseManager-Schema-/Migrationslogik tatsächlich Bestandteil des Startup-Pfads und nicht nur tote Infrastruktur.

Der Status bleibt **NOT VERIFIED**, bis der aktuelle Rebuild-Head die vollständige CI-Pipeline einschließlich Source-Boundary- und Artifact-Prüfungen erfolgreich durchlaufen hat.

### Aktueller Rebuild-Head

`b999e3080d1d209bd753bc6efa197d45441bb4b7`


## 35. Guild Runtime Rebuild — 2026-09-24

**REBUILT / NOT VERIFIED**

Der Guild-Block wurde als eigenständige Runtime neu aufgebaut und in die aktuelle Clean-Rebuild-Composition-Root integriert.

Nachgewiesene Bestandteile:

- immutable Guild-Domain-Snapshot;
- persistenter GuildManager mit YAML-Snapshot-Persistence und atomischem Schreiben;
- Guild-Erstellung mit Referenzregeln: Level 20, 2.500 Gold, 3–24 Zeichen, eindeutiger Name;
- Einladung, Annahme, Verlassen und Auflösen;
- Mitglieds-/Leader-Zuordnung;
- öffentliche GuildAPI-Delegation auf den Runtime-Manager;
- aktuelle Paper BasicCommand-Registrierung für die player-facing Guild-Commands;
- Adventure-Components statt Legacy-Chat/Farb-APIs;
- Shutdown-Ownership und Service-Unregistration.

Bewusst nicht als VERIFIED markiert: Guild-GUI/Dialoge, Gildenbank, Guild Compass, Quick Actions, Scoreboard-Anbindung und vollständige Paritätsprüfung aller Guild-Abläufe gegen main.

### Aktueller Rebuild-Head

`2f7c2292e15fbd0d6b9d44d7efaaa67c5330ec8c`

### CI

Für diesen Head läuft GitHub Actions Run `35948125975`. Der Run ist noch nicht abgeschlossen; Build-, Boundary- und Artifact-Prüfungen sind daher weiterhin offen.


## 36. Personal Bank / Trade Depot Foundation — 2026-09-24

**REBUILT / NOT VERIFIED**

Der abhängige Storage-/Trading-Block wurde als eigenständige Runtime neu aufgebaut:

- persönliche Bank mit zwei 54-Slot-Seiten;
- separates 54-Slot-Handelsfach;
- defensive ItemStack-Snapshots;
- asynchrone, atomische YAML-Persistence über Java-25-Virtual-Thread-I/O;
- Legacy-Aufteilung des früher kombinierten Bank-/Handelsfach-Bestands wird beim ersten Start berücksichtigt;
- TradeDepotListing als immutable Domain-Snapshot;
- Handelsangebote mit 7 Tagen Laufzeit;
- 5-%-Verkaufsgebühr;
- Kauf, Rücknahme und Ablauf-Rückgabe ins Handelsfach;
- ausstehende Verkäufer-Auszahlungen für offline geladene Profile;
- Economy-sichere RPG-Items als Voraussetzung für Handelsangebote;
- exakte Ablaufplanung statt permanenter Polling-Schleifen;
- Integration in den Plugin-Lifecycle inklusive Shutdown.

Bewusst noch OPEN: vollständige Bank-/Trade-Depot-GUIs, native Bank-Dialoge, NPC-Einstiege, Kauf-/Verkaufsdarstellung, vollständige Parität der sichtbaren Abläufe und die End-to-End-Integration mit Guild-Bank/Quick-Actions.

### Aktueller Rebuild-Head

`94cc760560d1b2ca7034b87b3361a9caeee83203`


## 37. Party Command Runtime — 2026-09-24

**REBUILT / NOT VERIFIED**

Der bestehende PartyManager wurde um den player-facing Command-Pfad ergänzt:

- aktuelle Paper `BasicCommand` API;
- `/pixelrpgparty invite|accept|leave|kick|transfer|disband|info`;
- Permissions über `rpg.member`;
- Tab-Completion für Party-Unterbefehle und Online-Spieler;
- Registrierung, Validierung und Rückmeldungen auf Basis des bestehenden PartyManagers;
- keine Legacy-Bukkit-Command-Implementierung.

Die PartyGUI und Quick-Actions bleiben weiterhin OPEN; deshalb ist der Party-Bereich noch nicht VERIFIED.

### Aktueller Rebuild-Head

`ae409ae8928a86bafcd5cccf551b6f7d56812067`


## 38. GUI-/Questlog-Grundlage — 2026-09-24

**REBUILT / NOT VERIFIED**

Die Präsentationsgrundlage wurde neu aufgebaut:

- generisches `AbstractGUI`-/Holder-/Listener-Modell für Inventar-GUIs;
- zentrale Click-/Close-Verarbeitung mit Schutz vor Eingriffen in das Player-Inventar;
- Questlog mit Statussortierung (verfügbar, aktiv, nicht verfügbar, abgeschlossen);
- Questdetailansicht mit Ziel, Menge, Level, Belohnungen, Fortschritt und Abbruchaktion;
- aktueller Paper-BasicCommand-Pfad für `/pixelrpgquestlog`;
- GUI-Lifecycle in die Composition Root integriert.

Noch nicht VERIFIED: Rücknavigation in das vollständige main-Reception-/Dialogsystem, sämtliche Quest-Navigationselemente, QuestText-Lokalisierung/Formatierung und End-to-End-Parität.

### Aktueller Rebuild-Head

`e57ac24ddebe736d4648a0f2938a85e9bb7045ef`


## 39. NPC Runtime / Mannequin / Skin Persistence Foundation — 2026-09-24

**REBUILT / NOT VERIFIED**

Der NPC-Runtime-Block wurde auf Basis der aktuell verifizierten Paper-26.2-Mannequin-API neu aufgebaut. Die aktuelle Paper-API bestätigt `Mannequin#getProfile()`, `Mannequin#setProfile(ResolvableProfile)` und die 26.2-`ResolvableProfile`-Mechanik. citeturn1search3turn1search0

Neu aufgebaut:

- NpcType und RPGNpc-Domain;
- persistenter NpcManager;
- data-driven NPC-IDs, Typen, Positionen, Professionen und Skin-Quellen;
- Chunk-Index und Chunk-Load/Unload-Lifecycle;
- aktuelle Paper-26.2-Mannequin-Entities;
- NPC-PDC-Identität über `RPGKeys.Npc`;
- Invulnerability, AI-Deaktivierung, Kollisionsschutz und kontrolliertes Despawn;
- Skin-Auflösung über aktuelle Paper-`PlayerProfile`-/`ResolvableProfile`-Mechanik;
- persistierte Texture-Property inklusive Signatur;
- serverseitige Skin-Wiederherstellung nach Neustart;
- Client-Refresh für betroffene Spieler;
- asynchrone atomische NPC-Persistence;
- Plugin-Lifecycle-Integration.

Noch OPEN: externe Skin-Quellen/URL-Resolver, NPC-Interaktion, Behavior Registry, Look-/Nameplate-Runtime, Dialog-/Quest-/Shop-/Bank-Verknüpfungen, Admin-NPC-Commands und vollständige Paritätsprüfung gegen main.

### Aktueller Rebuild-Head

`dceeea4e6a7f350abfc8186572c8bb3a8636cc76`
