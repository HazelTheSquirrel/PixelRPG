# PixelRPG Rebuild Audit

## 1. Zweck und verbindliche Arbeitsanweisung

Diese Datei ist die **verbindliche Arbeitsanweisung für den vollständigen Neuaufbau des Projekts auf dem Branch `rebuild`**.

Der Branch `main` ist der eingefrorene technische und funktionale **SOLL-/Referenzzustand**. Der Branch `test` ist der bisherige Entwicklungsstand. Der Branch `rebuild` ist ab diesem Audit der einzige Ort für den strukturellen Neuaufbau.

### Primäres Ziel

Das bestehende Serververhalten muss nach dem Neuaufbau funktional erhalten bleiben. Die interne Implementierung darf vollständig neu strukturiert werden.

Der Neuaufbau soll:

- fachliche Verantwortlichkeiten klar trennen;
- unnötige oder doppelte Klassen und Datenmodelle entfernen;
- gewachsene Übergangslösungen nicht blind übernehmen;
- I/O und teure Operationen vom Main Thread fernhalten;
- Paper-26.2- und Java-25-APIs konsequent verwenden;
- Persistenz, Lifecycle und Shutdown deterministisch gestalten;
- Content von Engine-/Infrastruktur-Code trennen;
- das native Paper/Minecraft-Dialog-System als Grundlage für zukünftige NPC-, Story- und Quest-Interaktionen verwenden;
- eine Architektur schaffen, auf der später Texte, Lore, Geschichte, Quests und weitere Content-Systeme erweitert werden können, ohne die technische Basis erneut umzubauen.

**Wichtig:** Nicht die Anzahl der Klassen minimieren. Ziel ist eine minimale, verständliche und fachlich begründete Architektur.

---

## 2. Unveränderliche technische Basis

Die folgenden Vorgaben dürfen beim Rebuild nicht verändert werden, sofern kein ausdrücklicher Auftrag dafür vorliegt:

- Paper 26.2
- Paper Dev Bundle: `26.2.build.121-stable`
- `io.papermc.paperweight.userdev` 2.0.0-beta.21
- Shadow 9.6.1
- Java 25
- Mojang-Mappings
- `paper-plugin.yml`
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0
- Shadow-Relocations:
  - `com.google.gson` -> `de.pixelrpg.rpg.libs.gson`
  - `com.zaxxer.hikari` -> `de.pixelrpg.rpg.libs.hikari`
  - `com.mysql` -> `de.pixelrpg.rpg.libs.mysql`
- `Pixel-RPG.jar` als ShadowJar
- `slim` als Classifier des normalen JAR
- bestehende Build-Verifikationen
- `check` muss weiterhin sämtliche bestehenden Verifikationen ausführen.

Keine Legacy-Bukkit-/CraftBukkit-/Legacy-NMS-/Spigot-Mapping-Lösungen einführen.

---

## 3. Referenzzustand

Referenz-Commit von `main` zum Zeitpunkt dieses Audits:

`b02747a21cde6d1308c59a7f027cda80877cfe50`

`main` und `test` sind zu diesem Zeitpunkt identisch.

Die Repository-Forensik ergibt für den Referenzzustand:

- 321 Dateien insgesamt im Git-Tree
- 247 Java-Dateien
- umfangreiche Ressourcen-/Content-Struktur
- mehrere bereits getrennte fachliche Subsysteme

Der aktuelle `rebuild`-Branch wurde absichtlich nur mit der Build-/Entwicklungsgrundlage initialisiert:

- `.github/workflows/`
- `.vscode/`
- `build.gradle`
- `settings.gradle`

Es wurden noch keine fachlichen Java-/Ressourcen-Systeme in `rebuild` übernommen.

---

## 4. Grundregel für den Neuaufbau

### NICHT

- `src/main` einfach komplett aus `main` kopieren;
- Klassen nur umbenennen oder verschieben;
- bestehende Manager blind weiterverwenden;
- alte Dialog-Abstraktionen übernehmen, wenn native Paper-Dialoge die Aufgabe lösen;
- mehrere parallele Datenquellen für denselben fachlichen Zustand behalten;
- God-Manager erzeugen, die NPC, Quest, Dialog, Persistence und Content gleichzeitig verwalten;
- tote Klassen nur aus Angst vor Funktionsverlust behalten;
- bestehende technische Struktur als automatisch richtig behandeln.

### STATTDESSEN

Für jedes Subsystem:

1. Ist-Verhalten aus `main` erfassen.
2. Öffentliche/funktional relevante Schnittstellen identifizieren.
3. Datenflüsse und Persistenzpfade nachvollziehen.
4. Event-/Listener-Abhängigkeiten erfassen.
5. Main-/Async-Grenzen erfassen.
6. tatsächliche Verantwortlichkeiten der vorhandenen Klassen bestimmen.
7. KEEP / REBUILD / MERGE / REMOVE / REPLACE entscheiden.
8. neue Zielstruktur definieren.
9. neu implementieren.
10. Build prüfen.
11. Verhalten gegen `main` vergleichen.
12. erst danach zum nächsten abhängigen Subsystem weitergehen.

Keine Löschung darf ausschließlich aufgrund des Dateinamens erfolgen.

---

## 5. Festgestellte Architekturmerkmale des SOLL-Zustands

### Plugin-Lifecycle

`PixelRPGPlugin` übernimmt aktuell sehr viele Initialisierungsaufgaben und hält zahlreiche fachliche Services als Felder. Unter anderem werden Spielerprofile, Stats, Berufe, Items, Equipment, Shops, Story, Parties, Quests, Regionen, Bosse, Scoreboard, NPCs, Dialoge, Companions und weitere Listener/Tasks aus einer zentralen Klasse verdrahtet.

**Rebuild-Anweisung:**

Der Lifecycle muss zentral orchestriert bleiben, darf aber nicht selbst fachliche Logik enthalten. Initialisierung und Shutdown müssen über klar definierte Komponenten erfolgen. Abhängigkeiten müssen explizit sein. Direkte globale Rückgriffe auf `PixelRPGPlugin.getInstance()` sind im Domain-/Service-Code nach Möglichkeit zu entfernen und durch explizite Dependencies zu ersetzen.

### Lifecycle / Ressourcen

Es existiert bereits ein `LifecycleCoordinator`. Dieser Ansatz ist als Grundlage zu prüfen und nicht ohne Grund zu duplizieren.

Alle Executor, Scheduler, Datenbank-Pools und persistenten Services müssen einen eindeutigen Besitzer und einen eindeutigen Shutdown-Pfad haben.

### NPC

Der aktuelle NPC-Bestand umfasst u. a.:

- `NpcManager`
- `RPGNpc`
- `NpcBehaviorRegistry`
- mehrere konkrete NPC-Behaviors
- Chunk-/Interaction-/Look-Listener
- Skin-Resolver und externen Skin-Service.

Die aktuelle Skin-Persistenz speichert die aufgelöste Texture-Property inklusive Signatur. Das ist funktional relevant und muss im Rebuild erhalten bleiben.

**Rebuild-Anweisung:**

NPC-Domainzustand, Runtime-Entity-Verwaltung, Skin-Auflösung, Persistence und Interaktion müssen sauber getrennt werden. Ein einzelner God-Manager für alle Bereiche ist zu vermeiden.

Chunk Load/Unload, Entity-Tracking und Skin-Anwendung müssen einen klaren Lifecycle besitzen.

### Dialog

Der aktuelle Stand verwendet bereits Paper-Native-Dialog-APIs, insbesondere:

- `io.papermc.paper.dialog.Dialog`
- `DialogBase`
- `DialogType`
- `ActionButton`
- `DialogAction`
- native `Player.showDialog(...)`.

Zusätzlich existiert eine eigene höhere Dialog-/Tree-Schicht.

**Rebuild-Anweisung:**

Native Dialoge sind die verbindliche technische Grundlage. Es darf keine Legacy-1.21.x-Dialogimplementierung entstehen.

Die höhere Schicht darf nur fachliche Orchestrierung liefern: z. B. Dialogzustand, Bedingungen, Optionen, nächste Knoten und Aktionen. Sie darf nicht die native Dialog-API unnötig nachbauen.

Für zukünftigen Content müssen Dialogdefinitionen von technischer Dialogausführung getrennt werden.

### Quests

Die aktuelle Struktur verteilt Questfunktionalität über mehrere Klassen und Ressourcen. Im Java-Code existieren u. a.:

- `QuestManager`
- `QuestRepository`
- `QuestProgress`
- `QuestText`
- `QuestType`
- `QuestPassiveCheckTask`
- Quest-Listener und Quest-Kommandos.

Auch die Ressourcen enthalten mehrere Quest-Dateien, darunter:

- `quests_crafting_orders.json`
- `quests_additional.json`
- `quests_content_expansion_01.json`
- `quests_expansion_02.json`
- `quests_v2.json`
- `quests_world_expansion.json`.

**Rebuild-Anweisung:**

Diese Dateien dürfen nicht einfach zusammenkopiert werden.

Zuerst muss festgestellt werden, welche Dateien tatsächlich unterschiedliche fachliche Inhalte, Versionen oder Duplikate enthalten. Danach muss eine eindeutige Content-Struktur entstehen.

Quest-Domainmodell, Spieler-Quest-State, Questdefinitionen, Repository/Persistence, Progression, Trigger und UI/Dialog-Interaktion sind getrennt zu betrachten.

Das Ziel ist nicht zwingend eine einzelne Datei, sondern eine **fachlich eindeutige Quelle pro Verantwortungsbereich**.

### Story / Lore

Aktuell existieren u. a.:

- `StoryManager`
- `StoryChapter`
- `StoryBookFactory`
- `StoryNpcDialogue`
- Story-bezogene NPC-Strukturen.

Der aktuelle Story-Code enthält teilweise direkt eingebetteten Content.

**Rebuild-Anweisung:**

Story-/Lore-Content muss aus der technischen Engine herausgelöst werden. Java soll nicht der primäre Speicherort für umfangreiche Spieltexte sein.

Die spätere Content-Struktur muss Story, Kapitel, Dialoge, Lore und Voraussetzungen sauber modellieren können.

---

## 6. Persistenz

Aktuell existieren mehrere Persistenzformen:

- MySQL/Hikari
- YAML-Dateien
- JSON-Ressourcen für statischen Content
- NPC-YAML-Persistenz
- weitere fachliche Speichermechanismen.

Die vorhandene `DatabaseManager`-Implementierung besitzt Connection-Pooling und Schema-Migrationen.

**Rebuild-Anweisung:**

Vor jedem Neubau muss pro Datenbestand festgelegt werden:

- Ist es statischer Content?
- Ist es Server-Konfiguration?
- Ist es persistenter Runtime-State?
- Ist es Cache?
- Wer ist die Source of Truth?
- Wann wird gelesen?
- Wann wird geschrieben?
- Auf welchem Thread?
- Was passiert bei Shutdown?
- Was passiert bei Schreibfehlern?

Kein Zustand darf gleichzeitig von mehreren unabhängigen Quellen autoritativ verwaltet werden.

Datenbankzugriffe dürfen nicht im Main Thread stattfinden, sofern sie nicht ausdrücklich durch die aktuelle Paper-API erforderlich sind.

---

## 7. Performance-Regeln

Der Rebuild muss folgende Regeln erfüllen:

1. Paper-/World-/Entity-/Player-Operationen auf dem Serverthread.
2. Datenbank, Datei-I/O und externe HTTP-Anfragen außerhalb des Serverthreads.
3. Ergebnisse asynchron berechnen/laden und notwendige Serveränderungen anschließend gezielt auf den Serverthread zurückführen.
4. Keine unkontrollierten Scheduler-Kaskaden.
5. Keine permanent laufenden Tasks, wenn eventbasierte Ausführung genügt.
6. Keine unnötigen Polling-Schleifen.
7. Caches nur mit klarer Invalidierungsstrategie.
8. Keine globale Speicherung von Live-`Player`, `Entity` oder `World`-Objekten.
9. Snapshot-then-I/O bei Persistenz.
10. Shutdown muss laufende Persistenzarbeiten berücksichtigen.
11. Externe Services müssen Timeouts und Fehlerbehandlung besitzen.
12. Runtime-Registries müssen nur Daten halten, die tatsächlich für schnelle Runtime-Lookups benötigt werden.

Performanceoptimierung darf niemals dazu führen, dass Serverthread-Grenzen oder Datenkonsistenz verletzt werden.

---

## 8. Content-Architektur

Der Rebuild ist ausdrücklich die technische Grundlage für die kommende Content-Phase.

Die Architektur muss deshalb folgende Content-Arten unterstützen können, ohne erneuten Architekturumbau:

- NPC-Texte
- NPC-Geschichten
- Dialoge
- Dialogzweige
- Lore
- Kapitel
- Quests
- Questziele
- Questvoraussetzungen
- Questbelohnungen
- Folgequests
- Entscheidungen
- wiederkehrende Dialoge
- Zustandsabhängige Dialoge.

Technische Engine und Content müssen getrennt bleiben.

Content-IDs müssen stabil und eindeutig sein.

---

## 9. Dialog-/Quest-Zielarchitektur

Für die kommende Content-Phase soll der fachliche Ablauf grundsätzlich folgende Form ermöglichen:

`NPC -> native Dialog -> Spielerentscheidung -> fachliche Aktion -> Quest/Story-State -> nächster Dialog`

Die Dialogschicht darf nicht selbst Questregeln duplizieren.

Questregeln gehören in die Quest-Domain/Quest-Service-Schicht.

Storyfortschritt gehört in die Story-Schicht.

NPC-Entity-Lifecycle gehört in die NPC-Schicht.

Persistence gehört in die Persistence-Schicht.

Der Dialog orchestriert die Interaktion, besitzt aber nicht die fachliche Wahrheit aller Systeme.

---

## 10. Verifikation jedes Rebuild-Schrittes

Nach jedem größeren Subsystem:

### Statische Prüfung

- Java 25
- Paper 26.2
- keine Legacy-NMS
- kein CraftBukkit
- kein `ChatColor`
- keine statischen Live-Server-Referenzen
- nur Mojang-Mappings
- korrekte Third-Party-Relocations
- Build-Verifikationen unverändert funktionsfähig.

### Build

`gradle clean build --no-daemon --stacktrace`

Ein erfolgreicher Build ist notwendig, aber nicht ausreichend.

### Funktionalität

Der jeweilige Funktionsbereich muss gegen den aktuellen `main`-SOLLzustand geprüft werden.

Insbesondere:

- Serverstart
- Serverstop
- Reload-/Lifecycle-relevante Pfade
- Player Join/Quit
- Persistence
- NPC Spawn/Despawn
- Chunk Load/Unload
- NPC Skin Save/Load
- Dialoge
- Quests
- Story
- Regionen
- Berufe
- Items
- Companions
- Shops
- Bosse
- Statistiken.

---

## 11. Rebuild-Reihenfolge

Die Implementierung soll nicht nach Dateinamen, sondern nach Abhängigkeiten erfolgen:

1. Build-/Projektgrundlage
2. Plugin-Lifecycle / Core
3. zentrale Domain-/Runtime-Grundlagen
4. Persistence-Abstraktion und konkrete Speicher
5. Player/Profile
6. Items/Equipment/ökonomische Grundlagen
7. NPC Runtime + Persistence
8. native Dialog-Grundlage
9. Content-/Text-Grundlage
10. Quest-Domain + Quest-Persistence
11. Story/Lore
12. Regionen
13. Berufe/Crafting
14. Companions
15. Shops/Trading
16. Combat/Bosse/Stats
17. Commands/UI/administrative Werkzeuge
18. abschließende Integration und Forensik.

Die tatsächliche Reihenfolge darf angepasst werden, wenn die Codeanalyse eine andere Abhängigkeitsreihenfolge nachweist.

---

## 12. Akzeptanzkriterium

Der Rebuild ist erst dann abgeschlossen, wenn:

> **Das neue System das funktionale Verhalten des Referenzzustands reproduziert und gleichzeitig eine nachvollziehbare, wartbare und erweiterbare Architektur besitzt.**

Zusätzlich muss die neue Architektur für die nächste Phase geeignet sein:

> **Texte, Lore, Geschichte, Quests und native Dialoge sollen als Content erweitert werden können, ohne die technische Basis erneut grundlegend umzubauen.**

Keine Funktion darf stillschweigend entfallen, nur weil ihre alte Implementierung entfernt wurde.

Wenn eine Funktion bewusst anders umgesetzt wird, muss dokumentiert werden:

- altes Verhalten;
- neues Verhalten;
- Grund der Änderung;
- Nachweis der funktionalen Gleichwertigkeit.

---

## 13. Verbindliche Regel für weitere Agent-/Entwicklungsarbeit

Vor jeder Änderung auf `rebuild`:

1. diese `audit.md` lesen;
2. `main` als Referenz prüfen;
3. tatsächlichen Code prüfen;
4. Abhängigkeiten des betroffenen Systems prüfen;
5. Zielarchitektur für den betroffenen Bereich festlegen;
6. nur notwendige Dateien ändern;
7. keine funktionalen Bereiche ohne Auftrag verändern;
8. nach der Änderung bauen/verifizieren;
9. Audit bei neuen Erkenntnissen aktualisieren.

**Keine Ratespiele. Keine erfundenen APIs. Keine Legacy-Lösungen. Keine blinden Kopien aus `main`.**

Der Rebuild soll ein bewusst neu entworfenes System werden, dessen Verhalten aus dem Referenzzustand abgeleitet und dessen Architektur für die kommende Content-Entwicklung optimiert wird.


---

## 14. Rebuild-Fortschritt

### Phase 1 — Build-/Projektgrundlage

Status: **abgeschlossen**

Auf rebuild vorhanden und geprüft:

- verbindliche Gradle-/Paper-/Java-25-Basis;
- bestehende Shadow-Relocations und Build-Verifikationen;
- `paper-plugin.yml`;
- vollständige Runtime-Konfigurationsgrundlage aus dem Referenzzustand;
- zentraler `LifecycleCoordinator` als einziger Runtime-Resource-Owner der neuen Composition Root;
- minimale `PixelRPGPlugin`-Composition-Root ohne Übernahme der alten God-Manager-Verdrahtung;
- Paper-Bootstrap als separater Bootstrap-Einstiegspunkt;
- CI prüft rebuild zusätzlich zu den bestehenden Branches.

Bewusst noch nicht übernommen:

- fachliche Feature-Module;
- alte `PixelRPGPlugin`-Verdrahtung;
- alte statische/globalen Zugriffe;
- alte Persistenz- und Domainmodelle.

Nächster Schritt ist die forensische Rekonstruktion der zentralen Persistenz-/Player-Abhängigkeiten aus `main`, bevor Player/Profile und weitere Fachmodule neu implementiert werden.


---

## 15. Rebuild-Fortschritt

### Phase 2–5 — Core, Persistence und Player/Profile

Status: **implementiert, Build-Verifikation ausstehend**

Forensisch aus `main` geprüft und auf `rebuild` neu zusammengesetzt:

- `LifecycleCoordinator` bleibt der zentrale Resource-Owner.
- `DatabaseManager` bildet weiterhin Connection-Pooling und Schema-Migrationen ab.
- Das bestehende Schema inklusive `persistence_revision`, aktiver Quests, Statistiken und Equipment wurde funktional erhalten.
- `PlayerProfile` enthält weiterhin den vollständigen bisher persistierten Player-State: Registrierung, XP, Geld, Berufe, Rezepte, Wegpunkte, Story-Kapitel, Quests, Statistiken, Equipment, UI-Flags und Playtime.
- YAML- und MySQL-Persistenz bleiben als getrennte konkrete Repositories bestehen.
- Persistenz arbeitet mit Snapshots und asynchronem I/O.
- Player-Profile werden weiterhin vor dem Join geladen und beim Quit/Shutdown gespeichert.
- Die Profile-Revision bleibt Teil des Persistenzprotokolls; MySQL prüft Stale-Revisionen.
- Fehler im MySQL-Initialisierungspfad behalten den bisherigen YAML-Fallback.
- Player-Lifecycle und Profile-Persistenz sind von der Composition Root getrennt.
- Guild-/Economy-Schnittstellen und die vorhandenen Player-Events wurden als fachliche API-Grenze übernommen.

### Architekturentscheidungen

Bewusst **nicht** übernommen wurden:

- die alte zentrale `PixelRPGPlugin`-Verdrahtung;
- globale Plugin-Singletons als Dependency-Quelle;
- synchrone Player-Datei-I/O;
- fachliche Systeme außerhalb des Player-/Persistence-Scope;
- alte Feature-Manager, die erst in ihren jeweiligen Rebuild-Phasen untersucht werden.

### Verifikation

Die Quellstruktur enthält in diesem Schritt keine verbotenen Legacy-NMS-/CraftBukkit-/ChatColor-Referenzen und keine statischen Live-`Player`/`Entity`/`World`-Felder.

Ein vollständiger `gradle clean build --no-daemon --stacktrace` konnte über die verfügbare GitHub-Actions-Schnittstelle für den aktuellen `rebuild`-HEAD noch nicht als abgeschlossenes Ergebnis festgestellt werden. Daher wird dieser Schritt **nicht** als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Items/Equipment/ökonomische Grundlagen**. Vor dessen Implementierung müssen die tatsächlichen Item-/Equipment-Abhängigkeiten aus `main` forensisch erfasst werden.


## 16. Rebuild-Fortschritt

### Phase 6 — Items / Equipment / ökonomische Grundlagen

Status: **implementiert; Code-Stand CI-verifiziert, Audit-Only-Commit durch externe Paper-Maven-Störung nicht erneut verifizierbar**

Forensisch aus `main` geprüft und für `rebuild` neu strukturiert:

- zentrale Item-API und Item-Domain mit stabilen Item-IDs, Kategorien, Raritäten und Item-Definitionen;
- zentrale `RPGKeys`-Instanz statt globalem Plugin-Singleton für Item-PDC-Schlüssel;
- JSON-Content als `data/`-Quelle für Itemdefinitionen, Food, Scaling, Equipment-Sets, Unique-State und Boss-Reward-Items;
- aktuelle Paper-Data-Component-API für Food/Consumable-Konfiguration;
- RPG-Item-Erzeugung mit PDC-Identität, Instanz-ID, Level, Gearscore und bestehenden Item-Statfeldern;
- Economy-Safety-Prüfung für handelbare Items;
- Unique-Item-State mit asynchroner Persistierung und eindeutigem Lifecycle-Besitzer;
- Soulbound-Operation als fachlich isolierter Service;
- persistentes Equipment bleibt im PlayerProfile und wird über einen separaten Equipment-Service beim Join/Respawn wiederhergestellt und beim Quit synchronisiert;
- physisches Gilden-Gold bleibt über eine eigene Currency-Factory und einen isolierten Pickup-Listener von der Profile-/Economy-API getrennt;
- bestehende Guild-/Economy-Services werden weiterhin über die PlayerProfile-Komponente bereitgestellt.

### Bewusst nicht übernommen

- alter `EquipmentService` mit direkter Abhängigkeit auf `StatEngine` und globalem `PixelRPGPlugin.getInstance()`;
- alte `WakeScheduler`-Abhängigkeit für Equipment-Refreshes;
- alte globale/static `RPGKeys.init(...)`-Initialisierung;
- weitere Stats-/Combat-Logik, da diese gemäß Abhängigkeitsreihenfolge erst im Stats-/Combat-Rebuild behandelt wird.

### Verifikation

- Die neuen Item-/Equipment-/Economy-Klassen verwenden keine CraftBukkit- oder Legacy-NMS-Klassen.
- Es werden keine statischen Live-`Player`/`Entity`/`World`-Referenzen eingeführt.
- Listener besitzen Zweckkommentare direkt über jedem `@EventHandler`.
- Paper-26.2-Data-Component-APIs werden für die Food-Konfiguration verwendet.
- GitHub Actions Build Run `35865272470` für den vollständigen Code-Stand `29f3e3fc4e171a46182313ef33d576f80239afcc` wurde erfolgreich abgeschlossen. `Build PixelRPG`, `Verify source API boundaries` und `Verify plugin artifact` meldeten jeweils `success`. Der anschließende reine Audit-Dokumentations-Commit `49665081859ed36755c23368c6e8311f1159e19c` wurde zweimal gegen GitHub Actions geprüft; beide Läufe scheiterten bereits in `paperweightUserdevSetup`, weil `io.papermc.codebook:codebook-cli:2.0.1-SNAPSHOT` vom Paper-Maven-Repository mit HTTP 502/503 nicht abrufbar war. Das ist ein externer Repository-/Infrastrukturfehler und kein im Buildlog erkennbarer Quellcodefehler.

Nächster abhängiger Bereich ist **NPC Runtime + Persistence**. Vor dessen Implementierung sind NPC-Domainzustand, Runtime-Entity-Lifecycle, Skin-Persistenz und Chunk-Lifecycle aus `main` forensisch zu erfassen.


## 17. Rebuild-Fortschritt

### Phase 7 — NPC Runtime + Persistence

Status: **implementiert; ein CI-Kompilierfehler wurde anhand des Buildlogs korrigiert, erneute CI-Verifikation läuft**

Forensisch gegen den NPC-Bestand von `main` aufgebaut und nicht als Blindkopie übernommen:

- NPC-Domainzustand mit stabiler ID, Typ, Name, Position, Skin-Quelle und optionalem Beruf;
- getrennte Persistence-Abstraktion mit YAML-Repository;
- NPC-YAML-Lesen vollständig außerhalb des Serverthreads;
- Snapshot-basierte, asynchrone NPC-Persistierung mit temporärer Datei und atomarem Replace;
- deterministische Persistence-Kette und Shutdown-Flush;
- Runtime-Entity-Tracking über NPC-ID <-> Entity-UUID;
- Chunk-Index für gezieltes Spawn/Despawn-Verhalten;
- NPC-Mannequins werden nur als Runtime-Entities geführt und nicht als persistente Minecraft-Entities gespeichert;
- aktuelle Paper-Mannequin-/ResolvableProfile-basierte Skin-Anwendung;
- aufgelöste Texture-Property inklusive Signatur wird exakt persistiert und nach Neustart wiederverwendet;
- Player-Skin-Auflösung und externe Skin-Quellen bleiben asynchron;
- externe Skin-URLs besitzen Größen-, Redirect- und Private-Network-Schutz;
- NPC-PDC-Identität verwendet die neue instanzgebundene `RPGKeys`-Quelle;
- Chunk-Load/Unload und Player-Join sind über einen eigenen Listener vom Runtime-Manager getrennt;
- NPC-Schadensschutz ist als eigener Listener isoliert;
- globale `PixelRPGPlugin.getInstance()`-/Plugin-Manager-Rückgriffe wurden im neuen NPC-Core nicht übernommen;
- NPC-Ressourcen besitzen einen eindeutigen Lifecycle-Besitzer über `LifecycleCoordinator`.

### Bewusst noch nicht übernommen

Die alten NPC-Behaviors, Quest-/Shop-/Story-/Travel-Interaktionen und die alte Look-/Nameplate-Speziallogik wurden nicht vorgezogen. Sie hängen fachlich von späteren Rebuild-Phasen ab und werden erst zusammen mit ihrer tatsächlichen Domain-/Dialogabhängigkeit neu aufgebaut. Dadurch wird keine unfertige Quest-/Dialogarchitektur in den NPC-Core eingebaut.

### Skin-Persistenz — funktionale Zielerhaltung

Der Referenzzustand speichert die aufgelöste Texture-Property inklusive Signatur in `npcs.yml`. Der Rebuild übernimmt genau diesen Persistenzvertrag: Die Property wird unmittelbar nach erfolgreicher Auflösung aus dem Resolver übernommen und nicht nachträglich aus einem veränderlichen Runtime-Profil rekonstruiert. Beim Neustart wird die gespeicherte Property vor einer erneuten externen Auflösung verwendet.

### Verifikation

- Neue NPC-Klassen verwenden keine Legacy-NMS- oder CraftBukkit-Pakete.
- Keine statischen Live-`Player`/`Entity`/`World`-Referenzen wurden eingeführt.
- Listener besitzen Zweckkommentare direkt über jedem `@EventHandler`.
- Datei-I/O und externe HTTP-Auflösung liegen außerhalb des Serverthreads.
- Der erste vollständige CI-Lauf des NPC-Stands erreichte `compileJava` und meldete genau einen Fehler in der Berufsermittlung (`String` statt `int` bei `substring`). Dieser Fehler wurde direkt anhand des Logs korrigiert. Der daraus resultierende Stand muss erneut vollständig durch `clean build`, Source-Boundary- und Artifact-Verifikation laufen.

Nächster abhängiger Bereich ist **native Dialog-Grundlage**. NPC-Interaktion wird dort fachlich an die native Paper-Dialogschicht angebunden; Quest-/Story-Regeln bleiben in ihren späteren Domain-Schichten.


## 18. Rebuild-Fortschritt

### Phase 8 — Native Dialog-Grundlage

Status: **implementiert; CI-Verifikation läuft**

Forensisch gegen den bestehenden Dialog-Bestand von `main` geprüft und auf `rebuild` als eigenständige technische Grundlage neu aufgebaut:

- native Paper-Dialog-Erzeugung über `io.papermc.paper.dialog.Dialog`, `DialogBase`, `DialogType`, `ActionButton`, `DialogAction` und `Player.showDialog(...)`;
- keine Legacy-1.21.x-Dialogmechanik und keine eigene Nachbildung des Minecraft-Dialogprotokolls;
- technische Dialogausführung in `DialogueEngine` gekapselt;
- fachliche Dialogstruktur über `DialogueTree`, `DialogueNode`, `DialogueOption` und `DialogueCondition` getrennt;
- Dialogoptionen können Bedingungen, fachliche Aktionen und Folge-Nodes besitzen, ohne Quest-/Story-Regeln im Dialogsystem zu duplizieren;
- native Dialoge unterstützen Notice-, Multi-Action-, Confirmation- und Input-Aktionen;
- per-Spieler Dialogfortschritt für gesehen/abgeschlossen wird separat persistiert;
- Dialogfortschritt wird asynchron geladen und gespeichert;
- Persistenz verwendet Snapshot-then-I/O sowie temporäre Datei mit atomarem Replace-Fallback;
- Shutdown wartet auf laufenden Dialog-I/O;
- NPC-Interaktion ist über einen eigenen Listener an registrierte Dialogbäume angebunden: `npc:<npc-id>`;
- NPC-Runtime und Dialogsystem bleiben getrennt; der Dialoglistener kennt nur NPC-Laufzeitauflösung und Dialogservice;
- noch nicht vorhandene Content-Dialoge werden nicht künstlich erzeugt. Dadurch bleiben Quest-, Story-, Berufs-, Shop- und Reise-Regeln bis zu ihren jeweiligen Rebuild-Phasen außerhalb der technischen Dialoggrundlage.

### Verifikation

Der Stand muss nach dem Dialog-Rebuild erneut vollständig über `gradle clean build --no-daemon --stacktrace`, Source-Boundary-Checks und Artifact-Verifikation laufen. Erst nach erfolgreichem CI-Lauf wird Phase 8 als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Content-/Text-Grundlage**. Dabei werden stabile Content-IDs und externe Text-/Dialogdefinitionen aufgebaut, bevor Quest-/Story-Logik darauf aufsetzt.


### Phase 9 — Content-/Text-Grundlage

Status: **implementiert; CI-Verifikation ausstehend**

Forensisch gegen den tatsächlichen Content-Bestand von `main` geprüft. Dabei wurde festgestellt, dass insbesondere Quest-Content über mehrere JSON-Dateien verteilt ist und Story-Text im alten Stand teilweise direkt in Java/YAML verarbeitet wird. Es wurde deshalb **nicht** blind Content zusammenkopiert.

Auf `rebuild` wurde stattdessen die technische Content-Grundlage geschaffen:

- stabile `ContentId` mit validiertem, versionsunabhängigem ID-Format;
- immutable `TextContent` als fachlicher Textdatensatz;
- `ContentCatalog` als unveränderlicher Runtime-Snapshot;
- asynchroner JSON-Loader für externen Text-Content;
- `ContentCatalogService` als Lifecycle-besitzender Runtime-Zugriff;
- separater `PixelRPG-ContentIO`-Executor;
- Snapshot-Austausch über `AtomicReference`, sodass Runtime-Lookups keinen Datei-I/O benötigen;
- initiale Content-Datei `data/content/texts.json` mit expliziter Schema-Version;
- Content-Datei wird beim ersten Start in den Plugin-Datenordner kopiert;
- fehlerhafte Content-Daten führen zu einem kontrollierten Ladefehler statt zu einem teilweise geladenen Katalog;
- Content-I/O liegt vollständig außerhalb des Serverthreads;
- Content-Service besitzt einen eindeutigen Shutdown-Pfad.

Bewusst **nicht** übernommen wurden die sechs vorhandenen Quest-Dateien als vermeintlich bereits konsolidierte Textquelle. Vor Quest-Rebuild muss deren fachlicher Inhalt dedupliziert und auf stabile Content-IDs abgebildet werden. Ebenso werden Story-/NPC-Texte erst in der Content-Phase migriert, nachdem ihre tatsächliche Verwendung aus `main` vollständig erfasst ist.

Damit ist die technische Grundlage vorhanden, ohne bereits fachliche Quest-/Story-Regeln vorwegzunehmen.

### Verifikation

Nach Phase 9 ist ein vollständiger `gradle clean build --no-daemon --stacktrace` inklusive bestehender Source-/Artifact-Verifikationen erforderlich. Die Phase wird erst nach erfolgreichem CI-Lauf als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Quest-Domain + Quest-Persistence**. Vor dessen Implementierung müssen die sechs vorhandenen Quest-JSON-Dateien hinsichtlich IDs, Duplikaten, Versionen, Voraussetzungen, Rewards und Content-Überschneidungen vollständig normalisiert werden.


### Phase 10 — Quest-Domain + Quest-Persistence

Status: **implementiert; CI-verifiziert**

Die Quest-Struktur von `main` wurde vor der Übernahme forensisch ausgewertet. Der tatsächlich aktive alte Loader verwendet vier Dateien:

- `quests_v2.json`
- `quests_additional.json`
- `quests_world_expansion.json`
- `quests_expansion_02.json`

Diese enthalten zusammen **163 eindeutige Quest-Definitionen**:

- 113 `COLLECT`
- 49 `HUNT`
- 1 `GLOBAL_EVENT`

Zusätzlich existieren im Referenzzustand `quests_crafting_orders.json` mit 60 `craft_order_*`-Definitionen und `quests_content_expansion_01.json` mit 46 Definitionen. Beide wurden bewusst **nicht** in die aktive Quelle übernommen, weil der alte Runtime-Loader sie ebenfalls nicht als reguläre Questquelle verwendet. Insbesondere die Crafting-Orders waren im alten Code explizit als veralteter/entfernter Recipe-Datenbestand ausgeschlossen.

Die vier tatsächlich geladenen Quellen wurden zu einer eindeutigen kanonischen Runtime-Quelle normalisiert:

- `data/quests/definitions.json`
- 163 eindeutige IDs
- keine doppelten IDs
- keine fehlenden referenzierten Prerequisites
- keine fehlenden Follow-up-Quest-IDs
- Prerequisite-Zyklen werden beim Laden abgelehnt.

Neu aufgebaut wurden:

- `QuestDefinition` als immutable fachliche Definition;
- `QuestReward` für Geld, Erfahrung, Item-Rewards und Companion-Rewards;
- `QuestNavigation` für Struktur-/Biome-Navigation;
- asynchron ladendes `QuestRepository`;
- `QuestService` als fachliche Runtime-Grenze für Start, Abbruch, Progress und Abschluss;
- `QuestCompletedEvent` als entkoppelte Integrationsschnittstelle;
- bestehender persistenter Quest-State in `PlayerProfile`/Profile-Persistenz bleibt die Source of Truth.

Das Quest-Repository führt **keine eigene zweite Persistenz des Spieler-Quest-States** ein. Aktive Quests, Fortschritt, Ablaufzeit und abgeschlossene Quests bleiben Teil des bestehenden PlayerProfile-Snapshots und damit Teil der bereits vorhandenen YAML-/MySQL-Persistenz.

Quest-Datei-I/O erfolgt ausschließlich über einen eigenen `PixelRPG-QuestIO`-Executor. Die Definitionsdatei wird beim ersten Start aus den Plugin-Ressourcen in den Datenordner kopiert.

Der neue Service erzwingt bereits:

- maximal 5 aktive Quests;
- Level-Voraussetzungen;
- abgeschlossene Voraussetzungen;
- Berufszugehörigkeit und Berufslevel;
- keine bereits aktiven/abgeschlossenen Quests;
- Ablaufzeit bei zeitlich begrenzten Quests;
- Fortschrittsbegrenzung auf das Quest-Ziel;
- atomaren Wechsel von aktiv zu abgeschlossen auf dem PlayerProfile.

Bewusst noch **nicht** aus `main` übernommen:

- alte Quest-GUIs;
- alte Polling-/Passive-Check-Tasks;
- alte Event-Listener für Mob-Kills/Inventar/Navigation;
- Reward-Ausführung für konkrete Item-/Companion-Systeme;
- NPC-/Dialog-Integration.

Diese Teile sind abhängige Integrationsschritte. Die fachliche Quest-Wahrheit liegt jetzt bereits getrennt von UI, NPC Runtime und Dialogtechnik.

### Verifikation

Die kanonische Quelle wurde vor Commit programmatisch auf **163 eindeutige IDs und keine fehlenden Prerequisite-/Follow-up-Referenzen** geprüft.

Der vollständige `gradle clean build --no-daemon --stacktrace` lief auf Commit `b3a464b0b517edac084214910367435ed65850f2` erfolgreich durch. Zusätzlich waren die bestehenden Source-API-Grenzprüfungen und die Plugin-Artefaktprüfung erfolgreich. Ein vorheriger Lauf scheiterte ausschließlich an konkret festgestellten Compile-Fehlern (`QuestType` fehlte, `QuestRepository` war nicht `AutoCloseable`, bestehender `NpcRuntimeManager`-Substring-Aufruf war syntaktisch falsch); diese wurden gezielt korrigiert und anschließend erfolgreich verifiziert.

Nächster abhängiger Bereich ist **Story/Lore**. Dabei müssen `StoryManager`, `StoryChapter`, `StoryBookFactory` und `StoryNpcDialogue` gegen die neue Content-/Dialog-Grundlage forensisch aufgelöst werden, ohne Quest-Regeln in die Dialogschicht zu verschieben.


### Phase 11 — Story / Lore

Status: **neu aufgebaut; CI-verifiziert**

Die Story-/Lore-Schicht wurde erneut gegen den tatsächlichen Referenzcode von main betrachtet. Die fachlich relevante Funktion des Referenzzustands bleibt erhalten:

- StoryManager lädt im Referenzzustand eine externe story.yml, erzeugt bei fehlender Datei ein Default-Kapitel und verwaltet den persistenten storyChapterIndex aus PlayerProfile.
- StoryChapter beschreibt Reihenfolge, ID, Titel, Textzeilen und XP-Belohnung.
- StoryBehavior öffnet für registrierte Spieler einen nativen Dialog, zeigt das nächste Kapitel und schaltet das Kapitel anschließend inklusive XP frei.
- StoryNpcDialogue behandelt auch den Zustand ohne neues Kapitel.

Die erste rebuild-Implementierung wurde für diese Revision bewusst geändert: **Die Buch-Erzeugung ist vollständig entfernt.**

### Aktuelle Zielstruktur

Beibehalten bzw. neu aufgebaut wurden:

- immutable StoryChapter als reine Story-Domain;
- asynchrones StoryRepository für `data/story/definitions.json`;
- StoryService als fachliche Story-State-Grenze auf Basis des bestehenden PlayerProfile;
- StoryNpcDialogue als native Paper-Dialog-Integration;
- StoryNpcInteractionListener als getrennte NPC-Interaktionsgrenze;
- Story-Definitionen über stabile Content-IDs und den bestehenden ContentCatalogService;
- der vorhandene Prolog mit den stabilen Content-IDs:
  - story.prologue.title
  - story.prologue.line.1
  - story.prologue.line.2
  - story.prologue.line.3
- Story-I/O über einen eigenen PixelRPG-StoryIO-Executor;
- eindeutiger Lifecycle-/Shutdown-Pfad für StoryRepository und Story-I/O;
- PlayerProfile.storyChapterIndex als einzige autoritative Quelle für den Spieler-Storyfortschritt;
- XP-Vergabe beim erfolgreichen Kapitelabschluss über PlayerProfileManager;
- native Paper-Dialoge als einzige Darstellung des Story-Kapitels.

Das Kapitel wird jetzt direkt im nativen Dialog als Textinhalt dargestellt. Die Dialogaktion schließt das Kapitel fachlich über StoryService ab. Es existiert keine zusätzliche Buchrepräsentation und kein separater Buch-UI-State.

### Entfernt

Die folgende rebuild-Struktur wurde vollständig entfernt:

- StoryBookFactory;
- die StoryBookFactory-Verdrahtung in PixelRPGPlugin;
- `story.chars-per-line`;
- `story.lines-per-page`;
- die Abhängigkeit von `BookMeta`/`WRITTEN_BOOK` für Story-Inhalte.

Damit enthält die Story-Schicht keine eigene Buch-/Seiten-/Zeilenaufteilungslogik mehr.

### Bewusst nicht übernommen

Nicht übernommen werden:

- der alte StoryManager;
- direkte Runtime-Erzeugung und Persistierung einer story.yml;
- globale Plugin-Singletons;
- die alte StoryBehavior-God-Integration mit direkter Kopplung an Story, Dialog und NPC-Behavior-Registry;
- eine zusätzliche Buchdarstellung als parallele Story-UI.

Die fachliche Wahrheit bleibt bei StoryService und PlayerProfile; die Darstellung liegt bei der nativen Dialogschicht.

### Verifikation

Die Revision wurde zusammen mit dem anschließenden Regionen-Rebuild im vollständigen GitHub-Actions-Lauf **35872994976** auf Commit **73fc9372ac65cc494c1d1bcc0ec5801c327f69b7** verifiziert.

Der Workflow meldete **success** für Build, Source-API-Grenzen und Plugin-Artefakt.

Nächster abhängiger Bereich ist **Regionen**.



### Phase 12 — Regionen

Status: **implementiert; CI-verifiziert**

Die Regionen wurden vor der Implementierung gegen den tatsächlichen Referenzbestand von main forensisch erfasst. Der Referenzbereich besteht aus Polygon-Geometrie, globalen Weltregionen, Priorität, Y-Grenzen, Besitzern/Mitgliedern, Flags, Enter-/Leave-Nachrichten, Properties und expliziten Mob-Spawnpunkten.

Neu auf rebuild aufgebaut wurden:

- PixelRegion als Region-Domainobjekt mit normaler und globaler Region;
- RegionGeometry mit Polygonvalidierung, Selbstüberschneidungsprüfung, Bounding-Box und Point-in-Polygon;
- RegionPoint, RegionType, RegionFlag, RegionFlagCategory;
- RegionSpawnPoint und validierter SpawnMobType;
- asynchrones RegionRepository für regions.yml;
- atomare YAML-Snapshots und getrennte Speicherung normaler/globaler Regionen;
- RegionManager als Source of Truth für Runtime-Regionen und Chunk-/Spawnpunkt-Indizes;
- asynchrone Region-Ladung mit Rückkehr auf den Serverthread für Runtime-Anwendung;
- serialisierte Snapshot-Persistenz und deterministischer Shutdown-Flush;
- RegionPolicyService für Gameplay-Regeln;
- RegionTransitionService für Enter-/Leave-Zustand und Titel;
- RegionListener als dünne Paper-Event-Grenze;
- eventgetriebener RegionSpawnService ohne globalen Polling-Task;
- RegionEditor mit temporären Admin-Sessions und Partikelvisualisierung;
- nativer Paper-Dialog RegionFlagDialogService für kategorisierte Flag-Verwaltung.

### Funktional erhaltene Regeln

Der neue Regionskern bildet die im Referenzzustand vorhandenen zentralen Regeln ab:

- Polygonregionen über X/Z-Konturen;
- globale Standardregion pro Welt;
- Prioritätsauflösung bei überlappenden Regionen;
- Y-Grenzen;
- Owner-/Member-Bypass für geschützte Aktionen;
- PvP-, Mob-, Tier- und Fallschaden;
- Blockabbau/-platzierung;
- Entity-, Block- und Containerinteraktionen;
- Item-Drop/-Pickup;
- Feuer-, Wasser- und Lavafluss;
- Explosionen, TNT, Creeper, Ghast und Enderman-Griefing;
- Lightning, Crop-Growth und Leaf-Decay;
- Respawn-Anker, Schlafen, Enderperlen und Chorusfrucht;
- natürliche Heilung und Hunger;
- Region Entry/Exit;
- explizite Monster-Spawnpunkte mit Respawn-Verzögerung und Näheprüfung.

### Bewusst nicht vorgezogen

Die alte RegionSubCommand-Integration wird nicht in Phase 12 dupliziert. Die vorhandene Command-Infrastruktur gehört gemäß Audit-Reihenfolge in Phase 17 — Commands/UI/administrative Werkzeuge. Der Region-Editor ist deshalb technisch vorbereitet, aber seine /pixelrpg region ...-Befehlsverdrahtung wird erst zusammen mit der neuen Command-Schicht angeschlossen.

Ebenso wurde kein alter WakeScheduler übernommen. Die wenigen benötigten Spawn-Wakeup-Timer werden direkt vom RegionSpawnService besessen und beim Shutdown deterministisch beendet.

### Architekturentscheidungen

Bewusst nicht übernommen wurden:

- synchrone Region-Datei-I/O im Serverthread;
- globale Bukkit-Manager als Dependency-Quelle;
- die alte RegionManager-Implementierung als unveränderte Kopie;
- die alte WakeScheduler-Abhängigkeit;
- parallele Persistenzquellen für Regionszustand.

Die Region-Persistenz bleibt ausschließlich in RegionRepository, der Runtime-State ausschließlich in RegionManager.

### Verifikation

GitHub Actions Workflow 35872994976 für Commit 73fc9372ac65cc494c1d1bcc0ec5801c327f69b7 lief vollständig mit success durch. Damit wurden gradle clean build --no-daemon --stacktrace, Source-API-Grenzprüfungen und Plugin-Artefaktprüfung erfolgreich bestätigt.

Nächster abhängiger Bereich ist Berufe/Crafting.


### Phase 13 — Berufe / Crafting

Status: **implementiert; CI-verifiziert**

Der Berufs-/Crafting-Bereich wurde vor der Implementierung gegen den tatsächlichen Referenzbestand von `main` geprüft. Die bestehende `PlayerProfile`-Persistenz enthielt bereits die fachlich benötigten Quellen für Berufslevel, Berufserfahrung, erlernte Berufe und freigeschaltete Rezepte. Diese Datenstruktur wurde deshalb nicht dupliziert.

Neu aufgebaut wurden:

- `Profession` als zentrale Berufsdefinition mit den neun Referenzberufen und Levelbereich 1–100;
- immutable `CraftRecipe`-Domainmodell;
- `CraftingCategory` als fachliche Rezeptkategorie;
- `CraftingRarityRoller` mit der bestehenden Raritätsverteilung;
- `CraftingRecipeRegistry` als validierte Runtime-Quelle der Rezeptdefinitionen;
- kanonische Rezeptressource `data/recipes/crafting-recipes.json` mit **300** Rezepten:
  - 61 BLACKSMITH
  - 25 FARMER
  - 23 COOK
  - 26 TAILOR
  - 39 ALCHEMIST
  - 58 MASON
  - 68 SCHOLAR
- Material-/Alias-Auflösung und Validierung von Tränken und Verzauberungen;
- `ProfessionService` für Berufserlernen, Berufs-XP, Levelberechnung und Rezeptfreischaltung;
- `CraftingService` für serverseitiges Herstellen, Materialprüfung/-verbrauch, Vanilla-Potion- und Enchanted-Book-Erzeugung sowie Berufs-XP;
- `ProfessionActivityListener` für die im Referenzzustand vorhandenen XP-/Passivmechaniken:
  - Bergbau/Metallurgie
  - Landwirtschaft
  - Holzfällen inklusive sicherem Baumfällen ab dem vorgesehenen Level
  - Steinmetz
  - Alchemisten-Pflanzen
  - Gelehrten-XP
  - Fischer-XP, Fangmengen- und Schatzchance
  - Koch-/Schneider-XP durch Tierdrops
  - Verzauberungs-XP
  - Amboss-XP
  - Berufs-XP bei passenden Quest-ID-Präfixen
- `ProfessionSystem` als Lifecycle-Besitzer mit eigenem asynchronen `PixelRPG-ProfessionIO`-Executor;
- `ProfessionDialogService` als native Paper-26.2-Dialogoberfläche für Berufslehrer, Kategorien, Rezeptdetails, Freischaltung und Herstellung;
- `ProfessionNpcListener` als getrennte NPC-/Berufs-Interaktionsgrenze.

### Architekturentscheidungen

Bewusst nicht übernommen wurden:

- die alte `ProfessionSystem`-Verdrahtung mit direktem Plugin-Singleton;
- `CraftingGUI`/Inventar-GUI. Die neue Rebuild-Architektur verwendet für die aktuelle Berufsinteraktion native Paper-Dialoge; die allgemeine Command-/UI-Schicht bleibt gemäß Audit-Reihenfolge Phase 17;
- eine zweite Persistenzquelle für Berufsstate oder Rezeptfreischaltungen;
- synchrones Laden der umfangreichen Rezeptdatei im Plugin-Hauptpfad.

Berufsstate bleibt ausschließlich im `PlayerProfile`. Rezeptdefinitionen sind statischer Content und werden über das eigene Profession-I/O außerhalb des Serverthreads geladen. Crafting und World-/Inventory-Operationen bleiben auf dem Serverthread.

### Verifikation

GitHub Actions Workflow **35875087550** für Commit **b64d8359656d28a9b55d7430e388f41e8e1b46ac** lief vollständig mit **success** durch.

Damit wurden erfolgreich verifiziert:

- `gradle clean build --no-daemon --stacktrace`;
- bestehende Source-API-Grenzprüfungen;
- bestehende Plugin-Artefaktprüfung inklusive Third-Party-Relocations/JDBC-Service.

Ein vorheriger Lauf **35874911327** scheiterte ausschließlich an zwei konkret ermittelten Compile-Fehlern in der Quest-Integration des neuen ProfessionActivityListener: `QuestCompletedEvent` stellt `playerId()`/`questId()` bereit, nicht `getPlayer()`/`getQuestId()`. Die Integration wurde daraufhin auf die tatsächliche Event-API korrigiert und über Workflow 35875087550 erfolgreich verifiziert.

Nächster abhängiger Bereich ist **Companions**.

### Phase 14 — Companions

Status: **implementiert; CI-verifiziert**

Der Companion-Bereich wurde vor der Implementierung gegen den tatsächlichen Referenzbestand von main forensisch geprüft. Der Referenzzustand umfasst Companion-Definitionen, Spielerzustand, Progression, Equipment, Skin-Persistenz, Runtime-Follow, Wolf-Combat, Mounting, Boss-Unlocks und die Companion-Dialog-/Command-Schicht.

Auf rebuild wurde der Bereich bewusst neu aufgebaut:

- CompanionDefinition bildet statische Companion-Daten aus companions.json ab;
- CompanionRegistry validiert und hält die immutable Definition-Quelle;
- die kanonische Ressource data/companions.json wurde vollständig aus dem Referenzzustand übernommen;
- Companion-Spielerzustand liegt jetzt ausschließlich in PlayerProfile über CompanionState;
- Companion-Level, XP, Aktivstatus, Name, Equipment und aufgelöste Mannequin-Skin-Texture werden gemeinsam mit dem bestehenden PlayerProfile persistiert;
- YAML-Persistenz schreibt den Companion-State in die vorhandene Player-Datei;
- MySQL-Persistenz verwendet eine eigene pixelrpg_player_companions-Tabelle innerhalb derselben Profile-Transaktion;
- Datenbankschema wurde auf Version 3 erweitert;
- es existiert keine separate Companion-/Equipment-Persistenz mehr außerhalb des PlayerProfile;
- CompanionService ist die fachliche Runtime-Grenze für Freischalten, Aktivieren, Deaktivieren, Umbenennen, Equipment, Skin-State und XP;
- Companion-Definitionen werden über einen eigenen PixelRPG-CompanionIO-Executor außerhalb des Serverthreads geladen;
- Companion-Runtime ist eventgetrieben: Join, Bewegung, Weltwechsel, Quit, Tod und gezielte Wake-Ups statt eines globalen permanenten Polling-Tasks;
- aktive Companion-Entities werden ausschließlich über UUID-Indizes verfolgt;
- Follow-/Teleport-Verhalten, skalierte Attribute und die spezielle tamed-Wolf-Kampfreaktion des Referenzzustands wurden in die neue Runtime-Schicht übernommen;
- normale Companion-Entities sind gegen Vanilla-Schaden geschützt; Unique-Mannequin-Companions bleiben davon ausgenommen;
- Mannequin-Skins verwenden weiterhin MannequinSkinResolver; bereits aufgelöste Texture-Properties werden im PlayerProfile gespeichert und nach Neustart zuerst wieder angewendet;
- native Paper-26.2-Dialoge ersetzen die alte Companion-Inventar-/UI-Schicht für Besitz, Aktivierung, Progression und Umbenennung;
- COMPANION wurde als eigener NPC-Typ/Funktionspunkt für die native NPC-Interaktion ergänzt;
- Quest-Abschluss veröffentlicht jetzt das bestehende QuestCompletedEvent, wodurch aktive Companions ihre konfigurierte Quest-XP erhalten können;
- Boss-Reward-Integration bleibt bewusst an die spätere Combat/Boss-Phase gekoppelt, da der vollständige Boss-Domainbereich gemäß Audit-Reihenfolge erst in Phase 16 aufgebaut wird. Die Companion-Definitionen besitzen die dafür notwendige datengetriebene Unlock-Information bereits.

### Bewusst nicht übernommen

Nicht als unveränderte Kopie übernommen wurden:

- CompanionEquipmentStore als zweite Persistenzquelle;
- der alte permanente Companion-Stats-Refresh-Task;
- globale Bukkit-/PixelRPGPlugin.getInstance()-Dependency-Zugriffe;
- die alte Inventar-Equipment-GUI;
- die alte administrative Command-Implementierung. Commands/UI bleiben gemäß Audit-Reihenfolge Teil der Phase 17-Schicht;
- die alte Boss-Listener-Kopplung, solange das Combat/Boss-System selbst noch nicht auf rebuild existiert.

Damit bleibt Companion-State fachlich eindeutig im PlayerProfile, während statische Definitionen, Runtime-Entity-Verwaltung, Dialoge und Persistence sauber getrennt sind.

### Verifikation

GitHub Actions Workflow 35879395929 für den Companion-Stand lief vollständig mit **success** durch.

Erfolgreich verifiziert wurden:

- gradle clean build --no-daemon --stacktrace;
- Source-API-Grenzen;
- Plugin-Artefaktprüfung;
- Third-Party-Relocations und JDBC-Service-Prüfung.

Der erste Companion-Build 35879040919 scheiterte an konkret ermittelten Compile-Fehlern in den neu hinzugefügten Integrationen. Diese wurden anhand des CI-Logs korrigiert, insbesondere bei aktuellen NPC-/Skin-APIs, Quest-Event-Import, Companion-NPC-Funktion und Runtime-Typprüfungen. Der anschließend vollständig durchgelaufene Workflow 35879395929 bestätigt den korrigierten Stand.

Nächster abhängiger Bereich ist Shops/Trading.

### Phase 15 — Shops / Trading

Status: **implementiert; CI-verifiziert**

Der Shop-/Trading-Bereich wurde vor der Implementierung gegen den tatsächlichen Referenzbestand von main geprüft. Der Referenzzustand bestand aus ShopEntry, ShopManager, ShopGUI, ShopEditorGUI, ShopBehavior sowie dem TradeDepotListing-/TradeDepotManager-Bereich mit separater Handelswaren-Persistenz und nativer Texteingabe für Verkaufspreise.

Auf rebuild wurde der Bereich fachlich getrennt neu aufgebaut:

- ShopEntry bleibt das immutable Angebot mit unabhängigem Kauf- und Verkaufspreis;
- ShopRepository übernimmt die shops.yml-Persistenz asynchron und atomar;
- alte Shopdaten mit dem historischen price-Feld werden beim Laden weiterhin verstanden und auf getrennte Kauf-/Verkaufspreise normalisiert;
- ShopService ist die fachliche Shop-Grenze für Kaufen, Verkaufen, Tradeability und Admin-Bestandsänderungen;
- Käufe und Verkäufe verwenden weiterhin den bestehenden PlayerProfile als autoritative Geldquelle;
- handelbare Items werden über den bestehenden ItemService.isEconomySafeItem(...) geprüft;
- Käufe prüfen den freien Inventarplatz vor dem Geldabzug;
- Verkäufe entfernen weiterhin genau einen passenden Itemstapel-Eintrag in der bestehenden Referenzsemantik;
- ShopDialogService ersetzt die alte Inventar-Shop-GUI durch native Paper-26.2-Dialoge;
- ShopNpcListener bindet NpcType.SHOP direkt an die neue native Interaktion;
- TradeDepotListing bildet weiterhin ID, Verkäufer, Item, Preis und 7-Tage-Ablaufzeit ab;
- TradeDepotRepository persistiert Listings und ausstehende Verkäuferauszahlungen asynchron;
- TradeGoodsRepository bildet das separate 54-Slot-Handelsfach des Referenzzustands als eigene Persistenzquelle ab;
- TradeDepotService übernimmt Kauf, Rücknahme, Ablauf, 5-%-Verkaufsgebühr, ausstehende Auszahlungen und Handelswaren;
- abgelaufene oder zurückgenommene Ware landet weiterhin im Handelsfach statt direkt im Inventar;
- gekaufte Ware landet weiterhin im Handelsfach;
- TradeDepotDialogService ersetzt die alte Trade-Depot-Inventar-GUI durch native Dialoge;
- das Einstellen einer Handelsware erfolgt ohne alte Inventar-GUI über native Texteingabe für Inventarslot und Verkaufspreis;
- Shop-Dialog und Handelsdepot sind über die bestehende SHOP-NPC-Interaktion erreichbar;
- Shop-, Trade-Depot- und Trade-Goods-I/O besitzen getrennte Executor und deterministische Shutdown-Pfade.

### Bewusst nicht übernommen

Nicht als unveränderte Kopie übernommen wurden:

- ShopGUI;
- ShopEditorGUI;
- TradeDepotGUI;
- TradeDepotSellGUI;
- die alte ShopBehavior-God-Integration;
- die alte ShopSubCommand-Verdrahtung. Commands und administrative Werkzeuge bleiben gemäß Audit-Reihenfolge Phase 17;
- die vollständige alte BankStorageService-GUI/Persistenz. Für Phase 15 wurde nur der für das Handelsdepot fachlich benötigte separate Handelswaren-Speicher neu aufgebaut. Eine allgemeine Bank-Funktion bleibt ein späteres abhängiges UI-/Command-Thema.

Damit besitzt Shop-/Trading-State jeweils eine eindeutige fachliche Source of Truth, während die Darstellung vollständig auf native Paper-Dialoge umgestellt ist.

### Verifikation

Der erste CI-Lauf 35881240481 schlug an einem konkreten Compile-Fehler in TradeDepotDialogService fehl: der verwendete UUID-Typ war nicht importiert. Dieser Fehler wurde direkt anhand des CI-Logs korrigiert.

Der anschließende vollständige GitHub-Actions-Lauf 35881419581 für Commit 220c40fad3d439e9480dc5abef7401164cfc06e7 lief mit success durch.

Erfolgreich verifiziert wurden:

- gradle clean build --no-daemon --stacktrace;
- Source-API-Grenzprüfungen;
- Plugin-Artefaktprüfung;
- Third-Party-Relocations/JDBC-Service-Prüfung.

Nächster abhängiger Bereich ist Combat / Bosse / Stats.


### Phase 16 — Combat / Bosse / Stats

Status: **implementiert; CI-verifiziert**

Der Combat-/Boss-/Stats-Bereich wurde vor der Umsetzung gegen den tatsächlichen Referenzbestand von `main` geprüft. Übernommen wurden die fachlichen Kernbereiche für Character Stats, RPG-Schadensberechnung, Combat-State, aktive Mob-Skalierung, Loot/XP, Weapon-Abilities, Boss-Definitionen, Boss-Phasen, Attack-Patterns, Boss-Teilnahme und Boss-Rewards. Die technische Einbindung wurde an die bereits auf `rebuild` vorhandenen PlayerProfile-, Item-, Companion- und Lifecycle-Strukturen angepasst.

Neu auf `rebuild` integriert:

- `CharacterStatType` und `StatisticsAPI` als öffentliche Stat-Schnittstelle;
- zentrale `StatEngine` mit gecachten Charakterwerten für HP, Armor, Bewegung, Reichweite, Crit, Crit-Damage, Lifesteal und Attack-Power;
- Stat-Aggregation aus tatsächlich ausgerüsteten PixelRPG-Items über die bestehende PDC-/Itemstruktur;
- serverseitige Attribute-Modifikatoren für aktuelle Paper-26.2-Attribute;
- `RPGStatsListener` zur Neuberechnung bei Join, Registrierung, Level-Up und Equipmentänderungen;
- `StatisticsService` als Zugriffsschicht auf den bereits in `PlayerProfile` persistierten Statistik-State;
- `CombatDamageCalculator` für Raw-Damage, Armor-Mitigation, Crit, Lifesteal und Mob-/Boss-Skalierung;
- `CombatDamageListener` für zentrale Spieler-/Mob-Schadensverarbeitung, RPG-Zielschutz und Boss-Hit-Cap;
- `CombatStateService` mit zeitlich begrenztem Combat-State und Combat-Enter-/Exit-Events;
- `MobScalingConfig` mit kanonischer `data/mob-scaling.json`-Quelle;
- eventgetriebene `MobLevelScalingListener`-Skalierung mit WakeScheduler statt globalem permanenterem Mob-Polling;
- RPG-Mob-Nameplates mit per-Viewer TextDisplay und begrenzter Laufzeit;
- Monster-XP-Verteilung und Loot-Drops über die vorhandene PlayerProfile-/Item-/Guild-Currency-Struktur;
- Soulbound-Todesschutz über den bestehenden `SoulboundService`;
- Weapon-Ability-Engine, Input-Listener und Feuerball-Ability;
- Boss-Domain mit `BossDefinition`, `BossPhase`, `ActiveBoss`, Loot-Konfiguration und Attack-Pattern-Registry;
- vier bestehende Boss-Attack-Patterns: Enrage, Projectile Volley, Slam und Summon Adds;
- `BossManager` für Spawn, Base-Stats, Bossbar, Phasen, Attack-Timer, Damage-Contribution, Rewards und Cleanup;
- `BossRepository` mit bestehender YAML-Definitionsstruktur einschließlich Biome- und World-Boss-Daten;
- `BiomeBossSpawnTask` für seltene, begrenzte Biome-Boss-Begegnungen;
- Boss-Damage-Contribution und `BossDefeatedEvent`;
- Boss-Reward-Verteilung über bestehende Economy-/Guild- und Item-Services;
- Companion-Boss-Unlock kann jetzt über das vorhandene `BossDefeatedEvent`-System angebunden werden;
- aktuelle Boss-/Combat-PDC-Keys in `RPGKeys`, ohne statische Live-Server-Referenz;
- öffentliche `GuildAPI`, `EconomyAPI` und `PartyAPI`-Grenzen für den Combat-/Boss-Bereich. Die aktuelle Rebuild-Party-Implementierung bleibt bewusst eine No-Party-Basis, bis die allgemeine Party-Domain in ihrer Audit-Reihenfolge aufgebaut wird.

### Architekturentscheidungen

- Keine zweite Geldquelle: Economy-Adapter arbeiten direkt auf `PlayerProfile`.
- Keine zweite Spieler-Statpersistenz: Character-Statistics bleiben im bestehenden `PlayerProfile`.
- Keine zweite Item-/Loot-Persistenz: Drops verwenden den bestehenden `ItemService` und `GuildCurrencyItemFactory`.
- Kein globaler Plugin-Singleton wurde für die neue Combat-Integration eingeführt.
- Die auf `main` vorhandenen Legacy-Singleton-/GUI-Abhängigkeiten wurden nicht als technische Grundlage für `rebuild` übernommen.
- Native Adventure Components bleiben die Chat-/Actionbar-Darstellung.
- Die alte Inventory-/Command-Schicht bleibt weiterhin Phase 17.
- Boss-Rewards verwenden den bestehenden PlayerProfile-/Economy-/Item-Domain-State.
- Combat-Runtime bleibt eventgetrieben; aktive Skalierung verwendet begrenzte Wake-Ups statt eines globalen Dauer-Pollers.

### Korrekturverifikation

Workflow **35887659198** für Commit **cf5fc52ac36a304edaa24aa136882e9719926366**: **success**. Der Build inklusive Source-API-Boundaries und Plugin-Artefaktprüfung ist erfolgreich.

### Bewusst nicht unverändert übernommen

Nicht als unveränderte Kopie übernommen wurden:

- alte globale `PixelRPGPlugin.getInstance()`-Abhängigkeiten;
- alte Inventory-GUIs und Combat-/Boss-Commands;
- eine zweite Party-Persistenz bzw. ein erfundenes Party-System;
- alte statische Bukkit-Live-Objekt-Halter;
- die alte Companion-Stat-Doppelstruktur. Die Rebuild-StatEngine verwendet die bereits vorhandenen Rebuild-Itemdaten als autoritative Ausrüstungswerte;
- nicht benötigte Legacy-Kompatibilitätsverdrahtung.

### Verifikation

Die CI-Verifikation wurde mehrfach forensisch korrigiert. Die aufgetretenen Fehler waren konkrete API-/Architekturabweichungen zwischen `main` und dem bereits neu aufgebauten `rebuild`-Stand und wurden anhand der tatsächlichen Compiler-Logs korrigiert.

Der abschließende GitHub-Actions-Workflow **35884651456** für Commit **2bbe2de0a17cfbade6841d1ba12ebbd56d4acb40** lief vollständig mit **success** durch.

Erfolgreich verifiziert wurden:

- `gradle clean build --no-daemon --stacktrace`;
- Source-API-Grenzprüfungen;
- statische Live-Bukkit-Referenzprüfung;
- Plugin-Artefaktprüfung;
- Third-Party-Relocations;
- JDBC-Service-/ShadowJar-Prüfung.

Nächster Audit-Schritt: **Phase 17 — Commands / Administration / finale UI- und Integrationsschicht**.


### Phase 17 — Commands / Administration / finale Integrationsschicht

Status: **erneut forensisch geprüft; Korrekturen implementiert; CI-verifiziert**

Die Phase 17 wurde nach der ersten Freigabe nochmals vollständig gegen den tatsächlichen main-Referenzbestand und den aktuellen rebuild-Code geprüft. Dabei wurde nicht nur der erfolgreiche Build der ursprünglichen Phase 17 akzeptiert, sondern die tatsächliche Laufzeit-/Verhaltensabdeckung der neuen Command-Schicht erneut mit den Referenzbefehlen verglichen.

Dabei wurden folgende konkrete Abweichungen gefunden und behoben:

- NPC-create und create-id akzeptieren jetzt wieder die im Referenzsystem vorhandene optionale Skin-Quelle, statt sie fälschlich als Teil des NPC-Namens zu behandeln;
- die Referenzfunktion für Filler-NPCs ist wieder als expliziter npc-filler-Pfad vorhanden;
- NPC-Tab-Completion kennt die gültigen NpcType-Werte für die Erstellungsbefehle;
- /pixelrpg item give validiert die Mengenangabe jetzt vor dem Parsen/Erzeugen und liefert bei ungültigen Zahlen eine kontrollierte Fehlermeldung statt einer ungefangenen NumberFormatException;
- Item-Mengen werden strikt auf den zulässigen Bereich 1–64 begrenzt; UNIQUE-Items bleiben auf Menge 1 beschränkt;
- /pixelrpg boss event prüft vor dem Spawn, ob derselbe World-Boss bereits aktiv ist, entsprechend dem Referenzverhalten;
- die native Paper-26.2-BasicCommand-/Lifecycle-Registrierung bleibt unverändert;
- die neue Command-Schicht verwendet weiterhin ausschließlich Adventure Components und aktuelle Paper-26.2-Command-APIs;
- keine Legacy-CommandExecutor-/TabCompleter-Registrierung wurde wieder eingeführt.

Bewusst weiterhin nicht übernommen wurden fachliche Bereiche, deren Rebuild-Domain gemäß Audit noch nicht vollständig vorhanden ist, insbesondere Party-/Guild-Kommandos als eigenständige alte Implementierungen. Es wird kein Schein-Command für ein nicht vorhandenes Domain-System bereitgestellt.

### Verifikation

Der Korrekturstand wurde auf rebuild als Commit 251b04a1b62f4baa0750bbfe6b4bc39d0aa8f80b geschrieben. Der anschließende Build des Audit-Commits schlug zunächst an einem konkreten Java-25-Lambda-Compilefehler in der Mengenvalidierung von `PixelRPGCommand` fehl. Die Ursache wurde anhand des CI-Compilerlogs auf die nicht effektiv finale lokale Variable `amount` innerhalb des `ifPresentOrElse`-Lambdas zurückgeführt. Dies wurde im Commit `cf5fc52ac36a304edaa24aa136882e9719926366` durch eine finale `requestedAmount`-Variable korrigiert.

Die ursprüngliche Phase-17-Verifikation bleibt dokumentiert: Workflow 35885711518 bestätigte für Commit a1188ef79bcd28925205e1162bf643e4f1c83c07 den vollständigen Build inklusive Source-Grenzprüfungen, Live-Server-Referenzprüfung, Artefaktprüfung, Third-Party-Relocations und JDBC-Service-Prüfung.

Die erneute Prüfung hat damit reale Verhaltensfehler in der ersten Phase-17-Fassung gefunden und auf der aktuellen Paper-26.2-/Java-25-Basis korrigiert. Der korrigierte Stand `cf5fc52ac36a304edaa24aa136882e9719926366` wurde durch GitHub Actions Workflow `35887659198` vollständig verifiziert: `gradle clean build --no-daemon --stacktrace`, Source-API-Grenzprüfungen und Plugin-Artefaktprüfung liefen erfolgreich.

### Bewusst nicht unverändert übernommen

Nicht portiert wurden:

- alte `AbstractGUI`-/Inventory-GUI-Schicht;
- alte `ShopGUI`, `TradeDepotGUI`, `CraftingGUI`, `QuestLogGUI` und `PartyGUI`;
- alte Bukkit-`CommandExecutor`-/`TabCompleter`-Registrierung;
- alte globale `PixelRPGPlugin.getInstance()`-Command-Abhängigkeiten;
- nicht rekonstruierte Party-Domain. Die Command-Schicht behauptet keine Party-Funktionalität, solange die Party-Domain nicht separat aufgebaut wurde.

Die bereits auf `rebuild` vorhandenen nativen Dialoge bleiben die UI-Schicht für Shops, Trading, Berufe, Companions, Regionen und NPC-Dialoge; Commands dienen der Administration und den explizit erforderlichen textbasierten Einstiegsfunktionen.

### Verifikation

Der erste CI-Lauf **35885558248** schlug an einem konkreten Paper-26.2-API-Fehler in `PixelRPGCommand` fehl: `Player#rayTraceEntities` erwartet in der verwendeten aktuellen API einen ganzzahligen Radius. Der Fehler wurde anhand des Compiler-Logs auf `8` korrigiert.

Der anschließende Lauf **35885711518** für Commit **a1188ef79bcd28925205e1162bf643e4f1c83c07** verifizierte erfolgreich:

- `gradle clean build --no-daemon --stacktrace`;
- Source-API-Grenzprüfungen;
- statische Live-Server-Referenzprüfung;
- Plugin-Artefaktprüfung;
- Third-Party-Relocations;
- JDBC-Service-/ShadowJar-Prüfung.

Damit ist die im Audit definierte Phase 17 technisch integriert und CI-verifiziert.


---

## 18. Forensische Connectivity-Prüfung — NPC / Quest / Party / Guild

Status: **Connectivity-Batch abgeschlossen; Release-Parität weiterhin nicht freigegeben**

Die Prüfung wurde nicht anhand von Dateinamen, sondern entlang der Runtime-Kette durchgeführt:

`Definition/ID -> Repository -> Runtime -> Entity -> Listener -> Service -> Dialog/Action -> Persistence`

### NPC-Typen

Aktueller `NpcType`-Bestand:

- `RECEPTION` -> `ReceptionNpcListener`
- `PROFESSION_*` -> `ProfessionNpcListener`
- `QUEST` -> `QuestNpcListener` + `QuestNpcDialogService`
- `SHOP` -> `ShopNpcListener`
- `TRAVEL` -> `TravelNpcListener`
- `FILLER` -> `FillerNpcListener`
- `STORY` -> `StoryNpcInteractionListener`
- `BANKER` -> `BankerNpcListener`
- `COMPANION` -> `CompanionNpcListener`

Damit besitzt jeder aktuell definierte NPC-Typ eine konkrete Interaktionsroute. `FILLER` bleibt bewusst fachlich leichtgewichtig und dient als `TALK_TO_NPC`-Questziel.

### NPC-Persistenz

Die Kette ist vollständig:

`YamlNpcRepository -> NpcRuntimeManager -> NpcRecord -> spawned Mannequin -> npcId/npcType PDC -> Listener lookup`

Die aufgelöste Skin-Texture inklusive Signatur bleibt persistent und wird beim erneuten Spawn wieder angewendet.

### Quest-Runtime

Folgende bislang im Rebuild fehlende Laufzeitverbindungen wurden wiederhergestellt:

- `QUEST` NPC -> natives Quest-Dialogsystem;
- `HUNT` -> `EntityDeathEvent`;
- `COLLECT` -> Inventory/Pickup/Drop/Consume-Refresh;
- `TALK_TO_NPC` -> FILLER-NPC-Interaktion;
- Questabschluss -> Persistenz;
- Questbelohnungen -> Geld, XP und Items;
- Companion-Belohnungen -> Companion-Domain.

Die kanonische Questdatei enthält nach Abgleich mit `main` jetzt **209 Definitionen**. Die 46 Definitionen aus `quests_content_expansion_01.json`, die im Rebuild fehlten, wurden fachlich in `data/quests/definitions.json` integriert. Die alte Crafting-Order-Datei bleibt bewusst ausgeschlossen, da sie auf das entfernte Legacy-Rezeptmodell verweist.

### Party / Guild

Die zuvor vollständig entfernten Party-/Guild-Domänen wurden nicht als Fake-Adapter belassen:

- persistente `PartyManager`-Domain wiederhergestellt;
- `PartyAPI` tatsächlich registriert;
- Party-Disconnect-Lifecycle wieder verbunden;
- persistente `GuildManager`-Domain wiederhergestellt;
- `GuildAPI` verwendet jetzt die reale Guild-Domain statt des bisherigen falschen "registriert = Gilde"-Adapters;
- Party-/Guild-Kommandos sind wieder direkt über `/pixelrpg party ...` und `/pixelrpg guild ...` erreichbar;
- Quick-Actions-Guild-Eintrag zeigt nicht mehr künstlich "Gildensystem deaktiviert", sondern die reale Guild-Domain.

### Verbleibende bewusst dokumentierte Paritätslücken

Folgende historische Komponenten sind im Rebuild weiterhin durch neue Architektur ersetzt oder noch nicht vollständig rekonstruiert:

- alte Bukkit-GUI-Schicht;
- alter Guild-Bank-Speicher;
- alter Trade-/Shop-Manager;
- altes Scoreboard-/Playtime-Modul;
- alte Companion-Mount-/Equipment-GUI-Schicht;
- vollständiger Resourcepack-Bestand ist noch nicht vollständig in `rebuild` zurückgeführt.

Diese Punkte dürfen nicht als "fertig" markiert werden. Sie bleiben offene Paritätsarbeiten und sind vor einer Release-Freigabe erneut gegen `main` zu prüfen.

### Technische Integritätsregel

Es wurden keine alten 1.21.x-Dialogimplementierungen, CraftBukkit-Klassen, Legacy-NMS-Pakete oder `ChatColor`-Abhängigkeiten zurückgeführt.

Der Rebuild bleibt die aktive Entwicklungsbasis. `main` und `test` bleiben unverändert Referenzzustand.

## 19. Forensischer Paritäts-/Rebuild-Fortschritt — Abschlussrunde

Status: **implementiert und CI-verifiziert**

Die abschließende Gegenprüfung gegen den unveränderten `main`-Referenzstand wurde auf Dateibaum-, Domain-, Runtime-, Persistenz- und Integrationsniveau fortgeführt.

Ergänzt bzw. geschlossen wurden insbesondere:

- vollständiger fehlender Resourcepack-Bestand aus `main` wiederhergestellt, ohne die bereits modernisierten Rebuild-Dateien zu überschreiben;
- Scoreboard- und Playtime-Funktion als neue, explizit verdrahtete Rebuild-Services wiederhergestellt;
- Guild-Bank inklusive asynchroner YAML-Persistenz und Membership-Schutz wieder angebunden;
- BANKER-NPC mit Gildenbank-Aktion verbunden;
- Gildenkompass und Reise-Navigation auf die aktuelle `NpcRuntimeManager`-Architektur umgestellt;
- Companion-Mount-Funktion auf die aktuelle `CompanionDefinition`-/Runtime-Struktur neu angebunden;
- eventgetriebenes NPC-Look-Verhalten ergänzt;
- gemeinsame asynchrone Dateipersistenz über `AsyncFileWriter` wieder als Core-Infrastruktur hergestellt;
- mehrere zuvor nur dokumentierte, aber technisch nicht mehr verbundene APIs/Listener anhand des tatsächlichen Compiler-/Referenzstands korrigiert;
- Quest-/Companion-/NPC-Integrationen auf die tatsächlichen Rebuild-Repository- und Runtime-APIs bereinigt;
- die alte separate Guild-Command-Registrierung entfernt; Commands bleiben vollständig in der aktuellen Paper-Lifecycle-Composition-Root.

### Verifikation

GitHub Actions Workflow **35900296961** für Commit **24b14f4326ce910229b9c3a79893a95cba2addf2** lief vollständig mit **success** durch.

Erfolgreich ausgeführt:

- `gradle clean build --no-daemon --stacktrace`;
- Source-API-Boundary-Prüfungen;
- Plugin-Artefaktprüfung;
- Third-Party-Relocations;
- JDBC-Service-/ShadowJar-Prüfung.

Der zuvor fehlgeschlagene Build wurde nicht übersprungen oder maskiert. Die konkreten Compilerfehler wurden anhand des GitHub-Actions-Logs korrigiert und anschließend erneut vollständig verifiziert.

Die verbleibenden historischen Dateien aus `main`, die durch bewusst modernisierte Rebuild-Domänen ersetzt wurden, werden nicht blind zurückkopiert. Dazu gehören insbesondere die alte Inventory-GUI-Schicht, alte God-Manager und alte Legacy-Command-Strukturen. Ihre fachlichen Funktionen sind, soweit im Referenzbestand relevant, in den neuen Domain-/Dialog-/Service-Schichten abzubilden.

`main` und `test` wurden in dieser Runde nicht verändert. Der aktive Neuaufbau bleibt ausschließlich auf `rebuild`.

## 20. Clean-Rebuild-Recovery — 2026-09-23

Der aktuell aktive `rebuild`-Stand wurde auf den letzten forensisch aufgebauten und vollständig CI-verifizierten Paritätsstand `ebcc8ea07a8c5ce225b47bbfee77260bc246a540` zurückgesetzt. Dieser Stand basiert auf dem unveränderten `main`-Referenzcommit `b02747a21cde6d1308c59a7f027cda80877cfe50` und enthält die bewusst neu strukturierten Rebuild-Domänen statt eines blinden `src/main`-Kopierens.

Forensisch bestätigt: 203 Java-Dateien im Rebuild gegenüber 247 im Referenzzustand. Die entfernten Dateien sind Bestandteil der dokumentierten Architekturentscheidungen; insbesondere alte Inventory-GUIs, Legacy-Command-Schichten, doppelte Runtime-/Persistence-Modelle und ersetzte God-Manager wurden nicht wiederhergestellt.

Dieser Stand ist damit die technische Clean-Rebuild-Basis. Die abschließende Verifikation läuft erneut über GitHub Actions auf dem aktuellen `rebuild`-HEAD.

---

## 21. Vollständige SOLL-Paritätsanalyse gegen `main` — 2026-09-24

Status: **NEUE verbindliche offene Arbeitsliste — Release-Parität NICHT erreicht**

Diese Sektion ergänzt und präzisiert die vorherigen Audit-Abschnitte. Sie hat Vorrang, wenn ältere Abschnitte einen Bereich bereits als abgeschlossen markieren, der hier durch die tatsächliche Gegenprüfung gegen `main` wieder als offen festgestellt wurde.

### 21.1 Verbindlicher Maßstab

Die Analyse wurde direkt gegen den eingefrorenen Referenzstand

`main @ b02747a21cde6d1308c59a7f027cda80877cfe50`

und gegen den tatsächlichen aktuellen Stand von `rebuild` durchgeführt.

Wichtig:

> **`main` ist nicht nur eine Feature-Liste. `main` definiert das SOLL für Verhalten, Bedienabläufe, Darstellung, Befehle, Menüs, Dialoge, NPC-Interaktionen, Navigation, Rückmeldungen und Datenflüsse.**

Der Rebuild darf intern vollständig anders aufgebaut sein. Er darf aber keine fachliche oder sichtbare Funktion verlieren.

Ein bloßes Ersetzen einer bestehenden `main`-GUI durch einen Dialog ist deshalb **keine automatische Parität**. Wenn `main` einen bestimmten Inventar-/Menü-Ablauf verwendet, muss dieser Ablauf im Rebuild funktional und visuell reproduziert werden. Die aktuelle Paper-26.2-Technik ist dabei zu verwenden; Legacy-Code wird nicht blind übernommen.

### 21.2 Umfang der Dateiforensik

Aktueller Tree-Vergleich:

- `main`: 321 Dateien
- `rebuild`: 272 Dateien
- `main`: 247 Java-Dateien
- `rebuild`: 203 Java-Dateien

Der Unterschied von 49 Dateien ist **nicht automatisch ein Fehler**, weil mehrere alte Klassen bewusst in neue Services zusammengeführt wurden.

Der Tree-Vergleich zeigt jedoch konkrete `main`-Komponenten, die im Rebuild **überhaupt keine direkte Entsprechung besitzen**. Diese wurden anschließend gegen die tatsächliche Runtime-Verdrahtung geprüft.

---

### 21.3 KRITISCH — Benutzeroberflächen / Präsentation

Im Referenzzustand existiert eine vollständige GUI-Schicht:

- `AbstractGUI`
- `GUIHolder`
- `GUIListener`
- `PartyGUI`
- `QuestLogGUI`
- `QuestDetailGUI`
- `CraftingGUI`
- `ShopGUI`
- `ShopEditorGUI`
- `TradeDepotGUI`
- `TradeDepotSellGUI`

Im aktuellen `rebuild` existiert kein `rpg/gui`-Paket.

Damit sind die folgenden SOLL-Darstellungen aktuell **nicht nachgewiesen und als offen zu behandeln**:

1. Party-Hauptmenü
2. Questlog-Hauptmenü
3. Questdetail-/Questaktionsansicht
4. Crafting-/Rezeptoberfläche
5. Spieler-Shop
6. Shop-Editor
7. Handelsdepot
8. Auswahl der Handelsware aus dem Spielerinventar
9. gemeinsame GUI-Interaktions-/Holder-Logik.

Das ist kein rein technischer Unterschied. Diese GUIs definieren konkrete Spielerabläufe, Slotbelegung, Texte, Klickaktionen, Navigation und Rückkehrverhalten.

**Verbindliche Folgearbeit:**

Die Präsentationsschicht aus `main` muss fachlich rekonstruiert werden. Die alte GUI-Implementierung darf nicht blind kopiert werden; die Darstellung und das Verhalten müssen jedoch erhalten bleiben.

---

### 21.4 KRITISCH — Command-Surface und Administration

Der Referenzzustand besitzt neben dem Root-Command eine deutlich größere Command-Struktur, u. a.:

- `debug`
- `edit`
- `quest` / Quest-Administration
- `dialogue`
- Party
- Guild
- Companion
- NPC
- Region
- Boss
- Item
- Player-Administration
- Shop-Administration.

Im aktuellen `rebuild` wird alles über `PixelRPGCommand` zentralisiert. Der aktuelle Command-Code enthält jedoch **nicht die vollständige Referenzoberfläche**.

Bereits konkret festgestellt:

- `debug` fehlt;
- `edit` fehlt;
- Quest-Administration fehlt;
- `dialogue` fehlt;
- Party-Befehle sind gegenüber `main` reduziert;
- Guild-Befehle sind gegenüber `main` reduziert;
- Shop-Administration ist gegenüber `main` reduziert;
- Item-Administration ist gegenüber `main` reduziert;
- Tab-Completion ist nicht vollständig mit der Referenzoberfläche gleichgezogen;
- `pixelrpg` und `rpgadmin` werden aktuell auf dieselbe Command-Implementierung gelegt, obwohl `main` unterschiedliche administrative Einstiege besitzt.

Besonders relevant:

#### Party

In `main` öffnet

`/pixelrpg party`

ohne Unterbefehl das Party-Menü und `info` öffnet ebenfalls die Party-Oberfläche.

Der aktuelle Rebuild-Command liefert hier überwiegend textbasierte Ausgabe.

Damit ist **Verhaltens- und Präsentationsparität nicht erreicht**.

Zusätzlich sind im Referenzzustand vorhanden:

- invite
- accept
- leave
- kick
- transfer
- disband
- info
- GUI-Navigation
- Mitgliederdarstellung
- Leader-/Invite-Zustände.

Alle diese Pfade müssen gegen die aktuelle Party-Domain erneut abgeglichen werden.

#### Guild

Der Referenzzustand besitzt:

- create
- invite
- accept
- leave
- info
- disband
- konkrete Result-/Fehlermeldungen
- Mitglieder-/Leader-Darstellung.

Der aktuelle Rebuild muss jeden dieser Pfade einschließlich Darstellung, Fehlermeldungen und Berechtigungen gegen `main` verifizieren.

#### Item

Die Referenzoberfläche besitzt u. a.:

- list
- give
- create
- inspect
- set
- Item-Stat-Bearbeitung
- spezielle Admin-Item-Funktionen.

Der aktuelle Rebuild deckt davon nicht nachweislich die vollständige Oberfläche ab.

#### Shop

`main` besitzt neben dem Spieler-Shop einen administrativen `ShopEditorGUI`.

Eine reine `shop list`-Funktion ist **keine Parität**.

---

### 21.5 KRITISCH — Persönliche Bank / Handelsfach / Banker

Der Referenzzustand besitzt beim BANKER-NPC eine vollständige Kette:

`BANKER NPC -> BankDialog -> Einzahlen/Auszahlen -> Bankfach -> Handelsfach -> Handelsdepot`

Zusätzlich existieren:

- `BankDialog`
- `BankInventoryHolder`
- `BankInventoryListener`
- `BankStorageService`
- persönliche Bankpersistenz
- separates Handelsfach
- Migration alter Handelsfachdaten
- asynchrones Schreiben
- mehrere Bankseiten
- Handelsdepot-Einstieg
- Gildenbank-Einstieg.

Der aktuelle `rebuild`-`BankerNpcListener` bietet dagegen nur:

- Gildenbank öffnen;
- feste Einzahlungsbeträge 10/100/500;
- feste Auszahlungsbeträge 10/100/500.

Die Referenzfunktionen

- frei wählbarer Einzahlungsbetrag;
- frei wählbarer Auszahlungsbetrag;
- persönliches Bankfach;
- Bankseiten;
- separates Handelsfach;
- persistente Bankinhalte;
- Handelsfach-Navigation;
- direkter Handelsdepot-Einstieg

sind im aktuellen Rebuild **nicht vorhanden**.

Dies ist eine bestätigte funktionale Paritätslücke und keine reine Architekturfrage.

---

### 21.6 KRITISCH — Companion-Ausrüstung

Der Referenzzustand besitzt:

- `CompanionEquipment`
- `CompanionEquipmentHolder`
- `CompanionEquipmentListener`
- `CompanionEquipmentStore`
- `CompanionCombatController`
- `CompanionFollowTask`
- `CompanionInstance`
- `CompanionProgression`
- `CompanionRuntimeRegistry`
- `CompanionStatsCalculator`
- `MannequinCompanionController`
- `CompanionBossRewardListener`.

Der aktuelle Rebuild besitzt zwar eine neue `CompanionSystem`-/Service-Struktur und Mount-/Runtime-Komponenten, aber die vollständige Referenz-Ausrüstungskette ist nicht vorhanden.

Insbesondere fehlen im Rebuild:

- Companion-Equipment-Inventar;
- Equipment-Holder;
- Equipment-Listener;
- persistenter Companion-Equipment-Speicher;
- die damit verbundene Spieleroberfläche.

Der Referenz-Dialog bietet ausdrücklich:

- Rufen/Wegschicken;
- Umbenennen;
- Ausrüstung öffnen.

Die Ausrüstungsfunktion darf im Rebuild daher nicht als optionales Altfeature betrachtet werden.

**Offen:** vollständige funktionale Rekonstruktion von Companion-Equipment inklusive Persistence, Slotregeln, Runtime-Anwendung und Darstellung.

---

### 21.7 Companion — weitere Parität ausdrücklich prüfen

Die im Rebuild zusammengeführte Companion-Architektur darf nicht anhand gleicher Dateinamen als ausreichend betrachtet werden.

Gegen `main` müssen explizit geprüft werden:

- Follow-Verhalten;
- Teleport-/Distanzregeln;
- Combat-Targeting;
- Attack-Timing;
- Schaden;
- Crit/Lifesteal/Ability-Daten;
- Level-/XP-Progression;
- Rarity-Multiplikatoren;
- Unique-Mannequin-Verhalten;
- Skin-Persistenz;
- Boss-Unlocks;
- Mount;
- Rename;
- Active/Inactive-State;
- Equipment;
- Quick-Actions-Einstieg;
- Shutdown/Logout-Recovery.

**Regel:** Zusammengeführte Klassen gelten erst dann als Paritätsersatz, wenn die oben genannten Verhaltenspfade im tatsächlichen Code nachgewiesen sind.

---

### 21.8 KRITISCH — Quest-System: Referenzlogik gegen Rebuild-Domain abgleichen

Der Referenzzustand enthält zusätzlich:

- `GlobalEventState`
- `QuestInventoryTracker`
- `QuestManager`
- `QuestMobKillListener`
- `QuestNavigationLifecycleListener`
- `QuestNavigationService`
- `QuestPassiveCheckTask`
- `QuestText`
- `Quest.java`.

Der Rebuild besitzt ein bewusst anderes Modell mit:

- `QuestDefinition`
- `QuestService`
- `QuestRepository`
- `QuestProgressListener`
- `QuestNpcDialogService`
- `QuestNpcListener`
- `QuestNavigation`
- `QuestCompanionRewardListener`.

Die neue Struktur ist architektonisch zulässig, aber folgende Referenzfunktionen müssen einzeln nachgewiesen werden:

- HUNT-Progression;
- COLLECT-Progression;
- Inventory-/Pickup-/Drop-/Consume-Aktualisierung;
- passive Questbedingungen;
- zeitabhängige Questbedingungen;
- globale Quest-/Eventzustände;
- Quest-Navigation;
- Navigation-Lifecycle;
- Questzielanzeige;
- Questtext-/Darstellung;
- Questlog;
- Questdetails;
- Start-/Abbruch-/Abschlussabläufe;
- Belohnungen;
- Folgequests;
- Voraussetzungen;
- Zeitlimits;
- Companion-Rewards.

Die kanonische Definitiondatei ist bereits vereinheitlicht. **Das ersetzt nicht die Prüfung der gesamten Quest-Runtime.**

---

### 21.9 KRITISCH — NPC-Verhalten und Präsentation

Der Referenzzustand verwendet:

- `NpcManager`
- `NpcBehaviorRegistry`
- `NpcInteractListener`
- `NpcLookTask`
- `NpcNameVisibilityService`
- konkrete Behaviors für:
  - Banker
  - Filler
  - Profession Trainer
  - Quest
  - Reception
  - Shop
  - Story
  - Travel.

Der Rebuild verwendet stattdessen eine Runtime-/Listener-Architektur.

Diese Architektur ist ausdrücklich gewollt, aber folgende SOLL-Verhalten müssen pro NPC-Typ geprüft werden:

- Interaktionsradius / Interaktionsbedingungen;
- Main-Hand-/Off-Hand-Verhalten;
- Namensanzeige;
- Blickverhalten;
- Schutzverhalten;
- Chunk Spawn/Despawn;
- Skin-Auflösung;
- Skin-Persistenz;
- professionelle Zuordnung;
- Quest-Dialog;
- Story-Dialog;
- Shop;
- Banker;
- Travel;
- Filler;
- Companion.

Die bloße Existenz eines Listeners gilt nicht als Paritätsnachweis.

---

### 21.10 KRITISCH — Crafting / Berufe / Darstellung

`main` besitzt `CraftingGUI` als sichtbare Crafting-Oberfläche.

Der Rebuild besitzt bereits:

- `ProfessionSystem`
- `CraftingService`
- `CraftingRecipeRegistry`
- `ProfessionDialogService`
- `ProfessionNpcListener`.

Die fachliche Crafting-Domain ist damit vorhanden. Die Referenzdarstellung und der komplette Benutzerablauf müssen jedoch weiterhin gegen `CraftingGUI` geprüft werden.

Zu vergleichen sind insbesondere:

- Rezeptliste;
- Kategorien;
- Seltenheitsanzeige;
- Zutaten;
- Mengen;
- Levelanforderung;
- Freischaltung;
- Craft-Aktion;
- Fehlermeldungen;
- Resultat;
- Navigation;
- NPC-Einstieg;
- Quest-/Crafting-Integration.

---

### 21.11 KRITISCH — Shop / Trade Depot

Der Rebuild besitzt neue Services/Dialoge:

- `ShopService`
- `ShopDialogService`
- `TradeDepotService`
- `TradeDepotDialogService`.

Diese ersetzen jedoch die `main`-GUIs:

- `ShopGUI`
- `ShopEditorGUI`
- `TradeDepotGUI`
- `TradeDepotSellGUI`.

Damit ist die fachliche Domain nicht automatisch gleich der Referenzdarstellung.

Zu prüfen:

- gleiche Itemanzeige;
- Kauf;
- Verkauf;
- Preise;
- Inventarplatzprüfung;
- Fehlermeldungen;
- Handelswaren-Auswahl;
- Verkaufspreis-Eingabe;
- Laufzeit;
- Rücknahme;
- Ablauf;
- Handelsfach;
- Auszahlungen;
- Admin-Editor;
- Navigation;
- NPC-Einstieg.

Insbesondere der administrative Shop-Editor ist aktuell als offene Funktion zu behandeln.

---

### 21.12 KRITISCH — öffentliche API / externe Integrationen

Der Referenzzustand enthält:

- `ApiVersion`
- `PixelRPGProvider`
- öffentliche API-Services.

`PixelRPGProvider.java` ist im aktuellen Rebuild nicht vorhanden.

Damit ist die externe Zugriffsschicht des Referenzzustands nicht vollständig reproduziert.

Zu prüfen bzw. wiederherzustellen:

- API-Version;
- Provider;
- ItemAPI;
- EconomyAPI;
- PartyAPI;
- GuildAPI;
- StatisticsAPI;
- Service-Registrierung;
- Fehlerverhalten bei fehlendem Service;
- binäre/semantische Kompatibilität der öffentlichen Schnittstellen.

Die interne Rebuild-Architektur darf dafür nicht wieder in globale God-Manager zurückfallen. Eine schlanke aktuelle Provider-Fassade ist ausreichend, sofern sie das Referenzverhalten reproduziert.

---

### 21.13 Quest-/Content-Ressourcen

`main` besitzt mehrere historische Questdateien:

- `quests_v2.json`
- `quests_additional.json`
- `quests_world_expansion.json`
- `quests_expansion_02.json`
- `quests_content_expansion_01.json`
- `quests_crafting_orders.json`.

Der Rebuild besitzt stattdessen die kanonische:

`data/quests/definitions.json`

Die Konsolidierung ist grundsätzlich richtig, aber jede entfernte Ressource muss gegen den tatsächlichen fachlichen Inhalt geprüft werden.

Besonders:

- keine Questdefinition darf durch Konsolidierung verloren gegangen sein;
- IDs müssen stabil bleiben;
- Folgequests müssen erhalten bleiben;
- Voraussetzungen müssen erhalten bleiben;
- Belohnungen müssen erhalten bleiben;
- Story-/NPC-Verweise müssen erhalten bleiben;
- Crafting-Quest-Verweise müssen gegen das neue Rezeptmodell geprüft werden.

Die bereits dokumentierte Zahl von 209 Definitionen ist ein Content-Nachweis, aber kein vollständiger Runtime-Paritätsnachweis.

---

### 21.14 Scoreboard / Playtime

Diese Systeme wurden im Rebuild bereits neu aufgebaut.

Trotzdem ist die SOLL-Präsentation gegen `main` zu prüfen:

- Inhalt;
- Reihenfolge;
- sichtbare Werte;
- Update-Zeitpunkt;
- Aktivierung/Deaktivierung;
- Join/Quit;
- Persistenz;
- Charakterkarte;
- Quick-Actions-/Reception-Verknüpfung.

Die bloße Existenz von `ScoreboardService` und `PlaytimeTracker` reicht nicht als Freigabenachweis.

---

### 21.15 Guild / Party

Die Domain-Klassen sind auf `rebuild` vorhanden.

Zusätzlich zur Domain muss die Referenzoberfläche vollständig reproduziert werden:

#### Party

- create
- invite
- accept
- leave
- kick
- transfer
- disband
- info
- PartyGUI
- Leader-Anzeige
- Mitgliederliste
- Invite-Status
- Share-Range
- Disconnect
- Quick-Actions
- Reception-Einstieg.

#### Guild

- create
- invite
- accept
- leave
- info
- disband
- GuildDialog
- Leader-/Mitglied-Anzeige
- Mitgliederzahl
- Gildenbank
- Guild Compass
- Quick-Actions
- Reception-Einstieg.

Die aktuelle Domain-Existenz wird nicht als Abschluss akzeptiert, solange diese End-to-End-Flows nicht gegen `main` geprüft sind.

---

### 21.16 Region / Editor / World-Funktionen

Der Referenzzustand besitzt:

- RegionEditor;
- RegionFlags;
- RegionPolicy;
- RegionSpawn;
- RegionTransition;
- RegionCommand;
- Edit-Command.

Der Rebuild besitzt große Teile der Region-Domain bereits.

Offen ist insbesondere die **vollständige Bedienparität des Admin-Editors**, da `main` explizit `/pixelrpg edit spawn <living-entity>` besitzt und der aktuelle Rebuild-Command diese Referenzoberfläche nicht vollständig abbildet.

Zu prüfen:

- Spawn-Editor;
- Entity-Typ-Auswahl;
- Regionpunkte;
- Flags;
- Spawnregeln;
- Übergänge;
- Admin-Feedback;
- Tab-Completion;
- Persistenz.

---

### 21.17 Fehlende Listener / Verdrahtung — ausdrücklich prüfen

Eine statische Gegenprüfung des aktuellen Rebuild-Listenerbestands hat fünf Klassen ergeben, deren direkte Registrierung in der Composition Root nicht nachgewiesen ist:

- `CompanionExperienceListener`
- `CompanionRuntimeListener`
- `GuildBankListener`
- `ProfessionActivityListener`
- `GuildCompassListener`.

Das ist zunächst ein **Audit-Alarm und noch kein Beweis für einen Fehler**, da eine indirekte Registrierung möglich ist.

Vor Release muss für jede Klasse eindeutig dokumentiert sein:

`Listener -> Registrierungsstelle -> auslösender Event -> fachlicher Service -> sichtbare Wirkung`

Falls keine Registrierung existiert, ist die Funktion zu reparieren.

---

### 21.18 PartyAPI-Adapter — ausdrücklich prüfen

Im aktuellen Rebuild existiert eine `PartyApiAdapter`, deren Methoden als statische Fallbackwerte implementiert sind:

- `isInParty(...) -> false`
- `getPartyMembers(...) -> Set.of(id)`
- `getPartyLeader(...) -> id`
- `isLeader(...) -> true`
- `isWithinShareRange(...) -> source.equals(target)`
- `getShareRange() -> 0.0`.

Da gleichzeitig eine echte `PartyManager`-Domain vorhanden ist, muss vor Release eindeutig geklärt werden, ob dieser Adapter noch irgendwo verwendet wird.

**Regel:**

- Wenn unbenutzt: entfernen.
- Wenn verwendet: durch die echte Party-Domain anbinden.
- Kein Fake-/Fallback-Partyverhalten darf im produktiven Runtime-Pfad verbleiben.

---

### 21.19 Command- und UI-Parität ist ein eigener Abnahmetest

Vor einer Release-Freigabe wird eine reine Build-Prüfung nicht mehr als ausreichend betrachtet.

Es muss eine Paritätsmatrix abgearbeitet werden:

| Bereich | main SOLL | rebuild IST | Status |
|---|---|---|---|
| Root-Commands | vollständig | reduziert/zentralisiert | OFFEN |
| Admin-Commands | vollständig | nicht vollständig | OFFEN |
| Party | Domain + GUI | Domain + reduzierte Ausgabe | OFFEN |
| Guild | Domain + Dialog/Commands | Domain vorhanden | OFFEN |
| Questlog | GUI + Details | keine GUI | OFFEN |
| Crafting | GUI | Dialog-Service | OFFEN |
| Shop | GUI + Editor | Dialog + Service | OFFEN |
| Trade Depot | 2 GUIs | Dialog + Service | OFFEN |
| Persönliche Bank | Bankdialog + Inventar | nicht vollständig | OFFEN |
| Handelsfach | persistent + GUI | nicht vollständig | OFFEN |
| Companion Equipment | GUI + Persistence | nicht vollständig | OFFEN |
| Companion Runtime | vollständige Domain | neu strukturiert | PRÜFEN |
| NPC | Behavior-System | Runtime/Listener | PRÜFEN |
| Quest Runtime | Manager/Tracker/Navigation | Service/Listener | PRÜFEN |
| Region Editor | Admin-Editor | teilweise vorhanden | OFFEN |
| öffentliche API | Provider + Services | Provider fehlt | OFFEN |
| Scoreboard | Darstellung + Persistenz | neu aufgebaut | PRÜFEN |
| Playtime | Darstellung + Persistenz | neu aufgebaut | PRÜFEN |
| Resourcepack | vollständig | vollständig laut Tree-Abgleich | PRÜFEN |
| Build/CI | vollständige Verifikation | aktuelle HEAD-Verifikation ausstehend | OFFEN |

---

### 21.20 Was ausdrücklich NICHT gemacht werden darf

Die folgenden Abkürzungen sind ab diesem Audit verboten:

1. `main`-GUI löschen und behaupten, ein Dialog sei automatisch gleichwertig.
2. Einen fehlenden Command als "Legacy" markieren, ohne seine fachliche Funktion nachzuweisen.
3. Eine fehlende Funktion nur anhand eines neuen Service-Namens als vorhanden betrachten.
4. Eine fehlende Listener-Registrierung mit "wird bestimmt indirekt registriert" abhaken.
5. Einen Fake-API-Adapter als produktive Implementierung behalten.
6. Nur den Build als Funktionsnachweis verwenden.
7. Nur Dateianzahl vergleichen.
8. Alte Dateien blind kopieren.
9. Die Referenzdarstellung ohne ausdrücklichen Auftrag verändern.
10. Release-Parität anhand einer Audit-Behauptung statt anhand des aktuellen Codes feststellen.

---

### 21.21 Verbindliche nächste Arbeitsreihenfolge

Nach dieser Analyse wird nicht wieder pauschal "Phase 17 abgeschlossen" dokumentiert.

Die weitere Arbeit erfolgt in dieser Reihenfolge:

1. **Command-Parität vollständig herstellen**
2. **Questlog / Party / Crafting / Shop / Trade-Depot UI gegen main rekonstruieren**
3. **Bank + Handelsfach + Banker-End-to-End herstellen**
4. **Companion-Equipment + Persistence + UI herstellen**
5. **öffentliche API / Provider wiederherstellen**
6. **alle Listener-Registrierungen forensisch schließen**
7. **Quest-Navigation / passive / globale Questlogik gegen main verifizieren**
8. **NPC-Verhalten und Präsentation pro NpcType verifizieren**
9. **Region-/Admin-Editor-Parität herstellen**
10. **Scoreboard / Playtime / Guild / Party End-to-End gegen main testen**
11. **Resource-/Content-Parität final prüfen**
12. **vollständige Build-/CI-Verifikation auf aktuellem rebuild-HEAD**
13. **erst danach Release-Freigabe des Rebuilds**.

### 21.22 Abschlusskriterium dieses Audits

Der Rebuild darf erst als **SOLL erreicht** bezeichnet werden, wenn für jede relevante Funktion aus `main` eine der folgenden Aussagen nachweisbar ist:

- **REBUILT:** Funktion vollständig neu implementiert und Verhalten/Darstellung geprüft.
- **MERGED:** Funktion in einen neuen Service integriert und End-to-End geprüft.
- **REPLACED:** bewusst technisch anders umgesetzt, aber gleiche sichtbare/fachliche Funktion nachgewiesen.
- **REMOVED:** nur wenn nachweislich keine funktionale Bedeutung im Referenzzustand besteht.

**"Datei existiert nicht mehr" ist kein Nachweis für "Funktion existiert nicht mehr".**

Der aktuelle Stand wird daher ausdrücklich als:

> **CLEAN REBUILD — TECHNISCH FORTGESCHRITTEN, ABER NOCH NICHT SOLL-/RELEASE-PARITÄT**

geführt.

Diese Sektion ist ab sofort Bestandteil der verbindlichen `audit.md`-Arbeitsanweisung und bei jeder weiteren Änderung auf `rebuild` zuerst zu berücksichtigen.
